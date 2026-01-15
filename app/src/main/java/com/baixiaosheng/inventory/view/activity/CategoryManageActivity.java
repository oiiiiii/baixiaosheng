package com.baixiaosheng.inventory.view.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
 * 分类管理页面（简化删除逻辑版）
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

    private String btnDefaultText;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manage);
        initView();
        initBtnDefaultStyle();
        initViewModel();
        initListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 初始化视图控件
     */
    private void initView() {
        etCategoryName = findViewById(R.id.et_category_name);
        spParentCategory = findViewById(R.id.sp_parent_category);
        btnAddCategory = findViewById(R.id.btn_add_category);
        rvCategoryList = findViewById(R.id.rv_category_list);
        rvCategoryList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CategoryAdapter(new ArrayList<>(), this::handleEditClick, this::handleDeleteClick);
        rvCategoryList.setAdapter(mAdapter);
    }

    /**
     * 初始化按钮默认样式（仅保留文字备份）
     */
    private void initBtnDefaultStyle() {
        btnDefaultText = btnAddCategory.getText().toString();
    }

    /**
     * 初始化ViewModel及数据观察
     */
    private void initViewModel() {
        mViewModel = new ViewModelProvider(this).get(CategoryManageViewModel.class);

        // 观察分类列表
        mViewModel.getCategoryList().observe(this, categories -> {
            mAdapter.updateData(categories);
        });

        // 观察父分类列表
        mViewModel.getParentCategories().observe(this, parentCategories -> {
            mParentCategoryList = parentCategories;
            updateParentCategorySpinner();
        });

        // 观察操作错误信息
        mViewModel.getErrorMsg().observe(this, errorMsg -> {
            showAlertDialog("错误", errorMsg, false);
        });

        // 观察操作成功状态
        mViewModel.getOperationSuccess().observe(this, isSuccess -> {
            if (isSuccess) {
                resetEditState();
                btnAddCategory.setText("OK");
                mHandler.removeCallbacks(mRestoreBtnRunnable);
                mHandler.postDelayed(mRestoreBtnRunnable, 1000);
            }
        });
    }

    /**
     * 按钮文字恢复任务
     */
    private Runnable mRestoreBtnRunnable = new Runnable() {
        @Override
        public void run() {
            btnAddCategory.setText(btnDefaultText);
        }
    };

    // 独立的Spinner更新方法
    private void updateParentCategorySpinner() {
        if (mParentCategoryList == null) {
            mParentCategoryList = new ArrayList<>();
        }
        String[] parentNames = new String[mParentCategoryList.size() + 1];
        parentNames[0] = "无父分类";
        for (int i = 0; i < mParentCategoryList.size(); i++) {
            parentNames[i + 1] = mParentCategoryList.get(i).getCategoryName();
        }
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
            category.setCategoryName(categoryName);
            category.setParentCategoryId(parentId);

            if (mEditingCategoryId == 0) {
                mViewModel.addCategory(category);
            } else {
                category.setId(mEditingCategoryId);
                mViewModel.updateCategory(category);
            }
        });
    }

    /**
     * 处理编辑点击
     */
    private void handleEditClick(Category category) {
        mEditingCategoryId = category.getId();
        etCategoryName.setText(category.getCategoryName());
        if (category.getParentCategoryId() == 0) {
            spParentCategory.setSelection(0);
        } else {
            for (int i = 0; i < mParentCategoryList.size(); i++) {
                if (mParentCategoryList.get(i).getId() == category.getParentCategoryId()) {
                    spParentCategory.setSelection(i + 1);
                    break;
                }
            }
        }
        btnAddCategory.setText("更新");
    }

    /**
     * 处理删除点击（简化：仅弹确认框）
     */
    private void handleDeleteClick(Category category) {
        new AlertDialog.Builder(this)
                .setTitle("删除确认")
                .setMessage(String.format("是否确认删除分类「%s」？", category.getCategoryName()))
                .setPositiveButton("删除", (dialog, which) -> {
                    mViewModel.deleteCategory(category.getId());
                    dialog.dismiss();
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    /**
     * 通用弹窗提示
     */
    private void showAlertDialog(String title, String message, boolean isDelete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("确定", (dialog, which) -> dialog.dismiss());
        if (isDelete) {
            builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        }
        builder.show();
    }

    private void resetEditState() {
        etCategoryName.setText("");
        spParentCategory.setSelection(0);
        mEditingCategoryId = 0;
        btnAddCategory.setText("添加");
    }
}