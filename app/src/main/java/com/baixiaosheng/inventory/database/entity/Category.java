package com.baixiaosheng.inventory.database.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 分类表（支持父子分类级联）
 * id：主键自增
 * parentCategoryId：父分类ID（0表示一级分类）
 * categoryName：分类名称
 * createTime：创建时间（时间戳）
 * updateTime：更新时间（时间戳）
 */
@Entity(tableName = "category")
public class Category {
    // 主键自增
    @PrimaryKey(autoGenerate = true)
    private long id;
    // 父分类ID，0代表一级分类（规范字段名：parentCategoryId）
    private long parentCategoryId;
    // 分类名称（规范字段名：categoryName）
    private String categoryName;
    // 创建时间（毫秒级时间戳）
    private long createTime;
    // 更新时间（毫秒级时间戳）
    private long updateTime;

    // 空构造函数（Room要求）
    public Category() {}

    // 带参构造函数（方便创建）
    @Ignore
    public Category(long parentCategoryId, String categoryName, long createTime, long updateTime) {
        this.parentCategoryId = parentCategoryId;
        this.categoryName = categoryName;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    // Getter & Setter （规范命名）
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getParentCategoryId() {
        return parentCategoryId;
    }

    public void setParentCategoryId(long parentCategoryId) {
        this.parentCategoryId = parentCategoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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