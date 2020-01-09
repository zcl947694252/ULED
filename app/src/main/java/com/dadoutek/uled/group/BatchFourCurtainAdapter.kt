package com.dadoutek.uled.group

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbCurtain


/**
 * 创建者     ZCL
 * 创建时间   2019/10/16 15:52
 * 描述
 *
 * 更新时间   批量分组冷暖灯彩灯适配器$
 * 更新描述
 */
class BatchFourCurtainAdapter(layoutResId: Int, data: MutableList<DbCurtain>) : BaseQuickAdapter<DbCurtain, BaseViewHolder>(layoutResId, data) {
    private val bestRssi: Long = -70
    private val normalRssi: Long = -80
    override fun convert(helper: BaseViewHolder?, item: DbCurtain?) {
        helper ?: return
        val icon = helper.getView<ImageView>(R.id.batch_img_icon)
        val groupName = helper.getView<TextView>(R.id.batch_tv_group_name)
        val rssiIcon = helper.getView<ImageView>(R.id.batch_img_rssi)
        when {
            item?.rssi?:-1000>=bestRssi -> rssiIcon.setBackgroundResource(R.drawable.rect_blue)
            item?.rssi?:-1000 in normalRssi..bestRssi -> rssiIcon.setBackgroundResource(R.drawable.rect_yellow)
            else -> rssiIcon.setBackgroundResource(R.drawable.btn_rectangle_circle_red)
        }

        helper.setText(R.id.batch_tv_device_name, item?.name)
        if (item?.isSelected == true) {
            helper.setImageResource(R.id.batch_selected, R.drawable.icon_checkbox_selected)
        } else {
            helper.setImageResource(R.id.batch_selected, R.drawable.icon_checkbox_unselected)
        }

        if (item?.hasGroup == true) {
            helper.setTextColor(R.id.batch_tv_device_name, mContext.getColor(R.color.blue_text))
                    .setTextColor(R.id.batch_tv_group_name, mContext.getColor(R.color.blue_text))
            groupName.visibility = View.VISIBLE
            groupName.text = item.groupName +"=="+item.rssi

            icon.setImageResource(R.drawable.icon_curtain)
        } else {
            helper.setTextColor(R.id.batch_tv_device_name, mContext.getColor(R.color.gray_3))
            groupName.visibility = View.GONE
            //groupName.text ="-----"+item?.rssi
            icon.setImageResource(R.drawable.curtain_off)
        }
    }
}
