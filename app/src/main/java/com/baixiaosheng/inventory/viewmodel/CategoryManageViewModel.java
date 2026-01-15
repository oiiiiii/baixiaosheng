package com.baixiaosheng.inventory.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baixiaosheng.inventory.database.DatabaseManager;
import com.baixiaosheng.inventory.database.entity.Category;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 分类管理ViewModel（简化删除逻辑版）
 */
public class CategoryManageViewModel extends AndroidViewModel {

    private final DatabaseManager mDbManager;
    private final ExecutorService mExecutorService;
    private final MutableLiveData<List<Category>> mCategoryList;
    private final MutableLiveData<Boolean> mOperationSuccess;
    private final MutableLiveData<String> mErrorMsg;
    private final MutableLiveData<List<Category>> mParentCategoryListLiveData;

    public CategoryManageViewModel(@NonNull Application application) {
        super(application);
        mDbManager = DatabaseManager.getInstance(application.getApplicationContext());
        mExecutorService = Executors.newSingleThreadExecutor();
        mCategoryList = new MutableLiveData<>();
        mOperationSuccess = new MutableLiveData<>();
        mErrorMsg = new MutableLiveData<>();
        mParentCategoryListLiveData = new MutableLiveData<>();
        loadAllCategories();
        refreshParentCategories();
    }

    /**
     * 加载所有分类
     */
    public void loadAllCategories() {
        mExecutorService.execute(() -> {
            List<Category> categories = mDbManager.listAllCategories();
            mCategoryList.postValue(categories);
        });
    }

    /**
     * 添加分类
     */
    public void addCategory(Category category) {
        mExecutorService.execute(() -> {
            try {
                if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
                    mErrorMsg.postValue("分类名称不能为空");
                    mOperationSuccess.postValue(false);
                    return;
                }
                if (category.getParentCategoryId() != 0) {
                    Category parent = mDbManager.getCategoryById(category.getParentCategoryId());
                    if (parent == null) {
                        mErrorMsg.postValue("父分类不存在");
                        mOperationSuccess.postValue(false);
                        return;
                    }
                }
                mDbManager.addCategory(category);
                mOperationSuccess.postValue(true);
                loadAllCategories();
                refreshParentCategories();
            } catch (Exception e) {
                mErrorMsg.postValue("添加分类失败：" + e.getMessage());
                mOperationSuccess.postValue(false);
            }
        });
    }

    /**
     * 更新分类
     */
    public void updateCategory(Category category) {
        mExecutorService.execute(() -> {
            try {
                if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
                    mErrorMsg.postValue("分类名称不能为空");
                    mOperationSuccess.postValue(false);
                    return;
                }
                mDbManager.updateCategory(category);
                mOperationSuccess.postValue(true);
                loadAllCategories();
                refreshParentCategories();
            } catch (Exception e) {
                mErrorMsg.postValue("更新分类失败：" + e.getMessage());
                mOperationSuccess.postValue(false);
            }
        });
    }

    /**
     * 直接删除分类（核心简化）
     */
    public void deleteCategory(long categoryId) {
        mExecutorService.execute(() -> {
            try {
                Category targetCategory = mDbManager.getCategoryById(categoryId);
                if (targetCategory == null) {
                    mErrorMsg.postValue("分类不存在，无法删除");
                    mOperationSuccess.postValue(false);
                    return;
                }

                // 处理分类关联的物品：清空物品的分类关联
                if (targetCategory.getParentCategoryId() == 0) {
                    // 父分类：清空物品父分类ID + 所有子分类关联的物品子分类ID
                    mDbManager.clearItemCategoryByParentId(categoryId);
                    List<Category> childCategories = mDbManager.listChildCategoriesByParentId(categoryId);
                    for (Category child : childCategories) {
                        mDbManager.clearItemCategoryByChildId(child.getId());
                        mDbManager.deleteCategoryById(child.getId());
                    }
                } else {
                    // 子分类：仅清空物品子分类ID
                    mDbManager.clearItemCategoryByChildId(categoryId);
                }

                // 删除目标分类
                mDbManager.deleteCategoryById(categoryId);
                mOperationSuccess.postValue(true);
                loadAllCategories();
                refreshParentCategories();
            } catch (Exception e) {
                mErrorMsg.postValue("删除分类失败：" + e.getMessage());
                mOperationSuccess.postValue(false);
            }
        });
    }

    /**
     * 获取所有父分类（用于Spinner选择）
     */
    public LiveData<List<Category>> getParentCategories() {
        return mParentCategoryListLiveData;
    }

    /**
     * 刷新父分类列表
     */
    public void refreshParentCategories() {
        mExecutorService.execute(() -> {
            List<Category> parentCategories = mDbManager.listTopLevelParentCategories();
            mParentCategoryListLiveData.postValue(parentCategories);
        });
    }

    // 对外暴露LiveData
    public LiveData<List<Category>> getCategoryList() {
        return mCategoryList;
    }

    public LiveData<Boolean> getOperationSuccess() {
        return mOperationSuccess;
    }

    public LiveData<String> getErrorMsg() {
        return mErrorMsg;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mExecutorService.shutdown();
    }
}