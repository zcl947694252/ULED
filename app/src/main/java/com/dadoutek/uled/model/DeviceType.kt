package com.dadoutek.uled.model

object DeviceType {
    const val OLD_NORMAL_SWITCH: Byte = 0x18
    const val OLD_NORMAL_SWITCH2: Byte = 0x19
    const val NORMAL_SWITCH: Int = 0x20
    const val SCENE_SWITCH: Int = 0x21
    const val NORMAL_SWITCH2: Int = 0x22
    const val SENSOR: Int = 0x23//pir 版本:PS-1.1.3
    const val NIGHT_LIGHT: Int = 0x24       //人体感应器 版本: 2.x.x以上
    const val LIGHT_NORMAL: Int = 0x04
    const val LIGHT_NORMAL_OLD: Int = 0xFF
    const val LIGHT_RGB: Int = 0x06
    const val SMART_CURTAIN: Int = 0x10
    const val SMART_CURTAIN_SWITCH: Int = 0x25
    const val SMART_CONNECTOR: Int = 0x05
    const val USER_CHANNEL = "dadou"
}