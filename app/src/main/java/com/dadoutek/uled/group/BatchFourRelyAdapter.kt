package com.dadoutek.uled.group

import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbConnector


/**
 * 创建者     ZCL
 * 创建时间   2019/10/16 15:52
 * 描述
 *
 * 更新时间   批量分组冷暖灯彩灯适配器$
 * 更新描述
 */
class BatchFourRelayAdapter(layoutResId: Int, data: MutableList<DbConnector>) : BaseQuickAdapter<DbConnector, BaseViewHolder>(layoutResId, data) {
    private val allLightId: Long = 1
    override fun convert(helper: BaseViewHolder?, item: DbConnector?) {
        helper ?: return
        val icon = helper.getView<ImageView>(R.id.template_device_batch_icon)
        val groupName = helper.getView<TextView>(R.id.template_device_batch_title_blow)

        helper.setText(R.id.template_device_batch_title, item?.name)

        if (item?.isSelected == true)
            helper.setImageResource(R.id.template_device_batch_selected, R.drawable.icon_checkbox_selected)
         else
            helper.setImageResource(R.id.template_device_batch_selected, R.drawable.icon_checkbox_unselected)


        if (item?.belongGroupId !=allLightId) {
            helper.setTextColor(R.id.template_device_batch_title, mContext.getColor(R.color.blue_text))
                    .setTextColor(R.id.template_device_batch_title_blow, mContext.getColor(R.color.blue_text))
            groupName.visibility = View.VISIBLE
            if (TextUtils.isEmpty(item?.groupName)) {
                if (item?.belongGroupId != 1L)
                    groupName.text = DBUtils.getGroupByID(item?.belongGroupId ?: 1)?.name
            } else
                groupName.text = item?.groupName

            icon.setImageResource(R.drawable.icon_acceptor_s)
        } else {
            helper.setTextColor(R.id.template_device_batch_title, mContext.getColor(R.color.gray_3))
            groupName.visibility = View.GONE
            icon.setImageResource(R.drawable.icon_acceptor_s)
        }
    }
}
