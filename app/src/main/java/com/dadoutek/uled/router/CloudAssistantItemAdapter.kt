package com.dadoutek.uled.router

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.network.ThirdPartyBean


/**
 * 创建者     ZCL
 * 创建时间   2020/11/9 17:59
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class CloudAssistantItemAdapter(resId: Int, data: MutableList<ThirdPartyBean>) :BaseQuickAdapter<ThirdPartyBean,BaseViewHolder>(resId, data) {
    override fun convert(helper: BaseViewHolder?, item: ThirdPartyBean?) {
        helper
                ?.setVisible(R.id.item_setting_icon,true)
                ?.setVisible(R.id.item_setting_back,false)
                ?.setText(R.id.item_setting_text,item?.name)
    }
}