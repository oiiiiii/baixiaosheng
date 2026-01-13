package com.baixiaosheng.inventory.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限申请工具类
 * 为什么这么写：封装权限申请逻辑，统一处理6.0+动态权限，简化录入页代码
 */
public class PermissionUtils {
    // 相机权限
    public static final int PERMISSION_CAMERA = 1001;
    // 存储权限（区分Android版本）
    public static final int PERMISSION_STORAGE = 1002;

    /**
     * 检查单个权限是否已授予
     */
    public static boolean checkPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查多个权限是否已授予
     */
    public static List<String> checkPermissions(Context context, String[] permissions) {
        List<String> deniedPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (!checkPermission(context, permission)) {
                deniedPermissions.add(permission);
            }
        }
        return deniedPermissions;
    }

    /**
     * 请求权限
     */
    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    /**
     * 获取相机权限数组（适配Android 13+）
     */
    public static String[] getCameraPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{android.Manifest.permission.CAMERA};
        } else {
            return new String[]{android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
    }

    /**
     * 获取存储权限数组（适配Android 13+）
     */
    public static String[] getStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{android.Manifest.permission.READ_MEDIA_IMAGES};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE};
        } else {
            return new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
    }
}