package com.dadoutek.uled.switches

import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.pir.CheckItemBean
import com.dadoutek.uled.util.OtherUtils

class SceneMoreItemAdapter(resId: Int, checkList: ArrayList<CheckItemBean>) : BaseQuickAdapter<CheckItemBean, BaseViewHolder>(resId,checkList){
    override fun convert(helper: BaseViewHolder?, item: CheckItemBean?) {
        var icon = if (TextUtils.isEmpty(item?.imgName)) R.drawable.icon_1 else OtherUtils.getResourceId(item?.imgName, mContext)
        helper?.setText(R.id.template_device_batch_title, item?.name)
                ?.setImageResource(R.id.template_device_batch_icon,icon)
                ?.setVisible(R.id.template_device_batch_selected, true)

        helper?.getView<TextView>(R.id.template_device_batch_title_blow)?.visibility = View.GONE
        if (item?.checked == true)
            helper?.getView<ImageView>(R.id.template_device_batch_selected)?.setImageResource(R.drawable.icon_checkbox_selected)
        else
            helper?.getView<ImageView>(R.id.template_device_batch_selected)?.setImageResource(R.drawable.icon_checkbox_unselected)
    }
}
