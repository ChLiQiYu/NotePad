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
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

/**
 * NoteDataSource类封装了对NotePadProvider的数据访问操作
 * 提供统一的数据查询和操作接口
 */
public class NoteDataSource {
    private ContentResolver contentResolver;

    public NoteDataSource(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    /**
     * 获取所有笔记数据
     * @return 笔记列表
     */
    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        
        // 查询所有笔记数据
        Cursor cursor = contentResolver.query(
                NotePad.Notes.CONTENT_URI,
                new String[]{
                        NotePad.Notes._ID,
                        NotePad.Notes.COLUMN_NAME_TITLE,
                        NotePad.Notes.COLUMN_NAME_NOTE,
                        NotePad.Notes.COLUMN_NAME_CREATE_DATE,
                        NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
                },
                null,
                null,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    Note note = new Note();
                    note.setId(cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Notes._ID)));
                    note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_TITLE)));
                    note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_NOTE)));
                    note.setCreateTime(cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_CREATE_DATE)));
                    note.setModifyTime(cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)));
                    notes.add(note);
                }
            } finally {
                cursor.close();
            }
        }
        
        return notes;
    }

    /**
     * 根据ID获取单个笔记
     * @param id 笔记ID
     * @return 笔记对象，如果不存在返回null
     */
    public Note getNoteById(long id) {
        Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, id);
        
        Cursor cursor = contentResolver.query(
                noteUri,
                new String[]{
                        NotePad.Notes._ID,
                        NotePad.Notes.COLUMN_NAME_TITLE,
                        NotePad.Notes.COLUMN_NAME_NOTE,
                        NotePad.Notes.COLUMN_NAME_CREATE_DATE,
                        NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
                },
                null,
                null,
                null
        );

        Note note = null;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    note = new Note();
                    note.setId(cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Notes._ID)));
                    note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_TITLE)));
                    note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_NOTE)));
                    note.setCreateTime(cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_CREATE_DATE)));
                    note.setModifyTime(cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)));
                }
            } finally {
                cursor.close();
            }
        }
        
        return note;
    }

    /**
     * 插入新的笔记
     * @param note 笔记对象
     * @return 新插入笔记的URI，如果失败返回null
     */
    public Uri insertNote(Note note) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_TITLE, note.getTitle());
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, note.getContent());
        values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, note.getCreateTime());
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, note.getModifyTime());

        return contentResolver.insert(NotePad.Notes.CONTENT_URI, values);
    }

    /**
     * 更新笔记
     * @param note 要更新的笔记对象
     * @return 更新的记录数
     */
    public int updateNote(Note note) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_TITLE, note.getTitle());
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, note.getContent());
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, note.getModifyTime());

        Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, note.getId());
        
        return contentResolver.update(noteUri, values, null, null);
    }

    /**
     * 删除笔记
     * @param id 要删除的笔记ID
     * @return 删除的记录数
     */
    public int deleteNote(long id) {
        Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, id);
        return contentResolver.delete(noteUri, null, null);
    }
}