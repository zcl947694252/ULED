package com.dadoutek.uled.group

import android.widget.ImageView
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
class BatchFourLightAdapter(layoutResId: Int, data: ArrayList<DbLight>) : BaseQuickAdapter<DbLight, BaseViewHolder>(layoutResId, data) {
    override fun convert(helper: BaseViewHolder?, item: DbLight?) {
        helper?:return
        val icon = helper.getView<ImageView>(R.id.img_icon)

        helper.setText(R.id.tv_group_name, item?.deviceName)
                .setText(R.id.tv_device_name, item?.name)
                .setChecked(R.id.selected, item?.selected?:false)

        if (item?.productUUID == DeviceType.LIGHT_RGB) {
            icon.setImageResource(R.drawable.icon_rgblight)
        } else {
            icon.setImageResource(R.drawable.icon_rgblight_down)
        }
    }
}