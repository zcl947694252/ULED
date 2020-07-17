package com.dadoutek.uled.region.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.network.bean.RegionAuthorizeBean


class AreaAuthorizeItemAdapter(layoutResId: Int, data: List<RegionAuthorizeBean>, var user: DbUser?) : BaseQuickAdapter<RegionAuthorizeBean, BaseViewHolder>(layoutResId, data) {
    @SuppressLint("StringFormatMatches")
    override fun convert(helper: BaseViewHolder?, item: RegionAuthorizeBean?) {
        item?.let {
            helper?.let {
                it.addOnClickListener(R.id.item_area_state)
                        .addOnClickListener(R.id.item_area_more)
                val personTv = it.getView<TextView>(R.id.item_area_share_person)
                if (item.count_all <= 0)
                    it.setText(R.id.item_area_title, item.name)
                else
                    it.setText(R.id.item_area_title, item.name + mContext.getString(R.string.total_device, item.count_all))

                    personTv.visibility = View.GONE

                Log.e("zcl","zcl*授权区域判断*****${user!!.last_region_id}*******************${user!!.last_authorizer_user_id}-----$user")

                if (item.id.toString() == user!!.last_region_id  && user!!.last_authorizer_user_id == item.authorizer_id.toString()) {
                    it.setText(R.id.item_area_state, mContext.getString(R.string.in_use))
                            .setTextColor(R.id.item_area_state, mContext.getColor(R.color.black_nine))
                            .setTextColor(R.id.item_area_title, mContext.getColor(R.color.blue_background))
                            .setVisible(R.id.item_area_more,false)
                } else {
                    it.setText(R.id.item_area_state, mContext.getString(R.string.confirm))
                            .setTextColor(R.id.item_area_state, mContext.getColor(R.color.black_three))
                            .setTextColor(R.id.item_area_title, mContext.getColor(R.color.black_three))
                         .setVisible(R.id.item_area_more, true)
                }
            }
        }
    }
}