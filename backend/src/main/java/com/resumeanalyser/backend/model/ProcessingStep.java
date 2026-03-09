package com.resumeanalyser.backend.model;

import java.time.LocalDateTime;

public class ProcessingStep {

    private String name;
    private String status;
    private LocalDateTime timestamp;

    public ProcessingStep(String name, String status, LocalDateTime timestamp) {
        this.name = name;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
