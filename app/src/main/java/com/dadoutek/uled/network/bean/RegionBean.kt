package com.dadoutek.uled.region.bean

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2019/8/2 19:36
 * 描述	      ${TODO}
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
class RegionBean: Serializable {

    /**
     * count_light : 121
     * count_curtain : 0
     * count_sensor : 0
     * count_all : 121
     * installMesh : dadousmart
     * count_switch : 0
     * code_info : {"expire":86396,"code":"dadoueyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpemVyX2lkIjoyNjQ0NywicmVnaW9uX2lkIjoxLCJsZXZlbCI6MX0.ys4q7YTbaDD56IaDHUfqJftl86_yFWKHWkgH1zFYwHosmartlight","type":1}
     * belongAccount : 0611685
     * ref_users : [{"password":"99762463923db138661c164aa2e7bda3","role":0,"create_time":"2019-05-27 07:14:11","phone":"9999166229","channel":"dadou_java","name":"","last_region_id":1,"id":300354,"avatar":"","email":"","account":"10300354","token":"eyJhbGciOiJIUzI1NiIsImlhdCI6MTU1ODk1NTY1MSwiZXhwIjoxNTU4OTU5MjUxfQ.eyJpZCI6MzAwMzU0fQ.kkdfG9mnvV-JRVyTO-EmXGj6wBSa1dqzWE2bLbFAy2"},{"password":"bd11ad0f062f4e8725446f2e8402669d","role":0,"create_time":"2019-05-28 04:25:45","phone":"13424500503","channel":"dadou_java","name":"","last_region_id":1,"id":300358,"avatar":"","email":"","account":"10300358","token":"eyJhbGciOiJIUzI1NiIsImlhdCI6MTU1OTAzMTk0NSwiZXhwIjoxNTU5MDM1NTQ1fQ.eyJpZCI6MzAwMzU4fQ.l9cVDFWsQcVrCSevLRB-Aud-OP8QXJzUk9D-HNLZRL"}]
     * controlMesh : 0611685
     * installMeshPwd : 123
     * count_relay : 0
     * name : 区域一/region1
     * id : 1
     * controlMeshPwd : 0611685
     * state : 1
     */
    var lastGenMeshAddr: Int = 0
    var authorizer_id: Int = 0
    var count_light: Int = 0
    var count_curtain: Int = 0
    var count_sensor: Int = 0
    var count_all: Int = 0
    var installMesh: String? = null
    var count_switch: Int = 0
    var code_info: CodeInfoBean? = null
    var belongAccount: String? = null
    var controlMesh: String? = null
    var installMeshPwd: String? = null
    var count_relay: Int = 0
    var name: String? = null
    var id: Long = 0
    var controlMeshPwd: String? = null
    var state: Int = 0
    var ref_users: List<RefUsersBean>? = null
    var is_selected :Boolean? = false

    constructor(name: String?) {
        this.name = name
    }

    constructor()


    class CodeInfoBean :Serializable {
        /**
         * expire : 86396
         * code : dadoueyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpemVyX2lkIjoyNjQ0NywicmVnaW9uX2lkIjoxLCJsZXZlbCI6MX0.ys4q7YTbaDD56IaDHUfqJftl86_yFWKHWkgH1zFYwHosmartlight
         * type : 1
         */

        var expire: Int = 0
        var code: String? = null
        var type: Int = 0
        override fun toString(): String {
            return "CodeInfoBean(expire=$expire, code=$code, type=$type)"
        }
    }

    class RefUsersBean :Serializable{
        /**
         * password : 99762463923db138661c164aa2e7bda3
         * role : 0
         * create_time : 2019-05-27 07:14:11
         * phone : 9999166229
         * channel : dadou_java
         * name :
         * last_region_id : 1
         * id : 300354
         * avatar :
         * email :
         * account : 10300354
         * token : eyJhbGciOiJIUzI1NiIsImlhdCI6MTU1ODk1NTY1MSwiZXhwIjoxNTU4OTU5MjUxfQ.eyJpZCI6MzAwMzU0fQ.kkdfG9mnvV-JRVyTO-EmXGj6wBSa1dqzWE2bLbFAy2
         */

        var password: String? = null
        var role: Int = 0
        var create_time: String? = null
        var phone: String? = null
        var channel: String? = null
        var name: String? = null
        var last_region_id: Int = 0
        var id: Int = 0
        var avatar: String? = null
        var email: String? = null
        var account: String? = null
        var token: String? = null

        override fun toString(): String {
            return "RefUsersBean(password=$password, role=$role, create_time=$create_time, phone=$phone, channel=$channel, name=$name, last_region_id=$last_region_id, id=$id, avatar=$avatar, email=$email, account=$account, token=$token)"
        }
    }
    override fun toString(): String {
        return "RegionBean(lastGenMeshAddr=$lastGenMeshAddr, authorizer_id=$authorizer_id, count_light=$count_light, count_curtain=$count_curtain, count_sensor=$count_sensor, count_all=$count_all, installMesh=$installMesh, count_switch=$count_switch, code_info=$code_info, belongAccount=$belongAccount, controlMesh=$controlMesh, installMeshPwd=$installMeshPwd, count_relay=$count_relay, name=$name, id=$id, controlMeshPwd=$controlMeshPwd, state=$state, ref_users=$ref_users, is_selected=$is_selected)"
    }
}
