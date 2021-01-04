package com.dadoutek.uled.region.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.region.bean.RegionBean

class UnbindNetWorkAdapter(layoutId: Int, data: List<RegionBean.RefUsersBean>) : BaseQuickAdapter<RegionBean.RefUsersBean,BaseViewHolder>(layoutId,data) {
    override fun convert(helper: BaseViewHolder?, item: RegionBean.RefUsersBean?) {
        helper?.setText(R.id.item_unbind_net_name, item?.phone)?.addOnClickListener(R.id.item_unbind_net_unbind)
    }

}
