package com.dadoutek.uled.region

import java.io.Serializable

/**
 * 创建者     ZCL
 * 创建时间   2019/8/1 9:59
 * 描述	      ${TODO}
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
class SettingItemBean() : Serializable {
    var icon: Int = 0
    lateinit var title: String

    constructor(icon_local_data: Int, title: String) : this() {
        this.icon = icon_local_data
        this.title = title
    }
}





