package com.dadoutek.uled.region.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.region.bean.RegionBean

class RegionDialogAdapter(layoutId: Int, data: MutableList<RegionBean>?) : BaseQuickAdapter<RegionBean, BaseViewHolder>(layoutId,data) {
    override fun convert(helper: BaseViewHolder?, item: RegionBean?) {

    }
}
