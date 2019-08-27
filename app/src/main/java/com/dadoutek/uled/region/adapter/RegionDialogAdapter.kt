package com.dadoutek.uled.region.adapter

import android.widget.ImageView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.region.bean.RegionBean

class RegionDialogAdapter(layoutId: Int, data: List<RegionBean>) : BaseQuickAdapter<RegionBean, BaseViewHolder>(layoutId,data) {
    override fun convert(helper: BaseViewHolder?, item: RegionBean?) {
            helper?.setText(R.id.item_area_dialog_title,item?.name)
        var iv = helper?.getView<ImageView>(R.id.item_area_dialog_more)
        if (item?.is_selected!!)
            iv?.setImageResource(R.drawable.icon_checkbox_selected)
        else
            iv?.setImageResource(R.drawable.icon_checkbox_unselected)
    }
}
