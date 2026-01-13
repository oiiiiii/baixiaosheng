package com.baixiaosheng.inventory.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private final OnEditClickListener mOnEditClickListener;
    private final OnDeleteClickListener mOnDeleteClickListener;

    // 编辑点击回调
    public interface OnEditClickListener {
        void onEditClick(Location location);
    }

    // 删除点击回调
    public interface OnDeleteClickListener {
        void onDeleteClick(Location location);
    }

    public LocationAdapter(List<Location> locationList, OnEditClickListener onEditClickListener, OnDeleteClickListener onDeleteClickListener) {
        this.mLocationList = locationList;
        this.mOnEditClickListener = onEditClickListener;
        this.mOnDeleteClickListener = onDeleteClickListener;
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
        // 编辑按钮点击
        holder.btnEditLocation.setOnClickListener(v -> mOnEditClickListener.onEditClick(location));
        // 删除按钮点击
        holder.btnDeleteLocation.setOnClickListener(v -> mOnDeleteClickListener.onDeleteClick(location));
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

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocationName;
        Button btnEditLocation;
        Button btnDeleteLocation;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tv_location_name);
            btnEditLocation = itemView.findViewById(R.id.btn_edit_location);
            btnDeleteLocation = itemView.findViewById(R.id.btn_delete_location);
        }
    }
}