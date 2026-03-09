package com.resumeanalyser.backend.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyser.backend.model.AnalysisResult;

@Repository
public class AnalysisResultRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AnalysisResultRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public long save(AnalysisResult result, String matchedJson, String missingJson, String weaknessesJson,
            String recommendationsJson, String stepsJson) {
        jdbcTemplate.update(
                "INSERT INTO analysis_results (user_id, resume_id, job_id, match_score, confidence_score, "
                + "matched_skills, missing_skills, weaknesses, recommendations, processing_steps) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                result.getUserId(), result.getResumeId(), result.getJobId(), result.getMatchScore(),
                result.getConfidenceScore(), matchedJson, missingJson, weaknessesJson, recommendationsJson, stepsJson
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM analysis_results WHERE user_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                result.getUserId()
        );
    }

    public long countAnalyses() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM analysis_results", Long.class);
    }

    public Double averageMatchScore() {
        return jdbcTemplate.queryForObject("SELECT COALESCE(AVG(match_score), 0) FROM analysis_results", Double.class);
    }

    public List<AnalysisResult> findByUserId(long userId) {
        return jdbcTemplate.query(
                "SELECT * FROM analysis_results WHERE user_id = ? ORDER BY created_at DESC LIMIT 50",
                new AnalysisResultRowMapper(objectMapper),
                userId
        );
    }

    private static class AnalysisResultRowMapper implements RowMapper<AnalysisResult> {

        private final ObjectMapper objectMapper;

        public AnalysisResultRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public AnalysisResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            AnalysisResult result = new AnalysisResult();
            result.setId(rs.getLong("id"));
            result.setUserId(rs.getLong("user_id"));
            result.setResumeId(rs.getLong("resume_id"));
            result.setJobId(rs.getLong("job_id"));
            result.setMatchScore(rs.getDouble("match_score"));
            result.setConfidenceScore(rs.getDouble("confidence_score"));
            result.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));

            // Parse JSON fields
            try {
                String matchedJson = rs.getString("matched_skills");
                if (matchedJson != null) {
                    result.setMatchedSkills(objectMapper.readValue(matchedJson,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
                }

                String missingJson = rs.getString("missing_skills");
                if (missingJson != null) {
                    result.setMissingSkills(objectMapper.readValue(missingJson,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
                }

                String weaknessesJson = rs.getString("weaknesses");
                if (weaknessesJson != null) {
                    result.setWeaknesses(objectMapper.readValue(weaknessesJson,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
                }

                String recommendationsJson = rs.getString("recommendations");
                if (recommendationsJson != null) {
                    result.setRecommendations(objectMapper.readValue(recommendationsJson,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
                }
            } catch (Exception e) {
                // If JSON parsing fails, just leave fields empty
            }

            return result;
        }
    }
}
