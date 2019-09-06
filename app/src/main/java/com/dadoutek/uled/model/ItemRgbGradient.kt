package com.dadoutek.uled.model

class ItemRgbGradient {
    var name: String? = null
    var position: Int = 100
    var speed: Int = 0
    var select: Boolean = false
    var id: Int = 0
    override fun toString(): String {
        return "ItemRgbGradient(name=$name, position=$position, speed=$speed, select=$select, id=$id)"
    }

}