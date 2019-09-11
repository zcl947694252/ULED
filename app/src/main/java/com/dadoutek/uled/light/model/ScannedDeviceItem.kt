package com.dadoutek.uled.light.model

import com.telink.bluetooth.light.DeviceInfo

data class ScannedDeviceItem(val deviceInfo: DeviceInfo,  var name: String): DeviceInfo() {


    var isSelected: Boolean =false
    var hasGroup: Boolean = false
    var belongGroupId: Long = 0
}