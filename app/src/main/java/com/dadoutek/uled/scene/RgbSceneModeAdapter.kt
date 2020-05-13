package com.dadoutek.uled.scene

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.ItemRgbGradient


/**
 * 创建者     ZCL
 * 创建时间   2020/5/9 15:58
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class RgbSceneModeAdapter(sceneMode: Int, buildInModeList: ArrayList<ItemRgbGradient>) : BaseQuickAdapter<ItemRgbGradient, BaseViewHolder>(sceneMode, buildInModeList) {
    override fun convert(helper: BaseViewHolder?, item: ItemRgbGradient?) {
        if (item?.isSceneModeSelect == true)
            helper?.setTextColor(R.id.scene_mode_tv, mContext.getColor(R.color.blue_text))
        else
            helper?.setTextColor(R.id.scene_mode_tv, mContext.getColor(R.color.gray9))

        helper?.setText(R.id.scene_mode_tv, item?.name)

    }

}