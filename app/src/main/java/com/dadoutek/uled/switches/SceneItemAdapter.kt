package com.dadoutek.uled.switches

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbScene


/**
 * 创建者     ZCL
 * 创建时间   2020/1/13 10:47
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class SceneItemAdapter(resId: Int, data: MutableList<DbScene>):BaseQuickAdapter<DbScene,BaseViewHolder>(resId,data){
    override fun convert(helper: BaseViewHolder?, item: DbScene?) {
        helper?.setText(R.id.template_device_batch_title,item?.name)
                ?.setVisible(R.id.template_device_batch_selected,false)
                ?.setVisible(R.id.template_device_batch_title_blow,false)
    }

}