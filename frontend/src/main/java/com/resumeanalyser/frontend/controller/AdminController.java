package com.resumeanalyser.frontend.controller;

import com.resumeanalyser.frontend.model.AdminSummary;
import com.resumeanalyser.frontend.service.ApiClient;
import com.resumeanalyser.frontend.util.ViewNavigator;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

@SuppressWarnings("unused")
public class AdminController {

    @FXML
    private Label usersLabel;
    @FXML
    private Label analysesLabel;
    @FXML
    private Label avgLabel;

    private final ApiClient api = new ApiClient("http://localhost:8081");

    @FXML
    public void initialize() {
        Task<AdminSummary> task = new Task<>() {
            @Override
            protected AdminSummary call() throws Exception {
                return api.fetchAdminSummary();
            }
        };

        task.setOnSucceeded(event -> {
            showSummary(task.getValue());
        });

        new Thread(task).start();
    }

    @FXML
    private void onBack() {
        ViewNavigator.navigate("/fxml/upload.fxml");
    }

    private void showSummary(AdminSummary summary) {
        usersLabel.setText(String.valueOf(summary.getTotalUsers()));
        analysesLabel.setText(String.valueOf(summary.getTotalAnalyses()));
        avgLabel.setText(String.format("%.1f%%", summary.getAverageMatchScore()));
    }
}
