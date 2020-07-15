package com.bese.lib.picker

import android.app.Activity
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.pic.picker.loader.ImageLoader

/**
 * Glide封装使用 图片选择库使用
 */
class PickerGlideImageLoader : ImageLoader {

    fun displayImage(path: Any?, imageView: ImageView?) {
        try {
            imageView?.run { Glide.with(this).load(path).into(this) }
        } catch (e: Exception) {
            Log.e("GlideImageLoader Error:", e.message + "")
        }
    }

    override fun displayImage(activity: Activity, path: String?, imageView: ImageView?, width: Int, height: Int) {
        displayImage(path, imageView)
    }

    override fun displayImagePreview(activity: Activity, path: String?, imageView: ImageView?, width: Int, height: Int) {
        displayImage(path, imageView)
    }

    override fun clearMemoryCache() {}
}