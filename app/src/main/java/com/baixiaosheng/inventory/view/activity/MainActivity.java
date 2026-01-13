package com.baixiaosheng.inventory.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.MenuItem;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.view.fragment.HomeFragment;
import com.baixiaosheng.inventory.view.fragment.InputFragment;
import com.baixiaosheng.inventory.view.fragment.QueryFragment;
import com.baixiaosheng.inventory.view.fragment.SettingFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * 主Activity（底部Tab框架核心）
 * 实现BottomNavigationView与4个Fragment的切换逻辑
 */
public class MainActivity extends AppCompatActivity {

    // Fragment对象（缓存，避免重复创建）
    private HomeFragment homeFragment;
    private InputFragment inputFragment;
    private QueryFragment queryFragment;
    private SettingFragment settingFragment;
    // 当前显示的Fragment
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化Fragment（懒加载：首次使用时创建）
        initFragments();
        // 初始化底部Tab栏
        initBottomNavigation();
        // 默认显示首页Fragment
        switchFragment(homeFragment);
    }

    /**
     * 初始化Fragment对象（缓存，避免切换时重复创建）
     */
    private void initFragments() {
        homeFragment = new HomeFragment();
        inputFragment = new InputFragment();
        queryFragment = new QueryFragment();
        settingFragment = new SettingFragment();
        // 初始当前Fragment为首页
        currentFragment = homeFragment;
    }

    /**
     * 初始化底部Tab栏，设置点击事件
     */
    private void initBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // 根据点击的Tab切换对应的Fragment
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    switchFragment(homeFragment);
                    return true;
                } else if (itemId == R.id.nav_input) {
                    switchFragment(inputFragment);
                    return true;
                } else if (itemId == R.id.nav_query) {
                    switchFragment(queryFragment);
                    return true;
                } else if (itemId == R.id.nav_setting) {
                    switchFragment(settingFragment);
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Fragment切换核心逻辑（保证切换不重建、数据不丢失）
     * @param targetFragment 要切换的目标Fragment
     */
    private void switchFragment(Fragment targetFragment) {
        // 如果点击的是当前显示的Fragment，直接返回
        if (currentFragment == targetFragment) {
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // 1. 如果是首次切换（当前Fragment未添加），直接添加目标Fragment
        if (!currentFragment.isAdded()) {
            transaction.add(R.id.fragment_container, targetFragment).commit();
        } else {
            // 2. 如果目标Fragment未添加到容器中，先添加
            if (!targetFragment.isAdded()) {
                transaction.hide(currentFragment).add(R.id.fragment_container, targetFragment).commit();
            } else {
                // 3. 如果目标Fragment已添加，直接显示，隐藏当前Fragment
                transaction.hide(currentFragment).show(targetFragment).commit();
            }
        }

        // 更新当前Fragment为目标Fragment
        currentFragment = targetFragment;
    }
}