package com.dadoutek.uled.switches

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.tellink.TelinkLightApplication


/**
 * 创建者     ZCL
 * 创建时间   2020/1/13 10:47
 * 描述  组列表单选
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GroupItemAdapter(resId: Int, data: MutableList<DbGroup>):BaseQuickAdapter<DbGroup,BaseViewHolder>(resId,data){
    override fun convert(helper: BaseViewHolder?, item: DbGroup?) {
        helper?.setText(R.id.template_device_batch_title,item?.name)
                ?.setVisible(R.id.template_device_batch_selected,true)
        if (item?.meshAddr == 0xffff)
            helper?.setText(R.id.template_device_batch_title, mContext.getString(R.string.allLight))

        helper?.getView<TextView>(R.id.template_device_batch_title_blow)?.visibility = View.GONE
        when {
            item?.checked != true -> helper?.getView<ImageView>(R.id.template_device_batch_selected)?.setImageResource(R.drawable.icon_checkbox_unselected)
            else -> helper?.getView<ImageView>(R.id.template_device_batch_selected)?.setImageResource(R.drawable.icon_checkbox_selected)
        }
    }
}

