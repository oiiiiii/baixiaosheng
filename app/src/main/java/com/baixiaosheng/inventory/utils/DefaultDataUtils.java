package com.baixiaosheng.inventory.utils;

import android.content.Context;
import android.os.AsyncTask;

import com.baixiaosheng.inventory.database.DatabaseManager;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.entity.Location;

import java.util.List;

/**
 * 默认数据初始化工具类
 * 预置分类、位置的默认数据，避免重复插入
 */
public class DefaultDataUtils {

    /**
     * 初始化默认分类数据
     * @param context 上下文
     */
    public static void initDefaultCategories(Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                DatabaseManager dbManager = DatabaseManager.getInstance(context);

                // 优化：用精确查询（名称+父ID）判断是否已存在默认父分类，避免模糊查询误判
                List<Category> defaultCategories = dbManager.getCategoryByNameAndParentId("办公用品", 0);
                if (!defaultCategories.isEmpty()) {
                    return null;
                }

                // 1. 父分类：办公用品
                Category officeParent = new Category();
                officeParent.setParentId(0);
                officeParent.setName("办公用品");
                officeParent.setCreateTime(System.currentTimeMillis());
                officeParent.setUpdateTime(System.currentTimeMillis());
                // 修复：调用正确的addCategory方法（原代码调用了不存在的insert）
                long officeParentId = dbManager.addCategory(officeParent);

                // 子分类：文具
                Category stationery = new Category();
                stationery.setParentId(officeParentId);
                stationery.setName("文具");
                stationery.setCreateTime(System.currentTimeMillis());
                stationery.setUpdateTime(System.currentTimeMillis());
                dbManager.addCategory(stationery);

                // 子分类：设备
                Category equipment = new Category();
                equipment.setParentId(officeParentId);
                equipment.setName("设备");
                equipment.setCreateTime(System.currentTimeMillis());
                equipment.setUpdateTime(System.currentTimeMillis());
                dbManager.addCategory(equipment);

                // 2. 父分类：生活用品
                Category lifeParent = new Category();
                lifeParent.setParentId(0);
                lifeParent.setName("生活用品");
                lifeParent.setCreateTime(System.currentTimeMillis());
                lifeParent.setUpdateTime(System.currentTimeMillis());
                long lifeParentId = dbManager.addCategory(lifeParent);

                // 子分类：洗漱用品
                Category wash = new Category();
                wash.setParentId(lifeParentId);
                wash.setName("洗漱用品");
                wash.setCreateTime(System.currentTimeMillis());
                wash.setUpdateTime(System.currentTimeMillis());
                dbManager.addCategory(wash);

                // 子分类：食品
                Category food = new Category();
                food.setParentId(lifeParentId);
                food.setName("食品");
                food.setCreateTime(System.currentTimeMillis());
                food.setUpdateTime(System.currentTimeMillis());
                dbManager.addCategory(food);

                return null;
            }
        }.execute();
    }

    /**
     * 初始化默认位置数据
     * @param context 上下文
     */
    public static void initDefaultLocations(Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                DatabaseManager dbManager = DatabaseManager.getInstance(context);

                // 检查是否已有默认位置
                List<Location> defaultLocations = dbManager.getLocationByName("办公室1号柜");
                if (!defaultLocations.isEmpty()) {
                    return null;
                }

                // 位置1：办公室1号柜
                Location loc1 = new Location();
                loc1.setName("办公室1号柜");
                loc1.setRemark("左侧上层");
                loc1.setCreateTime(System.currentTimeMillis());
                loc1.setUpdateTime(System.currentTimeMillis());
                // 修复：调用正确的addLocation方法（原代码调用了不存在的insert）
                dbManager.addLocation(loc1);

                // 位置2：仓库A区
                Location loc2 = new Location();
                loc2.setName("仓库A区");
                loc2.setRemark("货架3层");
                loc2.setCreateTime(System.currentTimeMillis());
                loc2.setUpdateTime(System.currentTimeMillis());
                dbManager.addLocation(loc2);

                // 位置3：家用储物柜
                Location loc3 = new Location();
                loc3.setName("家用储物柜");
                loc3.setRemark("客厅右侧");
                loc3.setCreateTime(System.currentTimeMillis());
                loc3.setUpdateTime(System.currentTimeMillis());
                dbManager.addLocation(loc3);

                return null;
            }
        }.execute();
    }

    /**
     * 初始化所有默认数据
     * @param context 上下文
     */
    public static void initAllDefaultData(Context context) {
        initDefaultCategories(context);
        initDefaultLocations(context);
    }
}