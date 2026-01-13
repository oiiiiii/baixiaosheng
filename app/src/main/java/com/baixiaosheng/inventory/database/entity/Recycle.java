package com.baixiaosheng.inventory.database.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 回收站表（存储被删除的物品记录）
 * id：主键自增
 * itemId：关联物品表的ID
 * itemUuid：关联物品表的UUID（冗余存储，防止物品表数据丢失）
 * itemName：物品名称（冗余存储，用于列表展示）
 * deleteTime：删除时间
 * deleteReason：删除原因（可选）
 */
@Entity(tableName = "recycle")
public class Recycle {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long itemId; // 关联物品ID
    private String itemUuid; // 物品UUID
    private String itemName; // 物品名称（新增）
    private long deleteTime; // 删除时间
    private String deleteReason; // 删除原因（可选）

    // 空构造函数
    public Recycle() {}

    // 带参构造函数（新增 itemName 参数）
    @Ignore
    public Recycle(long itemId, String itemUuid, String itemName, long deleteTime, String deleteReason) {
        this.itemId = itemId;
        this.itemUuid = itemUuid;
        this.itemName = itemName; // 赋值
        this.deleteTime = deleteTime;
        this.deleteReason = deleteReason;
    }

    // Getter & Setter（新增 itemName 的 Get/Set 方法）
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public String getItemUuid() {
        return itemUuid;
    }

    public void setItemUuid(String itemUuid) {
        this.itemUuid = itemUuid;
    }

    // 新增 itemName Getter
    public String getItemName() {
        return itemName;
    }

    // 新增 itemName Setter
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public long getDeleteTime() {
        return deleteTime;
    }

    public void setDeleteTime(long deleteTime) {
        this.deleteTime = deleteTime;
    }

    public String getDeleteReason() {
        return deleteReason;
    }

    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }
}