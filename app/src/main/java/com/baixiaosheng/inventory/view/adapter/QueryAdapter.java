package com.baixiaosheng.inventory.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.ItemWithName;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 查询页物品列表适配器：仅保留单选跳转详情、长按多选删除功能
 */
public class QueryAdapter extends RecyclerView.Adapter<QueryAdapter.ItemViewHolder> {
    // 数据
    private final Context context;
    private List<ItemWithName> itemList = new ArrayList<>(); // 统一使用ItemWithName
    // 多选模式
    private boolean isMultiSelectMode = false;
    private final List<String> selectedUuids = new ArrayList<>();
    // 回调（仅保留必要的）
    private OnItemClickListener itemClickListener;
    private OnMultiSelectChangeListener multiSelectChangeListener;

    // 日期格式化
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    public QueryAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_query, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ItemWithName itemWithName = itemList.get(position);

        // 使用ItemWithName中的Item部分
        if (itemWithName != null && itemWithName.item != null) {
            // 绑定基础数据
            holder.tvName.setText(itemWithName.item.getName());


            // 使用分类和位置名称（ItemWithName的额外字段）

            // 优化分类显示：未设置时展示“未设置/无”
            if (itemWithName.parentCategoryName != null && itemWithName.categoryName != null) {
                holder.tvCategory.setText(itemWithName.parentCategoryName + "/" + itemWithName.categoryName);
            } else {
                holder.tvCategory.setText("未设置");
            }

            if (itemWithName.locationName != null) {
                holder.tvLocation.setText(itemWithName.locationName);
            } else {
                holder.tvLocation.setText(itemWithName.item.getLocationId() == 0 ? "未设置" : String.valueOf(itemWithName.item.getLocationId()));
            }

            holder.tvQuantity.setText(String.valueOf(itemWithName.item.getCount()));

            // 过期时间
            if (itemWithName.item.getValidTime() != 0) {
                holder.tvExpire.setText(dateFormat.format(itemWithName.item.getValidTime()));
            } else {
                holder.tvExpire.setText("无");
            }

            // 物品说明
            if (itemWithName.item.getRemark() != null && !itemWithName.item.getRemark().isEmpty()) {
                holder.tvDesc.setVisibility(View.VISIBLE);
                holder.tvDesc.setText(itemWithName.item.getRemark());
            } else {
                holder.tvDesc.setVisibility(View.GONE);
            }

            // 多选模式处理
            holder.cbSelect.setOnCheckedChangeListener(null);
            holder.cbSelect.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
            holder.cbSelect.setChecked(selectedUuids.contains(itemWithName.item.getUuid()));
            holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateSelectionState(itemWithName.item.getUuid(), isChecked);
            });

            // 条目点击事件：仅跳转详情（多选模式下切换选中状态）
            holder.itemView.setOnClickListener(v -> {
                if (isMultiSelectMode) {
                    holder.cbSelect.setChecked(!holder.cbSelect.isChecked());
                } else {
                    if (itemClickListener != null) {
                        // 传递Item，因为ItemDetailActivity只需要Item对象
                        itemClickListener.onItemClick(itemWithName.item);
                    }
                }
            });

            // 条目长按事件：进入多选模式
            holder.itemView.setOnLongClickListener(v -> {
                if (!isMultiSelectMode) {
                    setMultiSelectMode(true);
                    holder.cbSelect.setChecked(true);
                }
                return true;
            });
        }
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

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    // 设置数据
    public void setItemList(List<ItemWithName> newList) {
        this.itemList = newList;
        if (isMultiSelectMode) {
            setMultiSelectMode(false);
        }
        notifyDataSetChanged();
    }

    // 切换多选模式
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

    // 获取选中的UUID列表
    public List<String> getSelectedUuids() {
        return new ArrayList<>(selectedUuids);
    }

    // 清空选中
    public void clearSelection() {
        selectedUuids.clear();
        notifyDataSetChanged();
        if (multiSelectChangeListener != null) {
            multiSelectChangeListener.onSelectCountChanged(0);
        }
    }

    // 设置回调
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnMultiSelectChangeListener(OnMultiSelectChangeListener listener) {
        this.multiSelectChangeListener = listener;
    }

    // 点击事件回调接口（仅跳转详情）
    public interface OnItemClickListener {
        void onItemClick(com.baixiaosheng.inventory.database.entity.Item item); // 使用全限定名
    }

    // 多选状态变化回调
    public interface OnMultiSelectChangeListener {
        void onSelectModeChanged(boolean isMultiSelect);
        void onSelectCountChanged(int count);
    }

    // ViewHolder
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvName, tvCategory, tvLocation, tvQuantity, tvExpire, tvDesc;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_select);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvCategory = itemView.findViewById(R.id.tv_item_category);
            tvLocation = itemView.findViewById(R.id.tv_item_location);
            tvQuantity = itemView.findViewById(R.id.tv_item_quantity);
            tvExpire = itemView.findViewById(R.id.tv_item_expire);
            tvDesc = itemView.findViewById(R.id.tv_item_desc);
        }
    }
}