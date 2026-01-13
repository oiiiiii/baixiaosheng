package com.baixiaosheng.inventory.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

/**
 * 分类表（支持父子分类级联）
 * id：主键自增
 * parentId：父分类ID（0表示一级分类）
 * name：分类名称
 * createTime：创建时间（时间戳）
 * updateTime：更新时间（时间戳）
 */
@Entity(tableName = "category")
public class Category {
    // 主键自增
    @PrimaryKey(autoGenerate = true)
    private long id;
    // 父分类ID，0代表一级分类
    private long parentId;
    // 分类名称
    private String name;
    // 创建时间（毫秒级时间戳）
    private long createTime;
    // 更新时间（毫秒级时间戳）
    private long updateTime;

    // 空构造函数（Room要求）
    public Category() {}

    // 带参构造函数（方便创建）
    @Ignore
    public Category(long parentId, String name, long createTime, long updateTime) {
        this.parentId = parentId;
        this.name = name;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    // Getter & Setter （Room需要访问字段）
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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