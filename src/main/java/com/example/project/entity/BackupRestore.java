package com.example.project.entity;

import lombok.Setter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

@Setter
public class BackupRestore {
    private Crypto crypto;
    private String password;
    private FileFilter filter;

    public BackupRestore() {
        this.crypto = new Crypto();
        this.filter = new FileFilter();
    }

    public void createDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    public byte[] readFile(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    public void writeFile(Path path, byte[] data) throws IOException {
        Files.write(path, data);
    }

    public void backupSingleFile(Path sourcePath, Path backupPath) throws IOException {
        try {
            if (!Files.exists(sourcePath)) {
                throw new IOException("源文件不存在");
            }
            if (!Files.isRegularFile(sourcePath)) {
                throw new IOException("源路径不是一个常规文件");
            }

            // createDirectory(backupPath.getParent());

            // 读取源文件
            byte[] data = readFile(sourcePath);

            // 压缩
            byte[] compressed = Compressor.compress(data);

            // 加密
            byte[] encrypted = crypto.encrypt(compressed, password);

            // 写入备份文件
            writeFile(backupPath, encrypted);

            // 保存元数据
            Map<String, Object> metadata = new HashMap<>();

            // 基本文件属性在所有平台都支持
            BasicFileAttributes basicAttrs = Files.readAttributes(sourcePath, BasicFileAttributes.class);
            metadata.put("lastAccessTime", basicAttrs.lastAccessTime().toMillis());
            metadata.put("lastModifiedTime", basicAttrs.lastModifiedTime().toMillis());
            metadata.put("creationTime", basicAttrs.creationTime().toMillis());

            try {
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    // Windows特定属性
                    DosFileAttributes dosAttrs = Files.readAttributes(sourcePath, DosFileAttributes.class);
                    metadata.put("isHidden", dosAttrs.isHidden());
                    metadata.put("isReadOnly", dosAttrs.isReadOnly());
                    metadata.put("isSystem", dosAttrs.isSystem());
                    metadata.put("isArchive", dosAttrs.isArchive());
                } else {
                    // Unix/Linux/MacOS特定属性
                    PosixFileAttributes posixAttrs = Files.readAttributes(sourcePath, PosixFileAttributes.class);
                    metadata.put("owner", posixAttrs.owner().getName());
                    metadata.put("group", posixAttrs.group().getName());
                    metadata.put("permissions", posixAttrs.permissions());
                }
            } catch (UnsupportedOperationException e) {
                // 如果某些属性不支持，仅记录基本属性
                System.out.println("警告：某些文件属性在当前系统不受支持");
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    Files.newOutputStream(Paths.get(backupPath + ".meta")))) {
                oos.writeObject(metadata);
            }

            System.out.println("备份文件成功: " + sourcePath + " -> " + backupPath);
        } catch (Exception e) {
            System.err.println("备份文件失败: " + sourcePath + " - " + e.getMessage());
            throw new IOException(e);
        }
    }

    public void restoreSingleFile(Path backupPath, Path restorePath) throws IOException {
        try {
            if (!Files.exists(backupPath)) {
                throw new IOException("备份文件不存在");
            }
            if (!Files.isRegularFile(backupPath)) {
                throw new IOException("备份路径不是一个常规文件");
            }

            // createDirectory(restorePath.getParent());

            // 读取加密的备份文件
            byte[] encrypted = readFile(backupPath);

            // 解密
            byte[] compressed = crypto.decrypt(encrypted, password);

            // 解压
            byte[] data = Compressor.decompress(compressed);

            // 写入还原文件
            writeFile(restorePath, data);

            // 还原元数据
            Path metaPath = Paths.get(backupPath + ".meta");
            if (Files.exists(metaPath)) {
                try (ObjectInputStream ois = new ObjectInputStream(
                        Files.newInputStream(metaPath))) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadata = (Map<String, Object>) ois.readObject();

                    // 还原基本属性
                    if (metadata.containsKey("lastAccessTime")) {
                        Files.setAttribute(restorePath, "lastAccessTime",
                                FileTime.fromMillis((Long) metadata.get("lastAccessTime")));
                    }
                    if (metadata.containsKey("lastModifiedTime")) {
                        Files.setAttribute(restorePath, "lastModifiedTime",
                                FileTime.fromMillis((Long) metadata.get("lastModifiedTime")));
                    }

                    try {
                        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                            // 还原Windows特定属性
                            if (metadata.containsKey("isHidden")) {
                                Files.setAttribute(restorePath, "dos:hidden", metadata.get("isHidden"));
                            }
                            if (metadata.containsKey("isReadOnly")) {
                                Files.setAttribute(restorePath, "dos:readonly", metadata.get("isReadOnly"));
                            }
                            if (metadata.containsKey("isSystem")) {
                                Files.setAttribute(restorePath, "dos:system", metadata.get("isSystem"));
                            }
                            if (metadata.containsKey("isArchive")) {
                                Files.setAttribute(restorePath, "dos:archive", metadata.get("isArchive"));
                            }
                        } else {
                            // 还原POSIX属性
                            if (metadata.containsKey("permissions")) {
                                Files.setPosixFilePermissions(restorePath,
                                        (Set<PosixFilePermission>) metadata.get("permissions"));
                            }
                            if (metadata.containsKey("owner")) {
                                UserPrincipalLookupService lookupService =
                                        FileSystems.getDefault().getUserPrincipalLookupService();
                                Files.setOwner(restorePath,
                                        lookupService.lookupPrincipalByName((String) metadata.get("owner")));
                            }
                            if (metadata.containsKey("group")) {
                                UserPrincipalLookupService lookupService =
                                        FileSystems.getDefault().getUserPrincipalLookupService();
                                Files.getFileAttributeView(restorePath, PosixFileAttributeView.class)
                                        .setGroup(lookupService.lookupPrincipalByGroupName(
                                                (String) metadata.get("group")));
                            }
                        }
                    } catch (UnsupportedOperationException e) {
                        System.out.println("警告：某些文件属性在当前系统不受支持");
                    }
                }
            }

            System.out.println("还原文件成功: " + backupPath + " -> " + restorePath);
        } catch (Exception e) {
            System.err.println("还原文件失败: " + backupPath + " - " + e.getMessage());
            throw new IOException(e);
        }
    }

    public void backupDirectory(String sourcePathStr, String backupPath, List<String> files) throws IOException {
        Path sourceDir = Paths.get(sourcePathStr);
        Path backupDir = Paths.get(backupPath);

        if (!Files.exists(sourceDir)) {
            throw new IOException("源目录不存在: " + sourcePathStr);
        }
        if (!Files.isDirectory(sourceDir)) {
            throw new IOException("源路径不是一个目录: " + sourcePathStr);
        }

        // 创建以源文件夹名称命名的.bak子目录
        String sourceDirName = sourceDir.getFileName().toString();
        Path bakDir = backupDir.resolve(sourceDirName + ".bak");
        createDirectory(bakDir);

        // 创建目录元数据
        Map<String, Object> dirMetadata = new HashMap<>();
        dirMetadata.put("sourcePath", sourcePathStr);
        dirMetadata.put("backupFiles", files);

        // 保存目录元数据
        try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(backupDir.resolve(sourceDirName + ".bak.meta")))) {
            oos.writeObject(dirMetadata);
        }

        System.out.println("\n开始备份...");
        System.out.println("备份目录: " + bakDir);
        System.out.println("需要备份 " + files.size() + " 个文件");

        long processedFiles = 0;

        // 备份指定的文件
        for (String fileName : files) {
            try {
                Path fileToBackup = sourceDir.resolve(fileName);
                Path relativePath = sourceDir.relativize(fileToBackup);
                Path targetPath = bakDir.resolve(relativePath.toString() + ".bak");

                if (!Files.exists(fileToBackup)) {
                    System.err.println("文件不存在，跳过: " + fileToBackup);
                    continue;
                }

                if (!Files.isRegularFile(fileToBackup)) {
                    System.err.println("不是常规文件，跳过: " + fileToBackup);
                    continue;
                }

                createDirectory(targetPath.getParent());
                backupSingleFile(fileToBackup, targetPath);
                processedFiles++;

                double progress = (double) processedFiles / files.size() * 100;
                System.out.printf("\r备份进度: %.1f%% (%d/%d)",
                        progress, processedFiles, files.size());

            } catch (IOException e) {
                System.err.println("\n备份文件失败: " + fileName + " - " + e.getMessage());
            }
        }

        System.out.println("\n目录备份完成! 共备份 " + processedFiles + " 个文件");
    }

    public void restoreDirectory(Path backupDir, Path restoreDir) throws IOException {
        // 读取目录元数据
        Path metaPath = Paths.get(backupDir + ".meta");
        if (!Files.exists(metaPath)) {
            throw new IOException("备份目录元数据文件不存在: " + metaPath);
        }

        Map<String, Object> dirMetadata;
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(metaPath))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) ois.readObject();
            dirMetadata = metadata;
        } catch (ClassNotFoundException e) {
            throw new IOException("读取目录元数据失败", e);
        }

        @SuppressWarnings("unchecked")
        List<String> backupFiles = (List<String>) dirMetadata.get("backupFiles");
        if (backupFiles == null || backupFiles.isEmpty()) {
            throw new IOException("备份文件列表为空");
        }

        createDirectory(restoreDir);
        System.out.println("\n开始还原...");
        System.out.println("还原目录: " + restoreDir);

        long totalFiles = backupFiles.size();
        long processedFiles = 0;

        // 还原文件
        for (String fileName : backupFiles) {
            try {
                Path backupFilePath = backupDir.resolve(fileName.substring(fileName.lastIndexOf("/") + 1) + ".bak");
                Path restoreFilePath = restoreDir.resolve(fileName.substring(fileName.lastIndexOf("/") + 1));

                if (!Files.exists(backupFilePath)) {
                    System.err.println("备份文件不存在，跳过: " + backupFilePath);
                    continue;
                }

                // 确保还原文件的父目录存在
                // createDirectory(restoreFilePath.getParent());

                // 还原单个文件
                restoreSingleFile(backupFilePath, restoreFilePath);
                processedFiles++;

                if (totalFiles > 0) {
                    double progress = (double) processedFiles / totalFiles * 100;
                    System.out.printf("\r还原进度: %.1f%% (%d/%d)",
                            progress, processedFiles, totalFiles);
                }
            } catch (IOException e) {
                System.err.println("\n还原文件失败: " + fileName + " - " + e.getMessage());
            }
        }

        System.out.println("\n目录还原完成! 共还原 " + processedFiles + " 个文件");
    }
}
