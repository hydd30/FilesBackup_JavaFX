package com.example.project;

import com.example.project.controller.IndexController;
import com.example.project.entity.Compressor;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import javafx.fxml.JavaFXBuilderFactory;


public class IndexApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("index.fxml"));

        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 900, 600);

        IndexController indexController = fxmlLoader.getController();

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose a directory");
        indexController.setPath(directoryChooser.showDialog(stage).getAbsolutePath());
        indexController.getCurrentPath().setText(indexController.getPath());
        indexController.getInitialize();

        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
