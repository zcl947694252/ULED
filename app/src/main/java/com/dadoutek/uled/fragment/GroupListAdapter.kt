package com.dadoutek.uled.fragment

import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.telink.bluetooth.light.ConnectionStatus

/**
 * 群组页面内的设备的List的Adapter
 * @param isDelete      是否处于删除模式
 */
class GroupListAdapter(layoutResId: Int, data: List<DbGroup>, internal var isDelete: Boolean) : BaseItemDraggableAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {
    override fun convert(helper: BaseViewHolder, group: DbGroup?) {
        if (group != null) {
            val isSuportOpenOrClose = (group.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL || group.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB
                    || group.deviceType == Constant.DEVICE_TYPE_CONNECTOR || group.deviceType == Constant.DEVICE_TYPE_DEFAULT_ALL)

            helper.addOnClickListener(R.id.template_device_more)
                    .addOnClickListener(R.id.template_device_card_delete)
           // if (isSuportOpenOrClose)//支持点击 使用设置开关三个图标 将其显示 不支持设置按钮图标使其隐藏
                helper.addOnClickListener(R.id.template_device_icon)
         //   else
               // helper.setImageResource(R.id.template_device_icon, R.drawable.icon_group_g_n)

            when {
                group.deviceCount > 0 -> {
                    if (group.meshAddr == 0xffff) { //所有组允许点击
                        helper.addOnClickListener(R.id.template_device_setting)
                                .addOnClickListener(R.id.template_device_icon)
                                .setImageResource(R.id.template_device_setting, R.drawable.icon_setting_n)
                    } else {
                        helper.setImageResource(R.id.template_device_setting, R.drawable.icon_setting_n)//设置
                                .setImageResource(R.id.template_device_more, R.drawable.icon_more)//进入
                                .setImageResource(R.id.template_device_icon, R.drawable.icon_group_g_n)
                                .addOnClickListener(R.id.template_device_setting)
                        if (group.deviceType != Constant.DEVICE_TYPE_CURTAIN)
                            helper.addOnClickListener(R.id.template_device_icon)
                    }
                    when (group.connectionStatus) {
                        ConnectionStatus.ON.value -> helper.setImageResource(R.id.template_device_icon, R.drawable.icon_group_n)
                        ConnectionStatus.OFF.value -> helper.setImageResource(R.id.template_device_icon, R.drawable.icon_group_g_n)
                    }
                }
                group.deviceCount <= 0 -> {
                    if (group.meshAddr == 0xffff) {  //所有组允许点击
                        helper.addOnClickListener(R.id.template_device_setting)
                                .addOnClickListener(R.id.template_device_icon)
                                .setImageResource(R.id.template_device_setting, R.drawable.icon_setting_n)
                        when (group.connectionStatus) {
                            ConnectionStatus.ON.value -> helper.setImageResource(R.id.template_device_icon, R.drawable.icon_group_n)
                            ConnectionStatus.OFF.value -> helper.setImageResource(R.id.template_device_icon, R.drawable.icon_group_g_n)
                        }
                    } else {
                        helper.setImageResource(R.id.template_device_setting, R.drawable.icon_setting_n_g)//设置
                                .setImageResource(R.id.template_device_more, R.drawable.icon_more_g)//进入
                                .setImageResource(R.id.template_device_icon, R.drawable.icon_group_g_n)
                                //.addOnClickListener(R.id.template_device_icon)
                    }
                }//group.deviceType == Constant.DEVICE_TYPE_CURTAIN  窗帘不使用原本三个图标 使用新的按钮
            }


            if (group.textColor == 0)
                group.textColor = mContext.resources.getColor(R.color.black)

            if (group.meshAddr == 0xffff)
                helper.setText(R.id.template_device_group_name, TelinkLightApplication.getApp().getString(R.string.allLight))
                        .setVisible(R.id.template_gp_name, false)
            else
                helper.setText(R.id.template_device_group_name, group.name)
                        .setVisible(R.id.template_gp_name, Constant.IS_OPEN_AUXFUN)
                        .setText(R.id.template_gp_name, mContext.getString(R.string.number)+":"+group.deviceCount)

            if (helper.adapterPosition != 0)
                helper.setVisible(R.id.template_device_card_delete, isDelete)
                        .setVisible(R.id.template_device_setting, true)
                        .setVisible(R.id.template_device_more, true)
            else
                if (group.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL || group.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB || group.deviceType == Constant.DEVICE_TYPE_NO)
                    helper.setVisible(R.id.template_device_more, false).setVisible(R.id.template_device_card_delete, false)//普通灯彩灯第一个为所有组不允许删除
                else
                    helper.setVisible(R.id.template_device_setting, true).setVisible(R.id.template_device_card_delete, isDelete)
        }
    }

    /**
     * 改变状态
     * @param isDelete  是否处于删除状态
     */
    fun changeState(isDelete: Boolean) {
        this.isDelete = isDelete
    }
}


