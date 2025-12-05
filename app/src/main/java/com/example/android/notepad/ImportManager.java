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
import java.util.ArrayList;
import java.util.List;

/**
 * ImportManager类负责处理从文件导入笔记数据的逻辑
 * 包括解析JSON文件、验证数据格式并插入到数据库中
 */
public class ImportManager {
    private NoteDataSource dataSource;
    private ContentResolver contentResolver;

    public ImportManager(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
        this.dataSource = new NoteDataSource(contentResolver);
    }

    /**
     * 异步从文件导入笔记
     * @param inputFile 输入文件
     * @param callback 导入回调接口
     */
    public void importNotes(File inputFile, ImportCallback callback) {
        new ImportTask(this, inputFile, callback).execute();
    }

    /**
     * 取消导入操作
     */
    public void cancelImport() {
        // 当前实现中没有需要特别取消的操作
        // 在实际应用中，可以在这里添加取消逻辑
    }

    /**
     * 导入任务的AsyncTask实现
     */
    private static class ImportTask extends AsyncTask<Void, Integer, ImportResult> {
        private WeakReference<ImportManager> managerRef;
        private File inputFile;
        private ImportCallback callback;

        ImportTask(ImportManager manager, File inputFile, ImportCallback callback) {
            this.managerRef = new WeakReference<>(manager);
            this.inputFile = inputFile;
            this.callback = callback;
        }

        @Override
        protected ImportResult doInBackground(Void... voids) {
            try {
                // 检查是否已经取消
                if (isCancelled()) {
                    return new ImportResult(new Exception("Import cancelled"), 0);
                }

                ImportManager manager = managerRef.get();
                if (manager == null) {
                    return new ImportResult(new Exception("ImportManager is null"), 0);
                }

                // 从文件读取内容
                String fileContent = FileUtils.readFromFile(inputFile);

                // 更新进度
                publishProgress(30);

                // 反序列化JSON数据
                JsonSerializer.NotesData notesData = JsonSerializer.deserializeNotes(fileContent);

                // 更新进度
                publishProgress(60);

                // 处理导入的笔记数据
                Note[] importedNotes = notesData.getNotes();
                int importedCount = 0;

                for (int i = 0; i < importedNotes.length; i++) {
                    Note importedNote = importedNotes[i];
                    
                    // 检查是否已存在相同ID的笔记
                    Note existingNote = manager.dataSource.getNoteById(importedNote.getId());
                    
                    if (existingNote != null) {
                        // 如果存在，更新现有笔记
                        importedNote.setModifyTime(System.currentTimeMillis());
                        manager.dataSource.updateNote(importedNote);
                    } else {
                        // 如果不存在，插入新笔记
                        // 注意：我们需要重新设置创建时间和修改时间以避免时间错乱
                        importedNote.setCreateTime(System.currentTimeMillis());
                        importedNote.setModifyTime(System.currentTimeMillis());
                        manager.dataSource.insertNote(importedNote);
                    }
                    
                    importedCount++;
                    
                    // 更新进度
                    if (i % 5 == 0) { // 每5个笔记更新一次进度
                        int progress = 60 + (i * 40 / importedNotes.length);
                        publishProgress(progress);
                    }
                }

                // 更新进度
                publishProgress(100);

                return new ImportResult(null, importedCount);
            } catch (JSONException e) {
                return new ImportResult(e, 0);
            } catch (IOException e) {
                return new ImportResult(e, 0);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (callback != null && values.length > 0) {
                callback.onProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(ImportResult result) {
            if (callback != null) {
                if (result.getError() == null) {
                    callback.onSuccess(result.getImportedCount());
                } else {
                    callback.onError(result.getError());
                }
            }
        }

        @Override
        protected void onCancelled() {
            if (callback != null) {
                callback.onError(new Exception("Import cancelled"));
            }
        }
    }

    /**
     * 导入结果包装类
     */
    private static class ImportResult {
        private Exception error;
        private int importedCount;

        ImportResult(Exception error, int importedCount) {
            this.error = error;
            this.importedCount = importedCount;
        }

        public Exception getError() {
            return error;
        }

        public int getImportedCount() {
            return importedCount;
        }
    }

    /**
     * 导入回调接口
     */
    public interface ImportCallback {
        void onSuccess(int importedCount);
        void onError(Exception error);
        void onProgress(int progress);
    }
}