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
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12：检查READ_EXTERNAL_STORAGE（用于媒体文件访问）
            // 如果需要全文件访问，额外检查MANAGE_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 6.0-10 用 READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // 新增：检查是否有全文件访问权限（MANAGE_EXTERNAL_STORAGE）
    public static boolean hasManageExternalStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return android.os.Environment.isExternalStorageManager();
        }
        return true; // Android 10及以下不需要此权限
    }

    // 请求相机权限
    public static void requestCameraPermission(Fragment fragment, int requestCode) {
        fragment.requestPermissions(new String[]{android.Manifest.permission.CAMERA}, requestCode);
    }

    // 核心：存储权限请求（适配全版本）
    public static void requestStoragePermission(Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+：请求READ_MEDIA_IMAGES
            fragment.requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, requestCode);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0-12：请求READ_EXTERNAL_STORAGE
            fragment.requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, requestCode);
        }
        // Android 5.1及以下不需要请求权限
    }

    // 新增：Activity版本的存储权限请求
    public static void requestStoragePermission(AppCompatActivity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+：请求READ_MEDIA_IMAGES
            androidx.core.app.ActivityCompat.requestPermissions(activity,
                    new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, requestCode);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0-12：请求READ_EXTERNAL_STORAGE
            androidx.core.app.ActivityCompat.requestPermissions(activity,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, requestCode);
        }
    }

    // 判断是否为HarmonyOS
    private static boolean isHarmonyOS() {
        try {
            Class<?> clazz = Class.forName("com.huawei.system.BuildEx");
            return clazz != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // HarmonyOS特殊处理
    public static void requestStoragePermissionHarmony(Fragment fragment, int requestCode) {
        if (isHarmonyOS()) {
            fragment.requestPermissions(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            }, requestCode);
        } else {
            requestStoragePermission(fragment, requestCode);
        }
    }

    // 判断是否需要引导用户到设置页开启权限
    public static boolean shouldShowPermissionRationale(Fragment fragment, String permission) {
        return fragment.shouldShowRequestPermissionRationale(permission);
    }

    // Activity版本的shouldShowPermissionRationale
    public static boolean shouldShowPermissionRationale(AppCompatActivity activity, String permission) {
        return androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    // 跳转到应用权限设置页
    public static void goToAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }

    // 请求Android 11+的MANAGE_EXTERNAL_STORAGE权限（保留这个方法）
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

    // 检查是否有写入外部存储的权限（Android 9及以下）
    public static boolean hasWriteStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true; // Android 10+ 使用作用域存储，无需WRITE权限
        }
        return ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // 请求写入外部存储权限（仅Android 9及以下）
    public static void requestWriteStoragePermission(Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            fragment.requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
        }
    }

    // Activity版本的写入权限请求
    public static void requestWriteStoragePermission(AppCompatActivity activity, int requestCode) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            androidx.core.app.ActivityCompat.requestPermissions(activity,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
        }
    }
}