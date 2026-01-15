package com.baixiaosheng.inventory.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Category;

import java.util.List;

/**
 * 分类列表适配器
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> mCategoryList;
    private final OnEditClickListener mOnEditClickListener;
    private final OnDeleteClickListener mOnDeleteClickListener;

    // 编辑点击回调
    public interface OnEditClickListener {
        void onEditClick(Category category);
    }

    // 删除点击回调
    public interface OnDeleteClickListener {
        void onDeleteClick(Category category);
    }

    public CategoryAdapter(List<Category> categoryList, OnEditClickListener onEditClickListener, OnDeleteClickListener onDeleteClickListener) {
        this.mCategoryList = categoryList;
        this.mOnEditClickListener = onEditClickListener;
        this.mOnDeleteClickListener = onDeleteClickListener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = mCategoryList.get(position);
        holder.tvCategoryName.setText(category.getCategoryName());
        // 显示父分类名称（无则显示"无"）
        holder.tvParentCategory.setText(category.getParentCategoryId() == 0 ? "无" : getParentName(category.getParentCategoryId()));
        // 编辑按钮点击
        holder.btnEditCategory.setOnClickListener(v -> mOnEditClickListener.onEditClick(category));
        // 删除按钮点击
        holder.btnDeleteCategory.setOnClickListener(v -> mOnDeleteClickListener.onDeleteClick(category));
    }

    @Override
    public int getItemCount() {
        return mCategoryList.size();
    }

    /**
     * 更新数据
     */
    public void updateData(List<Category> categoryList) {
        this.mCategoryList = categoryList;
        notifyDataSetChanged();
    }

    /**
     * 根据父分类ID获取父分类名称
     */
    private String getParentName(long parentId) {
        for (Category category : mCategoryList) {
            if (category.getId() == parentId) {
                return category.getCategoryName();
            }
        }
        return "未知";
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        TextView tvParentCategory;
        Button btnEditCategory;
        Button btnDeleteCategory;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvParentCategory = itemView.findViewById(R.id.tv_parent_category);
            btnEditCategory = itemView.findViewById(R.id.btn_edit_category);
            btnDeleteCategory = itemView.findViewById(R.id.btn_delete_category);
        }
    }
}