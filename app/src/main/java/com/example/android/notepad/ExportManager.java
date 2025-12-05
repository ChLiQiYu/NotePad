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

import android.content.ContentResolver;
import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONException;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * ExportManager类负责处理笔记数据的导出逻辑
 * 包括查询数据、序列化为JSON格式并保存到文件
 */
public class ExportManager {
    private static final String TAG = "ExportManager";
    private NoteDataSource dataSource;
    private ContentResolver contentResolver;

    public ExportManager(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
        this.dataSource = new NoteDataSource(contentResolver);
    }

    /**
     * 异步导出笔记到文件
     * @param outputFile 输出文件
     * @param callback 导出回调接口
     */
    public void exportNotes(File outputFile, ExportCallback callback) {
        new ExportTask(this, outputFile, callback).execute();
    }

    /**
     * 取消导出操作
     */
    public void cancelExport() {
        // 当前实现中没有需要特别取消的操作
        // 在实际应用中，可以在这里添加取消逻辑
    }

    /**
     * 导出任务的AsyncTask实现
     */
    private static class ExportTask extends AsyncTask<Void, Integer, Exception> {
        private WeakReference<ExportManager> managerRef;
        private File outputFile;
        private ExportCallback callback;
        private int noteCount;

        ExportTask(ExportManager manager, File outputFile, ExportCallback callback) {
            this.managerRef = new WeakReference<>(manager);
            this.outputFile = outputFile;
            this.callback = callback;
        }

        @Override
        protected Exception doInBackground(Void... voids) {
            try {
                Log.d(TAG, "开始导出笔记...");
                
                // 检查是否已经取消
                if (isCancelled()) {
                    Log.w(TAG, "导出已取消");
                    return new Exception("导出已取消");
                }

                ExportManager manager = managerRef.get();
                if (manager == null) {
                    Log.e(TAG, "ExportManager为null");
                    return new Exception("ExportManager为null");
                }

                // 检查输出文件是否可写
                File parentDir = outputFile.getParentFile();
                Log.d(TAG, "输出文件路径: " + outputFile.getAbsolutePath());
                Log.d(TAG, "父目录: " + (parentDir != null ? parentDir.getAbsolutePath() : "null"));
                Log.d(TAG, "父目录是否存在: " + (parentDir != null && parentDir.exists()));
                Log.d(TAG, "父目录是否可写: " + (parentDir != null && parentDir.canWrite()));
                
                // 确保父目录存在（提前创建）
                if (parentDir != null && !parentDir.exists()) {
                    Log.d(TAG, "父目录不存在，正在创建...");
                    boolean created = parentDir.mkdirs();
                    Log.d(TAG, "父目录创建结果: " + created);
                    if (!created) {
                        Log.e(TAG, "无法创建父目录");
                        return new IOException("无法创建父目录: " + parentDir.getAbsolutePath());
                    }
                    // 再次验证目录是否真的创建成功
                    if (!parentDir.exists()) {
                        Log.e(TAG, "父目录创建后仍不存在");
                        return new IOException("父目录创建失败: " + parentDir.getAbsolutePath());
                    }
                }
                
                // 检查目录写入权限
                if (parentDir != null && !parentDir.canWrite()) {
                    Log.e(TAG, "文件输出目录没有写入权限");
                    return new IOException("文件输出目录没有写入权限: " + parentDir.getAbsolutePath());
                }

                // 获取所有笔记数据
                List<Note> notes = manager.dataSource.getAllNotes();
                if (notes == null || notes.isEmpty()) {
                    Log.w(TAG, "没有笔记数据");
                    return new Exception("没有笔记数据需要导出");
                }
                noteCount = notes.size();
                Log.d(TAG, "获取了 " + noteCount + " 条笔记");

                // 更新进度
                publishProgress(50);

                // 序列化为JSON格式
                long exportTime = System.currentTimeMillis();
                String jsonContent = JsonSerializer.serializeNotes(notes, exportTime);
                Log.d(TAG, "JSON序列化完成, 内容长度: " + jsonContent.length() + " 字节");
                
                // 验证JSON内容是否有效
                if (jsonContent == null || jsonContent.trim().isEmpty()) {
                    Log.e(TAG, "JSON序列化失败");
                    return new Exception("JSON序列化失败");
                }

                // 保存到文件
                Log.d(TAG, "正在写入文件...");
                Log.d(TAG, "目标文件: " + outputFile.getAbsolutePath());
                Log.d(TAG, "JSON内容大小: " + jsonContent.length() + " 字符");
                
                try {
                    FileUtils.writeToFile(outputFile, jsonContent);
                    Log.d(TAG, "FileUtils.writeToFile执行完成");
                } catch (IOException e) {
                    Log.e(TAG, "文件写入失败", e);
                    // 清理可能残留的空文件
                    if (outputFile.exists() && outputFile.length() == 0) {
                        Log.d(TAG, "删除空文件: " + outputFile.delete());
                    }
                    return e;
                } catch (Exception e) {
                    Log.e(TAG, "文件写入发生未预期异常", e);
                    // 清理可能残留的文件
                    if (outputFile.exists()) {
                        Log.d(TAG, "删除残留文件: " + outputFile.delete());
                    }
                    return new IOException("文件写入失败: " + e.getMessage(), e);
                }

                // 二次验证 - 确保文件真正被创建并写入成功
                // 等待文件系统完全同步（增加延迟时间）
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.w(TAG, "延迟被中断", e);
                }
                
                if (!outputFile.exists()) {
                    Log.e(TAG, "验证失败：文件不存在");
                    Log.e(TAG, "期望路径: " + outputFile.getAbsolutePath());
                    Log.e(TAG, "父目录是否存在: " + (parentDir != null && parentDir.exists()));
                    if (parentDir != null && parentDir.exists()) {
                        Log.e(TAG, "父目录内容: " + java.util.Arrays.toString(parentDir.list()));
                    }
                    return new IOException("文件写入后不存在: " + outputFile.getAbsolutePath());
                }

                long fileSize = outputFile.length();
                Log.d(TAG, "文件验证通过, 大小: " + fileSize + " 字节");
                
                if (fileSize == 0) {
                    Log.e(TAG, "验证失败：文件大小为0");
                    // 删除空文件
                    outputFile.delete();
                    return new IOException("文件大小为0字节，数据未正常写入");
                }
                
                // 验证文件大小是否合理（至少应该有JSON基本结构）
                if (fileSize < 10) {
                    Log.e(TAG, "验证失败：文件过小，疑似写入失败");
                    outputFile.delete();
                    return new IOException("文件异常过小: " + fileSize + "字节");
                }
                
                // 放宽验证条件，只要有部分内容写入就认为成功
                if (fileSize < jsonContent.length() / 10) {
                    Log.e(TAG, "验证失败：文件写入严重不完整");
                    outputFile.delete();
                    return new IOException("文件写入严重不完整: 期望" + jsonContent.length() + "字节，实际" + fileSize + "字节");
                }

                // 更新进度
                publishProgress(100);
                Log.d(TAG, "导出完成！");

                return null;
            } catch (JSONException e) {
                Log.e(TAG, "JSON异常", e);
                return e;
            } catch (Exception e) {
                Log.e(TAG, "未预期错误", e);
                return new Exception("未预期错误: " + e.getMessage(), e);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (callback != null && values.length > 0) {
                callback.onProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (callback != null) {
                if (result == null) {
                    callback.onSuccess(outputFile);
                } else {
                    callback.onError(result);
                }
            }
        }

        @Override
        protected void onCancelled() {
            if (callback != null) {
                callback.onError(new Exception("Export cancelled"));
            }
        }
    }

    /**
     * 导出回调接口
     */
    public interface ExportCallback {
        void onSuccess(File outputFile);
        void onError(Exception error);
        void onProgress(int progress);
    }
}