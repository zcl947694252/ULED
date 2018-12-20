package com.dadoutek.uled.rgb

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbColorNode

class RGBDiyColorListAdapter(layoutResId: Int, data: List<DbColorNode>?) : BaseQuickAdapter<DbColorNode, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder?, item: DbColorNode?) {
        helper?.setBackgroundColor(R.id.colorRec,getColor(item!!.rgbw))
    }

    fun getColor(rgbw: Int) : Int{
        if(rgbw==-1){
            return 0x00
        }
        return 0xff000000.toInt() or rgbw
    }
}
