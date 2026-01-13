package com.baixiaosheng.inventory.model;

import java.util.Date;

/**
 * 筛选条件实体类：封装查询页的多条件筛选参数
 */
public class FilterCondition {
    // 基础搜索
    private String searchKeyword; // 物品名称模糊搜索关键词
    // 分类筛选
    private String parentCategory; // 父分类（空则不筛选）
    private String childCategory; // 子分类（空则不筛选）
    // 位置筛选
    private String location; // 放置位置（空则不筛选）
    // 数量筛选
    private Integer quantityMin; // 数量最小值（null则不筛选）
    private Integer quantityMax; // 数量最大值（null则不筛选）
    // 过期时间筛选
    private Date expireStart; // 过期开始时间（null则不筛选）
    private Date expireEnd; // 过期结束时间（null则不筛选）

    // 空构造
    public FilterCondition() {}

    // 全参构造
    public FilterCondition(String searchKeyword, String parentCategory, String childCategory,
                           String location, Integer quantityMin, Integer quantityMax,
                           Date expireStart, Date expireEnd) {
        this.searchKeyword = searchKeyword;
        this.parentCategory = parentCategory;
        this.childCategory = childCategory;
        this.location = location;
        this.quantityMin = quantityMin;
        this.quantityMax = quantityMax;
        this.expireStart = expireStart;
        this.expireEnd = expireEnd;
    }

    // getter & setter
    public String getSearchKeyword() {
        return searchKeyword == null ? "" : searchKeyword.trim();
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public String getParentCategory() {
        return parentCategory == null ? "" : parentCategory.trim();
    }

    public void setParentCategory(String parentCategory) {
        this.parentCategory = parentCategory;
    }

    public String getChildCategory() {
        return childCategory == null ? "" : childCategory.trim();
    }

    public void setChildCategory(String childCategory) {
        this.childCategory = childCategory;
    }

    public String getLocation() {
        return location == null ? "" : location.trim();
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getQuantityMin() {
        return quantityMin;
    }

    public void setQuantityMin(Integer quantityMin) {
        this.quantityMin = quantityMin;
    }

    public Integer getQuantityMax() {
        return quantityMax;
    }

    public void setQuantityMax(Integer quantityMax) {
        this.quantityMax = quantityMax;
    }

    public Date getExpireStart() {
        return expireStart;
    }

    public void setExpireStart(Date expireStart) {
        this.expireStart = expireStart;
    }

    public Date getExpireEnd() {
        return expireEnd;
    }

    public void setExpireEnd(Date expireEnd) {
        this.expireEnd = expireEnd;
    }

    /**
     * 判断是否为默认筛选条件（无任何筛选）
     */
    public boolean isDefault() {
        return getSearchKeyword().isEmpty()
                && getParentCategory().isEmpty()
                && getChildCategory().isEmpty()
                && getLocation().isEmpty()
                && quantityMin == null
                && quantityMax == null
                && expireStart == null
                && expireEnd == null;
    }

    /**
     * 重置筛选条件为默认
     */
    public void reset() {
        searchKeyword = "";
        parentCategory = "";
        childCategory = "";
        location = "";
        quantityMin = null;
        quantityMax = null;
        expireStart = null;
        expireEnd = null;
    }
}