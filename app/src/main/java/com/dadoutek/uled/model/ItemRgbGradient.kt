package com.dadoutek.uled.model

import com.dadoutek.uled.model.dbModel.DbColorNode
import org.greenrobot.greendao.annotation.ToMany
import java.io.Serializable

class ItemRgbGradient :Serializable{
    var name: String? = null
    var position: Int = 100
    var speed: Int = 1
    var select: Boolean = false//开停状态
    var id: Int = 0
    var isDiy = false
    var type = 0
    var index = 0
    var belongRegionId: Long? = null
    var isSceneModeSelect:Boolean = false
    var gradientType = 2 //渐变类型 1：自定义渐变  2：内置渐变

    @ToMany(referencedJoinProperty = "belongDynamicChangeId")
     var colorNodes: List<DbColorNode>? = null
    override fun toString(): String {
        return "ItemRgbGradient(name=$name, position=$position, speed=$speed, select=$select, id=$id, isDiy=$isDiy, type=$type, index=$index, belongRegionId=$belongRegionId, isSceneModeSelect=$isSceneModeSelect, gradientType=$gradientType, colorNodes=$colorNodes)"
    }


}