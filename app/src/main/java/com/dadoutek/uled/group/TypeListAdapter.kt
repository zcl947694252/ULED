package com.dadoutek.uled.group

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R


/**
 * 创建者     ZCL
 * 创建时间   2020/1/7 11:02
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class TypeListAdapter(layoutResId: Int, data: MutableList<String>) : BaseQuickAdapter<String, BaseViewHolder>(layoutResId, data) {
    override fun convert(helper: BaseViewHolder?, item: String?) {
        helper?.setText(R.id.tv_group_name, item)
    }

}