package com.dadoutek.uled.fragment

import android.view.View
import android.widget.CheckBox
import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.telink.bluetooth.light.ConnectionStatus

/**
 * 群组页面内的设备的List的Adapter
 * @param isDelete      是否处于删除模式
 */
class GroupListAdapter(layoutResId: Int, data: List<DbGroup>, internal var isDelete: Boolean) : BaseItemDraggableAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {


    override fun convert(helper: BaseViewHolder, group: DbGroup?) {

        if (group != null) {
            val num = group.deviceCount //内含设备的数量

            val deleteIcon = helper.getView<CheckBox>(R.id.selected_group)
            if (isDelete) {
                deleteIcon.visibility = View.VISIBLE
            } else {
                deleteIcon.visibility = View.GONE
            }

            helper.setText(R.id.group_num, TelinkLightApplication.getApp().getString(R.string.total) + num + TelinkLightApplication.getApp().getString(R.string.piece))
                    .setImageResource(R.id.btn_set, R.drawable.icon_setting_group)

            if (num>0){
                helper.setImageResource(R.id.btn_set, R.drawable.icon_setting_group)
                        .addOnClickListener(R.id.btn_on)
                        .addOnClickListener(R.id.btn_off)
                        .addOnClickListener(R.id.btn_set)

                if (group.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL || group.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB
                        || group.deviceType == Constant.DEVICE_TYPE_CONNECTOR || group.deviceType == Constant.DEVICE_TYPE_CURTAIN
                        || group.deviceType == Constant.DEVICE_TYPE_DEFAULT_ALL) {
                    if (group.connectionStatus == ConnectionStatus.ON.value) {
                        helper.setImageResource(R.id.btn_on, R.drawable.icon_open_group)
                                .setImageResource(R.id.btn_off, R.drawable.icon_down_group)
                                .setTextColor(R.id.textView8, TelinkLightApplication.getApp().getColor(R.color.white))
                                .setTextColor(R.id.textView11, TelinkLightApplication.getApp().getColor(R.color.black_nine))

                    } else if (group.connectionStatus == ConnectionStatus.OFF.value) {
                        helper.setImageResource(R.id.btn_on, R.drawable.icon_down_group)
                                .setImageResource(R.id.btn_off, R.drawable.icon_open_group)
                                .setTextColor(R.id.textView8, TelinkLightApplication.getApp().getColor(R.color.black_nine))
                                .setTextColor(R.id.textView11, TelinkLightApplication.getApp().getColor(R.color.white))
                    }
                }
            }else{
                helper.setImageResource(R.id.btn_set, R.drawable.shezhi)
                        .setImageResource(R.id.btn_on, R.drawable.icon_gray_group)
                        .setImageResource(R.id.btn_off, R.drawable.icon_down_group)
                        .setTextColor(R.id.textView8, TelinkLightApplication.getApp().getColor(R.color.white))
                        .setTextColor(R.id.textView11, TelinkLightApplication.getApp().getColor(R.color.color_c8))
            }

            if (group.textColor == 0)
                group.textColor = mContext.resources.getColor(R.color.black)

            if (group.meshAddr == 0xffff)
                helper.setText(R.id.tv_group_name, TelinkLightApplication.getApp().getString(R.string.allLight))
            else
                helper.setText(R.id.tv_group_name, group.name)


            if (group.isSelected)
                helper.setChecked(R.id.selected_group, true)
            else
                helper.setChecked(R.id.selected_group, false)

            helper.setTextColor(R.id.tv_group_name, group.textColor)
                    .addOnClickListener(R.id.item_layout)
        }
    }

    /**
     * 改变状态
     * @param isDelete  是否处于删除状态
     */
    fun changeState(isDelete: Boolean) {
        DeviceType
        this.isDelete = isDelete
    }
}


