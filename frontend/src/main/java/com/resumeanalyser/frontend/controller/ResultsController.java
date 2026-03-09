package com.resumeanalyser.frontend.controller;

import java.io.File;
import java.util.List;

import com.resumeanalyser.frontend.model.AnalysisResult;
import com.resumeanalyser.frontend.service.ApiClient;
import com.resumeanalyser.frontend.util.AppState;
import com.resumeanalyser.frontend.util.ViewNavigator;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

@SuppressWarnings("unused")
public class ResultsController {

    @FXML
    private Label matchLabel;
    @FXML
    private Label confidenceLabel;
    @FXML
    private Label seniorityLabel;
    @FXML
    private ProgressBar matchProgress;
    @FXML
    private VBox matchedContainer;
    @FXML
    private VBox missingContainer;
    @FXML
    private VBox recommendationContainer;
    @FXML
    private VBox weaknessContainer;
    @FXML
    private BorderPane rootPane;

    private final ApiClient apiClient = new ApiClient("http://localhost:8081");

    @FXML
    public void initialize() {
        AnalysisResult result = AppState.getLastResult();
        if (result == null) {
            ViewNavigator.navigate("/fxml/upload.fxml");
            return;
        }
        FadeTransition fade = new FadeTransition(Duration.millis(600), rootPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
        matchLabel.setText(String.format("%.1f%%", result.getMatchScore()));
        confidenceLabel.setText(String.format("%.1f%%", result.getConfidenceScore()));
        seniorityLabel.setText(result.getSeniority());

        populateSkillsContainer(matchedContainer, result.getMatchedSkills(), "No specific skills matched", "#10b981");
        populateSkillsContainer(missingContainer, result.getMissingSkills(), "No critical skills missing", "#ef4444");
        populateSkillsContainer(weaknessContainer, result.getWeaknesses(), "No significant weaknesses identified", "#f59e0b");
        populateSkillsContainer(recommendationContainer, result.getRecommendations(), "No recommendations at this time", "#8b5cf6");

        double matchProgressValue = result.getMatchScore() / 100.0;
        animateProgress(matchProgressValue);
    }

    private void populateSkillsContainer(VBox container, List<String> items, String emptyMessage, String accentColor) {
        container.getChildren().clear();

        if (items == null || items.isEmpty()) {
            Label emptyLabel = new Label(emptyMessage);
            emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-padding: 12;");
            container.getChildren().add(emptyLabel);
            return;
        }

        for (String item : items) {
            Label skillLabel = new Label("• " + item);
            skillLabel.setStyle(String.format(
                    "-fx-text-fill: #334155; -fx-padding: 6 8; -fx-background-color: white; "
                    + "-fx-border-radius: 4; -fx-background-radius: 4; "
                    + "-fx-border-width: 0 0 0 3; -fx-border-color: %s; -fx-font-size: 12;",
                    accentColor
            ));
            skillLabel.setWrapText(true);
            container.getChildren().add(skillLabel);
        }
    }

    private void animateProgress(double target) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, event -> matchProgress.setProgress(0)),
                new KeyFrame(Duration.seconds(1.4), event -> matchProgress.setProgress(target))
        );
        timeline.play();
    }

    @FXML
    private void onExportReport() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File target = chooser.showSaveDialog(matchLabel.getScene().getWindow());
        if (target == null) {
            return;
        }
        new Thread(() -> {
            try {
                apiClient.downloadReport(AppState.getLastAnalysisId(), target);
            } catch (Exception ex) {
                // ignore
            }
        }).start();
    }

    @FXML
    private void onBackToUpload() {
        ViewNavigator.navigate("/fxml/upload.fxml");
    }
}
