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

/**
 * Note数据模型类，用于表示单个笔记的数据结构
 */
public class Note {
    private long id;
    private String title;
    private String content;
    private long createTime;
    private long modifyTime;
    private long categoryId;

    public Note() {
    }

    public Note(long id, String title, String content, long createTime, long modifyTime) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createTime = createTime;
        this.modifyTime = modifyTime;
        this.categoryId = 0; // Default category ID
    }

    public Note(long id, String title, String content, long createTime, long modifyTime, long categoryId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createTime = createTime;
        this.modifyTime = modifyTime;
        this.categoryId = categoryId;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(long modifyTime) {
        this.modifyTime = modifyTime;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Note note = (Note) o;

        if (id != note.id) return false;
        if (createTime != note.createTime) return false;
        if (modifyTime != note.modifyTime) return false;
        if (categoryId != note.categoryId) return false;
        if (title != null ? !title.equals(note.title) : note.title != null) return false;
        return content != null ? content.equals(note.content) : note.content == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (int) (createTime ^ (createTime >>> 32));
        result = 31 * result + (int) (modifyTime ^ (modifyTime >>> 32));
        result = 31 * result + (int) (categoryId ^ (categoryId >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", createTime=" + createTime +
                ", modifyTime=" + modifyTime +
                ", categoryId=" + categoryId +
                '}';
    }
}