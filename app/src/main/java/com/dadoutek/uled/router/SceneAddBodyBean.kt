package com.dadoutek.uled.router

import com.dadoutek.uled.model.dbModel.DbSceneActions
import retrofit2.http.Field
import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/28 10:54
 * 描述
 *name	是	string	场景名称
imgName	是	string	场景展示icon名
actions	是	List<Action>	本地生成的actions直接上传即可，已做好兼容
ser_id
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class SceneAddBodyBean(var name: String, var imgName: String?, var actions: List<DbSceneActions>, var ser_id: String) : Serializable {
    override fun toString(): String {
        return "SceneAddBodyBean(sceneName='$name', imgName=$imgName, actions=$actions, ser_id='$ser_id')"
    }
}
