package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
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

    fun update(token: String, name: String, brightness: Int,
               colorTemperature: Int, belongGroupId: Int,color: Int,id: Long
               , lid: Int): Observable<String> {
        val dbLight = DbLight()
        dbLight.name = name
        dbLight.brightness = brightness
        dbLight.colorTemperature = colorTemperature
        dbLight.belongGroupId = belongGroupId.toLong()
        dbLight.color = color
        return NetworkFactory.getApi()
                .updateLight(token,lid,dbLight)
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