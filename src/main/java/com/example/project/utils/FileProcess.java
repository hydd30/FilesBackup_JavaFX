package com.example.project.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileProcess {
    public static String bytesToHexString(byte[] b) {
        StringBuilder stringBuilder = new StringBuilder();
        if(b == null || b.length <= 0) {
            return null;
        }
        for(int i = 0; i < b.length; i++) {
            int v = b[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if(hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static String getFileExtension(File file) {
        String fileName = file.getName();
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        } else {
            return "0000";
        }
    }

    public static String getFileFormat(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] b = new byte[10];
        fileInputStream.read(b, 0, b.length);
        switch (bytesToHexString(b)) {
            case "255044462d312e360d25":
                return "pdf";
            case "e69292e58f91e8bebee6":
                return "txt";
            case "d0cf11e0a1b11ae10000":
                return "doc,ppt,xls";
            case "504b0304140006000800":
                return "docx,xlsx";
            case "504b03040a0000000000":
                return "pptx,jar";
            case "89504e470d0a1a0a0000":
                return "png";
            case "ffd8ffe000104a464946":
                return "jpg,jpeg";
            case "47494638396126026f01":
                return "gif";
            case "49492a00227105008037":
                return "tif,tiff";
            //16位色图
            case "424d228c010000000000":
                return "bmp";
            //24位色图
            case "424d8240090000000000":
                return "bmp";
            //256位色图
            case "424d8e1be30000000000":
                return "bmp";
            case "3c21444f435459504520":
                return "html";
            case "526172211a0700cf9073":
                return "rar";
            case "504b0304140000000800":
                return "zip";
            case "235468697320636f6e66":
                return "ini";
            case "4d5a9000030000000400":
                return "exe";
            case "49443303000000002176":
                return "mp3";
            case "49443303000000034839":
                return "mp3";
            case "00000020667479706973":
                return "mp4";
            case "000001ba210001000180":
                return "mpg";
            case "3026b2758e66cf11a6d9":
                return "wmv,asf";
            case "52494646d07d60074156":
                return "avi";
            case "464c5601050000000900":
                return "flv,f4v";
            case "4d546864000000060001":
                return "mid,midi";
            default:
                return "0000";
        }
    }
}
