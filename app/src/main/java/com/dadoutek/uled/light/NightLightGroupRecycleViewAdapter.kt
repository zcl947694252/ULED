package com.dadoutek.uled.light

import android.widget.CheckBox
import android.widget.CompoundButton
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.ItemGroup

class NightLightGroupRecycleViewAdapter(layoutResId: Int, data: List<ItemGroup>?) : BaseQuickAdapter<ItemGroup, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, group: ItemGroup?) {
          helper.setText(R.id.tvGroupName,group?.gpName)
        helper.addOnClickListener(R.id.imgDelete)
    }
}
