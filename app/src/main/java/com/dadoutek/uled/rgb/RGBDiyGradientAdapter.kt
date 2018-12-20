package com.dadoutek.uled.rgb

import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbColorNode
import com.dadoutek.uled.model.DbModel.DbDiyGradient

class RGBDiyGradientAdapter(layoutResId: Int, data: List<DbDiyGradient>?) : BaseQuickAdapter<DbDiyGradient, BaseViewHolder>(layoutResId, data) {

    private val TYPE_GRADIENT = 0
    private val TYPE_JUMP = 1
    private val TYPE_STROBE = 2
    private var rgbDiyColorListAdapter:RGBDiyColorListAdapter?=null

    override fun convert(helper: BaseViewHolder, item: DbDiyGradient) {
        val colorListRecyclerView=helper.getView<RecyclerView>(R.id.diy_show_color_recyclerView)

        helper.setText(R.id.modeName, item.name)
                .setText(R.id.modeSpeed, item.speed)
                .setText(R.id.modeType, getTypeString(item.type))

        val layoutmanager = GridLayoutManager(mContext,4)
        colorListRecyclerView.layoutManager = layoutmanager as RecyclerView.LayoutManager?
        rgbDiyColorListAdapter = RGBDiyColorListAdapter(
                R.layout.item_color, item.colorNodeList)
        rgbDiyColorListAdapter?.bindToRecyclerView(colorListRecyclerView)
    }

    private fun getTypeString(type: Int): String {
        when (type) {
            TYPE_GRADIENT -> return mContext.getString(R.string.str_gradient)
            TYPE_JUMP -> return mContext.getString(R.string.str_jump)
            TYPE_STROBE -> return mContext.getString(R.string.str_strobe)
        }
        return ""
    }
}
