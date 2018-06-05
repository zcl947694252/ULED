package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.intf.NetworkFactory
import com.dadoutek.uled.intf.NetworkTransformer
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbUser
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object GroupMdodel {
    fun add(token: String, meshAddr: Int, name: String,brightness: Int,colorTemperature: Int): Observable<Void>? {
        return NetworkFactory.getApi()
                .addGroup(token,meshAddr,name,brightness,colorTemperature)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }


}