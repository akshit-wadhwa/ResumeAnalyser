package com.resumeanalyser.frontend.controller;

import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ComparisonController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private Label jobTitleLabel;

    @FXML
    private VBox comparisonContainer;

    private List<Map<String, Object>> results;

    public void initialize() {
        // Will be populated via setResults method
    }

    public void setResults(List<Map<String, Object>> results) {
        this.results = results;
        Platform.runLater(this::populateResults);
    }

    private void populateResults() {
        comparisonContainer.getChildren().clear();

        if (results == null || results.isEmpty()) {
            Label emptyLabel = new Label("No comparison results available");
            emptyLabel.getStyleClass().add("hint");
            comparisonContainer.getChildren().add(emptyLabel);
            return;
        }

        // Sort by match score descending
        results.sort((a, b) -> {
            Double scoreA = (Double) a.get("matchScore");
            Double scoreB = (Double) b.get("matchScore");
            return Double.compare(scoreB != null ? scoreB : 0.0, scoreA != null ? scoreA : 0.0);
        });

        int rank = 1;
        for (Map<String, Object> result : results) {
            VBox card = createResumeCard(rank, result);
            comparisonContainer.getChildren().add(card);
            rank++;
        }
    }

    private VBox createResumeCard(int rank, Map<String, Object> result) {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel");
        card.setPadding(new Insets(20));

        // Rank badge and filename
        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label rankLabel = new Label("#" + rank);
        rankLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #2563eb; -fx-min-width: 50;");

        Label filenameLabel = new Label((String) result.get("filename"));
        filenameLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        HBox.setHgrow(filenameLabel, Priority.ALWAYS);

        Double matchScore = (Double) result.get("matchScore");
        String scoreText = matchScore != null ? String.format("%.1f%%", matchScore) : "N/A";
        Label scoreLabel = new Label(scoreText);
        scoreLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + getScoreColor(matchScore) + ";");

        header.getChildren().addAll(rankLabel, filenameLabel, scoreLabel);

        // Score breakdown
        HBox metrics = new HBox(20);
        metrics.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        metrics.getChildren().add(createMetricBox("Match Score", matchScore, "#2563eb"));
        metrics.getChildren().add(createMetricBox("Confidence",
                (Double) result.get("confidence"), "#f59e0b"));

        // Matched skills indicator
        @SuppressWarnings("unchecked")
        List<String> matchedSkills = (List<String>) result.get("matchedSkills");
        int matchedCount = matchedSkills != null ? matchedSkills.size() : 0;

        @SuppressWarnings("unchecked")
        List<String> missingSkills = (List<String>) result.get("missingSkills");
        int missingCount = missingSkills != null ? missingSkills.size() : 0;

        Label skillsLabel = new Label(String.format("✓ %d skills matched  •  ✗ %d missing", matchedCount, missingCount));
        skillsLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #64748b;");

        card.getChildren().addAll(header, metrics, skillsLabel);

        // Add border color based on rank
        String borderColor = switch (rank) {
            case 1 ->
                "#10b981"; // Green for top candidate
            case 2 ->
                "#2563eb"; // Blue for second
            case 3 ->
                "#f59e0b"; // Orange for third
            default ->
                "#cbd5e1"; // Gray for others
        };
        card.setStyle(card.getStyle() + "; -fx-border-color: " + borderColor + "; -fx-border-width: 2;");

        return card;
    }

    private VBox createMetricBox(String label, Double value, String color) {
        VBox box = new VBox(4);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b;");

        HBox valueBox = new HBox(8);
        valueBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        String valueText = value != null ? String.format("%.1f%%", value) : "N/A";
        Label valueLabel = new Label(valueText);
        valueLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        ProgressBar progressBar = new ProgressBar(value != null ? value / 100.0 : 0.0);
        progressBar.setPrefWidth(100);
        progressBar.setPrefHeight(6);

        valueBox.getChildren().addAll(valueLabel, progressBar);
        box.getChildren().addAll(nameLabel, valueBox);

        return box;
    }

    private String getScoreColor(Double score) {
        if (score == null) {
            return "#64748b";
        }
        if (score >= 80) {
            return "#10b981"; // Green

                }if (score >= 60) {
            return "#2563eb"; // Blue

                }if (score >= 40) {
            return "#f59e0b"; // Orange

                }return "#ef4444"; // Red
    }

    @FXML
    private void onBackToDashboard() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/upload.fxml"));
            javafx.scene.Parent root = loader.load();
            rootPane.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
