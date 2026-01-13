package com.baixiaosheng.inventory.model;

import com.baixiaosheng.inventory.R; // 确保导入R类

/**
 * 设置页入口枚举
 */
public enum SettingEntry {
    // 枚举常量：名称 + 显示文本 + 图标资源ID
    CATEGORY_MANAGE("分类管理", R.drawable.ic_arrow_right),
    LOCATION_MANAGE("位置管理", R.drawable.ic_arrow_right),
    RECYCLE_BIN("回收站", R.drawable.ic_arrow_right),
    DATA_MANAGE("数据管理", R.drawable.ic_arrow_right),// 新增数据管理枚举
    ABOUT("关于页面", R.drawable.ic_arrow_right); // 对齐原代码中的"关于页面"文本

    private final String name;       // 显示文本
    private final int iconResId;     // 图标资源ID

    // 枚举构造方法（private修饰，默认也是private）
    SettingEntry(String name, int iconResId) {
        this.name = name;
        this.iconResId = iconResId;
    }

    // 获取显示文本
    public String getName() {
        return name;
    }

    // 获取图标资源ID
    public int getIconResId() {
        return iconResId;
    }
}