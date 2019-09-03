package com.dadoutek.uled.switches

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.StringUtils

class SwitchDeviceDetailsAdapter(layoutResId: Int, data: List<DbSwitch>?) : BaseQuickAdapter<DbSwitch, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, scene: DbSwitch) {
        if (scene != null) {

            if(scene.name!=null){
                helper.setText(R.id.light_name, scene.name)
            }else{
                helper.setText(R.id.light_name, TelinkLightApplication.getApp().getString(R.string.no_name))
            }

            helper.setText(R.id.name, StringUtils.getSwitchPirDefaultName(scene.productUUID))
            helper.setImageResource(R.id.img_light, R.drawable.icon_switch)
            helper.addOnClickListener(R.id.tv_setting)
                    .setTag(R.id.tv_setting, helper.adapterPosition)
                    .setTag(R.id.img_light, helper.adapterPosition)
                    .addOnClickListener(R.id.img_light)
        }
    }
}