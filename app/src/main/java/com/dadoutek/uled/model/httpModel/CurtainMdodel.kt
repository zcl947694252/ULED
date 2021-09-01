package com.dadoutek.uled.model.httpModel

import com.dadoutek.uled.model.BodyBias
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbCurtain
import com.dadoutek.uled.model.dbModel.DbSwitch
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.network.bean.CurtainListBodyBean
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object CurtainMdodel {
    fun add(token: String, curtain: DbCurtain, id: Long, changeId: Long?): Observable<String>? {
        return NetworkFactory.getApi()
                .addCurtain(token,curtain,changeId!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String,dbCurtain: DbCurtain,lid: Int,id: Long): Observable<String> {
        return NetworkFactory.getApi()
                .updateCurtain(token,lid,dbCurtain)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun delete(token: String,  id: Long,  lid: Int): Observable<String>? {
        return NetworkFactory.getApi()
                .deleteCurtain(token,lid)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun get(token: String): Observable<MutableList<DbCurtain>>? {
        return NetworkFactory.getApi()
                .getCurtainList(token)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    for(item in it){
                        DBUtils.saveCurtain(item,true)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun batchAddOrUpdateCurtain(list: List<DbCurtain>) : Observable<String>? {
        return NetworkFactory.getApi()
            .updateCurtain(CurtainListBodyBean(list))
            .compose(NetworkTransformer())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {

            }
    }

    fun remove(list: List<Int>) : Observable<String>? {
        return NetworkFactory.getApi()
            .deleteCurtains(BodyBias(list))
            .compose(NetworkTransformer())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {

            }
    }
}