package com.example.project.entity;

import lombok.Data;

@Data
public class FileInfo {
    private String FileName;
    private Boolean isDirectory;
    private String FileType;
    private Integer FileSize;
    private String FileDate;

    public FileInfo() { }

    public FileInfo(String FileName, String FileType, Integer FileSize, String FileDate) {
        this.FileName = FileName;
        this.FileType = FileType;
        this.FileSize = FileSize;
        this.FileDate = FileDate;
    }
}
