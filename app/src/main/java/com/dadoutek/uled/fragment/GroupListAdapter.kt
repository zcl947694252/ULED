package com.dadoutek.uled.fragment

import android.view.View
import android.widget.CheckBox
import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DbGroup
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
                    .addOnClickListener(R.id.selected_group)

            val isSuportOpenOrClose = (group.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL || group.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB
                    || group.deviceType == Constant.DEVICE_TYPE_CONNECTOR || group.deviceType == Constant.DEVICE_TYPE_DEFAULT_ALL)

            if (isSuportOpenOrClose) {//支持点击 使用设置开关三个图标 将其显示 不支持设置按钮图标使其隐藏
                if (num > 0) {
                    helper.setImageResource(R.id.btn_set, R.drawable.icon_setting_group)
                            .addOnClickListener(R.id.btn_on)
                            .addOnClickListener(R.id.btn_off)
                            .addOnClickListener(R.id.tv_on)
                            .addOnClickListener(R.id.tv_off)
                            .setVisible(R.id.view3, true)
                            .setVisible(R.id.view4, true)
                            .setVisible(R.id.btn_on, true)
                            .setVisible(R.id.btn_on, true)
                            .setVisible(R.id.btn_off, true)
                            .setVisible(R.id.btn_set, true)
                            .setVisible(R.id.tv_on, true)
                            .setVisible(R.id.tv_off, true)
                            .setVisible(R.id.curtain_setting, false)

                    if (group.connectionStatus == ConnectionStatus.ON.value) {
                        helper.setImageResource(R.id.btn_on, R.drawable.icon_open_group)
                                .setImageResource(R.id.btn_off, R.drawable.icon_down)
                                .setTextColor(R.id.tv_on, TelinkLightApplication.getApp().getColor(R.color.white))
                                .setTextColor(R.id.tv_off, TelinkLightApplication.getApp().getColor(R.color.black_nine))
                    } else if (group.connectionStatus == ConnectionStatus.OFF.value) {
                        helper.setImageResource(R.id.btn_on, R.drawable.icon_down)
                                .setImageResource(R.id.btn_off, R.drawable.icon_open_group)
                                .setTextColor(R.id.tv_on, TelinkLightApplication.getApp().getColor(R.color.black_nine))
                                .setTextColor(R.id.tv_off, TelinkLightApplication.getApp().getColor(R.color.white))
                    }
                } else if (num <= 0) {//group.deviceType == Constant.DEVICE_TYPE_CURTAIN  窗帘不使用原本三个图标 使用新的按钮
                    setCanNotClik(helper)
                }
                helper.addOnClickListener(R.id.btn_set).setImageResource(R.id.btn_set, R.drawable.icon_setting_group)
            } else {
                setNoClik(helper)
            }

            if (group.textColor == 0)
                group.textColor = mContext.resources.getColor(R.color.black)

            if (group.meshAddr == 0xffff)
                helper.setText(R.id.template_device_name, TelinkLightApplication.getApp().getString(R.string.allLight))
            else
                helper.setText(R.id.template_device_name, group.name)


            if (group.isSelected)
                helper.setChecked(R.id.selected_group, true)
            else
                helper.setChecked(R.id.selected_group, false)

            helper.setTextColor(R.id.template_device_name, group.textColor)
                    .addOnClickListener(R.id.item_layout)
        }
    }

    private fun setCanNotClik(helper: BaseViewHolder) {
        helper.setImageResource(R.id.btn_set, R.drawable.shezhi)
                .setImageResource(R.id.btn_on, R.drawable.icon_gray_group)
                .setImageResource(R.id.btn_off, R.drawable.icon_down_group)
                .setVisible(R.id.view3, true)
                .setVisible(R.id.view4, true)
                .setVisible(R.id.btn_on, true)
                .setVisible(R.id.btn_on, true)
                .setVisible(R.id.btn_off, true)
                .setVisible(R.id.btn_set, true)
                .setVisible(R.id.tv_on, true)
                .setVisible(R.id.tv_off, true)
                .setVisible(R.id.curtain_setting, false)
                .setTextColor(R.id.tv_on, TelinkLightApplication.getApp().getColor(R.color.white))
                .setTextColor(R.id.tv_off, TelinkLightApplication.getApp().getColor(R.color.color_c8))
    }

    private fun setNoClik(helper: BaseViewHolder) {
        helper.setImageResource(R.id.btn_set, R.drawable.icon_setting_group_no)
                .setVisible(R.id.view3, false)
                .setVisible(R.id.view4, false)
                .setVisible(R.id.btn_on, false)
                .setVisible(R.id.btn_off, false)
                .setVisible(R.id.tv_on, false)
                .setVisible(R.id.tv_off, false)
                .setVisible(R.id.btn_set, false)
                .setVisible(R.id.curtain_setting, true)
                .addOnClickListener(R.id.curtain_setting)
    }

    /**
     * 改变状态
     * @param isDelete  是否处于删除状态
     */
    fun changeState(isDelete: Boolean) {
        this.isDelete = isDelete
    }
}


