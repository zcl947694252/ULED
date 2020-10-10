package com.dadoutek.uled.model.httpModel

import com.dadoutek.uled.model.Response
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object GroupMdodel {
    fun add( dbGroup: DbGroup, /*belongRegionId: Int,*/ id: Long, changeId: Long?):
            Observable<String>? {
        return NetworkFactory.getApi()//todo 添加token lastReginID
                .addGroup(dbGroup, /*belongRegionId,*/ changeId!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
    fun batchAddOrUpdateGp(list: List<DbGroup>): Observable<Response<Any>>? {
        return NetworkFactory.getApi()
                .batchUpGroupList(list)
                .observeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, rid: Int, name: String, brightness: Int, colorTemperature: Int,color: Int,id: Long): Observable<String>? {
        val dbGroup = DbGroup()
        dbGroup.name = name
        dbGroup.brightness = brightness
        dbGroup.colorTemperature = colorTemperature
        dbGroup.color=color
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