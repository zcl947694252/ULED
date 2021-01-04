package com.dadoutek.uled.gateway.adapter

import android.annotation.SuppressLint
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.gateway.bean.GwTimePeriodsBean
import com.dadoutek.uled.model.dbModel.DBUtils.timeStr


/**
 * 创建者     ZCL
 * 创建时间   2020/3/17 16:15
 * 描述 时间段每一小段任务
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GwTpItemAdapter(layoutId: Int, data: ArrayList<GwTimePeriodsBean>):BaseQuickAdapter<GwTimePeriodsBean,BaseViewHolder>(layoutId,data){
    @SuppressLint("StringFormatMatches")
    override fun convert(helper: BaseViewHolder?, item: GwTimePeriodsBean?) {
        val sh = (item?.startTime ?: 0).div(60)
        val sm = (item?.startTime ?: 0).rem(60)

        val eh = (item?.endTime ?: 0).div(60)
        val em = (item?.endTime ?: 0).rem(60)

        var strTime = "${timeStr(sh)}:${timeStr(sm)}" + " － ${timeStr(eh)}:${timeStr(em)}"
        helper?.setText(R.id.item_gw_timer_title, (item?.index?:0).toString())
                ?.setText(R.id.item_gw_timer_scene,item?.sceneName)
                ?.setText(R.id.item_gw_timer_tv,mContext.getString(R.string.standing_time_num,item?.standingTime?:0))
    }

}