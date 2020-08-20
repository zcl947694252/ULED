package com.dadoutek.uled.model.routerModel

import com.dadoutek.uled.gateway.bean.DbRouter
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.network.*
import com.dadoutek.uled.router.bean.RouteScanResultBean
import com.dadoutek.uled.router.bean.RouterBatchGpBean
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.http.Field


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

    /**
     * 配置路由wifi
     */
    fun routerConfigWifi(macAddr: String,wifiAccount: String,pwd: String, hour: Int,min: Int,serid:String): Observable<Int>? {
        return NetworkFactory.getApi()
                .routerConfigWifi(macAddr, wifiAccount,pwd, hour,min,serid)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 添加路由入网
     */
    fun routerAccessInNet(macAddr: String, hour: Int,min: Int,serid:String): Observable<Int>? {
        return NetworkFactory.getApi()
                .routerAccessIn(macAddr, hour,min, serid)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 绑定路由设备
     */
    fun bindRouter(meshAddrsList: MutableList<Int>, meshType: Int, macAddr: String): Observable<Response<Any>>? {
        return NetworkFactory.getApi()
                .bindRouter(meshAddrsList, meshType, macAddr).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 删除路由
     */
    fun delete(it: DbRouter): Observable<Long>? {
        return NetworkFactory.getApi()
                .routerReset(it.macAddr, DBUtils.lastUser?.id ?: 0)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 更新路由相关信息
     */
    fun update(it: DbRouter): Observable<Any>? {
        return NetworkFactory.getApi()
                .updateRouter(it.id, it.name)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 路由扫描结果
     */
    fun routeScanningResult(): Observable<RouteScanResultBean>? {
        return NetworkFactory.getApi().scanResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 路由停止扫描
     */
    fun routeStopScan(serid: String,scanSerId: Long): Observable<Response<Long>>? {
        return NetworkFactory.getApi().routeStopScanDevcie(serid,scanSerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 路由停止扫描告诉服务器清除数据
     */
    fun routeStopScanClear(): Observable<Response<Any>>? {
        return NetworkFactory.getApi().tellServerClearScanning()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 路由批量分组
     */
    fun routeBatchGp( targetGroupMeshAddr:Long, deviceMeshAddrs:  List<Int>, meshType:Long,  ser_id:String ): Observable<Response<RouterBatchGpBean>>? {
        return NetworkFactory.getApi().routerBatchGp( targetGroupMeshAddr, deviceMeshAddrs, meshType,ser_id )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 路由开始扫描
     */
    fun routeStartScan(): Observable<Long>? {
        return NetworkFactory.getApi().routeScanDevcie()
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 路由开始扫描
     */
    fun routerStatus(): Observable<RouteStasusBean>? {
        return NetworkFactory.getApi().routerStatus
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

}