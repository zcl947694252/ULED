package com.dadoutek.uled.switches

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.DbModel.DbSwitch

class SwitchDeviceDetailsAdapter(layoutResId: Int, data: List<DbSwitch>?) : BaseQuickAdapter<DbSwitch, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, scene: DbSwitch) {
        if (scene != null) {
            helper.setText(R.id.name, scene.name)
            helper.setImageResource(R.id.img_light, R.drawable.icon_light_on)
        }
    }
}