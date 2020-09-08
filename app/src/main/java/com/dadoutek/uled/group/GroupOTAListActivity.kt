package com.dadoutek.uled.group

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.text.TextUtils
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.dbModel.*
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.RecoverMeshDeviceUtil
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
import java.util.regex.Pattern


/**
 * 创建者     ZCL
 * 创建时间   2020/6/10 11:25
 * 描述
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GroupOTAListActivity : TelinkBaseActivity() {
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
    private var gwList: MutableList<DbGateway> = mutableListOf()
    private var mapBin = mutableMapOf<String, Int>()
    private var lightAdaper = GroupOTALightAdapter(R.layout.group_ota_item, lightList)
    private var curtainAdaper = GroupOTACurtainAdapter(R.layout.group_ota_item, curtainList)
    private var relayAdaper = GroupOTARelayAdapter(R.layout.group_ota_item, relayList)
    private var switchAdaper = GroupOTASwitchAdapter(R.layout.group_ota_item, switchList)
    private var sensorAdaper = GroupOTASensorAdapter(R.layout.group_ota_item, sensorList)
    private var gwAdaper = GroupOTAGwAdapter(R.layout.group_ota_item, gwList)

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_ota_list)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        loading_tansform.setOnClickListener { }
        ota_swipe_refresh_ly.setOnRefreshListener {
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
        val serializableExtra = intent.getSerializableExtra("group")
        if (serializableExtra != null)
            dbGroup = serializableExtra as DbGroup

        deviceType = intent.getIntExtra("DeviceType", 0)
        isGroup = dbGroup != null

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

        getBin()

    }

    private fun setRelayData() {
        relayList.clear()
        if (isGroup)
            relayList.addAll(DBUtils.getRelayByGroupID(dbGroup!!.id))
        else
            relayList.addAll(DBUtils.allRely)

        relayAdaper.onItemClickListener = onItemClickListener
        supportAndUNConnector()
    }

    private fun setCurtainData() {
        curtainList.clear()
        if (isGroup)
            curtainList.addAll(DBUtils.getCurtainByGroupID(dbGroup!!.id))
        else
            curtainList.addAll(DBUtils.allCurtain)

        curtainAdaper.onItemClickListener = onItemClickListener

        supportAndUNCurtain()
        curtainAdaper.notifyDataSetChanged()
    }

    private fun setLightData() {
        lightList.clear()
        if (isGroup)
            lightList.addAll(DBUtils.getLightByGroupID(dbGroup!!.id))
        else {
            when (deviceType) {
                DeviceType.LIGHT_NORMAL -> lightList.addAll(DBUtils.getAllNormalLight())
                DeviceType.LIGHT_RGB -> lightList.addAll(DBUtils.getAllRGBLight())
            }
        }
        lightAdaper.onItemClickListener = onItemClickListener
        supportAndUNLight()
        lightAdaper.notifyDataSetChanged()
    }

    private fun setSwtichData() {
        switchList.clear()
        if (isGroup)
            switchList
        else
            switchList.addAll(DBUtils.getAllSwitch())
        switchAdaper.onItemClickListener = onItemClickListener
        supportAndUNSwitch()
        switchAdaper.notifyDataSetChanged()
    }

    private fun setGwData() {
        gwList.clear()
        if (isGroup)
            gwList
        else
            gwList.addAll(DBUtils.getAllGateWay())
        gwAdaper.onItemClickListener = onItemClickListener
        supportAndUNGateway()
        gwAdaper.notifyDataSetChanged()
    }

    private fun setSensorData() {
        sensorList.clear()
        if (isGroup)
            sensorList
        else
            sensorList.addAll(DBUtils.getAllSensor())
        sensorAdaper.onItemClickListener = onItemClickListener
        supportAndUNSensor()
        sensorAdaper.notifyDataSetChanged()
    }

    private fun supportAndUNLight() {
        lightList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(itv)
                val split = itv.split("-")
                if (split.size >= 2) {
                    val versionNum = numberCharat(split[1])
                    LogUtils.v("zcl比较版本号-------$itv------${mapBin[split[0]] ?: 0}-----${versionNum.toString().toInt()}")
                    if (!TextUtils.isEmpty(versionNum.toString())) {
                        it.isMostNew = versionNum.toString().toInt() >= mapBin[split[0]] ?: 0
                        it.isSupportOta = versionNum.toString().toInt() < mapBin[split[0]] ?: 0
                    }
                }
            }
        }

        var unsupport = lightList.filter {
            !it.isSupportOta
        }
        lightList.removeAll(unsupport)
        lightList.addAll(unsupport)
    }

    open fun numberCharat(string: String): String {
        val sBuffer = StringBuffer()
        string.forEach { i ->
            if (!(48 > i.toInt() || i.toInt() > 57)) {
                sBuffer.append(i)
            }
        }
        return sBuffer.toString()
    }

    private fun supportAndUNSwitch() {
        switchList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(itv)
                val split = itv.split("-")
                if (split.size >= 2) {
                    var versionNum = numberCharat(split[1])
                    LogUtils.v("zcl比较版本号-------$itv------${mapBin[split[0]] ?: 0}-----${versionNum.toString().toInt()}")
                    if (!TextUtils.isEmpty(versionNum)) {
                        it.isMostNew = versionNum.toString().toInt() >= mapBin[split[0]] ?: 0
                        it.isSupportOta = versionNum.toString().toInt() < mapBin[split[0]] ?: 0
                    }
                }
            }
        }

        var unsupport = switchList.filter {
            !it.isSupportOta
        }
        switchList.removeAll(unsupport)
        switchList.addAll(unsupport)
    }

    private fun supportAndUNSensor() {
        sensorList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(itv)
                val split = itv.split("-")
                if (split.size >= 2) {
                    val versionNum = numberCharat(split[1])
                    LogUtils.v("zcl比较版本号-------$itv------${mapBin[split[0]] ?: 0}-----${versionNum.toString().toInt()}")
                    if (!TextUtils.isEmpty(versionNum)) {
                        it.isMostNew = versionNum.toString().toInt() >= mapBin[split[0]] ?: 0
                        it.isSupportOta = versionNum.toString().toInt() < mapBin[split[0]] ?: 0
                    }
                }
            }
        }

        var unsupport = sensorList.filter {
            !it.isSupportOta
        }
        sensorList.removeAll(unsupport)
        sensorList.addAll(unsupport)
    }

    private fun supportAndUNCurtain() {
        curtainList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(itv)
                val split = itv.split("-")
                if (split.size >= 2) {
                    val versionNum = numberCharat(split[1])
                    LogUtils.v("zcl比较版本号-------$itv------${mapBin[split[0]] ?: 0}-----${versionNum.toString().toInt()}")
                    if (!TextUtils.isEmpty(versionNum)) {
                        it.isMostNew = versionNum.toString().toInt() >= mapBin[split[0]] ?: 0
                        it.isSupportOta = versionNum.toString().toInt() < mapBin[split[0]] ?: 0
                    }
                }
            }
        }

        var unsupport = curtainList.filter {
            !it.isSupportOta
        }
        curtainList.removeAll(unsupport)
        curtainList.addAll(unsupport)
    }

    private fun supportAndUNConnector() {
        relayList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(itv)
                val split = itv.split("-")
                if (split.size >= 2) {
                    val versionNum = numberCharat(split[1])
                    LogUtils.v("zcl比较版本号-------$itv------${mapBin[split[0]] ?: 0}-----${versionNum.toString().toInt()}")
                    if (!TextUtils.isEmpty(versionNum)) {
                        it.isMostNew = versionNum.toString().toInt() >= mapBin[split[0]] ?: 0
                        it.isSupportOta = versionNum.toString().toInt() < mapBin[split[0]] ?: 0
                    }
                }
            }
        }

        var unsupport = relayList.filter { !it.isSupportOta }
        relayList.removeAll(unsupport)
        relayList.addAll(unsupport)
        relayAdaper.notifyDataSetChanged()
    }

    private fun supportAndUNGateway() {
        lightList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(itv)
                val split = itv.split("-")
                if (split.size >= 2) {
                    val versionNum = numberCharat(split[1])
                    LogUtils.v("zcl比较版本号-------$itv------${mapBin[split[0]] ?: 0}-----${versionNum.toString().toInt()}")
                    if (!TextUtils.isEmpty(versionNum)) {
                        it.isMostNew = versionNum.toString().toInt() >= mapBin[split[0]] ?: 0
                        it.isSupportOta = versionNum.toString().toInt() < mapBin[split[0]] ?: 0
                    }
                }
            }
        }

        var unsupport = lightList.filter {
            !it.isSupportOta
        }
        lightList.removeAll(unsupport)
        lightList.addAll(unsupport)
    }

    private fun initView() {
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        toolbarTv.text = getString(R.string.group_ota)
        template_recycleView.layoutManager = GridLayoutManager(this, 2)/*LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)*/
        template_recycleView.addItemDecoration(RecyclerGridDecoration(this, 2))

        getBin()

        //设置进度View下拉的起始点和结束点，scale 是指设置是否需要放大或者缩小动画
        ota_swipe_refresh_ly.setProgressViewOffset(true, -0, 500)
        //设置进度View下拉的结束点，scale 是指设置是否需要放大或者缩小动画
        ota_swipe_refresh_ly.setProgressViewEndTarget(true, 360)
        //设置进度View的组合颜色，在手指上下滑时使用第一个颜色，在刷新中，会一个个颜色进行切换
        ota_swipe_refresh_ly.setColorSchemeColors(Color.BLACK, Color.GREEN, Color.RED, Color.YELLOW, Color.BLUE)
        //设置触发刷新的距离
        ota_swipe_refresh_ly.setDistanceToTriggerSync(200)
    }

    private fun getBin() {
        dispose = NetworkFactory.getApi().binList
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe({
                    LogUtils.v("zcl获取服务器bin-----------$it-------")
                    mapBin = it
                    updataDevice()

                }, {
                    ToastUtils.showShort(getString(R.string.get_bin_fail))
                    finish()
                })
    }

    private fun updataDevice() {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> setLightData()
            DeviceType.SMART_CURTAIN -> setCurtainData()
            DeviceType.SMART_RELAY -> setRelayData()
            DeviceType.NORMAL_SWITCH -> setSwtichData()
            DeviceType.SENSOR -> setSensorData()
            DeviceType.GATE_WAY -> setGwData()
        }
    }

    val onItemClickListener = BaseQuickAdapter.OnItemClickListener { _, _, position ->
        when (deviceType.toInt()) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                val dbLight = lightList[position]

                if (dbLight.isSupportOta) {
                    if (TextUtils.isEmpty(dbLight.version)) {
                        showLoadingDialog(getString(R.string.please_wait))
                        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbLight.meshAddr) {
                            getDeviceVersionLight(dbLight)
                        } else {
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
                    } else
                        getFilePath(dbLight.meshAddr, dbLight.macAddr, dbLight.version, dbLight.productUUID)
                } else {
                    if (dbLight.isMostNew)
                        ToastUtils.showShort(getString(R.string.the_last_version))
                    else
                        ToastUtils.showShort(getString(R.string.dissupport_ota))
                }
            }
            DeviceType.SMART_CURTAIN -> {
                val dbCurtain = curtainList[position]
                if (dbCurtain.isSupportOta)
                    if (TextUtils.isEmpty(dbCurtain.version)) {
                        showLoadingDialog(getString(R.string.please_wait))

                        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbCurtain.meshAddr) {
                            getDeviceVersionCurtain(dbCurtain)
                        } else {
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
                    } else
                        getFilePath(dbCurtain.meshAddr, dbCurtain.macAddr, dbCurtain.version, dbCurtain.productUUID)
                else {
                    if (dbCurtain.isMostNew)
                        ToastUtils.showShort(getString(R.string.the_last_version))
                    else
                        ToastUtils.showShort(getString(R.string.dissupport_ota))
                }
            }
            DeviceType.SMART_RELAY -> {
                val dbrelay = relayList[position]
                if (dbrelay.isSupportOta)
                    if (TextUtils.isEmpty(dbrelay.version)) {
                        showLoadingDialog(getString(R.string.please_wait))

                        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbrelay.meshAddr) {
                            getDeviceVersionConnector(dbrelay)
                        } else {
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

                    } else
                        getFilePath(dbrelay.meshAddr, dbrelay.macAddr, dbrelay.version, dbrelay.productUUID)
                else {
                    if (dbrelay.isMostNew)
                        ToastUtils.showShort(getString(R.string.the_last_version))
                    else
                        ToastUtils.showShort(getString(R.string.dissupport_ota))
                }
            }
            DeviceType.NORMAL_SWITCH -> {
                val dbsw = switchList[position]
                if (dbsw.isSupportOta)
                    if (TextUtils.isEmpty(dbsw.version)) {
                        showLoadingDialog(getString(R.string.please_wait))

                        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbsw.meshAddr) {
                            getDeviceVersionSwitch(dbsw)
                        } else {
                            showLoadingDialog(getString(R.string.please_wait))
                            TelinkLightService.Instance()?.idleMode(true)
                            Thread.sleep(500)

                            connect(macAddress = dbsw.macAddr, meshAddress = dbsw.meshAddr, connectTimeOutTime = 15)
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
                    } else
                        getFilePath(dbsw.meshAddr, dbsw.macAddr, dbsw.version, dbsw.productUUID)
                else {
                    if (dbsw.isMostNew)
                        ToastUtils.showShort(getString(R.string.the_last_version))
                    else
                        ToastUtils.showShort(getString(R.string.dissupport_ota))
                }
            }
            DeviceType.SENSOR -> {
                val dbsensor = sensorList[position]
                if (dbsensor.isSupportOta)
                    if (TextUtils.isEmpty(dbsensor.version)) {
                        showLoadingDialog(getString(R.string.please_wait))

                        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbsensor.meshAddr) {
                            getDeviceVersionSensor(dbsensor)
                        } else {
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

                    } else
                        getFilePath(dbsensor.meshAddr, dbsensor.macAddr, dbsensor.version, dbsensor.productUUID)
                else {
                    if (dbsensor.isMostNew)
                        ToastUtils.showShort(getString(R.string.the_last_version))
                    else
                        ToastUtils.showShort(getString(R.string.dissupport_ota))
                }
            }
            DeviceType.GATE_WAY -> {
                val dbgw = gwList[position]
                if (dbgw.isSupportOta)
                    if (TextUtils.isEmpty(dbgw.version)) {
                        showLoadingDialog(getString(R.string.please_wait))

                        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbgw.meshAddr) {
                            getDeviceVersionGw(dbgw)
                        } else {
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

                    } else
                        getFilePath(dbgw.meshAddr, dbgw.macAddr, dbgw.version, DeviceType.GATE_WAY)
                else {
                    if (dbgw.isMostNew)
                        ToastUtils.showShort(getString(R.string.the_last_version))
                    else
                        ToastUtils.showShort(getString(R.string.dissupport_ota))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        updataDevice()
    }

    private fun getDeviceVersionLight(dbLight: DbLight) {
        val dispos = Commander.getDeviceVersion(dbLight.meshAddr).subscribe(
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
        val dispos = Commander.getDeviceVersion(dbLight.meshAddr).subscribe(
                { s: String ->
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
                if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == meshAddr) {
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
        intent.putExtra(Constant.OTA_MES_Add, meshAddr)
        intent.putExtra(Constant.OTA_MAC, macAddr)
        intent.putExtra(Constant.OTA_VERSION, version)
        intent.putExtra(Constant.OTA_TYPE, deviceType)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        dispose?.dispose()
    }
}