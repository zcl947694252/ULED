package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.intf.NetworkFactory
import com.dadoutek.uled.intf.NetworkTransformer
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbRegion
import com.dadoutek.uled.model.DbModel.DbScene
import com.google.gson.JsonArray
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray

object SceneModel {
    fun add(token: String, name: String, actions: JsonArray,
            belongRegionId: Int,id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .addScene(token,name,actions,belongRegionId)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, rid: Int, name: String,actions: JsonArray
               ,id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .updateScene(token,rid,name,actions)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }


    fun delete(token: String, rid: Int,id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .deleteScene(token,rid)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun get(token: String): Observable<MutableList<DbScene>>? {
        return NetworkFactory.getApi()
                .getSceneList(token)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    for(item in it){
                        DBUtils.saveScene(item,true)
                        for(action in item.actions){
                            DBUtils.saveSceneActions(action)
                        }
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}