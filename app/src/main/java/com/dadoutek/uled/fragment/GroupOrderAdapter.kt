package com.dadoutek.uled.fragment

import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R

import com.dadoutek.uled.model.dbModel.DbGroup



/**
 * 创建者     Chown
 * 创建时间   2021/9/10 15:25
 * 描述
 */
class GroupOrderAdapter(layoutResId: Int, data: List<DbGroup>, internal var isDelete: Boolean) : BaseItemDraggableAdapter<DbGroup, BaseViewHolder>(layoutResId, data)  {
    override fun convert(helper: BaseViewHolder?, group: DbGroup?) {
        if (group != null) {
            helper?.setImageResource(R.id.template_device_icon, R.drawable.icon_group_n)
                ?.setText(R.id.template_device_group_name, group.name)
                ?.setGone(R.id.template_device_setting, false)
                ?.setGone(R.id.template_device_more,false)
        }
    }
}