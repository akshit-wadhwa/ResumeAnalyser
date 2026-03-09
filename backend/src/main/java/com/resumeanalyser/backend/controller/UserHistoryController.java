package com.resumeanalyser.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.resumeanalyser.backend.dto.AnalysisResultDto;
import com.resumeanalyser.backend.repository.UserRepository;
import com.resumeanalyser.backend.service.AnalysisService;

@RestController
@RequestMapping("/history")
public class UserHistoryController {

    private final UserRepository userRepository;
    private final AnalysisService analysisService;

    public UserHistoryController(UserRepository userRepository, AnalysisService analysisService) {
        this.userRepository = userRepository;
        this.analysisService = analysisService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<AnalysisResultDto>> getUserHistory(
            @PathVariable long userId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<AnalysisResultDto> history = analysisService.getUserAnalysisHistory(userId);
        return ResponseEntity.ok(history);
    }
}
