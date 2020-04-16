package com.dadoutek.uled.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow

/**
 * 全都是静态方法的情况 : class 类名 改为 object 类名 即可
object MoreImageUtils {
fun filesToMultipartBodyParts(files: List<File>): List<MultipartBody.Part>? {
}
普通静态方法
一部分是静态方法的情况 : 将方法用 companion object { } 包裹即可
 */
object PopUtil {
        fun makeMW(context: Context, res: Int,isClick:Boolean): PopupWindow {
            var popView = LayoutInflater.from(context).inflate(res, null)
            var pop = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            pop!!.isOutsideTouchable = isClick
            pop.isFocusable = true // 设置PopupWindow可获得焦点
            pop.isTouchable = true // 设置PopupWindow可触摸补充：

            return pop
        }

        fun makeMWf(context: Context, res: Int): PopupWindow {
            var popView = LayoutInflater.from(context).inflate(res, null)
            var pop = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            pop.isFocusable = true // 设置PopupWindow可获得焦点
            pop.isTouchable = true // 设置PopupWindow可触摸补充：
            pop!!.isOutsideTouchable = false
            return pop
        }

        fun show(pop: PopupWindow?, view: View, gravity: Int){
            if (pop!=null&&pop.isShowing)
                pop.showAtLocation(view, gravity, 0, 0)
        }
        fun dismiss(pop: PopupWindow?){
            if (pop!=null&&pop.isShowing)
                pop.dismiss()
        }
}
