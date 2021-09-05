package com.dadoutek.uled.othersview.adapter

import android.view.View
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
                if (item.number == 0) {
                    helper?.setImageResource(R.id.select_device_image, R.mipmap.touch_light_color1)
                    helper?.setGone(R.id.select_tv_name2, true)
                        ?.setText(R.id.select_tv_name2,R.string.monotonic)
                } else {
                    helper?.setImageResource(R.id.select_device_image, R.mipmap.touch_light_color1)
                    helper?.setGone(R.id.select_tv_name2, true)
                        ?.setText(R.id.select_tv_name2,R.string.dimming)
                }
            }
            DeviceType.NORMAL_SWITCH2 -> {
                if (item.number == 0){
                    helper?.setImageResource(R.id.select_device_image, R.mipmap.light_only1)
                    helper?.setGone(R.id.select_tv_name2, true)
                        ?.setText(R.id.select_tv_name2,R.string.monotonic)
                }
                else {
                    helper?.setImageResource(R.id.select_device_image, R.mipmap.light_color1)
                    helper?.setGone(R.id.select_tv_name2, true)
                        ?.setText(R.id.select_tv_name2,R.string.dimming)
                }
            }
            DeviceType.EIGHT_SWITCH -> {
                helper?.setImageResource(R.id.select_device_image, R.mipmap.eight_switch1)
            }
            DeviceType.DOUBLE_SWITCH -> {
                helper?.setImageResource(R.id.select_device_image, R.mipmap.touch_only_light1)
                helper?.setGone(R.id.select_tv_name2, true)
                    ?.setText(R.id.select_tv_name2,R.string.two_switch2)
            }
            DeviceType.SCENE_SWITCH -> {
                if (item.number == 0) {
                    helper?.setImageResource(R.id.select_device_image, R.mipmap.light_scene1)
                    helper?.setGone(R.id.select_tv_name2, true)
                        ?.setText(R.id.select_tv_name2,R.string.scene_text)
                }
                else {
                    helper?.setImageResource(R.id.select_device_image, R.mipmap.touch_scene1)
                    helper?.setGone(R.id.select_tv_name2, true)
                        ?.setText(R.id.select_tv_name2,R.string.scene_text)
                }
            }
            DeviceType.SMART_CURTAIN_SWITCH -> {
                helper?.setImageResource(R.id.select_device_image, R.mipmap.curtain_switch1)
            }
            DeviceType.FOUR_SWITCH -> {
                helper?.setImageResource(R.id.select_device_image, R.mipmap.four_switch1)
            }
            DeviceType.SIX_SWITCH -> {
                helper?.setImageResource(R.id.select_device_image, R.mipmap.six_switch1)
            }

        }
    }

}
