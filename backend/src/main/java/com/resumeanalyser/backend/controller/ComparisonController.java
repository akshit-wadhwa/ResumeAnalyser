package com.resumeanalyser.backend.controller;

import com.resumeanalyser.backend.dto.ComparisonResponse;
import com.resumeanalyser.backend.model.User;
import com.resumeanalyser.backend.service.AuthService;
import com.resumeanalyser.backend.service.ComparisonService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/comparison")
public class ComparisonController {

    private final ComparisonService comparisonService;
    private final AuthService authService;

    public ComparisonController(ComparisonService comparisonService, AuthService authService) {
        this.comparisonService = comparisonService;
        this.authService = authService;
    }

    @PostMapping(value = "/rank", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComparisonResponse> rank(
            @RequestParam("resumes") List<MultipartFile> resumes,
            @RequestParam("jobText") String jobText,
            @RequestParam("userEmail") String userEmail,
            @RequestParam("userPassword") String userPassword
    ) throws Exception {
        User user = authService.loginOrCreate(userEmail, userPassword);
        return ResponseEntity.ok(comparisonService.compare(user, resumes, jobText));
    }
}
