package com.dadoutek.uled.light

import android.widget.ImageView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.ItemGroup
import com.dadoutek.uled.util.DensityUtil

class NightLightGroupRecycleViewAdapter(layoutResId: Int, data: List<ItemGroup>?) : BaseQuickAdapter<ItemGroup, BaseViewHolder>(layoutResId, data) {
    private var isScene = false
    override fun convert(helper: BaseViewHolder, group: ItemGroup?) {
        helper.setImageResource(R.id.template_device_batch_selected, R.drawable.round_close)
                .setText(R.id.template_device_batch_title, group?.gpName)
                .setImageResource(R.id.template_device_batch_icon, group?.icon ?: 0)
                .addOnClickListener(R.id.template_device_batch_selected)
                .setVisible(R.id.template_device_batch_title_blow, false)
        if (isScene){
            val ly = helper.getView<ImageView>(R.id.template_device_batch_icon)
            ly.layoutParams.width = DensityUtil.dip2px(mContext,40f)
            ly.layoutParams.height = DensityUtil.dip2px(mContext,40f)
            helper.setBackgroundRes(R.id.template_device_batch_icon_ly, R.drawable.rect_r15_graye)
        }
        /* helper.setText(R.id.tvGroupName,group?.gpName)
       helper.addOnClickListener(R.id.imgDelete)*/
    }

    fun isScene(isScene: Boolean) {
        this.isScene = isScene
    }
}
