package com.dadoutek.uled.region

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/22 9:45
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */


 class RegionBcBean(
    val authorizer_id: Int,
    val belongAccount: String,
    val code_info: CodeInfo,
    val controlMesh: String,
    val controlMeshPwd: String,
    val count_all: Int,
    val count_curtain: Int,
    val count_light: Int,
    val count_relay: Int,
    val count_sensor: Int,
    val count_switch: Int,
    val id: Int,
    val installMesh: String,
    val installMeshPwd: String,
    val lastGenMeshAddr: Int,
    val name: String,
    val ref_users: List<Any>,
    val state: Int
):Serializable{
    override fun toString(): String {
        return "Data(authorizer_id=$authorizer_id, belongAccount='$belongAccount', code_info=$code_info, controlMesh='$controlMesh', controlMeshPwd='$controlMeshPwd', count_all=$count_all, count_curtain=$count_curtain, count_light=$count_light, count_relay=$count_relay, count_sensor=$count_sensor, count_switch=$count_switch, id=$id, installMesh='$installMesh', installMeshPwd='$installMeshPwd', lastGenMeshAddr=$lastGenMeshAddr, name='$name', ref_users=$ref_users, state=$state)"
    }
}

class CodeInfo(
)