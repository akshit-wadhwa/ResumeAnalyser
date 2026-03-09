package com.resumeanalyser.frontend.controller;

import com.resumeanalyser.frontend.model.AnalysisResult;
import com.resumeanalyser.frontend.service.ApiClient;
import com.resumeanalyser.frontend.util.AppState;
import com.resumeanalyser.frontend.util.ViewNavigator;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.util.Duration;

public class AnalyzingController {

    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label statusLabel;

    private final ApiClient apiClient = new ApiClient("http://localhost:8081");
    private Timeline poller;

    @FXML
    public void initialize() {
        statusLabel.setText("Analyzing resume and job description...");
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        poller = new Timeline(new KeyFrame(Duration.seconds(2), event -> pollStatus()));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();
    }

    private void pollStatus() {
        String analysisId = AppState.getLastAnalysisId();
        if (analysisId == null || analysisId.isBlank() || "null".equalsIgnoreCase(analysisId)) {
            statusLabel.setText("Missing analysis id. Please retry.");
            poller.stop();
            return;
        }
        Task<AnalysisResult> task = new Task<>() {
            @Override
            protected AnalysisResult call() throws Exception {
                return apiClient.pollStatus(analysisId);
            }
        };

        task.setOnSucceeded(event -> {
            if (task.getValue() != null) {
                AppState.setLastResult(task.getValue());
                poller.stop();
                ViewNavigator.navigate("/fxml/results.fxml");
            }
        });

        task.setOnFailed(event -> {
            String error = task.getException() != null ? task.getException().getMessage() : "Analysis failed";
            statusLabel.setText(error);
            poller.stop();
        });

        new Thread(task).start();
    }
}
