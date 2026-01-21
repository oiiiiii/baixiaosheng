package com.baixiaosheng.inventory.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.baixiaosheng.inventory.database.DatabaseManager;
import com.baixiaosheng.inventory.database.entity.Item;
import java.util.List;

/**
 * 回收站页面ViewModel
 * 负责获取和管理回收站物品数据
 */
public class RecycleViewModel extends AndroidViewModel {
    private final DatabaseManager databaseManager;
    private LiveData<List<Item>> recycleItems;

    public RecycleViewModel(@NonNull Application application) {
        super(application);
        // 初始化数据库管理类
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
}