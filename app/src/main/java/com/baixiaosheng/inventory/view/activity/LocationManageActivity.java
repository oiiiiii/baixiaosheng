package com.baixiaosheng.inventory.view.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.view.adapter.LocationAdapter;
import com.baixiaosheng.inventory.database.entity.Location;
import com.baixiaosheng.inventory.viewmodel.LocationManageViewModel;

import java.util.List;

/**
 * 位置管理页面
 */
public class LocationManageActivity extends AppCompatActivity {

    private RecyclerView rvLocationList;
    private Button btnAddNewLocation;

    private LocationManageViewModel mViewModel;
    private LocationAdapter mAdapter;
    private long mEditingLocationId = 0; // 编辑中的位置ID，0表示新增

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_manage);
        initView();
        initViewModel();
        initListener();
    }

    private void initView() {
        rvLocationList = findViewById(R.id.rv_location_list);
        btnAddNewLocation = findViewById(R.id.btn_add_new_location);
        rvLocationList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initViewModel() {
        mViewModel = new ViewModelProvider(this).get(LocationManageViewModel.class);

        // 观察位置列表
        mViewModel.getLocationList().observe(this, locations -> {
            if (mAdapter == null) {
                mAdapter = new LocationAdapter(locations, this::onItemClick, this::onItemLongClick);
                rvLocationList.setAdapter(mAdapter);
            } else {
                mAdapter.updateData(locations);
            }
        });

        // 观察操作结果
        mViewModel.getOperationSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, mEditingLocationId == 0 ? "添加成功" : "更新成功", Toast.LENGTH_SHORT).show();
            }
        });

        // 观察错误信息
        mViewModel.getErrorMsg().observe(this, errorMsg -> {
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        });
    }

    private void initListener() {
        // 底部添加按钮点击事件
        btnAddNewLocation.setOnClickListener(v -> {
            mEditingLocationId = 0; // 重置为新增状态
            showLocationInputDialog("添加位置", "");
        });
    }

    /**
     * 列表项点击事件（编辑）
     */
    private void onItemClick(Location location) {
        mEditingLocationId = location.getId();
        showLocationInputDialog("编辑位置", location.getName());
    }



    /**
     * 列表项长按事件（删除）
     */
    private void onItemLongClick(Location location) {
        // 弹出确认删除对话框
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                // 替换提示文案，明确告知删除后果
                .setMessage("该分类下存在关联物品，删除会清空物品位置属性，是否确定删除？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 确认后执行删除
                    mViewModel.deleteLocation(location.getId());
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * 显示位置输入弹窗
     */
    private void showLocationInputDialog(String title, String defaultName) {
        // 构建弹窗布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_location_input, null);
        EditText etLocationName = dialogView.findViewById(R.id.et_dialog_location_name);
        etLocationName.setText(defaultName);
        if (defaultName.length() > 0) {
            etLocationName.setSelection(defaultName.length());
        }

        // 构建弹窗
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("确认", null) // 先设为null，后续重写点击事件
                .setNegativeButton("取消", (d, which) -> d.dismiss())
                .create();

        // 重写确认按钮点击事件（校验名称）
        dialog.setOnShowListener(d -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> {
                String locationName = etLocationName.getText().toString().trim();
                // 1. 校验非空
                if (locationName.isEmpty()) {
                    Toast.makeText(this, "位置名称不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 2. 校验名称是否重复
                if (mAdapter.isNameDuplicate(locationName, mEditingLocationId)) {
                    Toast.makeText(this, "位置名称已存在，请修改", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 3. 执行添加/更新
                Location location = new Location();
                location.setName(locationName);
                if (mEditingLocationId == 0) {
                    // 新增
                    mViewModel.addLocation(location);
                } else {
                    // 更新
                    location.setId(mEditingLocationId);
                    mViewModel.updateLocation(location);
                }
                dialog.dismiss();
            });
        });

        dialog.show();
    }
}