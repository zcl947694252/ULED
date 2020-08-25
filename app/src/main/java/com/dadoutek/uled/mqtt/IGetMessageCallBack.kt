package com.dadoutek.uled.mqtt

interface IGetMessageCallBack {
    fun setMessage(cmd: Int, extCmd: Int, message: String)
}