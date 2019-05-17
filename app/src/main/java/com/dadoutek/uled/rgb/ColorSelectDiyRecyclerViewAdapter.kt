package com.dadoutek.uled.rgb

import android.widget.TextView

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.ItemColorPreset
import com.dadoutek.uled.util.Dot

/**
 * Created by hejiajun on 2018/3/28.
 */

class ColorSelectDiyRecyclerViewAdapter(layoutResId: Int, data: List<ItemColorPreset>?) : BaseQuickAdapter<ItemColorPreset, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: ItemColorPreset) {

        var doc = helper.getView<Dot>(R.id.btn_diy_preset)

//        doc.setBackgroundColor(item.color)

        doc.setChecked(true,item.color)

        helper.addOnClickListener(R.id.btn_diy_preset)
                .addOnLongClickListener(R.id.btn_diy_preset)
                //                .setText(R.id.btn_diy_preset, (item.getBrightness()==-1?"":item.getBrightness()+ "%"))
//                .setBackgroundColor(R.id.btn_diy_preset, -0x1000000 or item.color)
    }
}
