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
 * 主Activity（底部Tab框架核心 + 支持Fragment回退）
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
                // 清空回退栈，避免Tab切换后回退到编辑页
                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

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
     * 切换Fragment（复用实例，避免重复创建）
     * 改为public权限，允许外部Fragment调用
     */
    public void switchFragment(Fragment targetFragment) {
        if (currentFragment == targetFragment) {
            return; // 同一Fragment，无需切换
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // 隐藏当前Fragment
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }
        // 显示目标Fragment（未添加则添加）
        if (!targetFragment.isAdded()) {
            transaction.add(R.id.fragment_container, targetFragment);
        } else {
            transaction.show(targetFragment);
        }
        transaction.commit();
        // 更新当前Fragment
        currentFragment = targetFragment;
    }

    /**
     * 重载：支持带回退栈的Fragment切换（适配编辑页跳转）
     * @param targetFragment 目标Fragment
     * @param addToBackStack 是否加入回退栈
     */
    public void switchFragment(Fragment targetFragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // 隐藏当前Fragment
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }
        // 添加目标Fragment（编辑页每次新建，不复用）
        transaction.add(R.id.fragment_container, targetFragment);
        // 如果需要回退，加入回退栈
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
        // 更新当前Fragment
        currentFragment = targetFragment;
    }

    // 重写返回键，支持Fragment回退栈
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            // 有回退栈时，返回上一个Fragment
            getSupportFragmentManager().popBackStack();
        } else {
            // 无回退栈时，执行默认返回逻辑
            super.onBackPressed();
        }
    }
}