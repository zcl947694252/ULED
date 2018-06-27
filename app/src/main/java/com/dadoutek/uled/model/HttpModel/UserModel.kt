package com.dadoutek.uled.model.HttpModel

import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.intf.NetworkFactory
import com.dadoutek.uled.intf.NetworkTransformer
import com.dadoutek.uled.model.DbModel.DBUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object UserModel {
    fun deleteAllData(token: String): Observable<String>? {
        return NetworkFactory.getApi()
                .clearUserData(token)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}