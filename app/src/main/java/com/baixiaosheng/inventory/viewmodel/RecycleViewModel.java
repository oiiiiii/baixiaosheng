package com.baixiaosheng.inventory.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baixiaosheng.inventory.database.DatabaseManager;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.database.entity.Recycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 回收站页面ViewModel
 * 负责获取和管理回收站物品数据，处理恢复/删除业务逻辑
 */
public class RecycleViewModel extends AndroidViewModel {
    private static final String TAG = "RecycleViewModel";
    private final DatabaseManager databaseManager;
    private LiveData<List<Item>> recycleItems;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // 用于通知UI操作结果
    private final MutableLiveData<Boolean> restoreSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>();

    public RecycleViewModel(@NonNull Application application) {
        super(application);
        databaseManager = DatabaseManager.getInstance(application.getApplicationContext());
        // 初始化回收站物品数据（默认查询全部，无关键词）
        loadRecycleItems(null);
    }

    /**
     * 加载回收站物品数据
     * @param keyword 搜索关键词（可为null，查询全部）
     */
    public void loadRecycleItems(String keyword) {
        // 分页参数：默认第一页，每页20条（可根据需求调整）
        int pageNum = 1;
        int pageSize = 20;
        recycleItems = databaseManager.getRecycleItems(keyword, pageNum, pageSize);
    }

    /**
     * 获取回收站物品列表的LiveData
     */
    public LiveData<List<Item>> getRecycleItems() {
        return recycleItems;
    }

    /**
     * 获取回收站物品总数（用于辅助判断是否为空）
     */
    public LiveData<Integer> getRecycleItemsCount(String keyword) {
        return databaseManager.getRecycleItemsCount(keyword);
    }

    /**
     * 恢复单个回收站物品
     * @param itemId 物品ID
     */
    public void restoreSingleItem(long itemId) {
        executor.execute(() -> {
            try {
                Recycle recycle = databaseManager.getRecycleByItemId(itemId);
                if (recycle == null) {
                    Log.e(TAG, "未找到物品ID为" + itemId + "的回收站记录");
                    restoreSuccess.postValue(false);
                    return;
                }
                int result = databaseManager.restoreSingleRecycleItem(recycle);
                restoreSuccess.postValue(result > 0);
            } catch (Exception e) {
                Log.e(TAG, "恢复单个物品失败：", e);
                restoreSuccess.postValue(false);
            }
        });
    }

    /**
     * 彻底删除单个物品
     * @param itemId 物品ID
     */
    public void deleteSingleItem(long itemId) {
        executor.execute(() -> {
            try {
                // 先删除回收站记录，再删除物品
                Recycle recycle = databaseManager.getRecycleByItemId(itemId);
                if (recycle != null) {
                    databaseManager.deleteRecycleItemById(recycle.getId());
                }
                int result = databaseManager.deleteItemById(itemId);
                deleteSuccess.postValue(result > 0);
            } catch (Exception e) {
                Log.e(TAG, "彻底删除单个物品失败：", e);
                deleteSuccess.postValue(false);
            }
        });
    }

    /**
     * 批量恢复回收站物品
     * @param selectedItemWithNameList 选中的物品列表（ItemWithName）
     */
    public void batchRestoreItems(List<com.baixiaosheng.inventory.database.entity.ItemWithName> selectedItemWithNameList) {
        executor.execute(() -> {
            try {
                List<Long> recycleIds = new ArrayList<>();
                List<Long> itemIds = new ArrayList<>();
                for (com.baixiaosheng.inventory.database.entity.ItemWithName itemWithName : selectedItemWithNameList) {
                    long itemId = itemWithName.item.getId();
                    Recycle recycle = databaseManager.getRecycleByItemId(itemId);
                    if (recycle != null) {
                        recycleIds.add(recycle.getId());
                        itemIds.add(itemId);
                    }
                }
                int successCount = databaseManager.restoreItemsWithTransaction(recycleIds, itemIds);
                restoreSuccess.postValue(successCount > 0);
            } catch (Exception e) {
                Log.e(TAG, "批量恢复物品失败：", e);
                restoreSuccess.postValue(false);
            }
        });
    }

    /**
     * 批量彻底删除物品
     * @param selectedItemWithNameList 选中的物品列表（ItemWithName）
     */
    public void batchDeleteItems(List<com.baixiaosheng.inventory.database.entity.ItemWithName> selectedItemWithNameList) {
        executor.execute(() -> {
            try {
                List<Long> itemIds = new ArrayList<>();
                List<Long> recycleIds = new ArrayList<>();
                // 收集物品ID和回收站记录ID
                for (com.baixiaosheng.inventory.database.entity.ItemWithName itemWithName : selectedItemWithNameList) {
                    long itemId = itemWithName.item.getId();
                    itemIds.add(itemId);
                    Recycle recycle = databaseManager.getRecycleByItemId(itemId);
                    if (recycle != null) {
                        recycleIds.add(recycle.getId());
                    }
                }
                // 先删除回收站记录，再批量删除物品
                if (!recycleIds.isEmpty()) {
                    databaseManager.deleteRecycleItemsByIds(recycleIds);
                }
                int result = databaseManager.deleteItemsByIds(itemIds);
                deleteSuccess.postValue(result > 0);
            } catch (Exception e) {
                Log.e(TAG, "批量彻底删除物品失败：", e);
                deleteSuccess.postValue(false);
            }
        });
    }

    /**
     * 获取恢复操作结果的LiveData
     */
    public LiveData<Boolean> getRestoreSuccess() {
        return restoreSuccess;
    }

    /**
     * 获取删除操作结果的LiveData
     */
    public LiveData<Boolean> getDeleteSuccess() {
        return deleteSuccess;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 释放线程池资源
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * 获取已删除物品的LiveData（兼容原有逻辑）
     */
    public LiveData<List<Item>> getDeletedItemsLive() {
        return databaseManager.getDeletedItemsLive();
    }
}