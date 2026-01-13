package com.baixiaosheng.inventory.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static final int MAX_WIDTH = 800; // 压缩后最大宽度
    private static final int MAX_HEIGHT = 800; // 压缩后最大高度
    private static final int QUALITY = 80; // 压缩质量

    // 修复：获取拍照文件（确保文件路径正确，权限可访问）
    public static File createImageFile(Context context) throws IOException {
        // 1. 生成唯一文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        // 2. 获取应用私有存储目录（无需动态权限）
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        // 3. 创建临时文件
        File imageFile = File.createTempFile(
                imageFileName,  // 前缀
                ".jpg",         // 后缀
                storageDir      // 存储目录
        );
        return imageFile;
    }

    // 修复：图片解码+旋转矫正（解决拍照后图片旋转、解码失败问题）
    public static Bitmap decodeImage(String filePath) {
        try {
            // 1. 获取图片宽高（不加载到内存）
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);
            // 2. 计算采样率（压缩图片）
            options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);
            // 3. 加载图片到内存
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
            if (bitmap == null) {
                Log.e(TAG, "图片解码后为空，filePath：" + filePath);
                return null;
            }
            // 4. 矫正图片旋转角度
            int rotateAngle = getImageRotateAngle(filePath);
            if (rotateAngle != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotateAngle);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "图片解码失败：" + e.getMessage());
            return null;
        }
    }

    // 计算图片采样率（压缩）
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int width = options.outWidth;
        final int height = options.outHeight;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    // 获取图片旋转角度
    private static int getImageRotateAngle(String filePath) {
        int rotateAngle = 0;
        try {
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotateAngle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotateAngle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotateAngle = 270;
                    break;
                default:
                    rotateAngle = 0;
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, "获取图片旋转角度失败：" + e.getMessage());
        }
        return rotateAngle;
    }

    // 保存图片到指定路径
    public static boolean saveBitmap(Bitmap bitmap, String filePath) {
        FileOutputStream fos = null;
        try {
            File file = new File(filePath);
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            if (file.exists()) {
                file.delete();
            }
            fos = new FileOutputStream(file);
            return bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, fos);
        } catch (Exception e) {
            Log.e(TAG, "保存图片失败：" + e.getMessage());
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 从Uri获取图片路径（适配Android 10+）
    public static String getPathFromUri(Context context, Uri uri) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) {
                Log.e(TAG, "Uri打开输入流失败");
                return null;
            }
            File tempFile = createImageFile(context);
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            is.close();
            fos.close();
            return tempFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Uri转路径失败：" + e.getMessage());
            return null;
        }
    }
}