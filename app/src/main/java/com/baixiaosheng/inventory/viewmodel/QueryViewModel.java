package com.baixiaosheng.inventory.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baixiaosheng.inventory.database.DatabaseManager;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.database.entity.ItemWithName;
import com.baixiaosheng.inventory.database.entity.Location;
import com.baixiaosheng.inventory.database.entity.Recycle;
import com.baixiaosheng.inventory.model.FilterCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 查询页ViewModel：通过DatabaseManager访问数据，实现分层架构
 */
public class QueryViewModel extends AndroidViewModel {
    // 数据库管理器
    private final DatabaseManager databaseManager;
    // 线程池
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // 数据LiveData
    private final MutableLiveData<List<ItemWithName>> itemList = new MutableLiveData<>();
    private final MutableLiveData<List<String>> parentCategoryList = new MutableLiveData<>();
    private final MutableLiveData<List<String>> childCategoryList = new MutableLiveData<>();
    private final MutableLiveData<List<String>> locationList = new MutableLiveData<>();
    private final MutableLiveData<Item> currentItemLiveData = new MutableLiveData<>();


    // 筛选条件
    private final FilterCondition currentFilter = new FilterCondition();
    // 缓存全量分类和位置数据
    private List<Category> allCategoriesCache;
    private List<Location> allLocationsCache;

    public QueryViewModel(@NonNull Application application) {
        super(application);
        databaseManager = DatabaseManager.getInstance(application);
        // 初始化时仅调用一次，移除重复调用
        initData();
    }

    // 初始化数据：统一管理首次加载逻辑，避免重复
    private void initData() {
        // 先加载全量缓存，再加载UI展示的分类/位置
        loadAllCategories();
        loadAllLocations();
        loadParentCategories();
        loadLocations();
        queryItems(currentFilter);

    }

    // ==================== 数据加载方法 ====================

    /**
     * 重新加载全量分类（每次刷新时调用，更新缓存）
     * 供外部onResume()触发刷新
     */
    public void reloadAllCategories() {
        executor.execute(() -> {
            allCategoriesCache = databaseManager.getAllCategories();
            // 全量分类更新后，主动刷新父分类列表（联动UI）
            loadParentCategoriesInternal();
        });
    }

    /**
     * 重新加载全量位置（每次刷新时调用，更新缓存）
     * 供外部onResume()触发刷新
     */
    public void reloadAllLocations() {
        executor.execute(() -> {
            allLocationsCache = databaseManager.getAllLocations();
            // 全量位置更新后，主动刷新位置列表（联动UI）
            loadLocationsInternal();
        });
    }

    private void loadAllCategories() {
        executor.execute(() -> {
            allCategoriesCache = databaseManager.getAllCategories();
        });
    }

    private void loadAllLocations() {
        executor.execute(() -> {
            allLocationsCache = databaseManager.getAllLocations();
        });
    }
    /**
     * 对外暴露：加载父分类（供Fragment onResume调用）
     * 触发重新查询数据库并更新LiveData
     */
    public void loadParentCategories() {
        executor.execute(this::loadParentCategoriesInternal);
    }

    // 父分类加载核心逻辑（抽离为内部方法，避免重复代码）
    private void loadParentCategoriesInternal() {
        List<Category> categories = databaseManager.getParentCategories(0);
        List<String> names = new ArrayList<>();
        for (Category category : categories) {
            names.add(category.getName());
        }
        // postValue确保线程安全，更新LiveData
        parentCategoryList.postValue(names);
    }

    /**
     * 对外暴露：加载子分类（供Fragment 父分类选择时调用）
     */
    public void loadChildCategories(String parentCategory) {
        executor.execute(() -> {
            List<Category> childCategories;
            if (parentCategory == null || parentCategory.isEmpty()) {
                childCategories = databaseManager.getAllCategories();
            } else {
                long parentId = 0;
                if (allCategoriesCache != null) {
                    for (Category category : allCategoriesCache) {
                        if (parentCategory.equals(category.getName())) {
                            parentId = category.getId();
                            break;
                        }
                    }
                }
                childCategories = databaseManager.getChildCategoriesByParentId(parentId);
            }
            List<String> names = new ArrayList<>();
            names.add("全部");
            for (Category category : childCategories) {
                names.add(category.getName());
            }
            childCategoryList.postValue(names);
        });
    }

    /**
     * 对外暴露：加载位置（供Fragment onResume调用）
     * 触发重新查询数据库并更新LiveData
     */
    public void loadLocations() {
        executor.execute(this::loadLocationsInternal);
    }

    // 位置加载核心逻辑（抽离为内部方法）
    private void loadLocationsInternal() {
        List<Location> locations = databaseManager.getAllLocations();
        List<String> names = new ArrayList<>();
        for (Location location : locations) {
            names.add(location.getName());
        }
        locationList.postValue(names);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }

    // 新增：对外暴露刷新全量分类/位置的方法（供Fragment onResume调用）
    public void refreshAllCategoryAndLocation() {
        reloadAllCategories();
        reloadAllLocations();
    }

    // ==================== 查询方法 ====================

    /**
     * 多条件查询物品（使用DatabaseManager）
     */
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

        // 直接查询ItemWithName
        LiveData<List<ItemWithName>> result = databaseManager.queryItemsByFilter(condition);

        // 观察数据变化
        result.observeForever(items -> {
            itemList.postValue(items);
            // 移除观察者避免内存泄漏
            result.removeObserver(items1 -> itemList.postValue(items1));
        });
    }


    // ==================== 名称转ID辅助方法 ====================

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

    // ==================== 删除操作 ====================

    public void deleteItem(String uuid) {
        executor.execute(() -> {
            Item item = databaseManager.getItemByUuid(uuid);
            if (item != null) {
                databaseManager.markItemAsDeleted(item.getId());

                Recycle recycle = new Recycle();
                recycle.setItemId(item.getId());
                recycle.setItemUuid(item.getUuid());
                recycle.setItemName(item.getName());
                recycle.setDeleteTime(System.currentTimeMillis());
                recycle.setDeleteReason("用户手动删除");
                databaseManager.addRecycle(recycle);
            }
        });
        queryItems(currentFilter);
    }

    public void batchDeleteItems(List<String> uuidList) {
        executor.execute(() -> {
            for (String uuid : uuidList) {
                Item item = databaseManager.getItemByUuid(uuid);
                if (item != null) {
                    databaseManager.markItemAsDeleted(item.getId());

                    Recycle recycle = new Recycle();
                    recycle.setItemId(item.getId());
                    recycle.setItemUuid(item.getUuid());
                    recycle.setItemName(item.getName());
                    recycle.setDeleteTime(System.currentTimeMillis());
                    recycle.setDeleteReason("批量删除");
                    databaseManager.addRecycle(recycle);
                }
            }
        });
        queryItems(currentFilter);
    }



    // ==================== LiveData Getter ====================

    // 修改返回类型
    public LiveData<List<ItemWithName>> getItemList() {
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

    // 替换原错误的 getItemByIdLiveData 方法
    public LiveData<Item> getItemByIdLiveData(long itemId) {
        // 创建 MutableLiveData 用于包装查询结果
        MutableLiveData<Item> itemLiveData = new MutableLiveData<>();

        // 在线程池中执行同步查询，避免阻塞主线程
        executor.execute(() -> {
            Item item = databaseManager.getItemById(itemId);
            // 把查询结果post到主线程（postValue 自动切换到主线程）
            itemLiveData.postValue(item);
        });

        // 返回包装后的 LiveData
        return itemLiveData;
    }

    // 同理，补充分类/位置的 LiveData 查询方法（适配原Activity中的调用）
    public LiveData<Category> getCategoryByIdLiveData(long categoryId) {
        MutableLiveData<Category> categoryLiveData = new MutableLiveData<>();
        executor.execute(() -> {
            Category category = databaseManager.getCategoryById(categoryId);
            categoryLiveData.postValue(category);
        });
        return categoryLiveData;
    }

    public LiveData<Location> getLocationByIdLiveData(long locationId) {
        MutableLiveData<Location> locationLiveData = new MutableLiveData<>();
        executor.execute(() -> {
            Location location = databaseManager.getLocationById(locationId);
            locationLiveData.postValue(location);
        });
        return locationLiveData;
    }


    // ==================== 回调接口方法 ====================

    public interface OnItemLoadedListener {
        void onItemLoaded(Item item);
    }


    public void getItemById(long itemId, OnItemLoadedListener listener) {
        executor.execute(() -> {
            Item item = databaseManager.getItemById(itemId);
            listener.onItemLoaded(item);
        });
    }

    public interface OnCategoryLoadedListener {
        void onCategoryLoaded(Category category);
    }

    public void getCategoryById(long categoryId, OnCategoryLoadedListener listener) {
        executor.execute(() -> {
            Category category = databaseManager.getCategoryById(categoryId);
            listener.onCategoryLoaded(category);
        });
    }


    public interface OnLocationLoadedListener {
        void onLocationLoaded(Location location);
    }

    public void getLocationById(long locationId, OnLocationLoadedListener listener) {
        executor.execute(() -> {
            Location location = databaseManager.getLocationById(locationId);
            listener.onLocationLoaded(location);
        });
    }



    public void markItemAsDeleted(long itemId) {
        executor.execute(() -> {
            databaseManager.markItemAsDeleted(itemId);

            Item item = databaseManager.getItemById(itemId);
            if (item != null) {
                Recycle recycle = new Recycle();
                recycle.setItemId(item.getId());
                recycle.setItemUuid(item.getUuid());
                recycle.setItemName(item.getName());
                recycle.setDeleteTime(System.currentTimeMillis());
                recycle.setDeleteReason("详情页删除");
                databaseManager.addRecycle(recycle);
            }
        });
    }

}