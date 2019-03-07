package com.dadoutek.uled.intf

import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter

/**
 * Created by hejiajun on 2018/5/2.
 */

interface MyBaseQuickAdapterOnClickListner {
    fun onItemChildClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int,groupPosition: Int)
}
