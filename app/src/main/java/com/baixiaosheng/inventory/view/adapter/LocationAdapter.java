package com.baixiaosheng.inventory.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Location;

import java.util.List;

/**
 * 位置列表适配器
 */
public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    private List<Location> mLocationList;
    private final OnItemClickListener mOnItemClickListener;
    private final OnItemLongClickListener mOnItemLongClickListener;

    // 点击回调（编辑）
    public interface OnItemClickListener {
        void onItemClick(Location location);
    }

    // 长按回调（删除）
    public interface OnItemLongClickListener {
        void onItemLongClick(Location location);
    }

    public LocationAdapter(List<Location> locationList,
                           OnItemClickListener onItemClickListener,
                           OnItemLongClickListener onItemLongClickListener) {
        this.mLocationList = locationList;
        this.mOnItemClickListener = onItemClickListener;
        this.mOnItemLongClickListener = onItemLongClickListener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Location location = mLocationList.get(position);
        holder.tvLocationName.setText(location.getName());

        // 点击事件（编辑）
        holder.itemView.setOnClickListener(v -> mOnItemClickListener.onItemClick(location));
        // 长按事件（删除）
        holder.itemView.setOnLongClickListener(v -> {
            mOnItemLongClickListener.onItemLongClick(location);
            return true; // 消费长按事件，避免触发点击
        });
    }

    @Override
    public int getItemCount() {
        return mLocationList.size();
    }

    /**
     * 更新数据
     */
    public void updateData(List<Location> locationList) {
        this.mLocationList = locationList;
        notifyDataSetChanged();
    }

    /**
     * 检查名称是否重复（排除编辑中的位置）
     */
    public boolean isNameDuplicate(String name, long excludeId) {
        if (name == null || name.trim().isEmpty()) return false;
        for (Location location : mLocationList) {
            if (location.getId() != excludeId && name.trim().equals(location.getName().trim())) {
                return true;
            }
        }
        return false;
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocationName;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tv_location_name);
        }
    }
}