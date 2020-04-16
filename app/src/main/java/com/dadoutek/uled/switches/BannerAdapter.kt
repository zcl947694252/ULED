package com.dadoutek.uled.switches

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R


/**
 * 创建者     ZCL
 * 创建时间   2020/1/10 11:05
 * 描述 banner 条目
 *
 * 更新者     zcl$
 * 更新时间   $
 * 更新描述
 */

class BannerAdapter(resId:Int,data:List<Int>):BaseQuickAdapter<Int,BaseViewHolder>(resId,data){
    override fun convert(helper: BaseViewHolder?, item: Int?) {
       helper?.setImageResource(R.id.eight_switch_item_image,item!!)
    }

}
