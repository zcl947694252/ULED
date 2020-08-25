package com.dadoutek.uled.pir

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R

class GroupMoreItemAdapter(resId: Int, checkList: ArrayList<CheckItemBean>) : BaseQuickAdapter<CheckItemBean, BaseViewHolder>(resId,checkList){
    override fun convert(helper: BaseViewHolder?, item: CheckItemBean?) {
        helper?.setText(R.id.template_device_batch_title, item?.name)
                ?.setVisible(R.id.template_device_batch_selected, true)

        helper?.getView<TextView>(R.id.template_device_batch_title_blow)?.visibility = View.GONE
        if (item?.checked == true)
            helper?.getView<ImageView>(R.id.template_device_batch_selected)?.setImageResource(R.drawable.icon_checkbox_selected)
        else
            helper?.getView<ImageView>(R.id.template_device_batch_selected)?.setImageResource(R.drawable.icon_checkbox_unselected)
    }
}
