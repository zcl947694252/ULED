package com.dadoutek.uled.model.dbModel


/**
 * 创建者     ZCL
 * 创建时间   2020/1/16 10:22
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
data class KeysBean (
    var keys: List<Key>
)

data class Key(
    var featureId: Int,
    var keyId: Int,
    var params: List<Int>
)