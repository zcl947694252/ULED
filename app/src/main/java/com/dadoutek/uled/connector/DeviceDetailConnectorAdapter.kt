package com.dadoutek.uled.connector

import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbConnector
import com.dadoutek.uled.tellink.TelinkLightApplication

class DeviceDetailConnectorAdapter (layoutResId: Int, data: List<DbConnector>?) : BaseQuickAdapter<DbConnector, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, dbConnector: DbConnector) {
        if (dbConnector != null) {
            //val tvName = helper.getView<TextView>(R.id.name)
            val tvLightName = helper.getView<TextView>(R.id.template_group_name)
           // val tvRgbColor = helper.getView<TextView>(R.id.tv_rgb_color)
               // tvName.text = StringUtils.getConnectorName(scene)

            if (TelinkLightApplication.getApp().connectDevice == null) {
                //tvName.setTextColor(mContext.resources.getColor(R.color.black))
                tvLightName.setTextColor(mContext.resources.getColor(R.color.black))
            } else {
                if (TelinkLightApplication.getApp().connectDevice.meshAddress == dbConnector.meshAddr) {
                   // tvName.setTextColor(mContext.resources.getColor(R.color.primary))
                    tvLightName.setTextColor(mContext.resources.getColor(R.color.primary))
                } else {
                   // tvName.setTextColor(mContext.resources.getColor(R.color.gray))
                    tvLightName.setTextColor(mContext.resources.getColor(R.color.black))
                }
            }

            tvLightName.text = dbConnector.name
/*
            val myGrad = tvRgbColor.background as GradientDrawable
            if (dbConnector.getColor() == 0 || dbConnector.getColor() == 0xffffff) {
                tvRgbColor.visibility = View.GONE
            } else {
                tvRgbColor.visibility = View.VISIBLE
                myGrad.setColor(-0x1000000 or dbConnector.getColor())
            }*/

            helper.addOnClickListener(R.id.template_device_setting)
                    .setTag(R.id.template_device_setting, helper.adapterPosition)
                    .setVisible(R.id.template_device_more, false)
                    .setTag(R.id.template_device_icon, helper.adapterPosition)
                    .setImageResource(R.id.template_device_icon, dbConnector.icon)
                    .addOnClickListener(R.id.template_device_icon)
        }
    }
}
