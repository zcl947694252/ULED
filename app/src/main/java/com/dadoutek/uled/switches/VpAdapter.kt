package com.dadoutek.uled.switches

import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup


/**
 * 创建者     ZCL
 * 创建时间   2020/1/10 14:46
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class VpAdapter(var list: MutableList<View>) :PagerAdapter(){
    override fun isViewFromObject(p0: View, p1: Any): Boolean {
        return p0==p1
    }

    override fun getCount(): Int {
        return  list.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        container.addView(list[position])
        return container.addView(list[position])

    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        super.destroyItem(container, position, `object`)
        container.removeView(list[position])
    }

}