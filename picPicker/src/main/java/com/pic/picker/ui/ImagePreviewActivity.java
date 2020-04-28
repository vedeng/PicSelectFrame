package com.pic.picker.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.pic.picker.ImagePicker;
import com.pic.picker.R;
import com.pic.picker.bean.ImageItem;
import com.pic.picker.util.InnerToaster;
import com.pic.picker.util.NavigationBarChangeListener;
import com.pic.picker.util.Utils;
import com.pic.picker.view.SuperCheckBox;

import androidx.viewpager.widget.ViewPager;

public class ImagePreviewActivity extends ImagePreviewBaseActivity implements ImagePicker.OnImageSelectedListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public static final String ISORIGIN = "isOrigin";

    private boolean isOrigin;
    private SuperCheckBox mCbCheck;
    private SuperCheckBox mCbOrigin;
    private Button mBtnOk;
    private View bottomBar;
    private View marginView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isOrigin = getIntent().getBooleanExtra(ImagePreviewActivity.ISORIGIN, false);
        imagePicker.addOnImageSelectedListener(this);
        mBtnOk = (Button) findViewById(R.id.btn_ok);
        mBtnOk.setVisibility(View.VISIBLE);
        mBtnOk.setOnClickListener(this);

        bottomBar = findViewById(R.id.bottom_bar);
        bottomBar.setVisibility(View.VISIBLE);

        mCbCheck = (SuperCheckBox) findViewById(R.id.cb_check);
        mCbOrigin = (SuperCheckBox) findViewById(R.id.cb_origin);
        marginView = findViewById(R.id.margin_bottom);
        mCbOrigin.setText(getString(R.string.ip_origin));
        mCbOrigin.setOnCheckedChangeListener(this);
        mCbOrigin.setChecked(isOrigin);

        onImageSelected(0, null, false);
        ImageItem item = mImageItems.get(mCurrentPosition);
        boolean isSelected = imagePicker.isSelect(item);
        mTitleCount.setText(getString(R.string.ip_preview_image_count, mCurrentPosition + 1, mImageItems.size()));
        mCbCheck.setChecked(isSelected);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mCurrentPosition = position;
                ImageItem item = mImageItems.get(mCurrentPosition);
                boolean isSelected = imagePicker.isSelect(item);
                mCbCheck.setChecked(isSelected);
                mTitleCount.setText(getString(R.string.ip_preview_image_count, mCurrentPosition + 1, mImageItems.size()));
            }
        });
        mCbCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageItem imageItem = mImageItems.get(mCurrentPosition);
                if (mCbCheck.isChecked() && !checkSelectAllow(imageItem)) {
                    mCbCheck.setChecked(false);
                } else {
                    imagePicker.addSelectedImageItem(mCurrentPosition, imageItem, mCbCheck.isChecked());
                }
            }
        });
        NavigationBarChangeListener.with(this).setListener(new NavigationBarChangeListener.OnSoftInputStateChangeListener() {
            @Override
            public void onNavigationBarShow(int orientation, int height) {
                marginView.setVisibility(View.VISIBLE);
                ViewGroup.LayoutParams layoutParams = marginView.getLayoutParams();
                if (layoutParams.height == 0) {
                    layoutParams.height = Utils.getNavigationBarHeight(ImagePreviewActivity.this);
                    marginView.requestLayout();
                }
            }

            @Override
            public void onNavigationBarHide(int orientation) {
                marginView.setVisibility(View.GONE);
            }
        });
        NavigationBarChangeListener.with(this, NavigationBarChangeListener.ORIENTATION_HORIZONTAL)
                .setListener(new NavigationBarChangeListener.OnSoftInputStateChangeListener() {
                    @Override
                    public void onNavigationBarShow(int orientation, int height) {
                        topBar.setPadding(0, 0, height, 0);
                        bottomBar.setPadding(0, 0, height, 0);
                    }

                    @Override
                    public void onNavigationBarHide(int orientation) {
                        topBar.setPadding(0, 0, 0, 0);
                        bottomBar.setPadding(0, 0, 0, 0);
                    }
                });
    }

    @Override
    public void onImageSelected(int position, ImageItem item, boolean isAdd) {
        if (imagePicker.getSelectImageCount() > 0) {
            mBtnOk.setText(getString(R.string.ip_select_complete, imagePicker.getSelectImageCount(), imagePicker.getSelectLimit()));
        } else {
            mBtnOk.setText(getString(R.string.ip_complete));
        }

        if (mCbOrigin.isChecked()) {
            long size = 0;
            for (ImageItem imageItem : selectedImages) {
                size += imageItem.size;
            }
            String fileSize = Formatter.formatFileSize(this, size);
            mCbOrigin.setText(getString(R.string.ip_origin_size, fileSize));
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_ok) {
            if (imagePicker.getSelectedImages().size() == 0) {
                mCbCheck.setChecked(true);
                ImageItem imageItem = mImageItems.get(mCurrentPosition);
                imagePicker.addSelectedImageItem(mCurrentPosition, imageItem, mCbCheck.isChecked());
            }
            Intent intent = new Intent();
            intent.putExtra(ImagePicker.EXTRA_RESULT_ITEMS, imagePicker.getSelectedImages());
            setResult(ImagePicker.RESULT_CODE_ITEMS, intent);
            finish();

        } else if (id == R.id.btn_back) {
            Intent intent = new Intent();
            intent.putExtra(ImagePreviewActivity.ISORIGIN, isOrigin);
            setResult(ImagePicker.RESULT_CODE_BACK, intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(ImagePreviewActivity.ISORIGIN, isOrigin);
        setResult(ImagePicker.RESULT_CODE_BACK, intent);
        finish();
        super.onBackPressed();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.cb_origin) {
            if (isChecked) {
                long size = 0;
                for (ImageItem item : selectedImages) {
                    size += item.size;
                }
                String fileSize = Formatter.formatFileSize(this, size);
                isOrigin = true;
                mCbOrigin.setText(getString(R.string.ip_origin_size, fileSize));
            } else {
                isOrigin = false;
                mCbOrigin.setText(getString(R.string.ip_origin));
            }
        }
    }

    @Override
    protected void onDestroy() {
        imagePicker.removeOnImageSelectedListener(this);
        super.onDestroy();
    }

    private boolean checkSelectAllow(ImageItem imageItem) {
        int selectLimit = imagePicker.getSelectLimit();
        if (mCbCheck.isChecked() && selectedImages.size() >= selectLimit) {
            InnerToaster.obj(ImagePreviewActivity.this).show(getString(R.string.ip_select_limit, selectLimit));
            return false;
        }
        // 图片格式限制
        if (imagePicker.isFilterSelectFormat()) {
            String [] split = imageItem.mimeType.split("/");
            String fix = "";
            if (split.length > 1) {
                fix = split[split.length - 1].toLowerCase();
            }
            // 先查允许格式，可能取得的后缀不规范，导致错误的不被限制
            if (imagePicker.getFormatAllowCollection().size() > 0 && !imagePicker.getFormatAllowCollection().contains(fix)) {
                // 允许列表非空，代表有限允许；为空代表全允许；允许列表中无匹配，代表不允许此格式
                StringBuilder allow = new StringBuilder();
                for (String s : imagePicker.getFormatAllowCollection()) {
                    allow.append(s.toUpperCase()).append("、");
                }
                InnerToaster.obj(this).show("文件格式只支持" + allow.substring(0, allow.length() - 1));
                return false;
            }
            if (imagePicker.getFormatDisallowCollection().size() > 0 && imagePicker.getFormatDisallowCollection().contains(fix)) {
                // 禁止列表非空，代表有限禁止；为空代表无禁止；禁止列表中有匹配，代表不允许此格式
                InnerToaster.obj(this).show("文件格式不支持" + fix.toUpperCase());
                return false;
            }
        }

        // 单张选择大小限制
        float len = imagePicker.getSelectLimitSize();
        if (len > 0f && imageItem.size > len * 1024 * 1024) {
            // 大小限制打开，检查大小是否超限
            InnerToaster.obj(this).show("文件大小不能超过" + ((int) len) + "M");
            return false;
        }

        return true;
    }

    @Override
    public void onImageSingleTap() {
        if (topBar.getVisibility() == View.VISIBLE) {
            topBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.top_out));
            bottomBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            topBar.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            tintManager.setStatusBarTintResource(Color.TRANSPARENT);
        } else {
            topBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.top_in));
            bottomBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            topBar.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            tintManager.setStatusBarTintResource(R.color.ip_color_primary_dark);
        }
    }
}
