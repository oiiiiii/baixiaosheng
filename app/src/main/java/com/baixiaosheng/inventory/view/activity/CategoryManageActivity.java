package com.baixiaosheng.inventory.view.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.view.adapter.CategoryAdapter;
import com.baixiaosheng.inventory.viewmodel.CategoryManageViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类管理页面（弹窗交互版）
 */
public class CategoryManageActivity extends AppCompatActivity {

    private EditText etCategoryName;
    private Spinner spParentCategory;
    private Button btnAddCategory;
    private RecyclerView rvCategoryList;

    private CategoryManageViewModel mViewModel;
    private CategoryAdapter mAdapter;
    private List<Category> mParentCategoryList;
    private long mEditingCategoryId = 0; // 编辑中的分类ID，0表示新增
    private long mPendingDeleteCategoryId = 0; // 待删除的分类ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manage);
        initView();
        initViewModel();
        initListener();
    }

    /**
     * 初始化视图控件
     */
    private void initView() {
        etCategoryName = findViewById(R.id.et_category_name);
        spParentCategory = findViewById(R.id.sp_parent_category);
        btnAddCategory = findViewById(R.id.btn_add_category);
        rvCategoryList = findViewById(R.id.rv_category_list);
        // 设置RecyclerView布局
        rvCategoryList.setLayoutManager(new LinearLayoutManager(this));
        // 初始化适配器：传入空列表 + 编辑/删除回调
        mAdapter = new CategoryAdapter(new ArrayList<>(), this::handleEditClick, this::handleDeleteClick);
        rvCategoryList.setAdapter(mAdapter);
    }

    /**
     * 初始化ViewModel及数据观察
     */
    private void initViewModel() {
        mViewModel = new ViewModelProvider(this).get(CategoryManageViewModel.class);

        // 观察分类列表
        mViewModel.getCategoryList().observe(this, categories -> {
            // 使用Adapter的updateData方法更新数据
            mAdapter.updateData(categories);
        });


        // 重构：观察父分类列表（核心修改）
        mViewModel.getParentCategories(0).observe(this, parentCategories -> {
            mParentCategoryList = parentCategories;
            // 每次父分类数据变化时，重新构建Spinner适配器并设置
            updateParentCategorySpinner();
        });

        // 观察操作错误信息（原有逻辑不变）
        mViewModel.getErrorMsg().observe(this, errorMsg -> {
            showAlertDialog("错误", errorMsg, false);
        });

        // 新增：观察操作成功状态，确保UI提示和状态重置（可选，优化体验）
        mViewModel.getOperationSuccess().observe(this, isSuccess -> {
            if (isSuccess) {
                // 操作成功后重置编辑状态（原逻辑在btnAdd点击里，移到这里更合理）
                resetEditState();
                // 操作成功提示（根据编辑状态判断提示语）
                String tip = mEditingCategoryId == 0 ? "分类添加成功" :
                        (mPendingDeleteCategoryId != 0 ? "分类删除成功" : "分类修改成功");
                Toast toast = Toast.makeText(this, tip, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, dp2px(this, 100));
                toast.show();
            }
        });
    }

    // 新增：独立的Spinner更新方法（复用性更高）
    private void updateParentCategorySpinner() {
        if (mParentCategoryList == null) {
            mParentCategoryList = new ArrayList<>();
        }
        // 构建Spinner选项数组
        String[] parentNames = new String[mParentCategoryList.size() + 1];
        parentNames[0] = "无父分类";
        for (int i = 0; i < mParentCategoryList.size(); i++) {
            parentNames[i + 1] = mParentCategoryList.get(i).getName();
        }
        // 创建新的适配器（或更新原有适配器的数据源）
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, parentNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spParentCategory.setAdapter(adapter);
    }

    /**
     * 初始化事件监听
     */
    private void initListener() {
        // 添加/更新分类按钮点击事件
        btnAddCategory.setOnClickListener(v -> {
            String categoryName = etCategoryName.getText().toString().trim();
            if (categoryName.isEmpty()) {
                showAlertDialog("提示", "请输入分类名称", false);
                return;
            }

            // 获取父分类ID
            long parentId = 0;
            int selectedPos = spParentCategory.getSelectedItemPosition();
            if (selectedPos > 0 && mParentCategoryList != null
                    && mParentCategoryList.size() >= selectedPos - 1) {
                parentId = mParentCategoryList.get(selectedPos - 1).getId();
            }

            // 构建分类对象
            Category category = new Category();
            category.setName(categoryName);
            category.setParentId(parentId);

            // ========== 新增核心逻辑 ==========
            if (mEditingCategoryId == 0) {
                // 新增分类：调用ViewModel的添加方法
                mViewModel.addCategory(category);
            } else {
                // 编辑分类：设置ID并调用更新方法
                category.setId(mEditingCategoryId);
                mViewModel.updateCategory(category);
            }

        });
    }

    /**
     * 处理编辑点击（独立方法，定义在类中、其他方法外）
     */
    private void handleEditClick(Category category) {
        mEditingCategoryId = category.getId();
        etCategoryName.setText(category.getName());
        // 设置父分类选择
        if (category.getParentId() == 0) {
            spParentCategory.setSelection(0);
        } else {
            for (int i = 0; i < mParentCategoryList.size(); i++) {
                if (mParentCategoryList.get(i).getId() == category.getParentId()) {
                    spParentCategory.setSelection(i + 1);
                    break;
                }
            }
        }
        btnAddCategory.setText("更新");
    }

    /**
     * 处理删除点击（独立方法，定义在类中、其他方法外）
     */
    private void handleDeleteClick(Category category) {
        mPendingDeleteCategoryId = category.getId();
        // 检查该分类下是否有物品
        mViewModel.checkItemCountByCategoryId(category.getId()).observe(CategoryManageActivity.this, count -> {
            if (count > 0) {
                showAlertDialog("提示",
                        "该分类下有" + count + "个物品，删除后将影响物品分类显示，是否确认删除？",
                        true);
            } else {
                showAlertDialog("提示", "是否确认删除该分类？", true);
            }
        });
    }

    // 补充dp转px方法（放在CategoryManageActivity类中，与其他方法同级）
    private int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 通用弹窗提示
     * @param title 弹窗标题
     * @param message 弹窗内容
     * @param isDelete  是否为删除确认弹窗
     */
    private void showAlertDialog(String title, String message, boolean isDelete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false);

        // 确定按钮
        builder.setPositiveButton("确定", (dialog, which) -> {
            if (isDelete) {
                // 执行删除操作
                mViewModel.deleteCategory(mPendingDeleteCategoryId);
                mPendingDeleteCategoryId = 0;
            }
            dialog.dismiss();
        });

        // 取消按钮（仅删除弹窗显示）
        if (isDelete) {
            builder.setNegativeButton("取消", (dialog, which) -> {
                mPendingDeleteCategoryId = 0;
                dialog.dismiss();
            });
        }

        builder.show();
    }

    /**
     * 重置编辑状态
     */
    private void resetEditState() {
        etCategoryName.setText("");
        spParentCategory.setSelection(0);
        mEditingCategoryId = 0;
        btnAddCategory.setText("添加");
    }
}