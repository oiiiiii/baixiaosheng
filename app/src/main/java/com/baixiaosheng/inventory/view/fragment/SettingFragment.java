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
import com.baixiaosheng.inventory.view.adapter.SettingAdapter;
import com.baixiaosheng.inventory.model.SettingEntry;
import com.baixiaosheng.inventory.view.activity.AboutActivity;
import com.baixiaosheng.inventory.view.activity.CategoryManageActivity;
import com.baixiaosheng.inventory.view.activity.LocationManageActivity;
import com.baixiaosheng.inventory.view.activity.RecycleActivity;

import java.util.Arrays;
import java.util.List;

/**
 * 设置页面Fragment
 */
public class SettingFragment extends Fragment {

    private RecyclerView rvSettingList;
    private SettingAdapter settingAdapter;
    private List<SettingEntry> entryList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        initData();
        initListener();
    }

    private void initView(View view) {
        rvSettingList = view.findViewById(R.id.rv_setting_list);
        // 设置布局管理器
        rvSettingList.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void initData() {
        // 初始化设置项列表
        entryList = Arrays.asList(SettingEntry.values());
        settingAdapter = new SettingAdapter(entryList, this::onItemClick);
        rvSettingList.setAdapter(settingAdapter);
    }

    private void initListener() {
        // 暂无额外监听
    }

    /**
     * 列表项点击事件
     */
    private void onItemClick(SettingEntry entry) {
        Intent intent = null;
        switch (entry) {
            case CATEGORY_MANAGE:
                intent = new Intent(getContext(), CategoryManageActivity.class);
                break;
            case LOCATION_MANAGE:
                intent = new Intent(getContext(), LocationManageActivity.class);
                break;
            case RECYCLE_BIN:
                intent = new Intent(getContext(), RecycleActivity.class);
                break;
            case ABOUT:
                intent = new Intent(getContext(), AboutActivity.class);
                break;
        }
        if (intent != null) {
            startActivity(intent);
        }
    }
}