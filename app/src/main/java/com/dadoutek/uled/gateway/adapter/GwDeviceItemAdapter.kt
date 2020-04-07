package com.dadoutek.uled.gateway.adapter


/**
 * 创建者     ZCL
 * 创建时间   2020/3/13 15:56
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */


import android.content.Context
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.util.StringUtils

 class GwDeviceItemAdapter(layoutResId: Int, data: MutableList<DbGateway>, internal var context: Context) : BaseQuickAdapter<DbGateway, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, data: DbGateway) {
        if (data != null) {
            if(data.name!=null&&data.name!=""){
                helper.setText(R.id.tv_device_name, data.name)
            }else{
                helper.setText(R.id.tv_device_name, StringUtils.getSwitchPirDefaultName(data.productUUID, context)+"-"+helper.position)
            }

            helper.setVisible(R.id.name,false)

            helper.setImageResource(R.id.img_light,data.icon)
            helper.addOnClickListener(R.id.tv_setting)
                    .setTag(R.id.tv_setting, helper.adapterPosition)
                    .setTag(R.id.img_light, helper.adapterPosition)
                    .addOnClickListener(R.id.img_light)
        }
    }
}