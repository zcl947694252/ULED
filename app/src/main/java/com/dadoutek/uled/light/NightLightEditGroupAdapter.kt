package com.dadoutek.uled.light

import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbGroup

class NightLightEditGroupAdapter (layoutResId: Int, data: List<DbGroup>) : BaseQuickAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: DbGroup) {
        helper.setText(R.id.group_name,item.name)
        helper.addOnClickListener(R.id.group_check_state)
        val tvState = helper.getView<TextView>(R.id.group_check_state)
        if (item.isChecked) {
            tvState.text = "选中"
            tvState.setTextColor(mContext.resources.getColor(R.color.white))
            tvState.setBackgroundColor(mContext.resources.getColor(R.color.primary))
        } else {
            tvState.text = "未选中"
            tvState.setBackgroundColor(mContext.resources.getColor(R.color.white))
            if(item.enableCheck){
                tvState.setTextColor(mContext.resources.getColor(R.color.primary))
            }else{
                tvState.setTextColor(mContext.resources.getColor(R.color.gray))
            }
        }
    }
}