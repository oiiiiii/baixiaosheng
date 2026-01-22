package com.baixiaosheng.inventory.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.database.entity.Location;
import com.baixiaosheng.inventory.utils.DateUtils;
import com.baixiaosheng.inventory.utils.ImageUtils;
import com.baixiaosheng.inventory.viewmodel.QueryViewModel;

import java.util.List;

/**
 * 物品详情页：完善编辑、删除功能，增强用户体验
 */
public class ItemDetailActivity extends AppCompatActivity {

    private long itemId;
    private QueryViewModel queryViewModel;
    private Item currentItem;

    // 控件
    private TextView tvItemName, tvParentCategory, tvChildCategory, tvLocation, tvCount, tvValidTime, tvRemark;
    private LinearLayout llImagePreview;
    private Button btnBack, btnEdit, btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // 获取传递的物品ID
        itemId = getIntent().getLongExtra("item_id", 0);
        if (itemId == 0) {
            Toast.makeText(this, "物品ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化ViewModel
        queryViewModel = new ViewModelProvider(this).get(QueryViewModel.class);

        // 绑定控件
        bindViews();

        // 加载物品数据
        loadItemData();

        // 绑定事件
        bindEvents();
    }

    // 重写 onActivityResult 方法（接收InputActivity返回结果）
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // 编辑保存成功，重新加载物品数据
            loadItemData();
            Toast.makeText(this, "数据已更新", Toast.LENGTH_SHORT).show();
        }
    }

    private void bindViews() {
        tvItemName = findViewById(R.id.tv_item_name);
        tvParentCategory = findViewById(R.id.tv_parent_category);
        tvChildCategory = findViewById(R.id.tv_child_category);
        tvLocation = findViewById(R.id.tv_location);
        tvCount = findViewById(R.id.tv_count);
        tvValidTime = findViewById(R.id.tv_valid_time);
        tvRemark = findViewById(R.id.tv_remark);
        llImagePreview = findViewById(R.id.ll_image_preview);

        btnBack = findViewById(R.id.btn_back);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
    }

    private void loadItemData() {
        // 查询物品详情
        queryViewModel.getItemByIdNotDeletedLiveData(itemId).observe(this, item -> {
            if (isFinishing() || isDestroyed()) return;
            if (item == null) {
                Toast.makeText(this, "物品不存在", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            currentItem = item;

            // 填充基本信息
            tvItemName.setText(item.getName());
            tvCount.setText(String.valueOf(item.getCount()));
            tvValidTime.setText(item.getValidTime() == 0 ? "无" : DateUtils.formatTime(item.getValidTime()));
            tvRemark.setText(item.getRemark() == null ? "无" : item.getRemark());


            // 查询父分类名称
            queryViewModel.getCategoryByIdLiveData(item.getParentCategoryId()).observe(this, category -> {
                tvParentCategory.setText(category == null ? "无" : category.getCategoryName());
            });

            // 查询子分类名称
            queryViewModel.getCategoryByIdLiveData(item.getChildCategoryId()).observe(this, category -> {
                tvChildCategory.setText(category == null ? "无" : category.getCategoryName());
            });

            // 查询位置名称
            queryViewModel.getLocationByIdLiveData(item.getLocationId()).observe(this, location -> {
                tvLocation.setText(location == null ? "无" : location.getName());
            });


            // 加载图片预览
            loadItemImages(item.getImagePaths());
        });
    }

    /**
     * 加载物品图片
     * @param imagePaths 图片路径（逗号分隔）
     */
    private void loadItemImages(String imagePaths) {
        llImagePreview.removeAllViews();
        if (imagePaths == null || imagePaths.isEmpty()) {
            return;
        }

        String[] paths = imagePaths.split(",");
        for (String path : paths) {
            ImageView iv = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    200, 200, 0);
            params.setMargins(0, 0, 8, 0);
            iv.setLayoutParams(params);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // 加载图片
            ImageUtils.loadImage(path, iv);

            // 点击图片放大
            iv.setOnClickListener(v -> ImageUtils.showImagePreview(this, path));

            llImagePreview.addView(iv);
        }
    }

    private void bindEvents() {
        // 返回按钮
        btnBack.setOnClickListener(v -> finish());

        // 编辑按钮：跳转到编辑页（InputActivity）

        btnEdit.setOnClickListener(v -> {
            if (currentItem == null) {
                Toast.makeText(this, "物品数据异常，无法编辑", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, InputActivity.class);
            intent.putExtra("edit_item", currentItem);
            // 新版API用 registerForActivityResult，旧版用 startActivityForResult
            // 这里用兼容写法（API 33+推荐 registerForActivityResult）
            startActivityForResult(intent, 1001); // 1001为自定义请求码
        });

        // 删除按钮：增加确认弹窗，避免误操作
        btnDelete.setOnClickListener(v -> {
            if (currentItem == null) {
                Toast.makeText(this, "物品数据异常，无法删除", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("确认删除")
                    .setMessage("确定要将【" + currentItem.getName() + "】移入回收站吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        // 标记为已删除
                        queryViewModel.markItemAsDeleted(currentItem.getId());
                        Toast.makeText(ItemDetailActivity.this, "已移入回收站", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }
}