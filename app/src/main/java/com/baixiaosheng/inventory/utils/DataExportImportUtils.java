package com.baixiaosheng.inventory.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.baixiaosheng.inventory.database.DatabaseManager;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.database.entity.Location;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 数据导出/导入工具类
 * 支持：
 * 1. 导出物品数据（JSON）+ 图片文件 为ZIP包
 * 2. 导入ZIP包，校验数据格式，批量入库
 */
public class DataExportImportUtils {

    private static final String TAG = "DataExportImportUtils";
    private static final String JSON_FILE_NAME = "inventory_data.json";
    private static final String IMAGE_DIR_NAME = "images/";

    /**
     * 导出数据为ZIP文件
     * @param context 上下文
     * @param zipPath 导出的ZIP文件路径
     * @return 是否成功
     */
    public static boolean exportData(Context context, String zipPath) {
        try {
            // 1. 查询所有数据

            LiveData<List<Item>> itemLiveData = DatabaseManager.getInstance(context).getAllItems();
            List<Item> items = itemLiveData.getValue() == null ? new ArrayList<>() : itemLiveData.getValue();
            List<Category> categories = DatabaseManager.getInstance(context).getAllCategories();
            List<Location> locations = DatabaseManager.getInstance(context).getAllLocations();

            // 2. 将数据转换为JSON
            JSONObject root = new JSONObject();
            root.put("items", convertItemsToJson(items));
            root.put("categories", convertCategoriesToJson(categories));
            root.put("locations", convertLocationsToJson(locations));

            // 3. 创建ZIP文件
            File zipFile = new File(zipPath);
            if (zipFile.exists()) {
                zipFile.delete();
            }
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));

            // 4. 写入JSON文件
            ZipEntry jsonEntry = new ZipEntry(JSON_FILE_NAME);
            zos.putNextEntry(jsonEntry);
            zos.write(root.toString().getBytes("UTF-8"));
            zos.closeEntry();

            // 5. 写入图片文件
            for (Item item : items) {
                if (item.getImagePaths() == null || item.getImagePaths().isEmpty()) {
                    continue;
                }
                String[] imagePaths = item.getImagePaths().split(",");
                for (String path : imagePaths) {
                    File imageFile = new File(path);
                    if (!imageFile.exists()) {
                        continue;
                    }

                    // 图片在ZIP中的路径：images/物品UUID_文件名
                    String zipImagePath = IMAGE_DIR_NAME + item.getUuid() + "_" + imageFile.getName();
                    ZipEntry imageEntry = new ZipEntry(zipImagePath);
                    zos.putNextEntry(imageEntry);

                    // 读取并写入图片数据
                    FileInputStream fis = new FileInputStream(imageFile);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    fis.close();
                    zos.closeEntry();
                }
            }

            // 6. 关闭ZIP流
            zos.close();

            return true;
        } catch (Exception e) {
            Log.e(TAG, "导出数据失败", e);
            return false;
        }
    }

    /**
     * 导入ZIP文件中的数据
     * @param context 上下文
     * @param zipPath ZIP文件路径
     * @return 导入结果
     */
    public static ImportResult importData(Context context, String zipPath) {
        ImportResult result = new ImportResult();
        try {
            File zipFile = new File(zipPath);
            if (!zipFile.exists()) {
                result.setFailReason("ZIP文件不存在");
                return result;
            }

            // 1. 解压ZIP文件
            File tempDir = new File(context.getCacheDir(), "inventory_import_" + System.currentTimeMillis());
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            unzipFile(zipPath, tempDir.getAbsolutePath());

            // 2. 读取JSON文件
            File jsonFile = new File(tempDir, JSON_FILE_NAME);
            if (!jsonFile.exists()) {
                result.setFailReason("JSON数据文件不存在");
                deleteDir(tempDir);
                return result;
            }

            String jsonContent = readFileToString(jsonFile);
            JSONObject root = new JSONObject(jsonContent); // 原代码此处有错误，已修正

            // 3. 校验数据格式
            if (!root.has("items") || !root.has("categories") || !root.has("locations")) {
                result.setFailReason("数据格式错误，缺少核心字段");
                deleteDir(tempDir);
                return result;
            }

            // 4. 导入分类数据
            int categoryCount = importCategories(context, root.getJSONArray("categories"));
            // 5. 导入位置数据
            int locationCount = importLocations(context, root.getJSONArray("locations"));
            // 6. 导入物品数据（含图片）
            int itemCount = importItems(context, root.getJSONArray("items"), tempDir);

            result.setSuccessCount(categoryCount + locationCount + itemCount);
            result.setFailReason("导入成功");

            // 7. 删除临时目录
            deleteDir(tempDir);
        } catch (Exception e) {
            Log.e(TAG, "导入数据失败", e);
            result.setFailReason("导入异常：" + e.getMessage());
        }
        return result;
    }

    // ========== 私有辅助方法 ==========

    // 新增自定义文件读取方法
    /**
     * 自定义读取文件内容为字符串（替代系统FileUtils的缺失方法）
     * @param file 目标文件
     * @return 文件内容字符串
     * @throws IOException 读取异常
     */
    private static String readFileToString(File file) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    /**
     * 将物品列表转换为JSON数组
     */
    private static JSONArray convertItemsToJson(List<Item> items) throws Exception {
        JSONArray jsonArray = new JSONArray();
        for (Item item : items) {
            JSONObject json = new JSONObject();
            json.put("uuid", item.getUuid());
            json.put("name", item.getName());
            json.put("parentCategoryId", item.getParentCategoryId());
            json.put("childCategoryId", item.getChildCategoryId());
            json.put("locationId", item.getLocationId());
            json.put("validTime", item.getValidTime());
            json.put("count", item.getCount());
            json.put("imagePaths", item.getImagePaths());
            json.put("remark", item.getRemark());
            jsonArray.put(json);
        }
        return jsonArray;
    }

    /**
     * 将分类列表转换为JSON数组
     */
    private static JSONArray convertCategoriesToJson(List<Category> categories) throws Exception {
        JSONArray jsonArray = new JSONArray();
        for (Category category : categories) {
            JSONObject json = new JSONObject();
            json.put("parentId", category.getParentId());
            json.put("name", category.getName());
            jsonArray.put(json);
        }
        return jsonArray;
    }

    /**
     * 将位置列表转换为JSON数组
     */
    private static JSONArray convertLocationsToJson(List<Location> locations) throws Exception {
        JSONArray jsonArray = new JSONArray();
        for (Location location : locations) {
            JSONObject json = new JSONObject();
            json.put("name", location.getName());
            json.put("remark", location.getRemark());
            jsonArray.put(json);
        }
        return jsonArray;
    }

    /**
     * 导入分类数据
     */
    private static int importCategories(Context context, JSONArray jsonArray) throws Exception {
        int count = 0;
        DatabaseManager dbManager = DatabaseManager.getInstance(context);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject json = jsonArray.getJSONObject(i);
            Category category = new Category();
            category.setParentId(json.getLong("parentId"));
            category.setName(json.getString("name"));
            category.setCreateTime(System.currentTimeMillis());
            category.setUpdateTime(System.currentTimeMillis());

            // 避免重复插入
            List<Category> exist = dbManager.getCategoryByNameAndParentId(
                    category.getName(), category.getParentId());
            if (exist.isEmpty()) {
                dbManager.addCategory(category);
                count++;
            }
        }
        return count;
    }

    /**
     * 导入位置数据
     */
    private static int importLocations(Context context, JSONArray jsonArray) throws Exception {
        int count = 0;
        DatabaseManager dbManager = DatabaseManager.getInstance(context);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject json = jsonArray.getJSONObject(i);
            Location location = new Location();
            location.setName(json.getString("name"));
            location.setRemark(json.optString("remark", ""));
            location.setCreateTime(System.currentTimeMillis());
            location.setUpdateTime(System.currentTimeMillis());

            // 避免重复插入
            List<Location> exist = dbManager.getLocationByName(location.getName());
            if (exist.isEmpty()) {
                dbManager.addLocation(location);
                count++;
            }
        }
        return count;
    }

    /**
     * 导入物品数据
     */
    private static int importItems(Context context, JSONArray jsonArray, File tempDir) throws Exception {
        int count = 0;
        DatabaseManager dbManager = DatabaseManager.getInstance(context);
        File imageDir = new File(tempDir, IMAGE_DIR_NAME);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject json = jsonArray.getJSONObject(i);
            Item item = new Item();
            item.setUuid(json.getString("uuid"));
            item.setName(json.getString("name"));
            item.setParentCategoryId(json.getLong("parentCategoryId"));
            item.setChildCategoryId(json.getLong("childCategoryId"));
            item.setLocationId(json.getLong("locationId"));
            item.setValidTime(json.getLong("validTime"));
            item.setCount(json.getInt("count"));
            item.setRemark(json.optString("remark", ""));
            item.setCreateTime(System.currentTimeMillis());
            item.setUpdateTime(System.currentTimeMillis());
            item.setIsDeleted(0);

            // 处理图片路径
            String imagePaths = json.optString("imagePaths", "");
            if (!imagePaths.isEmpty() && imageDir.exists()) {
                String[] paths = imagePaths.split(",");
                StringBuilder newImagePaths = new StringBuilder();
                for (String path : paths) {
                    File oldImageFile = new File(path);
                    String imageName = item.getUuid() + "_" + oldImageFile.getName();
                    File newImageFile = new File(imageDir, imageName);
                    if (newImageFile.exists()) {
                        // 复制图片到应用私有目录
                        String appImagePath = copyImageToAppDir(context, newImageFile);
                        if (newImagePaths.length() > 0) {
                            newImagePaths.append(",");
                        }
                        newImagePaths.append(appImagePath);
                    }
                }
                item.setImagePaths(newImagePaths.toString());
            } else {
                item.setImagePaths("");
            }

            // 避免重复插入（按UUID判断）
            Item existItem = dbManager.getItemByUuid(item.getUuid());
            if (existItem == null) {
                dbManager.addItem(item);
                count++;
            }
        }
        return count;
    }

    /**
     * 复制图片到应用私有目录
     */
    private static String copyImageToAppDir(Context context, File sourceFile) throws IOException {
        File appImageDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "inventory");
        if (!appImageDir.exists()) {
            appImageDir.mkdirs();
        }

        File destFile = new File(appImageDir, sourceFile.getName());
        FileInputStream fis = new FileInputStream(sourceFile);
        FileOutputStream fos = new FileOutputStream(destFile);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }
        fis.close();
        fos.close();

        return destFile.getAbsolutePath();
    }

    /**
     * 解压ZIP文件
     */
    private static void unzipFile(String zipPath, String destPath) throws IOException {
        File destDir = new File(destPath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipPath)));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            File entryFile = new File(destPath, entry.getName());
            if (entry.isDirectory()) {
                entryFile.mkdirs();
                continue;
            }

            // 创建父目录
            File parentDir = entryFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 写入文件
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(entryFile));
            byte[] buffer = new byte[1024];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            bos.close();
            zis.closeEntry();
        }
        zis.close();
    }

    /**
     * 删除目录及子文件
     */
    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    /**
     * 选择ZIP文件（需实现文件选择器，此处简化）
     */
    public static void selectZipFile(Context context, OnFileSelectedListener listener) {
        // 新增：类型校验，避免强转异常
        if (!(context instanceof AppCompatActivity)) {
            Log.e(TAG, "selectZipFile error: Context must be AppCompatActivity");
            return;
        }
        // 此处可集成第三方文件选择器，或使用系统文件选择器
        // 简化实现：直接调用系统文件选择器
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        ((AppCompatActivity) context).startActivityForResult(Intent.createChooser(intent, "选择ZIP文件"), 1002);
    }

    // ========== 内部类 ==========

    /**
     * 导入结果实体
     */
    public static class ImportResult {
        private int successCount;
        private int failCount;
        private String failReason;

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public void setFailCount(int failCount) {
            this.failCount = failCount;
        }

        public String getFailReason() {
            return failReason;
        }

        public void setFailReason(String failReason) {
            this.failReason = failReason;
        }
    }

    /**
     * 文件选择回调
     */
    public interface OnFileSelectedListener {
        void onFileSelected(String filePath);
    }
}