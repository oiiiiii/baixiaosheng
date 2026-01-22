package com.baixiaosheng.inventory.database;

import android.content.Context;
import android.util.Log;

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

    // ==================== 事务操作封装 ====================
    /**
     * 执行数据库事务
     * @param runnable 事务内要执行的逻辑
     */
    public void runInTransaction(Runnable runnable) {
        db.runInTransaction(runnable);
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

        // 新增：删除位置前，清空所有关联物品的locationId（设为0）
        db.runInTransaction(() -> {
            // 调用Dao层方法清空物品关联的位置ID（需先在ItemDao中定义该方法）
            db.itemDao().clearItemLocationByLocationId(locationId);
            // 再删除位置本身
            db.locationDao().deleteLocationById(locationId);
        });
        return 1;
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

    public Item getItemByIdNotDeleted(long id) {
        return db.itemDao().getItemByIdNotDeleted(id);
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
        Item item = db.itemDao().getItemByIdNotDeleted(itemId);
        if (item != null) {
            long currentTime = System.currentTimeMillis();
            db.itemDao().markItemAsDeleted(item.getUuid(), currentTime);
        }
    }


    public int restoreItemById(long itemId) {
        Log.d("DatabaseManager", "开始恢复物品ID：" + itemId);
        Item item = db.itemDao().getItemById(itemId);
        if (item == null) {
            Log.e("DatabaseManager", "恢复失败：物品ID=" + itemId + " 在Item表中不存在");
            return 0;
        }
        Log.d("DatabaseManager", "物品ID=" + itemId + " 当前isDeleted状态：" + item.getIsDeleted());
        item.setIsDeleted(0);
        item.setUpdateTime(System.currentTimeMillis());
        int updateResult = db.itemDao().updateItem(item);
        Log.d("DatabaseManager", "物品ID=" + itemId + " 更新结果：" + updateResult);
        if (updateResult <= 0) {
            Log.e("DatabaseManager", "恢复失败：物品ID=" + itemId + " 更新isDeleted失败，返回值=" + updateResult);
        }
        return updateResult;
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

    // 添加这个方法
    public LiveData<List<Item>> getDeletedItemsLive() {
        // 需要在ItemDao中添加对应的LiveData查询方法
        return db.itemDao().getDeletedItemsLive();
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


    // ==================== 回收站表操作（完全封装Dao调用，简化上层逻辑） ====================
    // 封装：批量恢复回收站物品（上层无需处理事务细节）
    public int restoreItemsWithTransaction(List<Long> recycleIds, List<Long> itemIds) {
        if (recycleIds == null || itemIds == null || recycleIds.isEmpty() || itemIds.isEmpty()
                || recycleIds.size() != itemIds.size()) {
            Log.e("DatabaseManager", "批量还原参数错误：ID列表为空或长度不匹配");
            return 0;
        }

        AtomicInteger successCount = new AtomicInteger(0);
        try {
            runInTransaction (() -> { // 复用封装的事务方法
                for (int i = 0; i < recycleIds.size(); i++) {
                    long recycleId = recycleIds.get(i);
                    long itemId = itemIds.get(i);
                    try {
                        // 1. 恢复物品（复用封装方法）
                        int restoreResult = restoreItemById(itemId);
                        if (restoreResult <= 0) {
                            Log.w("DatabaseManager", "物品ID：" + itemId + " 恢复失败");
                            continue;
                        }

                        // 2. 删除回收站记录（封装成方法，见下方）
                        int deleteResult = deleteRecycleItemById(recycleId);
                        if (deleteResult <= 0) {
                            Log.w("DatabaseManager", "回收站记录ID：" + recycleId + " 删除失败");
                            // 回滚：重新标记为删除（复用封装方法）
                            markItemAsDeleted(itemId);
                            continue;
                        }
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        Log.e("DatabaseManager", "处理回收站ID：" + recycleId + " 失败：", e);
                        continue;
                    }
                }
            });
        } catch (Exception e) {
            Log.e("DatabaseManager", "批量还原事务执行异常：", e);
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
        if (recycle == null) return -1;
        if (recycle.getItemId() <= 0) return -1;
        if (recycle.getItemName() == null || recycle.getItemName().trim().isEmpty()) return -1;
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

    // 封装：删除单个回收站记录（上层无需调用Dao）
    public int deleteRecycleItemById(long recycleId) {
        try {
            return db.recycleDao().deleteRecycleItemById(recycleId);
        } catch (Exception e) {
            Log.e("DatabaseManager", "删除回收站记录失败：" + recycleId, e);
            return 0;
        }
    }

    public int deleteRecycleItemsByIds(List<Long> recycleIds) {
        return db.recycleDao().deleteRecycleItemsByIds(recycleIds);
    }

    // 封装：恢复单个回收站物品（上层无需处理事务/回滚逻辑）
    public int restoreSingleRecycleItem(Recycle recycle) {
        if (recycle == null || recycle.getItemId() <= 0) {
            Log.e("DatabaseManager", "恢复参数错误：回收站记录为空或物品ID无效");
            return 0;
        }
        AtomicInteger result = new AtomicInteger(0);
        try {
            runInTransaction (() -> { // 复用封装的事务方法
                // 1. 恢复物品（复用封装方法）
                int restoreResult = restoreItemById(recycle.getItemId());
                if (restoreResult <= 0) {
                    Log.w("DatabaseManager", "物品恢复失败：" + recycle.getItemId());
                    return;
                }

                // 2. 删除回收站记录（复用封装方法）
                int deleteRecycleResult = deleteRecycleItemById(recycle.getId());
                if (deleteRecycleResult <= 0) {
                    Log.w("DatabaseManager", "回收站记录删除失败，回滚物品状态：" + recycle.getId());
                    // 回滚：重新标记为删除（复用封装方法）
                    markItemAsDeleted(recycle.getItemId());
                    return;
                }
                result.set(1);
            });
        } catch (Exception e) {
            Log.e("DatabaseManager", "恢复单个回收站物品异常：", e);
        }
        return result.get();
    }

    // ==================== 新增：补充缺失的封装方法（避免上层接触Dao） ====================
    // 封装：批量标记物品为删除（上层无需传uuidList）
    public void batchMarkDeleted(List<Long> itemIds) {
        List<String> uuidList = itemIds.stream()
                .map(this::getItemByIdNotDeleted)
                .filter(item -> item != null)
                .map(Item::getUuid)
                .toList();
        long updateTime = System.currentTimeMillis();
        db.itemDao().batchMarkDeleted(uuidList, updateTime);
    }

    // 封装：恢复回收站物品（按uuid，上层无需接触Dao）
    public void restoreItemFromRecycle(String uuid) {
        long updateTime = System.currentTimeMillis();
        db.itemDao().restoreItemFromRecycle(uuid, updateTime);
    }

    // 封装：批量恢复回收站物品（按uuidList，上层无需接触Dao）
    public void batchRestoreFromRecycle(List<String> uuidList) {
        long updateTime = System.currentTimeMillis();
        db.itemDao().batchRestoreFromRecycle(uuidList, updateTime);
    }

    // 封装：获取回收站物品（分页+关键词，上层无需处理offset）
    public LiveData<List<Item>> getRecycleItems(String keyword, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return db.itemDao().getRecycleItems(keyword, pageSize, offset);
    }

    // 封装：获取回收站物品总数（上层无需调用Dao）
    public LiveData<Integer> getRecycleItemsCount(String keyword) {
        return db.itemDao().getRecycleItemsCount(keyword);
    }
}