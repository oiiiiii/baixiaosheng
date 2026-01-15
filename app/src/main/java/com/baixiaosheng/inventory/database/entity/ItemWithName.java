package com.baixiaosheng.inventory.database.entity;

import androidx.room.Embedded;

/**
 * 承载物品+分类/位置名称的关联查询结果
 */
public class ItemWithName {
    // 嵌入原有Item实体（保留所有item表字段）
    @Embedded
    public Item item;

    // 分类名称（从category表关联）
    public String categoryName;

    // 位置名称（从location表关联）
    public String locationName;

    // 可选：父分类名称（如果需要）
    public String parentCategoryName;
}