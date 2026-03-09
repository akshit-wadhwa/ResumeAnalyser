package com.resumeanalyser.backend.service;

import org.springframework.stereotype.Service;

import com.resumeanalyser.backend.model.User;
import com.resumeanalyser.backend.repository.UserRepository;
import com.resumeanalyser.backend.util.HashingUtils;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User login(String email, String password) {
        java.util.Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOptional.get();
        if (!user.getPasswordHash().equals(HashingUtils.sha256(password))) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return user;
    }

    public User register(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(HashingUtils.sha256(password));
        user.setRole("USER");
        long id = userRepository.save(user);
        user.setId(id);
        return user;
    }

    public User getUserByEmail(String email) {
        java.util.Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        return userOptional.get();
    }

    public User loginOrCreate(String email, String password) {
        java.util.Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return register(email, password);
        }

        User user = userOptional.get();
        if (!user.getPasswordHash().equals(HashingUtils.sha256(password))) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return user;
    }
}
