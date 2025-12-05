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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

/**
 * ImportActivity负责导入功能的用户界面展示
 * 提供文件选择和导入选项并调用ImportManager执行导入操作
 */
public class ImportActivity extends Activity {
    private TextView selectedFileTextView;
    private Button selectFileButton;
    private Button importButton;
    private ProgressDialog progressDialog;
    private ImportManager importManager;
    private File selectedFile;
    
    private static final int REQUEST_CODE_SELECT_FILE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        // 初始化视图
        initViews();

        // 初始化导入管理器
        importManager = new ImportManager(getContentResolver());
        
        // 检查存储权限
        checkStoragePermission();

        // 设置选择文件按钮点击事件
        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFile();
            }
        });

        // 设置导入按钮点击事件
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importNotes();
            }
        });
    }
    
    /**
     * 检查存储权限
     */
    private void checkStoragePermission() {
        // 在Android 6.0及以上版本中，可能需要动态请求权限
        // 但在这个简单的示例中，我们依赖于清单中的权限声明
    }
    
    /**
     * 初始化界面视图
     */
    private void initViews() {
        selectedFileTextView = (TextView) findViewById(R.id.selected_file_text_view);
        selectFileButton = (Button) findViewById(R.id.select_file_button);
        importButton = (Button) findViewById(R.id.import_button);
        
        // 初始状态下禁用导入按钮
        importButton.setEnabled(false);
    }

    /**
     * 选择文件
     */
    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // 允许选择所有类型的文件
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
            startActivityForResult(
                Intent.createChooser(intent, "选择备份文件"),
                REQUEST_CODE_SELECT_FILE
            );
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "请安装文件管理器以选择文件", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                try {
                    // 获取选择的文件路径
                    selectedFile = FileUtils.getFileFromUri(this, data.getData());
                    if (selectedFile != null && selectedFile.exists()) {
                        selectedFileTextView.setText(selectedFile.getName());
                        importButton.setEnabled(true);
                    } else {
                        Toast.makeText(this, "无法访问选择的文件", Toast.LENGTH_SHORT).show();
                        selectedFileTextView.setText("未选择文件");
                        importButton.setEnabled(false);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "选择文件时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    selectedFileTextView.setText("未选择文件");
                    importButton.setEnabled(false);
                }
            }
        }
    }

    /**
     * 执行导入操作
     */
    private void importNotes() {
        if (selectedFile == null || !selectedFile.exists()) {
            Toast.makeText(this, "请选择有效的备份文件", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示进度对话框
        showProgressDialog();

        // 执行导入操作
        importManager.importNotes(selectedFile, new ImportManager.ImportCallback() {
            @Override
            public void onSuccess(int importedCount) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissProgressDialog();
                        showSuccessDialog(importedCount);
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissProgressDialog();
                        showErrorDialog(error.getMessage());
                    }
                });
            }

            @Override
            public void onProgress(int progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateProgress(progress);
                    }
                });
            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("导入笔记");
        progressDialog.setMessage("正在导入...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    /**
     * 更新进度
     */
    private void updateProgress(int progress) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setProgress(progress);
        }
    }

    /**
     * 隐藏进度对话框
     */
    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * 显示成功对话框
     */
    private void showSuccessDialog(int importedCount) {
        new AlertDialog.Builder(this)
                .setTitle("导入成功")
                .setMessage("成功导入 " + importedCount + " 条笔记")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 导入成功后返回主页面
                        finish();
                    }
                })
                .show();
    }

    /**
     * 显示错误对话框
     */
    private void showErrorDialog(String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle("导入失败")
                .setMessage("导入过程中发生错误:\n" + errorMessage)
                .setPositiveButton("确定", null)
                .show();
    }
}