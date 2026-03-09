package com.resumeanalyser.backend.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.resumeanalyser.backend.model.User;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByEmail(String email) {
        return jdbcTemplate.query("SELECT * FROM users WHERE email = ?", new UserRowMapper(), email).stream().findFirst();
    }

    public Optional<User> findById(long id) {
        return jdbcTemplate.query("SELECT * FROM users WHERE id = ?", new UserRowMapper(), id).stream().findFirst();
    }

    public long save(User user) {
        jdbcTemplate.update(
                "INSERT INTO users (email, password_hash, role) VALUES (?, ?, ?)",
                user.getEmail(), user.getPasswordHash(), user.getRole()
        );
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, user.getEmail());
    }

    public long countUsers() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
    }

    private static class UserRowMapper implements RowMapper<User> {

        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setEmail(rs.getString("email"));
            user.setPasswordHash(rs.getString("password_hash"));
            user.setRole(rs.getString("role"));
            user.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
            return user;
        }
    }
}
