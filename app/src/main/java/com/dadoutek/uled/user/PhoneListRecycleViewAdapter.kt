package com.dadoutek.uled.user

import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.MyBaseQuickAdapterOnClickListner
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.StringUtils

class PhoneListRecycleViewAdapter(layoutResId: Int,data: List<DbUser>?) : BaseQuickAdapter<DbUser, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, scene: DbUser) {
        if (scene != null) {
            val tvName = helper.getView<TextView>(R.id.phone_text)
//            val tvRgbColor = helper.getView<TextView>(R.id.tv_rgb_color)
            tvName.text = scene.phone

//            val myGrad = tvRgbColor.background as GradientDrawable
//            if (scene.getTextColor() == 0 || scene.getTextColor() == 0xffffff) {
//                tvRgbColor.visibility = View.GONE
//            } else {
//                tvRgbColor.visibility = View.VISIBLE
//                myGrad.setColor(-0x1000000 or scene.getTextColor())
//            }

            helper.addOnClickListener(R.id.delete_image)
                    .setTag(R.id.delete_image, helper.adapterPosition)
                    .setTag(R.id.phone_text,helper.adapterPosition)
                    .addOnClickListener(R.id.phone_text)
        }
    }
}
