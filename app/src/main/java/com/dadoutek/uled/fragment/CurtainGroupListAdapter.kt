package com.dadoutek.uled.fragment

import android.view.View
import android.widget.Button
import android.widget.CheckBox
import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.tellink.TelinkLightApplication

class CurtainGroupListAdapter(layoutResId: Int, data: List<DbGroup>?, internal var isDelete: Boolean) : BaseItemDraggableAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {
    override fun convert(helper: BaseViewHolder, group: DbGroup?) {

        if (group != null) {
            var gpSet = helper.getView<Button>(R.id.btn_set)
            var num = DBUtils.getCurtainByGroupID(group.id).size

            val deleteIcon = helper.getView<CheckBox>(R.id.selected_group_curtain)
            if (isDelete) {
                deleteIcon.visibility = View.VISIBLE
            } else {
                deleteIcon.visibility = View.GONE
            }

            if(num==0){
                helper.setText(R.id.group_num, TelinkLightApplication.getApp().getString(R.string.total)+0+ TelinkLightApplication.getApp().getString(R.string.piece))
            }else{
                helper.setText(R.id.group_num, TelinkLightApplication.getApp().getString(R.string.total)+num+ TelinkLightApplication.getApp().getString(R.string.piece))
            }
            if (group.textColor == 0)
                group.textColor = mContext.resources
                        .getColor(R.color.black)
//            Log.d("setAddress", group.meshAddr.toString())
            if (group.meshAddr == 0xffff) {
                helper.setText(R.id.template_device_name, TelinkLightApplication.getApp().getString(R.string.allLight))
            } else {
                helper.setText(R.id.template_device_name, group.name)
//                if(OtherUtils.isCurtain(group)){
//                    helper.setText(R.id.btn_set)
//                }
            }

            if (group.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL && num == 0) {
                gpSet.setBackgroundResource(R.drawable.btn_rec_black)
            }else if(group.deviceType == Constant.DEVICE_TYPE_DEFAULT_ALL){
                gpSet.setBackgroundResource(R.drawable.btn_rec_black)
            }
            else {
                gpSet.setBackgroundResource(R.drawable.btn_rec_blue)
            }

            if(group.isSelected){
                helper.setChecked(R.id.selected_group_curtain,true)
            }else{
                helper.setChecked(R.id.selected_group_curtain,false)
            }

//            if (group.deviceType == Constant.DEVICE_TYPE_DEFAULT_ALL) {
//                gpSet.setBackgroundResource(R.drawable.btn_rec_black)
//            }else{
//                gpSet.setBackgroundResource(R.drawable.btn_rec_blue)
//            }
//            if(group.name==TelinkLightApplication.getApp().getString(R.string.curtain)){
//                helper.setVisible(R.id.btn_off,false)
//                helper.setVisible(R.id.btn_on,false)
//            }
            helper.setTextColor(R.id.template_device_name, group.textColor)
//                    .addOnClickListener(R.id.txt_name)
                    .addOnClickListener(R.id.btn_set)
                    .addOnClickListener(R.id.selected_group_curtain)
                    .addOnClickListener(R.id.item_layout)
//                    .addOnClickListener(R.id.add_group)
        }

    }

    fun changeState(isDelete: Boolean) {
        this.isDelete = isDelete
    }
}
