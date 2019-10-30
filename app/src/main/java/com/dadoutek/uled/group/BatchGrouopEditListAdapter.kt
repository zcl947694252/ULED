package com.dadoutek.uled.group


/**
 * 创建者     ZCL
 * 创建时间   2019/10/30 10:44
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbGroup
class BatchGrouopEditListAdapter(layoutResId: Int, data: List<DbGroup>) : BaseQuickAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: DbGroup) {
        helper.setText(R.id.batch_four_group_name,item.name)
                .setText(R.id.batch_four_group_num,mContext.getString(R.string.number)+":${item.deviceCount}")
        if (item.isChecked) {
            helper.setImageResource(R.id.batch_four_select,R.drawable.icon_checkbox_selected)
        } else {
            if(item.enableCheck){
                helper.setImageResource(R.id.batch_four_select,R.drawable.icon_checkbox_unselected)
            }else{
                helper.setImageResource(R.id.batch_four_select,R.drawable.icon_checkbox_unselected)
            }
        }
    }
}
