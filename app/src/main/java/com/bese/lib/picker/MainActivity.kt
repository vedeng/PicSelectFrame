package com.bese.lib.picker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.bese.lib.picker.helper.PicSelectContact
import com.bese.lib.picker.helper.PicSelectHelper
import com.blankj.utilcode.util.ToastUtils
import com.pic.picker.ImagePicker
import com.pic.picker.bean.ImageItem
import com.pic.picker.ui.ImageGridActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

/**
 * <>
 *
 * @author bei deng
 */
class MainActivity : AppCompatActivity(), PicSelectContact {

    companion object {
        const val CODE_SELECT = 101
        const val CODE_PREVIEW = 102
    }

    private var picHelper: PicSelectHelper? = null

    private var hasSelectList: ArrayList<ImageItem> = arrayListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
        picHelper = PicSelectHelper(this)
        picHelper?.setPickerLimit(3)
        main_rec?.adapter = picHelper?.picSelectAdapter
    }

    private fun init() {
        ImagePicker.getInstance().imageLoader = GlideImageLoader()

        main_select?.setOnClickListener { }
        rb_select_with_list?.setOnCheckedChangeListener { _, ch ->
            PicSelectHelper.SELECT_WITH_LIST = ch
        }
        rb_select_with_sort?.setOnCheckedChangeListener { _, ch ->
            PicSelectHelper.SELECT_WITH_SORT = ch
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101) {                   // 选图
            val list: ArrayList<ImageItem>? = data?.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS) as? ArrayList<ImageItem>
            picHelper?.addToSelectList(list)
            list?.forEach {
                hasSelectList.add(it)
                ToastUtils.showLong("Prepare to upload : \n ${it.path}")
            }
        }
    }

    override fun getCurrentActivity(): FragmentActivity? {
        return this
    }

    override fun openTakePhotoActivity(type: Int) {
        val intent = Intent(this, ImageGridActivity::class.java).putExtra(ImageGridActivity.EXTRAS_TAKE_PICKERS, true)
        startActivityForResult(intent, 101)
    }

    override fun openGalleryActivity(type: Int, hasSelectList: ArrayList<ImageItem>?) {
        val intent = Intent(this, ImageGridActivity::class.java)
        if (PicSelectHelper.SELECT_WITH_LIST) {     // 如果定制进入相册就选中之前的已选，需要传参。并且需要更改接收返回值时清空之前已选列表。选择上限也需要改。
            intent.putExtra(ImageGridActivity.EXTRAS_IMAGES, picHelper?.getSelectPicList())
        }
        startActivityForResult(intent, 101)
    }

    override fun removeImageItem(type: Int, item: ImageItem?) {
        // 移除回调，操作从上传队列移除
        ToastUtils.showLong("Remove from upload list: \n ${item?.path}")
        val img = hasSelectList.find { it.path == item?.path }
        hasSelectList.remove(img)
    }

}
