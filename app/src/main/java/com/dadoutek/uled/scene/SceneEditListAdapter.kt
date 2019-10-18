package com.dadoutek.uled.scene

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbGroup

/**
 * Created by hejiajun on 2018/5/5.
 */

class SceneEditListAdapter(layoutResId: Int, data: List<DbGroup>) : BaseQuickAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: DbGroup) {
         helper.setText(R.id.group_name,item.name)
        if (item.isChecked) {
            helper.setImageResource(R.id.scene_delete,R.drawable.icon_checkbox_selected)
        } else {
            if(item.enableCheck){
                helper.setImageResource(R.id.scene_delete,R.drawable.icon_checkbox_unselected)
            }else{
                helper.setImageResource(R.id.scene_delete,R.drawable.icon_checkbox_unselected)
            }
        }
    }
}
