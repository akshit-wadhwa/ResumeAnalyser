package com.resumeanalyser.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.resumeanalyser.backend.dto.LoginRequest;
import com.resumeanalyser.backend.dto.LoginResponse;
import com.resumeanalyser.backend.dto.RegisterRequest;
import com.resumeanalyser.backend.model.User;
import com.resumeanalyser.backend.service.AuthService;
import com.resumeanalyser.backend.util.HashingUtils;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            User user = authService.login(request.getEmail(), request.getPassword());
            String token = HashingUtils.sha256(user.getEmail() + ":" + System.currentTimeMillis());
            LoginResponse response = new LoginResponse(user.getId(), user.getRole(), token);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = authService.register(request.getEmail(), request.getPassword());
            String token = HashingUtils.sha256(user.getEmail() + ":" + System.currentTimeMillis());
            LoginResponse response = new LoginResponse(user.getId(), user.getRole(), token);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
