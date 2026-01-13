package com.baixiaosheng.inventory.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Item;

/**
 * 物品操作弹窗工具类（通用，供Home/QueryFragment复用）
 * 封装弹窗创建、事件绑定逻辑，避免重复代码
 */
public class ItemDialogUtils {

    // 弹窗点击回调接口
    public interface OnItemOperateListener {
        void onViewDetail(Item item); // 查看详情
        void onEdit(Item item);       // 编辑
        void onDelete(Item item);     // 删除（移入回收站）
    }

    /**
     * 显示物品操作弹窗
     * @param context 上下文
     * @param item 选中的物品
     * @param listener 操作回调
     */
    public static void showItemOperateDialog(Context context, Item item, OnItemOperateListener listener) {
        // 加载弹窗布局
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_item_operation, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // 绑定控件
        TextView tvViewDetail = dialogView.findViewById(R.id.tv_view_detail);
        TextView tvEdit = dialogView.findViewById(R.id.tv_edit);
        TextView tvDelete = dialogView.findViewById(R.id.tv_delete);
        TextView tvCancel = dialogView.findViewById(R.id.tv_cancel);

        // 查看详情
        tvViewDetail.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetail(item);
            }
            dialog.dismiss();
        });

        // 编辑
        tvEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(item);
            }
            dialog.dismiss();
        });

        // 删除
        tvDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(item);
            }
            dialog.dismiss();
        });

        // 取消
        tvCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}