package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.model.DbModel.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object GroupMdodel {
    fun add(token: String, meshAddr: Int, name: String, brightness: Int, colorTemperature: Int, belongRegionId: Int, id: Long, changeId: Long?): Observable<String>? {
        return NetworkFactory.getApi()
                .addGroup(token,meshAddr,name,brightness,colorTemperature,belongRegionId,changeId!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, rid: Int, name: String, brightness: Int, colorTemperature: Int, id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .updateGroup(token,rid,name,brightness,colorTemperature)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun delete(token: String, rid: Int,id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .deleteGroup(token,rid)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun get(token: String): Observable<MutableList<DbGroup>>? {
        return NetworkFactory.getApi()
                .getGroupList(token)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    for(item in it){
                        DBUtils.saveGroup(item,true)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}