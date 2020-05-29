package com.dadoutek.uled.switches

import android.widget.ImageView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.pir.CheckItemBean

class SceneMoreItemAdapter(resId: Int, checkList: ArrayList<CheckItemBean>) : BaseQuickAdapter<CheckItemBean, BaseViewHolder>(resId,checkList){
    override fun convert(helper: BaseViewHolder?, item: CheckItemBean?) {
        helper?.setText(R.id.item_more_group_name,item?.name)
        val imageView = helper?.getView<ImageView>(R.id.item_more_group_check)
        if (item?.checked==true)
            imageView?.setImageResource(R.drawable.icon_checkbox_selected)
        else
            imageView?.setImageResource(R.drawable.icon_checkbox_unselected)
    }
}
