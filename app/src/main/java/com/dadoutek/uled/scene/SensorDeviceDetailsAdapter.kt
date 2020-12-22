package com.dadoutek.uled.scene

import android.widget.ImageView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.dbModel.DbSensor
import com.dadoutek.uled.util.DensityUtil

class SensorDeviceDetailsAdapter(layoutResId: Int, data: List<DbSensor>?) : BaseQuickAdapter<DbSensor, BaseViewHolder>(layoutResId, data) {
    private var isDelete: Boolean = false

    fun changeState(isDelete: Boolean) {
        this.isDelete = isDelete
    }

    override fun convert(helper: BaseViewHolder, dbSensor: DbSensor) {
        if (dbSensor != null) {
            dbSensor.updateIcon()
            helper.setText(R.id.template_device_group_name, dbSensor.name)
            if (dbSensor.status == 1)
                helper.setImageResource(R.id.template_device_icon, R.drawable.icon_sensor)
            else
                helper.setImageResource(R.id.template_device_icon, R.drawable.icon_sensor_close)
            val iv = helper.getView<ImageView>(R.id.template_device_icon)
            iv.layoutParams.height = DensityUtil.dip2px(mContext, 60f)
            iv.layoutParams.width = DensityUtil.dip2px(mContext, 60f)
            helper.setVisible(R.id.template_device_card_delete, isDelete)
                    .setTag(R.id.template_device_setting, helper.adapterPosition)
                    .setTag(R.id.template_device_icon, helper.adapterPosition)
                    .setVisible(R.id.template_device_more, false)
                    .setVisible(R.id.template_gp_name, false)
                    .addOnClickListener(R.id.template_device_icon)
                    .addOnClickListener(R.id.template_device_card_delete)
                    .addOnClickListener(R.id.template_device_setting)
        }
    }
}