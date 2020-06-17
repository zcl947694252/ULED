package com.dadoutek.uled.curtains

import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.tellink.TelinkLightApplication

class CurtainDeviceDetailsAdapter(layoutResId: Int, data: List<DbCurtain>?) : BaseQuickAdapter<DbCurtain, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, dbCurtain: DbCurtain) {
        if (dbCurtain != null) {
            val tvName = helper.getView<TextView>(R.id.template_group_name_s)
           // val tvLightName = helper.getView<TextView>(R.id.tv_device_name)
//            val tvRgbColor = helper.getView<TextView>(R.id.tv_rgb_color)
           // tvName.text = StringUtils.getCurtainName(dbCurtain)
            tvName.text = dbCurtain.name
            if (TelinkLightApplication.getApp().connectDevice == null) {
                tvName.setTextColor(mContext.resources.getColor(R.color.black))
            } else {
                if (TelinkLightApplication.getApp().connectDevice.meshAddress == dbCurtain.meshAddr) {
                    tvName.setTextColor(mContext.resources.getColor(R.color.primary))
                    //tvLightName.setTextColor(mContext.resources.getColor(R.color.primary))
                } else {
                    tvName.setTextColor(mContext.resources.getColor(R.color.gray))
                    //tvLightName.setTextColor(mContext.resources.getColor(R.color.black))
                }
            }

            //tvLightName.text = dbCurtain.name
            helper.addOnClickListener(R.id.template_device_setting)
                    .setTag(R.id.template_device_setting, helper.adapterPosition)
                    .setTag(R.id.template_device_icon, helper.adapterPosition)
                    .setVisible(R.id.template_device_more, false)
                    .setImageResource(R.id.template_device_icon, dbCurtain.icon)
                    .addOnClickListener(R.id.template_device_icon)
        }
    }
}