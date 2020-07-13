package com.dadoutek.uled.curtains

import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.DensityUtil

class CurtainDeviceDetailsAdapter(layoutResId: Int, data: List<DbCurtain>?) : BaseQuickAdapter<DbCurtain, BaseViewHolder>(layoutResId, data) {
    private var isDelete: Boolean = false

    fun changeState(isDelete: Boolean) {
        this.isDelete = isDelete
    }

    override fun convert(helper: BaseViewHolder, dbCurtain: DbCurtain) {
        if (dbCurtain != null) {
            val tvName = helper.getView<TextView>(R.id.template_device_group_name)
           // val tvLightName = helper.getView<TextView>(R.id.tv_device_name)
//            val tvRgbColor = helper.getView<TextView>(R.id.tv_rgb_color)
           // tvName.text = StringUtils.getCurtainName(dbCurtain)
            val iv = helper.getView<ImageView>(R.id.template_device_icon)
            iv.layoutParams.height = DensityUtil.dip2px(mContext, 60f)
            iv.layoutParams.width = DensityUtil.dip2px(mContext, 60f)
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
                    .addOnClickListener(R.id.template_device_card_delete)
                    .setVisible(R.id.template_device_card_delete,isDelete)
                    .setVisible(R.id.template_gp_name,false)
                    .setText(R.id.template_gp_name, DBUtils.getGroupNameByID(dbCurtain.belongGroupId))
        }
    }
}