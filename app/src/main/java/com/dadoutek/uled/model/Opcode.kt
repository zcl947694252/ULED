package com.dadoutek.uled.model

object Opcode {
    val LIGHT_ON_OFF: Byte = 0xD0.toByte()
    val SET_GROUP: Byte = 0xD7.toByte()
    val SET_SCENE_FOR_SWITCH: Byte = 0xF1.toByte()
}