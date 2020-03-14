package com.bese.lib.picker

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bese.lib.picker.net.PicUploadRequest
import com.bese.lib.picker.net.PicUploadResponse
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
import com.netlib.BaseCallback
import com.netlib.upload.ProcessCallback
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_grid_pic.view.*
import retrofit2.Call
import java.io.File

/**
 * <>
 *
 * @author bei deng
 */
class MainActivity : AppCompatActivity() {

    companion object {
        const val CODE_SELECT = 101
        const val CODE_PREVIEW = 102

        const val ADD_FLAG = "add+"
        const val FLAG_PICKER_LIMIT = 6

        var SELECT_WITH_LIST = false
    }

    /** 当前选择的所有图片 */
    private val selectPathList: ArrayList<ImageItem> = ArrayList()

    private var mPhotoPopupWindow: PhotoPopupWindow? = null

    private var hasAddIcon = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        appendAddIconToSelectList()

        ImagePicker.getInstance().imageLoader = GlideImageLoader()

        main_select?.setOnClickListener {
            openSelect()
        }
        rb_select_with_list?.setOnCheckedChangeListener { _, ch ->
            SELECT_WITH_LIST = ch
        }

        main_rec?.adapter = picAdapter

        picAdapter.addData(selectPathList)
    }

    private fun openSelect() {
        mPhotoPopupWindow = PhotoPopupWindow(this, View.OnClickListener {
            XXPermissions.with(this).permission(Manifest.permission.CAMERA).request(object : OnPermission {
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
            XXPermissions.with(this).permission(Manifest.permission.READ_EXTERNAL_STORAGE).request(object : OnPermission {
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

    private fun gotoPicPickerPage(isCamera: Boolean) {
        if (isCamera) {
            // 相机
            ImagePicker.getInstance().run {
                selectLimit = 1
                isMultiMode = false
                isCrop = false
                val intent = Intent(this@MainActivity, ImageGridActivity::class.java).putExtra(ImageGridActivity.EXTRAS_TAKE_PICKERS, true)
                startActivityForResult(intent, CODE_SELECT)
            }
        } else {
            // 图库
            ImagePicker.getInstance().run {
                val intent = Intent(this@MainActivity, ImageGridActivity::class.java)
                if (SELECT_WITH_LIST) {
                    // 如果定制进入相册就选中之前的已选，需要传参。并且需要更改接收返回值时清空之前已选列表。选择上限也需要改。
                    intent.putExtra(
                        ImageGridActivity.EXTRAS_IMAGES,
                        selectPathList.filter { it.name != ADD_FLAG } as? ArrayList<ImageItem>)
                    selectLimit = FLAG_PICKER_LIMIT - selectPathList.size + (if (hasAddIcon) 1 else 0)
                } else {
                    selectLimit = FLAG_PICKER_LIMIT
                }
                isMultiMode = true
                isSelectLimitShowDialog = true      // 选图超限 以弹窗提示
                isFilterSelectFormat = true     // 开启选图限定类型功能
                formatAllowCollection = arrayListOf("jpg", "jpeg", "png", "bmp")    // 定义选图的允许类型
                selectLimitSize = 3f        // 选图大小限制参数，单位M
                startActivityForResult(intent, CODE_SELECT)
            }
        }
    }

    private fun gotoPreviewPic(currentPosition: Int) {
        //打开预览
        val preview: ArrayList<ImageItem> = selectPathList.filter { it.name != ADD_FLAG }.toMutableList() as ArrayList<ImageItem>
        val intentPreview = Intent(this, ImagePreviewDelActivity::class.java)
            .putExtra(ImagePicker.EXTRA_IMAGE_ITEMS, preview)
            .putExtra(ImagePicker.EXTRA_SELECTED_IMAGE_POSITION, if (currentPosition < 0) 0 else currentPosition)
            .putExtra(ImagePicker.EXTRA_FROM_ITEMS, true)
        startActivityForResult(intentPreview, CODE_PREVIEW)
    }

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
                        if (item.flag in 1..99) {
                            helper.itemView.item_upload_process?.visibility = View.VISIBLE
                            helper.itemView.item_mask?.visibility = View.VISIBLE
                            helper.itemView.item_upload_process?.progress = item.flag
                        } else {
                            helper.itemView.item_upload_process?.visibility = View.GONE
                            helper.itemView.item_mask?.visibility = View.GONE
                        }
                        helper.itemView.item_img?.run {
                            setOnClickListener { gotoPreviewPic(helper.adapterPosition) }
                            GlideImageLoader().displayImage(this@MainActivity, item.path, this, 0, 0)
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

    private fun removeFromList(item: ImageItem?) {
        selectPathList.remove(item)
        appendAddIconToSelectList()
        updateData(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODE_SELECT) {
            // 选图
            val list: ArrayList<ImageItem>? = data?.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS) as? ArrayList<ImageItem>
            if (list?.size ?: 0 > 0) {
                if (SELECT_WITH_LIST) {
                    // 带着列表选图，会有数据重复。需要清空当前列表
                    selectPathList.clear()
                } else {
                    // 添加到现有列表
                    selectPathList.removeAll(selectPathList.filter { it.name == ADD_FLAG })
                }

                selectPathList.addAll(list!!)
                appendAddIconToSelectList()
                replaceRecData(selectPathList)
                setSelectPicPaths()
                // 上传选中图片 - 支持批量
                list.forEach {
                    Log.e("返回列表1=====", " -- ${it.name}")
                    batchUploadPic(it.path)
                }
            }
        } else if (requestCode == CODE_PREVIEW) {
            val list: ArrayList<ImageItem>? = data?.getSerializableExtra(ImagePicker.EXTRA_IMAGE_ITEMS) as? ArrayList<ImageItem>

            if (list?.size ?: 0 > 0) {
                selectPathList.clear()
                selectPathList.addAll(list!!)
                appendAddIconToSelectList()
                replaceRecData(selectPathList)
                setSelectPicPaths()
                // 上传选中图片
                list.forEach {
                    Log.e("返回列表2=====", " -- ${it.name}")
                    batchUploadPic(it.path)
                }
            }
        }
    }

    private fun appendAddIconToSelectList() {
        // 先判断列表是否包含值
        if (selectPathList.any { it.name == ADD_FLAG }) {
            selectPathList.removeAll(selectPathList.filter { it.name == ADD_FLAG })
        }
        // 再判断列表是否需要添加一个加号图标
        if (selectPathList.size < FLAG_PICKER_LIMIT) {
            selectPathList.add(ImageItem().apply { name = ADD_FLAG; path = "" })
            hasAddIcon = true
        } else {
            hasAddIcon = false
        }
    }

    private fun setSelectPicPaths() {
        // 已选图片路径展示
        main_select_list?.run {
            var txt = ""
            selectPathList.forEach {
                if (it.name != ADD_FLAG) {
                    txt = txt.plus(it.path + "\n\n")
                }
            }
            text = txt
        }
    }

    private var interval: Long = 0L
    private fun updateData(withDone: Boolean) {
        val c = System.currentTimeMillis()
        if (c - interval > 150 || withDone) {
            interval = c
            replaceRecData(selectPathList)
        }
    }

    private fun replaceRecData(list: ArrayList<ImageItem>?) {
        list?.run {
            forEach {
                Log.e("展示====", it.name + "    -----      " + it.path)
            }
            picAdapter.replaceData(list)
        }
    }

    /**
     * 带进度回调的上传请求
     */
    private fun batchUploadPic(filePath: String?) {
        val file = File(filePath ?: "")
        if (!file.exists()) {
            ToastUtils.showShort("$filePath 上传失败，文件不存在")
            return
        }
        val callback = object : ProcessCallback {
            override fun onProgress(progress: Int, flag: String) {
                super.onProgress(progress, flag)
                Log.e("进度回调===", "$progress -- $flag")
                selectPathList.find { it.path.contains(flag) }?.flag = progress
                updateData(progress == 100)
            }
        }
        PicUploadRequest(file, callback).request(PicUploadRequest.Param(file), object : BaseCallback<PicUploadResponse>() {
            override fun onSuccess(response: PicUploadResponse?) {
                ToastUtils.showShort(response?.data?.filePath ?: "-------")
            }

            override fun onFailure(call: Call<PicUploadResponse>, t: Throwable) {
                super.onFailure(call, t)
                ToastUtils.showShort(t.message)
            }
        })
    }

}
