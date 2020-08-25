package com.dadoutek.uled.router

import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.router.bean.TitleBean


/**
 * 创建者     ZCL
 * 创建时间   2020/8/13 17:33
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class TitleAdapter(resId: Int, data: MutableList<TitleBean>) : BaseQuickAdapter<TitleBean, BaseViewHolder>(resId, data) {
    override fun convert(helper: BaseViewHolder?, item: TitleBean?) {
        val tv = helper?.getView<TextView>(R.id.item_title_tv)
           val b = item?.isChexked == true
           helper?.setVisible(R.id.item_title_line, b)?.setText(R.id.item_title_tv,item?.title)

       if (b)
           tv?.setTextColor(mContext.getColor(R.color.primary))
        else
           tv?.setTextColor(mContext.getColor(R.color.colorGray))


    }

}