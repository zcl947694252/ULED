package com.dadoutek.uled.light

import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbGroup

class NightLightEditGroupAdapter (layoutResId: Int, data: List<DbGroup>) : BaseQuickAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: DbGroup) {
        helper.setText(R.id.template_device_batch_title,item.name).addOnClickListener(R.id.item_more_group_check)
        val smallTv = helper.getView<TextView>(R.id.template_device_batch_title_blow)
        smallTv.visibility = View.GONE
        if (item.isChecked) {
            helper.setImageResource(R.id.template_device_batch_selected,R.drawable.icon_checkbox_selected)
        } else {
                helper.setImageResource(R.id.template_device_batch_selected,R.drawable.icon_checkbox_unselected)
           //if(item.isCheckedInGroup){
           //    helper.setImageResource(R.id.sensor_delete,R.drawable.icon_checkbox_unselected)
           //}else{
           //    helper.setImageResource(R.id.sensor_delete,R.drawable.icon_checkbox_unselected)
           //}
        }
    }
}