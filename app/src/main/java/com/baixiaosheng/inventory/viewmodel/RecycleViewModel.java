package com.baixiaosheng.inventory.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baixiaosheng.inventory.database.InventoryDatabase;
import com.baixiaosheng.inventory.database.dao.RecycleDao;
import com.baixiaosheng.inventory.database.dao.ItemDao;
import com.baixiaosheng.inventory.database.entity.Recycle;
import com.baixiaosheng.inventory.database.entity.Item;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 回收站ViewModel
 */
public class RecycleViewModel extends AndroidViewModel {

    private final RecycleDao mRecycleDao;
    private final ItemDao mItemDao;
    private final ExecutorService mExecutorService;
    private final MutableLiveData<List<Recycle>> mRecycleList;
    private final MutableLiveData<Boolean> mOperationSuccess;
    private final MutableLiveData<String> mErrorMsg;

    public RecycleViewModel(@NonNull Application application) {
        super(application);
        InventoryDatabase database = InventoryDatabase.getInstance(application);
        mRecycleDao = database.recycleDao();
        mItemDao = database.itemDao();
        mExecutorService = Executors.newSingleThreadExecutor();
        mRecycleList = new MutableLiveData<>();
        mOperationSuccess = new MutableLiveData<>();
        mErrorMsg = new MutableLiveData<>();
        // 加载回收站数据
        loadRecycleBinData();
    }

    /**
     * 加载回收站数据
     */
    public void loadRecycleBinData() {
        mExecutorService.execute(() -> {
            List<Recycle> recycleList = mRecycleDao.getAllRecycleItems();
            mRecycleList.postValue(recycleList);
        });
    }

    /**
     * 还原单个物品
     */
    public void restoreItem(long recycleId, long itemId) {
        mExecutorService.execute(() -> {
            try {
                // 1. 从回收站删除记录
                mRecycleDao.deleteRecycleItemById(recycleId);
                // 2. 恢复物品（如果需要标记物品状态）
                Item item = mItemDao.getItemById(itemId);
                if (item != null) {
                    // 此处假设物品表有isDeleted字段，还原时置为false
                    item.setIsDeleted(0);
                    mItemDao.updateItem(item);
                }
                mOperationSuccess.postValue(true);
                // 重新加载列表
                loadRecycleBinData();
            } catch (Exception e) {
                mErrorMsg.postValue("还原失败：" + e.getMessage());
                mOperationSuccess.postValue(false);
            }
        });
    }

    /**
     * 彻底删除单个物品
     */
    public void deleteItemForever(long recycleId, long itemId) {
        mExecutorService.execute(() -> {
            try {
                // 1. 从回收站删除记录
                mRecycleDao.deleteRecycleItemById(recycleId);
                // 2. 彻底删除物品
                mItemDao.deleteItemById(itemId);
                mOperationSuccess.postValue(true);
                // 重新加载列表
                loadRecycleBinData();
            } catch (Exception e) {
                mErrorMsg.postValue("删除失败：" + e.getMessage());
                mOperationSuccess.postValue(false);
            }
        });
    }

    /**
     * 批量还原选中物品
     */
    public void restoreSelectedItems(List<Long> recycleIds, List<Long> itemIds) {
        mExecutorService.execute(() -> {
            try {
                // 1. 批量删除回收站记录
                mRecycleDao.deleteRecycleItemsByIds(recycleIds);
                // 2. 批量恢复物品
                for (long itemId : itemIds) {
                    Item item = mItemDao.getItemById(itemId);
                    if (item != null) {
                        item.setIsDeleted(0);
                        mItemDao.updateItem(item);
                    }
                }
                mOperationSuccess.postValue(true);
                // 重新加载列表
                loadRecycleBinData();
            } catch (Exception e) {
                mErrorMsg.postValue("批量还原失败：" + e.getMessage());
                mOperationSuccess.postValue(false);
            }
        });
    }

    /**
     * 批量彻底删除选中物品
     */
    public void deleteSelectedItemsForever(List<Long> recycleIds, List<Long> itemIds) {
        mExecutorService.execute(() -> {
            try {
                // 1. 批量删除回收站记录
                mRecycleDao.deleteRecycleItemsByIds(recycleIds);
                // 2. 批量删除物品
                mItemDao.deleteItemsByIds(itemIds);
                mOperationSuccess.postValue(true);
                // 重新加载列表
                loadRecycleBinData();
            } catch (Exception e) {
                mErrorMsg.postValue("批量删除失败：" + e.getMessage());
                mOperationSuccess.postValue(false);
            }
        });
    }

    // 对外暴露LiveData
    public LiveData<List<Recycle>> getRecycleList() {
        return mRecycleList;
    }

    public LiveData<Boolean> getOperationSuccess() {
        return mOperationSuccess;
    }

    public LiveData<String> getErrorMsg() {
        return mErrorMsg;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mExecutorService.shutdown();
    }
}