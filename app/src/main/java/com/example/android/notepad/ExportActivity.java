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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

// 导入R类
import com.example.android.notepad.R;

/**
 * ExportActivity负责导出功能的用户界面展示
 * 提供导出选项设置并调用ExportManager执行导出操作
 *
 * API 23 专用修复说明：
 * 1. 完全移除 Android 11+ 专属API (R/API 30+)
 * 2. 重构权限系统，仅使用API 23支持的运行时权限
 * 3. 修复Environment路径获取方式
 * 4. 移除所有高版本Settings常量引用
 */
public class ExportActivity extends Activity {
    private EditText fileNameEditText;
    private Button exportButton;
    private ProgressDialog progressDialog;
    private ExportManager exportManager;

    // 仅保留API 23支持的权限请求码
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // 添加待导出文件的成员变量
    private String pendingFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        // 初始化视图
        initViews();

        // 初始化导出管理器
        exportManager = new ExportManager(getContentResolver());

        // 设置导出按钮点击事件
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportNotes();
            }
        });
    }

    /**
     * 初始化界面视图
     */
    private void initViews() {
        fileNameEditText = (EditText) findViewById(R.id.file_name_edit_text);
        exportButton = (Button) findViewById(R.id.export_button);

        // 设置默认文件名
        String defaultFileName = "notes_backup_" + System.currentTimeMillis() + ".json";
        fileNameEditText.setText(defaultFileName);
    }

    /**
     * 执行导出操作
     */
    private void exportNotes() {
        String fileName = fileNameEditText.getText().toString().trim();

        if (fileName.isEmpty()) {
            Toast.makeText(this, "请输入文件名", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查存储权限 (仅API 23+需要运行时权限)
        if (checkStoragePermission()) {
            performExport(fileName);
        } else {
            pendingFileName = fileName; // 保存待处理的文件名
            requestStoragePermission();
        }
    }

    /**
     * 检查存储权限 (仅使用API 23支持的方法)
     */
    private boolean checkStoragePermission() {
        // API 23+ 需要动态检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int writePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return writePermission == PackageManager.PERMISSION_GRANTED;
        }
        // API <23 默认有权限
        return true;
    }

    /**
     * 请求存储权限 (仅使用API 23支持的方法)
     */
    private void requestStoragePermission() {
        // 仅API 23+需要动态请求
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, PERMISSION_REQUEST_CODE);
        } else {
            // 低版本直接执行导出
            if (pendingFileName != null) {
                performExport(pendingFileName);
                pendingFileName = null;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，使用保存的文件名执行导出
                if (pendingFileName != null) {
                    performExport(pendingFileName);
                    pendingFileName = null;
                }
            } else {
                // 权限被拒绝，提示用户
                showPermissionDeniedDialog();
            }
        }
    }

    /**
     * 显示权限被拒绝的对话框
     */
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("权限被拒绝")
                .setMessage("导出功能需要存储权限才能保存文件到设备。您可以在设置中手动授予权限。")
                .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 跳转到应用设置页面 (API 23兼容方式)
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .show();
    }

    /**
     * 执行实际的导出操作
     */
    private void performExport(String fileName) {
        // 创建输出文件 (使用API 23兼容路径)
        File outputFile = createExportFile(fileName);

        // 显示进度对话框
        showProgressDialog();

        // 执行导出操作
        exportManager.exportNotes(outputFile, new ExportManager.ExportCallback() {
            @Override
            public void onSuccess(File outputFile) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissProgressDialog();
                        showSuccessDialog(outputFile.getAbsolutePath());
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
     * 创建导出文件 (API 23兼容实现)
     */
    private File createExportFile(String fileName) {
        // 确保文件名有.json扩展名
        if (!fileName.toLowerCase().endsWith(".json")) {
            fileName += ".json";
        }

        // API 23: 使用Environment.getExternalStoragePublicDirectory
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        // 确保目录存在
        if (!downloadDir.exists()) {
            boolean created = downloadDir.mkdirs();
            if (!created) {
                // 如果创建目录失败，尝试使用应用私有目录
                downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (downloadDir != null && !downloadDir.exists()) {
                    downloadDir.mkdirs();
                }
            }
        }

        return new File(downloadDir, fileName);
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("导出笔记");
        progressDialog.setMessage("正在导出...");
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
            progressDialog = null; // 避免内存泄漏
        }
    }

    /**
     * 显示成功对话框
     */
    private void showSuccessDialog(String filePath) {
        new AlertDialog.Builder(this)
                .setTitle("导出成功")
                .setMessage("笔记已成功导出到:\n" + filePath)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 导出成功后返回主页面
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }



    /**
     * 显示错误对话框
     */
    private void showErrorDialog(String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle("导出失败")
                .setMessage("导出过程中发生错误:\n" + (errorMessage != null ? errorMessage : "未知错误"))
                .setPositiveButton("确定", null)
                .setNeutralButton("重试", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (pendingFileName != null) {
                            exportNotes();
                        }
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        dismissProgressDialog();
        super.onDestroy();
    }
}