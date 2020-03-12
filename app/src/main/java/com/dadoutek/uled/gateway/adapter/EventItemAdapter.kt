package com.dadoutek.uled.gateway.adapter

import com.blankj.utilcode.util.GsonUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.gateway.bean.DbGatewayBean
import com.dadoutek.uled.gateway.bean.GatewayTagsBean


/**
 * 创建者     ZCL
 * 创建时间   2020/3/3 16:00
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class EventItemAdapter(resId: Int, data: MutableList<DbGatewayBean>) : BaseQuickAdapter<DbGatewayBean, BaseViewHolder>(resId,data) {
    override fun convert(helper: BaseViewHolder?, item: DbGatewayBean) {
        val tags = item.tags
        val tagsBean = GsonUtils.fromJson(tags, GatewayTagsBean::class.java)
        helper?.setText(R.id.item_event_title,item.name)
                ?.setText(R.id.item_event_week, tagsBean.week)
                ?.setChecked(R.id.item_event_switch,tagsBean.status==1)
                ?.addOnClickListener(R.id.item_event_title)
                ?.addOnClickListener(R.id.item_event_week)
                ?.addOnClickListener(R.id.item_event_switch)
    }

}