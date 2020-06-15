package com.dadoutek.uled.switches

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbScene


/**
 * 创建者     ZCL
 * 创建时间   2020/1/2 17:22
 * 描述 场景列表
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 *
 */
class  SceneListAdapter(resId: Int, data: List<DbScene>): BaseQuickAdapter<DbScene, BaseViewHolder>(resId,data){
    override fun convert(helper: BaseViewHolder?, item: DbScene?) {
      helper?.setText(R.id.template_device_name_n,item?.name)
              ?.setImageResource(R.id.template_device_icon_n,R.drawable.bg_on)
              ?.setVisible(R.id.template_group_name_n,false)
              ?.setVisible(R.id.template_select_n,false)
    }

}