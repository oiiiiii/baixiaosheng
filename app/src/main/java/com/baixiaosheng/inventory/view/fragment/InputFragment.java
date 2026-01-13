package com.baixiaosheng.inventory.view.fragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
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
 * 录入页Fragment（支持编辑模式 + 修复所有闪退问题：类型转换、空指针、资源缺失、权限适配）
 */
public class InputFragment extends Fragment {
    // 请求码
    private static final int REQUEST_TAKE_PHOTO = 101;
    private static final int REQUEST_CHOOSE_PHOTO = 102;
    private static final int PERMISSION_REQUEST_CODE = 103;
    private static final int PERMISSION_REQUEST_STORAGE = 104; // 单独的存储权限请求码
    private static final int PERMISSION_REQUEST_WRITE_STORAGE = 105; // 写入存储权限请求码

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

    // 新增：编辑模式相关变量
    private boolean isEditMode = false; // 是否为编辑模式
    private Item mEditItem; // 编辑的物品对象
    // 新增：标记当前权限请求对应的操作（拍照/选图）
    private String mPermissionAction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input, container, false);
        initView(view);
        initViewModel();
        initListener();
        initSpinnerData(); // 初始化Spinner默认数据，避免空指针
        // 新增：接收编辑参数，初始化编辑模式
        receiveEditParams();
        return view;
    }

    /**
     * 新增：接收编辑参数（从QueryFragment传递的物品对象）
     */
    private void receiveEditParams() {
        Bundle args = getArguments();
        if (args != null && args.containsKey("edit_item")) {
            isEditMode = true;
            mEditItem = (Item) args.getSerializable("edit_item");
            // 回填数据到表单
            fillFormData();
            // 修改保存按钮文字
            btnSave.setText("保存修改");
        }
    }

    /**
     * 新增：表单数据回填（编辑模式下）
     */
    private void fillFormData() {
        if (mEditItem == null) return;

        // 1. 基础字段回填
        etName.setText(mEditItem.getName());
        etQuantity.setText(String.valueOf(mEditItem.getCount()));
        etDescription.setText(mEditItem.getRemark());

        // 2. 有效期回填
        if (mEditItem.getValidTime() > 0) {
            etExpireDate.setText(sdf.format(new Date(mEditItem.getValidTime())));
        }

        // 3. 分类、位置Spinner回填（根据ID匹配选项）
        // 父分类回填
        fillSpinnerByValue(spParentCategory, getCategoryNameById(mEditItem.getParentCategoryId()));
        // 子分类回填
        fillSpinnerByValue(spChildCategory, getCategoryNameById(mEditItem.getChildCategoryId()));
        // 位置回填
        fillSpinnerByValue(spLocation, getLocationNameById(mEditItem.getLocationId()));

        // 4. 图片预览区回填
        if (mEditItem.getImagePaths() != null && !mEditItem.getImagePaths().isEmpty()) {
            // 清空原有图片列表，避免叠加
            mImagePaths.clear();
            llImagePreview.removeAllViews();
            // 分割图片路径并添加预览
            String[] paths = mEditItem.getImagePaths().split(",");
            for (String path : paths) {
                if (!path.isEmpty()) {
                    mImagePaths.add(path);
                    previewImage(path); // 复用原有预览逻辑
                }
            }
        }
    }

    /**
     * 辅助方法：根据分类/位置ID获取名称（适配Spinner回填）
     */
    private String getCategoryNameById(long id) {
        switch ((int) id) {
            case 1: return "食品";
            case 2: return "日用品";
            case 3: return "电子产品";
            default: return "未分类"; // 父分类默认 / 子分类默认"无"
        }
    }

    /**
     * 辅助方法：根据位置ID获取名称
     */
    private String getLocationNameById(long id) {
        switch ((int) id) {
            case 1: return "客厅";
            case 2: return "卧室";
            case 3: return "厨房";
            default: return "未指定";
        }
    }

    /**
     * 辅助方法：根据值设置Spinner选中项
     */
    private void fillSpinnerByValue(Spinner spinner, String value) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
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
                Toast.makeText(getContext(), isEditMode ? "修改保存成功" : "保存成功", Toast.LENGTH_SHORT).show();
                clearForm();
                // 编辑模式下保存成功后返回查询页
                if (isEditMode) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
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
        parentCats.add("食品");
        parentCats.add("日用品");
        parentCats.add("电子产品");
        ArrayAdapter<String> parentAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, parentCats);
        parentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spParentCategory.setAdapter(parentAdapter);

        // 子分类默认数据
        List<String> childCats = new ArrayList<>();
        childCats.add("无");
        childCats.add("食品");
        childCats.add("日用品");
        childCats.add("电子产品");
        ArrayAdapter<String> childAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, childCats);
        childAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spChildCategory.setAdapter(childAdapter);

        // 位置默认数据
        List<String> locations = new ArrayList<>();
        locations.add("未指定");
        locations.add("客厅");
        locations.add("卧室");
        locations.add("厨房");
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
            mPermissionAction = "take_photo"; // 标记当前是拍照操作
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
     * 从相册选择图片（修复Android 10+路径适配 + 权限申请逻辑）
     */

    // 优化choosePhoto方法，增加写入权限检查（Android 9及以下）
    private void choosePhoto() {
        // 检查读取存储权限
        if (!PermissionUtils.hasStoragePermission(requireContext())) {
            mPermissionAction = "choose_photo";
            PermissionUtils.requestStoragePermission(this, PERMISSION_REQUEST_STORAGE);
            return;
        }

        // 检查写入存储权限（仅Android 9及以下）
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !PermissionUtils.hasWriteStoragePermission(requireContext())) {
            mPermissionAction = "choose_photo";
            PermissionUtils.requestWriteStoragePermission(this, PERMISSION_REQUEST_WRITE_STORAGE);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_CHOOSE_PHOTO);
    }

    /**
     * 表单校验并保存（区分新增/编辑模式 + 修复所有空指针和类型转换问题）
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

        // 5. 构建Item对象（区分新增/编辑模式）
        Item item;
        if (isEditMode) {
            // 编辑模式：复用原有物品对象，仅更新字段
            item = mEditItem;
            item.setName(name);
            item.setParentCategoryId(getCategoryId(parentCat));
            item.setChildCategoryId(getCategoryId(childCat));
            item.setLocationId(getLocationId(location));
            item.setCount(quantity);
            item.setValidTime(validTime);
            item.setRemark(etDescription.getText().toString().trim());
            item.setUpdateTime(System.currentTimeMillis()); // 仅更新修改时间
        } else {
            // 新增模式：创建新对象
            item = new Item();
            item.setUuid(UUID.randomUUID().toString()); // 唯一标识
            item.setName(name);
            item.setParentCategoryId(getCategoryId(parentCat));
            item.setChildCategoryId(getCategoryId(childCat));
            item.setLocationId(getLocationId(location));
            item.setCount(quantity);
            item.setValidTime(validTime);
            item.setRemark(etDescription.getText().toString().trim());
            item.setIsDeleted(0); // 未删除
            item.setCreateTime(System.currentTimeMillis());
            item.setUpdateTime(System.currentTimeMillis());
        }

        // 6. 拼接图片路径（处理空列表）
        StringBuilder imagePaths = new StringBuilder();
        for (int i = 0; i < mImagePaths.size(); i++) {
            imagePaths.append(mImagePaths.get(i));
            if (i < mImagePaths.size() - 1) {
                imagePaths.append(",");
            }
        }
        item.setImagePaths(imagePaths.toString());

        // 7. 保存/更新数据（区分新增/编辑）
        if (isEditMode) {
            mInputViewModel.updateItem(item); // 编辑：调用更新接口
        } else {
            mInputViewModel.saveItem(item); // 新增：调用保存接口
        }
    }

    /**
     * 分类名称转ID（适配数据库字段类型）
     */
    private long getCategoryId(String catName) {
        switch (catName) {
            case "食品": return 1;
            case "日用品": return 2;
            case "电子产品": return 3;
            default: return 0; // 未分类/无
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
        // 编辑模式清空后重置为新增模式
        if (isEditMode) {
            isEditMode = false;
            mEditItem = null;
            btnSave.setText("保存");
        }
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
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        // 处理存储权限（选图）
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (granted) {
                // 权限授予，重新执行选图操作
                choosePhoto();
            } else {
                // 权限被拒绝
                if (shouldShowRequestPermissionRationale(permissions[0])) {
                    // 用户只是拒绝，未勾选「不再询问」，提示需要权限
                    Toast.makeText(getContext(), "存储权限被拒绝，无法从相册选择图片", Toast.LENGTH_SHORT).show();
                } else {
                    // 用户拒绝且勾选「不再询问」，引导到设置页
                    new AlertDialog.Builder(requireContext())
                            .setTitle("权限提示")
                            .setMessage("存储权限已被禁用，请前往设置页开启，否则无法从相册选择图片")
                            .setPositiveButton("去设置", (dialog, which) -> PermissionUtils.goToAppSettings(requireContext()))
                            .setNegativeButton("取消", null)
                            .show();
                }
            }
        }

        // 处理相机权限（拍照）
        else if (requestCode == PERMISSION_REQUEST_CODE) {
            if (granted) {
                // 权限授予，重新执行拍照操作
                if ("take_photo".equals(mPermissionAction)) {
                    takePhoto();
                }
            } else {
                if (shouldShowRequestPermissionRationale(permissions[0])) {
                    Toast.makeText(getContext(), "相机权限被拒绝，无法拍照", Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("权限提示")
                            .setMessage("相机权限已被禁用，请前往设置页开启，否则无法拍照")
                            .setPositiveButton("去设置", (dialog, which) -> PermissionUtils.goToAppSettings(requireContext()))
                            .setNegativeButton("取消", null)
                            .show();
                }
            }
        }
    }

    /**
     * 跳转到应用权限设置页（兼容HarmonyOS）
     */
    private void goToAppSettings() {
        try {
            // 优先尝试HarmonyOS专属设置页
            Intent intent = new Intent();
            intent.setAction("com.huawei.settings.permissionmanage.PermissionManageActivity");
            intent.putExtra("packageName", requireContext().getPackageName());
            startActivity(intent);
        } catch (Exception e) {
            // 兼容失败则用Android原生设置页
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
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