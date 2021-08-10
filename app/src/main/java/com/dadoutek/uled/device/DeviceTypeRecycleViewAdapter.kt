package com.dadoutek.uled.device

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.device.model.DeviceItem
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.tellink.TelinkLightApplication

class DeviceTypeRecycleViewAdapter(layoutResId: Int, data: List<DeviceItem>?) : BaseItemDraggableAdapter<DeviceItem, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: DeviceItem?) {
       helper.setText(R.id.template_device_num, TelinkLightApplication.getApp().getString(R.string.number)+":${item?.number}")
               .setVisible(R.id.template_gp_name, false)
        helper.setText(R.id.template_device_group_name, item?.name)

        val deviceNum = helper.getView<TextView>(R.id.template_device_num)
        val moreLy = helper.getView<LinearLayout>(R.id.template_device_more_ly)
        deviceNum.visibility = View.VISIBLE
        moreLy.visibility = View.GONE

        when (item?.productUUID) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> {
                helper.setImageResource(R.id.template_device_icon, R.drawable.icon_light_no_circle)
            }
            DeviceType.LIGHT_RGB -> {
                helper.setImageResource(R.id.template_device_icon, R.drawable.icon_rgb_no_circle)
            }
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SMART_CURTAIN_SWITCH, DeviceType.SIX_SWITCH,
            DeviceType.FOUR_SWITCH, DeviceType.SCENE_SWITCH, DeviceType.EIGHT_SWITCH, DeviceType.DOUBLE_SWITCH -> {
                helper.setImageResource(R.id.template_device_icon, R.drawable.icon_switch_device)
            }
            DeviceType.SENSOR -> {
                helper.setImageResource(R.id.template_device_icon, R.drawable.icon_sensor_device)
            }

            DeviceType.SMART_CURTAIN -> {
                helper.setImageResource(R.id.template_device_icon, R.drawable.icon_curtain_device)
            }
            DeviceType.SMART_RELAY -> {
                helper.setImageResource(R.id.template_device_icon, R.drawable.icon_acceptor_device)
            }
            DeviceType.GATE_WAY -> {
                helper.setImageResource(R.id.template_device_icon, R.drawable.icon_gateway)
            }
            DeviceType.ROUTER -> {
                helper.setImageResource(R.id.template_device_icon, R.drawable.icon_wifi)
            }
        }
    }
}
