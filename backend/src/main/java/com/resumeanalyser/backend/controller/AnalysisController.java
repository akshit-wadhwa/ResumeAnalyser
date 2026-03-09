package com.resumeanalyser.backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.resumeanalyser.backend.dto.AnalysisResultDto;
import com.resumeanalyser.backend.dto.AnalysisStartResponse;
import com.resumeanalyser.backend.dto.AnalysisStatusResponse;
import com.resumeanalyser.backend.model.User;
import com.resumeanalyser.backend.service.AnalysisService;
import com.resumeanalyser.backend.service.AnalysisState;
import com.resumeanalyser.backend.service.AnalysisStatus;
import com.resumeanalyser.backend.service.AuthService;
import com.resumeanalyser.backend.service.ReportService;

import jakarta.validation.constraints.Email;

@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AuthService authService;
    private final ReportService reportService;

    public AnalysisController(AnalysisService analysisService, AuthService authService, ReportService reportService) {
        this.analysisService = analysisService;
        this.authService = authService;
        this.reportService = reportService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisStartResponse> analyze(
            @RequestParam("resume") MultipartFile resume,
            @RequestParam(value = "jobFile", required = false) MultipartFile jobFile,
            @RequestParam(value = "jobText", required = false) String jobText,
            @RequestParam("userEmail") @Email String userEmail,
            @RequestParam("userPassword") String userPassword
    ) {
        try {
            // Authenticate user - no auto-creation
            User user = authService.login(userEmail, userPassword);
            String analysisId = analysisService.startAnalysis(user, resume, jobFile, jobText);
            return ResponseEntity.ok(new AnalysisStartResponse(analysisId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/status/{analysisId}")
    public ResponseEntity<AnalysisStatusResponse> status(@PathVariable("analysisId") String analysisId) {
        AnalysisState state = analysisService.getStatus(analysisId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        AnalysisResultDto result = state.getStatus() == AnalysisStatus.COMPLETED ? state.getResult() : null;
        return ResponseEntity.ok(new AnalysisStatusResponse(state.getStatus().name(), result, state.getErrorMessage()));
    }

    @GetMapping("/{analysisId}/report")
    public ResponseEntity<byte[]> report(@PathVariable("analysisId") String analysisId) throws Exception {
        AnalysisState state = analysisService.getStatus(analysisId);
        if (state == null || state.getResult() == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] pdf = reportService.buildReport(state.getResult());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analysis-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
