package com.dadoutek.uled.group

import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constants
import com.dadoutek.uled.model.dbModel.DbLight
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.StringUtils
import org.jetbrains.anko.padding
import org.jetbrains.anko.textColor


/**
 * 创建者     ZCL
 * 创建时间   2020/6/10 15:17
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GroupOTALightAdapter(resId: Int, data: MutableList<DbLight>) : BaseQuickAdapter<DbLight, BaseViewHolder>(resId, data) {
    private var isRgb = false
    fun setDeviceType(b: Boolean) {
        isRgb = b
    }

    override fun convert(helper: BaseViewHolder, item: DbLight?) {
        val groupName = helper.getView<TextView>(R.id.group_ota_group_name)
        val deviceName = helper.getView<TextView>(R.id.group_ota_name)
        val version = helper.getView<TextView>(R.id.group_ota_version)
        val otaUpdate = helper.getView<ImageView>(R.id.group_ota_update)
        otaUpdate.padding = 0
        version.text = item?.version
        deviceName.text = item?.name
        groupName.text = StringUtils.getLightGroupName(item)

        val connectDevice = TelinkLightApplication.getApp().connectDevice
        if (connectDevice != null && connectDevice.meshAddress == item?.meshAddr) {
            deviceName.textColor = mContext.getColor(R.color.blue_text)
            groupName.textColor = mContext.getColor(R.color.blue_text)
            version.textColor = mContext.getColor(R.color.blue_text)
        } else {
            deviceName.textColor = mContext.getColor(R.color.gray_3)
            groupName.textColor = mContext.getColor(R.color.gray_3)
            version.textColor = mContext.getColor(R.color.gray_3)
        }
        if (isRgb)
            helper.setImageResource(R.id.group_ota_icon, R.drawable.icon_rgb_n)
        else
            helper.setImageResource(R.id.group_ota_icon, R.drawable.icon_light_n)

        if (item?.isSupportOta == true && !item.isMostNew&& TelinkLightApplication.mapBin.isNotEmpty()) {
            when {
                Constants.IS_ROUTE_MODE && item.isGetVersion -> {
                    when {
                        isRgb -> helper.setImageResource(R.id.group_ota_icon, R.drawable.icon_rgb_no_circle)
                        else -> helper.setImageResource(R.id.group_ota_icon, R.drawable.icon_light_no_circle)
                    }
                    helper.setImageResource(R.id.group_ota_update, R.drawable.uparrow)
                }
                else -> {
                    when {
                        isRgb -> helper.setImageResource(R.id.group_ota_icon, R.drawable.icon_rgb_close)
                        else -> helper.setImageResource(R.id.group_ota_icon, R.drawable.icon_light_close_g)
                    }
                    helper.setImageResource(R.id.group_ota_update, R.drawable.up_arrow_g)

                }
            }
        } else {
            when {
                isRgb -> helper.setImageResource(R.id.group_ota_icon, R.drawable.icon_rgb_close)
                else -> helper.setImageResource(R.id.group_ota_icon, R.drawable.icon_light_close_g)
            }
            helper.setImageResource(R.id.group_ota_update, R.drawable.up_arrow_g)
        }
    }

}