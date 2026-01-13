package com.baixiaosheng.inventory.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baixiaosheng.inventory.database.DatabaseManager;
import com.baixiaosheng.inventory.database.entity.Item;

import java.util.List;

public class HomeViewModel extends ViewModel {
    private MutableLiveData<List<Item>> mExpiredItems;
    private DatabaseManager mDbManager;

    public HomeViewModel() {
        mExpiredItems = new MutableLiveData<>();
        // 移除原错误的无参getInstance()调用，改为延迟初始化
    }

    // 新增：初始化DatabaseManager的方法（必须先调用这个方法）
    public void init(Context context) {
        if (mDbManager == null) {
            // 使用Application Context避免内存泄漏
            mDbManager = DatabaseManager.getInstance(context.getApplicationContext());
        }
    }

    // 暴露给Fragment的LiveData（只读）
    public LiveData<List<Item>> getExpiredItems() {
        return mExpiredItems;
    }

    // 加载过期物品逻辑（修复空指针、参数容错）
    public void loadExpiredItems(Long startDate, Long endDate) {
        // 判空保护：确保mDbManager已初始化
        if (mDbManager == null) {
            mExpiredItems.postValue(null);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 处理默认时间范围：startDate默认0，endDate默认当前时间
                long actualStart = startDate == null ? 0 : startDate;
                long actualEnd = endDate == null ? System.currentTimeMillis() : endDate;

                // 查询未删除（isDeleted=0）的过期物品
                List<Item> expiredItems = mDbManager.getExpiredItems(
                        System.currentTimeMillis(),
                        actualStart,
                        actualEnd,
                        0
                );
                // 切换到主线程更新LiveData
                mExpiredItems.postValue(expiredItems);
            }
        }).start();
    }

    // 模糊搜索过期物品（补充空指针、关键词容错）
    public void searchExpiredItems(String keyword, Long startDate, Long endDate) {
        // 判空保护
        if (mDbManager == null) {
            mExpiredItems.postValue(null);
            return;
        }
        // 关键词空值处理，避免SQL语法错误
        String actualKeyword = keyword == null ? "" : keyword;

        new Thread(new Runnable() {
            @Override
            public void run() {
                long actualStart = startDate == null ? 0 : startDate;
                long actualEnd = endDate == null ? System.currentTimeMillis() : endDate;

                List<Item> items = mDbManager.searchExpiredItems(
                        "%" + actualKeyword + "%",
                        System.currentTimeMillis(),
                        actualStart,
                        actualEnd,
                        0
                );
                mExpiredItems.postValue(items);
            }
        }).start();
    }

    // ViewModel销毁时清理资源，避免内存泄漏
    @Override
    protected void onCleared() {
        super.onCleared();
        mExpiredItems = null;
        mDbManager = null;
    }
}