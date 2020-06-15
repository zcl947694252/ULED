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
        helper.setText(R.id.template_device_num, TelinkLightApplication.getApp().getString(R.string.number) + "：" + item?.number)
        .setText(R.id.template_group_name_n, item?.name)
        val deviceNum = helper.getView<TextView>(R.id.template_device_num)
        deviceNum.visibility = View.VISIBLE
        val moreLy = helper.getView<LinearLayout>(R.id.template_device_more_ly)
        moreLy.visibility = View.GONE
        when (item?.productUUID) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> {
                helper.setImageResource(R.id.template_device_icon, R.drawable.icon_light_device)
            }
            DeviceType.LIGHT_RGB -> {
                helper.setImageResource(R.id.template_device_icon, R.drawable.icon_rgb_light_device)
            }
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SMART_CURTAIN_SWITCH -> {
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
        }
    }
}
