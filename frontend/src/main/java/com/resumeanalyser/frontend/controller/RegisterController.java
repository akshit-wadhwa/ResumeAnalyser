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

    private final ApiClient api = new ApiClient("http://localhost:8081");

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

        String error = validateInput(email, fullName, password, confirmPassword);
        if (error != null) {
            statusLabel.setText(error);
            return;
        }

        statusLabel.setText("Creating account...");
        Task<UserSession> task = new Task<>() {
            @Override
            protected UserSession call() throws Exception {
                UserSession session = api.register(email, password, fullName);
                session.setEmail(email);
                return session;
            }
        };

        task.setOnSucceeded(event -> {
            onRegisterSuccess(task.getValue(), password);
        });

        task.setOnFailed(event -> {
            statusLabel.setText("Registration failed");
        });

        new Thread(task).start();
    }

    private String validateInput(String email, String fullName, String password, String confirmPassword) {
        if (email == null || email.isBlank()) {
            return "Email is required";
        }
        if (!email.contains("@")) {
            return "Please enter a valid email";
        }
        if (fullName == null || fullName.isBlank()) {
            return "Full name is required";
        }
        if (password == null || password.length() < 6) {
            return "Password must be at least 6 characters";
        }
        if (!password.equals(confirmPassword)) {
            return "Passwords do not match";
        }
        return null;
    }

    private void onRegisterSuccess(UserSession session, String password) {
        AppState.setSession(session);
        AppState.setPassword(password);
        ViewNavigator.navigate("/fxml/upload.fxml");
    }

    @FXML
    private void onGoToLogin() {
        ViewNavigator.navigate("/fxml/login.fxml");
    }
}
