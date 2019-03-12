package com.dadoutek.uled.light

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbLight

class DeviceDetailListAdapter(layoutResId: Int, data: List<DbLight>?) : BaseQuickAdapter<DbLight, BaseViewHolder>(layoutResId, data) {

        override fun convert(helper: BaseViewHolder, scene: DbLight) {
            if (scene != null) {
                helper.setText(R.id.name, scene.name)
                helper.setImageResource(R.id.img_icon,R.drawable.icon_light_on)
            }
        }
}