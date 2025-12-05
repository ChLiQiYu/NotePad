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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * CategoryDataSource数据访问类，负责分类的增删改查操作
 */
public class CategoryDataSource {
    private Context mContext;

    public CategoryDataSource(Context context) {
        this.mContext = context;
    }

    /**
     * 创建分类
     * @param category 分类对象
     * @return 新创建分类的ID，失败返回-1
     */
    public long createCategory(Category category) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Categories.COLUMN_NAME_NAME, category.getName());
        values.put(NotePad.Categories.COLUMN_NAME_CREATED_TIME, category.getCreatedTime());
        values.put(NotePad.Categories.COLUMN_NAME_MODIFIED_TIME, category.getModifiedTime());

        Uri uri = mContext.getContentResolver().insert(NotePad.Categories.CONTENT_URI, values);
        if (uri != null) {
            return Long.parseLong(uri.getLastPathSegment());
        }
        return -1;
    }

    /**
     * 更新分类
     * @param category 分类对象
     * @return 更新成功的记录数
     */
    public int updateCategory(Category category) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Categories.COLUMN_NAME_NAME, category.getName());
        values.put(NotePad.Categories.COLUMN_NAME_MODIFIED_TIME, category.getModifiedTime());

        String selection = NotePad.Categories._ID + " = ?";
        String[] selectionArgs = {String.valueOf(category.getId())};

        return mContext.getContentResolver().update(
                NotePad.Categories.CONTENT_URI,
                values,
                selection,
                selectionArgs
        );
    }

    /**
     * 删除分类
     * @param categoryId 分类ID
     * @return 删除成功的记录数
     */
    public int deleteCategory(long categoryId) {
        String selection = NotePad.Categories._ID + " = ?";
        String[] selectionArgs = {String.valueOf(categoryId)};

        return mContext.getContentResolver().delete(
                NotePad.Categories.CONTENT_URI,
                selection,
                selectionArgs
        );
    }

    /**
     * 查询所有分类
     * @return 分类列表
     */
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();

        String[] projection = {
                NotePad.Categories._ID,
                NotePad.Categories.COLUMN_NAME_NAME,
                NotePad.Categories.COLUMN_NAME_CREATED_TIME,
                NotePad.Categories.COLUMN_NAME_MODIFIED_TIME
        };

        Cursor cursor = mContext.getContentResolver().query(
                NotePad.Categories.CONTENT_URI,
                projection,
                null,
                null,
                NotePad.Categories.DEFAULT_SORT_ORDER
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Category category = new Category();
                category.setId(cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Categories._ID)));
                category.setName(cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Categories.COLUMN_NAME_NAME)));
                category.setCreatedTime(cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Categories.COLUMN_NAME_CREATED_TIME)));
                category.setModifiedTime(cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Categories.COLUMN_NAME_MODIFIED_TIME)));
                categories.add(category);
            }
            cursor.close();
        }

        return categories;
    }

    /**
     * 根据ID查询分类
     * @param id 分类ID
     * @return 分类对象，未找到返回null
     */
    public Category getCategoryById(long id) {
        String[] projection = {
                NotePad.Categories._ID,
                NotePad.Categories.COLUMN_NAME_NAME,
                NotePad.Categories.COLUMN_NAME_CREATED_TIME,
                NotePad.Categories.COLUMN_NAME_MODIFIED_TIME
        };

        String selection = NotePad.Categories._ID + " = ?";
        String[] selectionArgs = {String.valueOf(id)};

        Cursor cursor = mContext.getContentResolver().query(
                NotePad.Categories.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );

        Category category = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                category = new Category();
                category.setId(cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Categories._ID)));
                category.setName(cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Categories.COLUMN_NAME_NAME)));
                category.setCreatedTime(cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Categories.COLUMN_NAME_CREATED_TIME)));
                category.setModifiedTime(cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Categories.COLUMN_NAME_MODIFIED_TIME)));
            }
            cursor.close();
        }

        return category;
    }

    /**
     * 获取分类下的笔记数量
     * @param categoryId 分类ID
     * @return 笔记数量
     */
    public int getNotesCountByCategory(long categoryId) {
        String[] projection = {NotePad.Notes._ID};
        
        String selection = NotePad.Notes.COLUMN_NAME_CATEGORY_ID + " = ?";
        String[] selectionArgs = {String.valueOf(categoryId)};

        Cursor cursor = mContext.getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );

        int count = 0;
        if (cursor != null) {
            count = cursor.getCount();
            cursor.close();
        }

        return count;
    }
}