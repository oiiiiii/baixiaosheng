package com.baixiaosheng.inventory.database.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 位置表（存储物品放置位置）
 * id：主键自增
 * name：位置名称（如"仓库A-货架1"）
 * remark：位置备注（可选）
 * createTime：创建时间
 * updateTime：更新时间
 */
@Entity(tableName = "location")
public class Location {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String remark; // 可选备注
    private long createTime;
    private long updateTime;

    // 空构造函数
    public Location() {}

    // 带参构造函数
    @Ignore
    public Location(String name, String remark, long createTime, long updateTime) {
        this.name = name;
        this.remark = remark;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    // Getter & Setter
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}