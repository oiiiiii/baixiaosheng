package com.baixiaosheng.inventory.database;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.database.entity.ItemWithName;
import com.baixiaosheng.inventory.database.entity.Location;
import com.baixiaosheng.inventory.database.entity.Recycle;
import com.baixiaosheng.inventory.model.FilterCondition;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据库操作工具类（封装所有增删改查，对外提供统一接口）
 * 简化上层调用，无需直接操作Dao接口
 */
public class DatabaseManager {
    private static DatabaseManager INSTANCE;
    private final InventoryDatabase db;

    private DatabaseManager(Context context) {
        db = InventoryDatabase.getInstance(context);
    }

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
        long currentTime = System.currentTimeMillis();
        category.setCreateTime(currentTime);
        category.setUpdateTime(currentTime);
        return db.categoryDao().insertCategory(category);
    }

    public int updateCategory(Category category) {
        category.setUpdateTime(System.currentTimeMillis());
        return db.categoryDao().updateCategory(category);
    }

    public int deleteCategory(Category category) {
        return db.categoryDao().deleteCategory(category);
    }

    public int deleteCategoryById(long categoryId) {
        return db.categoryDao().deleteCategoryById(categoryId);
    }

    public Category getCategoryById(long categoryId) {
        return db.categoryDao().getCategoryById(categoryId);
    }

    public List<Category> listTopLevelParentCategories() {
        return db.categoryDao().listTopLevelParentCategories();
    }

    public List<Category> listChildCategoriesByParentId(long parentCategoryId) {
        return db.categoryDao().listChildCategoriesByParentId(parentCategoryId);
    }

    public List<Category> listAllCategories() {
        return db.categoryDao().listAllCategories();
    }

    public List<Category> searchCategoriesByKeyword(String keyword) {
        return db.categoryDao().searchCategoriesByKeyword(keyword);
    }

    public List<Category> getCategoriesByCategoryNameAndParentId(String categoryName, long parentCategoryId) {
        return db.categoryDao().getCategoriesByCategoryNameAndParentId(categoryName, parentCategoryId);
    }

    public int countItemsByParentCategoryId(long categoryId) {
        return db.categoryDao().countItemsByParentCategoryId(categoryId);
    }

    public int countItemsByChildCategoryId(long categoryId) {
        return db.categoryDao().countItemsByChildCategoryId(categoryId);
    }

    public int countChildCategoriesByParentId(long parentCategoryId) {
        return db.categoryDao().countChildCategoriesByParentId(parentCategoryId);
    }

    public int getRelatedItemCount(long categoryId) {
        return db.categoryDao().getRelatedItemCount(categoryId);
    }

    public int countAllItemsByParentCategoryId(long parentId) {
        int parentItemCount = db.categoryDao().countItemsByParentCategoryId(parentId);
        List<Category> childCategories = listChildCategoriesByParentId(parentId);
        int childItemTotal = 0;
        for (Category child : childCategories) {
            childItemTotal += db.categoryDao().countItemsByChildCategoryId(child.getId());
        }
        return parentItemCount + childItemTotal;
    }

    public void clearItemChildCategoryId(long childId) {
        db.categoryDao().clearItemChildCategoryId(childId);
    }

    public void clearItemParentCategoryId(long parentId) {
        db.categoryDao().clearItemParentCategoryId(parentId);
    }

    public void deleteParentCategoryWithTransaction(long parentId) {
        db.runInTransaction(() -> {
            clearItemParentCategoryId(parentId);
            List<Category> childCategories = listChildCategoriesByParentId(parentId);
            for (Category child : childCategories) {
                clearItemChildCategoryId(child.getId());
                deleteCategoryById(child.getId());
            }
            deleteCategoryById(parentId);
        });
    }


    /**
     * 校验分类名称是否重复
     * @param categoryName 分类名称
     * @param parentId 父分类ID（0=父分类，>0=子分类）
     * @param excludeId 排除的分类ID（编辑时排除自身）
     * @return true=重复，false=不重复
     */
    public boolean checkCategoryNameDuplicate(String categoryName, long parentId, long excludeId) {
        if (parentId == 0) {
            // 校验父分类名称（parentId=0且名称相同，排除自身）
            return db.categoryDao().countParentCategoryWithName(categoryName, excludeId) > 0;
        } else {
            // 校验子分类名称（同一父分类下名称相同，排除自身）
            return db.categoryDao().countChildCategoryWithName(categoryName, parentId, excludeId) > 0;
        }
    }

    /**
     * 清空指定父分类下所有物品的父/子分类ID
     * @param parentCategoryId 父分类ID
     */
    public void clearItemParentAndChildCategoryId(long parentCategoryId) {
        // 需实现Room DAO方法：UPDATE item SET parentCategoryId=0, childCategoryId=0 WHERE parentCategoryId=?
        db.itemDao().clearParentAndChildCategory(parentCategoryId);
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

    public List<Location> getLocationByName(String name) {
        return db.locationDao().getLocationByName(name);
    }

    public List<Location> getAllLocations() {
        return db.locationDao().getAllLocations();
    }

    /**
     * 检查位置名称是否重复（排除编辑中的ID）
     * @param name 位置名称
     * @param excludeId 排除的ID（编辑时传自身ID，新增时传0）
     * @return true=重复，false=不重复
     */
    public boolean checkLocationNameDuplicate(String name, long excludeId) {
        return db.locationDao().isNameExists(name, excludeId);
    }

    // 补充：添加根据ID删除位置的封装（原ViewModel中删除逻辑也可统一封装）
    public int deleteLocationById(long locationId) {
        return db.locationDao().deleteLocationById(locationId);
    }

    /**
     * 获取位置关联的物品数量
     * @param locationId 位置ID
     * @return 关联物品数量
     */
    public int getLocationRelatedItemCount(long locationId) {
        return db.locationDao().getRelatedItemCount(locationId);
    }

    public void clearItemLocationByLocationId(long locationId) {
        // 调用Dao层方法（需在子线程执行，此处已由ViewModel的ExecutorService保证）
        db.itemDao().clearItemLocationByLocationId(locationId);
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

    public void markItemAsDeleted(Long itemId) {
        Item item = db.itemDao().getItemById(itemId);
        if (item != null) {
            long currentTime = System.currentTimeMillis();
            db.itemDao().markItemAsDeleted(item.getUuid(), currentTime);
        }
    }

    public int restoreItemById(long itemId) {
        Item item = db.itemDao().getItemById(itemId);
        if (item != null) {
            item.setIsDeleted(0);
            item.setUpdateTime(System.currentTimeMillis());
            return db.itemDao().updateItem(item);
        }
        return 0;
    }

    public void restoreItemsByIds(List<Long> itemIds) {
        for (long itemId : itemIds) {
            restoreItemById(itemId);
        }
    }

    public int deleteItemById(long itemId) {
        return db.itemDao().deleteItemById(itemId);
    }

    public int deleteItemsByIds(List<Long> itemIds) {
        return db.itemDao().deleteItemsByIds(itemIds);
    }

    public List<Item> getDeletedItems() {
        return db.itemDao().getDeletedItems();
    }

    // ==================== 复杂查询方法 ====================

    public LiveData<List<ItemWithName>> queryItemsWithName(
            String keyword,
            String parentCategoryName,
            String childCategoryName,
            String locationName,
            Integer quantityMin,
            Integer quantityMax,
            Long expireStart,
            Long expireEnd) {
        return db.itemDao().queryItemsWithName(
                keyword, parentCategoryName, childCategoryName, locationName,
                quantityMin, quantityMax, expireStart, expireEnd);
    }


    public LiveData<List<Item>> queryItemsByCondition(
            String keyword,
            Long parentCategoryId,
            Long childCategoryId,
            Long locationId,
            Integer quantityMin,
            Integer quantityMax,
            Long expireStart,
            Long expireEnd) {

        // 将单个Long转换为List<Long>
        List<Long> parentIds = parentCategoryId != null ?
                Arrays.asList(parentCategoryId) : null;
        List<Long> childIds = childCategoryId != null ?
                Arrays.asList(childCategoryId) : null;
        List<Long> locationIds = locationId != null ?
                Arrays.asList(locationId) : null;

        return db.itemDao().queryItemsByCondition(
                keyword,
                parentIds,       // 现在传List
                childIds,        // 现在传List
                locationIds,     // 现在传List
                quantityMin, quantityMax, expireStart, expireEnd);
    }

    public LiveData<List<ItemWithName>> queryItemsByFilter(FilterCondition filter) {
        String keyword = (filter.getSearchKeyword() == null || filter.getSearchKeyword().isEmpty())
                ? null : filter.getSearchKeyword();
        String parentCategoryName = (filter.getParentCategory() == null || filter.getParentCategory().isEmpty())
                ? null : filter.getParentCategory();
        String childCategoryName = (filter.getChildCategory() == null || filter.getChildCategory().isEmpty())
                ? null : filter.getChildCategory();
        String locationName = (filter.getLocation() == null || filter.getLocation().isEmpty())
                ? null : filter.getLocation();

        Long expireStart = filter.getExpireStart() != null ? filter.getExpireStart().getTime() : null;
        Long expireEnd = filter.getExpireEnd() != null ? filter.getExpireEnd().getTime() : null;

        return db.itemDao().queryItemsWithName(
                keyword, parentCategoryName, childCategoryName, locationName,
                filter.getQuantityMin(), filter.getQuantityMax(),
                expireStart, expireEnd);
    }

    // ==================== 回收站表操作 ====================

    public int restoreItemsWithTransaction(List<Long> recycleIds, List<Long> itemIds) {
        if (recycleIds == null || itemIds == null || recycleIds.isEmpty() || itemIds.isEmpty()
                || recycleIds.size() != itemIds.size()) {
            android.util.Log.e("DatabaseManager", "批量还原参数错误：ID列表为空或长度不匹配");
            return 0;
        }

        AtomicInteger successCount = new AtomicInteger(0);
        try {
            db.runInTransaction(() -> {
                for (int i = 0; i < recycleIds.size(); i++) {
                    long recycleId = recycleIds.get(i);
                    long itemId = itemIds.get(i);
                    try {
                        Recycle recycle = getRecycleById(recycleId);
                        if (recycle == null || recycle.getItemId() != itemId) {
                            android.util.Log.w("DatabaseManager", "回收站记录ID：" + recycleId + " 与物品ID：" + itemId + " 不匹配，跳过");
                            continue;
                        }
                        Item item = getItemById(itemId);
                        if (item == null || item.getIsDeleted() != 1) {
                            android.util.Log.w("DatabaseManager", "物品ID：" + itemId + " 不存在或未删除，跳过");
                            continue;
                        }
                        int restoreResult = restoreItemById(itemId);
                        if (restoreResult <= 0) {
                            android.util.Log.w("DatabaseManager", "物品ID：" + itemId + " 恢复失败，跳过");
                            continue;
                        }
                        int deleteResult = deleteRecycleItemById(recycleId);
                        if (deleteResult <= 0) {
                            android.util.Log.w("DatabaseManager", "回收站记录ID：" + recycleId + " 删除失败，跳过");
                            item.setIsDeleted(1);
                            updateItem(item);
                            continue;
                        }
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        android.util.Log.e("DatabaseManager", "处理回收站ID：" + recycleId + " 失败：", e);
                        continue;
                    }
                }
            });
        } catch (Exception e) {
            android.util.Log.e("DatabaseManager", "批量还原事务执行异常：", e);
        }
        return successCount.get();
    }

    public LiveData<List<Recycle>> getAllRecycleItemsSyncLive() {
        return db.recycleDao().getAllRecycleItemsSyncLive();
    }

    public Recycle getRecycleById(long id) {
        return db.recycleDao().getRecycleById(id);
    }

    public Recycle getRecycleByItemId(long itemId) {
        return db.recycleDao().getRecycleByItemId(itemId);
    }

    public long addRecycle(Recycle recycle) {
        if (recycle == null) {
            return -1;
        }
        if (recycle.getItemId() <= 0) {
            return -1;
        }
        if (recycle.getItemName() == null || recycle.getItemName().trim().isEmpty()) {
            return -1;
        }
        if (recycle.getDeleteTime() <= 0) {
            recycle.setDeleteTime(System.currentTimeMillis());
        }
        if (recycle.getItemUuid() == null) {
            recycle.setItemUuid("");
        }
        return db.recycleDao().insertRecycle(recycle);
    }

    public List<Recycle> getAllRecycleItemsSync() {
        return db.recycleDao().getAllRecycleItemsSync();
    }

    public int deleteRecycleItemById(long recycleId) {
        return db.recycleDao().deleteRecycleItemById(recycleId);
    }

    public int deleteRecycleItemsByIds(List<Long> recycleIds) {
        return db.recycleDao().deleteRecycleItemsByIds(recycleIds);
    }
}