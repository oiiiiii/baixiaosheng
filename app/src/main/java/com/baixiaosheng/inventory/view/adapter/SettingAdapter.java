package com.baixiaosheng.inventory.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.model.SettingEntry;

import java.util.List;

/**
 * 设置页列表适配器
 */
public class SettingAdapter extends RecyclerView.Adapter<SettingAdapter.SettingViewHolder> {

    private final List<SettingEntry> mEntryList;
    private final OnItemClickListener mOnItemClickListener;

    // 点击事件回调
    public interface OnItemClickListener {
        void onItemClick(SettingEntry entry);
    }

    public SettingAdapter(List<SettingEntry> entryList, OnItemClickListener onItemClickListener) {
        this.mEntryList = entryList;
        this.mOnItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public SettingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_setting_entry, parent, false);
        return new SettingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingViewHolder holder, int position) {
        SettingEntry entry = mEntryList.get(position);
        holder.tvEntryName.setText(entry.getName());
        holder.itemView.setOnClickListener(v -> mOnItemClickListener.onItemClick(entry));
    }

    @Override
    public int getItemCount() {
        return mEntryList.size();
    }

    static class SettingViewHolder extends RecyclerView.ViewHolder {
        TextView tvEntryName;

        public SettingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEntryName = itemView.findViewById(R.id.tv_entry_name);
        }
    }
}