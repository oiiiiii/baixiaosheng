package com.baixiaosheng.inventory.database;

import android.content.Context;


import androidx.lifecycle.LiveData;

import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.database.entity.Location;
import com.baixiaosheng.inventory.database.entity.Recycle;

import java.util.List;

/**
 * 数据库操作工具类（封装所有增删改查，对外提供统一接口）
 * 简化上层调用，无需直接操作Dao接口
 */
public class DatabaseManager {
    private static DatabaseManager INSTANCE;
    private final InventoryDatabase db;

    // 私有构造函数
    private DatabaseManager(Context context) {
        db = InventoryDatabase.getInstance(context);
    }

    // 单例获取
    public static DatabaseManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DatabaseManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DatabaseManager(context);
                }
            }
        }
        return INSTANCE;
    }



    // ==================== 分类表操作 ====================
    public long addCategory(Category category) {
        // 设置创建/更新时间为当前时间
        long currentTime = System.currentTimeMillis();
        category.setCreateTime(currentTime);
        category.setUpdateTime(currentTime);
        return db.categoryDao().insertCategory(category);
    }

    public int updateCategory(Category category) {
        // 更新时间为当前时间
        category.setUpdateTime(System.currentTimeMillis());
        return db.categoryDao().updateCategory(category);
    }

    public int deleteCategory(Category category) {
        return db.categoryDao().deleteCategory(category);
    }

    public Category getCategoryById(long id) {
        return db.categoryDao().getCategoryById(id);
    }

    public List<Category> getParentCategories(long parentId) {
        return db.categoryDao().getParentCategories(parentId);
    }

    public List<Category> getChildCategories(long parentId) {
        return db.categoryDao().getChildCategories(parentId);
    }
    public List<Category> getChildCategoriesByParentId(long parentId) {
        return db.categoryDao().getChildCategoriesByParentId(parentId);
    }

    public List<Category> getAllCategories() {
        return db.categoryDao().getAllCategories();
    }

    // 新增：根据名称模糊查询分类（适配DefaultDataUtils）
    public List<Category> getCategoryByName(String name) {
        return db.categoryDao().searchCategory(name);
    }

    // 新增：根据名称和父ID查询分类（供导入工具类使用）
    public List<Category> getCategoryByNameAndParentId(String name, long parentId) {
        return db.categoryDao().getCategoryByNameAndParentId(name, parentId);
    }

    // ==================== 位置表操作 ====================
    public long addLocation(Location location) {
        long currentTime = System.currentTimeMillis();
        location.setCreateTime(currentTime);
        location.setUpdateTime(currentTime);
        return db.locationDao().insertLocation(location);
    }

    public int updateLocation(Location location) {
        location.setUpdateTime(System.currentTimeMillis());
        return db.locationDao().updateLocation(location);
    }

    public int deleteLocation(Location location) {
        return db.locationDao().deleteLocation(location);
    }

    public Location getLocationById(long id) {
        return db.locationDao().getLocationById(id);
    }

    // 精确查询位置（按名称，供导入去重使用）
    public List<Location> getLocationByName(String name) {
        return db.locationDao().getLocationByName(name);
    }

    public List<Location> getAllLocations() {
        return db.locationDao().getAllLocations();
    }

    // ==================== 物品表操作 ====================
    public long addItem(Item item) {
        long currentTime = System.currentTimeMillis();
        item.setCreateTime(currentTime);
        item.setUpdateTime(currentTime);
        return db.itemDao().insertItem(item)[0];
    }

    public int updateItem(Item item) {
        item.setUpdateTime(System.currentTimeMillis());
        return db.itemDao().updateItem(item);
    }

    public int deleteItem(Item item) {
        return db.itemDao().deleteItem(item);
    }

    public Item getItemById(long id) {
        return db.itemDao().getItemById(id);
    }

    // 新增：根据UUID查询物品（供导入去重使用）
    public Item getItemByUuid(String uuid) {
        return db.itemDao().getItemByUuid(uuid);
    }
    public LiveData<List<Item>> getAllItems() {
        return db.itemDao().getAllItems();
    }

    public List<Item> getExpiredItems(long currentTime, long startDate, long endDate, int isDeleted) {
        return db.itemDao().getExpiredItems(currentTime, startDate, endDate, isDeleted);
    }

    public List<Item> searchExpiredItems(String keyword, long currentTime, long startDate, long endDate, int isDeleted) {
        return db.itemDao().searchExpiredItems(keyword, currentTime, startDate, endDate, isDeleted);
    }

    // 重载：通过itemId标记物品为删除（逻辑删除）
    public void markItemAsDeleted(Long itemId) {
        // 先通过ID查询物品，获取UUID
        Item item = db.itemDao().getItemById(itemId);
        if (item != null) {
            long currentTime = System.currentTimeMillis();
            db.itemDao().markItemAsDeleted(item.getUuid(), currentTime);
        }
    }

    // ==================== 回收站表操作 ====================
    public long addRecycle(Recycle recycle) {
        return db.recycleDao().insertRecycle(recycle);
    }

    public int deleteRecycle(Recycle recycle) {
        return db.recycleDao().deleteRecycle(recycle);
    }

    public List<Recycle> getAllRecycles() {
        return db.recycleDao().getAllRecycles();
    }

    public int deleteRecycleByItemId(long itemId) {
        return db.recycleDao().deleteRecycleByItemId(itemId);
    }
}