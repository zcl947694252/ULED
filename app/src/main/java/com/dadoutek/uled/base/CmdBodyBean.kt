package com.dadoutek.uled.base

import java.io.Serializable

data class CmdBodyBean(
    var cmd: Int = 0,
    var finish: Boolean = false,
    var msg: String = "",
    var ser_id: String = "",
    var count: Int = 0,//扫描数
    var scanSerId: Long = 0,//扫描返回的扫描serid
    var status: Int = 0//status	int	当前处理结果。-1：全部失败；0：全部成功；1：部分成功
):Serializable


