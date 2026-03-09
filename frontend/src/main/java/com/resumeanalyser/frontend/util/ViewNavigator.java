package com.resumeanalyser.frontend.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ViewNavigator {

    private static Stage stage;

    public static void setStage(Stage primary) {
        stage = primary;
    }

    public static Parent load(String fxml) {
        try {
            return FXMLLoader.load(ViewNavigator.class.getResource(fxml));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load view: " + fxml, ex);
        }
    }

    public static void navigate(String fxml) {
        Parent root = load(fxml);
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1200, 720);
        } else {
            scene.setRoot(root);
        }
        stage.setScene(scene);
    }
}
