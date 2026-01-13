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
    // 基础增删改查（阶段2已实现，此处保留）
    @Insert
    long[] insertItem(Item... item);

    @Update
    int updateItem(Item... item);

    @Delete
    int deleteItem(Item... item);

    @Query("SELECT * FROM item ORDER BY validTime ASC")
    LiveData<List<Item>> getAllItem();

    // 新增：模糊搜索物品（名称/说明）
    @Query("SELECT * FROM item WHERE name LIKE '%' || :keyword || '%' OR remark LIKE '%' || :keyword || '%' ORDER BY ValidTime ASC")
    LiveData<List<Item>> searchItem(String keyword);

    // 新增：按过期时间范围筛选（endDate为null则查<=startDate，startDate为null则查>=endDate）
    @Query("SELECT * FROM item WHERE validTime BETWEEN :startDate AND :endDate OR (validTime <= :startDate AND :endDate IS NULL) OR (ValidTime >= :endDate AND :startDate IS NULL) ORDER BY ValidTime ASC")
    LiveData<List<Item>> filterItemByExpireTime(Long startDate, Long endDate);

    // 新增：模糊搜索+过期时间筛选组合查询
    @Query("SELECT * FROM item WHERE (name LIKE '%' || :keyword || '%' OR remark LIKE '%' || :keyword || '%') AND (ValidTime BETWEEN :startDate AND :endDate OR (ValidTime <= :startDate AND :endDate IS NULL) OR (ValidTime >= :endDate AND :startDate IS NULL)) ORDER BY ValidTime ASC")
    LiveData<List<Item>> searchAndFilterItem(String keyword, Long startDate, Long endDate);

    @Query("SELECT * FROM item WHERE id = :id")
    Item getItemById(long id);


    @Query("SELECT * FROM item") // 注意：原getAllItem是LiveData，这里加非LiveData版本供DatabaseManager调用
    List<Item> getAllItems();

    // 新增：多条件查询（核心筛选接口）
    @Query("SELECT * FROM item WHERE isDeleted = 0 " +
            "AND (:keyword IS NULL OR name LIKE '%' || :keyword || '%') " +
            "AND (:parentCategory IS NULL OR parentCategoryId = :parentCategory) " +
            "AND (:childCategory IS NULL OR childCategoryId = :childCategory) " +
            "AND (:location IS NULL OR locationId = :location) " +
            "AND (:quantityMin IS NULL OR count >= :quantityMin) " +
            "AND (:quantityMax IS NULL OR count <= :quantityMax) " +
            "AND (:expireStart IS NULL OR validTime >= :expireStart) " +
            "AND (:expireEnd IS NULL OR validTime <= :expireEnd)")
    LiveData<List<Item>> queryItemsByCondition(
            String keyword,
            String parentCategory,
            String childCategory,
            String location,
            Integer quantityMin,
            Integer quantityMax,
            Long expireStart,  // 替换 Date → Long（毫秒时间戳）
            Long expireEnd     // 替换 Date → Long（毫秒时间戳）
    );

    // 新增：根据UUID查询单个物品
    @Query("SELECT * FROM item WHERE uuid = :uuid AND isDeleted = 0 LIMIT 1")
    Item getItemByUuid(String uuid);

    // 新增：标记物品为删除（回收站）
    @Query("UPDATE item SET isDeleted = 1 WHERE uuid = :uuid")
    void markItemAsDeleted(String uuid);

    // 新增：批量标记删除
    @Query("UPDATE item SET isDeleted = 1 WHERE uuid IN (:uuidList)")
    void batchMarkDeleted(List<String> uuidList);

    //阶段7新添加的，有可能不对
    // 根据ID删除物品
    @Query("DELETE FROM item WHERE id = :itemId")
    void deleteItemById(long itemId);

    // 批量删除物品
    @Query("DELETE FROM item WHERE id IN (:itemIds)")
    void deleteItemsByIds(List<Long> itemIds);

    // 新增：查询过期物品的核心接口
    @Query("SELECT * FROM item WHERE validTime < :currentTime " +
            "AND validTime >= :startDate AND validTime <= :endDate " +
            "AND isDeleted = :isDeleted " +
            "ORDER BY validTime ASC")
    List<Item> getExpiredItems(long currentTime, long startDate, long endDate, int isDeleted);

    // 新增：模糊搜索过期物品
    @Query("SELECT * FROM item WHERE name LIKE :keyword " +
            "AND validTime < :currentTime " +
            "AND validTime >= :startDate AND validTime <= :endDate " +
            "AND isDeleted = :isDeleted " +
            "ORDER BY validTime ASC")
    List<Item> searchExpiredItems(String keyword, long currentTime, long startDate, long endDate, int isDeleted);
}