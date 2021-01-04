package com.dadoutek.uled.gateway

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R


/**
 * 创建者     ZCL
 * 创建时间   2020/4/7 16:21
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class StandingItemAdapter(layoutId: Int, data: MutableList<Int>) :BaseQuickAdapter<Int,BaseViewHolder>(layoutId,data){
    override fun convert(helper: BaseViewHolder?, item: Int?) {
       helper?.setText(R.id.standing_time_tv,item.toString()+mContext.getString(R.string.minute))
    }

}