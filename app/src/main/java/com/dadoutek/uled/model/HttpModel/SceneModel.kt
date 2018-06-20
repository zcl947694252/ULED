package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.intf.NetworkFactory
import com.dadoutek.uled.intf.NetworkTransformer
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.DbModel.DbSceneBody
import com.google.gson.JsonArray
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.RequestBody

object SceneModel {
    fun add(token: String,  body: RequestBody,
            id: Long, changeId: Long?): Observable<String>? {
        return NetworkFactory.getApi()
                .addScene(token,body,changeId!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, rid: Int, body: RequestBody
               ,id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .updateScene(token,rid,body)
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