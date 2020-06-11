package com.dadoutek.uled.connector

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbConnector
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.StringUtils

class DeviceDetailConnectorAdapter (layoutResId: Int, data: List<DbConnector>?) : BaseQuickAdapter<DbConnector, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, scene: DbConnector) {
        if (scene != null) {

            val tvName = helper.getView<TextView>(R.id.name)
            val tvLightName = helper.getView<TextView>(R.id.tv_device_name)
            val tvRgbColor = helper.getView<TextView>(R.id.tv_rgb_color)
            if (scene != null) {
                tvName.text = StringUtils.getConnectorGroupName(scene)
            }

            if (TelinkLightApplication.getApp().connectDevice == null) {
                tvName.setTextColor(mContext.resources.getColor(R.color.black))
            } else {
                if (TelinkLightApplication.getApp().connectDevice.meshAddress == scene.meshAddr) {
                    tvName.setTextColor(mContext.resources.getColor(R.color.primary))
                    tvLightName.setTextColor(mContext.resources.getColor(R.color.primary))
                } else {
                    tvName.setTextColor(mContext.resources.getColor(R.color.gray))
                    tvLightName.setTextColor(mContext.resources.getColor(R.color.black))
                }
            }

            tvLightName.text = scene.name

            val myGrad = tvRgbColor.background as GradientDrawable
            if (scene.getColor() == 0 || scene.getColor() == 0xffffff) {
                tvRgbColor.visibility = View.GONE
            } else {
                tvRgbColor.visibility = View.VISIBLE
                myGrad.setColor(-0x1000000 or scene.getColor())
            }

            helper.addOnClickListener(R.id.tv_setting)
                    .setTag(R.id.tv_setting, helper.adapterPosition)
                    .setTag(R.id.img_light, helper.adapterPosition)
                    .setBackgroundRes(R.id.img_light, scene.icon)
                    .addOnClickListener(R.id.img_light)
        }
    }
}
