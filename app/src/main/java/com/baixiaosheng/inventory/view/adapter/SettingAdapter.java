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

    private List<SettingEntry> mEntryList;
    private OnItemClickListener mOnItemClickListener;

    // 点击事件回调

    public SettingAdapter(List<SettingEntry> entryList, OnItemClickListener listener) {
        this.mEntryList = entryList;
        this.mOnItemClickListener = listener;
    }

    // 新增：设置点击回调的方法（支持置空）
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    // 原有点击回调接口（保持不变）
    public interface OnItemClickListener {
        void onItemClick(SettingEntry entry);
    }


    @NonNull
    @Override
    public SettingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_setting_entry, parent, false);
        return new SettingViewHolder(view);
    }



    // 修改 SettingAdapter.java 的 onBindViewHolder 方法
    @Override
    public void onBindViewHolder(@NonNull SettingViewHolder holder, int position) {
        SettingEntry entry = mEntryList.get(position);
        holder.tvEntryName.setText(entry.getName());
        // 校验回调不为空，避免空指针
        holder.itemView.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(entry);
            }
        });
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