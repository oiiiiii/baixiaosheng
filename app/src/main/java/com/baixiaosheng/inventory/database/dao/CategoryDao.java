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
    // ==================== 基础增删改查 ====================
    /** 插入单条分类 */
    @Insert
    long insertCategory(Category category);

    /** 批量插入分类 */
    @Insert
    long[] insertCategoryBatch(List<Category> categories);

    /** 更新分类 */
    @Update
    int updateCategory(Category category);

    /** 删除分类（实体类方式） */
    @Delete
    int deleteCategory(Category category);

    /** 根据ID删除分类 */
    @Query("DELETE FROM category WHERE id = :categoryId")
    int deleteCategoryById(long categoryId);

    /** 根据ID查询分类 */
    @Query("SELECT * FROM category WHERE id = :categoryId")
    Category getCategoryById(long categoryId);

    // ==================== 父/子分类专用查询 ====================
    /** 查询所有顶级父分类（parentCategoryId=0） */
    @Query("SELECT * FROM category WHERE parentCategoryId = 0 ORDER BY categoryName ASC")
    List<Category> listTopLevelParentCategories();

    /** 根据父分类ID查询子分类列表 */
    @Query("SELECT * FROM category WHERE parentCategoryId = :parentCategoryId ORDER BY categoryName ASC")
    List<Category> listChildCategoriesByParentId(long parentCategoryId);

    /** 查询所有分类（按父分类ID升序、创建时间降序） */
    @Query("SELECT * FROM category ORDER BY parentCategoryId ASC, createTime DESC")
    List<Category> listAllCategories();

    /** 模糊搜索分类（按名称） */
    @Query("SELECT * FROM category WHERE categoryName LIKE '%' || :keyword || '%'")
    List<Category> searchCategoriesByKeyword(String keyword);

    // ==================== 关联物品操作 ====================
    /** 查询指定父分类关联的有效物品数量 */
    @Query("SELECT COUNT(*) FROM item WHERE parentCategoryId = :categoryId AND isDeleted = 0")
    int countItemsByParentCategoryId(long categoryId);

    /** 查询指定子分类关联的有效物品数量 */
    @Query("SELECT COUNT(*) FROM item WHERE childCategoryId = :categoryId AND isDeleted = 0")
    int countItemsByChildCategoryId(long categoryId);

    /** 根据名称+父分类ID查询分类（导入去重） */
    @Query("SELECT * FROM category WHERE categoryName = :categoryName AND parentCategoryId = :parentCategoryId")
    List<Category> getCategoriesByCategoryNameAndParentId(String categoryName, long parentCategoryId);

    /** 清空指定父分类关联物品的父分类ID */
    @Query("UPDATE item SET parentCategoryId = 0, childCategoryId = 0 WHERE parentCategoryId = :parentCategoryId")
    void clearItemParentCategoryId(long parentCategoryId);

    /** 清空指定子分类关联物品的子分类ID */
    @Query("UPDATE item SET childCategoryId = 0 WHERE childCategoryId = :childCategoryId")
    void clearItemChildCategoryId(long childCategoryId);

    /** 查询指定父分类下的子分类数量 */
    @Query("SELECT COUNT(*) FROM category WHERE parentCategoryId = :parentCategoryId")
    int countChildCategoriesByParentId(long parentCategoryId);
    // ==================== 关联物品统计与清理 ====================
    /** 获取分类关联的物品数量（包括作为父分类或子分类） */
    @Query("SELECT COUNT(*) FROM item WHERE (parentCategoryId = :categoryId OR childCategoryId = :categoryId) AND isDeleted = 0")
    int getRelatedItemCount(long categoryId);

    /** 清空父分类关联物品的分类信息（包括父分类ID和子分类ID） */
    @Query("UPDATE item SET parentCategoryId = 0, childCategoryId = 0 WHERE parentCategoryId = :parentId")
    void clearItemCategoryByParentId(long parentId);

    /** 清空子分类关联物品的分类信息（仅清空子分类ID） */
    @Query("UPDATE item SET childCategoryId = 0 WHERE childCategoryId = :childId")
    void clearItemCategoryByChildId(long childId);
}