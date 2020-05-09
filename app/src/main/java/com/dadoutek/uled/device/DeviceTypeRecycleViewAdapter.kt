package com.dadoutek.uled.device

import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.device.model.DeviceItem
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.tellink.TelinkLightApplication

class DeviceTypeRecycleViewAdapter(layoutResId: Int, data: List<DeviceItem>?) : BaseItemDraggableAdapter<DeviceItem, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: DeviceItem?) {
        helper.setText(R.id.device_amount, TelinkLightApplication.getApp().getString(R.string.number) + "：" + item?.number)
        helper.setText(R.id.tv_group_name, item?.name)
        when (item?.productUUID) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> {
                helper.setImageResource(R.id.device_image, R.drawable.icon_light_device)
            }
            DeviceType.LIGHT_RGB -> {
                helper.setImageResource(R.id.device_image, R.drawable.icon_rgb_light_device)
            }
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SMART_CURTAIN_SWITCH -> {
                helper.setImageResource(R.id.device_image, R.drawable.icon_switch_device)
            }
            DeviceType.SENSOR -> {
                helper.setImageResource(R.id.device_image, R.drawable.icon_sensor_device)
            }

            DeviceType.SMART_CURTAIN -> {
                helper.setImageResource(R.id.device_image, R.drawable.icon_curtain_device)
            }
            DeviceType.SMART_RELAY -> {
                helper.setImageResource(R.id.device_image, R.drawable.icon_acceptor_device)
            }
            DeviceType.GATE_WAY -> {
                helper.setImageResource(R.id.device_image, R.drawable.icon_gateway)
            }
        }
    }
}
