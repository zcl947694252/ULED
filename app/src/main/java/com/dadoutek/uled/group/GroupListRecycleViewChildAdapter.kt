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

            if (group.meshAddr == 0xffff) {
                helper.setText(R.id.txt_name, TelinkLightApplication.getInstance().getString(R.string.allLight))
            } else {
                helper.setText(R.id.txt_name, group.name)
//                if(OtherUtils.isCurtain(group)){
//                    helper.setText(R.id.btn_set)
//                }
            }
            helper.setTextColor(R.id.txt_name, group.textColor)
                    .addOnClickListener(R.id.txt_name)
                    .addOnClickListener(R.id.btn_on)
                    .addOnClickListener(R.id.btn_off)
                    .addOnClickListener(R.id.btn_set)
//                    .addOnClickListener(R.id.add_group)
        }

    }

}
