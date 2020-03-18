package com.dadoutek.uled.gateway

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.gateway.bean.GwTimePeriodsBean


/**
 * 创建者     ZCL
 * 创建时间   2020/3/17 16:15
 * 描述 时间段每一小段任务
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
internal class GwSceneListAdapter(layoutId:Int, data:ArrayList<GwTimePeriodsBean>):BaseQuickAdapter<GwTimePeriodsBean,BaseViewHolder>(layoutId,data){
    override fun convert(helper: BaseViewHolder?, item: GwTimePeriodsBean?) {
        val hour = item?.hour
        val minute = item?.minute
        var hourStr = if (hour?:0<10) "0$hour" else hour.toString()
        var minuteStr = if (hour?:0<10) "0$minute" else minute.toString()
         var timeStr  = "$hourStr -- $minuteStr"
        helper?.setText(R.id.item_gw_timer_title, timeStr)
                ?.setText(R.id.item_gw_timer_scene,item?.sceneName)
    }

}