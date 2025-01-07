package com.example.project.entity;

import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Setter
public class FileFilter {
    private String pathPattern = "";
    private String namePattern = "";

    private Set<String> fileTypes = new HashSet<>();

    private long minSize = 0;
    private long maxSize = Long.MAX_VALUE;

    private Instant modifiedAfter = Instant.EPOCH;
    private Instant modifiedBefore = Instant.now();

    private boolean hasTimeFilter = false;
    private boolean hasFilter = false;

    private boolean matchPattern(String str, String pattern) {
        if (pattern.isEmpty()) return true;

        String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
        try {
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
                    .matcher(str)
                    .matches();
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    public boolean matches(Path filePath) throws IOException {
        if (!hasFilter) return true;

        if (!pathPattern.isEmpty() &&
                !matchPattern(filePath.toString(), pathPattern)) {
            return false;
        }

        if (!namePattern.isEmpty() &&
                !matchPattern(filePath.getFileName().toString(), namePattern)) {
            return false;
        }

        if (!fileTypes.isEmpty()) {
            String ext = getFileExtension(filePath);
            if (!fileTypes.contains(ext.toLowerCase())) {
                return false;
            }
        }

        long size = Files.size(filePath);
        if (size < minSize || size > maxSize) {
            return false;
        }

        if (hasTimeFilter) {
            FileTime lastModified = Files.getLastModifiedTime(filePath);
            Instant modTime = lastModified.toInstant();

            if (!modTime.isAfter(modifiedAfter) || !modTime.isBefore(modifiedBefore)) {
                return false;
            }
        }

        return true;
    }

    private String getFileExtension(Path path) {
        String name = path.getFileName().toString();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1) : "";

    }

    public void clearAllFilters() {
        pathPattern = "";
        namePattern = "";
        fileTypes.clear();
        minSize = 0;
        maxSize = Long.MAX_VALUE;
        modifiedAfter = Instant.EPOCH;
        modifiedBefore = Instant.now();
        hasTimeFilter = false;
        hasFilter = false;
    }

    public String getFilterDescription() {
        StringBuilder sb = new StringBuilder("当前过滤条件:\n");

        if (!hasFilter) {
            sb.append("  无过滤条件(接受所有文件)\n");
            return sb.toString();
        }

        if (!pathPattern.isEmpty()) {
            sb.append("  路径匹配: ").append(pathPattern).append("\n");
        }
        if (!namePattern.isEmpty()) {
            sb.append("  文件名匹配: ").append(namePattern).append("\n");
        }
        if (!fileTypes.isEmpty()) {
            sb.append("  文件类型: ")
                    .append(String.join(", ", fileTypes))
                    .append("\n");
        }
        if (minSize > 0 || maxSize < Long.MAX_VALUE) {
            sb.append("  文件大小范围: ")
                    .append(minSize)
                    .append(" bytes 至 ")
                    .append(maxSize == Long.MAX_VALUE ? "无限制" : maxSize + " bytes")
                    .append("\n");
        }
        if (hasTimeFilter) {
            sb.append("  修改时间范围: ")
                    .append(modifiedAfter.equals(Instant.EPOCH) ? "不限" :
                            modifiedAfter.truncatedTo(ChronoUnit.DAYS))
                    .append(" 至 ")
                    .append(modifiedBefore.equals(Instant.now()) ? "不限" :
                            modifiedBefore.truncatedTo(ChronoUnit.DAYS))
                    .append("\n");
        }
        return sb.toString();
    }
}
