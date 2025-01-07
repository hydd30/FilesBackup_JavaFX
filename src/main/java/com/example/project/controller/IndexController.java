package com.example.project.controller;

import com.example.project.entity.BackupRestore;
import com.example.project.entity.FileInfo;
import com.example.project.entity.FileInfoList;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Data
public class IndexController {
    private String path;

    private FileInfoList fileInfoList;

    @FXML
    private Label currentPath;

    @FXML
    private TableView<FileInfo> fileInfoTable = new TableView<FileInfo>();

//    @FXML
//    private TableColumn<FileInfo, Object> iconColumn = new TableColumn<FileInfo, Object>();

    @FXML
    private TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>();

    @FXML
    private TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();

    @FXML
    private TableColumn<FileInfo, Integer> fileSizeColumn = new TableColumn<>();

    @FXML
    private TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>();

    @FXML
    private TextField searchPath;

    public void initialize() throws IOException {
    }

    public void getInitialize() throws IOException {
        this.fileInfoList = new FileInfoList(this.path);
        this.searchPath.setText(this.path);
        this.fileInfoTable.setPlaceholder(new Label("No Files"));
        this.fileInfoTable.setItems(FXCollections.observableList(this.fileInfoList.getFileInfos()));
        this.fileNameColumn.setCellValueFactory(new PropertyValueFactory<FileInfo, String>("FileName"));
        this.fileTypeColumn.setCellValueFactory(new PropertyValueFactory<FileInfo, String>("FileType"));
        this.fileSizeColumn.setCellValueFactory(new PropertyValueFactory<FileInfo, Integer>("FileSize"));
        this.fileDateColumn.setCellValueFactory(new PropertyValueFactory<FileInfo, String>("FileDate"));

        this.fileInfoTable.setRowFactory(tv -> {
            TableRow<FileInfo> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2 && !row.isEmpty()) {
                    FileInfo rowData = row.getItem();
                    if (rowData.getIsDirectory()) {
                        this.path = this.path + "\\" + rowData.getFileName();
                        try {
                            this.refreshTable();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                if (event.getButton().equals(MouseButton.SECONDARY)) {
                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem backupMenu = new MenuItem("Backup Current Directory");
                    // 备份当前文件夹
                    backupMenu.setOnAction(e -> {
                        showFileFilterDialog();
                    });
                    contextMenu.getItems().add(backupMenu);
                    FileInfo rowItem = row.getItem();
                    if (rowItem.getIsDirectory()) {
                        if (row.getItem().getFileName().substring(rowItem.getFileName().lastIndexOf(".") + 1).equals("bak")) {
                            MenuItem restore = new MenuItem("Restore");   // Restore the directory
                            restore.setOnAction(e -> {
                                DirectoryChooser directoryChooser = new DirectoryChooser();
                                directoryChooser.setTitle("Choose a directory to store backup");
                                this.path = directoryChooser.showDialog(null).getAbsolutePath();
                                String restorePath = this.path + "/" + row.getItem().getFileName().substring(0, row.getItem().getFileName().length() - 4);

                                FileInfo rowData = row.getItem();
                                String filePath = this.currentPath.getText() + "/" + rowData.getFileName();
                                // Restore the file
                                BackupRestore backupRestore = new BackupRestore();
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                // input the password
                                TextInputDialog dialog = new TextInputDialog();
                                dialog.setTitle("Password");
                                dialog.setHeaderText("Please input the password");
                                dialog.setContentText("Password:");
                                dialog.showAndWait().ifPresent(password -> {
                                    backupRestore.setPassword(password);
                                    try {
                                        backupRestore.restoreDirectory(Path.of(filePath), Path.of(restorePath));
                                        alert.setTitle("Success");
                                        alert.setHeaderText("Restore Success");
                                        alert.setContentText("Restore Success");
                                        alert.showAndWait();
                                    } catch (IOException ex) {
                                        alert.setTitle("Error");
                                        alert.setHeaderText("Restore Error");
                                        alert.setContentText("Restore Error");
                                        alert.showAndWait();
                                    }
                                });
                            });
                            contextMenu.getItems().addAll(restore);
                        } else {
                            MenuItem backup = new MenuItem("Backup");   // Backup the directory
                            backup.setOnAction(e -> {
                                DirectoryChooser directoryChooser = new DirectoryChooser();
                                directoryChooser.setTitle("Choose a directory to store backup");
                                String restorePath = directoryChooser.showDialog(null).getAbsolutePath() + "/" +
                                        row.getItem().getFileName().substring(0, row.getItem().getFileName().length() - 4);

                                FileInfo rowData = row.getItem();
                                String filePath = this.currentPath.getText() + "/" + rowData.getFileName();
                                // Restore the file
                                BackupRestore backupRestore = new BackupRestore();
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                // input the password
                                TextInputDialog dialog = new TextInputDialog();
                                dialog.setTitle("Password");
                                dialog.setHeaderText("Please input the password");
                                dialog.setContentText("Password:");
                                dialog.showAndWait().ifPresent(password -> {
                                    backupRestore.setPassword(password);
                                    try {
                                        backupRestore.restoreSingleFile(Path.of(filePath), Path.of(restorePath));
                                        alert.setTitle("Success");
                                        alert.setHeaderText("Restore Success");
                                        alert.setContentText("Restore Success");
                                        alert.showAndWait();
                                    } catch (IOException ex) {
                                        alert.setTitle("Error");
                                        alert.setHeaderText("Restore Error");
                                        alert.setContentText("Restore Error");
                                        alert.showAndWait();
                                    }
                                });
                            });
                            contextMenu.getItems().addAll(backup);
                        }
                    } else if (row.getItem().getFileType().equals("bak")) {
                        MenuItem restore = new MenuItem("Restore");   // Restore the file
                        restore.setOnAction(e -> {
                            DirectoryChooser directoryChooser = new DirectoryChooser();
                            directoryChooser.setTitle("Choose a directory to store backup");
                            this.path = directoryChooser.showDialog(null).getAbsolutePath();
                            String restorePath = this.path + "/" + row.getItem().getFileName().substring(0, row.getItem().getFileName().length() - 4);

                            FileInfo rowData = row.getItem();
                            String filePath = this.currentPath.getText() + "/" + rowData.getFileName();
                            // Restore the file
                            BackupRestore backupRestore = new BackupRestore();
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            // input the password
                            TextInputDialog dialog = new TextInputDialog();
                            dialog.setTitle("Password");
                            dialog.setHeaderText("Please input the password");
                            dialog.setContentText("Password:");
                            dialog.showAndWait().ifPresent(password -> {
                                backupRestore.setPassword(password);
                                try {
                                    backupRestore.restoreSingleFile(Path.of(filePath), Path.of(restorePath));
                                    alert.setTitle("Success");
                                    alert.setHeaderText("Restore Success");
                                    alert.setContentText("Restore Success");
                                    alert.showAndWait();
                                    this.refreshTable();
                                } catch (IOException ex) {
                                    alert.setTitle("Error");
                                    alert.setHeaderText("Restore Error");
                                    alert.setContentText("Restore Error");
                                    alert.showAndWait();
                                }
                            });
                        });
                        contextMenu.getItems().addAll(restore);
                    } else {
                        MenuItem backup = new MenuItem("Backup");   // Backup the file
                        backup.setOnAction(e -> {
                            DirectoryChooser directoryChooser = new DirectoryChooser();
                            directoryChooser.setTitle("Choose a directory to store backup");
                            this.path = directoryChooser.showDialog(null).getAbsolutePath();
                            String backupPath = this.path + "/" + row.getItem().getFileName() + ".bak";
                            FileInfo rowData = row.getItem();
                            if (rowData.getFileType().equals("Directory")) {
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Error");
                                alert.setHeaderText("Invalid File");
                                alert.setContentText("Cannot backup a directory.");
                                alert.showAndWait();
                            } else {
                                String filePath = this.currentPath.getText() + "/" + rowData.getFileName();
                                // Backup the file
                                BackupRestore backupRestore = new BackupRestore();
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                // input the password
                                TextInputDialog dialog = new TextInputDialog();
                                dialog.setTitle("Password");
                                dialog.setHeaderText("Please input the password");
                                dialog.setContentText("Password:");
                                dialog.showAndWait().ifPresent(password -> {
                                    backupRestore.setPassword(password);
                                    try {
                                        backupRestore.backupSingleFile(Path.of(filePath), Path.of(backupPath));
                                        alert.setTitle("Success");
                                        alert.setHeaderText("Backup Success");
                                        alert.setContentText("Backup Success");
                                        alert.showAndWait();
                                        this.refreshTable();
                                    } catch (IOException ex) {
                                        alert.setTitle("Error");
                                        alert.setHeaderText("Backup Error");
                                        alert.setContentText("Backup Error");
                                        alert.showAndWait();
                                    }
                                });

                            }
                        });
                        contextMenu.getItems().addAll(backup);
                    }
                    row.setContextMenu(contextMenu);
                    try {
                        this.refreshTable();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return row;
        });
    }

    public void refreshTable() throws IOException {
        this.fileInfoList = new FileInfoList(this.path);
        this.currentPath.setText(this.path);
        this.searchPath.setText(this.path);
        this.fileInfoTable.setItems(FXCollections.observableList(this.fileInfoList.getFileInfos()));
    }

    @FXML
    public void handlePathLabelClick() {
        this.currentPath.setVisible(false);
        this.searchPath.setVisible(true);
    }

    @FXML
    public void handleBackgroundClick() {
        if (this.searchPath.isVisible()) {
            this.currentPath.setVisible(true);
            this.searchPath.setVisible(false);
            this.searchPath.setText(this.path);
        }
    }

    @FXML
    public void handleSearchEntered(KeyEvent keyEvent) throws IOException {
        try {
            if (keyEvent.getCode().toString().equals("ENTER")) {
                this.path = this.searchPath.getText();
                this.currentPath.setText(this.path);
                this.searchPath.setVisible(false);
                this.currentPath.setVisible(true);
                this.getInitialize();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Invalid Path");
            alert.setContentText("Please enter a valid path.");
            alert.showAndWait();
        }
    }

    public void handlePreviousDirectory(MouseEvent mouseEvent) {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            if (this.path.equals("C:\\") || this.path.equals("D:\\") || this.path.equals("E:\\") || this.path.equals("F:\\")) {
                return;
            } else {
                this.path = this.path.substring(0, this.path.lastIndexOf("\\"));
            }
        } else {
            if (this.path.equals("/")) {
                return;
            } else {
                this.path = this.path.substring(0, this.path.lastIndexOf("/"));
            }
        }
        try {
            this.refreshTable();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 在IndexController类中添加新的方法
// 用于存储所有文件选择框的引用
    private Map<String, List<CheckBox>> fileCheckboxes = new HashMap<>();

    private void showFileFilterDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Select Files to Backup");

        // 创建TabPane用于切换不同的筛选视图
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // 获取当前目录文件列表
        FileInfoList currentFiles;
        try {
            currentFiles = new FileInfoList(this.path);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        // 按类型筛选的Tab
        Tab typeTab = new Tab("By Type");
        VBox typeContent = createTypeFilterContent(currentFiles);
        typeTab.setContent(new ScrollPane(typeContent));

        // 按大小筛选的Tab
        Tab sizeTab = new Tab("By Size");
        VBox sizeContent = createSizeFilterContent(currentFiles);
        sizeTab.setContent(new ScrollPane(sizeContent));

        // 按时间筛选的Tab
        Tab dateTab = new Tab("By Date");
        VBox dateContent = createDateFilterContent(currentFiles);
        dateTab.setContent(new ScrollPane(dateContent));

        // 按名称筛选的Tab
        Tab nameTab = new Tab("By Name");
        VBox nameContent = createNameFilterContent(currentFiles);
        nameTab.setContent(new ScrollPane(nameContent));

        tabPane.getTabs().addAll(typeTab, sizeTab, dateTab, nameTab);

        dialog.getDialogPane().setContent(tabPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 处理备份操作
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Choose backup location");
                String backupPath = directoryChooser.showDialog(null).getAbsolutePath();

                BackupRestore backupRestore = new BackupRestore();

                TextInputDialog passwordDialog = new TextInputDialog();
                passwordDialog.setTitle("Password");
                passwordDialog.setHeaderText("Enter backup password");

                passwordDialog.showAndWait().ifPresent(password -> {
                    backupRestore.setPassword(password);

                    // 收集所有选中的文件
                    List<String> selectedFiles = collectSelectedFiles(fileCheckboxes);

                    try {
                        backupRestore.backupDirectory(this.currentPath.getText(), backupPath, selectedFiles);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }
        });
    }

    // 创建按类型筛选的内容
    private VBox createTypeFilterContent(FileInfoList currentFiles) {
        // 清除之前的类型筛选复选框
        fileCheckboxes.put("type", new ArrayList<>());

        VBox content = new VBox(10);
        CheckBox selectAll = new CheckBox("Select All");

        Map<String, List<CheckBox>> typeGroups = new HashMap<>();
        Map<String, CheckBox> groupHeaders = new HashMap<>();

        for (FileInfo file : currentFiles.getFileInfos()) {
            String type = file.getFileType();
            if (!typeGroups.containsKey(type)) {
                typeGroups.put(type, new ArrayList<>());
                CheckBox typeHeader = new CheckBox(type);
                groupHeaders.put(type, typeHeader);

                typeHeader.setOnAction(event -> {
                    typeGroups.get(type).forEach(check ->
                            check.setSelected(typeHeader.isSelected())
                    );
                });
            }

            CheckBox fileCheck = new CheckBox(file.getFileName());
            typeGroups.get(type).add(fileCheck);
        }

        selectAll.setOnAction(event -> {
            boolean selected = selectAll.isSelected();
            groupHeaders.values().forEach(header -> header.setSelected(selected));
            typeGroups.values().forEach(group ->
                    group.forEach(check -> check.setSelected(selected))
            );
        });

        content.getChildren().add(selectAll);
        groupHeaders.forEach((type, header) -> {
            VBox groupBox = new VBox(5);
            groupBox.getChildren().add(header);
            typeGroups.get(type).forEach(check ->
                    groupBox.getChildren().add(new HBox(20, new Region(), check))
            );
            content.getChildren().add(groupBox);
        });

        return content;
    }

    // 创建按大小筛选的内容
    private VBox createSizeFilterContent(FileInfoList currentFiles) {
        VBox content = new VBox(10);

        // 添加大小范围选择
        ComboBox<String> sizeRangeCombo = new ComboBox<>();
        sizeRangeCombo.getItems().addAll(
                "All Sizes",
                "< 1MB",
                "1MB - 10MB",
                "10MB - 100MB",
                "100MB - 1GB",
                "> 1GB"
        );
        sizeRangeCombo.setValue("All Sizes");

        VBox filesBox = new VBox(5);
        CheckBox selectAll = new CheckBox("Select All");

        List<CheckBox> fileCheckboxes = new ArrayList<>();
        for (FileInfo file : currentFiles.getFileInfos()) {
            CheckBox fileCheck = new CheckBox(file.getFileName() + " (" + formatFileSize(file.getFileSize()) + ")");
            fileCheckboxes.add(fileCheck);
        }

        selectAll.setOnAction(event -> {
            boolean selected = selectAll.isSelected();
            fileCheckboxes.forEach(check -> check.setSelected(selected));
        });

        sizeRangeCombo.setOnAction(event -> {
            updateSizeFilteredFiles(sizeRangeCombo.getValue(), fileCheckboxes, currentFiles);
        });

        content.getChildren().addAll(
                new HBox(10, new Label("Size Range:"), sizeRangeCombo),
                selectAll,
                filesBox
        );
        fileCheckboxes.forEach(check -> filesBox.getChildren().add(check));

        return content;
    }

    // 创建按日期筛选的内容
    private VBox createDateFilterContent(FileInfoList currentFiles) {
        VBox content = new VBox(10);

        DatePicker startDate = new DatePicker();
        DatePicker endDate = new DatePicker();

        VBox filesBox = new VBox(5);
        CheckBox selectAll = new CheckBox("Select All");

        List<CheckBox> fileCheckboxes = new ArrayList<>();
        for (FileInfo file : currentFiles.getFileInfos()) {
            CheckBox fileCheck = new CheckBox(file.getFileName() + " (" + file.getFileDate() + ")");
            fileCheckboxes.add(fileCheck);
        }

        selectAll.setOnAction(event -> {
            boolean selected = selectAll.isSelected();
            fileCheckboxes.forEach(check -> check.setSelected(selected));
        });

        content.getChildren().addAll(
                new HBox(10, new Label("Start Date:"), startDate),
                new HBox(10, new Label("End Date:"), endDate),
                selectAll,
                filesBox
        );
        fileCheckboxes.forEach(check -> filesBox.getChildren().add(check));

        return content;
    }

    // 创建按名称筛选的内容
    private VBox createNameFilterContent(FileInfoList currentFiles) {
        VBox content = new VBox(10);

        TextField searchField = new TextField();
        searchField.setPromptText("Search by filename...");

        VBox filesBox = new VBox(5);
        CheckBox selectAll = new CheckBox("Select All");

        List<CheckBox> fileCheckboxes = new ArrayList<>();
        for (FileInfo file : currentFiles.getFileInfos()) {
            CheckBox fileCheck = new CheckBox(file.getFileName());
            fileCheckboxes.add(fileCheck);
        }

        selectAll.setOnAction(event -> {
            boolean selected = selectAll.isSelected();
            fileCheckboxes.forEach(check -> check.setSelected(selected));
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateNameFilteredFiles(newValue, fileCheckboxes, currentFiles);
        });

        content.getChildren().addAll(
                searchField,
                selectAll,
                filesBox
        );
        fileCheckboxes.forEach(check -> filesBox.getChildren().add(check));

        return content;
    }

    // 辅助方法：格式化文件大小
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    // 根据大小范围更新文件显示
    private void updateSizeFilteredFiles(String range, List<CheckBox> checkboxes, FileInfoList files) {
        for (int i = 0; i < checkboxes.size(); i++) {
            FileInfo file = files.getFileInfos().get(i);
            CheckBox checkbox = checkboxes.get(i);

            long size = file.getFileSize();
            boolean visible = true;

            switch (range) {
                case "< 1MB":
                    visible = size < 1024 * 1024;
                    break;
                case "1MB - 10MB":
                    visible = size >= 1024 * 1024 && size < 10 * 1024 * 1024;
                    break;
                case "10MB - 100MB":
                    visible = size >= 10 * 1024 * 1024 && size < 100 * 1024 * 1024;
                    break;
                case "100MB - 1GB":
                    visible = size >= 100 * 1024 * 1024 && size < 1024 * 1024 * 1024;
                    break;
                case "> 1GB":
                    visible = size >= 1024 * 1024 * 1024;
                    break;
            }

            checkbox.setVisible(range.equals("All Sizes") || visible);
            checkbox.setManaged(checkbox.isVisible());
        }
    }

    // 根据文件名筛选更新显示
    private void updateNameFilteredFiles(String searchText, List<CheckBox> checkboxes, FileInfoList files) {
        for (int i = 0; i < checkboxes.size(); i++) {
            FileInfo file = files.getFileInfos().get(i);
            CheckBox checkbox = checkboxes.get(i);

            boolean visible = searchText.isEmpty() ||
                    file.getFileName().toLowerCase().contains(searchText.toLowerCase());

            checkbox.setVisible(visible);
            checkbox.setManaged(checkbox.isVisible());
        }
    }

    // 收集所有选中的文件
    private List<String> collectSelectedFiles(Map<String, List<CheckBox>> fileCheckboxes) {
        List<String> selectedFiles = new ArrayList<>();

        // 遍历所有标签页的选择框
        for (List<CheckBox> checkBoxes : fileCheckboxes.values()) {
            for (CheckBox checkbox : checkBoxes) {
                if (checkbox.isSelected() && checkbox.isVisible()) {
                    // 从CheckBox的文本中提取文件名（移除可能的大小和日期信息）
                    String fileName = checkbox.getText().split(" \\(")[0];
                    String filePath = this.path + "/" + fileName;
                    selectedFiles.add(filePath);
                }
            }
        }

        // 去重
        return new ArrayList<>(new LinkedHashSet<>(selectedFiles));
    }
}
