package com.dadoutek.uled.model.HttpModel

import com.blankj.utilcode.util.LogUtils
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
                    LogUtils.v("zcl请求升级版本$localVersion")
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}