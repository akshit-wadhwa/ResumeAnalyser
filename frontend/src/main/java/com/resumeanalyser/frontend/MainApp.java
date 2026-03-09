package com.resumeanalyser.frontend;

import com.resumeanalyser.frontend.util.ViewNavigator;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        ViewNavigator.setStage(stage);
        Scene scene = new Scene(ViewNavigator.load("/fxml/login.fxml"), 1200, 720);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setTitle("Resume Analyzer");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
