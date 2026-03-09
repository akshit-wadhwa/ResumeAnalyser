package com.resumeanalyser.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.resumeanalyser.backend.dto.RankedResumeDto;
import com.resumeanalyser.core.parsing.ParsingResult;

@Service
public class RankingService {

    private final ParsingService parsingService;
    private final MLClient mlClient;

    public RankingService(ParsingService parsingService, MLClient mlClient) {
        this.parsingService = parsingService;
        this.mlClient = mlClient;
    }

    public List<RankedResumeDto> rankResumes(List<MultipartFile> resumes, String jobText) throws Exception {
        List<RankedResumeDto> ranking = new ArrayList<>();
        for (MultipartFile file : resumes) {
            ParsingResult result = parsingService.parseDocument(file);
            MLClient.MLResponse response = mlClient.analyze(result.getText(), jobText);
            ranking.add(new RankedResumeDto(file.getOriginalFilename(), response.match_score,
                    response.confidence_score, response.matched_skills, response.missing_skills));
        }
        ranking.sort(Comparator.comparingDouble(RankedResumeDto::getMatchScore).reversed());
        return ranking;
    }
}
