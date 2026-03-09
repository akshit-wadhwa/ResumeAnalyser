package com.resumeanalyser.backend.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MLClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String url;

    public MLClient(RestTemplate restTemplate, ObjectMapper objectMapper,
            @Value("${ml.endpoint}") String endpoint) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.url = endpoint;
    }

    public MLResponse analyze(String resumeText, String jobText) {
        Map<String, String> payload = new HashMap<>();
        payload.put("resume_text", resumeText);
        payload.put("job_text", jobText);
        String json = restTemplate.postForObject(url, payload, String.class);
        try {
            return objectMapper.readValue(json, MLResponse.class);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("ML response parsing failed", ex);
        }
    }

    public static class MLResponse {

        public double match_score;
        public double confidence_score;
        public java.util.List<String> matched_skills;
        public java.util.List<String> missing_skills;
        public java.util.List<String> weaknesses;
        public java.util.List<String> recommendations;
        public double experience_years;
        public java.util.List<String> education;
        public String seniority;
    }
}
