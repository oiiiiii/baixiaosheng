package com.baixiaosheng.inventory.view.fragment;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.baixiaosheng.inventory.utils.DateUtils;
import com.baixiaosheng.inventory.view.adapter.QueryAdapter;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.model.FilterCondition;
import com.baixiaosheng.inventory.viewmodel.QueryViewModel;
import com.baixiaosheng.inventory.view.activity.MainActivity;
import com.baixiaosheng.inventory.view.activity.ItemDetailActivity;

import java.util.Calendar;
import java.util.List;

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
    private QueryAdapter adapter;
    // ViewModel
    private QueryViewModel queryViewModel;
    // 筛选条件
    private final FilterCondition filterCondition = new FilterCondition();


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
        adapter = new QueryAdapter(getContext());
        rvInventoryList.setAdapter(adapter);

        // 设置适配器回调：点击物品直接跳转ItemDetailActivity
        adapter.setOnItemClickListener(this::showItemOperationDialog);
        // 新增：绑定编辑监听
        adapter.setOnItemEditListener(this::jumpToEditFragment);

        adapter.setOnMultiSelectChangeListener(new QueryAdapter.OnMultiSelectChangeListener() {
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

    // 新增：跳转到编辑模式的InputFragment
    private void jumpToEditFragment(Item item) {
        // 检查宿主Activity是否为MainActivity（避免类型转换异常）
        if (getActivity() instanceof MainActivity) {
            InputFragment editFragment = new InputFragment();
            Bundle args = new Bundle();
            args.putSerializable("edit_item", item); // Item已实现Serializable接口
            editFragment.setArguments(args);
            // 切换到编辑模式的InputFragment
            ((MainActivity) getActivity()).switchFragment(editFragment);
        } else {
            Toast.makeText(getContext(), "跳转失败：当前上下文非MainActivity", Toast.LENGTH_SHORT).show();
        }
    }

    // 新增：供外部调用的刷新列表方法
    public void refreshItemList() {
        queryViewModel.queryItems(filterCondition);
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


        // 过期开始日期选择（修改后）
        tvExpireStart.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(getContext(), (view1, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);
                filterCondition.setExpireStart(selected.getTime());
                // 替换为DateUtils工具类
                tvExpireStart.setText(DateUtils.formatDateToYmd(selected.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

// 过期结束日期选择（修改后）
        tvExpireEnd.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(getContext(), (view1, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);
                filterCondition.setExpireEnd(selected.getTime());
                // 替换为DateUtils工具类
                tvExpireEnd.setText(DateUtils.formatDateToYmd(selected.getTime()));
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

    // 物品操作：直接跳转ItemDetailActivity（移除原弹窗逻辑）
    private void showItemOperationDialog(Item item) {
        // 先校验item非空
        if (item == null) {
            Toast.makeText(getContext(), "物品数据异常", Toast.LENGTH_SHORT).show();
            return;
        }
        // 跳转到ItemDetailActivity
        Intent intent = new Intent(getContext(), ItemDetailActivity.class);
        // 传递物品数据（Item需实现Serializable接口）
        intent.putExtra("item_id", item.getId());
        startActivity(intent);
    }

    // 移除原有的showItemDetailDialog和replaceLine方法（已无需使用）
}