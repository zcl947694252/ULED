package com.dadoutek.uled.model.HttpModel

import android.content.Context
import android.os.Environment
import android.util.Log
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object DownLoadFileModel {

    fun getUrl(type:Int,detailType:Int): Observable<String>? {
        return NetworkFactory.getApi()
                .getFirmwareUrl(type,detailType)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun getUrlNew(localVersion: String): Observable<Any>? {
        return NetworkFactory.getApi()
                .getFirmwareUrlNew(localVersion)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}