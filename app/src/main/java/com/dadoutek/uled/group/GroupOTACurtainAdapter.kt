package com.dadoutek.uled.group

import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.StringUtils
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
class GroupOTACurtainAdapter(resId: Int, data: MutableList<DbCurtain>) : BaseQuickAdapter<DbCurtain, BaseViewHolder>(resId, data) {
    override fun convert(helper: BaseViewHolder, item: DbCurtain?) {
        val groupName = helper.getView<TextView>(R.id.group_ota_group_name)
        val deviceName = helper.getView<TextView>(R.id.group_ota_name)
        val version = helper.getView<TextView>(R.id.group_ota_version)
        version.text = item?.version
        deviceName.text = item?.name
        groupName.text = StringUtils.getCurtainGroupName(item)

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

        if (item?.isSupportOta == true) {
            helper.setImageResource(R.id.group_ota_icon, R.drawable.icon_device_open)
                    .setImageResource(R.id.group_ota_update, R.drawable.add)
        } else {
            helper.setImageResource(R.id.group_ota_icon, R.drawable.icon_device_down)
                    .setImageResource(R.id.group_ota_update, R.drawable.add)
        }
    }
}