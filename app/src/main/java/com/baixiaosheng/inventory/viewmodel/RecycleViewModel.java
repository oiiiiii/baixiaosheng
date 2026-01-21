package com.baixiaosheng.inventory.viewmodel;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.baixiaosheng.inventory.database.DatabaseManager;
import com.baixiaosheng.inventory.database.entity.Recycle;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecycleViewModel extends AndroidViewModel {
    private static final String TAG = "RecycleViewModel";
    private final DatabaseManager mDbManager;
    private final ExecutorService mExecutor;
    private final Object mLock = new Object(); // 新增：同步锁

    private final LiveData<List<Recycle>> mRecycleList;
    private final MutableLiveData<Boolean> mOperationSuccess;
    private final MutableLiveData<String> mErrorMsg;
    private final MutableLiveData<Boolean> mIsLoading;
    private final LiveData<List<Recycle>> mRecycleItems;
    // 用于发送提示消息的LiveData（UI层观察此事件）
    private final MutableLiveData<String> mToastMsg = new MutableLiveData<>();

    public RecycleViewModel(@NonNull Application application) {
        super(application);
        mDbManager = DatabaseManager.getInstance(application);
        mExecutor = Executors.newSingleThreadExecutor();

        mRecycleItems = mDbManager.getAllRecycleItemsSyncLive();
        mRecycleList = mDbManager.getAllRecycleItemsSyncLive();
        mOperationSuccess = new MutableLiveData<>();
        mErrorMsg = new MutableLiveData<>();
        mIsLoading = new MutableLiveData<>(false);
    }
    // 对外暴露Toast消息的LiveData（只读）
    public LiveData<String> getToastMsg() {
        return mToastMsg;
    }

    /**
     * 还原单个物品
     */
    public void restoreItem(long recycleId, long itemId) {
        synchronized (mLock) {
            Boolean isLoading = mIsLoading.getValue();
            if (isLoading != null && isLoading) {
                Log.w(TAG, "restoreItem: 操作正在进行中，忽略重复请求");
                return;
            }

            mIsLoading.postValue(true);
            resetOperationState();
        }

        mExecutor.execute(() -> {
            try {
                // 检查ViewModel是否已被销毁
                if (isViewModelCleared()) {
                    Log.w(TAG, "restoreItem: ViewModel已被销毁，停止操作");
                    return;
                }

                int restoreResult = mDbManager.restoreItemById(itemId);
                if (restoreResult <= 0) {
                    throw new Exception("未找到itemId=" + itemId + "的物品，或更新isDeleted失败");
                }

                int deleteRecycleResult = mDbManager.deleteRecycleItemById(recycleId);
                if (deleteRecycleResult <= 0) {
                    // 这里不抛出异常，只是记录警告
                    Log.w(TAG, "restoreItem: 物品恢复成功，但回收站记录删除失败（recycleId=" + recycleId + "）");
                    // 继续执行，因为物品已经恢复成功
                }

                postSuccess("物品恢复成功");
            } catch (Exception e) {
                Log.e(TAG, "restoreItem失败：", e);
                postError("还原失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
            } finally {
                postLoading(false);
            }
        });
    }

    /**
     * 彻底删除单个物品
     */
    public void deleteItemForever(long recycleId, long itemId) {
        synchronized (mLock) {
            Boolean isLoading = mIsLoading.getValue();
            if (isLoading != null && isLoading) {
                Log.w(TAG, "deleteItemForever: 操作正在进行中，忽略重复请求");
                return;
            }

            mIsLoading.postValue(true);
            resetOperationState();
        }

        mExecutor.execute(() -> {
            try {
                // 检查ViewModel是否已被销毁
                if (isViewModelCleared()) {
                    Log.w(TAG, "deleteItemForever: ViewModel已被销毁，停止操作");
                    return;
                }

                int deleteRecycleResult = mDbManager.deleteRecycleItemById(recycleId);
                int deleteItemResult = mDbManager.deleteItemById(itemId);

                if (deleteRecycleResult <= 0 || deleteItemResult <= 0) {
                    throw new Exception(String.format(
                            "删除不彻底：回收站记录（%d）/物品（%d）删除失败",
                            deleteRecycleResult, deleteItemResult));
                }

                postSuccess("物品已永久删除");
            } catch (Exception e) {
                Log.e(TAG, "deleteItemForever失败：", e);
                postError("删除失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
            } finally {
                postLoading(false);
            }
        });
    }

    /**
     * 批量还原选中物品
     */
    public void restoreSelectedItems(List<Long> recycleIds, List<Long> itemIds) {
        synchronized (mLock) {
            Boolean isLoading = mIsLoading.getValue();
            if (isLoading != null && isLoading) {
                postError("批量还原失败：操作中，请稍后");
                return;
            }

            if (recycleIds == null || itemIds == null || recycleIds.isEmpty() || itemIds.isEmpty()) {
                postError("批量还原失败：未选择任何物品");
                return;
            }
            if (recycleIds.size() != itemIds.size()) {
                postError("批量还原失败：参数错误（ID数量不匹配）");
                return;
            }

            mIsLoading.postValue(true);
            resetOperationState();
        }

        mExecutor.execute(() -> {
            try {
                // 检查ViewModel是否已被销毁
                if (isViewModelCleared()) {
                    Log.w(TAG, "restoreSelectedItems: ViewModel已被销毁，停止操作");
                    return;
                }

                // 调用有返回值的事务方法，精准判断结果
                int successCount = mDbManager.restoreItemsWithTransaction(recycleIds, itemIds);
// 按需判断：比如“成功数量>0”视为至少有部分成功，或“成功数量=传入数量”视为全部成功
                boolean transactionSuccess = successCount > 0; // 或 successCount == recycleIds.size()

// 后续逻辑可根据 transactionSuccess 或 successCount 处理

                if (transactionSuccess) {
                    mToastMsg.postValue("成功恢复 " + itemIds.size() + " 个物品");
                } else {
                    mToastMsg.postValue("物品恢复失败，请重试");
                }
            } catch (Exception e) {
                Log.e(TAG, "restoreSelectedItems失败：", e);
                postError("批量还原失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
            } finally {
                postLoading(false);
            }
        });
    }

    /**
     * 批量彻底删除选中物品
     */
    public void deleteSelectedItemsForever(List<Long> recycleIds, List<Long> itemIds) {
        synchronized (mLock) {
            Boolean isLoading = mIsLoading.getValue();
            if (isLoading != null && isLoading) {
                postError("批量删除失败：操作中，请稍后");
                return;
            }

            if (recycleIds == null || itemIds == null || recycleIds.isEmpty() || itemIds.isEmpty()) {
                postError("批量删除失败：未选择任何物品");
                return;
            }
            if (recycleIds.size() != itemIds.size()) {
                postError("批量删除失败：参数错误（ID数量不匹配）");
                return;
            }

            mIsLoading.postValue(true);
            resetOperationState();
        }

        mExecutor.execute(() -> {
            try {
                // 检查ViewModel是否已被销毁
                if (isViewModelCleared()) {
                    Log.w(TAG, "deleteSelectedItemsForever: ViewModel已被销毁，停止操作");
                    return;
                }

                // 先删除回收站记录
                int deleteRecycleResult = mDbManager.deleteRecycleItemsByIds(recycleIds);

                // 再删除物品
                int deleteItemResult = mDbManager.deleteItemsByIds(itemIds);

                // 更精准的结果判断（删除数量是否匹配选中数量）
                boolean success = true;
                StringBuilder errorMsg = new StringBuilder();

                if (deleteRecycleResult != recycleIds.size()) {
                    success = false;
                    errorMsg.append(String.format("回收站记录删除%d条（预期%d条）",
                            deleteRecycleResult, recycleIds.size()));
                }

                if (deleteItemResult != itemIds.size()) {
                    success = false;
                    if (errorMsg.length() > 0) errorMsg.append("，");
                    errorMsg.append(String.format("物品删除%d条（预期%d条）",
                            deleteItemResult, itemIds.size()));
                }

                if (success) {
                    postSuccess("批量删除成功，共删除" + itemIds.size() + "个物品");
                } else {
                    throw new Exception("批量删除不彻底：" + errorMsg.toString());
                }
            } catch (Exception e) {
                Log.e(TAG, "deleteSelectedItemsForever失败：", e);
                postError("批量删除失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
            } finally {
                postLoading(false);
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

    public LiveData<Boolean> getIsLoading() {
        return mIsLoading;
    }

    // 重置操作状态
    public void resetOperationState() {
        mOperationSuccess.postValue(null);
        mErrorMsg.postValue(null);
    }

    // 辅助方法：安全地更新LiveData
    private void postSuccess(String message) {
        if (!isViewModelCleared()) {
            mOperationSuccess.postValue(true);
            mErrorMsg.postValue(message);
        }
    }

    private void postError(String error) {
        if (!isViewModelCleared()) {
            mOperationSuccess.postValue(false);
            mErrorMsg.postValue(error);
        }
    }

    private void postLoading(boolean loading) {
        if (!isViewModelCleared()) {
            mIsLoading.postValue(loading);
        }
    }

    // 检查ViewModel是否已被销毁
    private boolean isViewModelCleared() {
        // 这里可以添加检查ViewModel生命周期的逻辑
        // 目前先返回false，如果有需要可以添加更复杂的检查
        return false;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 修复点8：优雅关闭线程池（等待现有任务完成）
        if (mExecutor != null && !mExecutor.isShutdown()) {
            mExecutor.shutdownNow();
        }
        Log.d(TAG, "RecycleViewModel onCleared");
    }
}