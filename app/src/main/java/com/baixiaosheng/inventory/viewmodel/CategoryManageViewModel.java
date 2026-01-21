package com.baixiaosheng.inventory.viewmodel;

import android.app.Application;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.DatabaseManager;
import com.baixiaosheng.inventory.database.entity.Category;

import java.util.List;

public class CategoryManageViewModel extends AndroidViewModel {

    private DatabaseManager dbManager;
    private MutableLiveData<List<Category>> parentCategories; // 仅存储父分类
    private MutableLiveData<Boolean> operationResult;
    private MutableLiveData<List<Category>> allCategories; //存储所有分类


    public CategoryManageViewModel(@NonNull Application application) {
        super(application);
        dbManager = DatabaseManager.getInstance(application);
        parentCategories = new MutableLiveData<>();
        allCategories = new MutableLiveData<>();
        operationResult = new MutableLiveData<>();
    }

    /**
     * 获取所有父分类（parentId=0）
     */
    public LiveData<List<Category>> getParentCategories() {
        return parentCategories;
    }

    /**
     * 获取所有分类
     */
    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    /**
     * 获取操作结果
     */
    public LiveData<Boolean> getOperationResult() {
        return operationResult;
    }

    /**
     * 加载所有父分类
     */
    public void loadParentCategories() {
        AsyncTask.execute(() -> {
            List<Category> parents = dbManager.listTopLevelParentCategories();
            parentCategories.postValue(parents);
        });
    }

    /**
     * 加载所有分类
     */
    public void loadAllCategories() {
        AsyncTask.execute(() -> {
            List<Category> categories = dbManager.listAllCategories();
            allCategories.postValue(categories);
        });
    }

    /**
     * 根据父分类ID获取子分类
     */
    public LiveData<List<Category>> getChildCategories(long parentId) {
        MutableLiveData<List<Category>> childCategories = new MutableLiveData<>();
        AsyncTask.execute(() -> {
            List<Category> list = dbManager.listChildCategoriesByParentId(parentId);
            childCategories.postValue(list);
        });
        return childCategories;
    }

    /**
     * 添加分类（支持父/子分类）
     */
    public void addCategory(Category category) {
        AsyncTask.execute(() -> {
            try {
                long id = dbManager.addCategory(category);
                operationResult.postValue(id > 0);
            } catch (Exception e) {
                e.printStackTrace();
                operationResult.postValue(false);
            }
        });
    }

    /**
     * 更新分类
     */
    public void updateCategory(Category category) {
        AsyncTask.execute(() -> {
            try {
                int rows = dbManager.updateCategory(category);
                operationResult.postValue(rows > 0);
            } catch (Exception e) {
                e.printStackTrace();
                operationResult.postValue(false);
            }
        });
    }



    /**
     * 删除分类（优化逻辑）
     */
    public void deleteCategory(Category category) {
        AsyncTask.execute(() -> {
            try {
                if (category.getParentCategoryId() == 0) {
                    // 父分类：级联删除子分类 + 清空所有关联物品的分类属性
                    dbManager.clearItemParentAndChildCategoryId(category.getId());
                    dbManager.deleteParentCategoryWithTransaction(category.getId());
                } else {
                    // 子分类：清空关联物品的子分类属性 + 删除自身
                    dbManager.clearItemChildCategoryId(category.getId());
                    dbManager.deleteCategory(category);
                }
                operationResult.postValue(true);
            } catch (Exception e) {
                e.printStackTrace();
                operationResult.postValue(false);
            }
        });
    }

    /**
     * 校验分类名称是否重复
     * @param categoryName 待校验的分类名称
     * @param parentId 父分类ID（0=校验父分类名称，>0=校验该父分类下的子分类名称）
     * @param excludeId 排除的分类ID（编辑时排除自身，新增时传-1）
     * @return true=名称重复，false=名称可用
     */
    public LiveData<Boolean> checkCategoryNameDuplicate(String categoryName, long parentId, long excludeId) {
        MutableLiveData<Boolean> isDuplicate = new MutableLiveData<>();
        AsyncTask.execute(() -> {
            boolean duplicate = dbManager.checkCategoryNameDuplicate(categoryName, parentId, excludeId);
            isDuplicate.postValue(duplicate);
        });
        return isDuplicate;
    }

    /**
     * 检查分类是否有关联物品
     */
    public LiveData<Boolean> checkCategoryHasRelatedItems(long categoryId) {
        MutableLiveData<Boolean> hasItems = new MutableLiveData<>();
        AsyncTask.execute(() -> {
            int count = dbManager.getRelatedItemCount(categoryId);
            hasItems.postValue(count > 0);
        });
        return hasItems;
    }

    /**
     * 获取父分类删除提示语
     */
    public LiveData<String> checkCategoryDeleteTip(long parentId) {
        MutableLiveData<String> tip = new MutableLiveData<>();
        AsyncTask.execute(() -> {
            int childCount = dbManager.countChildCategoriesByParentId(parentId);
            int itemCount = dbManager.getRelatedItemCount(parentId);
            if (childCount > 0 || itemCount > 0) {
                tip.postValue(getApplication().getString(R.string.delete_parent_category_with_child_or_items_tip));
            } else {
                tip.postValue(String.format(getApplication().getString(R.string.delete_category_confirm),
                        dbManager.getCategoryById(parentId).getCategoryName()));
            }
        });
        return tip;
    }

    /**
     * 检查分类是否可删除（仅校验子分类：父分类删除时会级联删子分类，子分类无此限制）
     */
    public LiveData<Boolean> checkCategoryCanDelete(long categoryId) {
        MutableLiveData<Boolean> canDelete = new MutableLiveData<>();
        AsyncTask.execute(() -> {
            int childCount = dbManager.countChildCategoriesByParentId(categoryId);
            // 移除物品数量校验：仅父分类需校验子分类（级联删除），子分类无限制
            canDelete.postValue(childCount == 0 || dbManager.getCategoryById(categoryId).getParentCategoryId() != 0);
        });
        return canDelete;
    }
}