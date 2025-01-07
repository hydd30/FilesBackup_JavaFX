package com.example.project.entity;

import com.example.project.utils.FileProcess;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

@Getter
public class FileInfoList {
    private String path;
    private Integer length;
    private List<String> fileNames;
    private List<FileInfo> fileInfos;

    public FileInfoList(String path) throws IOException {
        this.path = path;

        File directory = new File(path);
        this.fileNames = List.of(directory.list());
        this.length = this.fileNames.size();
        this.fileInfos = new ArrayList<>();
        File[] files = directory.listFiles();
        for(File f : files) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(f.getName());
            fileInfo.setIsDirectory(f.isDirectory());
            fileInfo.setFileSize(new Integer((int) f.length()));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            fileInfo.setFileDate(sdf.format(new Date(f.lastModified())));
            if(fileInfo.getIsDirectory()) {
                fileInfo.setFileType("Directory");
            } else {
                fileInfo.setFileType(FileProcess.getFileExtension(f));
            }
            this.fileInfos.add(fileInfo);
        }
    }

    public void setPath(String path) {
        this.path = path;
    }
}
