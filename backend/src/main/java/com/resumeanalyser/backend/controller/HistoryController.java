package com.resumeanalyser.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.resumeanalyser.backend.dto.AnalysisResultDto;
import com.resumeanalyser.backend.model.User;
import com.resumeanalyser.backend.service.AnalysisService;
import com.resumeanalyser.backend.service.AuthService;

@RestController
@RequestMapping("/history")
public class HistoryController {

    private final AnalysisService analysisService;
    private final AuthService authService;

    public HistoryController(AnalysisService analysisService, AuthService authService) {
        this.analysisService = analysisService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<AnalysisResultDto>> getHistory(
            @RequestParam("userEmail") String userEmail,
            @RequestParam("userPassword") String userPassword
    ) {
        try {
            User user = authService.login(userEmail, userPassword);
            List<AnalysisResultDto> history = analysisService.getUserAnalysisHistory(user.getId());
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }
}
