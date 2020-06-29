package com.dadoutek.uled.light

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.DensityUtil

/**
 * 创建者     zcl
 * 创建时间   2019/8/29 9:39
 * 描述	      ${补充描述 设备图标适配器}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${
 */
class DeviceDetailListAdapter(layoutResId: Int, data: List<DbLight>?) : BaseQuickAdapter<DbLight, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, dbLight: DbLight) {
       // val groupName = helper.getView<TextView>(R.id.template_device_group_icon)
        val deviceName = helper.getView<TextView>(R.id.template_device_group_name)
        val iv = helper.getView<ImageView>(R.id.template_device_icon)
        iv.layoutParams.height = DensityUtil.dip2px(mContext, 60f)
        iv.layoutParams.width = DensityUtil.dip2px(mContext, 60f)
//        if (dbLight.groupName==null||dbLight.groupName=="")
//            groupName.visibility = View.GONE
//        else
//            groupName.visibility = View.VISIBLE

        if (dbLight.name==null||dbLight.name=="")
            deviceName.visibility = View.GONE
        else
            deviceName.visibility = View.VISIBLE

        if (TelinkLightApplication.getApp().connectDevice!=null&&TelinkLightApplication.getApp().connectDevice.meshAddress == dbLight.meshAddr) {
            deviceName.setTextColor(mContext.resources.getColor(R.color.primary))
            //groupName.setTextColor(mContext.resources.getColor(R.color.primary))
        } else {
            deviceName.setTextColor(mContext.resources.getColor(R.color.black_three))
           // groupName.setTextColor(mContext.resources.getColor(R.color.black))
        }

        helper.setText(R.id.template_device_group_name,dbLight.name)
        .setText(R.id.template_gp_name,DBUtils.getGroupNameByID(dbLight.belongGroupId))
                .setVisible(R.id.template_gp_name,false)
                .setImageResource(R.id.template_device_icon,dbLight.icon)
                .addOnClickListener(R.id.template_device_setting)
                .addOnClickListener(R.id.template_device_icon)
                .setVisible(R.id.template_device_more,false)
    }
}