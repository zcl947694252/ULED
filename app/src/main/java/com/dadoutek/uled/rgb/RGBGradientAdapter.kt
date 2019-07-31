package com.dadoutek.uled.rgb

import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.ItemRgbGradient
import com.dadoutek.uled.tellink.TelinkLightApplication

class RGBGradientAdapter(layoutResId: Int, data: List<ItemRgbGradient>?) : BaseItemDraggableAdapter<ItemRgbGradient, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: ItemRgbGradient) {


        var gpOn = helper.getView<ImageView>(R.id.gradient_mode_on)
        var gpOff = helper.getView<ImageView>(R.id.gradient_mode_off)
        var gpOnText = helper.getView<TextView>(R.id.textOn)
        var gpOffText = helper.getView<TextView>(R.id.textStop)


        if (item.select) {
            gpOn.setImageResource(R.drawable.icon_open_group)
            gpOff.setImageResource(R.drawable.icon_down_group)
            gpOnText.setTextColor(TelinkLightApplication.getInstance().getColor(R.color.white))
            gpOffText.setTextColor(TelinkLightApplication.getInstance().getColor(R.color.black_nine))
        } else {
            gpOn.setImageResource(R.drawable.icon_down_group)
            gpOff.setImageResource(R.drawable.icon_open_group)
            gpOnText.setTextColor(TelinkLightApplication.getInstance().getColor(R.color.black_nine))
            gpOffText.setTextColor(TelinkLightApplication.getInstance().getColor(R.color.white))
        }

        helper.setText(R.id.modeName, item.name)
                .addOnClickListener(R.id.modeName)
                .addOnClickListener(R.id.gradient_mode_on)
                .addOnClickListener(R.id.gradient_mode_off)
                .addOnClickListener(R.id.gradient_mode_set)
    }

}
