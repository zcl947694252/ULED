package com.dadoutek.uled.model.httpModel

import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.model.BodyBias
import com.dadoutek.uled.model.Light
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbLight
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.network.bean.LightListBodyBean
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object LightModel {
    fun add(token: String,light: DbLight, id: Long, changeId: Long?): Observable<String>? {
        return NetworkFactory.getApi()
                .addLight(token,light,changeId!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, light:DbLight,id: Long
               , lid: Int): Observable<String> {
        return NetworkFactory.getApi()
                .updateLight(token,lid,light)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun delete(token: String,  id: Long,  lid: Int): Observable<String>? {
        return NetworkFactory.getApi()
                .deleteLight(token,lid)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun get(token: String): Observable<MutableList<DbLight>>? {
        return NetworkFactory.getApi()
                .getLightList(token)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    for(item in it){
                        DBUtils.saveLight(item,true)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun batchAddorUpdateLight(dbLights: List<DbLight>): Observable<String>? {
        LogUtils.v("chown ------batchAddorUpdateLight ${LightListBodyBean(dbLights).toString()}")
        return NetworkFactory.getApi()
            .updateLights(LightListBodyBean(dbLights))
            .compose(NetworkTransformer())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun remove(idList: List<Int>) :Observable<String>? {
        return NetworkFactory.getApi()
            .deleteLights(BodyBias(idList))
            .compose(NetworkTransformer())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }
}