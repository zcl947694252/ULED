package com.dadoutek.uled.connector

import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbConnector
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.DensityUtil

class DeviceDetailConnectorAdapter (layoutResId: Int, data: List<DbConnector>?) : BaseQuickAdapter<DbConnector, BaseViewHolder>(layoutResId, data) {
    private var isDelete: Boolean = false

    fun changeState(isDelete: Boolean) {
        this.isDelete = isDelete
    }
    override fun convert(helper: BaseViewHolder, dbConnector: DbConnector) {
        if (dbConnector != null) {
            val tvLightName = helper.getView<TextView>(R.id.template_device_group_name)
            val iv = helper.getView<ImageView>(R.id.template_device_icon)
            iv.layoutParams.height = DensityUtil.dip2px(mContext, 60f)
            iv.layoutParams.width = DensityUtil.dip2px(mContext, 60f)
            when (TelinkLightApplication.getApp().connectDevice) {
                null -> tvLightName.setTextColor(mContext.resources.getColor(R.color.black))
                else -> when (TelinkLightApplication.getApp().connectDevice.meshAddress) {
                        dbConnector.meshAddr -> tvLightName.setTextColor(mContext.resources.getColor(R.color.primary))
                        else -> tvLightName.setTextColor(mContext.resources.getColor(R.color.black))
                    }
            }

            tvLightName.text = dbConnector.name

            helper.addOnClickListener(R.id.template_device_setting)
                    .setTag(R.id.template_device_setting, helper.adapterPosition)
                    .setVisible(R.id.template_device_more, false)
                    .setVisible(R.id.template_gp_name,Constant.IS_OPEN_AUXFUN)
                    .setTag(R.id.template_device_icon, helper.adapterPosition)
                    .setImageResource(R.id.template_device_icon, dbConnector.icon)
                    .addOnClickListener(R.id.template_device_icon)
                    .addOnClickListener(R.id.template_device_card_delete)
                    .setVisible(R.id.template_device_card_delete,isDelete)
                    .setText(R.id.template_gp_name,DBUtils.getGroupNameByID(dbConnector.belongGroupId))
        }
    }
}
