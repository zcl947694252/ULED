package com.dadoutek.uled.rgb

import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbDiyGradient
import com.dadoutek.uled.tellink.TelinkLightApplication

class RGBDiyGradientAdapter(layoutResId: Int, data: List<DbDiyGradient>?,internal var isDelete: Boolean) : BaseQuickAdapter<DbDiyGradient, BaseViewHolder>(layoutResId, data) {

    private val TYPE_GRADIENT = 0
    private val TYPE_JUMP = 1
    private val TYPE_STROBE = 2
    private var rgbDiyColorListAdapter: RGBDiyColorListAdapter? = null

    override fun convert(helper: BaseViewHolder, item: DbDiyGradient) {
//        val colorListRecyclerView=helper.getView<RecyclerView>(R.id.diy_show_color_recyclerView)

        var gpOn = helper.getView<ImageView>(R.id.diy_mode_on)
        var gpOff = helper.getView<ImageView>(R.id.diy_mode_off)
        var gpOnText = helper.getView<TextView>(R.id.diy_mode_on_text)
        var gpOffText = helper.getView<TextView>(R.id.diy_mode_off_text)

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


        if(item.select){
            gpOn.setImageResource(R.drawable.icon_open_group)
            gpOff.setImageResource(R.drawable.icon_down_group)
            gpOnText.setTextColor(TelinkLightApplication.getApp().getColor(R.color.white))
            gpOffText.setTextColor(TelinkLightApplication.getApp().getColor(R.color.black_nine))
        }else{
            gpOn.setImageResource(R.drawable.icon_down_group)
            gpOff.setImageResource(R.drawable.icon_open_group)
            gpOnText.setTextColor(TelinkLightApplication.getApp().getColor(R.color.black_nine))
            gpOffText.setTextColor(TelinkLightApplication.getApp().getColor(R.color.white))
        }


        helper.setText(R.id.modeName, item.name)
                .addOnClickListener(R.id.diy_mode_on)
                .addOnClickListener(R.id.diy_mode_off)
                .addOnClickListener(R.id.diy_mode_set)
                .addOnClickListener(R.id.diy_selected)
//                .setText(R.id.modeSpeed, mContext.getString(R.string.tv_speed,item.speed.toString()))
//                .setText(R.id.modeType, getTypeString(item.type))
//                .addOnClickListener(R.id.more)
//        val layoutmanager = GridLayoutManager(mContext,4)
//        colorListRecyclerView.layoutManager = layoutmanager as RecyclerView.LayoutManager?
//        rgbDiyColorListAdapter = RGBDiyColorListAdapter(
//                R.layout.item_color, item.colorNodes)
//        rgbDiyColorListAdapter?.bindToRecyclerView(colorListRecyclerView)
    }

//    private fun getTypeString(type: Int): String {
//        when (type) {
//            TYPE_GRADIENT -> return mContext.getString(R.string.str_gradient)
//            TYPE_JUMP -> return mContext.getString(R.string.str_jump)
//            TYPE_STROBE -> return mContext.getString(R.string.str_strobe)
//        }
//        return ""
//    }

    fun changeState(isDelete: Boolean) {
        this.isDelete = isDelete
    }
}
