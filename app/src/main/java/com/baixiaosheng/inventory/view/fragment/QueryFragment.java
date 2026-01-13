package com.baixiaosheng.inventory.view.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.view.adapter.InventoryQueryAdapter;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.model.FilterCondition;
import com.baixiaosheng.inventory.viewmodel.QueryViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 查询页Fragment：核心筛选+列表交互逻辑
 */
public class QueryFragment extends Fragment {
    // View
    private SearchView svSearch;
    private LinearLayout llFilterTitle, llFilterContent, llBatchOperate;
    private ImageView ivFilterArrow;
    private Spinner spParentCategory, spChildCategory, spLocation;
    private EditText etQuantityMin, etQuantityMax;
    private TextView tvExpireStart, tvExpireEnd, tvSelectedCount, tvEmptyTip;
    private Button btnResetFilter, btnApplyFilter, btnBatchDelete;
    private RecyclerView rvInventoryList;
    // 适配器
    private InventoryQueryAdapter adapter;
    // ViewModel
    private QueryViewModel queryViewModel;
    // 筛选条件
    private final FilterCondition filterCondition = new FilterCondition();
    // 日期格式化
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_query, container, false);
        // 初始化View
        initView(view);
        // 初始化ViewModel
        queryViewModel = new ViewModelProvider(this).get(QueryViewModel.class);
        // 初始化适配器
        initAdapter();
        // 绑定数据观察
        bindViewModel();
        // 绑定事件
        bindEvents();
        return view;
    }

    // 初始化View
    private void initView(View view) {
        svSearch = view.findViewById(R.id.sv_search);
        llFilterTitle = view.findViewById(R.id.ll_filter_title);
        llFilterContent = view.findViewById(R.id.ll_filter_content);
        ivFilterArrow = view.findViewById(R.id.iv_filter_arrow);
        spParentCategory = view.findViewById(R.id.sp_parent_category);
        spChildCategory = view.findViewById(R.id.sp_child_category);
        spLocation = view.findViewById(R.id.sp_location);
        etQuantityMin = view.findViewById(R.id.et_quantity_min);
        etQuantityMax = view.findViewById(R.id.et_quantity_max);
        tvExpireStart = view.findViewById(R.id.tv_expire_start);
        tvExpireEnd = view.findViewById(R.id.tv_expire_end);
        btnResetFilter = view.findViewById(R.id.btn_reset_filter);
        btnApplyFilter = view.findViewById(R.id.btn_apply_filter);
        llBatchOperate = view.findViewById(R.id.ll_batch_operate);
        tvSelectedCount = view.findViewById(R.id.tv_selected_count);
        btnBatchDelete = view.findViewById(R.id.btn_batch_delete);
        rvInventoryList = view.findViewById(R.id.rv_inventory_list);
        tvEmptyTip = view.findViewById(R.id.tv_empty_tip);

        // 设置RecyclerView布局
        rvInventoryList.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    // 初始化适配器
    private void initAdapter() {
        adapter = new InventoryQueryAdapter(getContext());
        rvInventoryList.setAdapter(adapter);

        // 设置适配器回调
        adapter.setOnItemClickListener(this::showItemOperationDialog);
        adapter.setOnMultiSelectChangeListener(new InventoryQueryAdapter.OnMultiSelectChangeListener() {
            @Override
            public void onSelectModeChanged(boolean isMultiSelect) {
                // 显示/隐藏批量操作区
                llBatchOperate.setVisibility(isMultiSelect ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onSelectCountChanged(int count) {
                // 更新选中数量
                tvSelectedCount.setText("已选择：" + count + " 项");
                // 批量删除按钮状态
                btnBatchDelete.setEnabled(count > 0);
            }
        });
    }

    // 绑定ViewModel数据
    private void bindViewModel() {
        // 观察物品列表
        queryViewModel.getItemList().observe(getViewLifecycleOwner(), items -> {
            if (items == null || items.isEmpty()) {
                rvInventoryList.setVisibility(View.GONE);
                tvEmptyTip.setVisibility(View.VISIBLE);
            } else {
                rvInventoryList.setVisibility(View.VISIBLE);
                tvEmptyTip.setVisibility(View.GONE);
                adapter.setItemList(items);
            }
        });

        // 观察父分类列表
        queryViewModel.getParentCategoryList().observe(getViewLifecycleOwner(), categories -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, categories);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spParentCategory.setAdapter(adapter);
        });

        // 观察子分类列表
        queryViewModel.getChildCategoryList().observe(getViewLifecycleOwner(), categories -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, categories);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spChildCategory.setAdapter(adapter);
        });

        // 观察位置列表
        queryViewModel.getLocationList().observe(getViewLifecycleOwner(), locations -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, locations);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spLocation.setAdapter(adapter);
        });
    }

    // 绑定事件
    private void bindEvents() {
        // 搜索框事件
        svSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterCondition.setSearchKeyword(query);
                queryViewModel.queryItems(filterCondition);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCondition.setSearchKeyword(newText);
                queryViewModel.queryItems(filterCondition);
                return true;
            }
        });

        // 筛选区折叠/展开
        llFilterTitle.setOnClickListener(v -> {
            boolean isExpanded = llFilterContent.getVisibility() == View.VISIBLE;
            llFilterContent.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            ivFilterArrow.setRotation(isExpanded ? 0 : 180);
        });

        // 父分类选择：联动子分类
        spParentCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String parentCategory = (String) parent.getItemAtPosition(position);
                filterCondition.setParentCategory(parentCategory);
                // 加载对应子分类
                queryViewModel.loadChildCategories(parentCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 子分类选择
        spChildCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String childCategory = (String) parent.getItemAtPosition(position);
                filterCondition.setChildCategory(childCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 位置选择
        spLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String location = (String) parent.getItemAtPosition(position);
                filterCondition.setLocation(location);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 过期开始日期选择
        tvExpireStart.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(getContext(), (view1, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);
                filterCondition.setExpireStart(selected.getTime());
                tvExpireStart.setText(dateFormat.format(selected.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        // 过期结束日期选择
        tvExpireEnd.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(getContext(), (view1, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);
                filterCondition.setExpireEnd(selected.getTime());
                tvExpireEnd.setText(dateFormat.format(selected.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        // 重置筛选
        btnResetFilter.setOnClickListener(v -> {
            filterCondition.reset();
            // 清空UI
            svSearch.setQuery("", false);
            etQuantityMin.setText("");
            etQuantityMax.setText("");
            tvExpireStart.setText("");
            tvExpireEnd.setText("");
            spParentCategory.setSelection(0);
            spChildCategory.setSelection(0);
            spLocation.setSelection(0);
            // 重新查询
            queryViewModel.queryItems(filterCondition);
        });

        // 应用筛选
        btnApplyFilter.setOnClickListener(v -> {
            // 解析数量范围
            try {
                String minStr = etQuantityMin.getText().toString().trim();
                Integer min = minStr.isEmpty() ? null : Integer.parseInt(minStr);
                filterCondition.setQuantityMin(min);

                String maxStr = etQuantityMax.getText().toString().trim();
                Integer max = maxStr.isEmpty() ? null : Integer.parseInt(maxStr);
                filterCondition.setQuantityMax(max);

                // 验证数量范围
                if (min != null && max != null && min > max) {
                    Toast.makeText(getContext(), "最小值不能大于最大值", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "数量请输入数字", Toast.LENGTH_SHORT).show();
                return;
            }

            // 执行筛选
            queryViewModel.queryItems(filterCondition);
            Toast.makeText(getContext(), "筛选条件已应用", Toast.LENGTH_SHORT).show();
        });

        // 批量删除
        btnBatchDelete.setOnClickListener(v -> {
            List<String> selectedUuids = adapter.getSelectedUuids();
            new AlertDialog.Builder(getContext())
                    .setTitle("批量删除")
                    .setMessage("确定要将选中的" + selectedUuids.size() + "项物品移入回收站吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        queryViewModel.batchDeleteItems(selectedUuids);
                        adapter.setMultiSelectMode(false);
                        Toast.makeText(getContext(), "批量删除成功", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    // 显示物品操作弹窗
    private void showItemOperationDialog(Item item) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_item_operation, null);
        Button btnViewDetail = dialogView.findViewById(R.id.btn_view_detail);
        Button btnEditItem = dialogView.findViewById(R.id.btn_edit_item);
        Button btnDeleteItem = dialogView.findViewById(R.id.btn_delete_item);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        dialog.show();

        // 查看详情
        btnViewDetail.setOnClickListener(v -> {
            dialog.dismiss();
            showItemDetailDialog(item);
        });

        // 编辑（阶段7扩展，此处预留）
        btnEditItem.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(getContext(), "编辑功能将在后续阶段实现", Toast.LENGTH_SHORT).show();
        });

        // 删除
        btnDeleteItem.setOnClickListener(v -> {
            dialog.dismiss();
            new AlertDialog.Builder(getContext())
                    .setTitle("删除物品")
                    .setMessage("确定要将【" + item.getName() + "】移入回收站吗？")
                    .setPositiveButton("确定", (d, which) -> {
                        queryViewModel.deleteItem(item.getUuid());
                        Toast.makeText(getContext(), "删除成功", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    // 显示物品详情弹窗
    private void showItemDetailDialog(Item item) {
        StringBuilder detail = new StringBuilder();
        detail.append("物品名称：").append(item.getName()).append("\n");
        detail.append("唯一标识：").append(item.getUuid()).append("\n");
        detail.append("父分类：").append(item.getParentCategoryId()).append("\n");
        detail.append("子分类：").append(item.getChildCategoryId()).append("\n");
        detail.append("放置位置：").append(item.getLocationId() != 0 ? "未设置" : item.getLocationId()).append("\n");
        detail.append("数量：").append(item.getCount()).append("\n");
        detail.append("过期时间：").append(item.getValidTime() != 0 ? dateFormat.format(item.getValidTime()) : "无").append("\n");
        detail.append("物品说明：").append(item.getRemark() != null ? item.getRemark() : "无").append("\n");

        new AlertDialog.Builder(getContext())
                .setTitle("物品详情")
                .setMessage(detail.toString())
                .setPositiveButton("关闭", null)
                .show();
    }
}