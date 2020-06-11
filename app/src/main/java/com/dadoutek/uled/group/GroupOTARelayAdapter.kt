package com.dadoutek.uled.group

import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbConnector
import com.dadoutek.uled.model.DbModel.DbCurtain


/**
 * 创建者     ZCL
 * 创建时间   2020/6/10 15:17
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GroupOTARelayAdapter(resId: Int, data: MutableList<DbConnector>) : BaseQuickAdapter<DbConnector, BaseViewHolder>(resId, data) {
    override fun convert(helper: BaseViewHolder, item: DbConnector?) {
        val groupName = helper.getView<TextView>(R.id.device_detail_item_group_name)
        val deviceName = helper.getView<TextView>(R.id.device_detail_item_device_name)

        if (item?.version == null)
            groupName.visibility = View.GONE
        else {
            groupName.text = item?.version
            groupName.visibility = View.VISIBLE
        }

        if (item?.name == null || item?.name == "")
            deviceName.visibility = View.GONE
        else {
            deviceName.text = item?.name
            deviceName.visibility = View.VISIBLE
        }
    }
}