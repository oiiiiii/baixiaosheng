package com.baixiaosheng.inventory.viewmodel;

import android.app.Application;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baixiaosheng.inventory.database.InventoryDatabase;
import com.baixiaosheng.inventory.database.entity.Item;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 录入页ViewModel
 * 为什么这么写：遵循MVVM架构，将数据操作和业务逻辑从View层分离，避免内存泄漏
 */
public class InputViewModel extends AndroidViewModel {
    private final InventoryDatabase mDb;
    private final ExecutorService mExecutorService;
    private final MutableLiveData<Boolean> mSaveSuccess = new MutableLiveData<>();

    public InputViewModel(@NonNull Application application) {
        super(application);
        mDb = InventoryDatabase.getInstance(application);
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 保存物品信息到数据库
     */
    public void saveItem(Item item) {
        // 移除错误的id设置：id是Room自增主键，无需手动设置
        // 若需要UUID标识，应设置uuid字段而非id字段
        item.setUuid(java.util.UUID.randomUUID().toString());

        // 补充：设置创建/更新时间（可选，根据业务需求）
        long currentTime = System.currentTimeMillis();
        item.setCreateTime(currentTime);
        item.setUpdateTime(currentTime);

        // 子线程执行数据库操作（Room不允许主线程操作）
        mExecutorService.execute(() -> {
            try {
                // 打印Item数据，排查字段异常
                Log.d("InputViewModel", "保存Item: " + item.toString());
                mDb.itemDao().insertItem(item);
                mSaveSuccess.postValue(true);
            } catch (SQLiteException e) {
                Log.e("InputViewModel", "数据库插入失败: " + e.getMessage());
                // 区分字段过长/类型错误
                if (e.getMessage().contains("too long")) {
                    Log.e("InputViewModel", "图片路径过长：" + item.getImagePaths());
                }
                mSaveSuccess.postValue(false);
            } catch (Exception e) {
                Log.e("InputViewModel", "保存失败: " + e.getMessage());
                mSaveSuccess.postValue(false);
                e.printStackTrace();
            }
        });
    }

    /**
     * 获取保存结果的LiveData
     */
    public LiveData<Boolean> getSaveSuccess() {
        return mSaveSuccess;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 释放线程池
        mExecutorService.shutdown();
    }
}