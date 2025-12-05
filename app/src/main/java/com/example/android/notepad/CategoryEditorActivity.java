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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 分类编辑界面，用于创建或编辑分类
 */
public class CategoryEditorActivity extends Activity {
    public static final String EXTRA_CATEGORY_ID = "category_id";
    
    private static final String TAG = "CategoryEditorActivity";
    private static final int STATE_INSERT = 0;
    private static final int STATE_EDIT = 1;
    
    private int mState;
    private long mCategoryId;
    private Category mCategory;
    private CategoryDataSource mCategoryDataSource;
    private EditText mNameEditText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_editor);
        
        // 初始化数据源
        mCategoryDataSource = new CategoryDataSource(this);
        
        // 获取界面元素
        mNameEditText = (EditText) findViewById(R.id.category_name);
        
        // 处理传入的意图
        Intent intent = getIntent();
        if (Intent.ACTION_EDIT.equals(intent.getAction())) {
            // 编辑模式
            mState = STATE_EDIT;
            mCategoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1);
            if (mCategoryId != -1) {
                mCategory = mCategoryDataSource.getCategoryById(mCategoryId);
                if (mCategory != null) {
                    mNameEditText.setText(mCategory.getName());
                    setTitle("编辑分类");
                } else {
                    Toast.makeText(this, "找不到指定的分类", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } else {
                Toast.makeText(this, "无效的分类ID", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            // 插入模式
            mState = STATE_INSERT;
            mCategory = new Category();
            setTitle("新建分类");
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.category_editor_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.menu_save) {
            saveCategory();
            return true;
        } else if (itemId == R.id.menu_cancel) {
            finish();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 保存分类
     */
    private void saveCategory() {
        String name = mNameEditText.getText().toString().trim();
        
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入分类名称", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查分类名称是否已存在
        if (isCategoryNameExists(name)) {
            Toast.makeText(this, "分类名称已存在", Toast.LENGTH_SHORT).show();
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        if (mState == STATE_INSERT) {
            // 创建新分类
            mCategory.setName(name);
            mCategory.setCreatedTime(currentTime);
            mCategory.setModifiedTime(currentTime);
            
            long result = mCategoryDataSource.createCategory(mCategory);
            if (result != -1) {
                Toast.makeText(this, "分类创建成功", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "分类创建失败", Toast.LENGTH_SHORT).show();
            }
        } else if (mState == STATE_EDIT) {
            // 更新现有分类
            mCategory.setName(name);
            mCategory.setModifiedTime(currentTime);
            
            int result = mCategoryDataSource.updateCategory(mCategory);
            if (result > 0) {
                Toast.makeText(this, "分类更新成功", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "分类更新失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * 检查分类名称是否已存在
     * @param name 分类名称
     * @return 如果存在返回true，否则返回false
     */
    private boolean isCategoryNameExists(String name) {
        // 如果是编辑模式且名称没有改变，则不认为重复
        if (mState == STATE_EDIT && mCategory.getName().equals(name)) {
            return false;
        }
        
        // 检查是否有其他同名分类
        for (Category category : mCategoryDataSource.getAllCategories()) {
            if (category.getName().equals(name)) {
                return true;
            }
        }
        
        return false;
    }
}