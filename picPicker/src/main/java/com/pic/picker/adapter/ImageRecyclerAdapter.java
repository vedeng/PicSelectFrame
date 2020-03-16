package com.pic.picker.adapter;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.pic.picker.ImagePicker;
import com.pic.picker.R;
import com.pic.picker.bean.ImageItem;
import com.pic.picker.ui.ImageBaseActivity;
import com.pic.picker.ui.ImageGridActivity;
import com.pic.picker.util.InnerToaster;
import com.pic.picker.util.Utils;
import com.pic.picker.view.CheckTextView;

import java.util.ArrayList;

/**
 * 仿系统图库查看列表适配
 *
 * @author 我也知道是谁
 */
public class ImageRecyclerAdapter extends RecyclerView.Adapter<ViewHolder> {


    private static final int ITEM_TYPE_CAMERA = 0;
    private static final int ITEM_TYPE_NORMAL = 1;
    private ImagePicker imagePicker;
    private Activity mActivity;
    private ArrayList<ImageItem> images;
    private ArrayList<ImageItem> mSelectedImages;
    private boolean isShowCamera;
    private int mImageSize;
    private LayoutInflater mInflater;
    private OnImageItemClickListener listener;

    public void setOnImageItemClickListener(OnImageItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnImageItemClickListener {
        void onImageItemClick(View view, ImageItem imageItem, int position);
    }

    public void refreshData(ArrayList<ImageItem> images) {
        if (images == null || images.size() == 0) {
            this.images = new ArrayList<>();
        } else {
            this.images = images;
        }
        notifyDataSetChanged();
    }


    public ImageRecyclerAdapter(Activity activity, ArrayList<ImageItem> images) {
        this.mActivity = activity;
        if (images == null || images.size() == 0) {
            this.images = new ArrayList<>();
        } else {
            this.images = images;
        }

        mImageSize = Utils.getImageItemWidth(mActivity);
        imagePicker = ImagePicker.getInstance();
        isShowCamera = imagePicker.isShowCamera();
        mSelectedImages = imagePicker.getSelectedImages();
        mInflater = LayoutInflater.from(activity);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE_CAMERA){
            return new CameraViewHolder(mInflater.inflate(R.layout.adapter_camera_item,parent,false));
        }
        return new ImageViewHolder(mInflater.inflate(R.layout.adapter_image_list_item,parent,false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (holder instanceof CameraViewHolder){
            ((CameraViewHolder)holder).bindCamera();
        }else if (holder instanceof ImageViewHolder){
            ((ImageViewHolder)holder).bind(position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isShowCamera) {
            return position == 0 ? ITEM_TYPE_CAMERA : ITEM_TYPE_NORMAL;
        }
        return ITEM_TYPE_NORMAL;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return isShowCamera ? images.size() + 1 : images.size();
    }

    public ImageItem getItem(int position) {
        if (isShowCamera) {
            if (position == 0) {
                return null;
            }
            return images.get(position - 1);
        } else {
            return images.get(position);
        }
    }

    private class ImageViewHolder extends ViewHolder{

        View rootView;
        ImageView ivThumb;
        View mask;
        View checkView;
        CheckTextView cbCheck;


        ImageViewHolder(View itemView) {
            super(itemView);
            rootView = itemView;
            ivThumb = (ImageView) itemView.findViewById(R.id.iv_thumb);
            mask = itemView.findViewById(R.id.mask);
            checkView=itemView.findViewById(R.id.checkView);
            cbCheck = (CheckTextView) itemView.findViewById(R.id.tv_check_num);
            itemView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mImageSize));
        }

        void bind(final int position){
            final ImageItem imageItem = getItem(position);
            ivThumb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onImageItemClick(rootView, imageItem, position);
                    }
                }
            });
            checkView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cbCheck.setChecked(!cbCheck.isChecked());
                    if (cbCheck.isChecked() && !checkSelectAllow(imageItem)) {
                        cbCheck.setChecked(false);
                        mask.setVisibility(View.GONE);
                    } else {
                        imagePicker.addSelectedImageItem(position, imageItem, cbCheck.isChecked());
                        mask.setVisibility(View.VISIBLE);
                    }
                }
            });
            if (imagePicker.isMultiMode()) {
                cbCheck.setVisibility(View.VISIBLE);
                boolean checked = mSelectedImages.contains(imageItem);
                if (checked) {
                    mask.setVisibility(View.VISIBLE);
                    cbCheck.setChecked(true);
                    if (imagePicker.isSelectPicWithSortNumber()) {
                        cbCheck.setBackgroundResource(R.drawable.shape_r12_green);
                        cbCheck.setText(getTextSort(imageItem.path));
                    } else {
                        cbCheck.setBackgroundResource(R.mipmap.bg_select_green);
                        cbCheck.setText("");
                    }
                } else {
                    mask.setVisibility(View.GONE);
                    cbCheck.setChecked(false);
                    // BackgroundTint 动态使用时，tint属性不会因为setResource 消失，需要通过setDrawable的方式消除Tint
                    cbCheck.setBackgroundResource(R.mipmap.bg_unselect);
                    cbCheck.setText("");
                }
            } else {
                cbCheck.setVisibility(View.GONE);
            }
            imagePicker.getImageLoader().displayImage(mActivity, imageItem.path, ivThumb, mImageSize, mImageSize);
        }

    }

    private String getTextSort(String path) {
        int index = imagePicker.getSelectSortList().indexOf(path);
        Log.e("角标====", "+ " + (index + 1));
        return String.valueOf(index + 1);
    }

    private boolean checkSelectAllow(ImageItem imageItem) {
        int limit = imagePicker.getSelectLimit();
        // 允许选择数量上限超限
        if (mSelectedImages.size() >= limit) {
            if (!imagePicker.isSelectLimitShowDialog()) {
                InnerToaster.obj(mActivity).show(mActivity.getString(R.string.ip_select_limit, limit));
            } else {
                final Dialog d = new Dialog(mActivity);
                if (d.getWindow() != null) {
                    d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }
                View view = LayoutInflater.from(mActivity).inflate(R.layout.dialog_limit_style, null);
                TextView tipTv = (TextView) view.findViewById(R.id.select_limit_tip);
                TextView knowTv = (TextView) view.findViewById(R.id.select_limit_know);
                tipTv.setText(mActivity.getResources().getString(R.string.select_limit_tip, limit));
                knowTv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { d.dismiss(); }
                });
                d.setContentView(view);
                d.show();
            }
            return false;
        }

        // 图片格式限制
        if (imagePicker.isFilterSelectFormat()) {
            if (!TextUtils.isEmpty(imageItem.name)) {
                String [] split = imageItem.name.split("\\.");
                String fix = split[split.length - 1];
                // 先查允许格式，可能取得的后缀不规范，导致错误的不被限制
                if (imagePicker.getFormatAllowCollection().size() > 0 && !imagePicker.getFormatAllowCollection().contains(fix)) {
                    // 允许列表非空，代表有限允许；为空代表全允许；允许列表中无匹配，代表不允许此格式
                    StringBuilder allow = new StringBuilder();
                    for (String s : imagePicker.getFormatAllowCollection()) {
                        allow.append(s.toUpperCase()).append("、");
                    }
                    InnerToaster.obj(mActivity).show("文件格式只支持" + allow.substring(0, allow.length() - 1));
                    return false;
                }
                if (imagePicker.getFormatDisallowCollection().size() > 0 && imagePicker.getFormatDisallowCollection().contains(fix)) {
                    // 禁止列表非空，代表有限禁止；为空代表无禁止；禁止列表中有匹配，代表不允许此格式
                    InnerToaster.obj(mActivity).show("文件格式不支持" + fix.toUpperCase());
                    return false;
                }
            }
        }

        // 单张选择大小限制
        float len = imagePicker.getSelectLimitSize();
        if (len > 0f && imageItem.size > len * 1024 * 1024) {
            // 大小限制打开，检查大小是否超限
            InnerToaster.obj(mActivity).show("文件大小不能超过" + ((int) len) + "M");
            return false;
        }

        return true;
    }

    private class CameraViewHolder extends ViewHolder{

        View mItemView;

        CameraViewHolder(View itemView) {
            super(itemView);
            mItemView = itemView;
        }

        void bindCamera(){
            mItemView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mImageSize));
            mItemView.setTag(null);
            mItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!((ImageBaseActivity) mActivity).checkPermission(Manifest.permission.CAMERA)) {
                        ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CAMERA}, ImageGridActivity.REQUEST_PERMISSION_CAMERA);
                    } else {
                        imagePicker.takePicture(mActivity, ImagePicker.REQUEST_CODE_TAKE);
                    }
                }
            });
        }
    }

}
