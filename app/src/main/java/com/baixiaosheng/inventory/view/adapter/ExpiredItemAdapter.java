package com.baixiaosheng.inventory.view.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Item;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpiredItemAdapter extends RecyclerView.Adapter<ExpiredItemAdapter.ItemViewHolder> {
    private List<Item> itemList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    // 构造方法
    public ExpiredItemAdapter(List<Item> itemList) {
        this.itemList = itemList;
    }

    // 更新列表数据
    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<Item> newList) {
        this.itemList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_valid_time, parent, false);
        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        // 物品名称
        holder.tvItemName.setText(item.getName());
        // 过期时间（空值处理）
        if (item.getValidTime() > 0) {
            String expireTime = dateFormat.format(new Date(item.getValidTime()));
            holder.tvExpireTime.setText("过期时间：" + expireTime);
        } else {
            holder.tvExpireTime.setText("过期时间：未设置");
        }
        // 物品位置（空值处理）
        holder.tvItemLocation.setText(item.getLocationId() == 0 ? "位置：未设置" : "位置：" + item.getLocationId());
        // 物品数量
        holder.tvItemCount.setText("数量：" +  item.getCount());
    }

    @Override
    public int getItemCount() {
        return itemList == null ? 0 : itemList.size();
    }

    // 列表项ViewHolder
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvExpireTime, tvItemLocation, tvItemCount;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvExpireTime = itemView.findViewById(R.id.tv_ValidTime);
            tvItemLocation = itemView.findViewById(R.id.tv_item_location);
            tvItemCount = itemView.findViewById(R.id.tv_item_count);
        }
    }
}