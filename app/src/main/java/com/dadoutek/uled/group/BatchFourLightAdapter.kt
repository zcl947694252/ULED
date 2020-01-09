package com.dadoutek.uled.group

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.blankj.utilcode.util.LogUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.DeviceType


/**
 * 创建者     ZCL
 * 创建时间   2019/10/16 15:52
 * 描述
 *
 * 更新者     $
 * 更新时间   批量分组冷暖灯彩灯适配器$
 * 更新描述
 */
class BatchFourLightAdapter(layoutResId: Int, data: MutableList<DbLight>) : BaseQuickAdapter<DbLight, BaseViewHolder>(layoutResId, data) {
    // -70 至 -80一般  >=-65 很好
    private val allLightId: Long = 1
    private val bestRssi: Long = -70
    private val normalRssi: Long = -80
    override fun convert(helper: BaseViewHolder?, item: DbLight?) {
        helper ?: return
        val icon = helper.getView<ImageView>(R.id.batch_img_icon)
        val groupName = helper.getView<TextView>(R.id.batch_tv_group_name)
        val rssiIcon = helper.getView<ImageView>(R.id.batch_img_rssi)

        LogUtils.e("zcl更新信号${item?.rssi}---------${item?.rssi?:-1000>=bestRssi}----------------${ item?.rssi?:-1000 in bestRssi..normalRssi}")

        when {
            item?.rssi?:-1000>=bestRssi -> rssiIcon.setBackgroundResource(R.drawable.rect_blue)
            item?.rssi?:-1000 in normalRssi..bestRssi -> rssiIcon.setBackgroundResource(R.drawable.rect_yellow)
            else -> rssiIcon.setBackgroundResource(R.drawable.btn_rectangle_circle_red)
        }

        helper.setText(R.id.batch_tv_device_name, item?.name)

        if (item?.isSelected == true) {
            helper.setImageResource(R.id.batch_selected,R.drawable.icon_checkbox_selected)
        } else {
            helper.setImageResource(R.id.batch_selected,R.drawable.icon_checkbox_unselected)
        }


        if (item?.belongGroupId !=allLightId) {
            helper.setTextColor(R.id.batch_tv_device_name, mContext.getColor(R.color.blue_text))
                    .setTextColor(R.id.batch_tv_group_name, mContext.getColor(R.color.blue_text))
            groupName.visibility = View.VISIBLE
            groupName.text = item?.groupName

            if (item?.productUUID == DeviceType.LIGHT_RGB) {
                icon.setImageResource(R.drawable.icon_rgblight)
            } else {
                icon.setImageResource(R.drawable.icon_device_open)
            }
        } else {
            helper.setTextColor(R.id.batch_tv_device_name, mContext.getColor(R.color.gray_3))
            groupName.visibility = View.GONE
            //groupName.text ="=="+item?.rssi
            if (item?.productUUID == DeviceType.LIGHT_RGB) {
                icon.setImageResource(R.drawable.icon_rgblight_down)
            } else {
                icon.setImageResource(R.drawable.icon_device_down)
            }
        }
    }
}