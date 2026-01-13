package com.baixiaosheng.inventory.view.fragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.baixiaosheng.inventory.R;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.utils.ImageUtils;
import com.baixiaosheng.inventory.utils.PermissionUtils;
import com.baixiaosheng.inventory.viewmodel.InputViewModel;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 录入页Fragment（修复所有闪退问题：类型转换、空指针、资源缺失、权限适配）
 */
public class InputFragment extends Fragment {
    // 请求码
    private static final int REQUEST_TAKE_PHOTO = 101;
    private static final int REQUEST_CHOOSE_PHOTO = 102;
    private static final int PERMISSION_REQUEST_CODE = 103;

    // 视图控件（修复类型转换错误）
    private EditText etName, etExpireDate, etQuantity, etDescription;
    private Spinner spParentCategory, spChildCategory, spLocation;
    private LinearLayout llImagePreview;
    private ImageView ivAddImage; // 修正为ImageView，匹配XML中的ivAddImage
    private Button btnSave;

    // 图片相关
    private File mPhotoFile;
    private final List<String> mImagePaths = new ArrayList<>();
    private InputViewModel mInputViewModel;

    // 日期格式化
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input, container, false);
        initView(view);
        initViewModel();
        initListener();
        initSpinnerData(); // 初始化Spinner默认数据，避免空指针
        return view;
    }

    /**
     * 初始化视图控件（修复所有ID引用和类型转换问题）
     */
    private void initView(View view) {
        // 基础表单控件
        etName = view.findViewById(R.id.et_name);
        etExpireDate = view.findViewById(R.id.et_expire_date);
        etQuantity = view.findViewById(R.id.et_quantity);
        etDescription = view.findViewById(R.id.et_description);

        // Spinner控件
        spParentCategory = view.findViewById(R.id.sp_parent_category);
        spChildCategory = view.findViewById(R.id.sp_child_category);
        spLocation = view.findViewById(R.id.sp_location);

        // 图片相关控件（核心修复：ivAddImage改为ImageView）
        llImagePreview = view.findViewById(R.id.ll_image_preview); // 统一使用ll_image_preview
        ivAddImage = view.findViewById(R.id.ivAddImage); // 正确匹配XML中的ImageView

        // 保存按钮
        btnSave = view.findViewById(R.id.btn_save);
    }

    /**
     * 初始化ViewModel
     */
    private void initViewModel() {
        mInputViewModel = new ViewModelProvider(this).get(InputViewModel.class);
        // 观察保存结果
        mInputViewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(getContext(), "保存成功", Toast.LENGTH_SHORT).show();
                clearForm();
            } else {
                Toast.makeText(getContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 初始化Spinner默认数据（解决空指针问题）
     */
    private void initSpinnerData() {
        // 父分类默认数据
        List<String> parentCats = new ArrayList<>();
        parentCats.add("未分类");
        ArrayAdapter<String> parentAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, parentCats);
        parentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spParentCategory.setAdapter(parentAdapter);

        // 子分类默认数据
        List<String> childCats = new ArrayList<>();
        childCats.add("无");
        ArrayAdapter<String> childAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, childCats);
        childAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spChildCategory.setAdapter(childAdapter);

        // 位置默认数据
        List<String> locations = new ArrayList<>();
        locations.add("未指定");
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, locations);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLocation.setAdapter(locationAdapter);
    }

    /**
     * 初始化事件监听（修复所有空指针和权限问题）
     */
    private void initListener() {
        // 有效期选择
        etExpireDate.setOnClickListener(v -> showDatePicker());

        // 添加图片（拍照/选图）
        ivAddImage.setOnClickListener(v -> showImageChooseDialog());

        // 保存按钮
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    /**
     * 显示日期选择器
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(Locale.CHINA, "%d-%02d-%02d",
                            year, month + 1, dayOfMonth);
                    etExpireDate.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    /**
     * 显示图片选择弹窗（拍照/从相册选择）
     */
    private void showImageChooseDialog() {
        String[] options = {"拍照", "从相册选择"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("选择图片来源")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        takePhoto(); // 拍照
                    } else {
                        choosePhoto(); // 选图
                    }
                })
                .show();
    }

    /**
     * 拍照逻辑（修复FileProvider和权限适配）
     */
    private void takePhoto() {
        // 检查相机权限
        if (!PermissionUtils.hasCameraPermission(requireContext())) {
            PermissionUtils.requestCameraPermission(this, PERMISSION_REQUEST_CODE);
            return;
        }

        try {
            // 创建图片文件（使用应用私有目录，无需外部存储权限）
            mPhotoFile = ImageUtils.createImageFile(requireContext());
            Uri photoUri = FileProvider.getUriForFile(requireContext(),
                    "com.baixiaosheng.inventory.fileprovider", // 需与AndroidManifest中一致
                    mPhotoFile);

            // 启动相机
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            } else {
                Toast.makeText(getContext(), "未找到相机应用", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e("InputFragment", "创建拍照文件失败：" + e.getMessage());
            Toast.makeText(getContext(), "创建图片文件失败", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("InputFragment", "拍照异常：" + e.getMessage());
            Toast.makeText(getContext(), "拍照失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 从相册选择图片（修复Android 10+路径适配）
     */
    private void choosePhoto() {
        // 检查存储权限
        if (!PermissionUtils.hasStoragePermission(requireContext())) {
            PermissionUtils.requestStoragePermission(this, PERMISSION_REQUEST_CODE);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_CHOOSE_PHOTO);
    }

    /**
     * 表单校验并保存（修复所有空指针和类型转换问题）
     */
    private void validateAndSave() {
        // 1. 基础校验
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "物品名称不能为空", Toast.LENGTH_SHORT).show();
            etName.requestFocus();
            return;
        }

        // 2. 数量校验
        int quantity = 1;
        String quantityStr = etQuantity.getText().toString().trim();
        if (!quantityStr.isEmpty()) {
            try {
                quantity = Integer.parseInt(quantityStr);
                if (quantity < 0) {
                    Toast.makeText(getContext(), "数量不能为负数", Toast.LENGTH_SHORT).show();
                    etQuantity.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "数量必须是数字", Toast.LENGTH_SHORT).show();
                etQuantity.requestFocus();
                return;
            }
        }

        // 3. 日期校验（做空值判断）
        long validTime = 0;
        String expireDate = etExpireDate.getText().toString().trim();
        if (!expireDate.isEmpty()) {
            try {
                Date date = sdf.parse(expireDate);
                validTime = date.getTime();
            } catch (ParseException e) {
                Toast.makeText(getContext(), "日期格式错误（yyyy-MM-dd）", Toast.LENGTH_SHORT).show();
                etExpireDate.requestFocus();
                return;
            }
        }

        // 4. Spinner选中项（做空值判断）
        String parentCat = spParentCategory.getSelectedItem() != null ?
                spParentCategory.getSelectedItem().toString() : "未分类";
        String childCat = spChildCategory.getSelectedItem() != null ?
                spChildCategory.getSelectedItem().toString() : "无";
        String location = spLocation.getSelectedItem() != null ?
                spLocation.getSelectedItem().toString() : "未指定";

        // 5. 构建Item对象（修复字段类型匹配）
        Item item = new Item();
        item.setUuid(UUID.randomUUID().toString()); // 唯一标识
        item.setName(name);
        item.setParentCategoryId(getCategoryId(parentCat)); // 名称转ID
        item.setChildCategoryId(getCategoryId(childCat));
        item.setLocationId(getLocationId(location));
        item.setCount(quantity);
        item.setValidTime(validTime);
        item.setRemark(etDescription.getText().toString().trim());
        item.setIsDeleted(0); // 未删除
        item.setCreateTime(System.currentTimeMillis());
        item.setUpdateTime(System.currentTimeMillis());

        // 6. 拼接图片路径（处理空列表）
        StringBuilder imagePaths = new StringBuilder();
        for (int i = 0; i < mImagePaths.size(); i++) {
            imagePaths.append(mImagePaths.get(i));
            if (i < mImagePaths.size() - 1) {
                imagePaths.append(",");
            }
        }
        item.setImagePaths(imagePaths.toString());

        // 7. 保存数据
        mInputViewModel.saveItem(item);
    }

    /**
     * 分类名称转ID（适配数据库字段类型）
     */
    private long getCategoryId(String catName) {
        switch (catName) {
            case "食品": return 1;
            case "日用品": return 2;
            case "电子产品": return 3;
            default: return 0; // 未分类
        }
    }

    /**
     * 位置名称转ID（适配数据库字段类型）
     */
    private long getLocationId(String locName) {
        switch (locName) {
            case "客厅": return 1;
            case "卧室": return 2;
            case "厨房": return 3;
            default: return 0; // 未指定
        }
    }

    /**
     * 清空表单
     */
    private void clearForm() {
        etName.setText("");
        etExpireDate.setText("");
        etQuantity.setText("");
        etDescription.setText("");
        spParentCategory.setSelection(0);
        spChildCategory.setSelection(0);
        spLocation.setSelection(0);
        mImagePaths.clear();
        llImagePreview.removeAllViews();
    }

    /**
     * 图片预览（简化版，避免依赖缺失的布局文件）
     */
    private void previewImage(String filePath) {
        try {
            Bitmap bitmap = ImageUtils.decodeImage(filePath);
            if (bitmap == null) {
                Toast.makeText(getContext(), "图片解码失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建预览ImageView（无需额外布局文件）
            ImageView ivPreview = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dp2px(100), dp2px(100));
            params.setMargins(dp2px(5), dp2px(5), dp2px(5), dp2px(5));
            ivPreview.setLayoutParams(params);
            ivPreview.setImageBitmap(bitmap);
            ivPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // 长按删除
            ivPreview.setOnLongClickListener(v -> {
                mImagePaths.remove(filePath);
                llImagePreview.removeView(ivPreview);
                // 回收Bitmap
                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                return true;
            });

            llImagePreview.addView(ivPreview);
        } catch (Exception e) {
            Log.e("InputFragment", "预览图片失败：" + e.getMessage());
            Toast.makeText(getContext(), "预览图片失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * dp转px工具方法（做空值判断）
     */
    private int dp2px(int dp) {
        if (getContext() == null) return dp;
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != requireActivity().RESULT_OK) {
            return;
        }

        // 拍照返回
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (mPhotoFile != null && mPhotoFile.exists()) {
                String path = mPhotoFile.getAbsolutePath();
                mImagePaths.add(path);
                previewImage(path);
            } else {
                Toast.makeText(getContext(), "拍照文件丢失", Toast.LENGTH_SHORT).show();
            }
        }

        // 选图返回（适配Android 10+路径）
        else if (requestCode == REQUEST_CHOOSE_PHOTO) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                String path = ImageUtils.getPathFromUri(requireContext(), uri);
                if (path != null) {
                    mImagePaths.add(path);
                    previewImage(path);
                } else {
                    Toast.makeText(getContext(), "获取图片路径失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                Toast.makeText(getContext(), "权限被拒绝，无法完成操作", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 回收所有Bitmap，避免内存泄漏
        for (int i = 0; i < llImagePreview.getChildCount(); i++) {
            View child = llImagePreview.getChildAt(i);
            if (child instanceof ImageView) {
                ImageView iv = (ImageView) child;
                iv.setImageBitmap(null);
            }
        }
    }
}