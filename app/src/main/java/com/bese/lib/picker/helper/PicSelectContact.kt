package com.bese.lib.picker.helper

import androidx.fragment.app.FragmentActivity
import com.pic.picker.bean.ImageItem
import java.util.*

/**
 * 公司认证View层
 */
interface PicSelectContact {

    fun getCurrentActivity(): FragmentActivity?
    fun openTakePhotoActivity(type: Int)        // type作为一个可选标记，如果存在多种选框，用来区分哪一个
    fun openGalleryActivity(type: Int, hasSelectList: ArrayList<ImageItem>?)

    /**
     *  选图助手移除一个Item，部分场景下需要通知到调用方，移除上传或其他处理队列
     */
    fun removeImageItem(type: Int, item: ImageItem?)
}
