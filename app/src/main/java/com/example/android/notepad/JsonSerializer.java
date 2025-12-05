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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * JsonSerializer类用于将笔记数据序列化为JSON格式以及从JSON格式反序列化
 */
public class JsonSerializer {

    private static final String VERSION_KEY = "version";
    private static final String EXPORT_TIME_KEY = "exportTime";
    private static final String NOTES_KEY = "notes";
    private static final String ID_KEY = "id";
    private static final String TITLE_KEY = "title";
    private static final String CONTENT_KEY = "content";
    private static final String CREATE_TIME_KEY = "createTime";
    private static final String MODIFY_TIME_KEY = "modifyTime";
    private static final String CURRENT_VERSION = "1.0";

    /**
     * 将笔记列表序列化为JSON字符串
     * @param notes 笔记列表
     * @param exportTime 导出时间
     * @return JSON字符串
     * @throws JSONException 如果序列化过程中出现错误
     */
    public static String serializeNotes(List<Note> notes, long exportTime) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(VERSION_KEY, CURRENT_VERSION);
        jsonObject.put(EXPORT_TIME_KEY, exportTime);

        JSONArray notesArray = new JSONArray();
        for (Note note : notes) {
            JSONObject noteObject = new JSONObject();
            noteObject.put(ID_KEY, note.getId());
            noteObject.put(TITLE_KEY, note.getTitle());
            noteObject.put(CONTENT_KEY, note.getContent());
            noteObject.put(CREATE_TIME_KEY, note.getCreateTime());
            noteObject.put(MODIFY_TIME_KEY, note.getModifyTime());
            notesArray.put(noteObject);
        }

        jsonObject.put(NOTES_KEY, notesArray);
        return jsonObject.toString();
    }

    /**
     * 从JSON字符串反序列化笔记列表
     * @param jsonString JSON字符串
     * @return 包含版本信息和笔记列表的包装对象
     * @throws JSONException 如果反序列化过程中出现错误
     */
    public static NotesData deserializeNotes(String jsonString) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        String version = jsonObject.getString(VERSION_KEY);
        long exportTime = jsonObject.getLong(EXPORT_TIME_KEY);

        JSONArray notesArray = jsonObject.getJSONArray(NOTES_KEY);
        Note[] notes = new Note[notesArray.length()];

        for (int i = 0; i < notesArray.length(); i++) {
            JSONObject noteObject = notesArray.getJSONObject(i);
            Note note = new Note();
            note.setId(noteObject.getLong(ID_KEY));
            note.setTitle(noteObject.getString(TITLE_KEY));
            note.setContent(noteObject.getString(CONTENT_KEY));
            note.setCreateTime(noteObject.getLong(CREATE_TIME_KEY));
            note.setModifyTime(noteObject.getLong(MODIFY_TIME_KEY));
            notes[i] = note;
        }

        return new NotesData(version, exportTime, notes);
    }

    /**
     * 包装类，用于存储反序列化的结果
     */
    public static class NotesData {
        private String version;
        private long exportTime;
        private Note[] notes;

        public NotesData(String version, long exportTime, Note[] notes) {
            this.version = version;
            this.exportTime = exportTime;
            this.notes = notes;
        }

        public String getVersion() {
            return version;
        }

        public long getExportTime() {
            return exportTime;
        }

        public Note[] getNotes() {
            return notes;
        }
    }
}