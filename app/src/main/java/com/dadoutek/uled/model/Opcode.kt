package com.dadoutek.uled.model

object Opcode {
    const val GROUP_BRIGHTNESS_ADD: Byte = 0x1C.toByte()//增加组亮度 C代表组 A代表灯
    const val GROUP_BRIGHTNESS_MINUS: Byte = 0x2C.toByte()//降低组亮度
    const val GROUP_CCT_ADD: Byte = 0x3C.toByte()//增加组色温
    const val GROUP_CCT_MINUS: Byte = 0x4C.toByte()//减少组色温
    /**
     * 單组开关
     */
    const val SCENE_SWITCH8K: Byte = 0x00.toByte()//场景8开关
    const val GROUP_SWITCH8K: Byte = 0x7C.toByte()//單组开关
    const val CLOSE: Byte = 0x7e.toByte()//组开关
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