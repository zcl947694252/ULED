package com.dadoutek.uled.network


/**
 * 创建者     ZCL
 * 创建时间   2020/3/20 18:29
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GwGattBody {
   open var macAddr: String? = null
   open var idList: List<Int>? = null
   open var ser_id: Int = 0//app会话id，成功or失败会回传给app
   open var data: String? = null //base64编码后的指令
   open var tagHead:Int = 0//是否是标签头 1是标签头

}