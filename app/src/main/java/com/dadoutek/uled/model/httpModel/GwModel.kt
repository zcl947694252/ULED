package com.dadoutek.uled.model.httpModel

import com.dadoutek.uled.gateway.bean.ClearGwBean
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


/**
 * 创建者     ZCL
 * 创建时间   2020/3/20 17:32
 * 描述 网关相关接口
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
object GwModel {
    fun add(dbGateway: DbGateway): Observable<DbGateway>? {
        return NetworkFactory.getApi().addGw(dbGateway.id, dbGateway)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext {

                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun getGwList(): Observable<List<DbGateway>>? {
        return NetworkFactory.getApi().gwList.compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun deleteGwList(gattBody: GwGattBody): Observable<String>? {
        return NetworkFactory.getApi().deleteGw(gattBody).compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { }
    }

    fun sendToGatt(gattBody: GwGattBody): Observable<String>? {
        return NetworkFactory.getApi().sendGwToService(gattBody)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext { }
                .observeOn(AndroidSchedulers.mainThread())
    }
    fun sendDeviceToGatt(gattBody: GwGattBody): Observable<String>? {
        return NetworkFactory.getApi().sendDeviceToMqtt(gattBody)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext { }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun clearGwData(gwId:Long): Observable<ClearGwBean>? {
        return NetworkFactory.getApi().clearGwInfo(gwId)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext { }
                .observeOn(AndroidSchedulers.mainThread())
    }

}