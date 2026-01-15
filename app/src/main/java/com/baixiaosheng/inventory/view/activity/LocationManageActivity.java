package com.baixiaosheng.inventory.view.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
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

    private EditText etLocationName;
    private Button btnAddLocation;
    private RecyclerView rvLocationList;

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
        etLocationName = findViewById(R.id.et_location_name);
        btnAddLocation = findViewById(R.id.btn_add_location);
        rvLocationList = findViewById(R.id.rv_location_list);
        rvLocationList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initViewModel() {
        mViewModel = new ViewModelProvider(this).get(LocationManageViewModel.class);

        // 观察位置列表
        mViewModel.getLocationList().observe(this, locations -> {
            if (mAdapter == null) {
                mAdapter = new LocationAdapter(locations, this::onEditClick, this::onDeleteClick);
                rvLocationList.setAdapter(mAdapter);
            } else {
                mAdapter.updateData(locations);
            }
        });

        // 观察操作结果
        mViewModel.getOperationSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, mEditingLocationId == 0 ? "添加成功" : "更新成功", Toast.LENGTH_SHORT).show();
                resetEditState();
            }
        });

        // 观察错误信息
        mViewModel.getErrorMsg().observe(this, errorMsg -> {
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        });
    }

    private void initListener() {
        // 添加/更新位置按钮点击事件
        btnAddLocation.setOnClickListener(v -> {
            String locationName = etLocationName.getText().toString().trim();
            if (locationName.isEmpty()) {
                Toast.makeText(this, "请输入位置名称", Toast.LENGTH_SHORT).show();
                return;
            }
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
        });
    }

    /**
     * 编辑位置点击事件
     */
    private void onEditClick(Location location) {
        mEditingLocationId = location.getId();
        etLocationName.setText(location.getName());
        btnAddLocation.setText("更新");
    }


    /**
     * 删除位置点击事件
     */
    private void onDeleteClick(Location location) {
        // 弹出确认删除对话框
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("是否确定删除该位置？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 确认后执行删除
                    mViewModel.deleteLocation(location.getId());
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    // 取消则关闭对话框
                    dialog.dismiss();
                })
                .show();
    }
    /**
     * 重置编辑状态
     */
    private void resetEditState() {
        etLocationName.setText("");
        mEditingLocationId = 0;
        btnAddLocation.setText("添加");
    }
}