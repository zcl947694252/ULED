package com.dadoutek.uled.model.httpModel

import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbDeleteGradientBody
import com.dadoutek.uled.model.dbModel.DbDiyGradient
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.RequestBody

object GradientModel {
    fun add(token: String,  body: RequestBody,
            id: Long, changeId: Long?): Observable<String>? {
        return NetworkFactory.getApi()
                .addGradient(token,body,changeId!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, rid: Int, body: RequestBody
               ,id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .updateGradient(token,rid,body)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }


    fun delete(token: String, body: DbDeleteGradientBody, id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .deleteGradients(token,body)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun get(token: String): Observable<MutableList<DbDiyGradient>>? {
        return NetworkFactory.getApi()
                .getGradientList(token)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    for(item in it){
                        DBUtils.saveGradient(item,true)
                       /* for(node in item.colorNodes){
                            DBUtils.saveColorNode(node)
                        }*/
                        for (i in 0..7)
                            DBUtils.saveColorNode(item.colorNodes[i])

                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}