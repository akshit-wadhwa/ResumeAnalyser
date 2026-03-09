package com.resumeanalyser.backend.dto;

import java.util.List;

public class RankedResumeDto {

    private String filename;
    private double matchScore;
    private double confidenceScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;

    public RankedResumeDto(String filename, double matchScore, double confidenceScore,
            List<String> matchedSkills, List<String> missingSkills) {
        this.filename = filename;
        this.matchScore = matchScore;
        this.confidenceScore = confidenceScore;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
    }

    public String getFilename() {
        return filename;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }
}
