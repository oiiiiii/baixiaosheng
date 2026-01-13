package com.baixiaosheng.inventory.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 图片工具类（修复解码失败、Android 10+路径适配）
 */
public class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 800;
    private static final int QUALITY = 80;

    /**
     * 创建图片文件（应用私有目录，无需外部权限）
     */
    public static File createImageFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
        String fileName = "IMG_" + timeStamp;
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    /**
     * 解码图片（修复旋转、压缩、OOM问题）
     */
    public static Bitmap decodeImage(String filePath) {
        if (filePath == null || !new File(filePath).exists()) {
            Log.e(TAG, "图片文件不存在：" + filePath);
            return null;
        }

        try {
            // 第一步：获取图片尺寸（不加载到内存）
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);

            // 第二步：计算采样率
            options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // 减少内存占用
            options.inPurgeable = true;

            // 第三步：加载图片
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
            if (bitmap == null) {
                Log.e(TAG, "图片解码为空：" + filePath);
                return null;
            }

            // 第四步：矫正旋转角度
            int rotate = getRotateAngle(filePath);
            if (rotate != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotate);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), matrix, true);
            }

            return bitmap;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "图片解码OOM：" + e.getMessage());
            System.gc(); // 触发垃圾回收
            return null;
        } catch (Exception e) {
            Log.e(TAG, "图片解码失败：" + e.getMessage());
            return null;
        }
    }

    /**
     * 计算采样率（压缩图片）
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * 获取图片旋转角度
     */
    private static int getRotateAngle(String filePath) {
        try {
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException e) {
            Log.e(TAG, "获取旋转角度失败：" + e.getMessage());
            return 0;
        }
    }

    /**
     * Uri转真实路径（适配Android 10+）
     */
    public static String getPathFromUri(Context context, Uri uri) {
        if (uri == null) return null;

        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;

            // 复制到应用私有目录
            File tempFile = createImageFile(context);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                return tempFile.getAbsolutePath();
            }
        } catch (Exception e) {
            Log.e(TAG, "Uri转路径失败：" + e.getMessage());
            return null;
        }
    }

    /**
     * 保存Bitmap到文件
     */
    public static boolean saveBitmap(Bitmap bitmap, String filePath) {
        if (bitmap == null || filePath == null) return false;

        File file = new File(filePath);
        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            return bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, fos);
        } catch (Exception e) {
            Log.e(TAG, "保存图片失败：" + e.getMessage());
            return false;
        }
    }
}