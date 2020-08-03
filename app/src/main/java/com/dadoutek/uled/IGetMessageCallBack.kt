package com.dadoutek.uled

interface IGetMessageCallBack {
    fun setMessage(cmd: Int, extCmd: Int, message: String)
}