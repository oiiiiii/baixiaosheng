package com.baixiaosheng.inventory.viewmodel;

import android.app.Application;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baixiaosheng.inventory.database.InventoryDatabase;
import com.baixiaosheng.inventory.database.DatabaseManager;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.database.entity.Location;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 录入页ViewModel
 * 为什么这么写：遵循MVVM架构，将数据操作和业务逻辑从View层分离，避免内存泄漏
 */
public class InputViewModel extends AndroidViewModel {
    // 原有核心变量
    private final InventoryDatabase mDb;
    private final ExecutorService mExecutorService;
    private final MutableLiveData<Boolean> mSaveSuccess = new MutableLiveData<>();

    // 新增分类/位置相关变量
    private MutableLiveData<List<Category>> mParentCategories;
    private MutableLiveData<List<Category>> mChildCategories;
    private MutableLiveData<List<Location>> mLocations;
    private DatabaseManager mDbManager;

    public InputViewModel(@NonNull Application application) {
        super(application);
        // 初始化原有数据库和线程池
        mDb = InventoryDatabase.getInstance(application);
        mExecutorService = Executors.newSingleThreadExecutor();

        // 修复：DatabaseManager.getInstance() 需要传入Context（Application上下文）
        mDbManager = DatabaseManager.getInstance(application);

        // 初始化新增的分类/位置相关变量
        mParentCategories = new MutableLiveData<>();
        mChildCategories = new MutableLiveData<>();
        mLocations = new MutableLiveData<>();
    }

    /**
     * 原有功能：保存物品信息到数据库
     */
    public void saveItem(Item item) {
        // 移除错误的id设置：id是Room自增主键，无需手动设置
        // 若需要UUID标识，应设置uuid字段而非id字段
        item.setUuid(UUID.randomUUID().toString());

        // 补充：设置创建/更新时间（可选，根据业务需求）
        long currentTime = System.currentTimeMillis();
        item.setCreateTime(currentTime);
        item.setUpdateTime(currentTime);

        // 子线程执行数据库操作（Room不允许主线程操作）
        mExecutorService.execute(() -> {
            try {
                // 打印Item数据，排查字段异常
                Log.d("InputViewModel", "保存Item: " + item.toString());
                mDb.itemDao().insertItem(item);
                mSaveSuccess.postValue(true);
            } catch (SQLiteException e) {
                Log.e("InputViewModel", "数据库插入失败: " + e.getMessage());
                // 区分字段过长/类型错误
                if (e.getMessage().contains("too long")) {
                    Log.e("InputViewModel", "图片路径过长：" + item.getImagePaths());
                }
                mSaveSuccess.postValue(false);
            } catch (Exception e) {
                Log.e("InputViewModel", "保存失败: " + e.getMessage());
                mSaveSuccess.postValue(false);
                e.printStackTrace();
            }
        });
    }

    /**
     * 原有功能：获取保存结果的LiveData
     */
    public LiveData<Boolean> getSaveSuccess() {
        return mSaveSuccess;
    }

    // 新增：父分类LiveData（只读）
    public LiveData<List<Category>> getParentCategories() {
        return mParentCategories;
    }

    // 新增：子分类LiveData（只读）
    public LiveData<List<Category>> getChildCategories() {
        return mChildCategories;
    }

    // 新增：位置LiveData（只读）
    public LiveData<List<Location>> getLocations() {
        return mLocations;
    }

    // 新增：加载父分类（parentId=0的分类）
    public void loadParentCategories() {
        mExecutorService.execute(() -> {
            try {
                List<Category> parentCats = mDbManager.getParentCategories(0);
                mParentCategories.postValue(parentCats);
            } catch (Exception e) {
                Log.e("InputViewModel", "加载父分类失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // 新增：加载指定父分类下的子分类
    public void loadChildCategories(long parentId) {
        mExecutorService.execute(() -> {
            try {
                List<Category> childCats = mDbManager.getChildCategories(parentId);
                mChildCategories.postValue(childCats);
            } catch (Exception e) {
                Log.e("InputViewModel", "加载子分类失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // 新增：加载所有位置
    public void loadLocations() {
        mExecutorService.execute(() -> {
            try {
                List<Location> locations = mDbManager.getAllLocations();
                mLocations.postValue(locations);
            } catch (Exception e) {
                Log.e("InputViewModel", "加载位置失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 原有功能：释放线程池
        mExecutorService.shutdown();
    }
}