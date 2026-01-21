package com.baixiaosheng.inventory.database.dao;

import androidx.lifecycle.LiveData;
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

    // 根据ID查询回收站记录（同步，仅用于后台操作）
    @Query("SELECT * FROM recycle WHERE id = :id")
    Recycle getRecycleById(long id);

    // 根据物品ID查询回收站记录（同步，仅用于后台操作）
    @Query("SELECT * FROM recycle WHERE itemId = :itemId")
    Recycle getRecycleByItemId(long itemId);

    // 【核心修复1】获取所有有效回收站物品（关联Item表，确保isDeleted=1）
    // 返回LiveData，支持数据监听和UI自动刷新
    @Query("SELECT r.* FROM recycle r " +
            "JOIN item i ON r.itemId = i.id " +
            "WHERE i.isDeleted = 1 " +  // 仅显示未恢复的物品
            "ORDER BY r.deleteTime DESC")
    LiveData<List<Recycle>> getAllRecycleItemsSyncLive();

    // 保留同步查询方法（用于后台批量操作，非UI展示）
    @Query("SELECT * FROM recycle ORDER BY deleteTime DESC")
    List<Recycle> getAllRecycleItemsSync();

    // 根据ID删除回收站记录
    @Query("DELETE FROM recycle WHERE id = :recycleId")
    int deleteRecycleItemById(long recycleId);

    // 批量删除回收站记录
    @Query("DELETE FROM recycle WHERE id IN (:recycleIds)")
    int deleteRecycleItemsByIds(List<Long> recycleIds);
}