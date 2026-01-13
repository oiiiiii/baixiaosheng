package com.baixiaosheng.inventory.view.adapter;

import android.content.Context;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Item;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 查询页物品列表适配器：支持单选/长按多选（修复计数错误 + 匹配布局ID）
 */
public class InventoryQueryAdapter extends RecyclerView.Adapter<InventoryQueryAdapter.ItemViewHolder> {
    // 数据
    private final Context context;
    private List<Item> itemList = new ArrayList<>();
    // 多选模式
    private boolean isMultiSelectMode = false;
    private final List<String> selectedUuids = new ArrayList<>();
    // 回调
    private OnItemClickListener itemClickListener;
    private OnMultiSelectChangeListener multiSelectChangeListener;
    // 新增：编辑点击监听
    private OnItemEditListener editListener;

    // 日期格式化
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    public InventoryQueryAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_inventory_query, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        // 绑定基础数据（完全匹配布局ID）
        holder.tvName.setText(item.getName());
        holder.tvUuid.setText(item.getUuid().substring(0, 8) + "..."); // 简化UUID显示
        holder.tvCategory.setText(item.getParentCategoryId() + "/" + item.getChildCategoryId());
        holder.tvLocation.setText(item.getLocationId() == 0 ? "未设置" : String.valueOf(item.getLocationId()));
        holder.tvQuantity.setText(String.valueOf(item.getCount())); // 数量：匹配tv_item_quantity
        // 过期时间
        if (item.getValidTime() != 0) {
            holder.tvExpire.setText(dateFormat.format(item.getValidTime()));
        } else {
            holder.tvExpire.setText("无");
        }
        // 物品说明
        if (item.getRemark() != null && !item.getRemark().isEmpty()) {
            holder.tvDesc.setVisibility(View.VISIBLE);
            holder.tvDesc.setText(item.getRemark());
        } else {
            holder.tvDesc.setVisibility(View.GONE);
        }

        // 多选模式处理：先移除复选框监听，避免绑定数据时触发计数
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
        holder.cbSelect.setChecked(selectedUuids.contains(item.getUuid()));
        // 重新设置复选框监听（核心：仅通过这里处理计数，避免重复）
        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateSelectionState(item.getUuid(), isChecked);
        });

        // 条目点击事件
        holder.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                // 多选模式：切换选中状态（直接操作复选框，由监听处理计数）
                holder.cbSelect.setChecked(!holder.cbSelect.isChecked());
            } else {
                // 普通模式：弹出操作菜单（查看/编辑/删除）
                showItemOperationMenu(item);
            }
        });

        // 条目长按事件
        holder.itemView.setOnLongClickListener(v -> {
            if (!isMultiSelectMode) {
                // 进入多选模式
                setMultiSelectMode(true);
                // 选中当前长按的item（直接操作复选框，避免重复触发）
                holder.cbSelect.setChecked(true);
            }
            return true;
        });
    }

    /**
     * 新增：物品操作菜单（查看/编辑/删除）
     */
    private void showItemOperationMenu(Item item) {
        new AlertDialog.Builder(context)
                .setTitle("操作选项")
                .setItems(new String[]{"查看详情", "编辑物品", "删除物品"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // 查看详情
                            if (itemClickListener != null) {
                                itemClickListener.onItemClick(item);
                            }
                            break;
                        case 1:
                            // 编辑物品
                            if (editListener != null) {
                                editListener.onItemEdit(item);
                            }
                            break;
                        case 2:
                            // 删除物品（复用原有删除逻辑，通过itemClickListener透传）
                            if (itemClickListener != null) {
                                itemClickListener.onItemClick(item);
                            }
                            break;
                    }
                })
                .create()
                .show();
    }

    /**
     * 统一更新选中状态（核心修复：防重复、单入口计数）
     * @param uuid 物品唯一标识
     * @param isChecked 是否选中
     */
    private void updateSelectionState(String uuid, boolean isChecked) {
        if (isChecked) {
            // 避免重复添加
            if (!selectedUuids.contains(uuid)) {
                selectedUuids.add(uuid);
            }
        } else {
            // 避免重复移除
            if (selectedUuids.contains(uuid)) {
                selectedUuids.remove(uuid);
            }
        }
        // 回调选中数量（仅一次，无重复）
        if (multiSelectChangeListener != null) {
            multiSelectChangeListener.onSelectCountChanged(selectedUuids.size());
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    // 设置数据
    public void setItemList(List<Item> newList) {
        this.itemList = newList;
        // 清空选中状态（切换数据时重置多选）
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
        // 回调模式变化
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
        // 回调清空后的计数
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

    // 新增：设置编辑监听
    public void setOnItemEditListener(OnItemEditListener listener) {
        this.editListener = listener;
    }

    // 点击事件回调接口
    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    // 多选状态变化回调
    public interface OnMultiSelectChangeListener {
        void onSelectModeChanged(boolean isMultiSelect);
        void onSelectCountChanged(int count);
    }

    // 新增：编辑监听接口
    public interface OnItemEditListener {
        void onItemEdit(Item item);
    }

    // ViewHolder（完全匹配布局ID，无tv_count）
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvName, tvUuid, tvCategory, tvLocation, tvQuantity, tvExpire, tvDesc;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            // 绑定布局中的所有控件ID（无tv_count，替换为tv_item_quantity）
            cbSelect = itemView.findViewById(R.id.cb_select);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvUuid = itemView.findViewById(R.id.tv_item_uuid);
            tvCategory = itemView.findViewById(R.id.tv_item_category);
            tvLocation = itemView.findViewById(R.id.tv_item_location);
            tvQuantity = itemView.findViewById(R.id.tv_item_quantity); // 数量：匹配布局ID
            tvExpire = itemView.findViewById(R.id.tv_item_expire);
            tvDesc = itemView.findViewById(R.id.tv_item_desc);
        }
    }
}