package com.resumeanalyser.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class MLClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String endpoint;

    public MLClient(RestTemplate restTemplate, ObjectMapper objectMapper,
            @Value("${ml.endpoint}") String endpoint) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
    }

    public MLResponse analyze(String resumeText, String jobText) {
        Map<String, String> payload = Map.of("resume_text", resumeText, "job_text", jobText);
        String response = restTemplate.postForObject(endpoint, payload, String.class);
        try {
            return objectMapper.readValue(response, MLResponse.class);
        } catch (Exception ex) {
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
