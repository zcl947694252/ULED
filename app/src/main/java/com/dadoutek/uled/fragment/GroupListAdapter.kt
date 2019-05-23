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

class GroupListAdapter(layoutResId: Int, data: List<DbGroup>?, internal var isDelete: Boolean) : BaseItemDraggableAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {
    override fun convert(helper: BaseViewHolder, group: DbGroup?) {

        if (group != null) {
            var gpImageView = helper.getView<ImageView>(R.id.btn_on)
            var gpSet = helper.getView<ImageView>(R.id.btn_set)
            var num = DBUtils.getLightByGroupID(group.id).size

            val deleteIcon = helper.getView<CheckBox>(R.id.selected_group)
            if (isDelete) {
                deleteIcon.visibility = View.VISIBLE
            } else {
                deleteIcon.visibility = View.GONE
            }

            if (num == 0) {
                helper.setText(R.id.group_num, TelinkLightApplication.getInstance().getString(R.string.total) + 0 + TelinkLightApplication.getInstance().getString(R.string.piece))
            } else {
                helper.setText(R.id.group_num, TelinkLightApplication.getInstance().getString(R.string.total) + num + TelinkLightApplication.getInstance().getString(R.string.piece))
            }

            if (group.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL && num == 0 || group.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB && num == 0 || group.deviceType == Constant.DEVICE_TYPE_CONNECTOR && num == 0) {
                gpImageView.setImageResource(R.drawable.icon_open_group_no)
                gpSet.setImageResource(R.drawable.icon_device_group)
            } else if (group.deviceType == Constant.DEVICE_TYPE_DEFAULT_ALL) {
                gpImageView.setImageResource(R.drawable.icon_open_group_no)
                gpSet.setImageResource(R.drawable.icon_device_group)
            } else {
                gpImageView.setImageResource(R.drawable.icon_open_group)
                gpSet.setImageResource(R.drawable.icon_setting_group)
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