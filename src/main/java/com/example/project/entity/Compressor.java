package com.example.project.entity;

import java.io.ByteArrayOutputStream;

public class Compressor {
    public static class CompressionException extends RuntimeException {
        public CompressionException(String message) {
            super(message);
        }
    }

    public static byte[] compress(byte[] data) {
        try {
            if (data.length == 0) {
                return new byte[0];
            }

            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            int i = 0;

            while (i < data.length) {
                if (i + 3 < data.length &&
                        data[i] == data[i + 1] &&
                        data[i] == data[i + 2] &&
                        data[i] == data[i + 3]) {
                    // 压缩重复数据
                    byte count = 1;
                    int j = i + 1;
                    while (j < data.length && data[j] == data[i] && count < 127) {
                        count++;
                        j++;
                    }
                    compressed.write(count);
                    compressed.write(data[i]);
                    i += count;
                } else {
                    // 处理不重复数据
                    int start = i;
                    int j = i + 1;
                    byte count = 1;
                    while (j < data.length && count < 127) {
                        if (j + 2 < data.length &&
                                data[j] == data[j + 1] &&
                                data[j] == data[j + 2]) {
                            break;
                        }
                        count++;
                        j++;
                    }
                    compressed.write(count | 0x80);
                    compressed.write(data, i, count);
                    i += count;
                }
            }

            return compressed.toByteArray();
        } catch (Exception e) {
            throw new CompressionException("压缩错误: " + e.getMessage());
        }
    }

    public static byte[] decompress(byte[] compressed) {
        try {
            if (compressed.length == 0) {
                return new byte[0];
            }

            ByteArrayOutputStream decompressed = new ByteArrayOutputStream();
            int i = 0;

            while (i < compressed.length) {
                byte control = compressed[i++];

                if (i >= compressed.length) {
                    throw new CompressionException("压缩数据格式错误");
                }

                if ((control & 0x80) != 0) {  // 不重复数据块
                    byte count = (byte)(control & 0x7F);
                    if (i + count > compressed.length) {
                        throw new CompressionException("压缩数据格式错误");
                    }
                    decompressed.write(compressed, i, count);
                    i += count;
                } else {  // 重复数据块
                    byte count = control;
                    byte value = compressed[i++];
                    for (int j = 0; j < count; j++) {
                        decompressed.write(value);
                    }
                }
            }

            return decompressed.toByteArray();
        } catch (Exception e) {
            throw new CompressionException("解压错误: " + e.getMessage());
        }
    }

    public static double getCompressionRatio(byte[] original, byte[] compressed) {
        if (original.length == 0) return 0.0;
        return (double)compressed.length / original.length * 100.0;
    }
}