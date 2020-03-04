package com.bese.lib.picker.view

import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.PopupWindow
import com.bese.lib.picker.R

class PhotoPopupWindow(
    context: Activity,
    takeOnClick: View.OnClickListener, chooseOnClick: View.OnClickListener
) : PopupWindow(context) {
    /**
     * PopupWindow 菜单布局
     */
    private var mMenuView: View? = null
    /**
     * 上下文参数
     */
    private val context: Context
    private val takeOnClick: View.OnClickListener
    private val chooseOnClick: View.OnClickListener
    private fun Init() { // popupWindow 导入
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mMenuView = inflater.inflate(R.layout.photo_popup_window, null)
        val btn_camera = mMenuView?.findViewById<View>(R.id.icon_btn_camera) as Button
        val btn_photo = mMenuView?.findViewById<View>(R.id.icon_btn_select) as Button
        val btn_cancel = mMenuView?.findViewById<View>(R.id.icon_btn_cancel) as Button
        btn_camera.setOnClickListener(takeOnClick)
        btn_photo.setOnClickListener(chooseOnClick)
        btn_cancel.setOnClickListener { dismiss() }
        // 导入布局
        this.contentView = mMenuView
        // 设置动画效果
        this.animationStyle = R.style.popwindow_anim_style
        this.width = WindowManager.LayoutParams.MATCH_PARENT
        this.height = WindowManager.LayoutParams.WRAP_CONTENT
        // 设置可触
        this.isFocusable = true
        val dw = ColorDrawable(0x0000000)
        setBackgroundDrawable(dw)
        // 单击 popupWindow 以外即关闭
        mMenuView?.setOnTouchListener { v, event ->
            val height = mMenuView?.findViewById<View>(R.id.ll_pop)?.top ?: 1
            val y = event.y.toInt()
            if (event.action == MotionEvent.ACTION_UP) {
                if (y < height) {
                    dismiss()
                }
            }
            true
        }
    }

    init {
        this.context = context
        this.takeOnClick = takeOnClick
        this.chooseOnClick = chooseOnClick
        Init()
    }
}