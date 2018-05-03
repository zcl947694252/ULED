package com.dadoutek.uled.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Group

class SceneGroupAdapter(layoutResId: Int, data: MutableList<Group>?) : BaseQuickAdapter<Group, BaseViewHolder>(layoutResId, data) {
    override fun convert(helper: BaseViewHolder?, item: Group?) {
        helper?.setText(R.id.txt_name, item?.name)
//        helper?.addOnClickListener(mLayoutResId)
        if(item?.checked!!){
            helper?.setVisible(R.id.right_bt,true)
        }else{
            helper?.setVisible(R.id.right_bt,false)
        }
    }

}