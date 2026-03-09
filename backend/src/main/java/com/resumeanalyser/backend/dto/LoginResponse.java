package com.resumeanalyser.backend.dto;

public class LoginResponse {

    private long userId;
    private String role;
    private String token;

    public LoginResponse(long userId, String role, String token) {
        this.userId = userId;
        this.role = role;
        this.token = token;
    }

    public long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }
}
