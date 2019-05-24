package com.dadoutek.uled.rgb

import android.graphics.drawable.GradientDrawable
import android.widget.TextView

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.ItemColorPreset
import com.dadoutek.uled.util.Dot

/**
 * Created by hejiajun on 2018/3/28.
 */

class ColorSceneSelectDiyRecyclerViewAdapter(layoutResId: Int, data: List<ItemColorPreset>?) : BaseQuickAdapter<ItemColorPreset, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: ItemColorPreset) {
//        helper.addOnClickListener(R.id.btn_diy_preset)
//                .addOnLongClickListener(R.id.btn_diy_preset)
//                .setText(R.id.diy_preset, "")
//                .setBackgroundColor(R.id.btn_diy_preset, -0x1000000 or item.color)


        var doc = helper.getView<Dot>(R.id.btn_diy_preset)

//        doc.setBackgroundColor(item.color)

        doc.setChecked(true,0xff000000.toInt() or item.color)

        helper.addOnClickListener(R.id.btn_diy_preset)
                .addOnLongClickListener(R.id.btn_diy_preset)
                .setText(R.id.diy_preset, if (item.brightness == -1) "" else item.brightness.toString() + "%")
    }
}
