package com.dadoutek.uled.othersview.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.device.model.DeviceItem
import com.dadoutek.uled.model.DeviceType

class DeviceTypeAdapter(resuresId: Int, data: MutableList<DeviceItem>) : BaseQuickAdapter<DeviceItem,BaseViewHolder>(resuresId,data) {
    override fun convert(helper: BaseViewHolder?, item: DeviceItem) {
        helper?.setText(R.id.select_tv_name, item.name)
        when (item.productUUID) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> {
                helper?.setImageResource(R.id.select_device_image, R.drawable.icon_light_device)
            }
            DeviceType.LIGHT_RGB -> {
                helper?.setImageResource(R.id.select_device_image, R.drawable.icon_rgb_light_device)
            }
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SMART_CURTAIN_SWITCH -> {
                helper?.setImageResource(R.id.select_device_image, R.drawable.icon_switch_device)
            }
            DeviceType.SENSOR -> {
                helper?.setImageResource(R.id.select_device_image, R.drawable.icon_sensor_device)
            }

            DeviceType.SMART_CURTAIN -> {
                helper?.setImageResource(R.id.select_device_image, R.drawable.icon_curtain_device)
            }
            DeviceType.SMART_RELAY -> {
                helper?.setImageResource(R.id.select_device_image, R.drawable.icon_acceptor_device)
            }
            DeviceType.GATE_WAY -> {
                helper?.setImageResource(R.id.select_device_image, R.drawable.icon_gateway)
            }
        }
    }

}
