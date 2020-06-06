package com.dadoutek.uled.util

import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.telink.util.MeshUtils
import java.util.concurrent.atomic.AtomicInteger

/**
 * 专门用于生成可用的Mesh地址
 */
class MeshAddressGenerator {
    var meshAddress: AtomicInteger = AtomicInteger(0)
        get() {
            //field代表meshAddress这个变量
            do {//先执行再判断
                field.incrementAndGet()
                if (field.get() == 0xFF)     //为了旧设备的兼容性，要排除0xFF此地址，因为以前的PIR，开关等控制设备的meshAddress都是0xFF
                    field.incrementAndGet()

                TelinkLightApplication.getApp().lastMeshAddress = field.get()
            } while (DBUtils.isDeviceExist(field.get()) || field.get() == 0||field.get() < TelinkLightApplication.getApp().lastMeshAddress)

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
        val i = when {
            addressList.isEmpty() -> MeshUtils.DEVICE_ADDRESS_MIN
            else -> {
                addressList.last()
            }
        }
        meshAddress = AtomicInteger(i)
    }
}