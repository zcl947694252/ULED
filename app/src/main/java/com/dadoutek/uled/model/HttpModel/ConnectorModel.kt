package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbConnector
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object ConnectorModel {
    fun add(token: String, connector: DbConnector, id: Long, changeId: Long?): Observable<String>? {
        return NetworkFactory.getApi()
                .addRely(token,connector,changeId!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, connector: DbConnector, id: Long
               , lid: Int): Observable<String> {
        return NetworkFactory.getApi()
                .updateRely(token,lid,connector)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun delete(token: String,  id: Long,  lid: Int): Observable<String>? {
        return NetworkFactory.getApi()
                .deleteRely(token,lid)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun get(token: String): Observable<MutableList<DbConnector>>? {
        return NetworkFactory.getApi()
                .getRelyList(token)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    for(item in it){
                        DBUtils.saveConnector(item,true)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}