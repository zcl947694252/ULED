package com.dadoutek.uled.light

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.StringUtils

class DeviceDetailListAdapter(layoutResId: Int, data: List<DbLight>?) : BaseQuickAdapter<DbLight, BaseViewHolder>(layoutResId, data) {

        override fun convert(helper: BaseViewHolder, scene: DbLight) {
            val tvName = helper.getView<TextView>(R.id.name)
            val tvLightName = helper.getView<TextView>(R.id.light_name)
            val tvRgbColor = helper.getView<TextView>(R.id.tv_rgb_color)
            tvName.text = StringUtils.getLightName(scene)

            if (TelinkLightApplication.getInstance().connectDevice == null) {
                tvName.setTextColor(mContext.resources.getColor(R.color.black))
            } else {
                if (TelinkLightApplication.getInstance().connectDevice.meshAddress == scene.getMeshAddr()) {
                    tvName.setTextColor(mContext.resources.getColor(R.color.primary))
                    tvLightName.setTextColor(mContext.resources.getColor(R.color.primary))
                } else {
                    tvName.setTextColor(mContext.resources.getColor(R.color.gray))
                    tvLightName.setTextColor(mContext.resources.getColor(R.color.black))
                }
            }

            tvLightName.setText(scene.getName())

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