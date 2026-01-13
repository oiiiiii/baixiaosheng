package com.baixiaosheng.inventory.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.baixiaosheng.inventory.database.InventoryDatabase;
import com.baixiaosheng.inventory.database.entity.Item;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;

/**
 * 录入页ViewModel（修复数据库操作线程和异常处理）
 */
public class InputViewModel extends AndroidViewModel {
    private final InventoryDatabase mDb;
    private final ExecutorService mExecutor;
    private final MutableLiveData<Boolean> mSaveSuccess = new MutableLiveData<>();

    public InputViewModel(@NonNull Application application) {
        super(application);
        mDb = InventoryDatabase.getInstance(application);
        mExecutor = Executors.newSingleThreadExecutor(); // 单线程池，避免并发问题
    }

    /**
     * 保存物品（修复字段过长、类型不匹配问题）
     */
    public void saveItem(Item item) {
        mExecutor.execute(() -> {
            try {
                // 检查图片路径长度（避免数据库字段溢出）
                if (item.getImagePaths() != null && item.getImagePaths().length() > 500) {
                    Log.e("InputViewModel", "图片路径过长，截断处理");
                    item.setImagePaths(item.getImagePaths().substring(0, 500));
                }

                // 插入数据库
                long[] ids = mDb.itemDao().insertItem(item);
                mSaveSuccess.postValue(ids != null && ids.length > 0 && ids[0] != -1);
            } catch (Exception e) {
                Log.e("InputViewModel", "保存失败：" + e.getMessage());
                mSaveSuccess.postValue(false);
            }
        });
    }

    /**
     * 获取保存结果
     */
    public MutableLiveData<Boolean> getSaveSuccess() {
        return mSaveSuccess;
    }

    /**
     * 释放线程池，避免内存泄漏
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        if (!mExecutor.isShutdown()) {
            mExecutor.shutdown();
        }
    }
}