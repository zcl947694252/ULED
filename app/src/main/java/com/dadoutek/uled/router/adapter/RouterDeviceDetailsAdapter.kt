package com.dadoutek.uled.router.adapter


/**
 * 创建者     ZCL
 * 创建时间   2020/9/2 11:19
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
import com.dadoutek.uled.gateway.bean.DbRouter

class RouterDeviceDetailsAdapter(layoutResId: Int, data: MutableList<DbRouter>, internal var context: Context) : BaseQuickAdapter<DbRouter, BaseViewHolder>(layoutResId, data) {
    private var isDelete: Boolean = false

    fun changeState(isDelete: Boolean) {
        this.isDelete = isDelete
    }
    override fun convert(helper: BaseViewHolder, dbRouter: DbRouter) {
        when {
            dbRouter.name!=null&&dbRouter.name!="" ->helper.setText(R.id.template_device_group_name, dbRouter.name)
            else -> helper.setText(R.id.template_device_group_name, helper.position)
        }

        helper.setImageResource(R.id.template_device_icon, R.drawable.icon_wifi)
        helper.setVisible(R.id.template_device_card_delete,isDelete)
                .setTag(R.id.template_device_setting, helper.adapterPosition)
                .setTag(R.id.template_device_icon, helper.adapterPosition)
                .setVisible(R.id.template_device_more, false)
                .setVisible(R.id.template_gp_name, false)
                .addOnClickListener(R.id.template_device_icon)
                .addOnClickListener(R.id.template_device_card_delete)
                .addOnClickListener(R.id.template_device_setting)
    }
}