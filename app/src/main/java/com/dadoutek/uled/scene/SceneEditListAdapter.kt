package com.dadoutek.uled.scene

import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbGroup

/**
 * Created by hejiajun on 2018/5/5.
 */

class SceneEditListAdapter(layoutResId: Int, data: List<DbGroup>) : BaseQuickAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: DbGroup) {
        helper.setText(R.id.template_device_batch_title, item.name)
        helper.setVisible(R.id.template_device_batch_title_blow, false)
        if (item.isChecked) {
            helper.setImageResource(R.id.template_device_batch_selected, R.drawable.icon_checkbox_selected)
        } else {
            if (item.isCheckedInGroup) {
                helper.setImageResource(R.id.template_device_batch_selected, R.drawable.icon_checkbox_unselected)
            } else {
                helper.setImageResource(R.id.template_device_batch_selected, R.drawable.icon_checkbox_unselected)
            }
        }
    }
}
