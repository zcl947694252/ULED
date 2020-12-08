package com.dadoutek.uled.router.bean

import java.io.Serializable

data class CmdBodyBean(
    var cmd: Int = 0,
    var finish: Boolean = false,
    var msg: String = "",
    var ser_id: String = "",
    var count: Int = 0,//扫描数
    var scanSerId: Long = 0,//扫描返回的扫描serid
    var status: Int = 0,//status	int	当前处理结果。-1：全部失败；0：全部成功；1：部分成功
    val meshAddr: Int,
    val success: Boolean,
    val reportTimeout: Int,
    val version: String
):Serializable{
    override fun toString(): String {
        return "CmdBodyBean(cmd=$cmd, finish=$finish, msg='$msg', ser_id='$ser_id', count=$count, scanSerId=$scanSerId, status=$status, meshAddr=$meshAddr, success=$success, reportTimeout=$reportTimeout, version='$version')"
    }
}
