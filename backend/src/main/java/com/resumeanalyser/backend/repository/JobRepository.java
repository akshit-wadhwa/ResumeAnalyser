package com.resumeanalyser.backend.repository;

import com.resumeanalyser.backend.model.JobDescription;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JobRepository {

    private final JdbcTemplate jdbcTemplate;

    public JobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long save(JobDescription job) {
        jdbcTemplate.update(
                "INSERT INTO job_descriptions (user_id, title, content_text) VALUES (?, ?, ?)",
                job.getUserId(), job.getTitle(), job.getContentText()
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM job_descriptions WHERE user_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                job.getUserId()
        );
    }
}
