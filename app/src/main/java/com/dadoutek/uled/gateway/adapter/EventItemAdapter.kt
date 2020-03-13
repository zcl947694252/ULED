package com.dadoutek.uled.gateway.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.gateway.bean.GatewayTagBean


/**
 * 创建者     ZCL
 * 创建时间   2020/3/3 16:00
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class EventItemAdapter(resId: Int, data: MutableList<GatewayTagBean>) : BaseQuickAdapter<GatewayTagBean, BaseViewHolder>(resId, data) {
    override fun convert(helper: BaseViewHolder?, item: GatewayTagBean) {

        helper?.addOnClickListener(R.id.item_event_ly)
                ?.addOnClickListener(R.id.item_event_switch)
                ?.setText(R.id.item_event_title, item.tagName)
                ?.setText(R.id.item_event_week, item.weekStr)
                ?.setChecked(R.id.item_event_switch, item.status == 1)

    }

}