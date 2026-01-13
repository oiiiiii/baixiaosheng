package com.baixiaosheng.inventory.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Recycle;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 回收站列表适配器
 */
public class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.RecycleBinViewHolder> {

    private List<Recycle> mRecycleList;
    private final OnItemSelectListener mOnItemSelectListener;
    private final OnRestoreClickListener mOnRestoreClickListener;
    private final OnDeleteForeverClickListener mOnDeleteForeverClickListener;
    private final SimpleDateFormat mDateFormat;

    // 选中状态变化回调
    public interface OnItemSelectListener {
        void onItemSelect(long recycleId, long itemId, boolean isSelected);
    }

    // 还原点击回调
    public interface OnRestoreClickListener {
        void onRestoreClick(long recycleId, long itemId);
    }

    // 彻底删除点击回调
    public interface OnDeleteForeverClickListener {
        void onDeleteForeverClick(long recycleId, long itemId);
    }

    public RecycleAdapter(List<Recycle> recycleList, OnItemSelectListener onItemSelectListener,
                          OnRestoreClickListener onRestoreClickListener, OnDeleteForeverClickListener onDeleteForeverClickListener) {
        this.mRecycleList = recycleList;
        this.mOnItemSelectListener = onItemSelectListener;
        this.mOnRestoreClickListener = onRestoreClickListener;
        this.mOnDeleteForeverClickListener = onDeleteForeverClickListener;
        this.mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public RecycleBinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycle, parent, false);
        return new RecycleBinViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecycleBinViewHolder holder, int position) {
        Recycle recycleItem = mRecycleList.get(position);
        // 设置物品名称
        holder.tvItemName.setText(recycleItem.getItemName());
        // 设置删除时间
        String deleteTime = mDateFormat.format(new Date(recycleItem.getDeleteTime()));
        holder.tvItemDeleteTime.setText("删除时间：" + deleteTime);
        // 复选框选中状态
        holder.cbSelectItem.setChecked(false);
        // 复选框点击事件
        holder.cbSelectItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mOnItemSelectListener.onItemSelect(recycleItem.getId(), recycleItem.getItemId(), isChecked);
        });
        // 还原按钮点击
        holder.btnRestore.setOnClickListener(v -> {
            mOnRestoreClickListener.onRestoreClick(recycleItem.getId(), recycleItem.getItemId());
        });
        // 彻底删除按钮点击
        holder.btnDeleteForever.setOnClickListener(v -> {
            mOnDeleteForeverClickListener.onDeleteForeverClick(recycleItem.getId(), recycleItem.getItemId());
        });
    }

    @Override
    public int getItemCount() {
        return mRecycleList.size();
    }

    /**
     * 更新数据
     */
    public void updateData(List<Recycle> recycleList) {
        this.mRecycleList = recycleList;
        notifyDataSetChanged();
    }

    static class RecycleBinViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelectItem;
        TextView tvItemName;
        TextView tvItemDeleteTime;
        Button btnRestore;
        Button btnDeleteForever;

        public RecycleBinViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelectItem = itemView.findViewById(R.id.cb_select_item);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvItemDeleteTime = itemView.findViewById(R.id.tv_item_delete_time);
            btnRestore = itemView.findViewById(R.id.btn_restore);
            btnDeleteForever = itemView.findViewById(R.id.btn_delete_forever);
        }
    }
}