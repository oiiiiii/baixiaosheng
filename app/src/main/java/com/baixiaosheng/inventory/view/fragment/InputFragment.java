package com.baixiaosheng.inventory.view.fragment;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

/**
 * 录入页Fragment
 * 核心功能：表单展示、字段校验、图片处理、数据保存
 */
public class InputFragment extends Fragment {
    // 请求码
    private static final int REQUEST_TAKE_PHOTO = 101;
    private static final int REQUEST_CHOOSE_PHOTO = 102;

    // 新增：保存拍照的Uri
    private Uri mTakePhotoUri;

    // 视图控件
    private EditText etName, etExpireDate, etQuantity, etDescription;
    private Spinner spParentCategory, spChildCategory, spLocation;
    private LinearLayout llImagePreview;
    private Button btnTakePhoto, btnChoosePhoto, btnSave;

    // 图片相关
    private File mPhotoFile;
    private final List<String> mImagePaths = new ArrayList<>();

    // ViewModel
    private InputViewModel mInputViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input, container, false);
        initView(view);
        initViewModel();
        initListener();
        return view;
    }

    /**
     * 初始化视图控件
     */
    private void initView(View view) {
        etName = view.findViewById(R.id.et_name);
        etExpireDate = view.findViewById(R.id.et_expire_date);
        etQuantity = view.findViewById(R.id.et_quantity);
        etDescription = view.findViewById(R.id.et_description);
        spParentCategory = view.findViewById(R.id.sp_parent_category);
        spChildCategory = view.findViewById(R.id.sp_child_category);
        spLocation = view.findViewById(R.id.sp_location);
        llImagePreview = view.findViewById(R.id.ll_image_preview);
        btnTakePhoto = view.findViewById(R.id.btn_take_photo);
        btnChoosePhoto = view.findViewById(R.id.btn_choose_photo);
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
                clearForm(); // 清空表单
            } else {
                Toast.makeText(getContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 初始化事件监听
     */
    private void initListener() {
        // 有效期选择（日期选择器）
        etExpireDate.setOnClickListener(v -> showDatePickerDialog());

        // 拍照按钮
        btnTakePhoto.setOnClickListener(v -> requestCameraPermission());

        // 选择图片按钮
        btnChoosePhoto.setOnClickListener(v -> requestStoragePermission());

        // 保存按钮
        btnSave.setOnClickListener(v -> validateAndSaveForm());
    }

    /**
     * 显示日期选择器
     */
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year1, month1, dayOfMonth) -> {
                    // 格式化日期显示
                    String dateStr = String.format(Locale.CHINA, "%d-%02d-%02d", year1, month1 + 1, dayOfMonth);
                    etExpireDate.setText(dateStr);
                },
                year, month, day);
        datePickerDialog.show();
    }

    /**
     * 请求相机权限
     */
    private void requestCameraPermission() {
        List<String> deniedPermissions = PermissionUtils.checkPermissions(requireContext(), PermissionUtils.getCameraPermissions());
        if (deniedPermissions.isEmpty()) {
            // 权限已授予，执行拍照
            takePhoto();
        } else {
            // 请求权限
            PermissionUtils.requestPermissions(requireActivity(),
                    deniedPermissions.toArray(new String[0]),
                    PermissionUtils.PERMISSION_CAMERA);
        }
    }

    /**
     * 请求存储权限
     */
    private void requestStoragePermission() {
        List<String> deniedPermissions = PermissionUtils.checkPermissions(requireContext(), PermissionUtils.getStoragePermissions());
        if (deniedPermissions.isEmpty()) {
            // 权限已授予，选择图片
            choosePhoto();
        } else {
            // 请求权限
            PermissionUtils.requestPermissions(requireActivity(),
                    deniedPermissions.toArray(new String[0]),
                    PermissionUtils.PERMISSION_STORAGE);
        }
    }

    /**
     * 拍照逻辑
     */
    private void takePhoto() {
        try {
            // 创建临时图片文件
            mPhotoFile = ImageUtils.createImageFile(requireContext());
            // 获取拍照Intent
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                // 获取文件Uri（适配Android 7.0+ FileProvider）
                Uri photoURI = androidx.core.content.FileProvider.getUriForFile(
                        requireContext(),
                        "com.baixiaosheng.inventory.fileprovider", // 和AndroidManifest中配置的一致
                        mPhotoFile
                );
                mTakePhotoUri = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "创建图片文件失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 选择图片逻辑
     */
    private void choosePhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_CHOOSE_PHOTO);
    }

    /**
     * 表单校验并保存（核心修复方法）
     */
    private void validateAndSaveForm() {
        // 1. 获取表单数据
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "物品名称不能为空", Toast.LENGTH_SHORT).show();
            etName.requestFocus();
            return;
        }

        // 修复：分类/位置名称转ID（long类型）
        String parentCategoryName = spParentCategory.getSelectedItem() != null ? spParentCategory.getSelectedItem().toString() : "";
        String childCategoryName = spChildCategory.getSelectedItem() != null ? spChildCategory.getSelectedItem().toString() : "";
        String locationName = spLocation.getSelectedItem() != null ? spLocation.getSelectedItem().toString() : "";

        long parentCategoryId = getCategoryIdByName(parentCategoryName);
        long childCategoryId = getCategoryIdByName(childCategoryName);
        long locationId = getLocationIdByName(locationName);

        String expireDate = etExpireDate.getText().toString().trim();
        String quantityStr = etQuantity.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // 2. 数量校验（非空则必须是数字，且为非负数）
        int quantity = 1; // 默认数量1
        if (!quantityStr.isEmpty()) {
            try {
                quantity = Integer.parseInt(quantityStr);
                if (quantity < 0) {
                    Toast.makeText(getContext(), "物品数量不能为负数", Toast.LENGTH_SHORT).show();
                    etQuantity.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "物品数量必须是数字", Toast.LENGTH_SHORT).show();
                etQuantity.requestFocus();
                return;
            }
        }

        // 3. 有效期字符串转时间戳（long类型）
        long validTime = 0; // 0表示永久有效
        if (!expireDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
                Date date = sdf.parse(expireDate);
                validTime = date.getTime(); // 转为毫秒时间戳
            } catch (ParseException e) {
                Toast.makeText(getContext(), "日期格式错误，请输入yyyy-MM-dd", Toast.LENGTH_SHORT).show();
                etExpireDate.requestFocus();
                return;
            }
        }

        // 4. 封装Item数据（严格匹配字段类型）
        Item item = new Item();
        item.setName(name);
        item.setParentCategoryId(parentCategoryId); // long类型
        item.setChildCategoryId(childCategoryId);   // long类型
        item.setValidTime(validTime);               // long类型（时间戳）
        item.setLocationId(locationId);             // long类型
        item.setCount(quantity);                    // int类型
        item.setRemark(description);

        // 拼接图片路径（多个用逗号分隔）
        StringBuilder imagePaths = new StringBuilder();
        for (int i = 0; i < mImagePaths.size(); i++) {
            imagePaths.append(mImagePaths.get(i));
            if (i < mImagePaths.size() - 1) {
                imagePaths.append(",");
            }
        }
        item.setImagePaths(imagePaths.toString());

        // 补充创建/更新时间（当前时间戳）
        long currentTime = System.currentTimeMillis();
        item.setCreateTime(currentTime);
        item.setUpdateTime(currentTime);

        // 5. 保存数据
        mInputViewModel.saveItem(item);
    }

    /**
     * 根据分类名称获取分类ID（需匹配Spinner中的选项）
     */
    private long getCategoryIdByName(String categoryName) {
        if (categoryName == null) return 0;
        switch (categoryName) {
            case "食品": return 1;
            case "日用品": return 2;
            case "电子产品": return 3;
            case "服饰": return 4;
            case "未分类": return 0;
            // 补充你的其他分类
            default: return 0;
        }
    }

    /**
     * 根据位置名称获取位置ID（需匹配Spinner中的选项）
     */
    private long getLocationIdByName(String locationName) {
        if (locationName == null) return 0;
        switch (locationName) {
            case "客厅": return 1;
            case "卧室": return 2;
            case "厨房": return 3;
            case "卫生间": return 4;
            case "未指定": return 0;
            // 补充你的其他位置
            default: return 0;
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
     * 图片预览逻辑（修复后）
     */
    private void previewImage(String filePath) {
        // 1. 解码图片（使用修复后的ImageUtils）
        Bitmap bitmap = ImageUtils.decodeImage(filePath);
        if (bitmap != null) {
            // 2. 创建ImageView展示图片
            ImageView ivPreview = new ImageView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dp2px(100), // 宽度100dp
                    dp2px(100)  // 高度100dp
            );
            params.setMargins(dp2px(5), dp2px(5), dp2px(5), dp2px(5));
            ivPreview.setLayoutParams(params);
            ivPreview.setImageBitmap(bitmap);
            ivPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // 长按删除逻辑
            ivPreview.setOnLongClickListener(v -> {
                mImagePaths.remove(filePath);
                llImagePreview.removeView(ivPreview);
                Toast.makeText(getContext(), "已删除图片", Toast.LENGTH_SHORT).show();
                // 回收Bitmap
                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                return true;
            });

            // 点击预览逻辑
            ivPreview.setOnClickListener(v -> Toast.makeText(getContext(), "图片预览", Toast.LENGTH_SHORT).show());

            // 3. 添加到预览容器
            llImagePreview.addView(ivPreview);
        } else {
            Toast.makeText(getContext(), "图片解码失败，无法预览", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * dp转px工具方法
     */
    private int dp2px(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtils.PERMISSION_CAMERA) {
            // 相机权限回调
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                Toast.makeText(getContext(), "相机权限被拒绝，无法拍照", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PermissionUtils.PERMISSION_STORAGE) {
            // 存储权限回调
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                choosePhoto();
            } else {
                Toast.makeText(getContext(), "存储权限被拒绝，无法选择图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != requireActivity().RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_TAKE_PHOTO) {
            try {
                if (mPhotoFile == null || !mPhotoFile.exists()) {
                    Log.e("InputFragment", "拍照文件不存在或为空");
                    Toast.makeText(getContext(), "拍照文件丢失", Toast.LENGTH_SHORT).show();
                    return;
                }
                String photoPath = mPhotoFile.getAbsolutePath();
                // 解码并保存图片
                Bitmap bitmap = ImageUtils.decodeImage(photoPath);
                if (bitmap == null) {
                    Toast.makeText(getContext(), "图片解码失败，请重试", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 保存压缩后的图片
                String savePath = ImageUtils.createImageFile(requireContext()).getAbsolutePath();
                boolean saveSuccess = ImageUtils.saveBitmap(bitmap, savePath);
                if (saveSuccess) {
                    mImagePaths.add(savePath);
                    previewImage(savePath);
                    Toast.makeText(getContext(), "图片保存成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "图片保存失败", Toast.LENGTH_SHORT).show();
                }
                // 回收Bitmap
                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            } catch (OutOfMemoryError e) {
                Log.e("InputFragment", "图片解码OOM：" + e.getMessage());
                Toast.makeText(getContext(), "图片尺寸过大，无法处理", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("InputFragment", "拍照图片处理异常：" + e.getMessage());
                Toast.makeText(getContext(), "图片处理失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CHOOSE_PHOTO) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                try {
                    String path = ImageUtils.getPathFromUri(requireContext(), uri);
                    if (path == null) {
                        Toast.makeText(getContext(), "无法获取图片路径", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 解码并预览图片
                    Bitmap bitmap = ImageUtils.decodeImage(path);
                    if (bitmap == null) {
                        Toast.makeText(getContext(), "图片解码失败", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 保存压缩后的图片
                    String savePath = ImageUtils.createImageFile(requireContext()).getAbsolutePath();
                    boolean saveSuccess = ImageUtils.saveBitmap(bitmap, savePath);
                    if (saveSuccess) {
                        mImagePaths.add(savePath);
                        previewImage(savePath);
                    } else {
                        Toast.makeText(getContext(), "图片保存失败", Toast.LENGTH_SHORT).show();
                    }
                    // 回收Bitmap
                    if (!bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "处理图片失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}