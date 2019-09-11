package com.dadoutek.uled.curtains

import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.StringUtils

class CurtainDeviceDetailsAdapter(layoutResId: Int, data: List<DbCurtain>?) : BaseQuickAdapter<DbCurtain, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, scene: DbCurtain) {
        if (scene != null) {
            val tvName = helper.getView<TextView>(R.id.name)
            val tvLightName = helper.getView<TextView>(R.id.tv_device_name)
//            val tvRgbColor = helper.getView<TextView>(R.id.tv_rgb_color)
            tvName.text = StringUtils.getCurtainName(scene)

            if (TelinkLightApplication.getApp().connectDevice == null) {
                tvName.setTextColor(mContext.resources.getColor(R.color.black))
            } else {
                if (TelinkLightApplication.getApp().connectDevice.meshAddress == scene.getMeshAddr()) {
                    tvName.setTextColor(mContext.resources.getColor(R.color.primary))
                    tvLightName.setTextColor(mContext.resources.getColor(R.color.primary))
                } else {
                    tvName.setTextColor(mContext.resources.getColor(R.color.gray))
                    tvLightName.setTextColor(mContext.resources.getColor(R.color.black))
                }
            }

            tvLightName.setText(scene.getName())

//            val myGrad = tvRgbColor.background as GradientDrawable
//            if (scene.getTextColor() == 0 || scene.getTextColor() == 0xffffff) {
//                tvRgbColor.visibility = View.GONE
//            } else {
//                tvRgbColor.visibility = View.VISIBLE
//                myGrad.setColor(-0x1000000 or scene.getTextColor())
//            }

            helper.addOnClickListener(R.id.tv_setting)
                    .setTag(R.id.tv_setting, helper.adapterPosition)
                    .setTag(R.id.img_light, helper.adapterPosition)
                    .setBackgroundRes(R.id.img_light, scene.icon)
                    .addOnClickListener(R.id.img_light)
        }
    }
}