package com.dadoutek.uled.windowcurtains

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbCurtain

class CurtainDeviceDetailsAdapter(layoutResId: Int, data: List<DbCurtain>?) : BaseQuickAdapter<DbCurtain, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, scene: DbCurtain) {
        if (scene != null) {
            helper.setText(R.id.name, scene.name)
            helper.setImageResource(R.id.img_light, R.drawable.icon_light_on)
        }
    }
}