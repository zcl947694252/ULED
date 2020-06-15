package com.dadoutek.uled.switches

import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbScene


/**
 * 创建者     ZCL
 * 创建时间   2020/1/2 17:22
 * 描述 场景列表
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 *
 */
class SceneListAdapter(resId: Int, data: List<DbScene>) : BaseQuickAdapter<DbScene, BaseViewHolder>(resId, data) {
    override fun convert(helper: BaseViewHolder?, item: DbScene?) {
        helper?.setText(R.id.template_device_batch_title, item?.name)
                ?.setImageResource(R.id.template_device_batch_icon, R.drawable.icon_sence)
                ?.setVisible(R.id.template_device_batch_selected, false)
        helper?.getView<TextView>(R.id.template_device_batch_title)?.visibility = View.GONE
    }

}