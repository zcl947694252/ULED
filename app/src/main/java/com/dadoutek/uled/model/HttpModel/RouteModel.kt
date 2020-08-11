package com.dadoutek.uled.model.HttpModel

import com.blankj.utilcode.util.ApiUtils
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.network.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


/**
 * 创建者     ZCL
 * 创建时间   2020/8/11 15:01
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
object RouteModel {
    fun routeScanningResult(): Observable<RouteScanResultBean>? {
        return NetworkFactory.getApi().scanResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun routeStartScan(): Observable<Long>? {
        return NetworkFactory.getApi().routeScanDevcie()
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}