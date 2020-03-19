package com.dadoutek.uled.gateway.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.gateway.bean.GwTasksBean
import com.dadoutek.uled.model.DbModel.DBUtils


/**
 * 创建者     ZCL
 * 创建时间   2020/3/4 10:42
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GwTaskItemAdapter(resId: Int, data: MutableList<GwTasksBean>) : BaseQuickAdapter<GwTasksBean, BaseViewHolder>(resId, data) {
    override fun convert(helper: BaseViewHolder?, item: GwTasksBean) {
        val periods = item.timingPeriods
        val time = if (periods == null)
            "${DBUtils.timeStr(item.startHour)}:${DBUtils.timeStr(item.startMins)}"
        else
            "${DBUtils.timeStr(item.startHour)}:${DBUtils.timeStr(item.startMins)} - ${DBUtils.timeStr(item.endHour)}:${DBUtils.timeStr(item.endMins)}"

        helper?.setText(R.id.item_gw_timer_title, time)
                ?.setText(R.id.item_gw_timer_scene, item.senceName)
    }
}