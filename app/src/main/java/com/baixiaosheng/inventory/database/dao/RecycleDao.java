package com.baixiaosheng.inventory.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.baixiaosheng.inventory.database.entity.Recycle;

import java.util.List;

/**
 * 回收站表数据访问接口
 */
@Dao
public interface RecycleDao {
    // 插入回收站记录
    @Insert
    long insertRecycle(Recycle recycle);

    // 插入多条回收站记录
    @Insert
    long[] insertRecycles(List<Recycle> recycles);

    // 更新回收站记录
    @Update
    int updateRecycle(Recycle recycle);

    // 删除回收站记录（彻底删除）
    @Delete
    int deleteRecycle(Recycle recycle);

    // 根据ID查询回收站记录
    @Query("SELECT * FROM recycle WHERE id = :id")
    Recycle getRecycleById(long id);

    // 根据物品ID查询回收站记录
    @Query("SELECT * FROM recycle WHERE itemId = :itemId")
    Recycle getRecycleByItemId(long itemId);

    // 查询所有回收站记录
    @Query("SELECT * FROM recycle ORDER BY deleteTime DESC")
    List<Recycle> getAllRecycles();

    // 删除回收站中指定物品ID的记录（还原物品时调用）
    @Query("DELETE FROM recycle WHERE itemId = :itemId")
    int deleteRecycleByItemId(long itemId);

    //阶段7新添加的，有可能不对
    // 获取所有回收站物品
    @Query("SELECT * FROM recycle ORDER BY deleteTime DESC")
    List<Recycle> getAllRecycleItems();

    // 根据ID删除回收站记录
    @Query("DELETE FROM recycle WHERE id = :recycleId")
    void deleteRecycleItemById(long recycleId);

    // 批量删除回收站记录
    @Query("DELETE FROM recycle WHERE id IN (:recycleIds)")
    void deleteRecycleItemsByIds(List<Long> recycleIds);
}