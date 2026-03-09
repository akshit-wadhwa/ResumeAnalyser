package com.resumeanalyser.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyser.backend.dto.ComparisonResponse;
import com.resumeanalyser.backend.dto.RankedResumeDto;
import com.resumeanalyser.backend.model.JobDescription;
import com.resumeanalyser.backend.model.ResumeDocument;
import com.resumeanalyser.backend.model.User;
import com.resumeanalyser.backend.repository.ComparisonRepository;
import com.resumeanalyser.backend.repository.JobRepository;
import com.resumeanalyser.backend.repository.ResumeRepository;
import com.resumeanalyser.backend.util.JsonUtils;
import com.resumeanalyser.core.parsing.ParsingResult;

@Service
public class ComparisonService {

    private final ParsingService parsingService;
    private final MLClient mlClient;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final ComparisonRepository comparisonRepository;
    private final JsonUtils jsonUtils;

    public ComparisonService(ParsingService parsingService,
            MLClient mlClient,
            ResumeRepository resumeRepository,
            JobRepository jobRepository,
            ComparisonRepository comparisonRepository,
            ObjectMapper mapper) {
        this.parsingService = parsingService;
        this.mlClient = mlClient;
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.comparisonRepository = comparisonRepository;
        this.jsonUtils = new JsonUtils(mapper);
    }

    public ComparisonResponse compare(User user, List<MultipartFile> resumes, String jobText) throws Exception {
        JobDescription job = new JobDescription();
        job.setUserId(user.getId());
        job.setTitle("Comparison Job");
        job.setContentText(jobText);
        job.setCreatedAt(LocalDateTime.now());
        long jobId = jobRepository.save(job);

        List<String> resumeIds = new ArrayList<>();
        List<RankedResumeDto> ranking = new ArrayList<>();

        for (MultipartFile file : resumes) {
            ParsingResult result = parsingService.parseDocument(file);
            ResumeDocument resume = new ResumeDocument();
            resume.setUserId(user.getId());
            resume.setFilename(file.getOriginalFilename());
            resume.setContentText(result.getText());
            resume.setCreatedAt(LocalDateTime.now());
            long resumeId = resumeRepository.save(resume);
            resumeIds.add(String.valueOf(resumeId));

            MLClient.MLResponse response = mlClient.analyze(result.getText(), jobText);
            ranking.add(new RankedResumeDto(file.getOriginalFilename(), response.match_score,
                    response.confidence_score, response.matched_skills, response.missing_skills));
        }

        ranking.sort(Comparator.comparingDouble(RankedResumeDto::getMatchScore).reversed());
        comparisonRepository.save(user.getId(), jobId, jsonUtils.toJson(resumeIds), jsonUtils.toJson(ranking));
        return new ComparisonResponse(ranking);
    }
}
