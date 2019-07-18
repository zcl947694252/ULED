package com.dadoutek.uled.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R


class AreaItemAdapter(layoutResId: Int, data: List<String>) : BaseQuickAdapter<String, BaseViewHolder>(layoutResId, data) {
    override fun convert(helper: BaseViewHolder?, item: String?) {
        helper!!
                .setText(R.id.item_area_title, item)
        if (item!!.equals("2"))
            helper!!.setText(R.id.item_area_state, "使用中")
                    .setTextColor(R.id.item_area_state, mContext.getColor(R.color.black_nine))
        else
            helper!!.setText(R.id.item_area_state, "使用")
                    .setTextColor(R.id.item_area_state, mContext.getColor(R.color.black))
    }
}
