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

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类管理界面，显示所有分类列表
 */
public class CategoriesListActivity extends ListActivity {
    private static final String TAG = "CategoriesListActivity";
    
    private CategoryDataSource mCategoryDataSource;
    private List<Map<String, Object>> mDataList;
    private SimpleAdapter mAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories_list);
        
        // 初始化数据源
        mCategoryDataSource = new CategoryDataSource(this);
        
        // 初始化数据列表
        mDataList = new ArrayList<>();
        
        // 创建适配器
        mAdapter = new SimpleAdapter(
                this,
                mDataList,
                R.layout.categories_list_item,
                new String[]{"name", "count"},
                new int[]{R.id.category_name, R.id.category_count}
        );
        
        setListAdapter(mAdapter);
        
        // 注册上下文菜单
        registerForContextMenu(getListView());
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
    
    /**
     * 加载分类数据
     */
    private void loadData() {
        mDataList.clear();
        
        // 获取所有分类
        List<Category> categories = mCategoryDataSource.getAllCategories();
        
        // 添加默认分类（所有笔记）
        Map<String, Object> allNotesItem = new HashMap<>();
        allNotesItem.put("id", 0L);
        allNotesItem.put("name", "所有笔记");
        allNotesItem.put("count", "全部");
        mDataList.add(allNotesItem);
        
        // 添加各个分类
        for (Category category : categories) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", category.getId());
            item.put("name", category.getName());
            
            // 获取该分类下的笔记数量
            int count = mCategoryDataSource.getNotesCountByCategory(category.getId());
            item.put("count", String.valueOf(count));
            
            mDataList.add(item);
        }
        
        mAdapter.notifyDataSetChanged();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.categories_list_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.menu_add_category) {
            // 启动分类编辑界面，创建新分类
            Intent intent = new Intent(this, CategoryEditorActivity.class);
            startActivity(intent);
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        
        // 第一项是"所有笔记"，不允许编辑和删除
        if (info.position == 0) {
            return;
        }
        
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.categories_context_menu, menu);
        
        // 设置菜单标题
        @SuppressWarnings("unchecked")
        Map<String, Object> item = (Map<String, Object>) getListAdapter().getItem(info.position);
        String categoryName = (String) item.get("name");
        menu.setHeaderTitle(categoryName);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        
        // 第一项是"所有笔记"，不允许编辑和删除
        if (info.position == 0) {
            return super.onContextItemSelected(item);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) getListAdapter().getItem(info.position);
        long categoryId = (Long) map.get("id");
        String categoryName = (String) map.get("name");
        
        int itemId = item.getItemId();
        
        if (itemId == R.id.menu_edit_category) {
            // 编辑分类
            Intent intent = new Intent(this, CategoryEditorActivity.class);
            intent.putExtra(CategoryEditorActivity.EXTRA_CATEGORY_ID, categoryId);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.menu_delete_category) {
            // 删除分类
            deleteCategory(categoryId, categoryName);
            return true;
        }
        
        return super.onContextItemSelected(item);
    }
    
    /**
     * 删除分类
     * @param categoryId 分类ID
     * @param categoryName 分类名称
     */
    private void deleteCategory(long categoryId, String categoryName) {
        // 检查该分类下是否有笔记
        int noteCount = mCategoryDataSource.getNotesCountByCategory(categoryId);
        
        if (noteCount > 0) {
            Toast.makeText(this, "无法删除非空分类，请先清空该分类下的笔记", Toast.LENGTH_LONG).show();
            return;
        }
        
        // 删除分类
        int result = mCategoryDataSource.deleteCategory(categoryId);
        if (result > 0) {
            Toast.makeText(this, "分类 \"" + categoryName + "\" 已删除", Toast.LENGTH_SHORT).show();
            loadData(); // 刷新列表
        } else {
            Toast.makeText(this, "删除分类失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        @SuppressWarnings("unchecked")
        Map<String, Object> item = (Map<String, Object>) getListAdapter().getItem(position);
        long categoryId = (Long) item.get("id");
        String categoryName = (String) item.get("name");
        
        // 返回选中的分类信息
        Intent resultIntent = new Intent();
        resultIntent.putExtra("category_id", categoryId);
        resultIntent.putExtra("category_name", categoryName);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}