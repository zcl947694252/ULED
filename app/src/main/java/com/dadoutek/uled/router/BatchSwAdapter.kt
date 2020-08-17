package com.dadoutek.uled.router

import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.model.DeviceType

class BatchSwAdapter(layoutResId: Int, data: MutableList<DbSwitch>) : BaseQuickAdapter<DbSwitch, BaseViewHolder>(layoutResId, data) {
    // -70 至 -80一般  >=-65 很好
    private val allLightId: Long = 1
    override fun convert(helper: BaseViewHolder?, item: DbSwitch?) {
        helper ?: return
        val icon = helper.getView<ImageView>(R.id.template_device_batch_icon)
        val groupName = helper.getView<TextView>(R.id.template_device_batch_title_blow)

        helper.setText(R.id.template_device_batch_title, item?.name)

        if (item?.isSelected == true) {
            helper.setImageResource(R.id.template_device_batch_selected, R.drawable.icon_checkbox_selected)
        } else {
            helper.setImageResource(R.id.template_device_batch_selected, R.drawable.icon_checkbox_unselected)
        }


        groupName.text = item?.routerName

        if (item?.routerId != allLightId) {
            helper.setTextColor(R.id.template_device_batch_title, mContext.getColor(R.color.blue_text))
                    .setTextColor(R.id.template_device_batch_title_blow, mContext.getColor(R.color.blue_text))
            groupName.visibility = View.VISIBLE
        } else {
            helper.setTextColor(R.id.template_device_batch_title, mContext.getColor(R.color.gray_3))
            groupName.visibility = View.GONE
        }

        if (item?.productUUID == DeviceType.LIGHT_RGB) {
            icon.setImageResource(R.drawable.icon_rgb_n)
        } else {
            icon.setImageResource(R.drawable.icon_light_n)
        }
    }
}