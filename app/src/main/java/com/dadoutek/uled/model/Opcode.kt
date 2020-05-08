package com.dadoutek.uled.model

object Opcode {
    /**
     * 配置单调光双组开关
     */
    const val CONFIG_DOUBLE_SWITCH: Byte = 0xf1.toByte()

    /**
     * 扩展指令用户数据清除功能
     */
    const val CONFIG_EXTEND_ALL_CLEAR: Byte = 0x04.toByte()
    /**
     * 扩展指令修改调光频率
     */
    const val CONFIG_EXTEND_ALL_PWM: Byte = 0x03.toByte()
    /**
     * 扩展指令修改渐变速度
     */
    const val CONFIG_EXTEND_ALL_JBSD: Byte = 0x02.toByte()
    /**
     * 扩展指令设置渐变指令
     */
    const val CONFIG_EXTEND_ALL_JBZL: Byte = 0x01.toByte()
    /**
     * 扩展指令头
     */
    const val CONFIG_EXTEND_OPCODE: Byte = 0x26.toByte()
    /**
     * 开关网关
     */
    const val CONFIG_GW_SWITCH: Byte = 0xd0.toByte()
    /**
     * 网关默认恢复出厂
     */
    const val CONFIG_GW_REST_FACTORY: Byte = 0xf0.toByte()
    /**
     * 设置获取MAC的Opcode
     */
    const val CONFIG_GW_GET_MAC: Byte = 0xfe.toByte()
    /**
     * 设置时区和时间
     */
    const val CONFIG_GW_SET_TIME_ZONE: Byte = 0xfd.toByte()
    /**
     * 设置wifi sdid
     */
    const val CONFIG_GW_WIFI_PASSWORD: Byte = 0xfb.toByte()
    /**
     * 设置wifi sdid
     */
    const val CONFIG_GW_WIFI_SDID: Byte = 0xfa.toByte()
    /**
     * 删除循环场景时间段
     */
    const val CONFIG_GW_TIMER_PERIOD_DELETE_TASK: Byte = 0xf9.toByte()
    /**
     * 删除循环场景标签:
     */
    const val CONFIG_GW_TIMER_PERIOD_DELETE_LABLE: Byte = 0xf8.toByte()
    /**
     * 循环场景时间下发:
     */
    const val CONFIG_GW_TIMER_PERIOD_LABLE_TASK: Byte = 0xf7.toByte()
    /**
     * w网关循环场景标签头下发
     */
    const val CONFIG_GW_TIMER_PERIOD_LABLE_HEAD: Byte =0xf6.toByte()
    /**
     * 网关定时模式删除TASK
     */
    const val CONFIG_GW_TIMER_DELETE_TASK: Byte = 0xf5.toByte()
    /**
     * 网关定时模式删除标签
     */
    const val CONFIG_GW_TIMER_DELETE_LABLE: Byte = 0xf4.toByte()
    /**
     * 网关定时场景标签头下发
     */
    const val CONFIG_GW_TIMER_LABLE_HEAD: Byte = 0xf2.toByte()
    /**
     * 网关定时场景时间下发
     */
    const val CONFIG_GW_TIMER_LABLE_TIME: Byte = 0xf3.toByte()

    const val GROUP_BRIGHTNESS_ADD: Byte = 0x1C.toByte()//增加组亮度 C代表组 A代表灯
    const val GROUP_BRIGHTNESS_MINUS: Byte = 0x2C.toByte()//降低组亮度
    const val GROUP_CCT_ADD: Byte = 0x3C.toByte()//增加组色温
    const val GROUP_CCT_MINUS: Byte = 0x4C.toByte()//减少组色温
    /**
     * 單组开关
     */
    const val SCENE_SWITCH8K: Byte = 0x05.toByte()//场景8开关
    const val GROUP_SWITCH8K: Byte = 0x7C.toByte()//單组开关
    const val DEFAULT_SWITCH8K: Byte = 0xFF.toByte()//默认无效
    const val CLOSE: Byte = 0x03.toByte()//关  用于场景关闭按键
    const val SWITCH_ALL_GROUP: Byte = 0x7d.toByte()//全组开关

    const val BRIGHTNESS_ADD: Byte = 0x1A.toByte()
    const val BRIGHTNESS_MINUS: Byte = 0x2A.toByte()
    const val CCT_ADD: Byte = 0x3A.toByte()
    const val CCT_MINUS: Byte = 0x4A.toByte()

    const val LIGHT_ON_OFF: Byte = 0xD0.toByte()
    const val LIGHT_BLINK_ON_OFF: Byte = 0xF5.toByte()
    const val CURTAIN_ON_OFF: Byte = 0xF2.toByte()
    const val SET_GROUP: Byte = 0xD7.toByte()
    const val GET_GROUP: Byte = 0xDD.toByte()
    const val GET_VERSION: Byte = 0xFC.toByte()
    const val SEND_MESSAGE_BY_MESH: Byte = 0xC2.toByte()
    const val CONFIG_SCENE_SWITCH: Byte = 0xF1.toByte()
    const val CONFIG_PIR: Byte = 0xF1.toByte()
    const val SCENE_ADD_OR_DEL: Byte = 0xEE.toByte()
    const val SCENE_LOAD: Byte = 0xEF.toByte()
    const val SET_LUM: Byte = 0xD2.toByte()
    const val SET_W_LUM: Byte = 0xFA.toByte()
    const val SET_TEMPERATURE: Byte = 0xE2.toByte()
    const val KICK_OUT: Byte = 0xE3.toByte()
    const val APPLY_RGB_GRADIENT: Byte = 0xFE.toByte()
    const val CONFIG_LIGHT_LIGHT: Byte = 0xF1.toByte()
    const val CURTAIN_SPECIFIED_LOCATION: Byte = 0x0F.toByte()
    const val CURTAIN_MOTOR_COMMUTATION: Byte = 0x11.toByte()
    const val CURTAIN_MANUAL_MODE: Byte = 0x12.toByte()
    const val CURTAIN_PACK_START: Byte = 0xE1.toByte()
    const val CURTAIN_PACK_END: Byte = 0xEF.toByte()
    const val MESH_KICK_OUT: Byte = 0xF4.toByte()

    const val MESH_PROVISION: Byte = 0xC9.toByte()
    const val FIX_MESHADDR_CONFLICT: Byte = 0xF4.toByte()
//    const val CURTAIN_MOTOR_COMMUTATION: Byte = 0x01.toByte()
}