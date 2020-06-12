package com.dadoutek.uled.switches

import android.content.Context
import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.util.StringUtils

class SwitchDeviceDetailsAdapter(layoutResId: Int, data: List<DbSwitch>?,internal var context: Context) : BaseQuickAdapter<DbSwitch, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, dbSwitch: DbSwitch) {
        if (dbSwitch != null) {
            if(dbSwitch.name!=null&&dbSwitch.name!=""){
                helper.setText(R.id.template_device_name, dbSwitch.name)
            }else{
               helper.setText(R.id.template_device_name, StringUtils.getSwitchPirDefaultName(dbSwitch.productUUID, context)+"-"+helper.position)
            }

//            helper.setText(R.id.name, StringUtils.getSwitchName(dbSwitch))
//                    .setVisible(R.id.name,false)

            helper.setImageResource(R.id.template_device_icon, R.drawable.icon_switch)
            helper.addOnClickListener(R.id.template_device_setting)
                    .setTag(R.id.template_device_setting, helper.adapterPosition)
                    .setTag(R.id.template_device_icon, helper.adapterPosition)
                    .setVisible(R.id.template_device_more, false)
                    .addOnClickListener(R.id.template_device_icon)
        }
    }
}