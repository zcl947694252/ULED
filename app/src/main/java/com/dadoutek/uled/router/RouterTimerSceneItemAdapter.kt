package com.dadoutek.uled.router

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.gateway.bean.WeekBean
import com.dadoutek.uled.model.Constants
import com.dadoutek.uled.network.RouterTimerSceneBean


/**
 * 创建者     ZCL
 * 创建时间   2020/10/28 9:51
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class RouterTimerSceneItemAdapter(resId: Int, data: MutableList<RouterTimerSceneBean>) : BaseQuickAdapter<RouterTimerSceneBean, BaseViewHolder>(resId, data) {
    override fun convert(helper: BaseViewHolder?, item: RouterTimerSceneBean?) {
        val min = item?.min?:0
        val hour = item?.hour?:0
        var minStr = if (min<10) "0$min" else min.toString()
        var hourStr = if (hour<10) "0$hour" else hour.toString()

        val value ="${hourStr ?:0}:${minStr ?:0}"
        helper?.addOnClickListener(R.id.item_event_ly)
                ?.addOnClickListener(R.id.item_event_switch)
                ?.setText(R.id.item_event_title, value)
                ?.setText(R.id.item_event_week, getWeekStr(item?.week))
                ?.setText(R.id.item_event_name, item?.sname)
                ?.setVisible(R.id.item_event_name, true)
                ?.setChecked(R.id.item_event_switch, item?.status == 1)
    }

    private fun getWeekStr(week: Int?): String {
        var tmpWeek = week
        if (week == 0b10000000)
            tmpWeek = Constants.SATURDAY or Constants.FRIDAY or Constants.THURSDAY or Constants.WEDNESDAY or Constants.TUESDAY or Constants.MONDAY or Constants.SUNDAY //每一天
        if (tmpWeek != null) {
            val list = mutableListOf(
                    WeekBean(mContext.getString(R.string.monday), 1, (tmpWeek and Constants.MONDAY) != 0),
                    WeekBean(mContext.getString(R.string.tuesday), 2, (tmpWeek and Constants.TUESDAY) != 0),
                    WeekBean(mContext.getString(R.string.wednesday), 3, (tmpWeek and Constants.WEDNESDAY) != 0),
                    WeekBean(mContext.getString(R.string.thursday), 4, (tmpWeek and Constants.THURSDAY) != 0),
                    WeekBean(mContext.getString(R.string.friday), 5, (tmpWeek and Constants.FRIDAY) != 0),
                    WeekBean(mContext.getString(R.string.saturday), 6, (tmpWeek and Constants.SATURDAY) != 0),
                    WeekBean(mContext.getString(R.string.sunday), 7, (tmpWeek and Constants.SUNDAY) != 0))
            val filter = list.filter { it.selected }
            return when {
                filter.size >= 7 -> mContext.getString(R.string.every_day)
                else -> {
                    val sb = StringBuilder()
                    for (i in filter.indices)
                        when {
                            i != filter.size - 1 -> sb.append(filter[i].week).append(",")
                            else -> sb.append(filter[i].week)
                        }
                    sb.toString()
                }
            }
        }
        return ""
    }


}