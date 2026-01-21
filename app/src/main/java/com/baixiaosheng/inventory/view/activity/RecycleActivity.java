package com.baixiaosheng.inventory.view.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.DatabaseManager;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.database.entity.ItemWithName;
import com.baixiaosheng.inventory.database.entity.Location;
import com.baixiaosheng.inventory.view.adapter.RecycleAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 回收站页面Activity（完整版）
 * 适配ItemWithName数据结构，复用QueryAdapter字段逻辑
 */
public class RecycleActivity extends AppCompatActivity {
    private static final String TAG = "RecycleActivity";
    private RecyclerView recyclerView;
    private RecycleAdapter adapter;
    private DatabaseManager databaseManager;
    private TextView tvEmptyHint;
    // 单线程池：处理数据库查询，避免多线程并发问题
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle);

        // 初始化数据库管理类
        databaseManager = DatabaseManager.getInstance(this);

        // 初始化视图
        initView();

        // 观察回收站数据变化
        observeDeletedItems();
    }

    /**
     * 初始化视图组件
     */
    private void initView() {
        recyclerView = findViewById(R.id.recycler_view_recycle);
        tvEmptyHint = findViewById(R.id.tv_empty_tip);

        // 设置RecyclerView布局
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // 初始化适配器（适配ItemWithName）
        adapter = new RecycleAdapter(this);
        recyclerView.setAdapter(adapter);
    }

    /**
     * 观察已删除物品的LiveData数据
     */
    private void observeDeletedItems() {
        // 显示加载状态
        tvEmptyHint.setVisibility(View.VISIBLE);
        tvEmptyHint.setText("加载中...");

        // 监听回收站物品数据变化
        databaseManager.getDeletedItemsLive().observe(this, new Observer<List<Item>>() {
            @Override
            public void onChanged(List<Item> items) {
                Log.d(TAG, "LiveData数据变化，物品数量：" + (items == null ? 0 : items.size()));

                if (items == null || items.isEmpty()) {
                    // 空数据处理
                    handleEmptyData();
                } else {
                    // 有数据：加载分类/位置名称并更新UI
                    loadItemWithNameData(items);
                }
            }
        });
    }

    /**
     * 处理空数据状态
     */
    private void handleEmptyData() {
        tvEmptyHint.setVisibility(View.VISIBLE);
        tvEmptyHint.setText("回收站为空");
        // 清空适配器数据
        adapter.setItemWithNameList(new ArrayList<>());
    }

    /**
     * 加载ItemWithName数据（包含分类、位置名称）
     * @param items 原始物品列表
     */
    private void loadItemWithNameData(List<Item> items) {
        executor.execute(() -> {
            List<ItemWithName> itemWithNameList = new ArrayList<>();

            for (Item item : items) {
                // 过滤非删除状态的物品（双重校验）
                if (item.getIsDeleted() != 1) {
                    continue;
                }

                // 构建ItemWithName对象（复用QueryAdapter逻辑）
                ItemWithName itemWithName = new ItemWithName();
                itemWithName.item = item;

                // ========== 1. 处理分类名称（父分类+子分类） ==========
                // 父分类名称
                Category parentCategory = databaseManager.getCategoryById(item.getParentCategoryId());
                itemWithName.parentCategoryName = (parentCategory != null) ? parentCategory.getCategoryName() : null;
                // 子分类名称
                Category childCategory = databaseManager.getCategoryById(item.getChildCategoryId());
                itemWithName.categoryName = (childCategory != null) ? childCategory.getCategoryName() : null;

                // ========== 2. 处理位置名称 ==========
                Location location = databaseManager.getLocationById(item.getLocationId());
                itemWithName.locationName = (location != null) ? location.getName() : null;

                // 添加到列表
                itemWithNameList.add(itemWithName);
            }

            // 切换到主线程更新UI
            runOnUiThread(() -> {
                tvEmptyHint.setVisibility(View.GONE);
                adapter.setItemWithNameList(itemWithNameList);
                Log.d(TAG, "适配器数据更新完成，数量：" + itemWithNameList.size());
            });
        });
    }

    /**
     * 释放资源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭线程池，避免内存泄漏
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}