package com.resumeanalyser.backend.service;

import com.resumeanalyser.backend.dto.AnalysisResultDto;

public class AnalysisState {

    private AnalysisStatus status;
    private AnalysisResultDto result;
    private String errorMessage;

    public AnalysisState(AnalysisStatus status) {
        this.status = status;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }

    public AnalysisResultDto getResult() {
        return result;
    }

    public void setResult(AnalysisResultDto result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
