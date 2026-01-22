package com.baixiaosheng.inventory.view.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.ItemWithName;
import java.util.ArrayList;
import java.util.List;

/**
 * 回收站物品列表适配器（复用QueryAdapter字段获取逻辑，添加多选+单击弹窗功能）
 */
public class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.RecycleViewHolder> {
    private static final String TAG = "RecycleAdapter";
    private final Context context;
    private List<ItemWithName> itemWithNameList;

    // 多选模式相关变量
    private boolean isMultiSelectMode = false;
    private final List<String> selectedUuids = new ArrayList<>();
    private OnMultiSelectChangeListener multiSelectChangeListener;
    // 单击事件回调
    private OnItemClickListener itemClickListener;

    public RecycleAdapter(Context context) {
        this.context = context;
    }

    /**
     * 更新列表数据
     */
    public void setItemWithNameList(List<ItemWithName> itemWithNameList) {
        this.itemWithNameList = itemWithNameList;
        if (isMultiSelectMode) {
            setMultiSelectMode(false);
        }
        notifyDataSetChanged();
    }

    /**
     * 设置条目单击事件监听
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
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
        Log.d(TAG, "绑定位置：" + position + "，数据总数：" + getItemCount());

        if (itemWithNameList == null || itemWithNameList.isEmpty()) {
            return;
        }

        ItemWithName itemWithName = itemWithNameList.get(position);
        if (itemWithName == null || itemWithName.item == null) {
            Log.e(TAG, "位置 " + position + " 的数据为null");
            return;
        }

        Log.d(TAG, "物品名称：" + itemWithName.item.getName());

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

        // 多选模式处理
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
        holder.cbSelect.setChecked(selectedUuids.contains(itemWithName.item.getUuid()));
        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateSelectionState(itemWithName.item.getUuid(), isChecked);
        });

        // 条目点击事件：多选模式下切换选中状态，非多选模式下触发单击弹窗
        holder.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                holder.cbSelect.setChecked(!holder.cbSelect.isChecked());
            } else {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(itemWithName);
                }
            }
        });

        // 条目长按事件：进入多选模式（参考QueryAdapter逻辑）
        holder.itemView.setOnLongClickListener(v -> {
            if (!isMultiSelectMode) {
                setMultiSelectMode(true);
                holder.cbSelect.setChecked(true);
            }
            return true;
        });
    }

    /**
     * 统一更新选中状态
     * @param uuid 物品唯一标识
     * @param isChecked 是否选中
     */
    private void updateSelectionState(String uuid, boolean isChecked) {
        if (isChecked) {
            if (!selectedUuids.contains(uuid)) {
                selectedUuids.add(uuid);
            }
        } else {
            if (selectedUuids.contains(uuid)) {
                selectedUuids.remove(uuid);
            }
        }
        if (multiSelectChangeListener != null) {
            multiSelectChangeListener.onSelectCountChanged(selectedUuids.size());
        }
    }

    /**
     * 切换多选模式
     */
    public void setMultiSelectMode(boolean enable) {
        this.isMultiSelectMode = enable;
        if (!enable) {
            selectedUuids.clear();
        }
        if (multiSelectChangeListener != null) {
            multiSelectChangeListener.onSelectModeChanged(enable);
            multiSelectChangeListener.onSelectCountChanged(selectedUuids.size());
        }
        notifyDataSetChanged();
    }

    /**
     * 退出多选模式
     */
    public void exitMultiSelectMode() {
        setMultiSelectMode(false);
    }

    /**
     * 获取选中的物品列表
     */
    public List<ItemWithName> getSelectedItems() {
        List<ItemWithName> selectedItems = new ArrayList<>();
        if (itemWithNameList == null || selectedUuids.isEmpty()) {
            return selectedItems;
        }
        for (ItemWithName itemWithName : itemWithNameList) {
            if (selectedUuids.contains(itemWithName.item.getUuid())) {
                selectedItems.add(itemWithName);
            }
        }
        return selectedItems;
    }

    /**
     * 获取选中的UUID列表
     */
    public List<String> getSelectedUuids() {
        return new ArrayList<>(selectedUuids);
    }

    /**
     * 清空选中
     */
    public void clearSelection() {
        selectedUuids.clear();
        notifyDataSetChanged();
        if (multiSelectChangeListener != null) {
            multiSelectChangeListener.onSelectCountChanged(0);
        }
    }

    /**
     * 获取当前是否在多选模式
     */
    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    /**
     * 设置多选状态变化监听器
     */
    public void setOnMultiSelectChangeListener(OnMultiSelectChangeListener listener) {
        this.multiSelectChangeListener = listener;
    }

    @Override
    public int getItemCount() {
        return itemWithNameList == null ? 0 : itemWithNameList.size();
    }

    /**
     * 多选状态变化回调接口
     */
    public interface OnMultiSelectChangeListener {
        void onSelectModeChanged(boolean isMultiSelect);
        void onSelectCountChanged(int count);
    }

    /**
     * 条目单击事件回调接口
     */
    public interface OnItemClickListener {
        void onItemClick(ItemWithName itemWithName);
    }

    /**
     * 视图持有者
     */
    static class RecycleViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvItemName;
        TextView tvCategory;
        TextView tvLocation;
        TextView tvQuantity;

        public RecycleViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_select);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
        }
    }
}