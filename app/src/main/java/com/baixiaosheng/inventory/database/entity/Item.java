package com.baixiaosheng.inventory.database.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.UUID;

/**
 * 物品表（核心业务表）
 * uuid：唯一标识（避免id自增重复问题）
 * id：主键自增
 * name：物品名称（必选）
 * parentCategoryId：父分类ID（关联Category表）
 * childCategoryId：子分类ID（关联Category表）
 * locationId：位置ID（关联Location表）
 * validTime：有效期（毫秒时间戳，可选）
 * count：物品数量（可选，默认1）
 * imagePaths：图片路径（多图用","分隔，如"/sdcard/1.jpg,/sdcard/2.jpg"）
 * remark：物品说明（可选）
 * createTime：创建时间
 * updateTime：更新时间
 * isDeleted：删除标记（0=未删除，1=已删除，默认0）
 */
@Entity(tableName = "item")
public class Item {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String uuid; // 唯一标识，创建时生成UUID
    private String name;
    private long parentCategoryId; // 父分类ID，0表示未分类
    private long childCategoryId; // 子分类ID，0表示未分类
    private long locationId; // 位置ID，0表示未指定
    private long validTime; // 有效期，0表示永久
    private int count; // 数量，默认1
    private String imagePaths; // 图片路径，多图逗号分隔
    private String remark; // 物品说明
    private long createTime;
    private long updateTime;
    private int isDeleted; // 删除标记：0=未删除，1=已删除

    // 空构造函数
    public Item() {
        // 默认生成UUID
        this.uuid = UUID.randomUUID().toString();
        // 默认数量为1
        this.count = 1;
        // 默认分类/位置为0
        this.parentCategoryId = 0;
        this.childCategoryId = 0;
        this.locationId = 0;
        // 默认有效期永久
        this.validTime = 0;
        // 默认未删除
        this.isDeleted = 0;
    }

    // 带参构造函数（核心字段）
    @Ignore
    public Item(String name, long parentCategoryId, long childCategoryId, long locationId,
                long validTime, int count, String imagePaths, String remark,
                long createTime, long updateTime) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.parentCategoryId = parentCategoryId;
        this.childCategoryId = childCategoryId;
        this.locationId = locationId;
        this.validTime = validTime;
        this.count = count;
        this.imagePaths = imagePaths;
        this.remark = remark;
        this.createTime = createTime;
        this.updateTime = updateTime;
        // 默认未删除
        this.isDeleted = 0;
    }

    // 扩展带参构造函数（包含isDeleted，按需使用）
    @Ignore
    public Item(String name, long parentCategoryId, long childCategoryId, long locationId,
                long validTime, int count, String imagePaths, String remark,
                long createTime, long updateTime, int isDeleted) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.parentCategoryId = parentCategoryId;
        this.childCategoryId = childCategoryId;
        this.locationId = locationId;
        this.validTime = validTime;
        this.count = count;
        this.imagePaths = imagePaths;
        this.remark = remark;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.isDeleted = isDeleted;
    }

    // Getter & Setter
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getParentCategoryId() {
        return parentCategoryId;
    }

    public void setParentCategoryId(long parentCategoryId) {
        this.parentCategoryId = parentCategoryId;
    }

    public long getChildCategoryId() {
        return childCategoryId;
    }

    public void setChildCategoryId(long childCategoryId) {
        this.childCategoryId = childCategoryId;
    }

    public long getLocationId() {
        return locationId;
    }

    public void setLocationId(long locationId) {
        this.locationId = locationId;
    }

    public long getValidTime() {
        return validTime;
    }

    public void setValidTime(long validTime) {
        this.validTime = validTime;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getImagePaths() {
        return imagePaths;
    }

    public void setImagePaths(String imagePaths) {
        this.imagePaths = imagePaths;
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

    // isDeleted 的 Getter & Setter
    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
}