package com.baixiaosheng.inventory.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.baixiaosheng.inventory.database.entity.Item;

import java.util.List;

@Dao
public interface ItemDao {
    // 基础增删改查
    @Insert
    long[] insertItem(Item... item);

    @Update
    int updateItem(Item... item);

    @Delete
    int deleteItem(Item... item);

    // 修正：补充isDeleted过滤 + 命名规范（getAllItem → getAllItems）
    @Query("SELECT * FROM item WHERE isDeleted = 0 ORDER BY validTime ASC")
    LiveData<List<Item>> getAllItems();

    // 新增：模糊搜索物品（补充isDeleted过滤）
    @Query("SELECT * FROM item WHERE isDeleted = 0 AND (name LIKE '%' || :keyword || '%' OR remark LIKE '%' || :keyword || '%') ORDER BY validTime ASC")
    LiveData<List<Item>> searchItem(String keyword);

    // 新增：按过期时间范围筛选（补充isDeleted过滤）
    @Query("SELECT * FROM item WHERE isDeleted = 0 AND (validTime BETWEEN :startDate AND :endDate OR (validTime <= :startDate AND :endDate IS NULL) OR (validTime >= :endDate AND :startDate IS NULL)) ORDER BY validTime ASC")
    LiveData<List<Item>> filterItemByExpireTime(Long startDate, Long endDate);

    // 新增：模糊搜索+过期时间筛选组合查询（补充isDeleted过滤）
    @Query("SELECT * FROM item WHERE isDeleted = 0 AND (name LIKE '%' || :keyword || '%' OR remark LIKE '%' || :keyword || '%') AND (validTime BETWEEN :startDate AND :endDate OR (validTime <= :startDate AND :endDate IS NULL) OR (validTime >= :endDate AND :startDate IS NULL)) ORDER BY validTime ASC")
    LiveData<List<Item>> searchAndFilterItem(String keyword, Long startDate, Long endDate);

    @Query("SELECT * FROM item WHERE id = :id AND isDeleted = 0")
    Item getItemById(long id);


    // 支持多个分类ID查询
    @Query("SELECT * FROM item WHERE isDeleted = 0 " +
            "AND (:keyword IS NULL OR name LIKE '%' || :keyword || '%') " +
            "AND (:parentCategoryIds IS NULL OR parentCategoryId IN (:parentCategoryIds)) " +
            "AND (:childCategoryIds IS NULL OR childCategoryId IN (:childCategoryIds)) " +
            "AND (:locationIds IS NULL OR locationId IN (:locationIds)) " +
            "AND (:quantityMin IS NULL OR count >= :quantityMin) " +
            "AND (:quantityMax IS NULL OR count <= :quantityMax) " +
            "AND (:expireStart IS NULL OR validTime >= :expireStart) " +
            "AND (:expireEnd IS NULL OR validTime <= :expireEnd)")
    LiveData<List<Item>> queryItemsByCondition(
            String keyword,
            Long parentCategoryIds,    // 改为List类型
            Long childCategoryIds,     // 改为List类型
            Long locationIds,          // 改为List类型
            Integer quantityMin,
            Integer quantityMax,
            Long expireStart,
            Long expireEnd
    );

    // 新增：根据UUID查询单个物品
    @Query("SELECT * FROM item WHERE uuid = :uuid AND isDeleted = 0 LIMIT 1")
    Item getItemByUuid(String uuid);

    // 修复：标记物品为删除（补充updateTime）
    @Query("UPDATE item SET isDeleted = 1, updateTime = :updateTime WHERE uuid = :uuid")
    void markItemAsDeleted(String uuid, long updateTime);

    // 修复：批量标记删除（补充updateTime）
    @Query("UPDATE item SET isDeleted = 1, updateTime = :updateTime WHERE uuid IN (:uuidList)")
    void batchMarkDeleted(List<String> uuidList, long updateTime);

    // 注意：以下两个接口为「物理删除」，仅回收站页面永久删除使用
    @Query("DELETE FROM item WHERE id = :itemId")
    void deleteItemById(long itemId);

    @Query("DELETE FROM item WHERE id IN (:itemIds)")
    void deleteItemsByIds(List<Long> itemIds);

    // 新增：恢复回收站物品
    @Query("UPDATE item SET isDeleted = 0, updateTime = :updateTime WHERE uuid = :uuid")
    void restoreItemFromRecycle(String uuid, long updateTime);

    // 新增：批量恢复回收站物品
    @Query("UPDATE item SET isDeleted = 0, updateTime = :updateTime WHERE uuid IN (:uuidList)")
    void batchRestoreFromRecycle(List<String> uuidList, long updateTime);

    // 修正：过期物品查询（补充空值兼容）
    @Query("SELECT * FROM item WHERE validTime < :currentTime " +
            "AND (:startDate IS NULL OR validTime >= :startDate) " +
            "AND (:endDate IS NULL OR validTime <= :endDate) " +
            "AND isDeleted = :isDeleted " +
            "ORDER BY validTime ASC")
    List<Item> getExpiredItems(Long currentTime, Long startDate, Long endDate, int isDeleted);

    // 修正：模糊搜索过期物品（补充通配符+空值兼容）
    @Query("SELECT * FROM item WHERE name LIKE '%' || :keyword || '%' " +
            "AND validTime < :currentTime " +
            "AND (:startDate IS NULL OR validTime >= :startDate) " +
            "AND (:endDate IS NULL OR validTime <= :endDate) " +
            "AND isDeleted = :isDeleted " +
            "ORDER BY validTime ASC")
    List<Item> searchExpiredItems(String keyword, Long currentTime, Long startDate, Long endDate, int isDeleted);

    // 新增：查询回收站物品（分页+关键词）
    @Query("SELECT * FROM item WHERE isDeleted = 1 " +
            "AND (:keyword IS NULL OR name LIKE '%' || :keyword || '%') " +
            "ORDER BY updateTime DESC LIMIT :pageSize OFFSET :offset")
    LiveData<List<Item>> getRecycleItems(String keyword, int pageSize, int offset);
}