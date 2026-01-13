package com.baixiaosheng.inventory.view.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.view.adapter.RecycleAdapter;
import com.baixiaosheng.inventory.database.entity.Recycle;
import com.baixiaosheng.inventory.viewmodel.RecycleViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 回收站页面
 */
public class RecycleActivity extends AppCompatActivity {

    private Button btnRestoreSelected;
    private Button btnDeleteSelected;
    private RecyclerView rvRecycleList;

    private RecycleViewModel mViewModel;
    private RecycleAdapter mAdapter;
    private List<Recycle> mRecycleList;
    private final List<Long> mSelectedRecycleIds = new ArrayList<>();
    private final List<Long> mSelectedItemIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle);
        initView();
        initViewModel();
        initListener();
    }

    private void initView() {
        btnRestoreSelected = findViewById(R.id.btn_restore_selected);
        btnDeleteSelected = findViewById(R.id.btn_delete_selected);
        rvRecycleList = findViewById(R.id.rv_recycle_list);
        rvRecycleList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initViewModel() {
        mViewModel = new ViewModelProvider(this).get(RecycleViewModel.class);

        // 观察回收站列表
        mViewModel.getRecycleList().observe(this, recycleList -> {
            mRecycleList = recycleList;
            if (mAdapter == null) {
                mAdapter = new RecycleAdapter(recycleList, this::onItemSelect, this::onRestoreClick, this::onDeleteForeverClick);
                rvRecycleList.setAdapter(mAdapter);
            } else {
                mAdapter.updateData(recycleList);
            }
            // 清空选中状态
            mSelectedRecycleIds.clear();
            mSelectedItemIds.clear();
        });

        // 观察操作结果
        mViewModel.getOperationSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "操作成功", Toast.LENGTH_SHORT).show();
            }
        });

        // 观察错误信息
        mViewModel.getErrorMsg().observe(this, errorMsg -> {
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        });
    }

    private void initListener() {
        // 批量还原按钮点击
        btnRestoreSelected.setOnClickListener(v -> {
            if (mSelectedRecycleIds.isEmpty()) {
                Toast.makeText(this, "请选择要还原的物品", Toast.LENGTH_SHORT).show();
                return;
            }
            mViewModel.restoreSelectedItems(mSelectedRecycleIds, mSelectedItemIds);
        });

        // 批量彻底删除按钮点击
        btnDeleteSelected.setOnClickListener(v -> {
            if (mSelectedRecycleIds.isEmpty()) {
                Toast.makeText(this, "请选择要删除的物品", Toast.LENGTH_SHORT).show();
                return;
            }
            mViewModel.deleteSelectedItemsForever(mSelectedRecycleIds, mSelectedItemIds);
        });
    }

    /**
     * 列表项选中状态变化
     */
    private void onItemSelect(long recycleId, long itemId, boolean isSelected) {
        if (isSelected) {
            mSelectedRecycleIds.add(recycleId);
            mSelectedItemIds.add(itemId);
        } else {
            mSelectedRecycleIds.remove(recycleId);
            mSelectedItemIds.remove(itemId);
        }
    }

    /**
     * 单个还原点击
     */
    private void onRestoreClick(long recycleId, long itemId) {
        mViewModel.restoreItem(recycleId, itemId);
    }

    /**
     * 单个彻底删除点击
     */
    private void onDeleteForeverClick(long recycleId, long itemId) {
        mViewModel.deleteItemForever(recycleId, itemId);
    }
}