package com.baixiaosheng.inventory.view.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.utils.ItemDialogUtils;
import com.baixiaosheng.inventory.view.adapter.ExpiredItemAdapter;
import com.baixiaosheng.inventory.viewmodel.HomeViewModel;

import java.util.List;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private ExpiredItemAdapter itemAdapter;
    private Long startDate;
    private Long endDate;
    private RecyclerView rvExpiredItems;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initView(view);
        initViewModel();
        //initAdapter();
        return view;
    }

    private void initView(View view) {
        rvExpiredItems = view.findViewById(R.id.rv_home_expired_item);
        rvExpiredItems.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void initViewModel() {
        // 1. 调用无参构造创建ViewModel实例（匹配构造器定义）
        homeViewModel = new HomeViewModel();
        // 2. 调用init方法初始化DatabaseManager（传入Application Context）
        homeViewModel.init(getActivity().getApplication());

        // 3. 修正观察LiveData的调用（原代码传参版本不存在，改为调用无参getExpiredItems()）
        homeViewModel.getExpiredItems().observe(getViewLifecycleOwner(), new Observer<List<Item>>() {
            @Override
            public void onChanged(List<Item> items) {
                itemAdapter.updateData(items);
                // 初始化时主动加载一次数据（使用默认时间范围）
                homeViewModel.loadExpiredItems(startDate, endDate);
            }
        });
    }

//    private void initAdapter() {
//        itemAdapter = new ExpiredItemAdapter();
//        rvExpiredItems.setAdapter(itemAdapter);

        // 设置列表项点击事件
//        itemAdapter.setOnItemClickListener(new ExpiredItemAdapter.OnItemClickListener() {
//            @Override
//            public void onItemClick(Item item) {
//                // 复用物品操作弹窗
//                ItemDialogUtils.showItemOperateDialog(getContext(), item, new ItemDialogUtils.OnItemOperateListener() {
//                    @Override
//                    public void onViewDetail(Item item) {
//                        // 跳转详情页
//                        Intent intent = new Intent(getContext(), ItemDetailActivity.class);
//                        intent.putExtra("item_id", item.getId());
//                        // 传递查询条件（如果需要返回后保留筛选状态，可补充）
//                        startActivity(intent);
//                    }
//
//                    @Override
//                    public void onEdit(Item item) {
//                        // 跳转录入页并回填数据
//                        Intent intent = new Intent(getContext(), InputActivity.class);
//                        intent.putExtra("item_id", item.getId());
//                        startActivity(intent);
//                    }
//
//                    @Override
//                    public void onDelete(Item item) {
//                        // 标记为回收站（复用SettingFragment的删除逻辑）
//                        homeViewModel.markItemAsDeleted(item.getId());
//                        Toast.makeText(getContext(), "已移入回收站", Toast.LENGTH_SHORT).show();
//                        // 刷新列表
//                        homeViewModel.refreshExpiredItems(startDate, endDate);
//                    }
//                });
//            }
//        });
//    }

    // 刷新数据（供外部调用）
//    public void refreshData(Long startDate, Long endDate) {
//        this.startDate = startDate;
//        this.endDate = endDate;
//        homeViewModel.refreshExpiredItems(startDate, endDate);
//    }
}