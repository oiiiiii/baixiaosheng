package com.baixiaosheng.inventory.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.view.activity.CategoryManageActivity;
import com.baixiaosheng.inventory.viewmodel.CategoryManageViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ParentCategoryViewHolder> {

    private Context context;
    private List<Category> parentCategories; // 仅存储父分类（parentId=0）
    private CategoryManageViewModel viewModel;
    private LifecycleOwner lifecycleOwner;

    public CategoryAdapter(Context context, CategoryManageViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
        this.lifecycleOwner = (LifecycleOwner) context;
    }

    public void setParentCategories(List<Category> parentCategories) {
        this.parentCategories = parentCategories;
        notifyDataSetChanged();
    }

    // 仅返回父分类数量（移除添加按钮Item）
    @Override
    public int getItemCount() {
        return parentCategories == null ? 0 : parentCategories.size();
    }

    @NonNull
    @Override
    public ParentCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_category, parent, false);
        return new ParentCategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParentCategoryViewHolder holder, int position) {
        Category parentCategory = parentCategories.get(position);
        if (parentCategory == null) return;

        // 显示父分类名称
        holder.tvName.setText(String.format("分类：%s", parentCategory.getCategoryName()));

        // 加载子分类Chip
        holder.cgChildCategories.removeAllViews();
        viewModel.getChildCategories(parentCategory.getId()).observe(lifecycleOwner, childCategories -> {
            holder.cgChildCategories.removeAllViews();
            for (Category child : childCategories) {
                Chip chip = new Chip(context);
                chip.setText(child.getCategoryName());
                chip.setChipBackgroundColorResource(R.color.chip_bg);
                chip.setTextColor(context.getResources().getColor(R.color.white));

                // 子分类Chip点击（编辑）
                chip.setOnClickListener(v -> {
                    ((CategoryManageActivity) context).showAddCategoryDialog(child, parentCategory.getId());
                });

                // 子分类Chip长按（删除）
                chip.setOnLongClickListener(v -> {
                    // 先查询子分类关联的物品数量
                    viewModel.checkCategoryHasRelatedItems(child.getId()).observe(lifecycleOwner, hasItems -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                .setTitle(R.string.delete_category)
                                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                                    // 执行子分类删除（内部已处理清空物品关联）
                                    viewModel.deleteCategory(child);
                                    viewModel.loadAllCategories();
                                })
                                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

                        // 根据是否有物品调整提示语
                        if (hasItems) {
                            builder.setMessage(context.getString(R.string.delete_child_category_with_items_tip));
                        } else {
                            builder.setMessage(String.format(context.getString(R.string.delete_category_confirm), child.getCategoryName()));
                        }
                        builder.show();
                    });
                    return true;
                });

                holder.cgChildCategories.addView(chip);
            }
        });

        // 父分类Item单击事件：编辑当前父分类名称
        holder.itemView.setOnClickListener(v -> {
            ((CategoryManageActivity) context).showAddCategoryDialog(parentCategory, 0);
        });

        // 父分类Item长按事件：删除当前父分类（弹窗确认）
        holder.itemView.setOnLongClickListener(v -> {
            ((CategoryManageActivity) context).showDeleteConfirmDialog(parentCategory);
            return true;
        });

        // ChipGroup末尾的添加子分类按钮点击事件
        holder.btnAddChildInChip.setOnClickListener(v -> {
            ((CategoryManageActivity) context).showAddCategoryDialog(null, parentCategory.getId());
        });
    }

    // 仅保留父分类ViewHolder（移除AddParentViewHolder）
    static class ParentCategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ChipGroup cgChildCategories;
        ImageButton btnAddChildInChip;

        public ParentCategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            cgChildCategories = itemView.findViewById(R.id.chip_group_subcategory);
            btnAddChildInChip = itemView.findViewById(R.id.btn_add_child_in_chip);
        }
    }
}