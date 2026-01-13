package com.baixiaosheng.inventory.utils;

import android.content.Context;
import android.content.pm.PackageManager;
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            fragment.requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, requestCode);
        } else {
            fragment.requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, requestCode);
        }
    }
}