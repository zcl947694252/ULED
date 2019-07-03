package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object UpdateModel {

    fun checkVersion(device:Int,version:String): Observable<Any>? {
        return NetworkFactory.getApi()
                .isAvailavle(device, version)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun isRegister(phone:String): Observable<Any>? {
        return NetworkFactory.getApi()
                .isRegister(phone)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}