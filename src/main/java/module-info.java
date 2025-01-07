module com.example.project {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;


    opens com.example.project to javafx.fxml;
    exports com.example.project;

    opens com.example.project.controller to javafx.fxml;
    exports com.example.project.controller;

    exports com.example.project.entity;
}