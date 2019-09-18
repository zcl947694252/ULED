package com.dadoutek.uled.util

import com.dadoutek.uled.model.DbModel.DBUtils
import com.telink.util.MeshUtils

/**
 * 专门用于生成可用的Mesh地址
 */
class MeshAddressGenerator {
    var meshAddress: Int = 0
        get() {
            //field代表meshAddress这个变量
            while (DBUtils.isDeviceExist(field)) {    //如果新的meshAddress已经存在，就继续+1
                ++field
                if (field == 0xFF)              //为了旧设备的兼容性，要排除0xFF此地址，因为以前的PIR，开关等控制设备的meshAddress都是0xFF
                    ++field
            }
            return field

        }

    init {
        val lights = DBUtils.allLight.map { it.meshAddr }
        val curtain = DBUtils.allCurtain.map { it.meshAddr }
        val relay = DBUtils.allRely.map { it.meshAddr }
        val addressList = mutableListOf<Int>()
        addressList.addAll(lights)
        addressList.addAll(curtain)
        addressList.addAll(relay)
        addressList.sortBy { it }
        meshAddress = when {
            addressList.isEmpty() -> MeshUtils.DEVICE_ADDRESS_MIN
            else -> {
                addressList.last()
            }
        }


    }

}