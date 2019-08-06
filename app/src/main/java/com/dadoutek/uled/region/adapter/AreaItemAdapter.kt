package com.dadoutek.uled.region.adapter

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.region.bean.RegionBean


class AreaItemAdapter(layoutResId: Int, data: List<RegionBean>, var last_region_id: String) : BaseQuickAdapter<RegionBean, BaseViewHolder>(layoutResId, data) {
    @SuppressLint("StringFormatMatches")
    override fun convert(helper: BaseViewHolder?, item: RegionBean?) {
        item?.let {
            helper?.let {
                it.addOnClickListener(R.id.item_area_state)
                        .addOnClickListener(R.id.item_area_more)
                val personTv = it.getView<TextView>(R.id.item_area_share_person)
                if (item.count_all <= 0)
                    it.setText(R.id.item_area_title, item.name)
                else
                    it.setText(R.id.item_area_title, item.name + mContext.getString(R.string.total_device, item.count_all))

                val b = item.ref_users != null && item.ref_users!!.isNotEmpty()

                if (b) {
                    personTv.text = mContext.getString(R.string.share_person, item.ref_users!!.size)
                    personTv.visibility = View.VISIBLE
                } else {
                    personTv.visibility = View.GONE
                }

                if (item.id.toString() == last_region_id) {
                    it.setText(R.id.item_area_state, mContext.getString(R.string.in_use))
                            .setTextColor(R.id.item_area_state, mContext.getColor(R.color.black_nine))
                            .setTextColor(R.id.item_area_title, mContext.getColor(R.color.blue_background))
                } else {
                    it.setText(R.id.item_area_state, mContext.getString(R.string.use))
                            .setTextColor(R.id.item_area_state, mContext.getColor(R.color.black_three))
                            .setTextColor(R.id.item_area_title, mContext.getColor(R.color.black_three))
                }
            }
        }
    }
}