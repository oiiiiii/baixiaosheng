package com.baixiaosheng.inventory;

import android.app.Application;

public class App extends Application {
    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    // 获取全局应用实例
    public static App getInstance() {
        return instance;
    }
}