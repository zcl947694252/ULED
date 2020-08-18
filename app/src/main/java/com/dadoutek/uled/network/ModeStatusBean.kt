package com.dadoutek.uled.network


/**
 * 创建者     ZCL
 * 创建时间   2020/8/18 16:11
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class ModeStatusBean(
        /**
         * 辅助模式是否开启
         */
        var auxiliaryFunction: Boolean = false,
        /**
         * app使用模式。0蓝牙，1路由
         */
        var mode: Int = 0
)