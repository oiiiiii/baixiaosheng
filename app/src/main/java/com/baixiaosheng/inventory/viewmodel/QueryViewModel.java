package com.baixiaosheng.inventory.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baixiaosheng.inventory.database.InventoryDatabase;
import com.baixiaosheng.inventory.database.dao.CategoryDao;
import com.baixiaosheng.inventory.database.dao.ItemDao;
import com.baixiaosheng.inventory.database.dao.LocationDao;
import com.baixiaosheng.inventory.database.dao.RecycleDao;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.database.entity.Location;
import com.baixiaosheng.inventory.database.entity.Recycle;
import com.baixiaosheng.inventory.model.FilterCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 查询页ViewModel：封装数据逻辑，解耦Fragment与数据库
 */
public class QueryViewModel extends AndroidViewModel {
    // 数据库Dao
    private final ItemDao itemDao;
    private final CategoryDao categoryDao;
    private final LocationDao locationDao;
    private final RecycleDao recycleDao;
    // 线程池（数据库操作）
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // 数据LiveData
    private final MutableLiveData<List<Item>> itemList = new MutableLiveData<>();
    private final MutableLiveData<List<String>> parentCategoryList = new MutableLiveData<>();
    private final MutableLiveData<List<String>> childCategoryList = new MutableLiveData<>();
    private final MutableLiveData<List<String>> locationList = new MutableLiveData<>();
    // 筛选条件
    private final FilterCondition currentFilter = new FilterCondition();
    // 缓存全量分类和位置数据（用于名称转ID）
    private List<Category> allCategoriesCache;
    private List<Location> allLocationsCache;

    public QueryViewModel(@NonNull Application application) {
        super(application);
        // 初始化数据库Dao
        InventoryDatabase db = InventoryDatabase.getInstance(application);
        itemDao = db.itemDao();
        categoryDao = db.categoryDao();
        locationDao = db.locationDao();
        recycleDao = db.recycleDao(); // 初始化回收站Dao
        // 加载基础数据（分类/位置）
        loadAllCategories();
        loadAllLocations();
        loadParentCategories();
        // 默认加载全量数据
        queryItems(currentFilter);
    }

    // 加载全量分类（缓存用于名称转ID）
    private void loadAllCategories() {
        executor.execute(() -> {
            allCategoriesCache = categoryDao.getAllCategories();
        });
    }

    // 加载全量位置（缓存用于名称转ID）
    private void loadAllLocations() {
        executor.execute(() -> {
            allLocationsCache = locationDao.getAllLocations();
        });
    }

    // 加载父分类列表
    private void loadParentCategories() {
        executor.execute(() -> {
            List<Category> categories = categoryDao.getParentCategories(0);
            // 兼容Java 8及以下版本，使用collect(Collectors.toList())替代toList()
            List<String> names = categories.stream()
                    .map(Category::getName)
                    .collect(Collectors.toList());
            parentCategoryList.postValue(names);
        });
    }

    // 根据父分类加载子分类（修复核心逻辑）
    public void loadChildCategories(String parentCategory) {
        executor.execute(() -> {
            // 步骤1：先获取所有分类，找到父分类名称对应的ID
            long parentId = 0;
            if (allCategoriesCache != null) {
                for (Category category : allCategoriesCache) {
                    if (parentCategory.equals(category.getName())) {
                        parentId = category.getId();
                        break;
                    }
                }
            }
            // 步骤2：根据父分类ID查询子分类
            List<Category> childCategories = categoryDao.getChildCategoriesByParentId(parentId);
            // 兼容Java 8及以下版本，使用collect(Collectors.toList())替代toList()
            List<String> names = childCategories.stream()
                    .map(Category::getName)
                    .collect(Collectors.toList());
            childCategoryList.postValue(names);
        });
    }

    // 加载位置列表
    private void loadLocations() {
        executor.execute(() -> {
            List<Location> locations = locationDao.getAllLocations();
            // 兼容Java 8及以下版本，使用collect(Collectors.toList())替代toList()
            List<String> names = locations.stream()
                    .map(Location::getName)
                    .collect(Collectors.toList());
            locationList.postValue(names);
        });
    }

    // 名称转分类ID
    private Long getCategoryIdByName(String categoryName) {
        if (categoryName == null || categoryName.isEmpty() || allCategoriesCache == null) {
            return null;
        }
        for (Category category : allCategoriesCache) {
            if (categoryName.equals(category.getName())) {
                return category.getId();
            }
        }
        return null;
    }

    // 名称转位置ID
    private Long getLocationIdByName(String locationName) {
        if (locationName == null || locationName.isEmpty() || allLocationsCache == null) {
            return null;
        }
        for (Location location : allLocationsCache) {
            if (locationName.equals(location.getName())) {
                return location.getId();
            }
        }
        return null;
    }

    // 多条件查询物品（修复：参数类型+数量+isDeleted过滤）
    public void queryItems(FilterCondition condition) {
        // 更新当前筛选条件
        currentFilter.setSearchKeyword(condition.getSearchKeyword());
        currentFilter.setParentCategory(condition.getParentCategory());
        currentFilter.setChildCategory(condition.getChildCategory());
        currentFilter.setLocation(condition.getLocation());
        currentFilter.setQuantityMin(condition.getQuantityMin());
        currentFilter.setQuantityMax(condition.getQuantityMax());
        currentFilter.setExpireStart(condition.getExpireStart());
        currentFilter.setExpireEnd(condition.getExpireEnd());
        // 关键修改：将Date转换为Long（时间戳），空值处理
        Long expireStartLong = condition.getExpireStart() != null ? condition.getExpireStart().getTime() : null;
        Long expireEndLong = condition.getExpireEnd() != null ? condition.getExpireEnd().getTime() : null;

        // 名称转ID（核心修复：匹配Dao层的Long类型参数）
        Long parentCategoryId = getCategoryIdByName(condition.getParentCategory());
        Long childCategoryId = getCategoryIdByName(condition.getChildCategory());
        Long locationId = getLocationIdByName(condition.getLocation());

        // 执行查询（修复：参数类型+数量，添加isDeleted=0过滤）
        LiveData<List<Item>> result = itemDao.queryItemsByCondition(
                condition.getSearchKeyword().isEmpty() ? null : condition.getSearchKeyword(),
                parentCategoryId,
                childCategoryId,
                locationId,
                condition.getQuantityMin(),
                condition.getQuantityMax(),
                expireStartLong,
                expireEndLong
        );
        result.observeForever(items -> {
            itemList.postValue(items);
            // 移除观察者避免内存泄漏
            result.removeObserver(items1 -> itemList.postValue(items1));
        });
    }

    // 删除单个物品（修复：标记为回收站而非彻底删除）
    public void deleteItem(String uuid) {
        executor.execute(() -> {
            // 1. 获取物品信息
            Item item = itemDao.getItemByUuid(uuid);
            if (item != null) {
                // 2. 更新物品的删除标记（isDeleted=1）
                item.setIsDeleted(1);
                item.setUpdateTime(System.currentTimeMillis());
                itemDao.updateItem(item);

                // 3. 插入回收站记录
                Recycle recycle = new Recycle();
                recycle.setItemId(item.getId());
                recycle.setItemUuid(item.getUuid());
                recycle.setItemName(item.getName());
                recycle.setDeleteTime(System.currentTimeMillis());
                recycle.setDeleteReason("用户手动删除");
                recycleDao.insertRecycle(recycle);
            }
        });
        // 重新查询刷新列表
        queryItems(currentFilter);
    }

    // 批量删除物品（修复：标记为回收站而非彻底删除）
    public void batchDeleteItems(List<String> uuidList) {
        executor.execute(() -> {
            for (String uuid : uuidList) {
                // 1. 获取物品信息
                Item item = itemDao.getItemByUuid(uuid);
                if (item != null) {
                    // 2. 更新物品的删除标记（isDeleted=1）
                    item.setIsDeleted(1);
                    item.setUpdateTime(System.currentTimeMillis());
                    itemDao.updateItem(item);

                    // 3. 插入回收站记录
                    Recycle recycle = new Recycle();
                    recycle.setItemId(item.getId());
                    recycle.setItemUuid(item.getUuid());
                    recycle.setItemName(item.getName());
                    recycle.setDeleteTime(System.currentTimeMillis());
                    recycle.setDeleteReason("批量删除");
                    recycleDao.insertRecycle(recycle);
                }
            }
        });
        // 重新查询刷新列表
        queryItems(currentFilter);
    }

    // 获取物品详情
    public Item getItemByUuid(String uuid) {
        return itemDao.getItemByUuid(uuid);
    }

    // LiveData getter（供Fragment观察）
    public LiveData<List<Item>> getItemList() {
        return itemList;
    }

    public LiveData<List<String>> getParentCategoryList() {
        return parentCategoryList;
    }

    public LiveData<List<String>> getChildCategoryList() {
        return childCategoryList;
    }

    public LiveData<List<String>> getLocationList() {
        return locationList;
    }

    // 根据ID查询物品（返回LiveData，适配Activity观察）
    public LiveData<Item> getItemById(long itemId) {
        MutableLiveData<Item> itemLiveData = new MutableLiveData<>();
        executor.execute(() -> {
            Item item = itemDao.getItemById(itemId);
            itemLiveData.postValue(item);
        });
        return itemLiveData;
    }

    // 根据ID查询分类（返回LiveData）
    public LiveData<Category> getCategoryById(long categoryId) {
        MutableLiveData<Category> categoryLiveData = new MutableLiveData<>();
        executor.execute(() -> {
            Category category = categoryDao.getCategoryById(categoryId);
            categoryLiveData.postValue(category);
        });
        return categoryLiveData;
    }

    // 根据ID查询位置（返回LiveData）
    public LiveData<Location> getLocationById(long locationId) {
        MutableLiveData<Location> locationLiveData = new MutableLiveData<>();
        executor.execute(() -> {
            Location location = locationDao.getLocationById(locationId);
            locationLiveData.postValue(location);
        });
        return locationLiveData;
    }

    // 标记物品为已删除（补充ItemDetailActivity调用的markItemAsDeleted方法）
    public void markItemAsDeleted(long itemId) {
        executor.execute(() -> {
            Item item = itemDao.getItemById(itemId);
            if (item != null) {
                item.setIsDeleted(1);
                item.setUpdateTime(System.currentTimeMillis());
                itemDao.updateItem(item);

                // 插入回收站记录
                Recycle recycle = new Recycle();
                recycle.setItemId(item.getId());
                recycle.setItemUuid(item.getUuid());
                recycle.setItemName(item.getName());
                recycle.setDeleteTime(System.currentTimeMillis());
                recycle.setDeleteReason("详情页删除");
                recycleDao.insertRecycle(recycle);
            }
        });
    }

    // 释放线程池
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}