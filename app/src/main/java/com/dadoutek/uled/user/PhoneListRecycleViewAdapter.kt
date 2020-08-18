package com.dadoutek.uled.user

import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.dbModel.DbUser

class PhoneListRecycleViewAdapter(layoutResId: Int,data: List<DbUser>?) : BaseQuickAdapter<DbUser, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, scene: DbUser) {
        if (scene != null) {
            val tvName = helper.getView<TextView>(R.id.phone_text)
            tvName.text = scene.phone

            helper.addOnClickListener(R.id.delete_image)
                    .setTag(R.id.delete_image, helper.adapterPosition)
                    .setTag(R.id.phone_text,helper.adapterPosition)
                    .addOnClickListener(R.id.phone_text)
        }
    }
}
