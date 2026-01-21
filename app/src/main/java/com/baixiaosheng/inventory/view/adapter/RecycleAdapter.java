package com.baixiaosheng.inventory.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.ItemWithName;
import java.util.List;

/**
 * 回收站物品列表适配器（复用QueryAdapter字段获取逻辑）
 */
public class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.RecycleViewHolder> {
    private final Context context;
    private List<ItemWithName> itemWithNameList;

    public RecycleAdapter(Context context) {
        this.context = context;
    }

    /**
     * 更新列表数据
     */
    public void setItemWithNameList(List<ItemWithName> itemWithNameList) {
        this.itemWithNameList = itemWithNameList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecycleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_recycle, parent, false);
        return new RecycleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecycleViewHolder holder, int position) {
        ItemWithName itemWithName = itemWithNameList.get(position);
        if (itemWithName == null || itemWithName.item == null) return;

        // 1. 物品名称（复用QueryAdapter逻辑）
        holder.tvItemName.setText(itemWithName.item.getName() == null ? "未知名称" : itemWithName.item.getName());

        // 2. 物品数量（复用QueryAdapter逻辑）
        holder.tvQuantity.setText("数量：" + itemWithName.item.getCount());

        // 3. 分类信息（完全复用QueryAdapter的分类展示规则）
        String categoryDisplay;
        if (itemWithName.item.getParentCategoryId() == 0) {
            categoryDisplay = "未设置";
        } else {
            String parentCatName = itemWithName.parentCategoryName;
            String displayParent = (parentCatName != null && !parentCatName.isEmpty())
                    ? parentCatName
                    : String.valueOf(itemWithName.item.getParentCategoryId());

            if (itemWithName.item.getChildCategoryId() == 0) {
                categoryDisplay = displayParent;
            } else {
                String childCatName = itemWithName.categoryName;
                String displayChild = (childCatName != null && !childCatName.isEmpty())
                        ? childCatName
                        : String.valueOf(itemWithName.item.getChildCategoryId());
                categoryDisplay = displayParent + "/" + displayChild;
            }
        }
        holder.tvCategory.setText("分类：" + categoryDisplay);

        // 4. 位置信息（复用QueryAdapter逻辑）
        String locationDisplay;
        if (itemWithName.locationName != null) {
            locationDisplay = itemWithName.locationName;
        } else {
            locationDisplay = itemWithName.item.getLocationId() == 0 ? "未设置" : String.valueOf(itemWithName.item.getLocationId());
        }
        holder.tvLocation.setText("位置：" + locationDisplay);
    }

    @Override
    public int getItemCount() {
        return itemWithNameList == null ? 0 : itemWithNameList.size();
    }

    /**
     * 视图持有者
     */
    static class RecycleViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName;
        TextView tvCategory;
        TextView tvLocation;
        TextView tvQuantity;

        public RecycleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
        }
    }
}