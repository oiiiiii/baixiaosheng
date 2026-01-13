package com.baixiaosheng.inventory.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 图片处理工具类
 * 为什么这么写：封装图片拍照、选择、压缩、Uri处理逻辑，避免代码冗余，适配不同Android版本的文件访问规则
 */
public class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static final String IMAGE_DIR = "InventoryImages";
    private static final String IMAGE_FORMAT = "JPEG";
    private static final int COMPRESS_QUALITY = 80;

    /**
     * 新增：将Uri转换为本地文件真实路径（适配相册选择的图片Uri）
     * @param context 上下文
     * @param uri 图片Uri（如相册返回的content:// 类型Uri）
     * @return 本地文件路径，转换失败返回null
     */
    public static String getPathFromUri(Context context, Uri uri) {
        if (uri == null) {
            Log.e(TAG, "Uri is null");
            return null;
        }

        // 1. 处理 file:// 类型Uri（直接返回路径）
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        // 2. 处理 content:// 类型Uri（从MediaStore查询真实路径）
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                String path = cursor.getString(columnIndex);
                // 验证路径是否存在
                if (new File(path).exists()) {
                    return path;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Uri转路径失败: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // 3. 适配Android 10+（MediaStore返回的路径可能不可直接访问，返回null时走Bitmap流处理）
        return null;
    }

    /**
     * 创建拍照的临时文件
     */
    public static File createImageFile(Context context) throws IOException {
        // 生成唯一文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        // 获取应用私有存储目录
        File storageDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_DIR);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        // 创建临时文件
        File imageFile = File.createTempFile(
                imageFileName,  /* 前缀 */
                ".jpg",         /* 后缀 */
                storageDir      /* 存储目录 */
        );
        return imageFile;
    }

    /**
     * 获取拍照的Intent
     */
    public static Intent getTakePhotoIntent(Context context, File photoFile, Uri[] photoUri) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            // 获取文件Uri（适配Android 7.0+ FileProvider）
            Uri photoURI = FileProvider.getUriForFile(
                    context,
                    "com.baixiaosheng.inventory.fileprovider", // 和AndroidManifest中配置的一致
                    photoFile
            );
            photoUri[0] = photoURI;
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        }
        return takePictureIntent;
    }

    /**
     * 获取选择图片的Intent
     */
    public static Intent getChoosePhotoIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        return Intent.createChooser(intent, "选择图片");
    }

    /**
     * 压缩图片并保存到本地
     */
    public static String compressAndSaveImage(Context context, Bitmap bitmap) {
        try {
            File storageDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_DIR);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            // 生成唯一文件名
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
            String fileName = "COMPRESS_" + timeStamp + ".jpg";
            File outputFile = new File(storageDir, fileName);

            // 压缩并保存图片
            FileOutputStream out = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, out);
            out.flush();
            out.close();

            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "压缩图片失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据路径获取Bitmap（防止OOM）
     */
    public static Bitmap getBitmapFromPath(String path, int reqWidth, int reqHeight) {
        // 先获取图片尺寸，不加载到内存
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // 计算缩放比例
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // 加载图片
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    /**
     * 计算图片缩放比例
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}