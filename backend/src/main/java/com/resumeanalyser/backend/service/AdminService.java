package com.resumeanalyser.backend.service;

import com.resumeanalyser.backend.dto.AdminSummaryDto;
import com.resumeanalyser.backend.repository.AnalysisResultRepository;
import com.resumeanalyser.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AnalysisResultRepository analysisResultRepository;

    public AdminService(UserRepository userRepository, AnalysisResultRepository analysisResultRepository) {
        this.userRepository = userRepository;
        this.analysisResultRepository = analysisResultRepository;
    }

    public AdminSummaryDto summary() {
        long users = userRepository.countUsers();
        long analyses = analysisResultRepository.countAnalyses();
        double avg = analysisResultRepository.averageMatchScore();
        return new AdminSummaryDto(users, analyses, avg);
    }
}
