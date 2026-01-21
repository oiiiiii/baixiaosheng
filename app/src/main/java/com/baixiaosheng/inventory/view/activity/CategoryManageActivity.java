package com.baixiaosheng.inventory.view.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.view.adapter.CategoryAdapter;
import com.baixiaosheng.inventory.viewmodel.CategoryManageViewModel;

import java.util.ArrayList;
import java.util.List;

public class CategoryManageActivity extends AppCompatActivity {

    private CategoryManageViewModel categoryViewModel;
    private CategoryAdapter categoryAdapter;
    private RecyclerView rvCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manage);

        // 初始化ViewModel
        categoryViewModel = new ViewModelProvider(this).get(CategoryManageViewModel.class);

        // 初始化视图
        initView();

        // 观察分类数据（仅观察父分类）
        observeCategoryData();

        // 加载所有父分类
        categoryViewModel.loadParentCategories();

        // 在CategoryManageActivity的onCreate方法中添加
        Button btnAddNewParent = findViewById(R.id.btn_add_new_parent_category);
        btnAddNewParent.setOnClickListener(v -> {
            // 调用原有添加父分类的弹窗逻辑
            showAddCategoryDialog(null, 0);
        });
    }

    private void initView() {
        // 标题栏
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.category_manage_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 分类列表（仅显示父分类 + 底部添加按钮）
        rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryAdapter(this, categoryViewModel);
        rvCategories.setAdapter(categoryAdapter);
    }

    private void observeCategoryData() {
        // 仅观察父分类（parentId=0）
        categoryViewModel.getParentCategories().observe(this, parentCategories -> {
            if (parentCategories == null) {
                parentCategories = new ArrayList<>();
            }
            categoryAdapter.setParentCategories(parentCategories);
        });

        categoryViewModel.getOperationResult().observe(this, result -> {
            if (result) {
                Toast.makeText(this, R.string.operation_success, Toast.LENGTH_SHORT).show();
                categoryViewModel.loadParentCategories();
            } else {
                Toast.makeText(this, R.string.operation_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 显示添加/编辑分类弹窗
     * @param category 编辑时传入分类对象（null=新增）
     * @param parentId 新增子分类时传入父分类ID（0=新增父分类）
     */
    public void showAddCategoryDialog(Category category, long parentId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(category == null
                ? (parentId == 0 ? R.string.add_parent_category : R.string.add_child_category)
                : R.string.edit_category);

        // 加载弹窗布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category_edit, null);
        EditText etCategoryName = dialogView.findViewById(R.id.et_category_name);

        // 编辑时填充分类名称
        if (category != null) {
            etCategoryName.setText(category.getCategoryName());
        }

        builder.setView(dialogView);

        // 确定按钮（修改为异步校验名称）
        builder.setPositiveButton(R.string.confirm, null);
        // 取消按钮（返回键）
        builder.setNegativeButton(R.string.cancel, (dialogInterface, which) -> {
            dialogInterface.dismiss(); // 点击取消关闭弹窗
        });

        AlertDialog dialog = builder.create();
        // 允许通过返回键关闭弹窗（默认开启，显式设置确保生效）
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        dialog.setOnShowListener(d -> {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = etCategoryName.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(CategoryManageActivity.this, R.string.category_name_empty, Toast.LENGTH_SHORT).show();
                    return;
                }

                // 校验名称是否重复：排除自身ID（编辑时），新增时传-1
                long excludeId = category == null ? -1 : category.getId();
                categoryViewModel.checkCategoryNameDuplicate(name, parentId, excludeId).observe(CategoryManageActivity.this, isDuplicate -> {
                    if (isDuplicate) {
                        // 名称重复提示
                        String tip = parentId == 0
                                ? getString(R.string.parent_category_name_duplicate)
                                : getString(R.string.child_category_name_duplicate);
                        Toast.makeText(CategoryManageActivity.this, tip, Toast.LENGTH_SHORT).show();
                    } else {
                        // 名称可用，执行添加/编辑操作
                        if (category == null) {
                            // 新增分类
                            Category newCategory = new Category();
                            newCategory.setCategoryName(name);
                            newCategory.setParentCategoryId(parentId);
                            categoryViewModel.addCategory(newCategory);
                        } else {
                            // 编辑分类
                            category.setCategoryName(name);
                            categoryViewModel.updateCategory(category);
                        }
                        dialog.dismiss();
                    }
                });
            });
        });

        dialog.show();
    }


    /**
     * 显示删除确认弹窗
     * @param category 要删除的父分类
     */
    public void showDeleteConfirmDialog(Category category) {
        // 异步查询子分类数量和关联物品数量
        categoryViewModel.checkCategoryDeleteTip(category.getId()).observe(this, deleteTip -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.delete_category)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        // 执行父分类删除（内部级联处理子分类和物品关联）
                        categoryViewModel.deleteCategory(category);
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

            // 设置对应提示语
            builder.setMessage(deleteTip);
            builder.show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}