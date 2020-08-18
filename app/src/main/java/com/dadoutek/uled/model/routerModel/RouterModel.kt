package com.dadoutek.uled.model.routerModel

import com.dadoutek.uled.gateway.bean.DbRouter
import com.dadoutek.uled.model.dbModel.DBUtils
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
object RouterModel {

    fun routerConfigWifi(macAddr: String,wifiAccount: String,pwd: String, hour: Int,min: Int,serid:String): Observable<Int>? {
        return NetworkFactory.getApi()
                .routerConfigWifi(macAddr, wifiAccount,pwd, hour,min,serid)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun routerAccessInNet(macAddr: String, hour: Int,min: Int,serid:String): Observable<Int>? {
        return NetworkFactory.getApi()
                .routerAccessIn(macAddr, hour,min, serid)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun bindRouter(meshAddrsList: MutableList<Int>, meshType: Int, macAddr: String): Observable<Response<Any>>? {
        return NetworkFactory.getApi()
                .bindRouter(meshAddrsList, meshType, macAddr).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun delete(it: DbRouter): Observable<Long>? {
        return NetworkFactory.getApi()
                .routerReset(it.macAddr, DBUtils.lastUser?.id ?: 0)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(it: DbRouter): Observable<Any>? {
        return NetworkFactory.getApi()
                .updateRouter(it.id, it.name)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

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