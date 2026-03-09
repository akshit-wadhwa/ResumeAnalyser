package com.resumeanalyser.backend.config;

import com.resumeanalyser.backend.service.AnalysisTracker;
import com.resumeanalyser.backend.service.CacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StateConfig {

    @Bean
    public CacheService cacheService(@Value("${app.cache.ttl-seconds}") long ttlSeconds) {
        return new CacheService(ttlSeconds);
    }

    @Bean
    public AnalysisTracker analysisTracker() {
        return new AnalysisTracker();
    }
}
