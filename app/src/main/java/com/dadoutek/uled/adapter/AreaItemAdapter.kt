package com.dadoutek.uled.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbRegion


class AreaItemAdapter(layoutResId: Int, data: List<DbRegion>, var last_region_id: String) : BaseQuickAdapter<DbRegion, BaseViewHolder>(layoutResId, data) {
    override fun convert(helper: BaseViewHolder?, item: DbRegion?) {
        item?.let {
            helper!!.setText(R.id.item_area_title, item.name)
            if (item.id.toString() == last_region_id)
                helper!!.setText(R.id.item_area_state, mContext.getString(R.string.in_use))
                        .setTextColor(R.id.item_area_state, mContext.getColor(R.color.black_nine))
            else
                helper!!.setText(R.id.item_area_state, mContext.getString(R.string.use))
                        .setTextColor(R.id.item_area_state, mContext.getColor(R.color.black))
        }
    }
}