package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object GroupMdodel {
    fun add(token: String, dbGroup: DbGroup, belongRegionId: Int, id: Long, changeId: Long?):
            Observable<String>? {
        return NetworkFactory.getApi()
                .addGroup(token, dbGroup, belongRegionId, changeId!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, rid: Int, name: String, brightness: Int, colorTemperature: Int, id: Long): Observable<String>? {
        val dbGroup = DbGroup()
        dbGroup.name = name
        dbGroup.brightness = brightness
        dbGroup.colorTemperature = colorTemperature
        return NetworkFactory.getApi()
                .updateGroup(token, rid, dbGroup)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun delete(token: String, rid: Int, id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .deleteGroup(token, rid)
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
                    for (item in it) {
                        DBUtils.saveGroup(item, true)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}