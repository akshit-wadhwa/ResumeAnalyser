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
public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
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
    private void onLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();
        if (!isValidInput(email, password)) {
            statusLabel.setText("Enter email and password");
            return;
        }

        statusLabel.setText("Signing in...");
        Task<UserSession> task = new Task<>() {
            @Override
            protected UserSession call() throws Exception {
                UserSession session = api.login(email, password);
                session.setEmail(email);
                return session;
            }
        };

        task.setOnSucceeded(event -> {
            onLoginSuccess(task.getValue(), password);
        });

        task.setOnFailed(event -> {
            statusLabel.setText("Invalid email or password");
        });

        new Thread(task).start();
    }

    private boolean isValidInput(String email, String password) {
        return email != null && !email.isBlank() && !password.isBlank();
    }

    private void onLoginSuccess(UserSession session, String password) {
        AppState.setSession(session);
        AppState.setPassword(password);
        ViewNavigator.navigate("/fxml/upload.fxml");
    }

    @FXML
    private void onGoToRegister() {
        ViewNavigator.navigate("/fxml/register.fxml");
    }
}
