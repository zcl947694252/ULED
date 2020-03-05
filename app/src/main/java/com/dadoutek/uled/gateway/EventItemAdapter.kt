package com.dadoutek.uled.gateway

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R


/**
 * 创建者     ZCL
 * 创建时间   2020/3/3 16:00
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class EventItemAdapter(resId: Int, data: MutableList<String>) : BaseQuickAdapter<String, BaseViewHolder>(resId,data) {
    override fun convert(helper: BaseViewHolder?, item: String?) {
        helper?.setText(R.id.item_event_title,item)
                ?.addOnClickListener(R.id.item_event_title)
                ?.addOnClickListener(R.id.item_event_week)
                ?.addOnClickListener(R.id.item_event_switch)
    }

}