package com.bese.lib.picker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bese.lib.picker.view.PhotoPopupWindow
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.hjq.permissions.OnPermission
import com.hjq.permissions.XXPermissions
import com.pic.picker.ImagePicker
import com.pic.picker.bean.ImageItem
import com.pic.picker.ui.ImageGridActivity
import com.pic.picker.ui.ImagePreviewDelActivity
import kotlinx.android.synthetic.main.item_grid_pic.view.*

/**
 * 图片选取展示控制器
 *      ：打开图库选择，选取后回调Activity的onResult，如果Activity想要获取选择的图片路径数据，可以拦截data
 *      Activity的onResult调用Helper的图片展示处理：
 *              selectPicResult()
 *                      处理图片在RecyclerView上的展示，自动判断和处理是否添加加号图标，自动展示图片，并带有预览和删除功能。
 *              previewPicResult()
 *                      图片大图预览回调，如果使用预设的预览功能就可以在预览中使用图片删除功能。
 *                      如果需要自定义预览，可以获取图片列表自行处理。
 *
 * @author Fires
 */
class PicShowHelper(private var mAct: Activity?, private var mRecView: RecyclerView?) {

    /** 弹窗选择是拍摄还是图库 */
    private var mPhotoPopupWindow: PhotoPopupWindow? = null

    /** 当前选择的所有图片 */
    private val selectPathList: ArrayList<ImageItem> = ArrayList()

    /** 是否展示了加号图标 */
    private var hasAddIcon = true

    //图片选择控件的选择数量上限
    private var mSelectLimit = 6
    //图片选择控件的选择模式，默认单选
    private var mSelectIsMultiMode = false
    //图片选择超限的提示类型，默认吐司，可选弹窗
    private var mLimitWillShowDialog = false
    //图片选择控件的单图大小上限，默认0 不限制。大于0时，以M为单位做限制
    private var mSelectLimitSize = 0f
    //图片选择控件的单图格式限制开关, 默认false，采用默认限制
    private var mFilterFormat = false
    //图片选择控件的单图格式限制允许格式
    private var mFormatAllowList = arrayListOf<String>()
    //图片选择控件的单图格式限制不允许格式
    private var mFormatDisallowList = arrayListOf<String>()

    /**
     * 图片拾取适配器
     */
    private var picAdapter = object : BaseQuickAdapter<ImageItem?, BaseViewHolder>(R.layout.item_grid_pic) {
        override fun convert(helper: BaseViewHolder, item: ImageItem?) {
            item?.run {
                if (item.name == ADD_FLAG) {
                    Log.e("设置add图====", "-----")
                    // 展示ADD图
                    helper.itemView.item_img?.setImageResource(R.drawable.ic_add)
                    helper.itemView.item_img?.setOnClickListener { openSelect() }
                    helper.itemView.item_delete?.visibility = View.GONE
                    helper.itemView.item_upload_process?.visibility = View.GONE
                } else {
                    // 展示库图，可预览
                    if (item.path.isNotEmpty()) {
                        helper.itemView.item_delete?.visibility = View.VISIBLE
                        helper.itemView.item_delete?.setOnClickListener {
                            removeFromList(item)
                        }
                        if (item.flag in 1..99) {       // 如不使用进度，进度会是0，会直接隐藏。
                            helper.itemView.item_upload_process?.visibility = View.VISIBLE
                            helper.itemView.item_mask?.visibility = View.VISIBLE
                            helper.itemView.item_upload_process?.progress = item.flag
                        } else {
                            helper.itemView.item_upload_process?.visibility = View.GONE
                            helper.itemView.item_mask?.visibility = View.GONE
                        }
                        helper.itemView.item_img?.run {
                            setOnClickListener { gotoPreviewPic(helper.adapterPosition) }
                            GlideImageLoader().displayImage(mAct, item.path, this, 0, 0)
                        }
                    } else {
                        // 展示错误图
                        helper.itemView.item_img?.setImageResource(R.drawable.ic_placeholder)
                        helper.itemView.item_img?.setOnClickListener { gotoPreviewPic(helper.adapterPosition) }
                        helper.itemView.item_delete?.visibility = View.GONE
                        helper.itemView.item_upload_process?.visibility = View.GONE
                    }
                }
            }
        }
    }

    /**
     * 打开图片选择入口：内部有权限判断逻辑
     *      存储权限，使用 WRITE_EXTERNAL_STORAGE  自动带有Read权限。
     *      如是用了 READ_EXTERNAL_STORAGE  不会自动带有Write权限
     */
    fun openSelect() {
        mAct?.run {
            mPhotoPopupWindow = PhotoPopupWindow(this, View.OnClickListener {
                XXPermissions.with(mAct).permission(Manifest.permission.CAMERA).request(object : OnPermission {
                    override fun noPermission(denied: MutableList<String>?, quick: Boolean) {
                        if (quick) { ToastUtils.showShort("缺少相机权限，请去设置页面打开相应权限") }
                        mPhotoPopupWindow?.dismiss()
                    }
                    override fun hasPermission(granted: MutableList<String>?, isAll: Boolean) {
                        gotoPicPickerPage(true)
                        mPhotoPopupWindow?.dismiss()
                    }
                })
            }, View.OnClickListener {
                XXPermissions.with(this).permission(Manifest.permission.WRITE_EXTERNAL_STORAGE).request(object : OnPermission {
                    override fun noPermission(denied: MutableList<String>?, quick: Boolean) {
                        if (quick) { ToastUtils.showShort("缺少写入存储权限，请去设置页面打开相应权限") }
                        mPhotoPopupWindow?.dismiss()
                    }
                    override fun hasPermission(granted: MutableList<String>?, isAll: Boolean) {
                        gotoPicPickerPage(false)
                        mPhotoPopupWindow?.dismiss()
                    }
                })
            })
            val rootView = LayoutInflater.from(this).inflate(R.layout.activity_main, null)
            mPhotoPopupWindow?.showAtLocation(rootView, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0)
        }
    }

    /**
     * 调用三方工具，展示设备图片
     */
    private fun gotoPicPickerPage(isCamera: Boolean) {
        if (isCamera) {     // 相机
            ImagePicker.getInstance().run {
                selectLimit = 1
                isMultiMode = false
                isCrop = false
                val intent = Intent(mAct, ImageGridActivity::class.java).putExtra(ImageGridActivity.EXTRAS_TAKE_PICKERS, true)
                mAct?.startActivityForResult(intent, MainActivity.CODE_SELECT)
            }
        } else {    // 图库
            ImagePicker.getInstance().run {
                val intent = Intent(mAct, ImageGridActivity::class.java)
                if (SELECT_WITH_LIST) {     // 如果定制进入相册就选中之前的已选，需要传参。并且需要更改接收返回值时清空之前已选列表。选择上限也需要改。
                    intent.putExtra(ImageGridActivity.EXTRAS_IMAGES, selectPathList.filter { it.name != ADD_FLAG } as? ArrayList<ImageItem>)
                    selectLimit = mSelectLimit
                } else {
                    selectLimit = mSelectLimit - selectPathList.size + (if (hasAddIcon) 1 else 0)
                }
                isMultiMode = mSelectIsMultiMode
                isSelectLimitShowDialog = mLimitWillShowDialog      // 选图超限 以弹窗提示
                isFilterSelectFormat = mFilterFormat     // 开启选图限定类型功能
                formatAllowCollection = mFormatAllowList    // 定义选图的允许类型
                selectLimitSize = mSelectLimitSize        // 选图大小限制参数，单位M
                isSelectPicWithSortNumber = SELECT_WITH_SORT
                mAct?.startActivityForResult(intent, MainActivity.CODE_SELECT)
            }
        }
    }

    /**
     * 图库选择回调处理：
     *      回调会从Activity的onResult中转一下，可以在Activity中拦截和使用数据
     *
     *      @return 选中整个列表
     */
    fun selectPicResult(data: Intent?) : ArrayList<ImageItem>? {
        val list: ArrayList<ImageItem>? = data?.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS) as? ArrayList<ImageItem>
        if (list?.size ?: 0 > 0) {
            if (SELECT_WITH_LIST) {     // 带着列表选图，会有数据重复。需要清空当前列表
                selectPathList.clear()
            } else { // 添加到现有列表
                selectPathList.removeAll(selectPathList.filter { it.name == ADD_FLAG })
            }
            selectPathList.addAll(list!!)
            appendAddIconToSelectList()
            replaceRecData(selectPathList)
        }
        return list
    }

    /**
     * 图片预览回调处理：
     *      在预览后删除了图片，执行了onResult后，需要调用此方法更新RecyclerView的数据展示
     */
    fun previewPicResult(data: Intent?) : ArrayList<ImageItem>? {
        val list: ArrayList<ImageItem>? = data?.getSerializableExtra(ImagePicker.EXTRA_IMAGE_ITEMS) as? ArrayList<ImageItem>
        if (list?.size ?: 0 > 0) {
            selectPathList.clear()
            selectPathList.addAll(list!!)
            appendAddIconToSelectList()
            replaceRecData(selectPathList)
        }
        return list
    }

    /**
     * 图开图片预览：使用的是预设的预览功能。带有删除功能。会回调Activity的onResult
     */
    private fun gotoPreviewPic(currentPosition: Int) {
        //打开预览
        val preview: ArrayList<ImageItem> = selectPathList.filter { it.name != ADD_FLAG }.toMutableList() as ArrayList<ImageItem>
        val intentPreview = Intent(mAct, ImagePreviewDelActivity::class.java)
            .putExtra(ImagePicker.EXTRA_IMAGE_ITEMS, preview)
            .putExtra(ImagePicker.EXTRA_SELECTED_IMAGE_POSITION, if (currentPosition < 0) 0 else currentPosition)
            .putExtra(ImagePicker.EXTRA_FROM_ITEMS, true)
        mAct?.startActivityForResult(intentPreview, MainActivity.CODE_PREVIEW)
    }

    /**
     * 获取列表的真实数据，过滤了加号Item
     */
    fun getSelectPathList() : ArrayList<ImageItem>? {
        return selectPathList.filter { it.name != ADD_FLAG } as? ArrayList<ImageItem>
    }

    /**
     * 列表自适应添加加号Item
     */
    private fun appendAddIconToSelectList() {
        // 先判断列表是否包含值
        if (selectPathList.any { it.name == ADD_FLAG }) {
            selectPathList.removeAll(selectPathList.filter { it.name == ADD_FLAG })
        }
        // 再判断列表是否需要添加一个加号图标
        if (selectPathList.size < mSelectLimit) {
            selectPathList.add(ImageItem().apply { name = ADD_FLAG; path = "" })
            hasAddIcon = true
        } else {
            hasAddIcon = false
        }
    }

    /**
     * 列表移除某个Item，移除后自动更新RecyclerView
     */
    private fun removeFromList(item: ImageItem?) {
        selectPathList.remove(item)
        appendAddIconToSelectList()
        updateData(true)
    }

    private var interval: Long = 0L
    private fun updateData(withDone: Boolean) {
        val c = System.currentTimeMillis()
        // 控制更新时间间隔不要太频繁。但如果是最后一次更新（完成时更新），需要更新
        if (c - interval > REC_UPDATE_INTERVAL || withDone) {
            interval = c
            replaceRecData(selectPathList)
        }
    }

    fun updateItemData(item: ImageItem?) {
        val progress = item?.flag ?: ""
        selectPathList.find { it.path.contains("$progress") }?.flag = item?.flag
        updateData(progress == 100)
    }

    /**
     * 更新RecyclerView数据
     */
    private fun replaceRecData(list: ArrayList<ImageItem>?) {
        list?.run {
            picAdapter.replaceData(list)
        }
    }

    fun setPickerRole(isMultiMode: Boolean = false, selectLimit: Int = 1, isSelectLimitShowDialog: Boolean = false, selectLimitSize: Float = 0f,
                      isFilterSelectFormat: Boolean = false, formatAllowCollection: ArrayList<String> = arrayListOf(), formatNotAllowCollection: ArrayList<String> = arrayListOf()) {
        mSelectLimit = selectLimit
        mSelectIsMultiMode = isMultiMode
        mLimitWillShowDialog = isSelectLimitShowDialog
        mSelectLimitSize = selectLimitSize
        mFilterFormat = isFilterSelectFormat
        mFormatAllowList = formatAllowCollection
        mFormatDisallowList = formatNotAllowCollection
    }

    fun buildHelper() {
        mRecView?.adapter = picAdapter
        // 创建时增加一个加号
        appendAddIconToSelectList()
        picAdapter.addData(selectPathList)
    }

    companion object {
        // 加号使用的名称标记
        const val ADD_FLAG = "add+"
        // 标记：选择图库时，带入已选列表，并默认选中已选项
        var SELECT_WITH_LIST = false
        // 标记：选择后是否使用数字排序标记已选，为true时，选中的项会根据先后顺序用阿拉伯数字排列
        var SELECT_WITH_SORT = false

        // RecyclerView更新时间间隔控制
        var REC_UPDATE_INTERVAL = 150L
    }

}