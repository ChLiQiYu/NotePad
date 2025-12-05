/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * FileUtils类提供文件读写相关的工具方法
 */
public class FileUtils {
    private static final String TAG = "FileUtils";

    /**
     * 将字符串写入文件
     * @param file 目标文件
     * @param content 要写入的内容
     * @throws IOException 如果写入过程中出现错误
     */
    public static void writeToFile(File file, String content) throws IOException {
        Log.d(TAG, "开始写入文件: " + file.getAbsolutePath());
        Log.d(TAG, "内容长度: " + content.length() + " 字符");
        
        // 确保父目录存在
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            Log.d(TAG, "父目录: " + parentDir.getAbsolutePath());
            Log.d(TAG, "父目录是否存在: " + parentDir.exists());
            
            if (!parentDir.exists()) {
                Log.d(TAG, "父目录不存在，正在创建: " + parentDir.getAbsolutePath());
                boolean mkdirsResult = parentDir.mkdirs();
                Log.d(TAG, "创建结果: " + mkdirsResult);
                
                // 再次验证目录是否创建成功
                if (!parentDir.exists()) {
                    Log.e(TAG, "目录创建失败");
                    throw new IOException("无法创建目录: " + parentDir.getAbsolutePath());
                }
            }
            
            // 检查目录是否可写
            if (!parentDir.canWrite()) {
                Log.e(TAG, "目录不可写");
                throw new IOException("目录不可写: " + parentDir.getAbsolutePath());
            }
        }
        
        // 如果文件已存在，先删除旧文件
        if (file.exists()) {
            Log.d(TAG, "文件已存在，删除旧文件: " + file.delete());
        }
        
        FileOutputStream fos = null;
        OutputStreamWriter writer = null;
        boolean writeSuccess = false;
        
        try {
            Log.d(TAG, "打开文件输出流...");
            fos = new FileOutputStream(file);
            writer = new OutputStreamWriter(fos, "UTF-8");
            
            Log.d(TAG, "写入 " + content.length() + " 字符...");
            writer.write(content);
            
            Log.d(TAG, "刷新缓冲区...");
            writer.flush();
            
            Log.d(TAG, "同步到磁盘...");
            fos.getFD().sync();
            
            writeSuccess = true;
            Log.d(TAG, "文件写入完成");
            
        } catch (IOException e) {
            Log.e(TAG, "写入过程中发生错误", e);
            throw e;
        } finally {
            // 关闭资源
            if (writer != null) {
                try {
                    writer.close();
                    Log.d(TAG, "Writer关闭成功");
                } catch (IOException e) {
                    Log.w(TAG, "关闭写入流失败", e);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                    Log.d(TAG, "FileOutputStream关闭成功");
                } catch (IOException e) {
                    Log.w(TAG, "关闭FileOutputStream失败", e);
                }
            }
        }
        
        // 在资源关闭后验证文件
        Log.d(TAG, "等待文件系统同步...");
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "延迟被中断", e);
        }
        
        Log.d(TAG, "验证文件是否存在...");
        if (!file.exists()) {
            Log.e(TAG, "文件写入失败，文件不存在");
            throw new IOException("文件写入失败，文件不存在: " + file.getAbsolutePath());
        }
        
        long fileSize = file.length();
        Log.d(TAG, "文件成功写入，大小: " + fileSize + " 字节");
        
        if (fileSize == 0) {
            Log.e(TAG, "文件大小为0");
            // 删除空文件
            file.delete();
            throw new IOException("文件写入失败，文件为空: " + file.getAbsolutePath());
        }
        
        // 验证文件大小是否合理
        long expectedSize = content.getBytes("UTF-8").length;
        Log.d(TAG, "期望大小: " + expectedSize + " 字节, 实际大小: " + fileSize + " 字节");
        
        // 容忍一定的编码差异，但不应该相差太大
        if (fileSize < expectedSize * 0.5) {
            Log.e(TAG, "文件大小异常，可能写入不完整");
            file.delete();
            throw new IOException("文件写入不完整: 期望" + expectedSize + "字节, 实际" + fileSize + "字节");
        }
    }

    /**
     * 从文件读取字符串内容
     * @param file 源文件
     * @return 文件内容
     * @throws IOException 如果读取过程中出现错误
     */
    public static String readFromFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader reader = new InputStreamReader(fis);
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuilder content = new StringBuilder();
        try {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } finally {
            bufferedReader.close();
            reader.close();
            fis.close();
        }
        return content.toString();
    }

    /**
     * 从Uri获取文件对象
     * @param context 应用上下文
     * @param uri 文件Uri
     * @return 文件对象
     */
    public static File getFileFromUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        
        try {
            // 尝试从Uri获取文件路径
            String filePath = getFilePathFromUri(context, uri);
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    return file;
                }
            }
            
            // 如果无法直接获取文件路径，则复制到临时文件
            return copyUriToFile(context, uri);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 从Uri获取文件路径
     * @param context 应用上下文
     * @param uri 文件Uri
     * @return 文件路径
     */
    private static String getFilePathFromUri(Context context, Uri uri) {
        try {
            // 尝试使用DocumentFile或其他方式获取真实路径
            String uriString = uri.toString();
            
            // 如果是文件Uri，直接返回路径
            if (uriString.startsWith("file://")) {
                return uri.getPath();
            }
            
            // 对于content Uri，尝试获取真实路径
            if (uriString.startsWith("content://")) {
                // 这里可以添加更多针对特定内容提供者的处理逻辑
                return null; // 让后续逻辑使用复制方式
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 将Uri指向的文件复制到临时文件
     * @param context 应用上下文
     * @param uri 文件Uri
     * @return 临时文件对象
     */
    private static File copyUriToFile(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }
            
            // 获取文件名
            String fileName = getFileNameFromUri(context, uri);
            if (fileName == null) {
                fileName = "import_notes.json";
            }
            
            // 创建临时文件
            File tempFile = new File(context.getCacheDir(), fileName);
            OutputStream outputStream = new FileOutputStream(tempFile);
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            outputStream.close();
            inputStream.close();
            
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 从Uri获取文件名
     * @param context 应用上下文
     * @param uri 文件Uri
     * @return 文件名
     */
    private static String getFileNameFromUri(Context context, Uri uri) {
        try {
            String fileName = null;
            
            // 尝试从Uri中获取文件名
            if (uri.getScheme() != null && uri.getScheme().equals("content")) {
                android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                            if (nameIndex >= 0) {
                                fileName = cursor.getString(nameIndex);
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
            
            // 如果无法从content Uri获取文件名，尝试从路径获取
            if (fileName == null) {
                fileName = uri.getLastPathSegment();
            }
            
            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 获取应用的外部存储目录
     * @param context 应用上下文
     * @return 外部存储目录
     */
    public static File getExternalStorageDirectory(Context context) {
        // 使用公共下载目录，确保文件可以被文件管理器访问
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    /**
     * 创建默认的备份文件路径
     * @param context 应用上下文
     * @param fileName 文件名
     * @return 完整的文件路径
     */
    public static File createDefaultBackupFile(Context context, String fileName) {
        File directory = getExternalStorageDirectory(context);
        if (directory != null && !directory.exists()) {
            directory.mkdirs();
        }
        return new File(directory, fileName);
    }
}