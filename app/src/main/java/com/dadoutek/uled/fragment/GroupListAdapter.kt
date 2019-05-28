package com.dadoutek.uled.fragment

import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.StringUtils
import kotlin.math.log

class GroupListAdapter(layoutResId: Int, data: List<DbGroup>?, internal var isDelete: Boolean) : BaseItemDraggableAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {
    override fun convert(helper: BaseViewHolder, group: DbGroup?) {

        if (group != null) {
            var lightNum = 0
            var relayNum = 0
            var curtianNum = 0
            var gpImageView = helper.getView<ImageView>(R.id.btn_on)
            var gpSet = helper.getView<ImageView>(R.id.btn_set)
            var gpOff = helper.getView<ImageView>(R.id.btn_off)
            var gpOnText = helper.getView<TextView>(R.id.textView8)
            var gpOffText = helper.getView<TextView>(R.id.textView11)
            if (group.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL || group.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB) {
                lightNum = DBUtils.getLightByGroupID(group.id).size
            }

            if (group.deviceType == Constant.DEVICE_TYPE_CURTAIN) {
                curtianNum = DBUtils.getConnectorByGroupID(group.id).size
            }

            if (group.deviceType == Constant.DEVICE_TYPE_CONNECTOR) {
                relayNum = DBUtils.getConnectorByGroupID(group.id).size
            }

            val deleteIcon = helper.getView<CheckBox>(R.id.selected_group)
            if (isDelete) {
                deleteIcon.visibility = View.VISIBLE
            } else {
                deleteIcon.visibility = View.GONE
            }

            if (group.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL || group.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB) {
                if (lightNum == 0) {
                    helper.setText(R.id.group_num, TelinkLightApplication.getInstance().getString(R.string.total) + 0 + TelinkLightApplication.getInstance().getString(R.string.piece))
                } else {
                    helper.setText(R.id.group_num, TelinkLightApplication.getInstance().getString(R.string.total) + lightNum + TelinkLightApplication.getInstance().getString(R.string.piece))
                }
            } else if (group.deviceType == Constant.DEVICE_TYPE_CONNECTOR) {
                if (relayNum == 0) {
                    helper.setText(R.id.group_num, TelinkLightApplication.getInstance().getString(R.string.total) + 0 + TelinkLightApplication.getInstance().getString(R.string.piece))
                } else {
                    helper.setText(R.id.group_num, TelinkLightApplication.getInstance().getString(R.string.total) + relayNum + TelinkLightApplication.getInstance().getString(R.string.piece))
                }
            } else if (group.deviceType == Constant.DEVICE_TYPE_CURTAIN) {
                if (curtianNum == 0) {
                    helper.setText(R.id.group_num, TelinkLightApplication.getInstance().getString(R.string.total) + 0 + TelinkLightApplication.getInstance().getString(R.string.piece))
                } else {
                    helper.setText(R.id.group_num, TelinkLightApplication.getInstance().getString(R.string.total) + curtianNum + TelinkLightApplication.getInstance().getString(R.string.piece))
                }
            }


            if (group.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL && lightNum == 0 || group.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB && lightNum == 0 || group.deviceType == Constant.DEVICE_TYPE_CONNECTOR && relayNum == 0 || group.deviceType == Constant.DEVICE_TYPE_CURTAIN && curtianNum == 0) {
                gpImageView.setImageResource(R.drawable.icon_open_group_no)
                gpSet.setImageResource(R.drawable.icon_device_group)
                gpOff.setImageResource(R.drawable.icon_down_group)
            } else if (group.deviceType == Constant.DEVICE_TYPE_DEFAULT_ALL) {
                gpImageView.setImageResource(R.drawable.icon_open_group_no)
                gpSet.setImageResource(R.drawable.icon_device_group)
                gpOff.setImageResource(R.drawable.icon_down_group)
            } else {
                    gpImageView.setImageResource(R.drawable.icon_open_group)
                    gpSet.setImageResource(R.drawable.icon_setting_group)
                    gpOff.setImageResource(R.drawable.icon_down_group)

            }

            if (group.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL || group.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB ) {
                if (group.status == 1) {
                    gpImageView.setImageResource(R.drawable.icon_open_group)
                    gpOff.setImageResource(R.drawable.icon_down_group)
                    gpOnText.setTextColor(TelinkLightApplication.getInstance().getColor(R.color.white))
                    gpOffText.setTextColor(TelinkLightApplication.getInstance().getColor(R.color.black_nine))
                }else if(group.status==2){
                    gpImageView.setImageResource(R.drawable.icon_down_group)
                    gpOff.setImageResource(R.drawable.icon_open_group)
                    gpOnText.setTextColor(TelinkLightApplication.getInstance().getColor(R.color.black_nine))
                    gpOffText.setTextColor(TelinkLightApplication.getInstance().getColor(R.color.white))
                }
            }


            if (group.textColor == 0)
                group.textColor = mContext.resources
                        .getColor(R.color.black)
            Log.d("setAddress", group.meshAddr.toString())
            if (group.meshAddr == 0xffff) {
                helper.setText(R.id.txt_name, TelinkLightApplication.getInstance().getString(R.string.allLight))
            } else {
                helper.setText(R.id.txt_name, group.name)
//                if(OtherUtils.isCurtain(group)){
//                    helper.setText(R.id.btn_set)
//                }
            }

            if (group.isSelected) {
                helper.setChecked(R.id.selected_group, true)
            } else {
                helper.setChecked(R.id.selected_group, false)
            }

//            if (group.deviceType == Constant.DEVICE_TYPE_DEFAULT_ALL) {
//                gpImageView.setImageResource(R.drawable.icon_open_group_no)
//                gpSet.setImageResource(R.drawable.icon_device_group)
//            } else {
//                gpImageView.setImageResource(R.drawable.icon_open_group)
//                gpSet.setImageResource(R.drawable.icon_setting_group)
//            }
//            if(group.name==TelinkLightApplication.getInstance().getString(R.string.curtain)){
//                helper.setVisible(R.id.btn_off,false)
//                helper.setVisible(R.id.btn_on,false)
//            }
            helper.setTextColor(R.id.txt_name, group.textColor)
                    .addOnClickListener(R.id.txt_name)
                    .addOnClickListener(R.id.btn_on)
                    .addOnClickListener(R.id.btn_off)
                    .addOnClickListener(R.id.btn_set)
                    .addOnClickListener(R.id.group_name)
                    .addOnClickListener(R.id.selected_group)
                    .addOnClickListener(R.id.item_layout)
//                    .addOnClickListener(R.id.add_group)
        }

    }

    fun changeState(isDelete: Boolean) {
        this.isDelete = isDelete
    }
}