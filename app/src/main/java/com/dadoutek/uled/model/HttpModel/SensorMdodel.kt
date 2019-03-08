package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object SensorMdodel {
    fun add(token: String, sensor: DbSensor, id: Long, changeId: Long?): Observable<String>? {
        return NetworkFactory.getApi()
                .addSensor(token,sensor,changeId!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, dbSensor: DbSensor, lid: Int, id: Long): Observable<String> {
        return NetworkFactory.getApi()
                .updateSensor(token,lid,dbSensor)
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

    fun get(token: String): Observable<MutableList<DbSensor>>? {
        return NetworkFactory.getApi()
                .getSensorList(token)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    for(item in it){
                        DBUtils.saveSensor(item,true)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}