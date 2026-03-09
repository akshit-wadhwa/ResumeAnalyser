package com.resumeanalyser.backend.dto;

public class AnalysisStartResponse {

    private String analysisId;

    public AnalysisStartResponse(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getAnalysisId() {
        return analysisId;
    }
}
