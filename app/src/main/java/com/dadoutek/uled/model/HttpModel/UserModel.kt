package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object UserModel {
    fun deleteAllData(token: String): Observable<String>? {
        return NetworkFactory.getApi()
                .clearUserData(token)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }
    fun clearUserData(regionId: Int): Observable<String>? {
        return NetworkFactory.getApi()
                .clearUserRegionData(regionId)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }
}