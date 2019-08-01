package com.dadoutek.uled.region.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.region.SettingItemBean

class SettingAdapter(layoutResId: Int, data: MutableList<SettingItemBean>) : BaseQuickAdapter<SettingItemBean,BaseViewHolder>(layoutResId, data){
    override fun convert(helper: BaseViewHolder?, item: SettingItemBean?) {
        helper!!.setImageResource(R.id.item_setting_icon, item!!.icon)
                .setText(R.id.item_setting_text,item.title)

    }
}
