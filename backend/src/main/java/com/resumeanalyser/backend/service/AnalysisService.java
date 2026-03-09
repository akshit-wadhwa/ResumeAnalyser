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
        AnalysisState analysisState = new AnalysisState(AnalysisStatus.RUNNING);
        List<ProcessingStepDto> steps = new ArrayList<>();
        try {
            steps.add(newStep("Parse resume", "RUNNING"));
            ParsingResult resumeResult = parsingService.parseDocument(resumeFilename, resumeBytes);
            steps.add(newStep("Parse resume", "DONE"));

            String jobContent = readJobContent(jobText, jobFilename, jobBytes, steps);

            String cacheKey = HashingUtils.sha256(resumeResult.getText() + "|" + jobContent);
            AnalysisResultDto cachedResult = cacheService.get(cacheKey);
            if (cachedResult != null) {
                cachedResult.setAnalysisId(analysisId);
                cachedResult.setProcessingSteps(steps);
                completeSuccess(analysisId, analysisState, cachedResult);
                return;
            }

            steps.add(newStep("ML analysis", "RUNNING"));
            MLClient.MLResponse mlResult = mlClient.analyze(resumeResult.getText(), jobContent);
            steps.add(newStep("ML analysis", "DONE"));

            steps.add(newStep("Store results", "RUNNING"));
            long resumeId = resumeRepository.save(buildResume(user, resumeFilename, resumeResult.getText()));
            long jobId = jobRepository.save(buildJob(user, jobContent));
            AnalysisResult result = buildAnalysisResult(user, resumeId, jobId, mlResult);

            analysisResultRepository.save(
                    result,
                    jsonUtils.toJson(result.getMatchedSkills()),
                    jsonUtils.toJson(result.getMissingSkills()),
                    jsonUtils.toJson(result.getWeaknesses()),
                    jsonUtils.toJson(result.getRecommendations()),
                    jsonUtils.toJson(steps)
            );
            steps.add(newStep("Store results", "DONE"));

            AnalysisResultDto resultDto = toDto(analysisId, result, steps);
            cacheService.put(cacheKey, resultDto);

            completeSuccess(analysisId, analysisState, resultDto);
        } catch (Exception ex) {
            logger.error("Analysis failed", ex);
            completeFailure(analysisId, analysisState, ex);
        }
    }

    private String readJobContent(String jobText, String jobFilename, byte[] jobBytes, List<ProcessingStepDto> steps) throws Exception {
        steps.add(newStep("Parse job", "RUNNING"));

        String value = jobText;
        if ((value == null || value.isBlank()) && jobBytes != null) {
            value = parsingService.parseDocument(jobFilename, jobBytes).getText();
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Job description is required");
        }

        steps.add(newStep("Parse job", "DONE"));
        return value;
    }

    private void completeSuccess(String analysisId, AnalysisState state, AnalysisResultDto result) {
        state.setStatus(AnalysisStatus.COMPLETED);
        state.setResult(result);
        tracker.complete(analysisId, state);
    }

    private void completeFailure(String analysisId, AnalysisState state, Exception ex) {
        state.setStatus(AnalysisStatus.FAILED);
        state.setErrorMessage(ex.getMessage());
        tracker.complete(analysisId, state);
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

    private ProcessingStepDto newStep(String name, String status) {
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
        List<AnalysisResult> rows = analysisResultRepository.findByUserId(userId);
        List<AnalysisResultDto> list = new ArrayList<>();
        for (AnalysisResult row : rows) {
            AnalysisResultDto resultDto = new AnalysisResultDto();
            resultDto.setMatchScore(row.getMatchScore());
            resultDto.setConfidenceScore(row.getConfidenceScore());
            resultDto.setMatchedSkills(row.getMatchedSkills());
            resultDto.setMissingSkills(row.getMissingSkills());
            resultDto.setWeaknesses(row.getWeaknesses());
            resultDto.setRecommendations(row.getRecommendations());
            resultDto.setExperienceYears(row.getExperienceYears());
            resultDto.setEducation(row.getEducation());
            resultDto.setSeniority(row.getSeniority());
            resultDto.setCreatedAt(row.getCreatedAt());
            list.add(resultDto);
        }
        return list;
    }
}
