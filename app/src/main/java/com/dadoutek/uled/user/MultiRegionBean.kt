package com.dadoutek.uled.region.bean

import com.chad.library.adapter.base.entity.MultiItemEntity


/**
 * 创建者     ZCL
 * 创建时间   2019/8/23 11:17
 * 描述	      ${TODO}
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
class MultiRegionBean :MultiItemEntity {
     var type: Int = 0
      var list: List<Any>? = null

    constructor(type: Int, list: List<Any>) {
        this.list = list
        this.type = type
    }


    override fun getItemType(): Int {
        return  type
    }
}



