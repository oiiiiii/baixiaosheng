package com.baixiaosheng.inventory.database.entity;

/**
 * 封装物品的完整展示信息
 */
public class ItemWithInfo {
    private Item item;
    private String categoryName; // 拼接后的分类名称
    private String locationName; // 位置名称

    public ItemWithInfo(Item item, String categoryName, String locationName) {
        this.item = item;
        this.categoryName = categoryName;
        this.locationName = locationName;
    }

    // Getter方法
    public Item getItem() {
        return item;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getLocationName() {
        return locationName;
    }
}