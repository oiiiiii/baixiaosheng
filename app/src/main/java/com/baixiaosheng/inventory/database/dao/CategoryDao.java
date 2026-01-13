package com.baixiaosheng.inventory.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.entity.Item;

import java.util.List;

/**
 * 分类表数据访问接口
 * Room会自动生成该接口的实现类，无需手动编写
 */
@Dao
public interface CategoryDao {
    // 插入单条分类
    @Insert
    long insertCategory(Category category);

    // 插入多条分类
    @Insert
    long[] insertCategories(List<Category> categories);

    // 更新分类
    @Update
    int updateCategory(Category category);

    // 删除分类
    @Delete
    int deleteCategory(Category category);

    // 根据ID查询分类
    @Query("SELECT * FROM category WHERE id = :id")
    Category getCategoryById(long id);

    // 查询所有一级分类（parentId=0）
    @Query("SELECT * FROM category WHERE parentId = 0 ORDER BY createTime DESC")
    List<Category> getParentCategories();

    // 根据父分类ID查询子分类
    @Query("SELECT * FROM category WHERE parentId = :parentId ORDER BY createTime DESC")
    List<Category> getChildCategoriesByParentId(long parentId);

    // 查询所有分类
    @Query("SELECT * FROM category ORDER BY parentId ASC, createTime DESC")
    List<Category> getAllCategories();

    // 根据分类名称查询（模糊匹配）
    @Query("SELECT * FROM category WHERE name LIKE '%' || :name || '%'")
    List<Category> searchCategory(String name);

    // 新增：根据ID删除分类（解决编译报错的核心）
    @Query("DELETE FROM category WHERE id = :categoryId")
    int deleteCategoryById(long categoryId);
    //阶段7新添加的，有可能不对
    // 获取分类关联的物品数量
    @Query("SELECT COUNT(*) FROM item WHERE parentCategoryId = :categoryId AND isDeleted = 0")
    int getRelatedItemCount(long categoryId);
}