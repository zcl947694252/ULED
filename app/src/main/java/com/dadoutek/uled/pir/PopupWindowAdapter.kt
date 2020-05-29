package com.dadoutek.uled.pir

import android.widget.SimpleAdapter
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R


/**
 * 创建者     ZCL
 * 创建时间   2020/5/19 9:59
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class PopupWindowAdapter(resId:Int,data:List<ItemCheckBean>) :BaseQuickAdapter<ItemCheckBean,BaseViewHolder>(resId,data) {
    override fun convert(helper: BaseViewHolder?, item: ItemCheckBean?) {
        helper?.setText(R.id.item_single_tv,item?.title)
        val title = helper?.getView<TextView>(R.id.item_single_tv)
        if (item?.checked==true)
            title?.setBackgroundColor(mContext.getColor(R.color.blue_background))
        else
            title?.setBackgroundColor(mContext.getColor(R.color.gray_e))
    }
}