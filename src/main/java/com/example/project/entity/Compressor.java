package com.example.project.entity;

import java.io.*;
import java.util.*;

public class Compressor {
    // 压缩算法类型枚举
    private enum Algorithm {
        RLE,
        HUFFMAN
    }

    private static Algorithm currentAlgorithm = Algorithm.RLE; // 默认使用RLE

    public static class CompressionException extends RuntimeException {
        public CompressionException(String message) {
            super(message);
        }
    }

    // 设置压缩算法
    public static void setAlgorithm(String algorithm) {
        try {
            currentAlgorithm = Algorithm.valueOf(algorithm.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CompressionException("不支持的压缩算法: " + algorithm);
        }
    }

    public static byte[] compress(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                return new byte[0];
            }

            switch (currentAlgorithm) {
                case RLE:
                    return compressRLE(data);
                case HUFFMAN:
                    return compressHuffman(data);
                default:
                    throw new CompressionException("不支持的压缩算法");
            }
        } catch (Exception e) {
            throw new CompressionException("压缩错误: " + e.getMessage());
        }
    }
    public static byte[] decompress(byte[] compressed) {
        try {
            if (compressed == null || compressed.length == 0) {
                return new byte[0];
            }

            // 检查压缩算法标记
            if (compressed.length >= 1) {
                byte algorithmMarker = compressed[0];
                if (algorithmMarker == 'R') {
                    return decompressRLE(compressed);
                } else if (algorithmMarker == 'H') {
                    return decompressHuffman(compressed);
                }
            }

            // 如果没有识别出算法标记，返回原始数据
            return compressed;

        } catch (Exception e) {
            throw new CompressionException("解压错误: " + e.getMessage());
        }
    }

    // RLE压缩实现
    private static byte[] compressRLE(byte[] data) throws IOException {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        compressed.write('R'); // RLE算法标记

        int i = 0;
        while (i < data.length) {
            // 寻找重复序列
            int runLength = findRunLength(data, i);

            if (runLength >= 4) { // 最小重复长度阈值
                // 压缩重复序列
                compressed.write((byte)Math.min(runLength, 127));
                compressed.write(data[i]);
                i += runLength;
            } else {
                // 处理非重复序列
                int literalLength = findLiteralLength(data, i);
                int lengthToEncode = Math.min(literalLength, 127);

                compressed.write((byte)(lengthToEncode | 0x80));
                compressed.write(data, i, lengthToEncode);
                i += lengthToEncode;
            }
        }

        byte[] result = compressed.toByteArray();
        return result.length < data.length + 1 ? result : data;
    }

    // Huffman压缩实现
    private static byte[] compressHuffman(byte[] data) throws IOException {
        // 计算字节频率
        Map<Byte, Integer> frequencies = new HashMap<>();
        for (byte b : data) {
            frequencies.merge(b, 1, Integer::sum);
        }

        // 构建Huffman树
        HuffmanNode root = buildHuffmanTree(frequencies);

        // 生成编码表
        Map<Byte, String> codes = new HashMap<>();
        generateHuffmanCodes(root, "", codes);

        // 写入压缩数据
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        compressed.write('H'); // Huffman算法标记

        // 写入频率表
        writeFrequencyTable(compressed, frequencies);

        // 写入压缩后的数据
        BitOutputStream bitOut = new BitOutputStream(compressed);
        for (byte b : data) {
            String code = codes.get(b);
            for (char c : code.toCharArray()) {
                bitOut.writeBit(c == '1');
            }
        }
        bitOut.flush();

        byte[] result = compressed.toByteArray();
        return result.length < data.length + 1 ? result : data;
    }

    // RLE解压实现
    private static byte[] decompressRLE(byte[] compressed) throws IOException {
        ByteArrayOutputStream decompressed = new ByteArrayOutputStream();
        int i = 1; // 跳过算法标记

        while (i < compressed.length) {
            byte control = compressed[i++];

            if (i >= compressed.length) {
                throw new CompressionException("压缩数据格式错误");
            }

            if ((control & 0x80) != 0) {  // 非重复数据块
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
    }

    // Huffman解压实现
    private static byte[] decompressHuffman(byte[] compressed) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(compressed, 1, compressed.length - 1);

        // 读取频率表
        Map<Byte, Integer> frequencies = readFrequencyTable(input);

        // 重建Huffman树
        HuffmanNode root = buildHuffmanTree(frequencies);

        // 计算原始数据长度
        int originalLength = frequencies.values().stream().mapToInt(Integer::intValue).sum();

        // 解压数据
        ByteArrayOutputStream decompressed = new ByteArrayOutputStream();
        BitInputStream bitIn = new BitInputStream(input);

        HuffmanNode current = root;
        int decompressedCount = 0;

        while (decompressedCount < originalLength) {
            while (current.left != null && current.right != null) {
                boolean bit = bitIn.readBit();
                current = bit ? current.right : current.left;
            }
            decompressed.write(current.value);
            decompressedCount++;
            current = root;
        }

        return decompressed.toByteArray();
    }

    // 查找重复序列长度
    private static int findRunLength(byte[] data, int start) {
        int count = 1;
        byte value = data[start];

        for (int i = start + 1; i < data.length && count < 127; i++) {
            if (data[i] != value) {
                break;
            }
            count++;
        }

        return count;
    }

    // 查找非重复序列长度
    private static int findLiteralLength(byte[] data, int start) {
        int count = 1;

        for (int i = start + 1; i < data.length && count < 127; i++) {
            if (i + 3 < data.length &&
                    data[i] == data[i + 1] &&
                    data[i] == data[i + 2] &&
                    data[i] == data[i + 3]) {
                break;
            }
            count++;
        }

        return count;
    }

    // Huffman树节点
    private static class HuffmanNode implements Comparable<HuffmanNode> {
        byte value;
        int frequency;
        HuffmanNode left;
        HuffmanNode right;

        HuffmanNode(byte value, int frequency) {
            this.value = value;
            this.frequency = frequency;
        }

        @Override
        public int compareTo(HuffmanNode other) {
            return this.frequency - other.frequency;
        }
    }

    // 构建Huffman树
    private static HuffmanNode buildHuffmanTree(Map<Byte, Integer> frequencies) {
        PriorityQueue<HuffmanNode> queue = new PriorityQueue<>();
        frequencies.forEach((value, freq) -> queue.offer(new HuffmanNode(value, freq)));

        while (queue.size() > 1) {
            HuffmanNode left = queue.poll();
            HuffmanNode right = queue.poll();
            HuffmanNode parent = new HuffmanNode((byte) 0, left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;
            queue.offer(parent);
        }

        return queue.poll();
    }

    // 生成Huffman编码表
    private static void generateHuffmanCodes(HuffmanNode node, String code, Map<Byte, String> codes) {
        if (node.left == null && node.right == null) {
            codes.put(node.value, code);
            return;
        }
        if (node.left != null) {
            generateHuffmanCodes(node.left, code + "0", codes);
        }
        if (node.right != null) {
            generateHuffmanCodes(node.right, code + "1", codes);
        }
    }

    // 写入频率表
    private static void writeFrequencyTable(OutputStream out, Map<Byte, Integer> frequencies) throws IOException {
        DataOutputStream dataOut = new DataOutputStream(out);
        dataOut.writeInt(frequencies.size());
        for (Map.Entry<Byte, Integer> entry : frequencies.entrySet()) {
            dataOut.writeByte(entry.getKey());
            dataOut.writeInt(entry.getValue());
        }
    }

    // 读取频率表
    private static Map<Byte, Integer> readFrequencyTable(InputStream in) throws IOException {
        DataInputStream dataIn = new DataInputStream(in);
        int size = dataIn.readInt();
        Map<Byte, Integer> frequencies = new HashMap<>();
        for (int i = 0; i < size; i++) {
            byte value = dataIn.readByte();
            int frequency = dataIn.readInt();
            frequencies.put(value, frequency);
        }
        return frequencies;
    }

    // 位操作输出流
    private static class BitOutputStream {
        private OutputStream out;
        private int buffer;
        private int bitsInBuffer;

        BitOutputStream(OutputStream out) {
            this.out = out;
            this.buffer = 0;
            this.bitsInBuffer = 0;
        }

        void writeBit(boolean bit) throws IOException {
            buffer = (buffer << 1) | (bit ? 1 : 0);
            bitsInBuffer++;
            if (bitsInBuffer == 8) {
                out.write(buffer);
                buffer = 0;
                bitsInBuffer = 0;
            }
        }

        void flush() throws IOException {
            if (bitsInBuffer > 0) {
                buffer = buffer << (8 - bitsInBuffer);
                out.write(buffer);
            }
        }
    }

    // 位操作输入流
    private static class BitInputStream {
        private InputStream in;
        private int buffer;
        private int bitsRemaining;

        BitInputStream(InputStream in) {
            this.in = in;
            this.buffer = 0;
            this.bitsRemaining = 0;
        }

        boolean readBit() throws IOException {
            if (bitsRemaining == 0) {
                buffer = in.read();
                if (buffer == -1) {
                    throw new EOFException();
                }
                bitsRemaining = 8;
            }
            boolean bit = ((buffer >> (bitsRemaining - 1)) & 1) == 1;
            bitsRemaining--;
            return bit;
        }
    }

    public static double getCompressionRatio(byte[] original, byte[] compressed) {
        if (original == null || original.length == 0) return 0.0;
        return (double) compressed.length / original.length * 100.0;
    }
}