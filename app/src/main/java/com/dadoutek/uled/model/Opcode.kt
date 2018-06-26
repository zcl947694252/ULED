package com.dadoutek.uled.model

import android.transition.Scene

object Opcode {
    const val LIGHT_ON_OFF: Byte = 0xD0.toByte()
    const val SET_GROUP: Byte = 0xD7.toByte()
    const val CONFIG_SCENE_SWITCH: Byte = 0xF1.toByte()
    const val CONFIG_PIR: Byte = 0xF1.toByte()
    const val SCENE_ADD_OR_DEL: Byte = 0xEE.toByte()
    const val SCENE_LOAD: Byte = 0xEF.toByte()
    const val SET_LUM: Byte = 0xD2.toByte()
    const val SET_TEMPERATURE: Byte = 0xE2.toByte()
    const val KICK_OUT: Byte = 0xE3.toByte()
}