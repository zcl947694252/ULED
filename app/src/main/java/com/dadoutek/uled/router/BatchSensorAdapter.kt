package com.dadoutek.uled.router

import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.blankj.utilcode.util.LogUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.dbModel.DbSensor
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.dbModel.DBUtils

class BatchSensorAdapter(layoutResId: Int, data: MutableList<DbSensor>) : BaseQuickAdapter<DbSensor, BaseViewHolder>(layoutResId, data) {
    // -70 至 -80一般  >=-65 很好
    private val allLightId: Long = 1
    override fun convert(helper: BaseViewHolder?, item: DbSensor?) {
        helper ?: return
        val icon = helper.getView<ImageView>(R.id.template_device_batch_icon)
        val groupName = helper.getView<TextView>(R.id.template_device_batch_title_blow)

        helper.setText(R.id.template_device_batch_title, item?.name)

        if (item?.selected == true) {
            helper.setImageResource(R.id.template_device_batch_selected, R.drawable.icon_checkbox_selected)
        } else {
            helper.setImageResource(R.id.template_device_batch_selected, R.drawable.icon_checkbox_unselected)
        }

        val routerByMac = DBUtils.getRouterByMac(item?.boundMac?:"")
        LogUtils.v("zcl-----------获取路由名称---${item?.boundMac?:""}----${routerByMac!![0].name}")
        if (routerByMac != null && routerByMac.size >= 1) {
            groupName.text = routerByMac[0].name
            groupName.visibility = View.VISIBLE
            helper.setTextColor(R.id.template_device_batch_title, mContext.getColor(R.color.blue_text))
                    .setTextColor(R.id.template_device_batch_title_blow, mContext.getColor(R.color.blue_text))
        } else {
            groupName.visibility = View.GONE
            helper.setTextColor(R.id.template_device_batch_title, mContext.getColor(R.color.gray_3))
        }

        if (item?.productUUID == DeviceType.LIGHT_RGB) {
            icon.setImageResource(R.drawable.icon_rgb_n)
        } else {
            icon.setImageResource(R.drawable.icon_light_n)
        }
    }
}