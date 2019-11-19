package com.dadoutek.uled.group

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbCurtain


/**
 * 创建者     ZCL
 * 创建时间   2019/10/16 15:52
 * 描述
 *
 * 更新时间   批量分组冷暖灯彩灯适配器$
 * 更新描述
 */
class BatchFourCurtainAdapter(layoutResId: Int, data: MutableList<DbCurtain>) : BaseQuickAdapter<DbCurtain, BaseViewHolder>(layoutResId, data) {
    override fun convert(helper: BaseViewHolder?, item: DbCurtain?) {
        helper ?: return
        val icon = helper.getView<ImageView>(R.id.batch_img_icon)
        val groupName = helper.getView<TextView>(R.id.batch_tv_group_name)
        helper.setText(R.id.batch_tv_device_name, item?.name)

        if (item?.isSelected == true) {
            helper.setImageResource(R.id.batch_selected, R.drawable.icon_checkbox_selected)
        } else {
            helper.setImageResource(R.id.batch_selected, R.drawable.icon_checkbox_unselected)
        }

        if (item?.groupName != "") {
            helper.setTextColor(R.id.batch_tv_device_name, mContext.getColor(R.color.blue_text))
                    .setTextColor(R.id.batch_tv_group_name, mContext.getColor(R.color.blue_text))
            groupName.visibility = View.VISIBLE
            groupName.text = item?.groupName

            icon.setImageResource(R.drawable.icon_curtain)

        } else {
            helper.setTextColor(R.id.batch_tv_device_name, mContext.getColor(R.color.gray_3))
            groupName.visibility = View.GONE
            icon.setImageResource(R.drawable.curtain_off)
        }
    }
}
