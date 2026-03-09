package com.resumeanalyser.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyser.backend.dto.AnalysisResultDto;
import com.resumeanalyser.backend.dto.ProcessingStepDto;
import com.resumeanalyser.backend.model.AnalysisResult;
import com.resumeanalyser.backend.model.JobDescription;
import com.resumeanalyser.backend.model.ResumeDocument;
import com.resumeanalyser.backend.model.User;
import com.resumeanalyser.backend.repository.AnalysisResultRepository;
import com.resumeanalyser.backend.repository.JobRepository;
import com.resumeanalyser.backend.repository.ResumeRepository;
import com.resumeanalyser.backend.util.HashingUtils;
import com.resumeanalyser.backend.util.JsonUtils;
import com.resumeanalyser.core.parsing.ParsingResult;

@Service
public class AnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisService.class);

    private final ParsingService parsingService;
    private final MLClient mlClient;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final CacheService cacheService;
    private final AnalysisTracker tracker;
    private final Executor analysisExecutor;
    private final JsonUtils jsonUtils;

    public AnalysisService(ParsingService parsingService,
            MLClient mlClient,
            ResumeRepository resumeRepository,
            JobRepository jobRepository,
            AnalysisResultRepository analysisResultRepository,
            CacheService cacheService,
            AnalysisTracker tracker,
            Executor analysisExecutor,
            ObjectMapper mapper) {
        this.parsingService = parsingService;
        this.mlClient = mlClient;
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.analysisResultRepository = analysisResultRepository;
        this.cacheService = cacheService;
        this.tracker = tracker;
        this.analysisExecutor = analysisExecutor;
        this.jsonUtils = new JsonUtils(mapper);
    }

    public String startAnalysis(User user, MultipartFile resumeFile, MultipartFile jobFile, String jobText) throws Exception {
        String analysisId = UUID.randomUUID().toString();
        tracker.start(analysisId);

        // Read file bytes immediately before async task starts (before Tomcat deletes temp files)
        byte[] resumeBytes = resumeFile.getBytes();
        byte[] jobBytes = jobFile != null ? jobFile.getBytes() : null;
        String resumeFilename = resumeFile.getOriginalFilename();
        String jobFilename = jobFile != null ? jobFile.getOriginalFilename() : null;

        CompletableFuture.runAsync(() -> runAnalysis(analysisId, user, resumeFilename, resumeBytes, jobFilename, jobBytes, jobText), analysisExecutor);
        return analysisId;
    }

    public AnalysisState getStatus(String analysisId) {
        return tracker.get(analysisId);
    }

    private void runAnalysis(String analysisId, User user, String resumeFilename, byte[] resumeBytes,
            String jobFilename, byte[] jobBytes, String jobText) {
        AnalysisState state = new AnalysisState(AnalysisStatus.RUNNING);
        List<ProcessingStepDto> steps = new ArrayList<>();
        try {
            steps.add(step("Parse resume", "RUNNING"));
            ParsingResult resumeResult = parsingService.parseDocument(resumeFilename, resumeBytes);
            steps.add(step("Parse resume", "DONE"));

            steps.add(step("Parse job", "RUNNING"));
            String jobContent = jobText;
            if ((jobContent == null || jobContent.isBlank()) && jobBytes != null) {
                jobContent = parsingService.parseDocument(jobFilename, jobBytes).getText();
            }
            if (jobContent == null || jobContent.isBlank()) {
                throw new IllegalArgumentException("Job description is required");
            }
            steps.add(step("Parse job", "DONE"));

            String cacheKey = HashingUtils.sha256(resumeResult.getText() + "|" + jobContent);
            AnalysisResultDto cached = cacheService.get(cacheKey);
            if (cached != null) {
                cached.setAnalysisId(analysisId);
                cached.setProcessingSteps(steps);
                state.setStatus(AnalysisStatus.COMPLETED);
                state.setResult(cached);
                tracker.complete(analysisId, state);
                return;
            }

            steps.add(step("ML analysis", "RUNNING"));
            MLClient.MLResponse ml = mlClient.analyze(resumeResult.getText(), jobContent);
            steps.add(step("ML analysis", "DONE"));

            steps.add(step("Store results", "RUNNING"));
            long resumeId = resumeRepository.save(buildResume(user, resumeFilename, resumeResult.getText()));
            long jobId = jobRepository.save(buildJob(user, jobContent));
            AnalysisResult result = buildAnalysisResult(user, resumeId, jobId, ml);

            analysisResultRepository.save(
                    result,
                    jsonUtils.toJson(result.getMatchedSkills()),
                    jsonUtils.toJson(result.getMissingSkills()),
                    jsonUtils.toJson(result.getWeaknesses()),
                    jsonUtils.toJson(result.getRecommendations()),
                    jsonUtils.toJson(steps)
            );
            steps.add(step("Store results", "DONE"));

            AnalysisResultDto dto = toDto(analysisId, result, steps);
            cacheService.put(cacheKey, dto);

            state.setStatus(AnalysisStatus.COMPLETED);
            state.setResult(dto);
            tracker.complete(analysisId, state);
        } catch (Exception ex) {
            logger.error("Analysis failed", ex);
            state.setStatus(AnalysisStatus.FAILED);
            state.setErrorMessage(ex.getMessage());
            tracker.complete(analysisId, state);
        }
    }

    private ResumeDocument buildResume(User user, String filename, String text) {
        ResumeDocument resume = new ResumeDocument();
        resume.setUserId(user.getId());
        resume.setFilename(filename != null ? filename : "resume.pdf");
        resume.setContentText(text);
        resume.setCreatedAt(LocalDateTime.now());
        return resume;
    }

    private JobDescription buildJob(User user, String jobText) {
        JobDescription job = new JobDescription();
        job.setUserId(user.getId());
        job.setTitle("Job Description");
        job.setContentText(jobText);
        job.setCreatedAt(LocalDateTime.now());
        return job;
    }

    private AnalysisResult buildAnalysisResult(User user, long resumeId, long jobId, MLClient.MLResponse ml) {
        AnalysisResult result = new AnalysisResult();
        result.setUserId(user.getId());
        result.setResumeId(resumeId);
        result.setJobId(jobId);
        result.setMatchScore(ml.match_score);
        result.setConfidenceScore(ml.confidence_score);
        result.setMatchedSkills(ml.matched_skills);
        result.setMissingSkills(ml.missing_skills);
        result.setWeaknesses(ml.weaknesses);
        result.setRecommendations(ml.recommendations);
        result.setExperienceYears(ml.experience_years);
        result.setEducation(ml.education);
        result.setSeniority(ml.seniority);
        result.setCreatedAt(LocalDateTime.now());
        return result;
    }

    private ProcessingStepDto step(String name, String status) {
        return new ProcessingStepDto(name, status, LocalDateTime.now().toString());
    }

    private AnalysisResultDto toDto(String analysisId, AnalysisResult result, List<ProcessingStepDto> steps) {
        AnalysisResultDto dto = new AnalysisResultDto();
        dto.setAnalysisId(analysisId);
        dto.setMatchScore(result.getMatchScore());
        dto.setConfidenceScore(result.getConfidenceScore());
        dto.setMatchedSkills(result.getMatchedSkills());
        dto.setMissingSkills(result.getMissingSkills());
        dto.setWeaknesses(result.getWeaknesses());
        dto.setRecommendations(result.getRecommendations());
        dto.setExperienceYears(result.getExperienceYears());
        dto.setEducation(result.getEducation());
        dto.setSeniority(result.getSeniority());
        dto.setProcessingSteps(steps);
        return dto;
    }

    public List<AnalysisResultDto> getUserAnalysisHistory(long userId) {
        List<AnalysisResult> results = analysisResultRepository.findByUserId(userId);
        return results.stream().map(result -> {
            AnalysisResultDto dto = new AnalysisResultDto();
            dto.setMatchScore(result.getMatchScore());
            dto.setConfidenceScore(result.getConfidenceScore());
            dto.setMatchedSkills(result.getMatchedSkills());
            dto.setMissingSkills(result.getMissingSkills());
            dto.setWeaknesses(result.getWeaknesses());
            dto.setRecommendations(result.getRecommendations());
            dto.setExperienceYears(result.getExperienceYears());
            dto.setEducation(result.getEducation());
            dto.setSeniority(result.getSeniority());
            dto.setCreatedAt(result.getCreatedAt());
            return dto;
        }).toList();
    }
}
