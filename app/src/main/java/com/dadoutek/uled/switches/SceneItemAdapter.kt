package com.dadoutek.uled.switches

import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.util.DensityUtil
import com.dadoutek.uled.util.OtherUtils


/**
 * 创建者     ZCL
 * 创建时间   2020/1/13 10:47
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class SceneItemAdapter(resId: Int, data: MutableList<DbScene>) : BaseQuickAdapter<DbScene, BaseViewHolder>(resId, data) {
    override fun convert(helper: BaseViewHolder?, item: DbScene?) {
        var icon = if (TextUtils.isEmpty(item?.imgName)) R.drawable.icon_out else OtherUtils.getResourceId(item?.imgName, mContext)
        helper?.setText(R.id.template_device_batch_title, item?.name)
                ?.setImageResource(R.id.template_device_batch_icon,icon)
                ?.setVisible(R.id.template_device_batch_selected, true)
            val ly = helper?.getView<ImageView>(R.id.template_device_batch_icon)
            ly?.layoutParams?.width = DensityUtil.dip2px(mContext,40f)
            ly?.layoutParams?.height = DensityUtil.dip2px(mContext,40f)
            helper?.setBackgroundRes(R.id.template_device_batch_icon_ly, R.drawable.rect_r15_graye)

        helper?.getView<TextView>(R.id.template_device_batch_title_blow)?.visibility = View.GONE
        if (item?.checked == true)
            helper?.getView<ImageView>(R.id.template_device_batch_selected)?.setImageResource(R.drawable.icon_checkbox_selected)
        else
            helper?.getView<ImageView>(R.id.template_device_batch_selected)?.setImageResource(R.drawable.icon_checkbox_unselected)
    }

}