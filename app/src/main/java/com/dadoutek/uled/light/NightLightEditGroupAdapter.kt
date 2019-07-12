package com.dadoutek.uled.light

import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbGroup

class NightLightEditGroupAdapter (layoutResId: Int, data: List<DbGroup>) : BaseQuickAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: DbGroup) {
//        helper.setText(R.id.group_name,item.name)
//        val tvState = helper.getView<TextView>(R.id.group_check_state)
//        if (item.isChecked) {
//            tvState.text = mContext.getString(R.string.selected)
//            tvState.setTextColor(mContext.resources.getColor(R.color.white))
//            tvState.setBackgroundColor(mContext.resources.getColor(R.color.primary))
//        } else {
//            tvState.text = mContext.getString(R.string.unSelect)
//            tvState.setBackgroundColor(mContext.resources.getColor(R.color.white))
//            if(item.enableCheck){
//                tvState.setTextColor(mContext.resources.getColor(R.color.primary))
//            }else{
//                tvState.setTextColor(mContext.resources.getColor(R.color.gray))
//            }
//        }

//        val tvState = helper.getView<ImageView>(R.id.sensor_delete)
        helper.setText(R.id.group_name,item.name)
        if (item.isChecked) {
//            tvState.text = mContext.getString(R.string.selected)
//            tvState.setTextColor(mContext.resources.getColor(R.color.white))
//            tvState.setBackgroundColor(mContext.resources.getColor(R.color.primary))
//            tvState.isEnabled = false
            helper.setImageResource(R.id.sensor_delete,R.drawable.icon_checkbox_selected)
        } else {
//            tvState.text = mContext.getString(R.string.unSelect)
//            tvState.setBackgroundColor(mContext.resources.getColor(R.color.white))
            if(item.enableCheck){
//                tvState.setTextColor(mContext.resources.getColor(R.color.primary))
//                tvState.isEnabled = true
                helper.setImageResource(R.id.sensor_delete,R.drawable.icon_checkbox_unselected)
            }else{
//                tvState.setTextColor(mContext.resources.getColor(R.color.gray))
//                tvState.isEnabled = false
                helper.setImageResource(R.id.sensor_delete,R.drawable.icon_checkbox_unselected)
            }
        }
    }
}