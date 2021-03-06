package com.dadoutek.uled.group


/**
 * 创建者     ZCL
 * 创建时间   2019/10/30 10:44
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.gateway.bean.DbRouter

class BatchRouterAdapter(layoutResId: Int, data: RxDeviceList<DbRouter>) : BaseQuickAdapter<DbRouter, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: DbRouter) {
        val tv = helper.getView<TextView>(R.id.template_device_batch_title_blow)
        tv.visibility = View.GONE
        helper.setText(R.id.template_device_batch_title, item.name)
                .setText(R.id.template_device_batch_title_blow, mContext.getString(R.string.number))
                .setImageResource(R.id.template_device_batch_icon,R.drawable.icon_wifi_small)

        when {
            item.isSelect -> helper.setImageResource(R.id.template_device_batch_selected, R.drawable.icon_checkbox_selected)
            else -> helper.setImageResource(R.id.template_device_batch_selected, R.drawable.icon_checkbox_unselected)
        }
    }
}
