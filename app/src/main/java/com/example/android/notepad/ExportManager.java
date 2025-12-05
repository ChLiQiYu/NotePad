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
                // 检查是否已经取消
                if (isCancelled()) {
                    return new Exception("Export cancelled");
                }

                ExportManager manager = managerRef.get();
                if (manager == null) {
                    return new Exception("ExportManager is null");
                }

                // 获取所有笔记数据
                List<Note> notes = manager.dataSource.getAllNotes();
                noteCount = notes.size();

                // 更新进度
                publishProgress(50);

                // 序列化为JSON格式
                long exportTime = System.currentTimeMillis();
                String jsonContent = JsonSerializer.serializeNotes(notes, exportTime);

                // 确保父目录存在
                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                // 保存到文件
                FileUtils.writeToFile(outputFile, jsonContent);

                // 验证文件是否真正创建成功
                if (!outputFile.exists() || outputFile.length() == 0) {
                    return new IOException("文件创建失败或为空");
                }

                // 更新进度
                publishProgress(100);

                return null;
            } catch (JSONException e) {
                return e;
            } catch (IOException e) {
                return e;
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