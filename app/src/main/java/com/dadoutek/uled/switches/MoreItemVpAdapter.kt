package com.dadoutek.uled.switches

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup


/**
 * 创建者     ZCL
 * 创建时间   2020/2/21 16:12
 * 描述	      多图片适配器
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述   多图片适配器
 */
class MoreItemVpAdapter(list: MutableList<View>, context: Context) : PagerAdapter(){
    var list = list
    var context = context
    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view==`object`
    }

    override fun getCount(): Int {
       return list.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = list[position]
        container.addView(view)
        return view
    }

    override fun getPageWidth(position: Int): Float {
        return 0.8f
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(list[position])
    }
}