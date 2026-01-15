package com.baixiaosheng.inventory.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.baixiaosheng.inventory.database.dao.CategoryDao;
import com.baixiaosheng.inventory.database.dao.ItemDao;
import com.baixiaosheng.inventory.database.dao.LocationDao;
import com.baixiaosheng.inventory.database.dao.RecycleDao;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.database.entity.Location;
import com.baixiaosheng.inventory.database.entity.Recycle;

/**
 * Room数据库核心类（单例模式）
 * 版本号：1（后续升级需修改版本号并编写迁移脚本）
 * 包含4张表：Category、Location、Item、Recycle
 */
@Database(
        entities = {Category.class, Location.class, Item.class, Recycle.class},
        version = 1,
        exportSchema = false // 国内环境关闭Schema导出，避免报错
)
public abstract class InventoryDatabase extends RoomDatabase {
    // 数据库名称
    private static final String DATABASE_NAME = "baixiaosheng_inventory.db";
    // 单例实例
    private static volatile InventoryDatabase INSTANCE;

    // 获取Dao接口实例（Room自动实现）
    public abstract CategoryDao categoryDao();
    public abstract LocationDao locationDao();
    public abstract ItemDao itemDao();
    public abstract RecycleDao recycleDao();

    // 单例获取方法（线程安全）
    public static InventoryDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (InventoryDatabase.class) {
                if (INSTANCE == null) {
                    // 创建数据库实例（allowMainThreadQueries：测试阶段允许主线程操作，后续优化为异步）
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    InventoryDatabase.class,
                                    DATABASE_NAME
                            )
                            // .allowMainThreadQueries() 注意：正式环境需替换为异步操作，此处为测试方便
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // 销毁实例（可选，如退出应用时调用）
    public static void destroyInstance() {
        INSTANCE = null;
    }
}