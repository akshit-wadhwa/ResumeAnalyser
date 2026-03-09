package com.resumeanalyser.backend.dto;

public class ProcessingStepDto {

    private String name;
    private String status;
    private String timestamp;

    public ProcessingStepDto() {
    }

    public ProcessingStepDto(String name, String status, String timestamp) {
        this.name = name;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
