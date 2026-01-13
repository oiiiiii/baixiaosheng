package com.baixiaosheng.inventory.view.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.view.adapter.CategoryAdapter;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.viewmodel.CategoryManageViewModel;

import java.util.List;

/**
 * 分类管理页面
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manage);
        initView();
        initViewModel();
        initListener();
    }

    private void initView() {
        etCategoryName = findViewById(R.id.et_category_name);
        spParentCategory = findViewById(R.id.sp_parent_category);
        btnAddCategory = findViewById(R.id.btn_add_category);
        rvCategoryList = findViewById(R.id.rv_category_list);
        rvCategoryList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initViewModel() {
        mViewModel = new ViewModelProvider(this).get(CategoryManageViewModel.class);

        // 观察分类列表
        mViewModel.getCategoryList().observe(this, categories -> {
            if (mAdapter == null) {
                mAdapter = new CategoryAdapter(categories, this::onEditClick, this::onDeleteClick);
                rvCategoryList.setAdapter(mAdapter);
            } else {
                mAdapter.updateData(categories);
            }
        });

        // 观察父分类列表（用于Spinner）
        mViewModel.getParentCategories().observe(this, parentCategories -> {
            mParentCategoryList = parentCategories;
            // 构建Spinner适配器
            String[] parentNames = new String[parentCategories.size() + 1];
            parentNames[0] = "无父分类";
            for (int i = 0; i < parentCategories.size(); i++) {
                parentNames[i + 1] = parentCategories.get(i).getName();
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, parentNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spParentCategory.setAdapter(adapter);
        });

        // 观察操作结果
        mViewModel.getOperationSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, mEditingCategoryId == 0 ? "添加成功" : "更新成功", Toast.LENGTH_SHORT).show();
                resetEditState();
            }
        });

        // 观察错误信息
        mViewModel.getErrorMsg().observe(this, errorMsg -> {
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        });
    }

    private void initListener() {
        // 添加/更新分类按钮点击事件
        btnAddCategory.setOnClickListener(v -> {
            String categoryName = etCategoryName.getText().toString().trim();
            if (categoryName.isEmpty()) {
                Toast.makeText(this, "请输入分类名称", Toast.LENGTH_SHORT).show();
                return;
            }
            // 获取父分类ID
            long parentId = 0;
            int selectedPos = spParentCategory.getSelectedItemPosition();
            if (selectedPos > 0 && mParentCategoryList != null && mParentCategoryList.size() >= selectedPos - 1) {
                parentId = mParentCategoryList.get(selectedPos - 1).getId();
            }
            Category category = new Category();
            category.setName(categoryName);
            category.setParentId(parentId);
            if (mEditingCategoryId == 0) {
                // 新增
                mViewModel.addCategory(category);
            } else {
                // 更新
                category.setId(mEditingCategoryId);
                mViewModel.updateCategory(category);
            }
        });
    }

    /**
     * 编辑分类点击事件
     */
    private void onEditClick(Category category) {
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
     * 删除分类点击事件
     */
    private void onDeleteClick(Category category) {
        mViewModel.deleteCategory(category.getId());
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