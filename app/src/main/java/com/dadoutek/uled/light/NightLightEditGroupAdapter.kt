package com.dadoutek.uled.light

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbGroup

class NightLightEditGroupAdapter (layoutResId: Int, data: List<DbGroup>) : BaseQuickAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: DbGroup) {
        helper.setText(R.id.item_more_group_name,item.name).addOnClickListener(R.id.item_more_group_check)
        if (item.isChecked) {
            helper.setImageResource(R.id.item_more_group_check,R.drawable.icon_checkbox_selected)
        } else {
                helper.setImageResource(R.id.item_more_group_check,R.drawable.icon_checkbox_unselected)
           //if(item.isCheckedInGroup){
           //    helper.setImageResource(R.id.sensor_delete,R.drawable.icon_checkbox_unselected)
           //}else{
           //    helper.setImageResource(R.id.sensor_delete,R.drawable.icon_checkbox_unselected)
           //}
        }
    }
}