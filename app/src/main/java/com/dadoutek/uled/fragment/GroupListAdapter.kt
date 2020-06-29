package com.dadoutek.uled.fragment

import android.widget.ImageView
import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.DensityUtil
import com.telink.bluetooth.light.ConnectionStatus

/**
 * 群组页面内的设备的List的Adapter
 * @param isDelete      是否处于删除模式
 */
class GroupListAdapter(layoutResId: Int, data: List<DbGroup>, internal var isDelete: Boolean) : BaseItemDraggableAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {
    override fun convert(helper: BaseViewHolder, group: DbGroup?) {
        if (group != null) {
            val num = group.deviceCount //内含设备的数量
            val iv = helper.getView<ImageView>(R.id.template_device_icon)
            iv.layoutParams.height = DensityUtil.dip2px(mContext, 60f)
            iv.layoutParams.width = DensityUtil.dip2px(mContext, 60f)

            helper.setImageResource(R.id.template_device_icon, R.drawable.icon_group_n)
                    .setImageResource(R.id.template_device_setting, R.drawable.icon_setting_n)
                    .addOnClickListener(R.id.template_device_icon)
                    .addOnClickListener(R.id.template_device_more)
                    .addOnClickListener(R.id.template_device_setting)
                    .addOnClickListener(R.id.template_device_card_delete)

            val isSuportOpenOrClose = (group.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL || group.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB
                    || group.deviceType == Constant.DEVICE_TYPE_CONNECTOR || group.deviceType == Constant.DEVICE_TYPE_DEFAULT_ALL)

            if (isSuportOpenOrClose) {//支持点击 使用设置开关三个图标 将其显示 不支持设置按钮图标使其隐藏
                helper.setImageResource(R.id.template_device_setting, R.drawable.icon_setting_n)//设置
                        .setImageResource(R.id.template_device_more, R.drawable.icon_more)//进入
                when {
                    num > 0 -> {
                        when (group.connectionStatus) {
                            ConnectionStatus.ON.value -> helper.setImageResource(R.id.template_device_icon, R.drawable.icon_group_n)
                            ConnectionStatus.OFF.value -> helper.setImageResource(R.id.template_device_icon, R.drawable.icon_group_n)
                        }
                    }
                    num <= 0 -> {
                    }//group.deviceType == Constant.DEVICE_TYPE_CURTAIN  窗帘不使用原本三个图标 使用新的按钮
                }
                helper.addOnClickListener(R.id.template_device_setting).addOnClickListener(R.id.template_device_more)
            } else
                helper.addOnClickListener(R.id.template_device_icon)


            if (group.textColor == 0)
                group.textColor = mContext.resources.getColor(R.color.black)

            if (group.meshAddr == 0xffff)
                helper.setText(R.id.template_device_group_name, TelinkLightApplication.getApp().getString(R.string.allLight))
            else
                helper.setText(R.id.template_device_group_name, group.name)
            if (helper.adapterPosition != 0)
                helper.setVisible(R.id.template_device_card_delete, isDelete)
                        .setVisible(R.id.template_device_setting, true)
            else
                if (group.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL || group.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB||group.deviceType ==Constant.DEVICE_TYPE_NO)
                    helper.setVisible(R.id.template_device_setting, false).setVisible(R.id.template_device_card_delete, false)//普通灯彩灯第一个为所有组不允许删除
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


