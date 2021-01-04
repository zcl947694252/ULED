package com.dadoutek.uled.network

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/10/27 10:58
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
 data class MeshAddressTypeBody( var meshType: Int, var meshAddrs:List<Int>,var ser_id: String) :Serializable