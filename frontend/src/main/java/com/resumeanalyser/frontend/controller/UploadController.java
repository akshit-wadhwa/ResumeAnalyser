package com.resumeanalyser.frontend.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.resumeanalyser.frontend.model.RankedResume;
import com.resumeanalyser.frontend.service.ApiClient;
import com.resumeanalyser.frontend.util.AppState;
import com.resumeanalyser.frontend.util.ViewNavigator;

import javafx.animation.FadeTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;

@SuppressWarnings("unused")
public class UploadController {

    @FXML
    private Label resumeLabel;
    @FXML
    private Label jobLabel;
    @FXML
    private TextArea jobTextArea;
    @FXML
    private Label statusLabel;
    @FXML
    private ListView<String> compareList;
    @FXML
    private StackPane dropZone;
    @FXML
    private BorderPane rootPane;

    private File resumeFile;
    private File jobFile;
    private final List<File> compareFiles = new ArrayList<>();
    private final ApiClient api = new ApiClient("http://localhost:8081");

    @FXML
    public void initialize() {
        statusLabel.setText("");
        resumeLabel.setText("No resume selected");
        jobLabel.setText("No job file selected");
        setupDragAndDrop();
        FadeTransition fade = new FadeTransition(Duration.millis(600), rootPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    @FXML
    private void onSelectResume() {
        resumeFile = chooseFile("Resume Files", "*.pdf", "*.docx", resumeLabel);
        if (resumeFile != null) {
            resumeLabel.setText(resumeFile.getName());
        }
    }

    @FXML
    private void onSelectJobFile() {
        jobFile = chooseFile("Job Files", "*.pdf", "*.docx", jobLabel);
        if (jobFile != null) {
            jobLabel.setText(jobFile.getName());
        }
    }

    @FXML
    private void onAnalyze() {
        if (resumeFile == null) {
            statusLabel.setText("Select a resume file");
            return;
        }
        if (jobFile == null && (jobTextArea.getText() == null || jobTextArea.getText().isBlank())) {
            statusLabel.setText("Add a job description or job file");
            return;
        }

        String jobText = jobTextArea.getText();
        AppState.setJobText(jobText);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return api.startAnalysis(
                        resumeFile,
                        jobFile,
                        jobText,
                        AppState.getSession().getEmail(),
                        AppState.getPassword());
            }
        };

        task.setOnSucceeded(event -> {
            String analysisId = task.getValue();
            if (analysisId == null || analysisId.isBlank() || "null".equalsIgnoreCase(analysisId)) {
                statusLabel.setText("Analysis request failed");
                return;
            }
            AppState.setLastAnalysisId(analysisId);
            ViewNavigator.navigate("/fxml/analyzing.fxml");
        });

        task.setOnFailed(event -> statusLabel.setText("Analysis request failed"));
        new Thread(task).start();
    }

    @FXML
    private void onAddComparisonResume() {
        File file = chooseFile("Resume Files", "*.pdf", "*.docx", compareList);
        if (file != null) {
            compareFiles.add(file);
            compareList.getItems().add(file.getName());
        }
    }

    @FXML
    private void onRankResumes() {
        if (compareFiles.isEmpty()) {
            statusLabel.setText("Add resumes to compare");
            return;
        }
        if (jobTextArea.getText() == null || jobTextArea.getText().isBlank()) {
            statusLabel.setText("Enter job description text");
            return;
        }

        String jobText = jobTextArea.getText();
        Task<List<RankedResume>> task = new Task<>() {
            @Override
            protected List<RankedResume> call() throws Exception {
                return api.rankResumes(compareFiles, jobText, AppState.getSession().getEmail(), AppState.getPassword());
            }
        };

        task.setOnSucceeded(event -> {
            List<RankedResume> results = task.getValue();
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/fxml/comparison.fxml"));
                javafx.scene.Parent root = loader.load();

                ComparisonController controller = loader.getController();
                controller.setResults(toComparisonRows(results));

                rootPane.getScene().setRoot(root);
            } catch (java.io.IOException ex) {
                statusLabel.setText("Failed to load comparison screen");
            }
        });

        task.setOnFailed(event -> statusLabel.setText("Ranking failed"));
        new Thread(task).start();
    }

    @FXML
    private void onAdminDashboard() {
        ViewNavigator.navigate("/fxml/admin.fxml");
    }

    @FXML
    private void onViewHistory() {
        ViewNavigator.navigate("/fxml/history.fxml");
    }

    @FXML
    private void onLogout() {
        AppState.setSession(null);
        AppState.setPassword(null);
        ViewNavigator.navigate("/fxml/login.fxml");
    }

    private void setupDragAndDrop() {
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                resumeFile = file;
                resumeLabel.setText(file.getName());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private File chooseFile(String description, String ext1, String ext2, javafx.scene.Node ownerNode) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, ext1, ext2));
        return chooser.showOpenDialog(ownerNode.getScene().getWindow());
    }

    private List<java.util.Map<String, Object>> toComparisonRows(List<RankedResume> results) {
        List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        for (RankedResume resume : results) {
            java.util.Map<String, Object> row = new java.util.HashMap<>();
            row.put("filename", resume.getFilename());
            row.put("matchScore", resume.getMatchScore());
            row.put("confidence", resume.getConfidenceScore());
            row.put("matchedSkills", resume.getMatchedSkills());
            row.put("missingSkills", resume.getMissingSkills());
            rows.add(row);
        }
        return rows;
    }
}
