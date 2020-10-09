package com.dadoutek.uled.model.routerModel

import com.dadoutek.uled.gateway.bean.DbRouter
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.model.dbModel.DbColorNode
import com.dadoutek.uled.model.dbModel.DbSceneActions
import com.dadoutek.uled.network.*
import com.dadoutek.uled.rgb.AddGradientBean
import com.dadoutek.uled.router.DelGradientBodyBean
import com.dadoutek.uled.router.GroupBlinkBodyBean
import com.dadoutek.uled.router.SceneAddBodyBean
import com.dadoutek.uled.router.bean.*
import com.dadoutek.uled.switches.bean.KeyBean
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

    /**
     * 配置路由wifi
     */
    fun routerConfigWifi(macAddr: String, wifiAccount: String, pwd: String, hour: Int, min: Int, serid: String): Observable<RouterTimeoutBean>? {
        return NetworkFactory.getApi()
                .routerConfigWifi(macAddr, wifiAccount, pwd, hour, min, serid)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 添加路由入网
     */
    fun routerAccessInNet(macAddr: String, hour: Int, min: Int, serid: String): Observable<Int>? {
        return NetworkFactory.getApi()
                .routerAccessIn(macAddr, hour, min, serid)
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
    fun delete(bodyBean: MacResetBody): Observable<Long>? {
        return NetworkFactory.getApi()
                .routerReset(bodyBean)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 更新路由相关信息
     */
    fun updateRouter(it: DbRouter): Observable<Any>? {
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
    fun getRouteScanningResult(): Observable<RouteScanResultBean>? {
        return NetworkFactory.getApi().scanResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 路由停止扫描
     */
    fun routeStopScan(serid: String, scanSerId: Long): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routeStopScanDevcie(serid, scanSerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 路由停止扫描告诉服务器清除数据
     */
    fun routeScanClear(): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().tellServerClearScanning()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 路由批量分组
     */
    fun routeBatchGp(targetGroupMeshAddr: Int, deviceMeshAddrs: List<Int>, meshType: Int, ser_id: String): Observable<Response<RouterBatchGpBean>>? {
        return NetworkFactory.getApi().routerBatchGp(targetGroupMeshAddr, deviceMeshAddrs, meshType, ser_id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun routeBatchGpNew(bodyBean: GroupBodyBean): Observable<Response<RouterBatchGpBean>>? {
        return NetworkFactory.getApi().routerBatchGpN(bodyBean)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun routeBatchGpBlink(bodyBean: GroupBlinkBodyBean): Observable<Response<RouterBatchGpBean>>? {
        return NetworkFactory.getApi().routerBatchGpBlink(bodyBean)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 路由开始扫描
     */
    fun routerStartScan(scanType: Int, ser_id: String): Observable<Response<ScanDataBean>>? {
        return NetworkFactory.getApi().routeScanDevcie(scanType, Constant.DEFAULT_MESH_FACTORY_NAME, ser_id)
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

    /**
     * 路由删除群组
     */
    fun routerDelGp(body: RouterDelGpBody): Observable<RouterTimeoutBean>? {
        return NetworkFactory.getApi().routerDeleteGroup(body)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 路由添加场景
     */
    fun routeAddScene(bodyBean: SceneAddBodyBean): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routerAddScene(bodyBean)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 路由更新场景
     */
    fun routeUpdateScene(sceneId: Long, actions: List<DbSceneActions>): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routerUpdateScene(sceneId, actions, "updateScene")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 路由删除场景
     */
    fun routeDelScene(sceneActionId: Int): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routerDelScene(SceneIdBodyBean("delScene", sceneActionId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     *获取设备版本号
     */
    fun getDevicesVersion(meshAddrs: MutableList<Int>, meshType: Int): Observable<Response<RouterVersionsBean>>? {
        return NetworkFactory.getApi().routerGetDevicesVersion(meshAddrs, meshType, "getVersion")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     *获取设备版本号
     */
    fun toDevicesOTA(meshAddrs: MutableList<Int>, meshType: Int): Observable<Response<Any>>? {
        return NetworkFactory.getApi().routerToDevicesOta(meshAddrs, meshType, System.currentTimeMillis())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     *获取ota升级结果
     */
    fun routerOTAResult(page: Int, size: Int, time: Long): Observable<MutableList<RouterOTAResultBean>>? {
        return NetworkFactory.getApi().routerGetOTAResult(page, size, time)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     *停止路由ota
     */
    fun routerStopOTA(time: Long): Observable<RouterTimeoutBean>? {
        return NetworkFactory.getApi().routerStopOTA("router_ota", time)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        /**
         * 添加自定义渐变
         */
    }

    fun routerAddGradient(bean: AddGradientBean): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routerAddCustomGradient(bean)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 更新自定义渐变
     */
    fun routerUpdateGradient(bean: UpdateGradientBean): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routerUpdateCustomGradient(bean)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 删除自定义渐变
     */
    fun routerDelGradient(customBodyBean: DelGradientBodyBean): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routerDelCustomGradient(customBodyBean)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
    /**
     * 设备&组应用自定义渐变
     */
    fun routerApplyGradient(applyGradientBodyBean: ApplyGradientBodyBean): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routerApplyCustomGradient(applyGradientBodyBean)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 直连开关或传感器
     */
    fun routerConnectSwOrSe(id: Long, meshType: Int): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routerConnectSwOrSensor(id.toInt(), meshType, "connectSwOrse")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 配置普通开关
     */
    fun configNormalSw(id: Long, groupMeshAddr: Int): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().configNormalSw(id.toInt(), groupMeshAddr, "configNormalSw")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }


    /**
     * 配置双组开关
     */
    fun configDoubleSw(id: Long, groupMeshAddrs: List<Int>): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().configDoubleSw(id.toInt(), groupMeshAddrs, "configDoubleSw")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 配置场景开关
     */
    fun configSceneSw(id: Long, groupMeshAddrs: List<Int>): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().configSceneSw(id.toInt(), groupMeshAddrs, "configSceneSw")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 配置场景开关
     */
    fun configEightSw(id: Long, keys: List<KeyBean>): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().configEightSw(id.toInt(), keys, "configEightSw")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 控制相关开始
     * 开关灯
     */
    fun routeOpenOrClose(meshAddr: Int, meshType: Int, status: Int, ser_id: String): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routeOpenOrClose(meshAddr, meshType, status, ser_id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 控制相关开始
     * 调节亮度
     */
    fun routeConfigBrightness(meshAddr: Int, meshType: Int, brightness: Int, ser_id: String): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routeConfigBrightness(meshAddr, meshType, brightness, ser_id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 控制相关开始
     * 调节色温
     */
    fun routeConfigColorTemp(meshAddr: Int, meshType: Int, colorTemperature: Int, ser_id: String): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routeConfigColorTemp(meshAddr, meshType, colorTemperature, ser_id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 控制相关开始
     * 调节白色
     */
    fun routeConfigWhiteNum(meshAddr: Int, meshType: Int, colorTemperature: Int, ser_id: String): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routeConfigWhiteNum(meshAddr, meshType, colorTemperature, ser_id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 控制相关开始  int	1开 2关 (特别注意2才是关)
     * 开关缓起缓灭
     */
    fun routeSlowUpSlowDownSw(status: Int, ser_id: String): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routeSlowUpSlowDownSwitch(status, ser_id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 控制相关开始
     * 调节缓起缓灭速度
     */
    fun routeSlowUpSlowDownSpeed(speed: Int, ser_id: String): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routeSlowUpSlowDownSpeed(speed, ser_id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 控制相关开始
     * 恢复出厂设置  meshType	是	int	meshAddr类型  普通灯 = 4彩灯 = 6 蓝牙连接器 = 5
     * 开关 = 99 或 0x20 或 0x22 或 0x21 或 0x28 或 0x27 或 0x25 传感器 = 98 或 0x23 或 0x24 组 = 97 全部 = 100
     * 不支持窗帘  meshType=97&meshAddr=65535时效果与meshType=100一致
     */
    fun routeResetFactory(bodyBean: MacResetBody): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routeResetFactory(bodyBean)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 控制相关开始
     * 恢复出厂设置  meshType	是	int	meshAddr类型  普通灯 = 4彩灯 = 6 蓝牙连接器 = 5
     * 开关 = 99 或 0x20 或 0x22 或 0x21 或 0x28 或 0x27 或 0x25 传感器 = 98 或 0x23 或 0x24 组 = 97 全部 = 100
     * 不支持窗帘  meshType=97&meshAddr=65535时效果与meshType=100一致
     */
    fun routeUpdateLightName(id: Long, name: String): Observable<Response<Any>>? {
        return NetworkFactory.getApi().routeUpdateLight(id, name)
                // .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun routeUpdateRelayName(id: Long, name: String): Observable<Response<Any>>? {
        return NetworkFactory.getApi().routeUpdateLight(id, name)
                // .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun routeUpdateCurtainName(id: Long, name: String): Observable<Response<Any>>? {
        return NetworkFactory.getApi().routeUpdateLight(id, name)
                // .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun routeApplyScene(id: Long, ser_id: String): Observable<Response<RouterTimeoutBean>>? {
        return NetworkFactory.getApi().routeApplyScene(id, ser_id)
                // .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}




