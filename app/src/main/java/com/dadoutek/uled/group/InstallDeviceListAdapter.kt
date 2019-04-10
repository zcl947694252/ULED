package com.dadoutek.uled.group

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.InstallDeviceModel

/**
 * Created by hejiajun on 2018/5/10.
 */

class InstallDeviceListAdapter(layoutResId: Int, data: List<InstallDeviceModel>?) : BaseQuickAdapter<InstallDeviceModel, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: InstallDeviceModel) {
       helper.setText(R.id.device_name,item.deviceType)
               .setText(R.id.device_describe,item.deviceDescribeTion)
    }
}
