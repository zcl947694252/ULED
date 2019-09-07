package com.dadoutek.uled.util

import com.dadoutek.uled.model.DbModel.DBUtils
import com.telink.util.MeshUtils

/**
 * 专门用于生成可用的Mesh地址
 */
class MeshAddressGenerator() {
    var meshAddress: Int = 0
        get() {
            return (field++)
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
        when {
            addressList.isEmpty() -> meshAddress = MeshUtils.DEVICE_ADDRESS_MIN
            else -> {
//                LogUtils.d("generateMeshAddr addressList.last() = ${addressList.last()}")
                meshAddress = addressList.last()
            }
        }


    }

}