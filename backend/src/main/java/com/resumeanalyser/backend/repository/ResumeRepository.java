package com.resumeanalyser.backend.repository;

import com.resumeanalyser.backend.model.ResumeDocument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ResumeRepository {

    private final JdbcTemplate jdbcTemplate;

    public ResumeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long save(ResumeDocument resume) {
        jdbcTemplate.update(
                "INSERT INTO resumes (user_id, filename, content_text) VALUES (?, ?, ?)",
                resume.getUserId(), resume.getFilename(), resume.getContentText()
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM resumes WHERE user_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                resume.getUserId()
        );
    }
}
