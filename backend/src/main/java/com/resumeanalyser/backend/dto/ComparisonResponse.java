package com.resumeanalyser.backend.dto;

import java.util.List;

public class ComparisonResponse {

    private final List<RankedResumeDto> ranking;

    public ComparisonResponse(List<RankedResumeDto> ranking) {
        this.ranking = ranking;
    }

    public List<RankedResumeDto> getRanking() {
        return ranking;
    }
}
