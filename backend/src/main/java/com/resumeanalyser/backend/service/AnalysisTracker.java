package com.resumeanalyser.backend.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnalysisTracker {

    private final Map<String, AnalysisState> states = new ConcurrentHashMap<>();

    public void start(String id) {
        states.put(id, new AnalysisState(AnalysisStatus.RUNNING));
    }

    public AnalysisState get(String id) {
        return states.get(id);
    }

    public void complete(String id, AnalysisState state) {
        states.put(id, state);
    }
}
