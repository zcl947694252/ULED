package com.dadoutek.uled.gateway.adapter

import android.annotation.SuppressLint
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.gateway.bean.WeekBean
import org.jetbrains.anko.image


/**
 * 创建者     ZCL
 * 创建时间   2020/3/9 12:08
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class WeeksItemAdapter(resId: Int, data: MutableList<WeekBean>) : BaseQuickAdapter<WeekBean, BaseViewHolder>(resId, data) {
    @SuppressLint("ResourceAsColor")
    override fun convert(helper: BaseViewHolder?, item: WeekBean?) {
        val textView = helper?.getView<TextView>(R.id.item_week_title)
        val cb = helper?.getView<ImageView>(R.id.item_week_checked)
        cb?.isSelected = item!!.selected
        if (item.selected) {
            cb?.setImageResource(R.drawable.icon_checkbox_selected)
            textView?.setTextColor(mContext.getColor(R.color.blue_text))
        } else {
            cb?.setImageResource(R.drawable.icon_checkbox_unselected)
            textView?.setTextColor(mContext.getColor(R.color.black_three))
        }

        helper?.setText(R.id.item_week_title, item.week)
        //?.setselect(R.id.item_week_checked, item.selected)
        //?.setVisible(R.id.item_week_checked, item!!.checked)
    }

}