package com.dadoutek.uled.group

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.RouteGetVerBean
import com.dadoutek.uled.base.RouterOTAFinishBean
import com.dadoutek.uled.base.RouterOTAingNumBean
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.Constants
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.dbModel.*
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.NetworkStatusCode
import com.dadoutek.uled.network.RouterOTAResultBean
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.RecoverMeshDeviceUtil
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.dadoutek.uled.widget.RecyclerGridDecoration
import com.polidea.rxandroidble2.scan.ScanSettings
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_group_ota_list.*
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2020/6/10 11:25
 * 描述
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GroupOTAListActivity : TelinkBaseActivity() {
    private var currentTime: Long = 0
    private var isStartOta: Boolean = false
    private var deviceType: Int = 0
    private var isGroup: Boolean = false
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private var disposableScan: Disposable? = null
    private var disposableTimerResfresh: Disposable? = null
    private var dispose: Disposable? = null
    private var dbGroup: DbGroup? = null
    private var lightList: MutableList<DbLight> = mutableListOf()
    private var switchList: MutableList<DbSwitch> = mutableListOf()
    private var sensorList: MutableList<DbSensor> = mutableListOf()
    private var curtainList: MutableList<DbCurtain> = mutableListOf()
    private var relayList: MutableList<DbConnector> = mutableListOf()
    private var meshAddrList: MutableList<Int> = mutableListOf()
    private var gwList: MutableList<DbGateway> = mutableListOf()
    private var lightAdaper = GroupOTALightAdapter(R.layout.group_ota_item, lightList)
    private var curtainAdaper = GroupOTACurtainAdapter(R.layout.group_ota_item, curtainList)
    private var relayAdaper = GroupOTARelayAdapter(R.layout.group_ota_item, relayList)
    private var switchAdaper = GroupOTASwitchAdapter(R.layout.group_ota_item, switchList)
    private var sensorAdaper = GroupOTASensorAdapter(R.layout.group_ota_item, sensorList)
    private var gwAdaper = GroupOTAGwAdapter(R.layout.group_ota_item, gwList)

    @SuppressLint("CheckResult")
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_ota_list)
        initView()
        initData()
        if (Constants.IS_ROUTE_MODE)
            getMostNewVersion()
        initListener()
    }

    private fun setPop() {
        hinitOne.text = getString(R.string.is_exit_ota)
        cancelf.setOnClickListener { popFinish?.dismiss() }
        confirmf.setOnClickListener {
            devicesStopOTA()
            popFinish?.dismiss()
        }
    }

    @SuppressLint("CheckResult")
    private fun getMostNewVersion() {
        RouterModel.getDevicesVersionNew(meshAddrList, deviceType, "gpOta")?.subscribe({
            LogUtils.v("zcl-----------收到路由请求版本申请-------$it")
            when (it.errorCode) {
                NetworkStatusCode.OK -> startGetVersionTimer(it.t.timeout.toLong())
                //全部选择的设备都未绑定路由，无法获取版本号, 请先去绑定路由 DEVICE_NOT_BINDROUTER 90008
                // 以下路由没有上线，无法删获取版本  ROUTER_ALL_OFFLINE= 90005
                NetworkStatusCode.DEVICE_NOT_BINDROUTER -> {
                    ToastUtils.showShort(getString(R.string.no_bind_router_cant_version))
                    finish()
                }
                NetworkStatusCode.ROUTER_ALL_OFFLINE -> {
                    ToastUtils.showShort(getString(R.string.router_offline))
                    finish()
                }
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
            finish()
        })
    }

    private fun startGetVersionTimer(t: Long) {
        showLoadingDialog(getString(R.string.please_wait))
        disposableRouteTimer?.dispose()
        disposableRouteTimer = Observable.timer(t+1L, TimeUnit.SECONDS)
                .subscribe {
                    LogUtils.v("zcl-----------执行路由获取版本超时-------")
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.get_version_fail))
                    //finish()
                }
    }

    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    override fun tzRouterUpdateVersionRecevice(routerVersion: RouteGetVerBean?) {
        LogUtils.v("zcl-----------收到路由得到版本的通知-------$routerVersion")
        disposableRouteTimer?.dispose()

        if (routerVersion?.finish == true) {
            hideLoadingDialog()
            SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, object : SyncCallback {
                override fun start() {}
                override fun complete() {
                    updataDevice()
                }

                override fun error(msg: String?) {
                    updataDevice()
                }
            })
        } else {
            ToastUtils.showShort(getString(R.string.get_version_success_num, routerVersion?.succeedTotal?.size))
        }
    }

    override fun tzRouteGetVersioningNum(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由gpOta通知-------$cmdBean")
        if (cmdBean.ser_id == "gpOta") {
            if (cmdBean.status == 0) {
                disposableRouteTimer?.dispose()
                startGetVersionTimer(cmdBean.timeout.toLong())
            }
        }
    }

    override fun tzRouteSendVersioning(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由gpOta上报通知-------$cmdBean")
        if (cmdBean.ser_id == "gpOta") {
            if (cmdBean.status != -1 && cmdBean.success) {
                val meshAddr = cmdBean.meshAddr
                when (deviceType) {
                    DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                        val device = DBUtils.getLightMeshAddr(meshAddr)
                        if (device.size > 0) {
                            device[0].version = cmdBean.version
                            DBUtils.saveLight(device[0], true)
                        }
                    }
                    DeviceType.SMART_CURTAIN -> {
                        val device = DBUtils.getCurtainMeshAddr(meshAddr)
                        if (device.size > 0) {
                            device[0].version = cmdBean.version
                            DBUtils.saveCurtain(device[0], true)
                        }
                    }
                    DeviceType.SMART_RELAY -> {
                        val device = DBUtils.getRelyByMeshAddr(meshAddr)
                        device?.version = cmdBean.version
                        device?.let {
                            DBUtils.saveConnector(device, true)
                        }
                    }
                    DeviceType.NORMAL_SWITCH -> {
                        val device = DBUtils.getSwitchByMeshAddr(meshAddr)
                        device?.version = cmdBean.version
                        device?.let {

                            DBUtils.saveSwitch(device, true)
                        }
                    }
                    DeviceType.SENSOR -> {
                        val device = DBUtils.getSensorByMeshAddr(meshAddr)
                        device?.version = cmdBean.version
                        device?.let {

                            DBUtils.saveSensor(device, true)
                        }
                    }
                    DeviceType.GATE_WAY -> {
                        val device = DBUtils.getGatewayByMeshAddr(meshAddr)
                        device?.version = cmdBean.version
                        device?.let {
                            DBUtils.saveGateWay(device, true)
                        }
                    }
                }
                updataDevice()
                if (cmdBean.finish) {
                    hideLoadingDialog()
                    disposableRouteTimer?.dispose()
                } else {
                    startGetVersionTimer(cmdBean.timeout.toLong())
                }
            }
        }
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun routerOtaDevice(meshList: MutableList<Int>) {
        val time = System.currentTimeMillis()
        startGetStatus(false)
        RouterModel.toDevicesOTA(meshList, deviceType, time)?.subscribe({
            LogUtils.v("zcl-----------收到路由升级请求---deviceMeshAddress$meshList---time$time-------deviceTye${deviceType}----$it")
            isStartOta = false
            when (it.errorCode) {
                0 -> {
                    ota_progress.visibility = View.VISIBLE
                    currentTime = time
                    SharedPreferencesUtils.setLastOtaTime(currentTime)
                    isStartOta = true
                    btn_gp_ota_start.text = getString(R.string.otaing)
                    group_ota_number_ly.visibility = View.VISIBLE
                    group_ota_all.text = getString(R.string.ota_all_num) + meshList.size
                    group_ota_success.text = getString(R.string.ota_success_num) + "0"
                    group_ota_fail.text = getString(R.string.ota_fail_num) + "0"
                    btn_gp_ota_start.isClickable = false
                    ToastUtils.showShort(getString(R.string.ota_update_title))
                } //比如扫描时杀掉APP后恢复至扫描页面，OTA时杀掉APP后恢复至OTA等待
                90998 -> {//扫描中不能OTA，请稍后。请尝试获取路由模式下状态以恢复上次扫描
                    goScanning()
                }
                90999 -> {//OTA中，不能再次进行OTA。请尝试获取路由模式下状态以恢复上次OTA
                    ToastUtils.showShort(getString(R.string.ota_update_title))
                }
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("SetTextI18n", "CheckResult")
    private fun startGetStatus(isFinish: Boolean) {
        RouterModel.routerOTAResult(1, 5000, currentTime)?.subscribe({
            //status	int	ota结果。-1失败 0成功 1升级中 2已停止 3处理中
            LogUtils.v("zcl-----------获取路由状态----$currentTime---$it")
            val filterSuccess = it.filter { item -> item.status == 0 }
            val filterFail = it.filter { item -> item.status == -1 }
            group_ota_success.text = getString(R.string.ota_success_num) + filterSuccess.size
            group_ota_fail.text = getString(R.string.ota_fail_num) + filterFail.size
            filterSuccess.forEach { it1 ->
                setMostNew(it1)
            }
            if (isFinish) {
                ota_progress.visibility = View.GONE
                isStartOta = false
                ToastUtils.showShort(getString(R.string.ota_finish))
                btn_gp_ota_start.text = getString(R.string.start_update)
                btn_gp_ota_start.isClickable = true
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    private fun devicesStopOTA() {
        RouterModel.routerStopOTA(currentTime, "router_ota")?.subscribe({
            LogUtils.v("zcl-----------收到路由停止升级请求---time$currentTime----$it")
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.ota_stop_fail))
                            }
                }
                90023 -> ToastUtils.showShort(getString(R.string.startTime_not_exit))
                90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                90018 -> ToastUtils.showShort(getString(R.string.device_not_exit))
                90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    override fun tzRouterOTAStopRecevice(routerOTAFinishBean: RouterOTAFinishBean?) {
        LogUtils.v("zcl-----------收到路由ota停止通知-------$routerOTAFinishBean")
        if (routerOTAFinishBean?.finish == true) {
            if (routerOTAFinishBean.ser_id == "router_ota") {
                if (routerOTAFinishBean.status == 0) {
                    disposableRouteTimer?.dispose()
                    finish()
                }
            }
        }
    }

    private fun setMostNew(it: RouterOTAResultBean) {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                val filter = lightList.filter { it1 -> it1.isSupportOta && !it1.isMostNew }
                filter.forEach { it2 ->
                    if (it.macAddr == it2.macAddr) {
                        it2.isMostNew = true
                        return@forEach
                    }
                }
                lightAdaper.notifyDataSetChanged()
            }
            DeviceType.NORMAL_SWITCH -> {
                val filter = lightList.filter { it1 -> it1.isSupportOta && !it1.isMostNew }
                filter.forEach { it2 ->
                    if (it.macAddr == it2.macAddr) {
                        it2.isMostNew = true
                        return@forEach
                    }
                }
                switchAdaper.notifyDataSetChanged()
            }
            DeviceType.SENSOR -> {
                val filter = lightList.filter { it1 -> it1.isSupportOta && !it1.isMostNew }
                filter.forEach { it2 ->
                    if (it.macAddr == it2.macAddr) {
                        it2.isMostNew = true
                        return@forEach
                    }
                }
                sensorAdaper.notifyDataSetChanged()
            }
            DeviceType.SMART_CURTAIN -> {
                val filter = lightList.filter { it1 -> it1.isSupportOta && !it1.isMostNew }
                filter.forEach { it2 ->
                    if (it.macAddr == it2.macAddr) {
                        it2.isMostNew = true
                        return@forEach
                    }
                }
                curtainAdaper.notifyDataSetChanged()
            }
            DeviceType.SMART_RELAY -> {
                val filter = lightList.filter { it1 -> it1.isSupportOta && !it1.isMostNew }
                filter.forEach { it2 ->
                    if (it.macAddr == it2.macAddr) {
                        it2.isMostNew = true
                        return@forEach
                    }
                }
                relayAdaper.notifyDataSetChanged()
            }
            DeviceType.GATE_WAY -> {
                val filter = lightList.filter { it1 -> it1.isSupportOta && !it1.isMostNew }
                filter.forEach { it2 ->
                    if (it.macAddr == it2.macAddr) {
                        it2.isMostNew = true
                        return@forEach
                    }
                }
                gwAdaper.notifyDataSetChanged()
            }
        }
    }

    override fun tzRouterOTAingNumRecevice(routerOTAingNumBean: RouterOTAingNumBean?) {
        //升级中通知
        LogUtils.v("zcl-----------收到路由ota通知-------$routerOTAingNumBean")
        disposableRouteTimer?.dispose()
        hideLoadingDialog()
        val status = routerOTAingNumBean?.status
        val otaResult = routerOTAingNumBean?.otaResult
        startGetStatus(routerOTAingNumBean?.finish == true)

//        if (status == 0 && deviceMac == otaResult?.macAddr && otaResult?.failedCode == -1)
//            afterOtaSuccess()
//        else
//            if (status == -1 || status == 1)
//                afterOtaFail()
    }


    private fun setRefresh() {
        if (Constants.IS_ROUTE_MODE) {
            ota_swipe_refresh_ly.isRefreshing = false
            return
        }

        loading_tansform.visibility = View.VISIBLE
        findMeshDevice(DBUtils.lastUser?.controlMeshName)
        disposableTimerResfresh?.dispose()
        disposableTimerResfresh = Observable.timer(4000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    loading_tansform.visibility = View.GONE
                    ota_swipe_refresh_ly.isRefreshing = false
                    disposableScan?.dispose()
                }
        compositeDisposable.add(disposableTimerResfresh!!)
    }

    @SuppressLint("CheckResult")
    fun findMeshDevice(deviceName: String?) {
        val scanFilter = com.polidea.rxandroidble2.scan.ScanFilter.Builder().setDeviceName(deviceName).build()
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        LogUtils.d("findMeshDevice name = $deviceName")
        disposableScan?.dispose()
        disposableScan = RecoverMeshDeviceUtil.rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .observeOn(Schedulers.io())
                .map { RecoverMeshDeviceUtil.parseData(it) }          //解析数据
                .timeout(RecoverMeshDeviceUtil.SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS) {
                    LogUtils.d("findMeshDevice name complete.")
                    when (deviceType) {
                        DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                            lightList.sortBy { it1 -> it1.rssi }
                            supportAndUNLight()
                            lightAdaper.notifyDataSetChanged()
                        }
                        DeviceType.NORMAL_SWITCH -> {
                            switchList.sortBy { it1 -> it1.rssi }
                            supportAndUNSwitch()
                            switchAdaper.notifyDataSetChanged()
                        }
                        DeviceType.SENSOR -> {
                            sensorList.sortBy { it1 -> it1.rssi }
                            supportAndUNSensor()
                            sensorAdaper.notifyDataSetChanged()
                        }
                        DeviceType.SMART_CURTAIN -> {
                            curtainList.sortBy { it1 -> it1.rssi }
                            supportAndUNCurtain()
                            curtainAdaper.notifyDataSetChanged()
                        }
                        DeviceType.SMART_RELAY -> {
                            relayList.sortBy { it1 -> it1.rssi }
                            supportAndUNConnector()
                            relayAdaper.notifyDataSetChanged()
                        }
                        DeviceType.GATE_WAY -> {
                            gwList.sortBy { it1 -> it1.rssi }
                            supportAndUNGateway()
                            gwAdaper.notifyDataSetChanged()
                        }
                    }
                    it.onComplete()                     //如果过了指定时间，还搜不到缺少的设备，就完成
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it != null)
                        refreshRssi(it)
                }, {})
        compositeDisposable?.add(disposableScan!!)

    }

    private fun refreshRssi(deviceInfo: DeviceInfo) {
        LogUtils.v("zcl信号$deviceInfo")
        GlobalScope.launch(Dispatchers.Main) {
            if (deviceInfo.productUUID == deviceType) {
                var deviceChangeL: DbLight? = null
                var deviceChangeSw: DbSwitch? = null
                var deviceChangeSensor: DbSensor? = null
                var deviceChangeC: DbCurtain? = null
                var deviceChangeR: DbConnector? = null
                var deviceChangeGw: DbGateway? = null

                when (deviceType) {
                    DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                        for (device in lightList) {
                            if (device.meshAddr == deviceInfo.meshAddress) {
                                device.rssi = deviceInfo.rssi
                                deviceChangeL = device
                                LogUtils.v("zcl设备信号$deviceInfo----------------$deviceChangeL")
                            }
                        }
                        if (null != deviceChangeL) {
                            lightList.remove(deviceChangeL)
                            lightList.add(deviceChangeL)
                        }
                    }
                    DeviceType.NORMAL_SWITCH -> {
                        for (device in switchList) {
                            if (device.macAddr == deviceInfo.macAddress) {
                                device.rssi = deviceInfo.rssi
                                deviceChangeSw = device
                                LogUtils.v("zcl设备信号$deviceInfo----------------$deviceChangeSw")
                            }
                        }
                        if (null != deviceChangeSw) {
                            switchList.remove(deviceChangeSw)
                            switchList.add(deviceChangeSw)
                        }
                    }
                    DeviceType.SENSOR -> {
                        for (device in sensorList) {
                            if (device.macAddr == deviceInfo.macAddress) {
                                device.rssi = deviceInfo.rssi
                                deviceChangeSensor = device
                                LogUtils.v("zcl设备信号$deviceInfo----------------$deviceChangeSensor")
                            }
                        }
                        if (null != deviceChangeSw) {
                            sensorList.remove(deviceChangeSensor)
                            sensorList.add(deviceChangeSensor!!)
                        }
                    }
                    DeviceType.SMART_CURTAIN -> {
                        for (device in curtainList) {
                            if (device.macAddr == deviceInfo.macAddress) {
                                device.rssi = deviceInfo.rssi
                                deviceChangeC = device
                                LogUtils.v("zcl设备信号$deviceInfo----------------$deviceChangeC")
                            }
                        }
                        if (null != deviceChangeC) {
                            curtainList.remove(deviceChangeC)
                            curtainList.add(deviceChangeC)
                        }
                    }
                    DeviceType.SMART_RELAY -> {
                        for (device in relayList) {
                            if (device.macAddr == deviceInfo.macAddress) {
                                device.rssi = deviceInfo.rssi
                                deviceChangeR = device
                                LogUtils.v("zcl设备信号$deviceInfo----------------$deviceChangeR")
                            }
                        }
                        if (null != deviceChangeL) {
                            relayList.remove(deviceChangeR)
                            relayList.add(deviceChangeR!!)
                        }
                    }
                    DeviceType.GATE_WAY -> {
                        for (device in gwList) {
                            if (device.macAddr == deviceInfo.macAddress) {
                                device.rssi = deviceInfo.rssi
                                deviceChangeGw = device
                                LogUtils.v("zcl设备信号$deviceInfo----------------$deviceChangeGw")
                            }
                        }
                        if (null != deviceChangeL) {
                            gwList.remove(deviceChangeGw)
                            gwList.add(deviceChangeGw!!)
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun initData() {
        var emptyView = View.inflate(this, R.layout.empty_view, null)
        val addBtn = emptyView.findViewById<Button>(R.id.add_device_btn)
        addBtn.visibility = View.INVISIBLE
        getIntentData()
        setAllAdapter(emptyView)
        updataDevice()
    }

    private fun setAllAdapter(emptyView: View?) {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                template_recycleView.adapter = lightAdaper
                lightAdaper.setDeviceType(deviceType == DeviceType.LIGHT_RGB)
                lightAdaper.bindToRecyclerView(template_recycleView)
                lightAdaper.emptyView = emptyView
            }
            DeviceType.SMART_CURTAIN -> {
                template_recycleView.adapter = curtainAdaper
                curtainAdaper.bindToRecyclerView(template_recycleView)
                curtainAdaper.emptyView = emptyView
            }
            DeviceType.SMART_RELAY -> {
                template_recycleView.adapter = relayAdaper
                relayAdaper.bindToRecyclerView(template_recycleView)
                relayAdaper.emptyView = emptyView
            }
            DeviceType.NORMAL_SWITCH -> {
                template_recycleView.adapter = switchAdaper
                switchAdaper.bindToRecyclerView(template_recycleView)
                switchAdaper.emptyView = emptyView
            }
            DeviceType.SENSOR -> {
                template_recycleView.adapter = sensorAdaper
                sensorAdaper.bindToRecyclerView(template_recycleView)
                sensorAdaper.emptyView = emptyView
            }
            DeviceType.GATE_WAY -> {
                template_recycleView.adapter = gwAdaper
                gwAdaper.bindToRecyclerView(template_recycleView)
                gwAdaper.emptyView = emptyView
            }
        }
    }

    private fun getIntentData() {
        val serializableExtra = intent.getSerializableExtra("group")//如果不是群组不传dbgroup
        if (serializableExtra != null)
            dbGroup = serializableExtra as DbGroup
        deviceType = intent.getIntExtra("DeviceType", 0)
        isGroup = dbGroup != null
        currentTime = SharedPreferencesUtils.getLastOtaTime()
    }

    private fun setRelayData() {
        getRelayData()
        relayAdaper.onItemClickListener = onItemClickListener
        supportAndUNConnector()
    }

    private fun getRelayData() {
        relayList.clear()
        if (isGroup) relayList.addAll(DBUtils.getRelayByGroupID(dbGroup!!.id)) else relayList.addAll(DBUtils.allRely)
        relayList.forEach { meshAddrList.add(it.meshAddr) }
    }

    private fun setCurtainData() {
        getCurtainData()
        curtainAdaper.onItemClickListener = onItemClickListener
        supportAndUNCurtain()
        curtainAdaper.notifyDataSetChanged()
    }

    private fun getCurtainData() {
        curtainList.clear()
        if (isGroup)
            curtainList.addAll(DBUtils.getCurtainByGroupID(dbGroup!!.id)) else curtainList.addAll(DBUtils.allCurtain)
        curtainList.forEach { meshAddrList.add(it.meshAddr) }
    }

    private fun setLightData() {
        getLightData()
        lightAdaper.onItemClickListener = onItemClickListener
        supportAndUNLight()
        lightAdaper.notifyDataSetChanged()
    }

    private fun getLightData() {
        lightList.clear()
        if (isGroup)
            lightList.addAll(DBUtils.getLightByGroupID(dbGroup!!.id))
        else when (deviceType) {
            DeviceType.LIGHT_NORMAL -> lightList.addAll(DBUtils.getAllNormalLight())
            DeviceType.LIGHT_RGB -> lightList.addAll(DBUtils.getAllRGBLight())
        }
        lightList.forEach { meshAddrList.add(it.meshAddr) }
    }

    private fun setSwtichData() {
        getSwtichData()
        switchAdaper.onItemClickListener = onItemClickListener
        supportAndUNSwitch()
        switchAdaper.notifyDataSetChanged()
    }

    private fun getSwtichData() {
        switchList.clear()
        if (isGroup) switchList else switchList.addAll(DBUtils.getAllSwitch())
        switchList.forEach { meshAddrList.add(it.meshAddr) }
    }

    private fun setGwData() {
        getGwData()
        gwAdaper.onItemClickListener = onItemClickListener
        supportAndUNGateway()
        gwAdaper.notifyDataSetChanged()
    }

    private fun getGwData() {
        gwList.clear()
        if (isGroup) gwList else gwList.addAll(DBUtils.getAllGateWay())
        gwList.forEach { meshAddrList.add(it.meshAddr) }
    }

    private fun setSensorData() {
        getSensorData()
        sensorAdaper.onItemClickListener = onItemClickListener
        supportAndUNSensor()
        sensorAdaper.notifyDataSetChanged()
    }

    private fun getSensorData() {
        sensorList.clear()
        if (isGroup) sensorList else sensorList.addAll(DBUtils.getAllSensor())
        sensorList.forEach { meshAddrList.add(it.meshAddr) }
    }

    private fun supportAndUNLight() {
        lightList.forEach {
            it.version?.let { itv ->
                // it.isSupportOta&&!it.isMostNew= OtaPrepareUtils.instance().checkSupportOta(itv)
                val suportOta = isSuportOta(itv)
                val mostNew = isMostNew(itv)
                it.isSupportOta = suportOta
                it.isMostNew = mostNew
            }
        }

        var unsupport = lightList.filter { !it.isSupportOta || it.isMostNew }
        lightList.removeAll(unsupport)
        lightList.addAll(unsupport)
    }

    private fun supportAndUNSwitch() {
        switchList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = isSuportOta(itv)
                it.isMostNew = isMostNew(itv)
            }
        }

        var unsupport = switchList.filter { !it.isSupportOta || it.isMostNew }
        switchList.removeAll(unsupport)
        switchList.addAll(unsupport)
    }

    private fun supportAndUNSensor() {
        sensorList.forEach {
            it.version?.let { itv ->
                // it.isSupportOta&&!it.isMostNew= OtaPrepareUtils.instance().checkSupportOta(itv)
                it.isSupportOta = isSuportOta(itv)
                it.isMostNew = isMostNew(itv)
            }
        }

        var unsupport = sensorList.filter {
            !it.isSupportOta || it.isMostNew
        }
        sensorList.removeAll(unsupport)
        sensorList.addAll(unsupport)
    }

    private fun supportAndUNCurtain() {
        curtainList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = isSuportOta(itv)
                it.isMostNew = isMostNew(itv)
            }
        }

        var unsupport = curtainList.filter {
            !it.isSupportOta || it.isMostNew
        }
        curtainList.removeAll(unsupport)
        curtainList.addAll(unsupport)
    }

    private fun supportAndUNConnector() {
        relayList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = isSuportOta(itv)
                it.isMostNew = isMostNew(itv)
            }
        }

        var unsupport = relayList.filter { !it.isSupportOta || it.isMostNew }
        relayList.removeAll(unsupport)
        relayList.addAll(unsupport)
        relayAdaper.notifyDataSetChanged()
    }

    private fun supportAndUNGateway() {
        lightList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = isSuportOta(itv)
                it.isMostNew = isMostNew(itv)
            }
        }

        var unsupport = lightList.filter { !it.isSupportOta || it.isMostNew }
        lightList.removeAll(unsupport)
        lightList.addAll(unsupport)
    }

    private fun initView() {
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            isFinish()
        }
        toolbarTv.text = getString(R.string.group_ota)
        template_recycleView.layoutManager = GridLayoutManager(this, 2)/*LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)*/
        template_recycleView.addItemDecoration(RecyclerGridDecoration(this, 2))
        when {
            Constants.IS_ROUTE_MODE -> {
                group_ota_number_ly.visibility = View.GONE
                btn_gp_ota_start.visibility = View.VISIBLE
            }
            else -> {
                group_ota_number_ly.visibility = View.GONE
                btn_gp_ota_start.visibility = View.GONE
            }
        }
        isStartOta = false
        setPop()

        //设置进度View下拉的起始点和结束点，scale 是指设置是否需要放大或者缩小动画
        ota_swipe_refresh_ly.setProgressViewOffset(true, -0, 500)
        //设置进度View下拉的结束点，scale 是指设置是否需要放大或者缩小动画
        ota_swipe_refresh_ly.setProgressViewEndTarget(true, 360)
        //设置进度View的组合颜色，在手指上下滑时使用第一个颜色，在刷新中，会一个个颜色进行切换
        ota_swipe_refresh_ly.setColorSchemeColors(Color.BLACK, Color.GREEN, Color.RED, Color.YELLOW, Color.BLUE)
        //设置触发刷新的距离
        ota_swipe_refresh_ly.setDistanceToTriggerSync(200)
        //是否禁止下拉刷新
        ota_swipe_refresh_ly.isEnabled = !Constants.IS_ROUTE_MODE
    }

    private fun isFinish() {
        if (isStartOta)
            popFinish.showAtLocation(window.decorView.rootView, Gravity.CENTER, 0, 0)
        else
            finish()
    }

    private fun initListener() {
        loading_tansform.setOnClickListener { }
        ota_swipe_refresh_ly.setOnRefreshListener {
            setRefresh()
        }
        btn_gp_ota_start.setOnClickListener {
            if (!isStartOta)
                routerStartOrStop()
            else
                ToastUtils.showShort(getString(R.string.otaing))
        }
    }

    private fun routerStartOrStop() {
        val meshList = mutableListOf<Int>()
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                val filter = lightList.filter { it.productUUID == deviceType }.filter { it.isSupportOta && !it.isMostNew }
                filter.forEach {
                    meshList.add(it.meshAddr)
                }
            }
            DeviceType.SMART_CURTAIN -> {
                val filter = curtainList.filter { it.productUUID == deviceType }.filter { it.isSupportOta && !it.isMostNew }
                filter.forEach {
                    meshList.add(it.meshAddr)
                }
            }
            DeviceType.SMART_RELAY -> {
                val filter = relayList.filter { it.productUUID == deviceType }.filter { it.isSupportOta && !it.isMostNew }
                filter.forEach {
                    meshList.add(it.meshAddr)
                }
            }
            DeviceType.NORMAL_SWITCH -> {

                val filter = switchList.filter { it.productUUID == deviceType }.filter { it.isSupportOta && !it.isMostNew }
                filter.forEach {
                    meshList.add(it.meshAddr)
                }
            }
            DeviceType.SENSOR -> {
                if (Constants.IS_ROUTE_MODE) {
                    val filter = sensorList.filter { it.productUUID == deviceType }.filter { it.isSupportOta && !it.isMostNew }
                    filter.forEach {
                        meshList.add(it.meshAddr)
                    }
                }
            }
            DeviceType.GATE_WAY -> {
                val filter = gwList.filter { it.productUUID == deviceType }.filter { it.isSupportOta && !it.isMostNew }
                filter.forEach {
                    meshList.add(it.meshAddr)
                }
            }
        }
        if (!isStartOta)
            routerOtaDevice(meshList)
    }

    private fun updataDevice() {
        meshAddrList.clear()
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> setLightData()
            DeviceType.SMART_CURTAIN -> setCurtainData()
            DeviceType.SMART_RELAY -> setRelayData()
            DeviceType.NORMAL_SWITCH -> setSwtichData()
            DeviceType.SENSOR -> setSensorData()
            DeviceType.GATE_WAY -> setGwData()
        }
    }

    @SuppressLint("CheckResult")
    private fun goScanning() {
        ToastUtils.showShort(getString(R.string.sanning_to_scan_activity))
        Observable.timer(2000, TimeUnit.MILLISECONDS).subscribe {
            startActivity(Intent(this@GroupOTAListActivity, DeviceScanningNewActivity::class.java))
            finish()
        }
    }

    val onItemClickListener = BaseQuickAdapter.OnItemClickListener { _, _, position ->
        if (Constants.IS_ROUTE_MODE)
            return@OnItemClickListener
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> bleOTALight(lightList[position])
            DeviceType.SMART_CURTAIN -> bleOTACurtain(curtainList[position])
            DeviceType.SMART_RELAY -> bleOTARelay(relayList[position])
            DeviceType.NORMAL_SWITCH -> bleOTASwitch(switchList[position])
            DeviceType.SENSOR -> bleOTASensor(sensorList[position])
            DeviceType.GATE_WAY -> bleOTAGw(gwList[position])
        }
    }

    private fun bleOTAGw(dbgw: DbGateway) {
        when {
            dbgw.isSupportOta -> when {
                dbgw.isMostNew -> ToastUtils.showShort(getString(R.string.the_last_version))
                TextUtils.isEmpty(dbgw.version) -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    when {
                        TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbgw.meshAddr -> {
                            getDeviceVersionGw(dbgw)
                        }
                        else -> {
                            TelinkLightService.Instance()?.idleMode(true)
                            Thread.sleep(500)
                            connect(macAddress = dbgw.macAddr, meshAddress = dbgw.meshAddr, connectTimeOutTime = 15)
                                    ?.subscribeOn(Schedulers.io())
                                    ?.observeOn(AndroidSchedulers.mainThread())
                                    ?.subscribe({
                                        hideLoadingDialog()
                                        getDeviceVersionGw(dbgw)
                                    }, {
                                        hideLoadingDialog()
                                        runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                                        LogUtils.d(it)
                                    })
                        }
                    }
                }
                else -> getFilePath(dbgw.meshAddr, dbgw.macAddr, dbgw.version, DeviceType.GATE_WAY)
            }
            else -> ToastUtils.showShort(getString(R.string.dissupport_ota))
        }
    }

    private fun bleOTASensor(dbsensor: DbSensor) {
        when {
            dbsensor.isSupportOta -> when {
                dbsensor.isMostNew -> ToastUtils.showShort(getString(R.string.the_last_version))
                TextUtils.isEmpty(dbsensor.version) -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    when {
                        TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbsensor.meshAddr -> {
                            getDeviceVersionSensor(dbsensor)
                        }
                        else -> {
                            showLoadingDialog(getString(R.string.please_wait))
                            TelinkLightService.Instance()?.idleMode(true)
                            Thread.sleep(500)
                            connect(macAddress = dbsensor.macAddr, meshAddress = dbsensor.meshAddr, connectTimeOutTime = 15)
                                    ?.subscribeOn(Schedulers.io())
                                    ?.observeOn(AndroidSchedulers.mainThread())
                                    ?.subscribe({
                                        hideLoadingDialog()
                                        getDeviceVersionSensor(dbsensor)
                                    }, {
                                        hideLoadingDialog()
                                        runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                                        LogUtils.d(it)
                                    })
                        }
                    }

                }
                else -> getFilePath(dbsensor.meshAddr, dbsensor.macAddr, dbsensor.version, dbsensor.productUUID)
            }
            else -> ToastUtils.showShort(getString(R.string.dissupport_ota))
        }
    }

    private fun bleOTASwitch(dbsw: DbSwitch) {
        when {
            dbsw.isMostNew -> ToastUtils.showShort(getString(R.string.the_last_version))
            dbsw.isSupportOta -> when {
                TextUtils.isEmpty(dbsw.version) -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    when {
                        TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbsw.meshAddr -> {
                            getDeviceVersionSwitch(dbsw)
                        }
                        else -> {
                            showLoadingDialog(getString(R.string.please_wait))
                            TelinkLightService.Instance()?.idleMode(true)
                            Thread.sleep(500)
                            connect(macAddress = dbsw.macAddr, meshAddress = dbsw.meshAddr, connectTimeOutTime = 12, retryTimes = 2)
                                    ?.subscribeOn(Schedulers.io())
                                    ?.observeOn(AndroidSchedulers.mainThread())
                                    ?.subscribe({
                                        hideLoadingDialog()
                                        getDeviceVersionSwitch(dbsw)
                                    }, {
                                        hideLoadingDialog()
                                        runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                                    })
                        }
                    }
                }
                else -> getFilePath(dbsw.meshAddr, dbsw.macAddr, dbsw.version, dbsw.productUUID)
            }
            else -> ToastUtils.showShort(getString(R.string.dissupport_ota))
        }
    }

    private fun bleOTARelay(dbrelay: DbConnector) {
        when {
            dbrelay.isSupportOta -> when {
                dbrelay.isMostNew -> ToastUtils.showShort(getString(R.string.the_last_version))
                TextUtils.isEmpty(dbrelay.version) -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    when {
                        TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbrelay.meshAddr -> {
                            getDeviceVersionConnector(dbrelay)
                        }
                        else -> {
                            showLoadingDialog(getString(R.string.please_wait))
                            val idleMode = TelinkLightService.Instance()?.idleMode(true)
                            Thread.sleep(500)
                            connect(macAddress = dbrelay.macAddr, meshAddress = dbrelay.meshAddr, connectTimeOutTime = 15)
                                    ?.subscribeOn(Schedulers.io())
                                    ?.observeOn(AndroidSchedulers.mainThread())
                                    ?.subscribe({
                                        hideLoadingDialog()
                                        getDeviceVersionConnector(dbrelay)
                                    }, {
                                        hideLoadingDialog()
                                        runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                                    })
                        }
                    }
                }
                else -> getFilePath(dbrelay.meshAddr, dbrelay.macAddr, dbrelay.version, dbrelay.productUUID)
            }
            else -> ToastUtils.showShort(getString(R.string.dissupport_ota))
        }
    }

    private fun bleOTACurtain(dbCurtain: DbCurtain) {
        when {
            dbCurtain.isSupportOta -> when {
                dbCurtain.isMostNew -> ToastUtils.showShort(getString(R.string.the_last_version))
                TextUtils.isEmpty(dbCurtain.version) -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    when {
                        TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbCurtain.meshAddr -> {
                            getDeviceVersionCurtain(dbCurtain)
                        }
                        else -> {
                            showLoadingDialog(getString(R.string.please_wait))
                            val idleMode = TelinkLightService.Instance()?.idleMode(true)
                            Thread.sleep(500)
                            connect(macAddress = dbCurtain.macAddr, meshAddress = dbCurtain.meshAddr, connectTimeOutTime = 15)
                                    ?.subscribeOn(Schedulers.io())
                                    ?.observeOn(AndroidSchedulers.mainThread())
                                    ?.subscribe({
                                        hideLoadingDialog()
                                        getDeviceVersionCurtain(dbCurtain)
                                    }, {
                                        hideLoadingDialog()
                                        runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                                    })
                        }
                    }
                }
                else -> getFilePath(dbCurtain.meshAddr, dbCurtain.macAddr, dbCurtain.version, dbCurtain.productUUID)
            }
            else -> ToastUtils.showShort(getString(R.string.dissupport_ota))
        }
    }

    @SuppressLint("CheckResult")
    private fun bleOTALight(dbLight: DbLight) {
        when {
            Constants.IS_ROUTE_MODE -> routerStartOrStop()
            dbLight.isMostNew -> ToastUtils.showShort(getString(R.string.the_last_version))
            dbLight.isSupportOta -> {
                when {
                    TextUtils.isEmpty(dbLight.version) -> {
                        showLoadingDialog(getString(R.string.please_wait))
                        when {
                            TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbLight.meshAddr || Constants.IS_ROUTE_MODE -> {
                                getDeviceVersionLight(dbLight)
                            }
                            else -> {
                                showLoadingDialog(getString(R.string.please_wait))
                                val idleMode = TelinkLightService.Instance()?.idleMode(true)
                                Thread.sleep(500)
                                connect(macAddress = dbLight.macAddr, meshAddress = dbLight.meshAddr, connectTimeOutTime = 15)
                                        ?.subscribeOn(Schedulers.io())
                                        ?.observeOn(AndroidSchedulers.mainThread())
                                        ?.subscribe({
                                            hideLoadingDialog()
                                            getDeviceVersionLight(dbLight)
                                        }, {
                                            hideLoadingDialog()
                                            runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                                        })
                            }
                        }
                    }
                    else -> getFilePath(dbLight.meshAddr, dbLight.macAddr, dbLight.version, dbLight.productUUID)
                }
            }
            else -> ToastUtils.showShort(getString(R.string.dissupport_ota))
        }
    }

    override fun onResume() {
        super.onResume()
        updataDevice()
    }

    @SuppressLint("CheckResult")
    private fun getDeviceVersionLight(dbLight: DbLight) {
        if (Constants.IS_ROUTE_MODE)

            Commander.getDeviceVersion(dbLight.meshAddr).subscribe(
                    { s: String ->
                        dbLight!!.version = s
                        DBUtils.saveLight(dbLight!!, true)
                        setLightData()
                        hideLoadingDialog()
                        ToastUtils.showShort(getString(R.string.get_version_success))
                    }, {
                hideLoadingDialog()
                ToastUtils.showLong(getString(R.string.get_version_fail))
            })
    }

    private fun getDeviceVersionCurtain(dbLight: DbCurtain) {
        val dispos = Commander.getDeviceVersion(dbLight.meshAddr).subscribe(
                { s: String ->
                    dbLight!!.version = s
                    DBUtils.saveCurtain(dbLight!!, true)
                    setLightData()
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.get_version_success))
                }, {
            hideLoadingDialog()
            ToastUtils.showLong(getString(R.string.get_version_fail))
        })
    }

    private fun getDeviceVersionConnector(dbLight: DbConnector) {
        val dispos = Commander.getDeviceVersion(dbLight.meshAddr).subscribe(
                { s: String ->
                    dbLight!!.version = s
                    DBUtils.saveConnector(dbLight!!, true)
                    setLightData()
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.get_version_success))
                }, {
            hideLoadingDialog()
            ToastUtils.showLong(getString(R.string.get_version_fail))
        })
    }

    private fun getDeviceVersionSwitch(dbLight: DbSwitch) {
        val dispos = Commander.getDeviceVersion(dbLight.meshAddr).subscribe({ s: String ->
            dbLight!!.version = s
            DBUtils.saveSwitch(dbLight!!, true)
            setLightData()
            hideLoadingDialog()
            ToastUtils.showShort(getString(R.string.get_version_success))
        }, {
            hideLoadingDialog()
            ToastUtils.showLong(getString(R.string.get_version_fail))
        })
    }

    private fun getDeviceVersionSensor(dbLight: DbSensor) {
        val dispos = Commander.getDeviceVersion(dbLight.meshAddr).subscribe(
                { s: String ->
                    dbLight!!.version = s
                    DBUtils.saveSensor(dbLight!!, true)
                    setLightData()
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.get_version_success))
                }, {
            hideLoadingDialog()
            ToastUtils.showLong(getString(R.string.get_version_fail))
        })
    }

    private fun getDeviceVersionGw(dbLight: DbGateway) {
        val dispos = Commander.getDeviceVersion(dbLight.meshAddr).subscribe(
                { s: String ->
                    dbLight!!.version = s
                    DBUtils.saveGateWay(dbLight!!, true)
                    setLightData()
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.get_version_success))
                }, {
            hideLoadingDialog()
            ToastUtils.showLong(getString(R.string.get_version_fail))
        })
    }

    private fun getFilePath(meshAddr: Int, macAddr: String, version: String, deviceType: Int) {
        OtaPrepareUtils.instance().gotoUpdateView(this@GroupOTAListActivity, version, object : OtaPrepareListner {

            override fun downLoadFileStart() {
                showLoadingDialog(getString(R.string.get_update_file))
            }

            override fun startGetVersion() {
                showLoadingDialog(getString(R.string.verification_version))
            }

            override fun getVersionSuccess(s: String) {
                hideLoadingDialog()
            }

            override fun getVersionFail() {
                ToastUtils.showLong(R.string.verification_version_fail)
                hideLoadingDialog()
            }


            @SuppressLint("CheckResult")
            override fun downLoadFileSuccess() {
                hideLoadingDialog()
                if ((TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == meshAddr)) {
                    startOtaAct(meshAddr, macAddr, version, deviceType)
                } else {
                    showLoadingDialog(getString(R.string.please_wait))
                    val idleMode = TelinkLightService.Instance()?.idleMode(true)
                    connect(macAddress = macAddr, meshAddress = meshAddr, fastestMode = true, connectTimeOutTime = 25)?.subscribe({
                        hideLoadingDialog()
                        startOtaAct(meshAddr, macAddr, version, deviceType)
                    }, {
                        hideLoadingDialog()
                        runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                        LogUtils.d(it)
                    })
                }

            }

            override fun downLoadFileFail(message: String) {
                hideLoadingDialog()
                ToastUtils.showLong(R.string.download_pack_fail)
            }
        })
    }

    private fun startOtaAct(meshAddr: Int, macAddr: String, version: String, deviceType: Int) {
        val intent = Intent(this@GroupOTAListActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constants.OTA_MES_Add, meshAddr)
        intent.putExtra(Constants.OTA_MAC, macAddr)
        intent.putExtra(Constants.OTA_VERSION, version)
        intent.putExtra(Constants.OTA_TYPE, deviceType)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        dispose?.dispose()
    }

    override fun onBackPressed() {
        isFinish()
    }
}