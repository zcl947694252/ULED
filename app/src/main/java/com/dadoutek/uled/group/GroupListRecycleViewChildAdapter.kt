package com.dadoutek.uled.group

import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.OtherUtils

class GroupListRecycleViewChildAdapter(layoutResId: Int, data: List<DbGroup>?) : BaseItemDraggableAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, group: DbGroup?) {

        if (group != null) {
            if (group.textColor == 0)
                group.textColor = mContext.resources
                        .getColor(R.color.black)
//            Log.d("setAddress", group.meshAddr.toString())
            if (group.meshAddr == 0xffff) {
                helper.setText(R.id.template_group_name_s, TelinkLightApplication.getApp().getString(R.string.allLight))
            } else {
                helper.setText(R.id.template_group_name_s, group.name)
//                if(OtherUtils.isCurtain(group)){
//                    helper.setText(R.id.btn_set)
//                }
            }
            if(OtherUtils.isCurtain(group)){
//                    helper.setText(R.id.btn_set)
                helper.setVisible(R.id.btn_off,false)
                helper.setVisible(R.id.btn_on,false)
                }
//            if(group.name==TelinkLightApplication.getApp().getString(R.string.curtain)){
//                helper.setVisible(R.id.btn_off,false)
//                helper.setVisible(R.id.btn_on,false)
//            }
            helper.setTextColor(R.id.template_group_name_s, group.textColor)
                    .addOnClickListener(R.id.template_group_name_s)
                    .addOnClickListener(R.id.btn_on)
                    .addOnClickListener(R.id.btn_off)
                    .addOnClickListener(R.id.btn_set)
//                    .addOnClickListener(R.id.add_group)
        }

    }

}
