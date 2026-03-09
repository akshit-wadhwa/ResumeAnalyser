package com.resumeanalyser.backend.dto;

public class AdminSummaryDto {

    private final long totalUsers;
    private final long totalAnalyses;
    private final double averageMatchScore;

    public AdminSummaryDto(long totalUsers, long totalAnalyses, double averageMatchScore) {
        this.totalUsers = totalUsers;
        this.totalAnalyses = totalAnalyses;
        this.averageMatchScore = averageMatchScore;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalAnalyses() {
        return totalAnalyses;
    }

    public double getAverageMatchScore() {
        return averageMatchScore;
    }
}
