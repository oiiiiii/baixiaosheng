package com.baixiaosheng.inventory.view.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
        btnCheckUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("https://52pojie.cn");
            }
        });

        // 使用说明按钮点击（预留接口）
        btnUsageGuide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 替换为实际使用说明URL
                openUrl("https://example.com/usage-guide");
            }
        });
    }
    // 打开指定URL
    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        // 适配Android 11+包可见性
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "未找到浏览器应用", Toast.LENGTH_SHORT).show();
        }
    }
}