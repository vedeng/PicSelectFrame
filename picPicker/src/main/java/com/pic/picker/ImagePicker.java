package com.pic.picker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import com.crop.cropview.FreeCropImageView;
import com.pic.picker.bean.ImageFolder;
import com.pic.picker.bean.ImageItem;
import com.pic.picker.loader.ImageLoader;
import com.pic.picker.util.InnerToaster;
import com.pic.picker.util.ProviderUtil;
import com.pic.picker.util.Utils;
import com.pic.picker.view.CropImageView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.core.content.FileProvider;

/**
 * 图片选择器配置类
 *
 * @author  Author
 */
public class ImagePicker {

    public static final String TAG = ImagePicker.class.getSimpleName();
    public static final int REQUEST_CODE_TAKE = 1001;
    public static final int REQUEST_CODE_CROP = 1002;
    public static final int REQUEST_CODE_PREVIEW = 1003;
    public static final int RESULT_CODE_ITEMS = 1004;
    public static final int RESULT_CODE_BACK = 1005;

    public static final String EXTRA_RESULT_ITEMS = "extra_result_items";
    public static final String EXTRA_SELECTED_IMAGE_POSITION = "selected_image_position";
    public static final String EXTRA_IMAGE_ITEMS = "extra_image_items";
    public static final String EXTRA_FROM_ITEMS = "extra_from_items";

    /** 图库展示一行几个，默认3 */
    private int itemSpanCount = 3;
    /** 选中图片大小限制, 单位默认是M。0代表无限制 */
    private float selectLimitSize = 0f;
    /** 数量选择限制提示是弹窗还是吐司 */
    private boolean selectLimitShowDialog = false;
    /** 是否限制选择图片格式 */
    private boolean filterSelectFormat = false;
    /** 选中图片允许格式列表, 优先级比禁用格式列表高 */
    private ArrayList<String> formatAllowCollection = new ArrayList<>();
    /** 选中图片禁用格式列表 */
    private ArrayList<String> formatDisallowCollection = new ArrayList<>();
    /** 选择图片选中是否带排序数字 */
    private boolean selectPicWithSortNumber = false;
    /** 上限为1时，是否允许多选模式：交互方式有区别 */
    private boolean changeSingleModeWhenLimitOne = false;

    private boolean multiMode = true;
    private int selectLimit = 9;
    private boolean crop = false;
    private boolean showCamera = false;
    private boolean isSaveRectangle = false;
    private int outPutX = 800;
    private int outPutY = 800;
    private int focusWidth = 280;
    private int focusHeight = 280;
    private ImageLoader imageLoader;
    private CropImageView.Style style = CropImageView.Style.RECTANGLE;
    private File cropCacheFolder;
    private File takeImageFile;

    public FreeCropImageView.CropMode mFreeCropMode = com.crop.cropview.FreeCropImageView.CropMode.FREE;
    public boolean isFreeCrop = false;
    private ArrayList<ImageItem> mSelectedImages = new ArrayList<>();
    private List<ImageFolder> mImageFolders;
    private int mCurrentImageFolderPosition = 0;
    private List<OnImageSelectedListener> mImageSelectedListeners;

    private static ImagePicker mInstance;

    /** 选中项Path列表：可用于记录选中顺序 */
    private ArrayList<String> selectSortList = new ArrayList<>();


    private ImagePicker() {
    }

    public static ImagePicker getInstance() {
        if (mInstance == null) {
            synchronized (ImagePicker.class) {
                if (mInstance == null) {
                    mInstance = new ImagePicker();
                }
            }
        }
        return mInstance;
    }

    public boolean isChangeSingleModeWhenLimitOne() {
        return changeSingleModeWhenLimitOne;
    }

    public void setChangeSingleModeWhenLimitOne(boolean changeSingleModeWhenLimitOne) {
        this.changeSingleModeWhenLimitOne = changeSingleModeWhenLimitOne;
    }

    public boolean isSelectPicWithSortNumber() {
        return selectPicWithSortNumber;
    }

    public void setSelectPicWithSortNumber(boolean selectPicWithSortNumber) {
        this.selectPicWithSortNumber = selectPicWithSortNumber;
    }

    public boolean isSelectLimitShowDialog() {
        return selectLimitShowDialog;
    }

    public void setSelectLimitShowDialog(boolean selectLimitShowDialog) {
        this.selectLimitShowDialog = selectLimitShowDialog;
    }

    public float getSelectLimitSize() {
        return selectLimitSize;
    }

    public void setSelectLimitSize(float selectLimitSize) {
        this.selectLimitSize = selectLimitSize;
    }

    public boolean isFilterSelectFormat() {
        return filterSelectFormat;
    }

    public void setFilterSelectFormat(boolean filterSelectFormat) {
        this.filterSelectFormat = filterSelectFormat;
    }

    public ArrayList<String> getFormatAllowCollection() {
        return formatAllowCollection;
    }

    public void setFormatAllowCollection(ArrayList<String> formatAllowCollection) {
        this.formatAllowCollection = formatAllowCollection;
    }

    public ArrayList<String> getFormatDisallowCollection() {
        return formatDisallowCollection;
    }

    public void setFormatDisallowCollection(ArrayList<String> formatDisallowCollection) {
        this.formatDisallowCollection = formatDisallowCollection;
    }

    public ArrayList<String> getSelectSortList() {
        return selectSortList;
    }

    public void setSelectSortList(ArrayList<String> selectSortList) {
        this.selectSortList = selectSortList;
    }

    public int getItemSpanCount() {
        return itemSpanCount;
    }

    public void setItemSpanCount(int itemSpanCount) {
        this.itemSpanCount = itemSpanCount;
    }

    public boolean isMultiMode() {
        return multiMode;
    }

    public void setMultiMode(boolean multiMode) {
        this.multiMode = multiMode;
    }

    public int getSelectLimit() {
        return selectLimit;
    }

    public void setSelectLimit(int selectLimit) {
        this.selectLimit = selectLimit;
    }

    public boolean isCrop() {
        return crop;
    }

    public void setCrop(boolean crop) {
        this.crop = crop;
    }

    public boolean isShowCamera() {
        return showCamera;
    }

    public void setShowCamera(boolean showCamera) {
        this.showCamera = showCamera;
    }

    public boolean isSaveRectangle() {
        return isSaveRectangle;
    }

    public void setSaveRectangle(boolean isSaveRectangle) {
        this.isSaveRectangle = isSaveRectangle;
    }

    public int getOutPutX() {
        return outPutX;
    }

    public void setOutPutX(int outPutX) {
        this.outPutX = outPutX;
    }

    public int getOutPutY() {
        return outPutY;
    }

    public void setOutPutY(int outPutY) {
        this.outPutY = outPutY;
    }

    public int getFocusWidth() {
        return focusWidth;
    }

    public void setFocusWidth(int focusWidth) {
        this.focusWidth = focusWidth;
    }

    public int getFocusHeight() {
        return focusHeight;
    }

    public void setFocusHeight(int focusHeight) {
        this.focusHeight = focusHeight;
    }

    public File getTakeImageFile() {
        return takeImageFile;
    }

    public File getCropCacheFolder(Context context) {
        if (cropCacheFolder == null) {
            cropCacheFolder = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/cropTemp/");
        }
        if (!cropCacheFolder.exists() || !cropCacheFolder.isDirectory()) {
            cropCacheFolder.mkdirs();
        }
        return cropCacheFolder;
    }

    public void setCropCacheFolder(File cropCacheFolder) {
        this.cropCacheFolder = cropCacheFolder;
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    public void setImageLoader(ImageLoader imageLoader) {
        this.imageLoader = imageLoader;
    }

    public CropImageView.Style getStyle() {
        return style;
    }

    public void setStyle(CropImageView.Style style) {
        this.style = style;
    }

    public List<ImageFolder> getImageFolders() {
        return mImageFolders;
    }

    public void setImageFolders(List<ImageFolder> imageFolders) {
        mImageFolders = imageFolders;
    }

    public int getCurrentImageFolderPosition() {
        return mCurrentImageFolderPosition;
    }

    public void setCurrentImageFolderPosition(int mCurrentSelectedImageSetPosition) {
        mCurrentImageFolderPosition = mCurrentSelectedImageSetPosition;
    }

    public ArrayList<ImageItem> getCurrentImageFolderItems() {
        return mImageFolders.get(mCurrentImageFolderPosition).images;
    }

    public boolean isSelect(ImageItem item) {
        return mSelectedImages.contains(item);
    }

    public int getSelectImageCount() {
        if (mSelectedImages == null) {
            return 0;
        }
        return mSelectedImages.size();
    }

    public ArrayList<ImageItem> getSelectedImages() {
        return mSelectedImages;
    }

    public void clearSelectedImages() {
        if (mSelectedImages != null) {
            mSelectedImages.clear();
        }
    }

    public void clear() {
        if (mImageSelectedListeners != null) {
            mImageSelectedListeners.clear();
            mImageSelectedListeners = null;
        }
        if (mImageFolders != null) {
            mImageFolders.clear();
            mImageFolders = null;
        }
        if (mSelectedImages != null) {
            mSelectedImages.clear();
        }
        mCurrentImageFolderPosition = 0;
    }


    public void takePicture(Activity activity, int requestCode) {
        PackageManager packageManager = activity.getPackageManager();
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            InnerToaster.obj(activity).show(R.string.ip_str_no_camera);
            return;
        }
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            if (Utils.existSDCard()) {
                takeImageFile = new File(Environment.getExternalStorageDirectory(), "/DCIM/camera/");
            } else {
                takeImageFile = Environment.getDataDirectory();
            }
            takeImageFile = createFile(takeImageFile, "IMG_", ".jpg");
            if (takeImageFile != null) {
                Uri uri;
                if (VERSION.SDK_INT <= VERSION_CODES.M) {
                    uri = Uri.fromFile(takeImageFile);
                } else {

                    uri = FileProvider.getUriForFile(activity, ProviderUtil.getFileProviderName(activity), takeImageFile);
                    List<ResolveInfo> resInfoList = activity.getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        activity.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                }

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            }
        }
        activity.startActivityForResult(takePictureIntent, requestCode);
    }

    public static File createFile(File folder, String prefix, String suffix) {
        if (!folder.exists() || !folder.isDirectory()) {
            folder.mkdirs();
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);
        String filename = prefix + dateFormat.format(new Date(System.currentTimeMillis())) + suffix;
        return new File(folder, filename);
    }


    public static void galleryAddPic(Context context, File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    public interface OnImageSelectedListener {
        void onImageSelected(int position, ImageItem item, boolean isAdd);
    }

    public void addOnImageSelectedListener(OnImageSelectedListener l) {
        if (mImageSelectedListeners == null) {
            mImageSelectedListeners = new ArrayList<>();
        }
        mImageSelectedListeners.add(l);
    }

    public void removeOnImageSelectedListener(OnImageSelectedListener l) {
        if (mImageSelectedListeners == null) {
            return;
        }
        mImageSelectedListeners.remove(l);
    }

    public void addSelectedImageItem(int position, ImageItem item, boolean isAdd) {
        if (isAdd) {
            mSelectedImages.add(item);
            selectSortList.add(item.path);
        } else {
            mSelectedImages.remove(item);
            selectSortList.remove(item.path);
        }
        notifyImageSelectedChanged(position, item, isAdd);
    }

    public void setSelectedImages(ArrayList<ImageItem> selectedImages) {
        if (selectedImages == null) {
            return;
        }
        this.mSelectedImages = selectedImages;
    }

    private void notifyImageSelectedChanged(int position, ImageItem item, boolean isAdd) {
        if (mImageSelectedListeners == null) {
            return;
        }
        for (OnImageSelectedListener l : mImageSelectedListeners) {
            l.onImageSelected(position, item, isAdd);
        }
    }

    public void restoreInstanceState(Bundle savedInstanceState) {
        cropCacheFolder = (File) savedInstanceState.getSerializable("cropCacheFolder");
        takeImageFile = (File) savedInstanceState.getSerializable("takeImageFile");
        imageLoader = (ImageLoader) savedInstanceState.getSerializable("imageLoader");
        style = (CropImageView.Style) savedInstanceState.getSerializable("style");
        multiMode = savedInstanceState.getBoolean("multiMode");
        crop = savedInstanceState.getBoolean("crop");
        showCamera = savedInstanceState.getBoolean("showCamera");
        isSaveRectangle = savedInstanceState.getBoolean("isSaveRectangle");
        selectLimit = savedInstanceState.getInt("selectLimit");
        outPutX = savedInstanceState.getInt("outPutX");
        outPutY = savedInstanceState.getInt("outPutY");
        focusWidth = savedInstanceState.getInt("focusWidth");
        focusHeight = savedInstanceState.getInt("focusHeight");
        itemSpanCount = savedInstanceState.getInt("itemSpanCount");
        selectLimitSize = savedInstanceState.getFloat("selectLimitSize");
        filterSelectFormat = savedInstanceState.getBoolean("filterSelectFormat");
        selectLimitShowDialog = savedInstanceState.getBoolean("selectLimitShowDialog");
        formatAllowCollection = savedInstanceState.getStringArrayList("formatAllowCollection");
        formatDisallowCollection = savedInstanceState.getStringArrayList("formatDisallowCollection");
        selectPicWithSortNumber = savedInstanceState.getBoolean("selectPicWithSortNumber");
    }


    public void saveInstanceState(Bundle outState) {
        outState.putSerializable("cropCacheFolder", cropCacheFolder);
        outState.putSerializable("takeImageFile", takeImageFile);
        outState.putSerializable("imageLoader", imageLoader);
        outState.putSerializable("style", style);
        outState.putInt("itemSpanCount", itemSpanCount);
        outState.putFloat("selectLimitSize", selectLimitSize);
        outState.putBoolean("filterSelectFormat", filterSelectFormat);
        outState.putBoolean("selectLimitShowDialog", selectLimitShowDialog);
        outState.putStringArrayList("formatAllowCollection", formatAllowCollection);
        outState.putStringArrayList("formatDisallowCollection", formatDisallowCollection);
        outState.putBoolean("selectPicWithSortNumber", selectPicWithSortNumber);
        outState.putBoolean("multiMode", multiMode);
        outState.putBoolean("crop", crop);
        outState.putBoolean("showCamera", showCamera);
        outState.putBoolean("isSaveRectangle", isSaveRectangle);
        outState.putInt("selectLimit", selectLimit);
        outState.putInt("outPutX", outPutX);
        outState.putInt("outPutY", outPutY);
        outState.putInt("focusWidth", focusWidth);
        outState.putInt("focusHeight", focusHeight);
    }


    public void setIToaster(Context aContext, InnerToaster.IToaster aIToaster) {
        InnerToaster.obj(aContext).setIToaster(aIToaster);
    }

    public void setFreeCrop(boolean need, FreeCropImageView.CropMode aCropMode) {
        mFreeCropMode = aCropMode;
        isFreeCrop = need;
    }
}