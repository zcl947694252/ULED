package com.dadoutek.uled.rgb

import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.CheckBox
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbColorNode
import com.dadoutek.uled.model.DbModel.DbDiyGradient

class RGBDiyGradientAdapter(layoutResId: Int, data: List<DbDiyGradient>?,internal var isDelete: Boolean) : BaseQuickAdapter<DbDiyGradient, BaseViewHolder>(layoutResId, data) {

    private val TYPE_GRADIENT = 0
    private val TYPE_JUMP = 1
    private val TYPE_STROBE = 2
    private var rgbDiyColorListAdapter: RGBDiyColorListAdapter? = null

    override fun convert(helper: BaseViewHolder, item: DbDiyGradient) {
//        val colorListRecyclerView=helper.getView<RecyclerView>(R.id.diy_show_color_recyclerView)


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
