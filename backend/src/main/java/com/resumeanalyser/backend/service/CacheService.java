package com.resumeanalyser.backend.service;

import com.resumeanalyser.backend.dto.AnalysisResultDto;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheService {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttlSeconds;

    public CacheService(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public AnalysisResultDto get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (Instant.now().isAfter(entry.createdAt.plusSeconds(ttlSeconds))) {
            cache.remove(key);
            return null;
        }
        return entry.result;
    }

    public void put(String key, AnalysisResultDto result) {
        cache.put(key, new CacheEntry(result, Instant.now()));
    }

    private static class CacheEntry {

        private final AnalysisResultDto result;
        private final Instant createdAt;

        private CacheEntry(AnalysisResultDto result, Instant createdAt) {
            this.result = result;
            this.createdAt = createdAt;
        }
    }
}
