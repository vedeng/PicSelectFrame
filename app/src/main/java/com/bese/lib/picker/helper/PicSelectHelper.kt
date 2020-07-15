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
import java.util.*

/**
 * 图片选择处理层
 * @param type 一个页面可能有多个选择框列表，需要实例化多个Helper，type用于区分哪个Helper做出的动作
 */
class PicSelectHelper(private var view: PicSelectContact?, var type: Int = 0) {

    companion object {
        const val FLAG_ADD_NAME = "-"
        const val FLAG_ADD_PATH = "/-"
        const val FLAG_ADD_MASK = "+"

        var SELECT_WAY_CAMERA = true
        // 标记：选择图库时，带入已选列表，并默认选中已选项
        var SELECT_WITH_LIST = false
        // 标记：选择后是否使用数字排序标记已选，为true时，选中的项会根据先后顺序用阿拉伯数字排列
        var SELECT_WITH_SORT = false
    }

    /**
     * 选图上限
     */
    private var pickerLimitCount = 3

    /**
     * 图片选择模式：默认一次可以多选
     */
    private var singleSelectFlag = false

    /**
     * 单图预览
     */
    private var previewSingleFlag = false

    /** 当前选择的图片列表 */
    private var selectPicList: ArrayList<ImageItem> = ArrayList()

    private var hasAddIcon = false

    /**
     * 初始化操作
     */
    fun initPicAdapter() {
        selectPicList.clear()
        appendAddIconToSelectList()
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
     * 指定列表设置值
     */
    fun setSelectPicList(picList: ArrayList<ImageItem>?) {
        this.selectPicList.clear()
        picList?.forEach { selectPicList.add(it) }
        appendAddIconToSelectList()
        picSelectAdapter.setList(selectPicList)
    }

    /**
     * 添加到当前的列表
     */
    fun addToSelectList(picList: ArrayList<ImageItem>?) {
        picList?.forEach {
            if (selectPicList.none { p -> p.path == it.path }) {
                selectPicList.add(ImageItem().apply { name = it.name; path = it.path })
            }
        }
        appendAddIconToSelectList()
        picSelectAdapter.setList(selectPicList)
    }

    /**
     * 手动操作删除
     *      删除时可能图片未上传完成。所以没有操作上传列表。提交时需要过滤
     */
    fun removeFromList(item: ImageItem?) {
        view?.removeImageItem(type, item)
        selectPicList.remove(item)
        appendAddIconToSelectList()
        picSelectAdapter.setList(selectPicList)
    }

    /**
     * 设置个数上限
     */
    fun setPickerLimit(num: Int) {
        pickerLimitCount = if (num < 1) 1 else num
        appendAddIconToSelectList()
        picSelectAdapter.setList(selectPicList)
    }

    /**
     * 设置是否单选
     */
    fun setSingleSelectType(isSingleSelect: Boolean) {
        singleSelectFlag = isSingleSelect
    }

    /**
     * 设置是否单图预览
     */
    fun setPreviewSingle(isPreviewSingle: Boolean) {
        previewSingleFlag = isPreviewSingle
    }

    var picSelectAdapter = object : BaseQuickAdapter<ImageItem?, BaseViewHolder>(R.layout.item_grid_pic_upload) {
        override fun convert(holder: BaseViewHolder, item: ImageItem?) {
            item?.run {
                if (item.mask == FLAG_ADD_MASK) {
                    // 展示ADD图
                    holder.itemView.item_img?.setImageResource(R.drawable.ic_add_icon)
                    holder.itemView.item_img?.setOnClickListener { openPicPicker() }
                    holder.itemView.item_mask?.visibility = View.GONE
                    holder.itemView.item_pic_delete?.visibility = View.GONE
                    holder.itemView.item_upload_process?.visibility = View.GONE
                } else {
                    // 展示库图，可预览
                    if (item.path.isNotEmpty()) {
                        holder.itemView.item_pic_delete?.visibility = View.VISIBLE
                        holder.itemView.item_pic_delete?.setOnClickListener {
                            removeFromList(item)
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
                            setOnClickListener { preview(selectPicList, holder.adapterPosition) }
                            PickerGlideImageLoader().displayImage(item.path, this)
                        }
                    } else {
                        // 展示错误图
                        holder.itemView.item_img?.setImageResource(R.drawable.img_placeholder)
                        holder.itemView.item_img?.setOnClickListener { preview(selectPicList, holder.adapterPosition) }
                        holder.itemView.item_pic_delete?.visibility = View.GONE
                        holder.itemView.item_upload_process?.visibility = View.GONE
                    }
                }
            }
        }
    }

    /**
     * 大图预览方法
     */
    private fun preview(list: ArrayList<ImageItem>, pos: Int) {
        view?.getCurrentActivity()?.run {
            var picList: ArrayList<String?>? = arrayListOf()
            if (previewSingleFlag) {
                picList = arrayListOf(list[pos].path)
            } else {
                picList = list.filter { it.path != FLAG_ADD_PATH }.map { it.path } as? ArrayList<String?>
            }
            PreviewDialog(this, picList, if (pos > 0) pos else 0)
                    .show(supportFragmentManager, "CompanyAuthPreview")
        }
    }

    /**
     * 处理添加加号图标
     */
    private fun appendAddIconToSelectList() {
        // 先判断列表是否包含值
        if (selectPicList.any { it.mask == FLAG_ADD_MASK }) {
            selectPicList.removeAll(selectPicList.filter { it.mask == FLAG_ADD_MASK })
        }
        // 再判断列表是否需要添加一个加号图标
        if (selectPicList.size < pickerLimitCount) {
            selectPicList.add(ImageItem().apply { name = FLAG_ADD_NAME; path = FLAG_ADD_PATH; mask = FLAG_ADD_MASK })
            hasAddIcon = true
        } else {
            hasAddIcon = false
        }
    }

    /**
     * 打开图片选择弹窗：相机/相册
     */
    fun openPicPicker() {
        view?.getCurrentActivity()?.run {
            PicPickerDialog(View.OnClickListener {
                requireCameraPermission(this)
            }, View.OnClickListener {
                requireGalleryPermission(this, false)
            }).show(supportFragmentManager, "PicPicker")
        }
    }

    /**
     * 相机权限
     */
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

    /**
     * 画廊权限
     */
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

    /**
     * 打开外部页面去拾取图片数据
     */
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
                val hasSelectList = selectPicList.filter { it.mask != FLAG_ADD_MASK } as? ArrayList<ImageItem>
                selectLimit = if (singleSelectFlag) 1 else (pickerLimitCount - picSelectAdapter.data.size + (if (hasAddIcon) 1 else 0) + (hasSelectList?.size ?: 0))
                isCrop = false
                isMultiMode = true
                isSelectLimitShowDialog = true
                isFilterSelectFormat = true
                formatAllowCollection = arrayListOf("jpg", "jpeg", "png", "bmp")
                selectLimitSize = 20f
                isSelectPicWithSortNumber = SELECT_WITH_SORT
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