package com.dadoutek.uled.fragment

import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
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
            val gpImageView = helper.getView<ImageView>(R.id.btn_on)
            val gpSet = helper.getView<ImageView>(R.id.btn_set)
            val gpOff = helper.getView<ImageView>(R.id.btn_off)
            val gpOnText = helper.getView<TextView>(R.id.textView8)
            val gpOffText = helper.getView<TextView>(R.id.textView11)
            val deleteIcon = helper.getView<CheckBox>(R.id.selected_group)

            if (isDelete) {
                deleteIcon.visibility = View.VISIBLE
            } else {
                deleteIcon.visibility = View.GONE
            }

            helper.setText(R.id.group_num, TelinkLightApplication.getApp().getString(R.string.total) + num + TelinkLightApplication.getApp().getString(R.string.piece))

            gpSet.setImageResource(R.drawable.icon_setting_group)

            if (group.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL || group.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB || group.deviceType == Constant.DEVICE_TYPE_CONNECTOR) {
                if (group.connectionStatus == ConnectionStatus.ON.value) {
                    gpImageView.setImageResource(R.drawable.icon_open_group)
                    gpOff.setImageResource(R.drawable.icon_down_group)
                    gpOnText.setTextColor(TelinkLightApplication.getApp().getColor(R.color.white))
                    gpOffText.setTextColor(TelinkLightApplication.getApp().getColor(R.color.black_nine))
                } else if (group.connectionStatus == ConnectionStatus.OFF.value) {
                    gpImageView.setImageResource(R.drawable.icon_down_group)
                    gpOff.setImageResource(R.drawable.icon_open_group)
                    gpOnText.setTextColor(TelinkLightApplication.getApp().getColor(R.color.black_nine))
                    gpOffText.setTextColor(TelinkLightApplication.getApp().getColor(R.color.white))
                }
            }


            if (group.textColor == 0)
                group.textColor = mContext.resources
                        .getColor(R.color.black)
//            Log.d("setAddress", group.meshAddr.toString())
            if (group.meshAddr == 0xffff) {
                helper.setText(R.id.txt_name, TelinkLightApplication.getApp().getString(R.string.allLight))
            } else {
                helper.setText(R.id.txt_name, group.name)

            }

            if (group.isSelected) {
                helper.setChecked(R.id.selected_group, true)
            } else {
                helper.setChecked(R.id.selected_group, false)
            }


            helper.setTextColor(R.id.txt_name, group.textColor)
//                    .addOnClickListener(R.id.txt_name)
                    .addOnClickListener(R.id.btn_on)
                    .addOnClickListener(R.id.btn_off)
                    .addOnClickListener(R.id.btn_set)
//                    .addOnClickListener(R.id.group_name)
                    .addOnClickListener(R.id.selected_group)
                    .addOnClickListener(R.id.item_layout)
//                    .addOnClickListener(R.id.add_group)
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