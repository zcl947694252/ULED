package com.dadoutek.uled.network

import java.io.Serializable


/**
 * 创建者     ZCL
 *configuration	是	Configuration	配置数据
 * mode	是	int	0群组，1场景
 * condition	是	int	触发条件。0全天，1白天，2夜晚
 * durationTimeUnit	是	int	持续时间单位。0秒，1分钟
 * durationTimeValue	是	int	持续时间
 * action	否	int	触发时执行逻辑。0开，1关，2自定义亮度。仅在群组模式下需要该配置
 * brightness	否	int	自定义亮度值。仅在群组模式下需要该配置
 * groupMeshAddrs	否	list	配置组meshAddr，可多个。仅在群组模式下需要该配置
 * sid	否	int	配置场景id。仅在场景模式下需要该配置
 */
data class ConfigurationBean(
    val action: Int,
    val brightness: Int,
    val condition: Int,
    val durationTimeUnit: Int,
    val durationTimeValue: Int,
    val groupMeshAddrs: List<Int>,
    val mode: Int,
    val sid: Int
):Serializable
