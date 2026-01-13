package com.baixiaosheng.inventory.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baixiaosheng.inventory.database.InventoryDatabase;
import com.baixiaosheng.inventory.database.dao.LocationDao;
import com.baixiaosheng.inventory.database.entity.Location;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 位置管理ViewModel
 */
public class LocationManageViewModel extends AndroidViewModel {

    private final LocationDao mLocationDao;
    private final ExecutorService mExecutorService;
    private final MutableLiveData<List<Location>> mLocationList;
    private final MutableLiveData<Boolean> mOperationSuccess;
    private final MutableLiveData<String> mErrorMsg;

    public LocationManageViewModel(@NonNull Application application) {
        super(application);
        InventoryDatabase database = InventoryDatabase.getInstance(application);
        mLocationDao = database.locationDao();
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
            List<Location> locations = mLocationDao.getAllLocations();
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
                // 插入位置
                mLocationDao.insertLocation(location);
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
                // 更新位置
                mLocationDao.updateLocation(location);
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
     * 删除位置
     */
    public void deleteLocation(long locationId) {
        mExecutorService.execute(() -> {
            try {
                // 校验是否有关联物品
                int itemCount = mLocationDao.getRelatedItemCount(locationId);
                if (itemCount > 0) {
                    mErrorMsg.postValue("该位置关联" + itemCount + "个物品，无法删除");
                    mOperationSuccess.postValue(false);
                    return;
                }
                // 执行删除
                mLocationDao.deleteLocationById(locationId);
                mOperationSuccess.postValue(true);
                // 重新加载列表
                loadAllLocations();
            } catch (Exception e) {
                mErrorMsg.postValue("删除位置失败：" + e.getMessage());
                mOperationSuccess.postValue(false);
            }
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