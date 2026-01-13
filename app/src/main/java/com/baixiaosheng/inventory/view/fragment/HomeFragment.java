package com.baixiaosheng.inventory.view.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.view.adapter.ExpiredItemAdapter;
import com.baixiaosheng.inventory.viewmodel.HomeViewModel;

import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {
    private HomeViewModel homeViewModel;
    private ExpiredItemAdapter itemAdapter;
    private RecyclerView rvExpiredItems;
    private TextView tvEmpty;
    private SearchView svSearch;
    private Button btnDateFilter;
    private LinearLayout llSearchFilter;

    // 当前筛选条件
    private String currentKeyword = "";
    private Long currentEndDate = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(rootView);
        initViewModel();
        setupRecyclerView();
        setupSearchView();
        setupDateFilter();

        return rootView;
    }

    private void initViews(View rootView) {
        rvExpiredItems = rootView.findViewById(R.id.rv_home_expired_item);
        tvEmpty = rootView.findViewById(R.id.tv_empty);
        svSearch = rootView.findViewById(R.id.sv_home_search);
        btnDateFilter = rootView.findViewById(R.id.btn_home_date_filter);
        llSearchFilter = rootView.findViewById(R.id.ll_search_filter);
    }

    private void initViewModel() {
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // 关键修复：确保观察者立即生效
        homeViewModel.getExpiredItems().observe(getViewLifecycleOwner(), new Observer<List<Item>>() {
            @Override
            public void onChanged(List<Item> items) {
                if (items != null) {
                    updateUI(items);
                }
            }
        });

        // 立即加载数据
        loadInitialData();
    }

    private void setupRecyclerView() {
        rvExpiredItems.setLayoutManager(new LinearLayoutManager(getContext()));
        itemAdapter = new ExpiredItemAdapter();
        rvExpiredItems.setAdapter(itemAdapter);
    }

    private void setupSearchView() {
        // 设置搜索框默认展开
        svSearch.setIconified(false);
        svSearch.setOnQueryTextFocusChangeListener(null);

        svSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // 搜索提交
                currentKeyword = query;
                performSearch();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                 // 实时搜索（可选，根据需要开启）
                 currentKeyword = newText;
                 if (newText.isEmpty()) {
                     performSearch();
                 }
                return false;
            }
        });

        // 搜索框关闭时也触发搜索（清除搜索条件时）
        svSearch.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                currentKeyword = "";
                performSearch();
                return false;
            }
        });
    }

    private void setupDateFilter() {
        btnDateFilter.setOnClickListener(v -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);
                    currentEndDate = selectedDate.getTimeInMillis();

                    // 更新按钮文本显示选中的日期
                    String dateStr = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay;
                    btnDateFilter.setText(dateStr);

                    performSearch();
                },
                year, month, day
        );

        datePickerDialog.setTitle("查询选定日期前过期的物品");
        datePickerDialog.show();
    }

    private void loadInitialData() {
        // 首次加载：显示所有过期物品（截止到当前时间）
        homeViewModel.loadExpiredItems(null, System.currentTimeMillis());
    }

    private void performSearch() {
        String keyword = svSearch.getQuery().toString().trim();

        // 如果没有结束日期，使用当前时间
        Long endDate = currentEndDate != null ? currentEndDate : System.currentTimeMillis();

        // 根据是否有关键词选择不同的查询方式
        if (!keyword.isEmpty()) {
            // 需要添加searchItems方法到ViewModel
            homeViewModel.searchExpiredItems(keyword, null,endDate);
        } else {
            // 使用日期查询
            homeViewModel.loadExpiredItems(null, endDate);
        }

        // 滚动到顶部
        if (rvExpiredItems.getLayoutManager() != null) {
            rvExpiredItems.getLayoutManager().scrollToPosition(0);
        }
    }

    private void updateUI(List<Item> items) {
        if (items.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvExpiredItems.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvExpiredItems.setVisibility(View.VISIBLE);
            itemAdapter.updateData(items);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 确保数据是最新的
        if (homeViewModel != null) {
            performSearch();
        }
    }
}