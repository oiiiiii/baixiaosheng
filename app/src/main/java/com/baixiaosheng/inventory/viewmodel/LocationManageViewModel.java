package com.baixiaosheng.inventory.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baixiaosheng.inventory.database.DatabaseManager;
import com.baixiaosheng.inventory.database.entity.Location;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 位置管理ViewModel
 */
public class LocationManageViewModel extends AndroidViewModel {

    private final DatabaseManager mDatabaseManager;
    private final ExecutorService mExecutorService;
    private final MutableLiveData<List<Location>> mLocationList;
    private final MutableLiveData<Boolean> mOperationSuccess;
    private final MutableLiveData<String> mErrorMsg;

    public LocationManageViewModel(@NonNull Application application) {
        super(application);
        mDatabaseManager = DatabaseManager.getInstance(application);
        mExecutorService = Executors.newSingleThreadExecutor();
        mLocationList = new MutableLiveData<>();
        mOperationSuccess = new MutableLiveData<>();
        mErrorMsg = new MutableLiveData<>();
        // 加载所有位置
        loadAllLocations();
    }

    /**
     * 加载所有位置
     */
    public void loadAllLocations() {
        mExecutorService.execute(() -> {
            List<Location> locations = mDatabaseManager.getAllLocations();
            mLocationList.postValue(locations);
        });
    }

    /**
     * 添加位置
     */
    public void addLocation(Location location) {
        mExecutorService.execute(() -> {
            try {
                // 校验位置名称非空
                if (location.getName() == null || location.getName().trim().isEmpty()) {
                    mErrorMsg.postValue("位置名称不能为空");
                    mOperationSuccess.postValue(false);
                    return;
                }
                // 校验名称重复（新增时排除ID为0）
                boolean isDuplicate = mDatabaseManager.checkLocationNameDuplicate(location.getName().trim(), 0);
                if (isDuplicate) {
                    mErrorMsg.postValue("位置名称已存在，无法重复添加");
                    mOperationSuccess.postValue(false);
                    return;
                }
                // 插入位置
                mDatabaseManager.addLocation(location);
                mOperationSuccess.postValue(true);
                // 重新加载列表
                loadAllLocations();
            } catch (Exception e) {
                mErrorMsg.postValue("添加位置失败：" + e.getMessage());
                mOperationSuccess.postValue(false);
            }
        });
    }

    /**
     * 更新位置
     */
    public void updateLocation(Location location) {
        mExecutorService.execute(() -> {
            try {
                // 校验位置名称非空
                if (location.getName() == null || location.getName().trim().isEmpty()) {
                    mErrorMsg.postValue("位置名称不能为空");
                    mOperationSuccess.postValue(false);
                    return;
                }
                // 校验名称重复（编辑时排除自身ID）
                boolean isDuplicate = mDatabaseManager.checkLocationNameDuplicate(location.getName().trim(), location.getId());
                if (isDuplicate) {
                    mErrorMsg.postValue("位置名称已存在，无法修改");
                    mOperationSuccess.postValue(false);
                    return;
                }
                // 更新位置
                mDatabaseManager.updateLocation(location);
                mOperationSuccess.postValue(true);
                // 重新加载列表
                loadAllLocations();
            } catch (Exception e) {
                mErrorMsg.postValue("更新位置失败：" + e.getMessage());
                mOperationSuccess.postValue(false);
            }
        });
    }



    /**
     * 删除位置（同时清空关联物品的位置属性）
     */
    public void deleteLocation(long locationId) {
        mExecutorService.execute(() -> {
            try {
                // 步骤1：先清空关联物品的位置属性（核心新增逻辑）
                // mDatabaseManager.clearItemLocationByLocationId(locationId);

                // 步骤2：再删除位置
                mDatabaseManager.deleteLocationById(locationId);

                mOperationSuccess.postValue(true);
                // 重新加载列表
                loadAllLocations();
            } catch (Exception e) {
                mErrorMsg.postValue("删除位置失败：" + e.getMessage());
                mOperationSuccess.postValue(false);
            }
        });
    }

    /**
     * 检查位置名称是否重复（排除编辑中的ID）
     */
    public void checkLocationNameDuplicate(String name, long excludeId, MutableLiveData<Boolean> isDuplicate) {
        mExecutorService.execute(() -> {
            boolean duplicate = mDatabaseManager.checkLocationNameDuplicate(name, excludeId);
            isDuplicate.postValue(duplicate);
        });
    }

    // 对外暴露LiveData
    public LiveData<List<Location>> getLocationList() {
        return mLocationList;
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