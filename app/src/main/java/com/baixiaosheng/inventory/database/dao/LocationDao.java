package com.baixiaosheng.inventory.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.baixiaosheng.inventory.database.entity.Location;

import java.util.List;

/**
 * 位置表数据访问接口
 */
@Dao
public interface LocationDao {
    // 插入单条位置
    @Insert
    long insertLocation(Location location);

    // 插入多条位置
    @Insert
    long[] insertLocations(List<Location> locations);

    // 更新位置
    @Update
    int updateLocation(Location location);

    // 删除位置
    @Delete
    int deleteLocation(Location location);

    // 根据ID查询位置
    @Query("SELECT * FROM location WHERE id = :id")
    Location getLocationById(long id);

    // 新增：根据名称精确查询位置（供导入去重使用）
    @Query("SELECT * FROM location WHERE name = :name")
    List<Location> getLocationByName(String name);

    // 查询所有位置
    @Query("SELECT * FROM location ORDER BY createTime DESC")
    List<Location> getAllLocations();

    // 根据名称模糊查询
    @Query("SELECT * FROM location WHERE name LIKE '%' || :name || '%'")
    List<Location> searchLocation(String name);

    // 新增：根据ID删除分类（解决编译报错的核心）
    @Query("DELETE FROM location WHERE id = :locationId")
    int deleteLocationById(long locationId);

    //阶段7新添加的，有可能不对
    // 获取位置关联的物品数量
    @Query("SELECT COUNT(*) FROM item WHERE locationId = :locationId AND isDeleted = 0")
    int getRelatedItemCount(long locationId);
}