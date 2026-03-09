package com.resumeanalyser.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ComparisonRepository {

    private final JdbcTemplate jdbcTemplate;

    public ComparisonRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(long userId, long jobId, String resumeIdsJson, String rankingJson) {
        jdbcTemplate.update(
                "INSERT INTO comparisons (user_id, job_id, resume_ids, ranking) VALUES (?, ?, ?, ?)",
                userId, jobId, resumeIdsJson, rankingJson
        );
    }
}
