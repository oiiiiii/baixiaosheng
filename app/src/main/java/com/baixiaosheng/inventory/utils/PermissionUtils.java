package com.baixiaosheng.inventory.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/**
 * 权限工具类（适配Android 6.0+动态权限）
 */
public class PermissionUtils {
    // 检查相机权限
    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    // 检查存储/图片权限（适配Android 13+）
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 用READ_MEDIA_IMAGES
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            // 低版本用READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // 请求相机权限
    public static void requestCameraPermission(Fragment fragment, int requestCode) {
        fragment.requestPermissions(new String[]{android.Manifest.permission.CAMERA}, requestCode);
    }

    // 请求存储/图片权限
    public static void requestStoragePermission(Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            fragment.requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, requestCode);
        } else {
            fragment.requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, requestCode);
        }
    }

    // 新增：请求Android 11+的MANAGE_EXTERNAL_STORAGE权限
    public static void requestManageExternalStorage(AppCompatActivity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // 跳转到系统设置页面申请权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivityForResult(intent, requestCode);
            } catch (Exception e) {
                // 兼容异常情况，跳转到通用存储设置页面
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(intent, requestCode);
            }
        }
    }
}