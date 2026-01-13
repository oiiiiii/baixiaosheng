package com.baixiaosheng.inventory.view.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.baixiaosheng.inventory.R;

/**
 * 关于页面
 */
public class AboutActivity extends AppCompatActivity {

    private Button btnCheckUpdate;
    private Button btnUsageGuide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        initView();
        initListener();
    }

    private void initView() {
        btnCheckUpdate = findViewById(R.id.btn_check_update);
        btnUsageGuide = findViewById(R.id.btn_usage_guide);
    }

    private void initListener() {
        // 检查更新按钮点击（预留接口）
        btnCheckUpdate.setOnClickListener(v -> {
            Toast.makeText(this, "当前已是最新版本", Toast.LENGTH_SHORT).show();
        });

        // 使用说明按钮点击（预留接口）
        btnUsageGuide.setOnClickListener(v -> {
            Toast.makeText(this, "使用说明：\n1. 首页可查看过期物品\n2. 录入页可添加新物品\n3. 查询页可筛选物品\n4. 设置页可管理分类/位置/回收站", Toast.LENGTH_LONG).show();
        });
    }
}