package com.dadoutek.uled.rgb

import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.dbModel.DbDiyGradient

class RGBDiyGradientAdapter(layoutResId: Int, data: List<DbDiyGradient>?,internal var isDelete: Boolean) : BaseQuickAdapter<DbDiyGradient, BaseViewHolder>(layoutResId, data) {


    override fun convert(helper: BaseViewHolder, item: DbDiyGradient) {
        var gpOn = helper.getView<ImageView>(R.id.diy_mode_on)
        var gpOff = helper.getView<ImageView>(R.id.diy_mode_off)
      /*  var gpOnText = helper.getView<TextView>(R.id.diy_mode_on_text)
        var gpOffText = helper.getView<TextView>(R.id.diy_mode_off_text)*/

        val deleteIcon = helper.getView<CheckBox>(R.id.diy_selected)
        if (isDelete) {
            deleteIcon.visibility = View.VISIBLE
        } else {
            deleteIcon.visibility = View.GONE
        }

        if (item.isSelected) {
            helper.setChecked(R.id.diy_selected, true)
        } else {
            helper.setChecked(R.id.diy_selected, false)
        }

        if (item.select) {
            gpOn.setImageResource(R.drawable.icon_open_blue)
            gpOff.setImageResource(R.drawable.icon_stop2_back)
            //gpOnText.setTextColor(TelinkLightApplication.getApp().getColor(R.color.white))
            // gpOffText.setTextColor(TelinkLightApplication.getApp().getColor(R.color.black_nine))
        } else {
            gpOn.setImageResource(R.drawable.icon_open2_back)
            gpOff.setImageResource(R.drawable.icon_stop_blue)
            //gpOnText.setTextColor(TelinkLightApplication.getApp().getColor(R.color.black_nine))
            // gpOffText.setTextColor(TelinkLightApplication.getApp().getColor(R.color.white))
        }


        helper.setText(R.id.modeName, item.name)
                .addOnClickListener(R.id.diy_mode_on)
                .addOnClickListener(R.id.diy_mode_off)
                .addOnClickListener(R.id.diy_mode_set)
                .addOnClickListener(R.id.diy_selected)
    }

    fun changeState(isDelete: Boolean) {
        this.isDelete = isDelete
    }
}
