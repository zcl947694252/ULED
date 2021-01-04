package com.dadoutek.uled.rgb

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.dbModel.DbColorNode
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.Dot

class RGBDiyColorCheckAdapter(layoutResId: Int, data: List<DbColorNode>?) : BaseQuickAdapter<DbColorNode, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder?, item: DbColorNode?) {
//        helper?.setBackgroundColor(R.id.colorRec,getColor(item!!.rgbw))
        var doc = helper!!.getView<Dot>(R.id.btn_diy_preset)

//        doc.setBackgroundColor(item.color)

        doc.setChecked(true,getColor(item!!.rgbw))


        if(item!!.rgbw!=-1){
            helper.setVisible(R.id.colorRec,false)
            helper?.setText(R.id.colorRec,item?.brightness.toString()+"%")
        }else{
            helper.setVisible(R.id.colorRec,true)
            helper?.setText(R.id.colorRec,TelinkLightApplication.getApp().getString(R.string.no_color))
        }
    }

    fun getColor(rgbw: Int) : Int{
        if(rgbw==-1){
            return 0xffc8c8c8.toInt()
        }
        return 0xff000000.toInt() or rgbw
    }
}
