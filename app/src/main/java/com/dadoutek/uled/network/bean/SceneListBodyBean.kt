package com.dadoutek.uled.network.bean

import com.dadoutek.uled.model.dbModel.DbScene
import okhttp3.RequestBody
import java.io.Serializable


/**
 * 创建者     Chown
 * 创建时间   2021/8/26 15:46
 * 描述
 */
class SceneListBodyBean (
    val scenes : List<RequestBody>
) : Serializable