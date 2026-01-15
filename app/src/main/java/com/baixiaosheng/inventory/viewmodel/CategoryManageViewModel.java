package com.baixiaosheng.inventory.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baixiaosheng.inventory.database.InventoryDatabase;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.dao.CategoryDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 分类管理ViewModel
 */
public class CategoryManageViewModel extends AndroidViewModel {

    private final CategoryDao mCategoryDao;
    private final ExecutorService mExecutorService;
    private final MutableLiveData<List<Category>> mCategoryList;
    private final MutableLiveData<Boolean> mOperationSuccess;
    private final MutableLiveData<String> mErrorMsg;
    private final MutableLiveData<List<Category>> mParentCategoryListLiveData;

    public CategoryManageViewModel(@NonNull Application application) {
        super(application);
        InventoryDatabase database = InventoryDatabase.getInstance(application);
        mCategoryDao = database.categoryDao();
        mExecutorService = Executors.newSingleThreadExecutor();
        mCategoryList = new MutableLiveData<>();
        mOperationSuccess = new MutableLiveData<>();
        mErrorMsg = new MutableLiveData<>();
        mParentCategoryListLiveData = new MutableLiveData<>();
        // 加载所有分类
        loadAllCategories();
        // 初始化时加载父分类
        refreshParentCategories();
    }

    /**
     * 加载所有分类
     */
    public void loadAllCategories() {
        mExecutorService.execute(() -> {
            List<Category> categories = mCategoryDao.getAllCategories();
            mCategoryList.postValue(categories);
        });
    }

    /**
     * 添加分类
     */
    public void addCategory(Category category) {
        mExecutorService.execute(() -> {
            try {
                // 校验分类名称非空
                if (category.getName() == null || category.getName().trim().isEmpty()) {
                    mErrorMsg.postValue("分类名称不能为空");
                    mOperationSuccess.postValue(false);
                    return;
                }
                // 校验父分类是否存在（如果有父分类）
                if (category.getParentId() != 0) {
                    Category parent = mCategoryDao.getCategoryById(category.getParentId());
                    if (parent == null) {
                        mErrorMsg.postValue("父分类不存在");
                        mOperationSuccess.postValue(false);
                        return;
                    }
                }
                // 插入分类
                mCategoryDao.insertCategory(category);
                mOperationSuccess.postValue(true);
                // 重新加载列表
                loadAllCategories();
                // 新增：刷新父分类列表
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
                // 校验分类名称非空
                if (category.getName() == null || category.getName().trim().isEmpty()) {
                    mErrorMsg.postValue("分类名称不能为空");
                    mOperationSuccess.postValue(false);
                    return;
                }
                // 校验父分类是否存在（如果有父分类）
                if (category.getParentId() != 0) {
                    Category parent = mCategoryDao.getCategoryById(category.getParentId());
                    if (parent == null) {
                        mErrorMsg.postValue("父分类不存在");
                        mOperationSuccess.postValue(false);
                        return;
                    }
                }
                // 级联校验：如果当前分类是父分类，不允许删除（此处是更新，仅提示）
                List<Category> childCategories = mCategoryDao.getChildCategoriesByParentId(category.getId());
                if (!childCategories.isEmpty()) {
                    mErrorMsg.postValue("该分类下存在子分类，更新前请确认子分类关联关系");
                }
                // 更新分类
                mCategoryDao.updateCategory(category);
                mOperationSuccess.postValue(true);
                // 重新加载列表
                loadAllCategories();
                refreshParentCategories();
            } catch (Exception e) {
                mErrorMsg.postValue("更新分类失败：" + e.getMessage());
                mOperationSuccess.postValue(false);
            }
        });

    }

    /**
     * 删除分类
     */
    public void deleteCategory(long categoryId) {
        mExecutorService.execute(() -> {
            try {
                // 1. 校验是否有关联物品
                int itemCount = mCategoryDao.getRelatedItemCount(categoryId);
                if (itemCount > 0) {
                    mErrorMsg.postValue("该分类关联" + itemCount + "个物品，无法删除");
                    mOperationSuccess.postValue(false);
                    return;
                }
                // 2. 级联校验：是否有子分类
                List<Category> childCategories = mCategoryDao.getChildCategoriesByParentId(categoryId);
                if (!childCategories.isEmpty()) {
                    mErrorMsg.postValue("该分类下存在" + childCategories.size() + "个子分类，无法删除");
                    mOperationSuccess.postValue(false);
                    return;
                }
                // 3. 执行删除
// 3. 执行删除
                int deleteCount = mCategoryDao.deleteCategoryById(categoryId);
                if (deleteCount == 0) {
                    mErrorMsg.postValue("分类不存在，删除失败");
                    mOperationSuccess.postValue(false);
                    return;
                }
                mOperationSuccess.postValue(true);
                // 重新加载列表
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


    public LiveData<List<Category>> getParentCategories(long parentId) {
        // 忽略parentId参数（原逻辑是加载所有可作为父分类的项），直接返回成员变量
        return mParentCategoryListLiveData;
    }


    /**
     * 刷新父分类列表（供Activity调用）
     */
    public void refreshParentCategories() {
        mExecutorService.execute(() -> {
            List<Category> parentCategories = mCategoryDao.getParentCategories(0);
            // 注意：需要新增一个MutableLiveData专门存储父分类列表
            mParentCategoryListLiveData.postValue(parentCategories);
        });
    }


    public LiveData<Integer> checkItemCountByCategoryId(long categoryId) {
        MutableLiveData<Integer> liveData = new MutableLiveData<>();
        mExecutorService.execute(() -> {
            int count = mCategoryDao.getRelatedItemCount(categoryId);
            liveData.postValue(count);
        });
        return liveData;
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
        // 关闭线程池
        mExecutorService.shutdown();
    }
}