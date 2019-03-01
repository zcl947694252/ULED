package com.dadoutek.uled.model

import android.transition.Scene

object Opcode {
    const val LIGHT_ON_OFF: Byte = 0xD0.toByte()
    const val CURTAIN_ON_OFF: Byte = 0xF2.toByte()
    const val SET_GROUP: Byte = 0xD7.toByte()
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
//    const val CURTAIN_MOTOR_COMMUTATION: Byte = 0x01.toByte()
}