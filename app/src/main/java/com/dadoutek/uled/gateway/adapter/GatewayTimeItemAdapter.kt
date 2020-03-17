package com.dadoutek.uled.gateway.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.gateway.bean.GatewayTasksBean


/**
 * 创建者     ZCL
 * 创建时间   2020/3/4 10:42
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GatewayTimeItemAdapter(resId:Int, data: MutableList<GatewayTasksBean>): BaseQuickAdapter<GatewayTasksBean, BaseViewHolder>(resId,data){
    override fun convert(helper: BaseViewHolder?, item: GatewayTasksBean) {
        val time =  "${item.startHour}:${item.startMins}"

        helper?.setText(R.id.item_gw_timer_time, time)
                ?.setText(R.id.item_gw_timer_scene,item.senceName)
    }
}