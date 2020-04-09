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
   open var ser_id: Int = 0//app会话id，成功or失败会回传给app
   open var data: String? = null //base64编码后的指令

   //macAddr	是	string	mac地址
   //data	是	string	Base64编码后指令
   //ser_id	是	string	app会话id，成功or失败会回传给app
   //下发标签给网关
   open var macAddr: String? = null
   open var tagHead:Int = 0//是否是标签头 1是标签头

   //同步delete时候用
   open var idList: List<Int>? = null

   //下发命令给网关转发
   open var cmd :Int =0
   open var meshAddr :Int =0

   //data	是	string	byte[]经过Base64编码得到的字符串
   //ser_id	是	string	会话id，推送中回传
   //cmd	是	int	操作类型，推送中回传
   //meshAddr
}