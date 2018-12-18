package com.dadoutek.uled.scene

import android.content.Context
import android.widget.SeekBar
import android.widget.TextView

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtherUtils
import java.util.Objects

/**
 * Created by hejiajun on 2018/5/5.
 */

class SceneEditListAdapter(layoutResId: Int, data: List<DbGroup>) : BaseQuickAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: DbGroup) {
        helper.setText(R.id.group_name,item.name)
        val tvState = helper.getView<TextView>(R.id.group_check_state)
        if (item.isChecked) {
            tvState.text = mContext.getString(R.string.selected)
            tvState.setTextColor(mContext.resources.getColor(R.color.white))
            tvState.setBackgroundColor(mContext.resources.getColor(R.color.primary))
        } else {
            tvState.text = mContext.getString(R.string.unSelect)
            tvState.setBackgroundColor(mContext.resources.getColor(R.color.white))
            if(item.enableCheck){
                tvState.setTextColor(mContext.resources.getColor(R.color.primary))
            }else{
                tvState.setTextColor(mContext.resources.getColor(R.color.gray))
            }
        }
    }
}
