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
        helper.setImageResource(R.id.template_device_batch_selected,R.drawable.round_close)
                .setText(R.id.template_device_batch_title,group?.gpName)
                .addOnClickListener(R.id.template_device_batch_selected)
         /* helper.setText(R.id.tvGroupName,group?.gpName)
        helper.addOnClickListener(R.id.imgDelete)*/
    }
}
