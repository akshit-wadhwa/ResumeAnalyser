package com.resumeanalyser.backend.dto;

public class AnalysisStatusResponse {

    private String status;
    private AnalysisResultDto result;
    private String errorMessage;

    public AnalysisStatusResponse(String status, AnalysisResultDto result, String errorMessage) {
        this.status = status;
        this.result = result;
        this.errorMessage = errorMessage;
    }

    public String getStatus() {
        return status;
    }

    public AnalysisResultDto getResult() {
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    

    }
}
