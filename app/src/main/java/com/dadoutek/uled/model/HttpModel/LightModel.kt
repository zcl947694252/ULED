package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.intf.NetworkFactory
import com.dadoutek.uled.intf.NetworkTransformer
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object LightModel {
    fun add(token: String, meshAddr: Int, name: String, brightness: Int, colorTemperature: Int,
            macAddr:String, meshUUID: Int, productUUID: Int, belongGroupId: Int,
            id: Long, changeId: Long?): Observable<String>? {
        return NetworkFactory.getApi()
                .addLight(token,meshAddr,name,brightness,colorTemperature,
                        macAddr,meshUUID,productUUID,belongGroupId,changeId!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, name: String, brightness: Int,
               colorTemperature: Int, belongGroupId: Int, id: Long
               , lid: Int): Observable<String>? {
        return NetworkFactory.getApi()
                .updateLight(token,name,brightness,colorTemperature,
                        belongGroupId,lid)
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
}