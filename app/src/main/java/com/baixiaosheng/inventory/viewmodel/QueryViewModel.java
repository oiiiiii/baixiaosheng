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
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.database.entity.Location;
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
    // 线程池（数据库操作）
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // 数据LiveData
    private final MutableLiveData<List<Item>> itemList = new MutableLiveData<>();
    private final MutableLiveData<List<String>> parentCategoryList = new MutableLiveData<>();
    private final MutableLiveData<List<String>> childCategoryList = new MutableLiveData<>();
    private final MutableLiveData<List<String>> locationList = new MutableLiveData<>();
    // 筛选条件
    private final FilterCondition currentFilter = new FilterCondition();

    public QueryViewModel(@NonNull Application application) {
        super(application);
        // 初始化数据库Dao
        InventoryDatabase db = InventoryDatabase.getInstance(application);
        itemDao = db.itemDao();
        categoryDao = db.categoryDao();
        locationDao = db.locationDao();
        // 加载基础数据（分类/位置）
        loadParentCategories();
        loadLocations();
        // 默认加载全量数据
        queryItems(currentFilter);
    }

    // 加载父分类列表
    private void loadParentCategories() {
        executor.execute(() -> {
            List<Category> categories = categoryDao.getParentCategories();
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
            List<Category> allCategories = categoryDao.getAllCategories();
            long parentId = 0;
            for (Category category : allCategories) {
                if (parentCategory.equals(category.getName())) {
                    parentId = category.getId();
                    break;
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

    // 多条件查询物品
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

        // 执行查询
        LiveData<List<Item>> result = itemDao.queryItemsByCondition(
                condition.getSearchKeyword().isEmpty() ? null : condition.getSearchKeyword(),
                condition.getParentCategory().isEmpty() ? null : condition.getParentCategory(),
                condition.getChildCategory().isEmpty() ? null : condition.getChildCategory(),
                condition.getLocation().isEmpty() ? null : condition.getLocation(),
                condition.getQuantityMin(),
                condition.getQuantityMax(),
                expireStartLong,  // 替换Date → Long
                expireEndLong     // 替换Date → Long
        );
        result.observeForever(items -> {
            itemList.postValue(items);
            // 移除观察者避免内存泄漏
            result.removeObserver(items1 -> itemList.postValue(items1));
        });
    }

    // 删除单个物品（标记回收站）
    public void deleteItem(String uuid) {
        executor.execute(() -> itemDao.markItemAsDeleted(uuid));
        // 重新查询刷新列表
        queryItems(currentFilter);
    }

    // 批量删除物品
    public void batchDeleteItems(List<String> uuidList) {
        executor.execute(() -> itemDao.batchMarkDeleted(uuidList));
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

    // 释放线程池
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}