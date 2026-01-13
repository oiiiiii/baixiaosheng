package com.baixiaosheng.inventory.model;

/**
 * 设置页入口枚举
 */
public enum SettingEntry {
    CATEGORY_MANAGE("分类管理"),
    LOCATION_MANAGE("位置管理"),
    RECYCLE_BIN("回收站"),
    ABOUT("关于");

    private final String name;

    SettingEntry(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}