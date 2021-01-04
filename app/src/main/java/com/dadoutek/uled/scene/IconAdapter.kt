package com.dadoutek.uled.scene

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import kotlinx.android.synthetic.main.item_icon.view.*


/**
 * 创建者     ZCL
 * 创建时间   2020/6/17 11:40
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class IconAdapter(resId: Int, data: ArrayList<Int>) :BaseQuickAdapter<Int,BaseViewHolder>(resId,data){
    override fun convert(helper: BaseViewHolder?, item: Int?) {
        helper?.setImageResource(R.id.item_icon_id,item!!)
    }

}