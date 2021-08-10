package com.dadoutek.uled.othersview.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.device.model.DeviceItem
import com.dadoutek.uled.model.DeviceType


/**
 * 创建者     ZCL
 * 创建时间   2021/7/28 17:02
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class SwitchTypeAdapter(resuresId: Int, data: MutableList<DeviceItem>) : BaseQuickAdapter<DeviceItem, BaseViewHolder>(resuresId,data) {
    override fun convert(helper: BaseViewHolder?, item: DeviceItem) {
        helper?.setText(R.id.select_tv_name, item.name)
        when (item.productUUID) {
            DeviceType.NORMAL_SWITCH -> {
                helper?.setImageResource(R.id.select_device_image, R.mipmap.normal_switch)
            }
            DeviceType.NORMAL_SWITCH2 -> {
                helper?.setImageResource(R.id.select_device_image, R.mipmap.normal_switch2)
            }
            DeviceType.EIGHT_SWITCH -> {
                helper?.setImageResource(R.id.select_device_image, R.mipmap.eight_switch)
            }
            DeviceType.DOUBLE_SWITCH -> {
                helper?.setImageResource(R.id.select_device_image, R.mipmap.normal_switch)
            }

            DeviceType.SCENE_SWITCH -> {
                helper?.setImageResource(R.id.select_device_image, R.drawable.icon_switch_device)
            }
            DeviceType.SMART_CURTAIN_SWITCH -> {
                helper?.setImageResource(R.id.select_device_image, R.mipmap.normal_switch2)
            }
            DeviceType.FOUR_SWITCH -> {
                helper?.setImageResource(R.id.select_device_image, R.mipmap.normal_switch2)
            }
            DeviceType.SIX_SWITCH -> {
                helper?.setImageResource(R.id.select_device_image, R.mipmap.normal_switch2)
            }


        }
    }

}
