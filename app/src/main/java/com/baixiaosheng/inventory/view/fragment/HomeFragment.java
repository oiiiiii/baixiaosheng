package com.baixiaosheng.inventory.view.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.view.adapter.ExpiredItemAdapter;
import com.baixiaosheng.inventory.viewmodel.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private HomeViewModel homeViewModel;
    private ExpiredItemAdapter itemAdapter;
    private RecyclerView rvExpiredItems;
    private Long startDate;
    private Long endDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. 初始化ViewModel（使用requireActivity()保证上下文稳定）
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // 2. 初始化RecyclerView（修复Adapter构造器参数问题）
        rvExpiredItems = rootView.findViewById(R.id.rv_home_expired_item);
        rvExpiredItems.setLayoutManager(new LinearLayoutManager(getContext()));
        // 核心修复：传入空ArrayList匹配ExpiredItemAdapter的List<Item>参数要求
        itemAdapter = new ExpiredItemAdapter(new ArrayList<>());
        rvExpiredItems.setAdapter(itemAdapter);

        // 3. 观察过期物品数据（优化数据更新逻辑，避免直接修改Adapter成员变量）
        homeViewModel.getExpiredItems().observe(getViewLifecycleOwner(), new Observer<List<Item>>() {
            @Override
            public void onChanged(List<Item> items) {
                if (items != null) {
                    // 推荐：如果Adapter有updateData方法，优先使用（规范写法）
                    itemAdapter.updateData(items);
                    // 备用：若Adapter无updateData方法，才用以下方式（不推荐直接修改成员变量）
                    // itemAdapter.itemList = items;
                    // itemAdapter.notifyDataSetChanged();

                    // 空数据提示
                    if (items.isEmpty()) {
                        Toast.makeText(getContext(), "暂无过期物品", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 加载默认过期物品（当前时间前的所有过期物品）
        homeViewModel.loadExpiredItems(null, null);
    }
}