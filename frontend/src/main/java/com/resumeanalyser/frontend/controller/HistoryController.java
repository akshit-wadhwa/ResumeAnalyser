package com.resumeanalyser.frontend.controller;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.resumeanalyser.frontend.model.AnalysisResult;
import com.resumeanalyser.frontend.service.ApiClient;
import com.resumeanalyser.frontend.util.AppState;
import com.resumeanalyser.frontend.util.ViewNavigator;

import javafx.animation.FadeTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

@SuppressWarnings("unused")
public class HistoryController {

    @FXML
    private TableView<Map<String, Object>> historyTable;
    @FXML
    private TableColumn<Map<String, Object>, String> scoreColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> confidenceColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> matchedColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> missingColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> dateColumn;
    @FXML
    private Label infoLabel;
    @FXML
    private Label countLabel;
    @FXML
    private BorderPane rootPane;

    private final ApiClient apiClient = new ApiClient("http://localhost:8081");

    @FXML
    public void initialize() {
        FadeTransition fade = new FadeTransition(Duration.millis(600), rootPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        loadHistory();
    }

    private void loadHistory() {
        if (AppState.getSession() == null || AppState.getPassword() == null) {
            infoLabel.setText("Please login first");
            return;
        }

        Task<List<AnalysisResult>> task = new Task<>() {
            @Override
            protected List<AnalysisResult> call() throws Exception {
                return apiClient.getHistory(AppState.getSession().getEmail(), AppState.getPassword());
            }
        };

        task.setOnSucceeded(event -> {
            List<AnalysisResult> history = task.getValue();
            if (history == null || history.isEmpty()) {
                infoLabel.setText("No analyses yet. Start by uploading a resume!");
                countLabel.setText("Total: 0 analyses");
            } else {
                infoLabel.setText("");
                countLabel.setText("Total: " + history.size() + " analyses");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

                for (AnalysisResult item : history) {
                    String matched = item.getMatchedSkills() != null && !item.getMatchedSkills().isEmpty()
                            ? String.join(", ", item.getMatchedSkills().subList(0, Math.min(3, item.getMatchedSkills().size())))
                            : "None";
                    String missing = item.getMissingSkills() != null && !item.getMissingSkills().isEmpty()
                            ? String.join(", ", item.getMissingSkills().subList(0, Math.min(3, item.getMissingSkills().size())))
                            : "None";
                    String dateStr = item.getCreatedAt() != null
                            ? item.getCreatedAt().format(formatter)
                            : "Unknown";

                    historyTable.getItems().add(Map.of(
                            "score", String.format("%.1f%%", item.getMatchScore()),
                            "confidence", String.format("%.1f%%", item.getConfidenceScore()),
                            "matched", matched,
                            "missing", missing,
                            "date", dateStr
                    ));
                }

                scoreColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("score")));
                confidenceColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("confidence")));
                matchedColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("matched")));
                missingColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("missing")));
                dateColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("date")));
            }
        });

        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            String errorMsg = "Failed to load history";
            if (exception != null) {
                errorMsg += ": " + exception.getMessage();
                exception.printStackTrace();
            }
            infoLabel.setText(errorMsg);
        });

        new Thread(task).start();
    }

    @FXML
    private void onRefresh() {
        historyTable.getItems().clear();
        loadHistory();
    }

    @FXML
    private void onBack() {
        ViewNavigator.navigate("/fxml/upload.fxml");
    }
}
