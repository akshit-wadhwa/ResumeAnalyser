package com.resumeanalyser.frontend.controller;

import com.resumeanalyser.frontend.model.UserSession;
import com.resumeanalyser.frontend.service.ApiClient;
import com.resumeanalyser.frontend.util.AppState;
import com.resumeanalyser.frontend.util.ViewNavigator;

import javafx.animation.FadeTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

@SuppressWarnings("unused")
public class RegisterController {

    @FXML
    private TextField emailField;
    @FXML
    private TextField fullNameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label statusLabel;
    @FXML
    private StackPane rootPane;

    private final ApiClient apiClient = new ApiClient("http://localhost:8081");

    @FXML
    public void initialize() {
        statusLabel.setText("");
        FadeTransition fade = new FadeTransition(Duration.millis(600), rootPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    @FXML
    private void onRegister() {
        String email = emailField.getText();
        String fullName = fullNameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (email == null || email.isBlank()) {
            statusLabel.setText("Email is required");
            return;
        }
        if (!email.contains("@")) {
            statusLabel.setText("Please enter a valid email");
            return;
        }
        if (fullName == null || fullName.isBlank()) {
            statusLabel.setText("Full name is required");
            return;
        }
        if (password == null || password.length() < 6) {
            statusLabel.setText("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords do not match");
            return;
        }

        statusLabel.setText("Creating account...");
        Task<UserSession> task = new Task<>() {
            @Override
            protected UserSession call() throws Exception {
                UserSession session = apiClient.register(email, password, fullName);
                session.setEmail(email);
                return session;
            }
        };

        task.setOnSucceeded(event -> {
            AppState.setSession(task.getValue());
            AppState.setPassword(password);
            ViewNavigator.navigate("/fxml/upload.fxml");
        });

        task.setOnFailed(event -> {
            String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
            statusLabel.setText(error);
        });

        new Thread(task).start();
    }

    @FXML
    private void onGoToLogin() {
        ViewNavigator.navigate("/fxml/login.fxml");
    }
}
