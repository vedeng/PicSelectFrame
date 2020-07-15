package com.bese.lib.picker.helper

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.view.View
import com.bese.lib.picker.PickerGlideImageLoader
import com.bese.lib.picker.R
import com.bese.widget.dialog.DialogListener
import com.bese.widget.dialog.PicPickerDialog
import com.bese.widget.dialog.PreviewDialog
import com.bese.widget.dialog.XDialog
import com.blankj.utilcode.util.PermissionUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.hjq.permissions.OnPermission
import com.hjq.permissions.XXPermissions
import com.pic.picker.ImagePicker
import com.pic.picker.bean.ImageItem
import kotlinx.android.synthetic.main.item_grid_pic_upload.view.*

/**
 * 图片选择处理层
 * @param type 一个页面可能有多个选择框列表，需要实例化多个Helper，type用于区分哪个Helper做出的动作
 */
class PicSelectMultiAddHelper(private var view: PicSelectContact?, var type: Int = 0) {

    companion object {
        const val FLAG_ADD_NAME = "-"
        const val FLAG_ADD_PATH = "/-"
        const val FLAG_ADD_MASK = "+"

        var FLAG_PICKER_LIMIT = 3

        var SELECT_WAY_CAMERA = true

    }

    /** 当前选择的图片列表 */
    private var selectPicList: ArrayList<ImageItem> = ArrayList()

    private var currentPosition = -1

    fun initPicAdapter() {
        selectPicList.clear()
        for (i in 1..FLAG_PICKER_LIMIT) {
            selectPicList.add(ImageItem().apply { name = FLAG_ADD_NAME; path = FLAG_ADD_PATH; mask = FLAG_ADD_MASK })
        }
        picSelectAdapter.setList(selectPicList)
    }

    /**
     * 获取当前选中列表
     */
    fun getSelectPicList(withAddIcon: Boolean = false) : ArrayList<ImageItem>? {
        return if (withAddIcon) {
            selectPicList
        } else {
            selectPicList.filter { it.path != FLAG_ADD_PATH } as ArrayList<ImageItem>
        }
    }

    /**
     * 更新一个位置的图片数据
     */
    fun updateSelectItem(picItem: ImageItem, position: Int = currentPosition) {
        if (position in 0 until selectPicList.size) {
            selectPicList[position] = picItem
        }
        picSelectAdapter.setList(selectPicList)
    }

    /**
     * 手动操作删除
     *      删除时可能图片未上传完成。所以没有操作上传列表。提交时需要过滤
     */
    private fun removeFromList(position: Int) {
        if (currentPosition in 0 until selectPicList.size) {
            selectPicList[position] = ImageItem().apply { name = FLAG_ADD_NAME; path = FLAG_ADD_PATH; mask = FLAG_ADD_MASK }
        }
        picSelectAdapter.setList(selectPicList)
    }

    /**
     * 设置上限
     */
    fun setPickerLimit(num: Int) {
        FLAG_PICKER_LIMIT = if (num <= 1) 1 else num
        val newList = ArrayList<ImageItem>()
        for (i in 0 until FLAG_PICKER_LIMIT) {
            if (i < selectPicList.size) {
                newList.add(selectPicList[i])
            } else {
                newList.add(ImageItem().apply { name = FLAG_ADD_NAME; path = FLAG_ADD_PATH; mask = FLAG_ADD_MASK })
            }
        }
        selectPicList = newList
        picSelectAdapter.setList(selectPicList)
    }

    var picSelectAdapter = object : BaseQuickAdapter<ImageItem?, BaseViewHolder>(R.layout.item_grid_pic_upload) {
        override fun convert(holder: BaseViewHolder, item: ImageItem?) {
            item?.run {
                if (item.mask == FLAG_ADD_MASK) {
                    // 展示ADD图
                    holder.itemView.item_img?.setImageResource(R.drawable.ic_add_icon)
                    holder.itemView.item_img?.setOnClickListener { openPicPicker(holder.layoutPosition) }
                    holder.itemView.item_mask?.visibility = View.GONE
                    holder.itemView.item_pic_delete?.visibility = View.GONE
                    holder.itemView.item_upload_process?.visibility = View.GONE
                } else {
                    // 展示库图，可预览
                    if (item.path.isNotEmpty()) {
                        holder.itemView.item_pic_delete?.visibility = View.VISIBLE
                        holder.itemView.item_pic_delete?.setOnClickListener {
                            removeFromList(holder.layoutPosition)
                        }
                        if (item.flag in 1..99) {
                            holder.itemView.item_upload_process?.visibility = View.VISIBLE
                            holder.itemView.item_mask?.visibility = View.VISIBLE
                            holder.itemView.item_upload_process?.progress = item.flag
                        } else {
                            holder.itemView.item_upload_process?.visibility = View.GONE
                            holder.itemView.item_mask?.visibility = View.GONE
                        }
                        holder.itemView.item_img?.run {
                            setOnClickListener { singlePreview(item.path) }
                            PickerGlideImageLoader().displayImage(item.path, this)
                        }
                    } else {
                        // 展示错误图
                        holder.itemView.item_img?.setImageResource(R.drawable.img_placeholder)
                        holder.itemView.item_img?.setOnClickListener { singlePreview(item.path) }
                        holder.itemView.item_pic_delete?.visibility = View.GONE
                        holder.itemView.item_upload_process?.visibility = View.GONE
                    }
                }
            }
        }
    }

    /**
     * 单图预览
     */
    private fun singlePreview(pic: String?) {
        view?.getCurrentActivity()?.run {
            PreviewDialog(this, arrayListOf(pic), 0)
                    .show(supportFragmentManager, "CompanyAuthPreview")
        }
    }

    /**
     * 开启 相机/相册 弹窗
     */
    fun openPicPicker(position: Int) {
        view?.getCurrentActivity()?.run {
            PicPickerDialog(View.OnClickListener {
                requireCameraPermission(this)
            }, View.OnClickListener {
                requireGalleryPermission(this, false)
            }).show(supportFragmentManager, "PicPicker")
        }
        currentPosition = position
    }

    fun getOperatePosition() : Int { return currentPosition }

    fun requireCameraPermission(act: Activity?) {
        act?.run {
            XXPermissions.with(this).permission(Manifest.permission.CAMERA).request(object : OnPermission {
                override fun noPermission(denied: MutableList<String>?, quick: Boolean) {
                    if (quick) {
                        XDialog(this@run)
                                .setTitle("开启相机权限")
                                .setMessage("未获得相机权限，导致无法拍摄照片")
                                .setEnterText("去开启")
                                .setEnterTextColor(Color.parseColor("#0099ff"))
                                .setListener(object : DialogListener {
                                    override fun doEnter(view: Dialog?) {
                                        PermissionUtils.launchAppDetailsSettings()                     // 跳到设置权限设置页面
                                    }
                                })
                                .build()
                    }
                }

                override fun hasPermission(granted: MutableList<String>?, isAll: Boolean) {
                    // 先相机权限，再存储权限
                    requireGalleryPermission(act, true)
                }
            })
        }

    }

    fun requireGalleryPermission(act: Activity?, fromCamera: Boolean) {
        act?.run {
            XXPermissions.with(this).permission(Manifest.permission.WRITE_EXTERNAL_STORAGE).request(object : OnPermission {
                override fun noPermission(denied: MutableList<String>?, quick: Boolean) {
                    if (quick) {
                        XDialog(this@run)
                                .setTitle("开启照片权限")
                                .setMessage("未获得照片权限，导致无法上传图片")
                                .setEnterText("去开启")
                                .setEnterTextColor(Color.parseColor("#0099ff"))
                                .setListener(object : DialogListener {
                                    override fun doEnter(view: Dialog?) {
                                        PermissionUtils.launchAppDetailsSettings()                  // 跳到设置权限设置页面
                                    }
                                })
                                .build()
                    }
                }

                override fun hasPermission(granted: MutableList<String>?, isAll: Boolean) {
                    gotoPicPickerPage(fromCamera)
                }
            })
        }
    }

    fun gotoPicPickerPage(isCamera: Boolean) {
        if (ImagePicker.getInstance().imageLoader == null) {
            ImagePicker.getInstance().imageLoader = PickerGlideImageLoader()
        }
        if (isCamera) {                 // 相机
            ImagePicker.getInstance().run {
                selectLimit = 1
                isMultiMode = false
                isCrop = false
                view?.openTakePhotoActivity(type)
            }
        } else {                                // 图库
            ImagePicker.getInstance().run {
                selectLimit = 1
                isCrop = false
                isMultiMode = true
                isSelectLimitShowDialog = true
                isFilterSelectFormat = true
                formatAllowCollection = arrayListOf("jpg", "jpeg", "png", "bmp")
                selectLimitSize = 20f
                isSelectPicWithSortNumber = false
                val hasSelectList = selectPicList.filter { it.mask != FLAG_ADD_MASK } as? ArrayList<ImageItem>
                view?.openGalleryActivity(type, hasSelectList)
            }
        }
        // 记录本地选择，是相机单选 还是图库多选
        SELECT_WAY_CAMERA = isCamera
    }

    init {
        initPicAdapter()
    }
}
