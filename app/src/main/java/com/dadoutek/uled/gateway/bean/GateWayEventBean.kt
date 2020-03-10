package com.dadoutek.uled.gateway.bean


/**
 * 创建者     zcl
 * 创建时间   2020/3/10 14:26
 * 描述	      {lable--标签名  mode - 1定时模式 2时间段模式  repetion week循环,0—6代表星期天----星期六  7代表当天}
 *
 * 更新者    $author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}$
 */
class GateWayEventBean (var lable:String,var mode:Int = 0,var repetion:Int)