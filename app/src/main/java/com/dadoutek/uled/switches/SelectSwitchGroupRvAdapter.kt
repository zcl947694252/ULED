package com.dadoutek.uled.switches

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbGroup

class SelectSwitchGroupRvAdapter(layoutResId: Int, data: MutableList<DbGroup>?) : BaseQuickAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {
    var selectedPos = 0
    override fun convert(helper: BaseViewHolder?, item: DbGroup?) {
        helper?.setText(R.id.checkBox, item?.name)
                ?.addOnClickListener(R.id.btnOff)
                ?.addOnClickListener(R.id.btnOn)
                ?.addOnClickListener(R.id.checkBox)
        if (helper?.layoutPosition == selectedPos) {
            helper.setChecked(R.id.checkBox, true)
        }
    }
}