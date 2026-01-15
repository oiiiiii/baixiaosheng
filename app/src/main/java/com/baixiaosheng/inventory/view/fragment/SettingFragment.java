package com.baixiaosheng.inventory.view.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.model.SettingEntry;
import com.baixiaosheng.inventory.utils.DefaultDataUtils;
import com.baixiaosheng.inventory.view.activity.AboutActivity;
import com.baixiaosheng.inventory.view.activity.CategoryManageActivity;
import com.baixiaosheng.inventory.view.activity.LocationManageActivity;
import com.baixiaosheng.inventory.view.activity.RecycleActivity;
import com.baixiaosheng.inventory.view.adapter.SettingAdapter;
import com.baixiaosheng.inventory.view.activity.DataManageActivity;

import java.util.ArrayList;
import java.util.List;

public class SettingFragment extends Fragment {

    private RecyclerView rvSettingList;
    private SettingAdapter settingAdapter;
    private List<SettingEntry> entryList;
    // SettingFragment.java 完整的 onDestroyView 实现
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 释放 RecyclerView 和 Adapter 资源，移除视图引用
        if (rvSettingList != null) {
            rvSettingList.setAdapter(null); // 解除 SettingAdapter 绑定
            rvSettingList.removeAllViews(); // 移除所有子视图
            rvSettingList = null;
        }
        if (settingAdapter != null) {
            settingAdapter.setOnItemClickListener(null); // 现在方法已定义，可正常调用
            settingAdapter = null;
        }
        entryList = null;
    }
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (getView() != null) {
            // 非当前页时直接隐藏根视图，避免透显
            getView().setVisibility(isVisibleToUser ? View.VISIBLE : View.GONE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        // 初始化默认数据（仅首次启动时执行，内部已做重复校验）
        DefaultDataUtils.initAllDefaultData(getContext());

        initView(view);
        initSettingEntries();
        initAdapter();
        return view;
    }

    private void initView(View view) {
        rvSettingList = view.findViewById(R.id.rv_setting_list);
        rvSettingList.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void initSettingEntries() {
        entryList = new ArrayList<>();
        // 直接使用枚举常量，无需new
        entryList.add(SettingEntry.CATEGORY_MANAGE);
        entryList.add(SettingEntry.LOCATION_MANAGE);
        entryList.add(SettingEntry.RECYCLE_BIN);

        // 新增数据管理入口
        entryList.add(SettingEntry.DATA_MANAGE);
        entryList.add(SettingEntry.ABOUT);
    }

    private void initAdapter() {
        // 修复：构造Adapter时同时传入entryList和点击事件回调
        settingAdapter = new SettingAdapter(entryList, new SettingAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SettingEntry entry) {

                // 先校验上下文和Fragment状态，避免空指针
                if (getContext() == null || !isAdded() || isDetached()) {
                    return;
                }
                // 处理条目点击逻辑
                switch (entry) {
                    case CATEGORY_MANAGE:
                        startActivity(new Intent(requireContext(), CategoryManageActivity.class));
                        break;
                    case LOCATION_MANAGE:
                        startActivity(new Intent(requireContext(), LocationManageActivity.class));
                        break;
                    case RECYCLE_BIN:
                        startActivity(new Intent(requireContext(), RecycleActivity.class));
                        break;
                    case ABOUT:
                        startActivity(new Intent(requireContext(), AboutActivity.class));
                        break;
                    case DATA_MANAGE:
                        // 跳转数据管理页面
                        startActivity(new Intent(requireContext(), DataManageActivity.class));
                        break;

                    // 新增default分支，避免枚举扩展后漏处理
                    default:
                        break;
                }
            }
        });
        rvSettingList.setAdapter(settingAdapter);
    }
}