package com.dadoutek.uled.scene

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbSensor

class SensorDeviceDetailsAdapter(layoutResId: Int, data: List<DbSensor>?) : BaseQuickAdapter<DbSensor, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, dbSensor: DbSensor) {
        if (dbSensor != null) {
            helper.setText(R.id.template_group_name, dbSensor.name)
            helper.setImageResource(R.id.template_device_icon, dbSensor.icon)
            helper.addOnClickListener(R.id.template_device_setting)
                    .setTag(R.id.template_device_setting, helper.adapterPosition)
                    .setTag(R.id.template_device_icon, helper.adapterPosition)
                    .setVisible(R.id.template_device_more, false)
                    .addOnClickListener(R.id.template_device_icon)
        }
    }
}