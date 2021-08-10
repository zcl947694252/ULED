package com.dadoutek.uled.model

object DeviceType {
    const val NORMAL_SWITCH: Int = 0x20 // 普通开关 触摸的调光调色开关
    const val SCENE_SWITCH: Int = 0x21 // 场景开关
    const val NORMAL_SWITCH2: Int = 0x22 // 普通开关 面板的调光调色开关
    const val DOUBLE_SWITCH: Int = 0x27 // 单调光双组开关
    const val EIGHT_SWITCH: Int = 0x28 // 8键开关
    const val EIGHT_SWITCH_VERSION: String = "B8SS"
    const val SMART_CURTAIN_SWITCH: Int = 0x25 // 窗帘开关
    const val FOUR_SWITCH:Int = 0x51 // 四键开关
    const val SIX_SWITCH:Int = 0x52 // 六键开关

    const val SENSOR: Int = 0x23//pir 版本:PS-1.1.3  35
    const val NIGHT_LIGHT: Int = 0x24       //人体感应器 版本: 2.x.x以上  36
    const val LIGHT_NORMAL: Int = 0x04
    const val LIGHT_NORMAL_OLD: Int = 0xFF
    const val LIGHT_RGB: Int = 0x06
    const val SMART_CURTAIN: Int = 0x10//窗帘
    const val GATE_WAY: Int = 0x31//网关
    const val ROUTER: Int = 0x32//路由
    const val SMART_RELAY: Int = 0x05       //就是connector蓝牙接收器
    const val NIGHT_LIGHT_CONFIG: Int = 0x01       //配置传感器
}