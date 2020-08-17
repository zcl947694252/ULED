package com.dadoutek.uled.light

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.group.GroupOTAListActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.DensityUtil
import org.jetbrains.anko.startActivity

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

    private var isDelete: Boolean = false

    override fun convert(helper: BaseViewHolder, dbLight: DbLight) {
       // val groupName = helper.getView<TextView>(R.id.template_device_group_icon)
        val deviceName = helper.getView<TextView>(R.id.template_device_group_name)

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
        helper.setVisible(R.id.template_device_card_delete,isDelete)
        .setText(R.id.template_gp_name,DBUtils.getGroupNameByID(dbLight.belongGroupId))
                .setVisible(R.id.template_gp_name, SharedPreferencesHelper.getBoolean(mContext,Constant.AUXFUN_IS_OPEN,false))
                .setImageResource(R.id.template_device_icon,dbLight.icon)
                .addOnClickListener(R.id.template_device_setting)
                .addOnClickListener(R.id.template_device_icon)
                .addOnClickListener(R.id.template_device_card_delete)
                .setVisible(R.id.template_device_more,false)
    }
    fun changeState(isDelete: Boolean) {
        this.isDelete = isDelete
    }
}