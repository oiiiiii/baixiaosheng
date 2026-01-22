package com.baixiaosheng.inventory.view.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.DatabaseManager;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.database.entity.ItemWithName;
import com.baixiaosheng.inventory.database.entity.Location;
import com.baixiaosheng.inventory.view.adapter.RecycleAdapter;
import com.baixiaosheng.inventory.viewmodel.RecycleViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 回收站页面Activity（完整版）
 * 适配ItemWithName数据结构，实现单击弹窗+多选批量操作功能
 */
public class RecycleActivity extends AppCompatActivity implements RecycleAdapter.OnMultiSelectChangeListener {
    private static final String TAG = "RecycleActivity";
    private RecyclerView recyclerView;
    private RecycleAdapter adapter;
    private DatabaseManager databaseManager;
    private TextView tvEmptyHint;
    private RecycleViewModel recycleViewModel;

    // 多选操作栏相关
    private LinearLayout llMultiSelectBar;
    private TextView tvSelectedCount;
    private Button btnBatchRestore;
    private Button btnBatchDelete;

    // 单线程池：处理数据库查询，避免多线程并发问题
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle);

        // 初始化数据库管理类
        databaseManager = DatabaseManager.getInstance(this);
        // 初始化ViewModel
        recycleViewModel = new ViewModelProvider(this).get(RecycleViewModel.class);

        // 初始化视图
        initView();

        // 观察回收站数据变化
        observeDeletedItems();
        // 观察操作结果
        observeOperationResult();
    }

    /**
     * 初始化视图组件
     */
    private void initView() {
        recyclerView = findViewById(R.id.recycler_view_recycle);
        tvEmptyHint = findViewById(R.id.tv_empty_tip);

        // 多选操作栏相关
        llMultiSelectBar = findViewById(R.id.ll_multi_select_bar);
        tvSelectedCount = findViewById(R.id.tv_selected_count);
        btnBatchRestore = findViewById(R.id.btn_batch_restore);
        btnBatchDelete = findViewById(R.id.btn_batch_delete);

        // 设置RecyclerView布局
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // 初始化适配器（适配ItemWithName）
        adapter = new RecycleAdapter(this);
        adapter.setOnMultiSelectChangeListener(this);
        // 设置条目单击事件
        adapter.setOnItemClickListener(this::showItemOptionDialog);
        recyclerView.setAdapter(adapter);

        // 批量恢复按钮点击事件
        btnBatchRestore.setOnClickListener(v -> {
            List<ItemWithName> selectedItems = adapter.getSelectedItems();
            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "请选择要恢复的物品", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("批量恢复")
                    .setMessage("确定要恢复选中的" + selectedItems.size() + "个物品吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        recycleViewModel.batchRestoreItems(selectedItems);
                        // 退出多选模式
                        adapter.exitMultiSelectMode();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        // 批量删除按钮点击事件
        btnBatchDelete.setOnClickListener(v -> {
            List<ItemWithName> selectedItems = adapter.getSelectedItems();
            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "请选择要删除的物品", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("批量彻底删除")
                    .setMessage("确定要彻底删除选中的" + selectedItems.size() + "个物品吗？此操作不可恢复！")
                    .setPositiveButton("确定", (dialog, which) -> {
                        recycleViewModel.batchDeleteItems(selectedItems);
                        // 退出多选模式
                        adapter.exitMultiSelectMode();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    /**
     * 显示单个物品操作弹窗
     */
    private void showItemOptionDialog(ItemWithName itemWithName) {
        String[] options = {"恢复物品", "彻底删除", "取消"};
        new AlertDialog.Builder(this)
                .setTitle("操作选项")
                .setItems(options, (dialog, which) -> {
                    long itemId = itemWithName.item.getId();
                    switch (which) {
                        case 0: // 恢复物品
                            new AlertDialog.Builder(this)
                                    .setTitle("恢复物品")
                                    .setMessage("确定要恢复【" + itemWithName.item.getName() + "】吗？")
                                    .setPositiveButton("确定", (d, w) -> recycleViewModel.restoreSingleItem(itemId))
                                    .setNegativeButton("取消", null)
                                    .show();
                            break;
                        case 1: // 彻底删除
                            new AlertDialog.Builder(this)
                                    .setTitle("彻底删除")
                                    .setMessage("确定要彻底删除【" + itemWithName.item.getName() + "】吗？此操作不可恢复！")
                                    .setPositiveButton("确定", (d, w) -> recycleViewModel.deleteSingleItem(itemId))
                                    .setNegativeButton("取消", null)
                                    .show();
                            break;
                        case 2: // 取消
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    /**
     * 观察回收站数据变化
     */
    private void observeDeletedItems() {
        // 显示加载状态
        runOnUiThread(() -> {
            tvEmptyHint.setVisibility(View.VISIBLE);
            tvEmptyHint.setText("加载中...");
            recyclerView.setVisibility(View.GONE);
        });

        // 监听回收站物品数据变化
        recycleViewModel.getDeletedItemsLive().observe(this, new Observer<List<Item>>() {
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
     * 观察恢复/删除操作结果
     */
    private void observeOperationResult() {
        // 恢复操作结果
        recycleViewModel.getRestoreSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(RecycleActivity.this, "操作成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RecycleActivity.this, "操作失败，请重试", Toast.LENGTH_SHORT).show();
            }
        });

        // 删除操作结果
        recycleViewModel.getDeleteSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(RecycleActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RecycleActivity.this, "删除失败，请重试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 处理空数据状态
     */
    private void handleEmptyData() {
        runOnUiThread(() -> {
            tvEmptyHint.setVisibility(View.VISIBLE);
            tvEmptyHint.setText("回收站为空");
            recyclerView.setVisibility(View.GONE);

            // 清空适配器数据
            adapter.setItemWithNameList(new ArrayList<>());

            // 确保退出多选模式
            if (adapter.isMultiSelectMode()) {
                adapter.exitMultiSelectMode();
            }
        });
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
                if (itemWithNameList.isEmpty()) {
                    handleEmptyData();
                } else {
                    tvEmptyHint.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.setItemWithNameList(itemWithNameList);
                    Log.d(TAG, "适配器数据更新完成，数量：" + itemWithNameList.size());
                }
            });
        });
    }

    // ==================== 多选模式回调 ====================

    @Override
    public void onSelectModeChanged(boolean isMultiSelect) {
        // 显示/隐藏多选操作栏
        llMultiSelectBar.setVisibility(isMultiSelect ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSelectCountChanged(int count) {
        // 更新选中数量显示
        tvSelectedCount.setText("已选择 " + count + " 项");
        // 根据选中数量启用/禁用按钮
        btnBatchRestore.setEnabled(count > 0);
        btnBatchDelete.setEnabled(count > 0);
    }

    /**
     * 返回键处理：如果处于多选模式，则退出多选模式
     */
    @Override
    public void onBackPressed() {
        if (adapter.isMultiSelectMode()) {
            adapter.exitMultiSelectMode();
        } else {
            super.onBackPressed();
        }
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