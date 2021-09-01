package com.dadoutek.uled.model.httpModel

import com.dadoutek.uled.model.BodyBias
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbSensor
import com.dadoutek.uled.model.dbModel.DbSensorChild
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.network.bean.SensorListBodyBean
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object SensorMdodel {
    fun add(token: String, sensor: DbSensor, id: Long, changeId: Long?): Observable<String>? {
        var dbChild=DbSensorChild()
        dbChild.productUUID=sensor.productUUID
        dbChild.meshAddr=sensor.meshAddr
        dbChild.macAddr=sensor.macAddr
        dbChild.name=sensor.name
        dbChild.id=sensor.id
        dbChild.index=sensor.index
        dbChild.version=sensor.version
        dbChild.status=sensor.status
        if(sensor.controlGroupAddr!=null){
            dbChild.list=sensor.controlGroupAddr
        }else{
            dbChild.list=" "
        }
        return NetworkFactory.getApi()
                .addSensor(token,dbChild,changeId!!.toInt())

                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, dbSensor: DbSensor, lid: Int, id: Long): Observable<String> {
        var dbChild=DbSensorChild()
        dbChild.productUUID=dbSensor.productUUID
        dbChild.meshAddr=dbSensor.meshAddr
        dbChild.macAddr=dbSensor.macAddr
        dbChild.name=dbSensor.name
        dbChild.id=dbSensor.id
        dbChild.index=dbSensor.index
        dbChild.version=dbSensor.version
        dbChild.status=dbSensor.status
        if(dbSensor.controlGroupAddr!=null){
            dbChild.list=dbSensor.controlGroupAddr
        }else{
            dbChild.list=" "
        }
        return NetworkFactory.getApi()
                .updateSensor(token,lid,dbChild)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun delete(token: String,  id: Long,  lid: Int): Observable<String>? {
        return NetworkFactory.getApi()
                .deleteSensor(token,lid)
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

    fun batchAddOrUpdateSensor(list: List<DbSensor>) : Observable<String> ? {
        return NetworkFactory.getApi()
            .updateSensors(SensorListBodyBean(list))
            .compose(NetworkTransformer())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {

            }
    }

    fun remove(list: List<Int>) : Observable<String> ? {
        return NetworkFactory.getApi()
            .deleteSensor(BodyBias(list))
            .compose(NetworkTransformer())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {

            }
    }
}