package com.dadoutek.uled.device

import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.tellink.TelinkLightApplication

class DeviceTypeRecycleViewAdapter(layoutResId: Int, data: List<String>?) : BaseItemDraggableAdapter<String, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, name: String?) {
                helper.setText(R.id.txt_name, name)
        }
}
