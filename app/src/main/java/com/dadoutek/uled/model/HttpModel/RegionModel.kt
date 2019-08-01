package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbRegion
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.network.bean.BaseBean
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object RegionModel {
   fun add(token: String, dbRegion: DbRegion, id: Long, changeId: Long?): Observable<String>? {
       return NetworkFactory.getApi()
               .addRegion(token, dbRegion, changeId!!.toInt())
               .compose(NetworkTransformer())
               .observeOn(Schedulers.io())
               .doOnNext {
                   DBUtils.deleteDbDataChange(id)
               }
               .observeOn(AndroidSchedulers.mainThread())
   }

    fun addRegions(token: String, dbRegion: DbRegion, changeId: Long?): Observable<BaseBean<Any>>? {
        return NetworkFactory.getApi()
                .addRegionNew(token, dbRegion, changeId!!)
                .subscribeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, rid: Int, dbRegion: DbRegion, id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .updateRegion(token, rid, dbRegion)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun delete(token: String, rid: Int, id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .deleteRegion(token, rid)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun get(token: String): Observable<MutableList<DbRegion>>? {
        return NetworkFactory.getApi()
                .getRegionList(token)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    for (item in it) {
                        DBUtils.saveRegion(item, true)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}