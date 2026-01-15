package com.baixiaosheng.inventory.view.fragment;

import android.app.Activity;
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
import com.baixiaosheng.inventory.database.entity.Category;
import com.baixiaosheng.inventory.database.entity.Item;
import com.baixiaosheng.inventory.database.entity.Location;
import com.baixiaosheng.inventory.utils.ImageUtils;
import com.baixiaosheng.inventory.utils.PermissionUtils;
import com.baixiaosheng.inventory.viewmodel.CategoryManageViewModel;
import com.baixiaosheng.inventory.viewmodel.InputViewModel;
import com.baixiaosheng.inventory.viewmodel.LocationManageViewModel;
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
 * 录入页Fragment（支持编辑模式 + 动态加载分类/位置数据 + 父分类联动子分类）
 */
public class InputFragment extends Fragment {
    // 请求码
    private static final int REQUEST_TAKE_PHOTO = 101;
    private static final int REQUEST_CHOOSE_PHOTO = 102;
    private static final int PERMISSION_REQUEST_CODE = 103;
    private static final int PERMISSION_REQUEST_STORAGE = 104;
    private static final int PERMISSION_REQUEST_WRITE_STORAGE = 105;

    // 视图控件
    private EditText etName, etExpireDate, etQuantity, etDescription;
    private Spinner spParentCategory, spChildCategory, spLocation;
    private LinearLayout llImagePreview;
    private ImageView ivAddImage;
    private Button btnSave;

    // 图片相关
    private File mPhotoFile;
    private final List<String> mImagePaths = new ArrayList<>();

    // ViewModel
    private InputViewModel mInputViewModel;
    private CategoryManageViewModel mCategoryViewModel;
    private LocationManageViewModel mLocationViewModel;

    // 缓存数据
    private List<Category> mAllCategories = new ArrayList<>();
    private List<Location> mAllLocations = new ArrayList<>();
    private long mCurrentParentCategoryId = 0; // 当前选中的父分类ID

    // 日期格式化
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    // 编辑模式相关
    private boolean isEditMode = false;
    private Item mEditItem;
    private boolean isFormFilled = false; // 标记表单是否已回填（避免重复回填）

    // 权限操作标记
    private String mPermissionAction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input, container, false);
        initView(view);
        initAddImageButton();
        initViewModel();
        observeViewModelData(); // 新增：观察分类/位置数据
        initListener();
        receiveEditParams();
        return view;
    }

    /**
     * 初始化ViewModel（添加分类/位置ViewModel依赖）
     */
    private void initViewModel() {
        // 物品录入ViewModel
        mInputViewModel = new ViewModelProvider(this).get(InputViewModel.class);

        // 分类管理ViewModel
        mCategoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryManageViewModel.class);

        // 位置管理ViewModel
        mLocationViewModel = new ViewModelProvider(requireActivity()).get(LocationManageViewModel.class);

        // 观察物品保存结果
        mInputViewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(getContext(), isEditMode ? "修改保存成功" : "保存成功", Toast.LENGTH_SHORT).show();
                if (isEditMode) {
                    requireActivity().setResult(Activity.RESULT_OK);
                    requireActivity().finish();
                } else {
                    clearForm();
                }
            } else {
                Toast.makeText(getContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 观察ViewModel数据（分类/位置），动态初始化Spinner
     */
    private void observeViewModelData() {
        // 观察分类列表
        mCategoryViewModel.getCategoryList().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                mAllCategories = new ArrayList<>(categories);
                // 初始化父分类Spinner
                initParentCategorySpinner();

                // 如果是编辑模式且数据已加载完成，回填表单
                if (isEditMode && !isFormFilled) {
                    fillFormData();
                }
            }
        });

        // 观察位置列表
        mLocationViewModel.getLocationList().observe(getViewLifecycleOwner(), locations -> {
            if (locations != null) {
                mAllLocations = new ArrayList<>(locations);
                // 初始化位置Spinner
                initLocationSpinner();

                // 如果是编辑模式且数据已加载完成，回填表单
                if (isEditMode && !isFormFilled) {
                    fillFormData();
                }
            }
        });
    }

    /**
     * 初始化父分类Spinner（动态加载 + 保留默认值）
     */
    private void initParentCategorySpinner() {
        List<String> parentCatNames = new ArrayList<>();
        // 添加默认选项
        parentCatNames.add("未分类");

        // 添加数据库中的父分类（parentId=0的分类）
        for (Category category : mAllCategories) {
            if (category.getParentId() == 0) {
                parentCatNames.add(category.getName());
            }
        }

        ArrayAdapter<String> parentAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, parentCatNames);
        parentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spParentCategory.setAdapter(parentAdapter);

        // 如果是编辑模式，先暂存父分类ID，等待数据加载完成后选中
        if (isEditMode && mEditItem != null) {
            for (int i = 0; i < parentCatNames.size(); i++) {
                if (parentCatNames.get(i).equals(getCategoryNameById(mEditItem.getParentCategoryId()))) {
                    spParentCategory.setSelection(i);
                    mCurrentParentCategoryId = mEditItem.getParentCategoryId();
                    // 联动加载子分类
                    initChildCategorySpinner(mCurrentParentCategoryId);
                    break;
                }
            }
        }
    }

    /**
     * 初始化子分类Spinner（根据父分类ID动态加载 + 保留默认值）
     */
    private void initChildCategorySpinner(long parentCategoryId) {
        List<String> childCatNames = new ArrayList<>();
        // 添加默认选项
        childCatNames.add("无");

        // 添加指定父分类下的子分类
        for (Category category : mAllCategories) {
            if (category.getParentId() == parentCategoryId && parentCategoryId != 0) {
                childCatNames.add(category.getName());
            }
        }

        ArrayAdapter<String> childAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, childCatNames);
        childAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spChildCategory.setAdapter(childAdapter);

        // 编辑模式下回填子分类
        if (isEditMode && mEditItem != null && !isFormFilled) {
            fillSpinnerByValue(spChildCategory, getCategoryNameById(mEditItem.getChildCategoryId()));
        }
    }

    /**
     * 初始化位置Spinner（动态加载 + 保留默认值）
     */
    private void initLocationSpinner() {
        List<String> locationNames = new ArrayList<>();
        // 添加默认选项
        locationNames.add("未指定");

        // 添加数据库中的位置
        for (Location location : mAllLocations) {
            locationNames.add(location.getName());
        }

        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, locationNames);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLocation.setAdapter(locationAdapter);

        // 编辑模式下回填位置
        if (isEditMode && mEditItem != null && !isFormFilled) {
            fillSpinnerByValue(spLocation, getLocationNameById(mEditItem.getLocationId()));
        }
    }

    /**
     * 初始化事件监听（添加父分类选择联动监听）
     */
    private void initListener() {
        // 有效期选择
        etExpireDate.setOnClickListener(v -> showDatePicker());

        // 添加图片
        ivAddImage.setOnClickListener(v -> showImageChooseDialog());

        // 保存按钮
        btnSave.setOnClickListener(v -> validateAndSave());

        // 父分类选择监听（核心：联动子分类）
        spParentCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 获取选中的父分类名称
                String selectedName = parent.getItemAtPosition(position).toString();
                // 转换为分类ID
                mCurrentParentCategoryId = getCategoryIdByName(selectedName, true);
                // 联动刷新子分类
                initChildCategorySpinner(mCurrentParentCategoryId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mCurrentParentCategoryId = 0;
                initChildCategorySpinner(0);
            }
        });
    }

    /**
     * 重构：根据分类名称和是否为父分类获取ID（适配动态数据）
     * @param name 分类名称
     * @param isParent 是否为父分类
     * @return 分类ID
     */
    private long getCategoryIdByName(String name, boolean isParent) {
        // 默认值处理
        if ("未分类".equals(name) || "无".equals(name)) {
            return 0;
        }

        // 从缓存列表中匹配
        for (Category category : mAllCategories) {
            if (category.getName().equals(name)) {
                // 父分类需满足parentId=0，子分类需满足parentId=当前选中的父分类ID
                if (isParent && category.getParentId() == 0) {
                    return category.getId();
                } else if (!isParent && category.getParentId() == mCurrentParentCategoryId) {
                    return category.getId();
                }
            }
        }
        return 0;
    }

    /**
     * 重构：根据分类ID获取名称（适配动态数据）
     */
    private String getCategoryNameById(long id) {
        // 默认值处理
        if (id == 0) {
            return "未分类";
        }

        // 从缓存列表中匹配
        for (Category category : mAllCategories) {
            if (category.getId() == id) {
                return category.getName();
            }
        }
        return "未分类";
    }

    /**
     * 重构：根据位置名称获取ID（适配动态数据）
     */
    private long getLocationIdByName(String name) {
        // 默认值处理
        if ("未指定".equals(name)) {
            return 0;
        }

        // 从缓存列表中匹配
        for (Location location : mAllLocations) {
            if (location.getName().equals(name)) {
                return location.getId();
            }
        }
        return 0;
    }

    /**
     * 重构：根据位置ID获取名称（适配动态数据）
     */
    private String getLocationNameById(long id) {
        // 默认值处理
        if (id == 0) {
            return "未指定";
        }

        // 从缓存列表中匹配
        for (Location location : mAllLocations) {
            if (location.getId() == id) {
                return location.getName();
            }
        }
        return "未指定";
    }

    /**
     * 表单校验并保存（适配动态分类/位置ID）
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

        // 3. 日期校验
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

        // 4. 动态获取分类/位置ID
        String parentCatName = spParentCategory.getSelectedItem().toString();
        String childCatName = spChildCategory.getSelectedItem().toString();
        String locationName = spLocation.getSelectedItem().toString();

        long parentCategoryId = getCategoryIdByName(parentCatName, true);
        long childCategoryId = getCategoryIdByName(childCatName, false);
        long locationId = getLocationIdByName(locationName);

        // 5. 构建Item对象
        Item item;
        if (isEditMode) {
            item = mEditItem;
            item.setName(name);
            item.setParentCategoryId(parentCategoryId);
            item.setChildCategoryId(childCategoryId);
            item.setLocationId(locationId);
            item.setCount(quantity);
            item.setValidTime(validTime);
            item.setRemark(etDescription.getText().toString().trim());
            item.setUpdateTime(System.currentTimeMillis());
        } else {
            item = new Item();
            item.setUuid(UUID.randomUUID().toString());
            item.setName(name);
            item.setParentCategoryId(parentCategoryId);
            item.setChildCategoryId(childCategoryId);
            item.setLocationId(locationId);
            item.setCount(quantity);
            item.setValidTime(validTime);
            item.setRemark(etDescription.getText().toString().trim());
            item.setIsDeleted(0);
            item.setCreateTime(System.currentTimeMillis());
            item.setUpdateTime(System.currentTimeMillis());
        }

        // 6. 拼接图片路径
        StringBuilder imagePaths = new StringBuilder();
        for (int i = 0; i < mImagePaths.size(); i++) {
            imagePaths.append(mImagePaths.get(i));
            if (i < mImagePaths.size() - 1) {
                imagePaths.append(",");
            }
        }
        item.setImagePaths(imagePaths.toString());

        // 7. 保存/更新数据
        if (isEditMode) {
            mInputViewModel.updateItem(item);
        } else {
            mInputViewModel.saveItem(item);
        }
    }

    // ---------------------- 以下为原有核心逻辑（仅适配动态数据） ----------------------

    private void initAddImageButton() {
        ivAddImage = new ImageView(requireContext());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                dp2px(70), dp2px(70));
        btnParams.setMargins(dp2px(4), dp2px(0), dp2px(4), dp2px(0));
        ivAddImage.setLayoutParams(btnParams);
        ivAddImage.setBackgroundResource(R.color.darker_gray);
        ivAddImage.setPadding(dp2px(15), dp2px(15), dp2px(15), dp2px(15));
        ivAddImage.setImageResource(R.drawable.ic_menu_camera);
        ivAddImage.setScaleType(ImageView.ScaleType.CENTER);
        ivAddImage.setOnClickListener(v -> showImageChooseDialog());
        llImagePreview.addView(ivAddImage);
    }

    private void receiveEditParams() {
        Bundle args = getArguments();
        if (args != null && args.containsKey("edit_item")) {
            isEditMode = true;
            mEditItem = (Item) args.getSerializable("edit_item");
            // 延迟回填，等待ViewModel数据加载完成
        }
    }

    /**
     * 表单数据回填（适配动态数据，等待数据加载完成后执行）
     */
    private void fillFormData() {
        if (mEditItem == null || isFormFilled || mAllCategories.isEmpty() || mAllLocations.isEmpty()) {
            return;
        }

        // 1. 基础字段回填
        etName.setText(mEditItem.getName());
        etQuantity.setText(String.valueOf(mEditItem.getCount()));
        etDescription.setText(mEditItem.getRemark());

        // 2. 有效期回填
        if (mEditItem.getValidTime() > 0) {
            etExpireDate.setText(sdf.format(new Date(mEditItem.getValidTime())));
        }

        // 3. 图片预览区回填
        if (mEditItem.getImagePaths() != null && !mEditItem.getImagePaths().isEmpty()) {
            mImagePaths.clear();
            int childCount = llImagePreview.getChildCount();
            for (int i = childCount - 1; i > 0; i--) {
                llImagePreview.removeViewAt(i);
            }

            String[] paths = mEditItem.getImagePaths().split(",");
            for (String path : paths) {
                if (!path.isEmpty()) {
                    mImagePaths.add(path);
                    previewImage(path);
                }
            }
        }

        // 标记表单已回填
        isFormFilled = true;
        // 修改保存按钮文字
        btnSave.setText("保存修改");
    }

    private void fillSpinnerByValue(Spinner spinner, String value) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void initView(View view) {
        etName = view.findViewById(R.id.et_name);
        etExpireDate = view.findViewById(R.id.et_expire_date);
        etQuantity = view.findViewById(R.id.et_quantity);
        etDescription = view.findViewById(R.id.et_description);
        spParentCategory = view.findViewById(R.id.sp_parent_category);
        spChildCategory = view.findViewById(R.id.sp_child_category);
        spLocation = view.findViewById(R.id.sp_location);
        llImagePreview = view.findViewById(R.id.ll_image_preview);
        btnSave = view.findViewById(R.id.btn_save);
    }

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

    private void showImageChooseDialog() {
        String[] options = {"拍照", "从相册选择"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("选择图片来源")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        takePhoto();
                    } else {
                        choosePhoto();
                    }
                })
                .show();
    }

    private void takePhoto() {
        if (!PermissionUtils.hasCameraPermission(requireContext())) {
            mPermissionAction = "take_photo";
            PermissionUtils.requestCameraPermission(this, PERMISSION_REQUEST_CODE);
            return;
        }

        try {
            mPhotoFile = ImageUtils.createImageFile(requireContext());
            Uri photoUri = FileProvider.getUriForFile(requireContext(),
                    "com.baixiaosheng.inventory.fileprovider", mPhotoFile);

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

    private void choosePhoto() {
        if (!PermissionUtils.hasStoragePermission(requireContext())) {
            mPermissionAction = "choose_photo";
            PermissionUtils.requestStoragePermission(this, PERMISSION_REQUEST_STORAGE);
            return;
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !PermissionUtils.hasWriteStoragePermission(requireContext())) {
            mPermissionAction = "choose_photo";
            PermissionUtils.requestWriteStoragePermission(this, PERMISSION_REQUEST_WRITE_STORAGE);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_CHOOSE_PHOTO);
    }

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
        initAddImageButton();
        if (isEditMode) {
            isEditMode = false;
            mEditItem = null;
            isFormFilled = false;
            btnSave.setText("保存");
        }
    }

    private void previewImage(String filePath) {
        try {
            Bitmap bitmap = ImageUtils.decodeImage(filePath);
            if (bitmap == null) {
                Toast.makeText(getContext(), "图片解码失败", Toast.LENGTH_SHORT).show();
                return;
            }

            ImageView ivPreview = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dp2px(100), dp2px(100));
            params.setMargins(dp2px(5), dp2px(5), dp2px(5), dp2px(5));
            ivPreview.setLayoutParams(params);
            ivPreview.setImageBitmap(bitmap);
            ivPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);

            ivPreview.setOnLongClickListener(v -> {
                mImagePaths.remove(filePath);
                llImagePreview.removeView(ivPreview);
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

        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (mPhotoFile != null && mPhotoFile.exists()) {
                String path = mPhotoFile.getAbsolutePath();
                mImagePaths.add(path);
                previewImage(path);
            } else {
                Toast.makeText(getContext(), "拍照文件丢失", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CHOOSE_PHOTO) {
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

        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (granted) {
                choosePhoto();
            } else {
                if (shouldShowRequestPermissionRationale(permissions[0])) {
                    Toast.makeText(getContext(), "存储权限被拒绝，无法从相册选择图片", Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("权限提示")
                            .setMessage("存储权限已被禁用，请前往设置页开启，否则无法从相册选择图片")
                            .setPositiveButton("去设置", (dialog, which) -> PermissionUtils.goToAppSettings(requireContext()))
                            .setNegativeButton("取消", null)
                            .show();
                }
            }
        } else if (requestCode == PERMISSION_REQUEST_CODE) {
            if (granted) {
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

    private void goToAppSettings() {
        try {
            Intent intent = new Intent();
            intent.setAction("com.huawei.settings.permissionmanage.PermissionManageActivity");
            intent.putExtra("packageName", requireContext().getPackageName());
            startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 页面恢复可见时，主动刷新分类和位置数据
        if (mCategoryViewModel != null) {
            mCategoryViewModel.loadAllCategories(); // 触发分类数据刷新
        }
        if (mLocationViewModel != null) {
            mLocationViewModel.loadAllLocations(); // 触发位置数据刷新
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (int i = 0; i < llImagePreview.getChildCount(); i++) {
            View child = llImagePreview.getChildAt(i);
            if (child instanceof ImageView) {
                ImageView iv = (ImageView) child;
                iv.setImageBitmap(null);
            }
        }
    }
}