package com.baixiaosheng.inventory.view.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.utils.DataExportImportUtils;
import com.baixiaosheng.inventory.utils.PermissionUtils;

import java.io.File;

/**
 * 数据管理页面
 * 实现数据导出（ZIP打包）、导入（ZIP解压+校验）
 */
public class DataManageActivity extends AppCompatActivity {

    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private Button btnExport, btnImport;
    private TextView tvStatus;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_manage);

        // 绑定控件
        btnExport = findViewById(R.id.btn_export_data);
        btnImport = findViewById(R.id.btn_import_data);
        tvStatus = findViewById(R.id.tv_status);

        // 初始化进度对话框
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // 绑定事件
        bindEvents();
    }

    private void bindEvents() {
        // 导出数据
        btnExport.setOnClickListener(v -> {
            // 检查存储权限
            if (checkStoragePermission()) {
                exportData();
            } else {
                requestStoragePermission();
            }
        });

        // 导入数据
        btnImport.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                importData();
            } else {
                requestStoragePermission();
            }
        });
    }

    /**
     * 检查存储权限
     */
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 无需申请READ_EXTERNAL_STORAGE，直接检查MANAGE_EXTERNAL_STORAGE
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * 请求存储权限
     */
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            PermissionUtils.requestManageExternalStorage(this, REQUEST_STORAGE_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        }
    }

    /**
     * 导出数据
     */
    private void exportData() {
        progressDialog.setMessage("正在导出数据...");
        progressDialog.show();

        new Thread(() -> {
            try {
                // 默认导出路径：内部存储/Download/
                String exportPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        + File.separator + "inventory_data_" + System.currentTimeMillis() + ".zip";

                // 执行导出
                boolean success = DataExportImportUtils.exportData(this, exportPath);

                // 更新UI
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (success) {
                        tvStatus.setText("导出成功：" + exportPath);
                        Toast.makeText(this, "导出成功，文件路径：" + exportPath, Toast.LENGTH_LONG).show();
                    } else {
                        tvStatus.setText("导出失败");
                        Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    tvStatus.setText("导出异常：" + e.getMessage());
                    Toast.makeText(this, "导出异常：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 导入数据
     */
    private void importData() {
        // 选择ZIP文件（需实现文件选择器）
        DataExportImportUtils.selectZipFile(this, filePath -> {
            progressDialog.setMessage("正在导入数据...");
            progressDialog.show();

            new Thread(() -> {
                try {
                    // 执行导入
                    DataExportImportUtils.ImportResult result = DataExportImportUtils.importData(this, filePath);

                    // 更新UI
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        tvStatus.setText(String.format("导入结果：成功%d条，失败%d条\n失败原因：%s",
                                result.getSuccessCount(), result.getFailCount(), result.getFailReason()));
                        Toast.makeText(this, "导入完成", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        tvStatus.setText("导入异常：" + e.getMessage());
                        Toast.makeText(this, "导入异常：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                    e.printStackTrace();
                }
            }).start();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限授予
                tvStatus.setText("存储权限已授予");
            } else {
                tvStatus.setText("存储权限被拒绝，无法导出/导入数据");
                Toast.makeText(this, "存储权限被拒绝，无法导出/导入数据", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_STORAGE_PERMISSION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 检查权限是否授予
            if (Environment.isExternalStorageManager()) {
                tvStatus.setText("存储权限已授予");
            } else {
                tvStatus.setText("存储权限被拒绝，无法导出/导入数据");
                Toast.makeText(this, "存储权限被拒绝，无法导出/导入数据", Toast.LENGTH_SHORT).show();
            }
        }
    }
}