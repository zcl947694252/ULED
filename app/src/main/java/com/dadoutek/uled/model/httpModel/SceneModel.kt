package com.dadoutek.uled.model.httpModel

import com.dadoutek.uled.model.BodyBias
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbScene
import com.dadoutek.uled.network.bean.SceneBody
import com.dadoutek.uled.network.bean.SceneListBodyBean
import com.dadoutek.uled.network.bean.SceneListBodyBean2
import io.reactivex.Observable
import io.reactivex.Scheduler
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
    fun batchAddOrUpdateScene(dbScene:List<SceneBody>):Observable<String>? {
        return NetworkFactory.getApi()
            .updateScenes(SceneListBodyBean2(dbScene))
            .compose(NetworkTransformer())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {  }
    }

    fun remove(list: List<Int>) : Observable<String> ? {
        return NetworkFactory.getApi()
            .deleteScenes(BodyBias(list))
            .compose(NetworkTransformer())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {  }
    }
}