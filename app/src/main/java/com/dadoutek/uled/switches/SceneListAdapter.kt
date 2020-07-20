package com.dadoutek.uled.switches

import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.util.OtherUtils


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
        var icon = if (TextUtils.isEmpty(item?.imgName)) R.drawable.icon_1 else OtherUtils.getResourceId(item?.imgName, mContext)
        helper?.setText(R.id.template_device_batch_title2, item?.name)
                ?.setImageResource(R.id.template_device_batch_icon2,icon)
                ?.setVisible(R.id.template_device_batch_selected2, true)

        helper?.getView<TextView>(R.id.template_device_batch_title_blow2)?.visibility = View.GONE
        if (item?.checked == true)
            helper?.getView<ImageView>(R.id.template_device_batch_selected2)?.setImageResource(R.drawable.icon_checkbox_selected)
        else
            helper?.getView<ImageView>(R.id.template_device_batch_selected2)?.setImageResource(R.drawable.icon_checkbox_unselected)
    }
}