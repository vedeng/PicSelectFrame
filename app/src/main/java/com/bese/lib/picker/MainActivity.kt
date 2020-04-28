package com.bese.lib.picker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bese.lib.picker.net.PicUploadRequest
import com.bese.lib.picker.net.PicUploadResponse
import com.blankj.utilcode.util.ToastUtils
import com.netlib.BaseCallback
import com.netlib.upload.ProcessCallback
import com.pic.picker.ImagePicker
import com.pic.picker.bean.ImageItem
import kotlinx.android.synthetic.main.activity_main.*
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
    }

    private var picHelper: PicShowHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()

        picHelper = PicShowHelper(this, main_rec)
        picHelper?.setPickerRole(isMultiMode = true, selectLimit = 6, selectLimitSize = 4f, isFilterSelectFormat = true, formatAllowCollection = arrayListOf("jpg", "jpeg", "png", "bmp"))
        picHelper?.buildHelper()

    }

    private fun init() {
        ImagePicker.getInstance().imageLoader = GlideImageLoader()

        main_select?.setOnClickListener {
            picHelper?.openSelect()
        }
        rb_select_with_list?.setOnCheckedChangeListener { _, ch ->
            PicShowHelper.SELECT_WITH_LIST = ch
        }
        rb_select_with_sort?.setOnCheckedChangeListener { _, ch ->
            PicShowHelper.SELECT_WITH_SORT = ch
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODE_SELECT) {
            val restList = picHelper?.selectPicResult(data)

            setSelectPicPaths(restList)

            // 自行过滤重复，再上传
            restList?.forEach {
                batchUploadPic(it.path)
            }
        } else if (requestCode == CODE_PREVIEW) {
            // 预览选择不会有新增Item，只可能缩减列表长度，所以不需要去重上传。但是可用于其他用途
            val restList = picHelper?.previewPicResult(data)

            setSelectPicPaths(restList)
        }
    }

    private fun setSelectPicPaths(list: ArrayList<ImageItem>?) {
        // 已选图片路径展示
        main_select_list?.run {
            var txt = ""
            list?.forEach {
                if (it.name != PicShowHelper.ADD_FLAG) {
                    txt = txt.plus(it.path + "\n\n")
                }
            }
            text = txt
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
                val item = picHelper?.getSelectPathList()?.find {  it.path.contains(flag) }
                item?.flag = progress
                picHelper?.updateItemData(item)
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
