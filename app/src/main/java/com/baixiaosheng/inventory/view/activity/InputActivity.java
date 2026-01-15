package com.baixiaosheng.inventory.view.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.view.fragment.InputFragment;

public class InputActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);

        // 获取编辑的Item参数
        Item editItem = (Item) getIntent().getSerializableExtra("edit_item");

        // 加载InputFragment（复用现有编辑逻辑，无需重复写UI）
        InputFragment inputFragment = new InputFragment();
        Bundle args = new Bundle();
        args.putSerializable("edit_item", editItem);
        inputFragment.setArguments(args);

        // 替换为InputFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_input_container, inputFragment)
                .commit();
    }
}