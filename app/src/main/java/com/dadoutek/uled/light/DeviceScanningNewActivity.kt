package com.dadoutek.uled.light

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.le.ScanFilter
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.RomUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.GwLoginActivity
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.group.BatchGroupFourDeviceActivity
import com.dadoutek.uled.group.GroupsRecyclerViewAdapter
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.model.ScannedDeviceItem
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Constant.VENDOR_ID
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Mesh
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.model.dbModel.*
import com.dadoutek.uled.model.dbModel.DBUtils.isFastDoubleClick
import com.dadoutek.uled.model.dbModel.DBUtils.lastRegion
import com.dadoutek.uled.model.dbModel.DBUtils.lastUser
import com.dadoutek.uled.model.dbModel.DBUtils.saveUser
import com.dadoutek.uled.model.httpModel.GwModel
import com.dadoutek.uled.model.httpModel.RegionModel
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkStatusCode
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.othersview.SplashActivity
import com.dadoutek.uled.router.RouterOtaActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.router.bean.Data
import com.dadoutek.uled.scene.SensorDeviceDetailsActivity
import com.dadoutek.uled.switches.*
import com.dadoutek.uled.switches.fourkey.ConfigFourSwitchActivity
import com.dadoutek.uled.switches.sixkey.ConfigSixSwitchActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.tellink.TelinkMeshErrorDealActivity
import com.dadoutek.uled.util.*
import com.polidea.rxandroidble2.scan.ScanSettings
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.event.MeshEvent
import com.telink.bluetooth.light.*
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.Strings
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_device_scanning.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.template_lottie_animation.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.singleLine
import org.jetbrains.anko.startActivity
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 创建者     zcl
 * 创建时间   2019/8/28 18:37
 * 描述	      ${搜索 冷暖灯 全彩灯  窗帘 Connector蓝牙接收器设备}$
 *路由逻辑: 1. http请求扫描 等待mqtt回调(无回调 失败) - 接收mqtt上传的设备每次超时每个6秒收到刷新-超时--走跳转逻辑(请求获取未确认扫描结果得到数据包装)
 * --如果得到数据的情况下是超时也就是mqtt没有接到但是没有结束 重新计算超时 如果借宿了 确认清除数据 跳转43
 */
class DeviceScanningNewActivity : TelinkMeshErrorDealActivity(), EventListener<String> {
    private var startConnect: Boolean = false
    private var isOffLine: Boolean = false
    private var isFinisAc: Boolean = false
    private var disposableUpMeshTimer: Disposable? = null
    private var scanRouterTimeoutTime: Long = 0
    private var routerScanCount: Int = 0
    private val TAG = "zcl-DeviceScanningNewActivity"
    private var disposableFind: Disposable? = null
    private var meshList: MutableList<Int> = mutableListOf()
    private lateinit var dbGw: DbGateway
    private var mConnectDisposal: Disposable? = null
    private val MAX_RETRY_COUNT = 4   //update mesh failed的重试次数设置为6次
    private val MAX_RSSI = 90
    private var scanTimeoutTime = 25L//25
    private val SCAN_DELAY: Long = 1000       // 每次Scan之前的Delay , 1000ms比较稳妥。
    private val HUAWEI_DELAY: Long = 2000       // 华为专用Delay
    val user = lastUser
    private var mAutoConnectDisposable: Disposable? = null
    private var disposable: Disposable? = null
    private var mApplication: TelinkLightApplication? = null
    private var mRxPermission: RxPermissions? = null

    //防止内存泄漏
    internal var mDisposable = CompositeDisposable()

    //分组所含灯的缓存
    private var nowDeviceList: MutableList<ScannedDeviceItem> = mutableListOf()
    private var inflater: LayoutInflater? = null
    private var grouping: Boolean = false
    private var isFirtst = true
    private var lastMyRegion = lastRegion

    //标记登录状态
    private lateinit var groupsRecyclerViewAdapter: GroupsRecyclerViewAdapter
    private var groups: MutableList<DbGroup> = ArrayList()
    private var mTimer: Disposable? = null
    private var mUpdateMeshRetryCount = 0
    private var mConnectRetryCount = 0

    //当前所选组index
    private var currentGroupIndex = -1
    private var updateList: MutableList<ScannedDeviceItem> = ArrayList()

    //对一个灯重复分组时记录上一次分组
    private val mGroupingDisposable: Disposable? = null

    //灯的mesh地址
    private var mConnectTimer: Disposable? = null
    private val mBlinkDisposables = SparseArray<Disposable>()
    private var initHasGroup = false
    private var allLightId: Long = 0
    private var meshUpdateType: Long = 0 //0发送命令更新mesh等待回调  1收到mesh回调
    private var updateMeshStatus: UPDATE_MESH_STATUS? = null
    private var mAddDeviceType: Int = 0
    private var mAddedDevices: MutableList<ScannedDeviceItem> = mutableListOf()
    private var mAddedDevicesInfos = arrayListOf<DeviceInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableConnectionStatusListener()
        mRxPermission = RxPermissions(this)
        //设置屏幕常亮***
        isScanningJM = true
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //***
        setContentView(R.layout.activity_device_scanning)
        meshList.clear()
        TelinkLightService.Instance()?.idleMode(true)
        initData()
        initView()
        initListener()
        startScan()
    }

    internal var syncCallback: SyncCallback = object : SyncCallback {
        override fun complete() {}
        override fun start() {}
        override fun error(msg: String) {
            ToastUtils.showLong(R.string.upload_data_failed)
        }
    }

    private var bestRssiDevice: DeviceInfo? = null

    enum class UPDATE_MESH_STATUS { SUCCESS, FAILED, UPDATING_MESH }

    //扫描失败处理方法
    private fun scanFail() {
        hideLoadingDialog()
        closeAnimation()
        btn_stop_scan.visibility = View.GONE
        scanning_no_device.visibility = View.VISIBLE
        //toolbarTv.text = getString(R.string.scan_end)
        scanning_no_factory_btn_ly.visibility = View.VISIBLE
    }

    private fun startTimer() {
        stopScanTimer()
        LogUtils.d("startTimer")
        mTimer = Observable.timer(scanTimeoutTime, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    LogUtils.d("onLeScanTimeout")
                    onLeScanTimeout()
                }
    }

    private fun retryScan() {
        // toolbarTv.text = getString(R.string.scanning)
        //TelinkLightService.Instance()?.mAdapter?.stop()//此处导致整个扫描中断
        if (mUpdateMeshRetryCount < MAX_RETRY_COUNT) {
            mUpdateMeshRetryCount++
            Log.d("TelinkBluetoothSDK ", "完整流程更新mesh失败重新扫秒 update mesh failed , retry count = $mUpdateMeshRetryCount")
            stopScanTimer()
            meshList.clear()
            startScan()
        } else {
            Log.d("TelinkBluetoothSDK", "update mesh failed , do not retry")
            stopScan()
            LogUtils.v("zcl-----------完整流程扫描重试超时-------")
        }
        updateMeshStatus = UPDATE_MESH_STATUS.FAILED
    }

    private fun stopScanTimer() {
        if (mTimer != null && !mTimer!!.isDisposed) {
            mTimer!!.dispose()
            setScanningMode(false)
        }
    }


    //处理扫描成功后
    @SuppressLint("CheckResult")
    private fun scanSuccess() {
        TelinkLightService.Instance()?.idleMode(true)
        //更新Title
        //存储当前添加的灯。
        //2018-4-19-hejiajun 添加灯调整位置，防止此时点击灯造成下标越界
        if (nowDeviceList.size > 0) {
            nowDeviceList.clear()
        }
        nowDeviceList.addAll(mAddedDevices)
        closeAnimation()
        showLoadingDialog(resources.getString(R.string.please_wait))

        disposableConnectTimer?.dispose()
        disposableConnectTimer = Observable.timer(1000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    // autoConnect(mutableListOf(mAddDeviceType))
                    autoConnect2()
                }

        // list_devices?.visibility = View.VISIBLE
        // btn_add_groups?.visibility = View.VISIBLE
        //if (mAddDeviceType != DeviceType.GATE_WAY)
        // btn_add_groups?.setText(R.string.start_group_bt)
        //scanning_num?.visibility = View.GONE
        // btn_stop_scan?.visibility = View.GONE

        //进入分组界面之前的监听
        // btn_add_groups?.setOnClickListener(onClick)
    }


    private fun disposeAllSubscribe() {
        for (i in 0 until mBlinkDisposables.size()) {
            val disposable = mBlinkDisposables.get(i)
            disposable?.dispose()
        }
        mGroupingDisposable?.dispose()
        mDisposable.dispose()
        mTimer?.dispose()
        mConnectTimer?.dispose()
    }

    @SuppressLint("CheckResult")
    private fun doFinish() {
        mApplication?.removeEventListener(this)
        if (updateList.size > 0) {
            checkNetworkAndSync()
        }
        updateList.clear()
        isFinisAc = false
        if (!isOffLine)
            routerStopScan()
        disposeAllSubscribe()

        mApplication?.removeEventListener(this)
        showLoadingDialog(getString(R.string.please_wait))
        if (!Constant.IS_ROUTE_MODE)
            TelinkLightService.Instance()?.idleMode(true)
        disposableConnectTimer?.dispose()
        disposableConnectTimer = Observable.timer(1000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideLoadingDialog()
                    if (ActivityUtils.isActivityExistsInStack(MainActivity::class.java))
                        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
                    else {
//                        ActivityUtils.startActivity(MainActivity::class.java)
                        LogUtils.d("MainActivity doesn't exist in stack")
                        finish()
                    }
                }
    }

    override fun onPause() {
        super.onPause()
        if (mConnectTimer != null)
            mConnectTimer!!.dispose()
        mConnectDisposal?.dispose()
        mApplication?.removeEventListener(this)
    }

    private fun updateDevice(item: ScannedDeviceItem) {
        when (item.deviceInfo.productUUID) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                val old = DBUtils.getLightByMeshAddr(item.deviceInfo.meshAddress)
                if (old == null || old.macAddr != item.deviceInfo.macAddress) {
                    val dbItem = DbLight()
                    dbItem.meshAddr = item.deviceInfo.meshAddress
                    if (DeviceType.LIGHT_NORMAL == item.deviceInfo.productUUID)
                        dbItem.name = getString(R.string.normal_light) + dbItem.meshAddr
                    else
                        dbItem.name = getString(R.string.rgb_light) + dbItem.meshAddr
                    dbItem.textColor = this.resources.getColor(R.color.black)
                    dbItem.belongGroupId = item.belongGroupId
                    dbItem.macAddr = item.deviceInfo.macAddress
                    dbItem.sixMac = item.deviceInfo.sixByteMacAddress
                    dbItem.meshUUID = item.deviceInfo.meshUUID
                    dbItem.productUUID = item.deviceInfo.productUUID
                    dbItem.isSelected = item.isSelected
                    dbItem.version = item.deviceInfo.firmwareRevision
                    dbItem.rssi = item.rssi
                    DBUtils.saveLight(dbItem, false)
                }
            }

            DeviceType.SENSOR -> {
                val dbItem = DbSensor()
                dbItem.name = getString(R.string.sensor) + dbItem.meshAddr
                dbItem.meshAddr = item.deviceInfo.meshAddress
                dbItem.belongGroupId = item.belongGroupId
                dbItem.macAddr = item.deviceInfo.macAddress
                dbItem.productUUID = item.deviceInfo.productUUID
                dbItem.version = item.deviceInfo.firmwareRevision
                dbItem.rssi = item.rssi
                DBUtils.saveSensor(dbItem, false)
            }
            DeviceType.SMART_RELAY -> {
                val dbItem = DbConnector()
                dbItem.name = getString(R.string.relay) + dbItem.meshAddr
                dbItem.meshAddr = item.deviceInfo.meshAddress
                dbItem.textColor = this.resources.getColor(R.color.black)
                dbItem.belongGroupId = item.belongGroupId
                dbItem.macAddr = item.deviceInfo.macAddress
                dbItem.version = item.deviceInfo.firmwareRevision
                dbItem.meshUUID = item.deviceInfo.meshUUID
                dbItem.productUUID = item.deviceInfo.productUUID
                dbItem.isSelected = item.isSelected
                dbItem.rssi = item.rssi
                DBUtils.saveConnector(dbItem, false)
            }
            DeviceType.SMART_CURTAIN -> {
                LogUtils.e("zcl保存分组curtain----${DBUtils.getCurtainByGroupID(item.belongGroupId).size}")
                val dbItem = DbCurtain()
                dbItem.name = getString(R.string.curtain) + dbItem.meshAddr
                dbItem.meshAddr = item.deviceInfo.meshAddress
                dbItem.version = item.deviceInfo.firmwareRevision
                dbItem.textColor = this.resources.getColor(R.color.black)
                dbItem.belongGroupId = item.belongGroupId
                dbItem.macAddr = item.deviceInfo.macAddress
                dbItem.productUUID = item.deviceInfo.productUUID
                dbItem.isSelected = item.isSelected
                dbItem.rssi = item.rssi
                DBUtils.saveCurtain(dbItem, false)
                LogUtils.e("zcl保存分组curtain----${DBUtils.getCurtainByGroupID(item.belongGroupId).size}----------${DBUtils.getAllCurtains().size}")
            }
            DeviceType.GATE_WAY -> {
                dbGw = DbGateway(getGwId())
                dbGw.macAddr = item.deviceInfo.macAddress
                dbGw.meshAddr = item.deviceInfo.meshAddress
                dbGw.productUUID = item.deviceInfo.productUUID
                dbGw.version = item.deviceInfo.firmwareRevision
                dbGw.sixByteMacAddr = item.deviceInfo.sixByteMacAddress
                dbGw.name = getString(R.string.Gate_way) + dbGw.meshAddr
                dbGw.tags = ""
                dbGw.timePeriodTags = ""
                dbGw.uid = (lastUser?.id ?: 0).toInt()
                DBUtils.saveGateWay(dbGw, false)
                addGw(dbGw)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun addGw(dbGw: DbGateway) {
        GwModel.add(dbGw)?.subscribe({
            LogUtils.v("zcl-----网关失添成功返回-------------$it")
        }, {
            LogUtils.v("zcl-------网关失添加败-----------" + it.message)
        })
    }

    override fun onBackPressed() {
        if (grouping) {
            for (meshAddr in mAddedDevices)
                doFinish()
        } else {
            if (isScanning) {
                AlertDialog.Builder(this)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            doFinish()
                        }
                        .setNegativeButton(R.string.btn_cancel) { _, _ -> }
                        .setMessage(R.string.exit_tips_in_scanning)
                        .show()
            } else {
                doFinish()
            }
        }
    }

    private fun addNewGroup() {
        val textGp = EditText(this)
        textGp.singleLine = true
        textGp.setText(DBUtils.getDefaultNewGroupName())
        StringUtils.initEditTextFilter(textGp)
        val builder = AlertDialog.Builder(this@DeviceScanningNewActivity)
        builder.setTitle(R.string.create_new_group)
        builder.setIcon(android.R.drawable.ic_dialog_info)
        builder.setView(textGp)
        builder.setCancelable(false)
        builder.setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
            // 获取输入框的内容
            if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                //往DB里添加组数据
                val newGroup: DbGroup? = DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, mAddDeviceType.toLong())
                newGroup?.let {
                    groups?.add(newGroup)
                }
                refreshView()
                dialog.dismiss()
                val imm = this@DeviceScanningNewActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }
        if (!isGuide) {
            builder.setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }
        }
        textGp.isFocusable = true
        textGp.isFocusableInTouchMode = true
        textGp.requestFocus()
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val inputManager = textGp.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.showSoftInput(textGp, 0)
            }
        }, 200)
        builder.show()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }


    private fun refreshView() {
        currentGroupIndex = groups.size - 1
        for (i in groups.indices.reversed()) {
            groups[i].checked = i == groups.size - 1
        }
        add_group_relativeLayout?.visibility = View.GONE
        add_group?.visibility = View.VISIBLE
        recycler_view_groups?.smoothScrollToPosition(groups.size - 1)
        groupsRecyclerViewAdapter.notifyDataSetChanged()
        SharedPreferencesHelper.putInt(TelinkLightApplication.getApp(),
                Constant.DEFAULT_GROUP_ID, currentGroupIndex)
    }


    /**
     * 自动重连
     * 此处用作设备登录
     */
    private fun autoConnect(elements: MutableList<Int>) {
        setScanningMode(false)
        when (mAddDeviceType) {
            DeviceType.GATE_WAY -> {
                LogUtils.v("zcl----连接类型---$mAddDeviceType----网关-----${DeviceType.GATE_WAY}-----mac---${bestRssiDevice?.macAddress}" +
                        "--------mesh--${bestRssiDevice?.meshAddress}")
                mAutoConnectDisposable = connect(deviceTypes = elements, macAddress = bestRssiDevice?.macAddress, fastestMode = true, connectTimeOutTime = 15)
                        ?.subscribe(
                                {
                                    TelinkLightApplication.getApp().isConnectGwBle = true
                                    onLogin()
                                }, {
                            // onLogin()
                            hideLoadingDialog()
                            ToastUtils.showLong(getString(R.string.connect_fail))
                            finish()
                            LogUtils.d(it)
                        })
            }
            else -> {
                val meshAddress = when {
                    mAddedDevices.size > 0 -> mAddedDevices[0].meshAddress
                    else -> bestRssiDevice!!.meshAddress
                }
                val macAddress = when {
                    mAddedDevices.size > 0 -> mAddedDevices[0].macAddress
                    else -> bestRssiDevice!!.macAddress
                }
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    if (TelinkLightService.Instance()?.adapter?.mLightCtrl?.currentLight?.isConnected != true) {
                        TelinkLightService.Instance()?.idleMode(true)
                        Thread.sleep(800)
                        val deviceTypes = mutableListOf(DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD,
                                DeviceType.LIGHT_RGB, DeviceType.SMART_RELAY, DeviceType.SMART_CURTAIN)
                        mConnectDisposable = connect(deviceTypes = deviceTypes, retryTimes = 3, fastestMode = true)
                                ?.subscribe({
                                    onLogin()
                                }, {
                                    hideLoadingDialog()
                                    ToastUtils.showLong(getString(R.string.connect_fail))
                                    LogUtils.d(it)
                                    onLogin()
                                })
                    } else
                        loginMes()

                } else {
                    TelinkLightApplication.getApp().isConnectGwBle = true
                    onLogin()
                }
            }
        }
    }

    /**
     * 自动重连
     * 此处用作设备登录
     */
    private fun autoConnect2() {
        if (TelinkLightService.Instance() != null) {
            if (TelinkLightService.Instance().mode != LightAdapter.MODE_AUTO_CONNECT_MESH) {
                //showLoadingDialog(resources.getString(R.string.connecting_tip))  //chown
                closeAnimation()
                startConnect = true

                val meshName = lastUser!!.controlMeshName

                //自动重连参数
                val connectParams = Parameters.createAutoConnectParameters()
                connectParams.setMeshName(meshName)
                connectParams.setPassword(NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16))
                connectParams.autoEnableNotification(true)
                //连接，如断开会自动重连
                Thread {
                    TelinkLightService.Instance().autoConnect(connectParams)
                }.start()
            }
            TelinkLightApplication.getApp().isConnectGwBle = true
            //刷新Notify参数
            val refreshNotifyParams = Parameters.createRefreshNotifyParameters()
            refreshNotifyParams.setRefreshRepeatCount(2)
            refreshNotifyParams.setRefreshInterval(1000)
            //开启自动刷新Notify
            TelinkLightService.Instance().autoRefreshNotify(refreshNotifyParams)
            disposableTimer?.dispose()
            disposableTimer = Observable.timer(15000, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        ToastUtils.showShort(getString(R.string.connect_fail))
                        hideLoadingDialog()
                    }
        }
    }

    private fun loginMes() {
        val account = lastUser?.account
        val pwd = NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16)
        TelinkLightService.Instance().login(Strings.stringToBytes(account, 16)
                , Strings.stringToBytes(pwd, 16))
    }

    @SuppressLint("CheckResult")
    private fun connect2(mac: String) {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it) {
                        //授予了权限
                        if (TelinkLightService.Instance() != null) {
                            progressBar?.visibility = View.VISIBLE
                            TelinkLightService.Instance().connect(mac, 10)
                        }
                    } else {
                        //没有授予权限
                        DialogUtils.showNoBlePermissionDialog(this, { connect2(mac) }, { finish() })
                    }
                }, {})
    }

    private fun closeAnimation() {
        setScanningMode(false)
        lottieAnimationView?.cancelAnimation()
        lottieAnimationView?.visibility = View.GONE
    }

    private fun initListener() {
        cancelf.setOnClickListener { popFinish.dismiss() }
        confirmf.setOnClickListener {
            popFinish.dismiss()
            stopScanTimer()
            closeAnimation()
            getAndFinish()
        }

        scanning_no_factory_btn.setOnClickListener {
            seeHelpe("#QA1")
        }

        btn_start_scan.setOnClickListener {
            initVisiable()
            //toolbarTv.text = getString(R.string.scanning)
            meshList.clear()
            startScan()
        }
        add_group_layout.setOnClickListener {
            //全彩灯以及普通等扫描完毕添加组
            isGuide = false
            addNewGroup()
        }
        btn_stop_scan?.setOnClickListener {//停止扫描
            if (Constant.IS_ROUTE_MODE) {
                isFinisAc = false
                routerStopScan()
            } else {
                closeAnimation()
                list_devices.visibility = View.GONE
                scanning_num.visibility = View.GONE
                btn_stop_scan.visibility = View.GONE
                stopScan()
            }
        }

        add_group_relativeLayout?.setOnClickListener { _ -> addNewGroup() }

    }

    private fun getAndFinish() {
        SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, object : SyncCallback {
            override fun start() {}

            override fun complete() {
                finish()
            }

            override fun error(msg: String?) {
                finish()
            }
        })
    }

    private fun stopScan() {
        stopScanTimer()
        if (mAddedDevices.size > 0) {//表示目前已经搜到了至少有一个设备
            scanSuccess()
        } else {
            closeAnimation()
            btn_stop_scan.visibility = View.GONE
            ToastUtils.showLong(getString(R.string.scan_end))
            if (mAddDeviceType == DeviceType.NORMAL_SWITCH || mAddDeviceType == DeviceType.GATE_WAY) TelinkLightService.Instance().disconnect()
            finish()
        }
    }

    @SuppressLint("CheckResult")
    private fun routerStopScan() {
        if (Constant.IS_ROUTE_MODE)
            RouterModel.routeStopScan(TAG, Constant.SCAN_SERID)
                    ?.subscribe({ itr ->
                        LogUtils.v("zcl-----------收到路由停止请求-------$itr")
                        when (itr.errorCode) {
                            0 -> {
                                showLoadingDialog(getString(R.string.please_wait))
                                disposableTimer?.dispose()
                                disposableTimer = Observable.timer(itr.t.timeout + 3L, TimeUnit.SECONDS)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe {
                                            hideLoadingDialog()
                                            if (!isFinisAc)
                                                ToastUtils.showShort(getString(R.string.router_stop_scan_faile))
                                            finish()
                                        }
                            }
                            NetworkStatusCode.ROUTER_STOP -> {
                                LogUtils.v("zcl------路由是否是点击返回-isFinisAc---$isFinisAc--------")
                                if (!isFinisAc) //路由停止
                                    skipeType()
                                else
                                    finish()
                            }
                        }
                    }, {
                        ToastUtils.showShort(it.message)
                    })
    }

    override fun tzRouteStopScan(cmdBean: CmdBodyBean) {
        hideLoadingDialog()
        LogUtils.v("zcl---------收到路由停止请求通知---------")
        if (cmdBean.status == 0) {
            closeAnimation()
            list_devices.visibility = View.GONE
            scanning_num.visibility = View.GONE
            btn_stop_scan.visibility = View.GONE
            getRouterScanResult()
        } else {
            ToastUtils.showShort(getString(R.string.router_stop_scan_faile))
        }
    }

    private fun initView() {
        initToolbar()
        addListerner()

        this.inflater = this.layoutInflater
        this.grouping_completed?.setBackgroundColor(resources.getColor(R.color.gray))

        this.updateList.clear()
        initVisiable()
    }

    @SuppressLint("StringFormatInvalid")
    private fun initVisiable() {
        image_bluetooth?.visibility = View.GONE
        btn_add_groups?.visibility = View.GONE
        grouping_completed?.visibility = View.GONE
        toolbar?.findViewById<TextView>(R.id.tv_function1)?.visibility = View.GONE
        scanning_no_device.visibility = View.GONE
        scanning_num.visibility = View.VISIBLE
        if (mAddDeviceType != DeviceType.GATE_WAY)
            scanning_num.text = getString(R.string.title_scanned_device_num, mAddedDevices.size)
        else
            scanning_num.text = getString(R.string.scanning)
        btn_stop_scan?.setText(R.string.stop_scan)
        btn_stop_scan?.visibility = View.VISIBLE
    }

    private fun addListerner() {
        //监听事件
        mApplication?.removeEventListener(this)
        mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)
        mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
        mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)
        mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)

        LogUtils.d("addEventListener(DeviceEvent.STATUS_CHANGED, this)")
    }

    @SuppressLint("ResourceType")
    private fun initToolbar() {
        toolbar?.setNavigationIcon(R.drawable.icon_return)
        toolbar?.setNavigationOnClickListener {
            /*if (isScanning) {
                   cancelf.isClickable = true
                   confirmf.isClickable = true
                   popFinish.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
               } else {
                   finish()
               }*/
            if (isScanning) {
                // chown 改，>0 与 else
                if (mAddedDevices.size > 0) {
                    AlertDialog.Builder(this)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                doFinish()
                            }
                            .setNegativeButton(R.string.btn_cancel) { _, _ -> }
                            .setMessage(R.string.exit_tips_in_scanning)
                            .show()
                } else {
                    closeAnimation()
                    btn_stop_scan.visibility = View.GONE
                    ToastUtils.showLong(getString(R.string.scan_end))
                    if (mAddDeviceType == DeviceType.NORMAL_SWITCH || mAddDeviceType == DeviceType.GATE_WAY) TelinkLightService.Instance().disconnect()
                    finish()
                }
            } else {
                doFinish()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initData() {
        // mAddDeviceType = intent.getIntExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
        val serializable = intent.getIntExtra(Constant.DEVICE_TYPE, 0)
        mAddDeviceType = if (serializable == 0) DeviceType.LIGHT_NORMAL else serializable

        LogUtils.v("zcl------扫描设备类型$mAddDeviceType------------扫描个数${mAddedDevices.size}----${DBUtils.getAllCurtains()}")

        allLightId = DBUtils.getGroupByMeshAddr(0xffff).id?.toLong() ?: 0
        mApplication = this.application as TelinkLightApplication

        when (mAddDeviceType) {
            DeviceType.NORMAL_SWITCH -> {
                groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.NORMAL_SWITCH))
                groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.NORMAL_SWITCH2))
                groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.SCENE_SWITCH))
                groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.SMART_CURTAIN_SWITCH))
                groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.FOUR_SWITCH))
                groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.EIGHT_SWITCH))
                groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.SIX_SWITCH))
                groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.DOUBLE_SWITCH))
            }
            else -> groups.addAll(DBUtils.getGroupsByDeviceType(mAddDeviceType))
        }

        val title = when (mAddDeviceType) {
            DeviceType.LIGHT_NORMAL -> getString(R.string.normal_light)
            DeviceType.LIGHT_RGB -> getString(R.string.rgb_light)
            98 -> getString(R.string.sensor)
            99 -> getString(R.string.switch_title)
            DeviceType.SMART_RELAY -> getString(R.string.relay)
            DeviceType.SMART_CURTAIN -> getString(R.string.curtain)
            DeviceType.GATE_WAY -> getString(R.string.Gate_way)
            else -> getString(R.string.normal_light)
        }
        toolbarTv?.text = title
        when {
            groups.size > 0 -> {
                for (i in groups.indices)
                    when (i) {
                        groups.size - 1 -> { //选中最后一个组
                            groups[i].checked = true
                            currentGroupIndex = i
                            SharedPreferencesHelper.putInt(TelinkLightApplication.getApp(), Constant.DEFAULT_GROUP_ID, currentGroupIndex)
                        }
                        else -> groups[i].checked = false
                    }
                initHasGroup = true
            }
            else -> {
                initHasGroup = false
                currentGroupIndex = -1
            }
        }
    }

    override fun onResume() {
        super.onResume()
        stopTimerUpdate()
        //disableConnectionStatusListener()//停止监听 否则扫描到新设备会自动创建新的对象
        if (TelinkLightService.Instance() == null) //检测service是否为空，为空则重启
            mApplication?.startLightService(TelinkLightService::class.java)
    }

    // 如果没有网络，则弹出网络设置对话框
    private fun checkNetworkAndSync() {
        if (NetWorkUtils.isNetworkAvalible(this)){
            LogUtils.v("chown -- 同步数据")

            SyncDataPutOrGetUtils.syncPutDataStart(this, syncCallback)
        }
    }

    override fun onLocationEnable() {}

    /**
     * 泰凌微蓝牙库的状态回调
     * 事件处理方法
     * @param event
     */
    override fun performed(event: Event<String>) {
        when (event.type) {
            LeScanEvent.LE_SCAN -> this.onLeScan(event as LeScanEvent)
            LeScanEvent.LE_SCAN_TIMEOUT -> {
            }
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            MeshEvent.ERROR -> this.onMeshEvent()
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
            }
        }
    }

    private fun onErrorReport(info: ErrorReportInfo) {
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> when (info.errorCode) {
                ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> LogUtils.e("蓝牙未开启")
                ErrorReportEvent.ERROR_SCAN_NO_ADV -> LogUtils.e("无法收到广播包以及响应包")
                ErrorReportEvent.ERROR_SCAN_NO_TARGET -> LogUtils.e("未扫到目标设备")
            }
            ErrorReportEvent.STATE_CONNECT -> when (info.errorCode) {
                ErrorReportEvent.ERROR_CONNECT_ATT -> LogUtils.e("未读到att表")
                ErrorReportEvent.ERROR_CONNECT_COMMON -> LogUtils.e("未建立物理连接")
            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> LogUtils.e("value check失败： 密码错误")
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> LogUtils.e("read login data 没有收到response")
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> LogUtils.e("write login data 没有收到response")
                }
                finish()
            }
        }
    }

    private fun onMeshEvent() {
        ToastUtils.showLong(R.string.restart_bluetooth)
    }

    /**
     * 扫描不到任何设备了
     * （扫描结束）
     */
    private fun onLeScanTimeout() {
        setScanningMode(false)
        if (Constant.IS_ROUTE_MODE) {
            skipeType()
        } else {
            TelinkLightService.Instance()?.idleMode(false)
            if (mAddedDevices.size > 0)//表示目前已经搜到了至少有一个设备
                scanSuccess()
            else
                scanFail()
        }
    }

    private fun routeTimerOut() {
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(scanRouterTimeoutTime, TimeUnit.SECONDS).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe {
                    skipeType()
                }
    }

    /**
     * 开始扫描
     */
    @SuppressLint("CheckResult")
    private fun startScan() {
        TelinkApplication.getInstance().isConnect = false
        isFirtst = false
        LogUtils.v("zcl-----------收到路由扫描开始-------${Constant.IS_ROUTE_MODE}IS_ROUTE_MODE")
        if (Constant.IS_ROUTE_MODE) {//发送命令
            routerStartScan()
        } else {
            setScanningMode(true)
            if (mRxPermission != null)
                mRxPermission!!.request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN)?.subscribeOn(Schedulers.io())
                        ?.observeOn(AndroidSchedulers.mainThread())?.subscribe { granted ->
                            if (granted!!) {
                                oldStartScan()
                            } else {
                                DialogUtils.showNoBlePermissionDialog(this, {
                                    startScan()
                                    null
                                }, {
                                    doFinish()
                                    null
                                })
                            }
                        }?.let {
                            mDisposable.add(it)
                        }
        }
    }

    @SuppressLint("CheckResult")
    private fun routerStartScan() {
        RouterModel.routerStartScan(mAddDeviceType, TAG)?.subscribe({
            LogUtils.v("zcl-----------收到路由h扫描开始-------$it")
            when (it.errorCode) {
                0 -> {
                    routeStartTimerOut(it.t.timeout.toLong())
                    startAnimation()
                }
                90025 -> {
                    showLoadingDialog(getString(R.string.scan_result_confirm))
                    getRouterScanResult()
                }
                90018 -> {
                    ToastUtils.showShort(getString(R.string.no_this_device_type) + mAddDeviceType)
                    finish()
                }//":999999未收录的scanType"
                20030 -> {
                    ToastUtils.showShort(getString(R.string.transfer_accounts_code_exit_cont_scan))
                    finish()
                }//移交码存在的时候不能扫描
                90999 -> {
                    startAnimation()
                    getScanRouterState()
                }//扫描中，不能再次进行扫描。请尝试获取路由模式下状态以恢复上次扫描
                90998 -> {
                    ToastUtils.showShort(getString(R.string.otaing_to_ota_activity))
                    startActivity(Intent(this@DeviceScanningNewActivity, RouterOtaActivity::class.java))
                    finish()
                }//OTA中不能扫描，请稍后。请尝试获取路由模式下状态以恢复上次OTA
                90006 -> {
                    ToastUtils.showShort(getString(R.string.mesh_not_enought))
                    finish()
                }//meshAddr地址不够, 无法再扫描设备
                90004 -> {
                    ToastUtils.showShort(getString(R.string.region_no_router))
                    finish()
                }//该账号该区域下没有路由
                90005 -> {
                    ToastUtils.showShort(getString(R.string.router_offline))
                    scanFail()
                    isOffLine = true;
                    //stopScanTimer()
                }//该账号该区域下没有可用的路由，请检查路由是否上电联网
                else -> ToastUtils.showShort(it.message)
            }

        }, {
            ToastUtils.showLong(it.message)
            scanFail()
        })
    }

    override fun tzStartRouterScan(cmdBodyBean: CmdBodyBean) {//收到路由是否开始扫描的回调
        LogUtils.v("zcl-----------收到路由开始扫描-------$cmdBodyBean")
        if (cmdBodyBean.ser_id == TAG) {
            disposableTimer?.dispose()
            if (cmdBodyBean.status == Constant.ALL_SUCCESS) {//扫描成功继续重新计算超时时间
                scanRouterTimeoutTime = cmdBodyBean.reportTimeout.toLong()
                routeTimerOut()
                Constant.SCAN_SERID = cmdBodyBean.scanSerId
            } else {
                ToastUtils.showShort(cmdBodyBean.msg)
                if (cmdBodyBean.cmd == 3001)//路由没有进行扫描，请重试
                    finish()
                closeAnimation()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun tzRouteDeviceNum(cmdBodyBean: CmdBodyBean) {//收到扫描的设备数
        if (cmdBodyBean.finish) {
            skipeType()
//            LogUtils.v("zcl-----------收到路由设备结束-------$cmdBodyBean")
        } else {
//            LogUtils.v("zcl-----------收到路由设备数-------$cmdBodyBean")
            routerScanCount = cmdBodyBean.count
            if (mAddDeviceType == DeviceType.LIGHT_NORMAL || mAddDeviceType == DeviceType.LIGHT_RGB || mAddDeviceType == DeviceType.SMART_RELAY ||
                    mAddDeviceType == DeviceType.SMART_CURTAIN || mAddDeviceType == 98 || mAddDeviceType == 99)
                scanning_num.text = getString(R.string.title_scanned_device_num, routerScanCount)
            else
                scanning_num.text = getString(R.string.scanning)
            routeTimerOut()
        }
    }

    @SuppressLint("CheckResult")
    private fun getScanRouterState() {
        RouterModel.getRouteScanningResult()?.subscribe({
            it.data?.let { it1 ->
                Constant.SCAN_SERID = it1.scanSerId.toLong()
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    private fun routeStartTimerOut(toLong: Long) {
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(toLong + 1L, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    isOffLine = false
                    stopScan()
                    ToastUtils.showShort(getString(R.string.router_scan_faile))
                }
    }

    @SuppressLint("CheckResult")
    private fun oldStartScan() {
        setScanningMode(true)
        TelinkLightService.Instance()?.idleMode(true)
        LogUtils.d("####TelinkBluetoothSDK 完整流程开始扫描------------ start scan idleMode true ####")
        startTimer()
        startAnimation()
        handleIfSupportBle()

        //断连后延时一段时间再开始扫描
        disposableConnectTimer?.dispose()
        val delay = if (RomUtils.isHuawei()) HUAWEI_DELAY else SCAN_DELAY
        disposableConnectTimer = Observable.timer(delay, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val mesh = mApplication?.mesh
                    //扫描参数
                    val params = LeScanParameters.create()
                    if (!AppUtils.isExynosSoc)
                        if (mAddDeviceType == DeviceType.NORMAL_SWITCH){
                            params.setScanFilters(getSwitchFilters())
                        }
                        else
                            params.setScanFilters(getFilters())

                    params.setMeshName(mesh?.factoryName)
                    params.setOutOfMeshName(Constant.OUT_OF_MESH_NAME)
                    params.setTimeoutSeconds(scanTimeoutTime.toInt())
                    params.setScanMode(true)

                    TelinkLightService.Instance()?.startScan(params)
                    bestRssiDevice = null
                }
    }

    private fun connectBestRssiDevice() {
        disposable?.dispose()
        val get = MeshAddressGenerator().meshAddress.get()
        var meshAddress = if (lastUser == null) {
            get
        } else {
            when {
                lastUser?.lastGenMeshAddr ?: 0 < get -> lastUser?.lastGenMeshAddr = get
                else -> lastUser?.lastGenMeshAddr
            }
            lastUser?.lastGenMeshAddr ?: 0
        }
        meshAddress = isUser(meshAddress)
        LogUtils.v("zcl-----------完整流程网关新的meshAddress-------$meshAddress----${bestRssiDevice != null}")
        disposable = Observable.timer(200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (bestRssiDevice != null) {
                        val mesh = mApplication!!.mesh
                        TelinkLightService.Instance().disconnect()
                        TelinkLightService.Instance()?.idleMode(true)
                        Thread.sleep(500)
                        updateMesh(bestRssiDevice!!, meshAddress, mesh)
                    } else {
                        retryScan()
                    }
                }
        startMeshTimeoutTimer()//更新mesh超时时间
    }

    @SuppressLint("CheckResult")
    private fun isUser(meshAddress: Int): Int {
        var mesh = meshAddress
        if (mesh == 0) mesh = 1
        while (meshList.contains(mesh)) {
            mesh += 1
        }
        // 添加/更新一个区域 http://dev.dadoutek.com/smartlight/region/add/{rid}  POST 更新区域
        //     * installMesh	否	string	默认mesh，目前固定dadousmart
        //     * installMeshPwd	否	string	默认meshPassword,目前固定123
        //     * name	否	string	区域的名称。默认”未命名区域”
        //     * lastGenMeshAddr	否	int	区域mesh累加, 不传默认0, 0不会进行处理
        //     *  "installMesh":"dadousmart",
        //     *  "installMeshPwd":"123456",
        //     *  "name":"a region",
        //     *  "lastGenMeshAddr": 0
        val dbRegion = DbRegion()
        dbRegion.installMesh = "dadousmart"
        dbRegion.installMeshPwd = "123"
        dbRegion.lastGenMeshAddr = mesh
        lastUser?.lastGenMeshAddr = mesh
        saveUser(lastUser!!)
        RegionModel.updateMesh((lastUser?.last_region_id ?: "0").toInt(), dbRegion)?.subscribe({
            LogUtils.v("zcl-----------上传最新mesh地址-------成功")
        }, {
            LogUtils.v("zcl-----------上传最新mesh地址-------失败")
        })
        return mesh
    }

    private fun getGwId(): Long {
        val list = DBUtils.getAllGateWay()
        val idList = ArrayList<Int>()
        for (i in list.indices)
            idList.add(list[i].id!!.toInt())

        var id = 0
        if (list.size == 0) {
            id = 1
        } else {
            for (i in 1..100000) {
                if (idList.contains(i)) {
                    Log.d("gwID", "getGwId: " + "aaaaa")
                    continue
                } else {
                    id = i
                    Log.d("sceneID", "getGwId: bbbbb$id")
                    break
                }
            }
        }
        return java.lang.Long.valueOf(id.toLong())
    }

//    private fun getFilters(): ArrayList<ScanFilter> {
//        val scanFilters = ArrayList<ScanFilter>()
//        val manuData = byteArrayOf(0, 0, 0, 0, 0, 0, mAddDeviceType.toByte())//转换16进制
////        val manuData = null //chown
//        val manuDataMask = byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())
//
//        val scanFilter = ScanFilter.Builder().setManufacturerData(VENDOR_ID, manuData, manuDataMask).build()
////        val scanFilter = ScanFilter.Builder().build();
//        scanFilters.add(scanFilter)
//        return scanFilters
//    }
    private fun getFilters(): ArrayList<ScanFilter> {
        val scanFilters = ArrayList<ScanFilter>()
        /*val manuData =      byteArrayOf(0, 0, 0, 0, 0, 0, mAddDeviceType.toByte())//转换16进制
        val manuDataMask =  byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())
        val scanFilter = ScanFilter.Builder().setManufacturerData(VENDOR_ID, manuData, manuDataMask).build()
        scanFilters.add(scanFilter)*/

        Log.d("Mack", "MobilePhoneBrand:"+ Build.BRAND)
        if (/*Build.BRAND.contains("samsung") && */  Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            scanFilters.add(ScanFilter.Builder().setManufacturerData(Constant.VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, 0x11.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        }
        scanFilters.add(ScanFilter.Builder().setManufacturerData(Constant.VENDOR_ID,
            byteArrayOf(0, 0, 0, 0, 0, 0, mAddDeviceType.toByte()),
            byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())

        return scanFilters
    }

    private fun getSwitchFilters(): MutableList<ScanFilter> {
        val scanFilters = ArrayList<ScanFilter>()
        Log.d("Mack", "MobilePhoneBrand:"+ Build.BRAND)
        LogUtils.v("=================================================================")
        if (/*Build.BRAND.contains("samsung") &&*/ Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            scanFilters.add(ScanFilter.Builder().setManufacturerData(Constant.VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, 0x11.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        }
        scanFilters.add(ScanFilter.Builder().setManufacturerData(VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.NORMAL_SWITCH.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        scanFilters.add(ScanFilter.Builder().setManufacturerData(VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.NORMAL_SWITCH2.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        scanFilters.add(ScanFilter.Builder().setManufacturerData(VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.EIGHT_SWITCH.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        scanFilters.add(ScanFilter.Builder().setManufacturerData(VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.DOUBLE_SWITCH.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        scanFilters.add(ScanFilter.Builder().setManufacturerData(VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.SCENE_SWITCH.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        scanFilters.add(ScanFilter.Builder().setManufacturerData(VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.SMART_CURTAIN_SWITCH.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        return scanFilters
    }

    private fun startAnimation() {
        lottieAnimationView?.playAnimation()
        lottieAnimationView?.visibility = View.VISIBLE
    }

    private fun handleIfSupportBle() {
        //检查是否支持蓝牙设备
        if (!LeBluetooth.getInstance().isSupport(applicationContext)) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show()
            doFinish()
            return
        }

        if (!LeBluetooth.getInstance().isEnabled) {
            LeBluetooth.getInstance().enable(applicationContext)
        }
    }

    /**
     * 处理扫描事件
     *  productUUID 区分设备类型
     * @param event
     */
    @SuppressLint("CheckResult")
    @Synchronized
    private fun onLeScan(event: LeScanEvent) {
        val deviceInfo = event.args
        LogUtils.v("获得扫描数据$deviceInfo")
        if (deviceInfo.productUUID == mAddDeviceType && deviceInfo.rssi < MAX_RSSI)
            if (bestRssiDevice == null || deviceInfo.rssi < bestRssiDevice!!.rssi)
                bestRssiDevice = deviceInfo
        connectBestRssiDevice()
    }

    private fun updateMesh(deviceInfo: DeviceInfo, meshAddress: Int, mesh: Mesh) {
        bestRssiDevice = null
        //更新参数
        updateMeshStatus = UPDATE_MESH_STATUS.UPDATING_MESH

        deviceInfo.meshAddress = meshAddress
        meshList.add(meshAddress)
        val params = Parameters.createUpdateParameters()
        params.setOldMeshName(mesh.factoryName)
        params.setOldPassword(mesh.factoryPassword)
        params.setNewMeshName(user?.controlMeshName)
        params.setNewPassword(NetworkFactory.md5(NetworkFactory.md5(user?.controlMeshName) + user?.controlMeshName).substring(0, 16))
        params.setUpdateDeviceList(deviceInfo)
        meshUpdateType = 0
        TelinkLightService.Instance()?.updateMesh(params)
        LogUtils.d("####TelinkBluetoothSDK 完整流程更新meshName--####")
        stopScanTimer()
        LogUtils.d("updateMesh: " + deviceInfo.meshAddress + "" +
                "--" + deviceInfo.macAddress + "--productUUID:" + deviceInfo.productUUID)
    }

    @SuppressLint("SetTextI18n")
    private fun onDeviceStatusChanged(event: DeviceEvent) {
        val deviceInfo = event.args
        when (deviceInfo.status) {
            LightAdapter.STATUS_UPDATE_MESH_COMPLETED -> {
                disposableUpMeshTimer?.dispose()
                LogUtils.v("zcl----------完整流程旧的收到更新mesh通知------进行重新扫描---------$deviceInfo")
                updateMeshAddrAfter(deviceInfo)
            }
            LightAdapter.STATUS_UPDATE_MESH_FAILURE -> {
                disposableUpMeshTimer?.dispose()
                retryScan()
            }

            LightAdapter.STATUS_LOGIN -> {
                if (startConnect)
                    onLogin()
            }
        }
    }

    private fun updateMeshAddrAfter(deviceInfo: DeviceInfo) {
        if (bestRssiDevice == null)
            bestRssiDevice = deviceInfo
        else
            if (bestRssiDevice!!.rssi < deviceInfo.rssi)
                bestRssiDevice = deviceInfo

        GlobalScope.launch { mApplication?.mesh?.saveOrUpdate(this@DeviceScanningNewActivity) }
        LogUtils.d("update mesh success meshAddress = ${deviceInfo.meshAddress}")
        val scannedDeviceItem = ScannedDeviceItem(deviceInfo, getString(R.string.not_grouped))
        LogUtils.v("zcl----版本赋值--" + deviceInfo.firmwareRevision + "===" + scannedDeviceItem.deviceInfo.firmwareRevision)
        //刚开始扫的设备mac是null所以不能mac去重
        mAddedDevices.add(scannedDeviceItem)
        Thread.sleep(500)
        updateDevice(scannedDeviceItem)

        //扫描出灯就设置为非首次进入
        if (isFirtst) {
            isFirtst = false
            SharedPreferencesHelper.putBoolean(this@DeviceScanningNewActivity, SplashActivity.IS_FIRST_LAUNCH, false)
        }
        // toolbarTv?.text = getString(R.string.title_scanned_device_num, mAddedDevices.size)
        if (mAddDeviceType != DeviceType.GATE_WAY)
            scanning_num.text = getString(R.string.title_scanned_device_num, mAddedDevices.size)

        updateMeshStatus = UPDATE_MESH_STATUS.SUCCESS
        mUpdateMeshRetryCount = 0
        mConnectRetryCount = 0
        LogUtils.d("####TelinkBluetoothSDK 完整流程更新meshName完成后是否开始重新扫描--####${mAddDeviceType != DeviceType.GATE_WAY}")
        if (mAddDeviceType != DeviceType.GATE_WAY)
            this.startScan()    //继续开始扫描设备
        else {
            btn_add_groups?.setText(R.string.config_gate_way)
            stopScan()
        }
    }

    private fun onLogin() {
        //进入分组
        if (meshList.size > mAddedDevices.size) {//如果生成的大于当前保存的找回设备
            meshList.removeAll(mAddedDevices.map { it.meshAddress })
            startToRecoverDevices()
        } else {
            skipeType()
        }
    }


    private fun skipeType() { //开关传感器不能批量也就不能使用找回
        when (mAddDeviceType) {
            DeviceType.NORMAL_SWITCH -> {
                when (bestRssiDevice?.productUUID) {//传感器开关单独扫描界面
                    DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2 -> startActivity<ConfigNormalSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false", "deviceType" to bestRssiDevice?.productUUID)
                    DeviceType.SCENE_SWITCH -> skipSwitch()
                    DeviceType.SMART_CURTAIN_SWITCH -> startActivity<ConfigCurtainSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false")
                    DeviceType.FOUR_SWITCH -> startActivity<ConfigFourSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false")
                    DeviceType.SIX_SWITCH -> startActivity<ConfigSixSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false")
                }
                hideLoadingDialog()
            }
            DeviceType.GATE_WAY -> {

                if (Constant.IS_ROUTE_MODE) {
                    skipeGw()
                } else {
                    if (TelinkLightApplication.getApp().isConnectGwBle) { //直连时候获取版本号
                        val disposable = Commander.getDeviceVersion(dbGw.meshAddr).subscribe({ s: String ->
                            dbGw.version = s
                            DBUtils.saveGateWay(dbGw, false)
                            skipeGw()
                        }, {
                            skipeGw()
                        })
                    }
                }
            }
            else -> skipeBatchActivity()
        }
    }

    private fun skipSwitch() {
        val version = bestRssiDevice?.firmwareRevision ?: ""
        when (bestRssiDevice?.productUUID) {
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2 -> {
                startActivity<ConfigNormalSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false", "version" to version)
            }
            DeviceType.DOUBLE_SWITCH -> {
                startActivity<DoubleTouchSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false", "version" to version)
            }
            DeviceType.SCENE_SWITCH -> {
                if (version.contains(DeviceType.EIGHT_SWITCH_VERSION))
                    startActivity<ConfigEightSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false", "version" to version)
                else
                    startActivity<ConfigSceneSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false", "version" to version)
            }

            DeviceType.EIGHT_SWITCH -> {
                startActivity<ConfigEightSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false", "version" to version)
            }
            DeviceType.SMART_CURTAIN_SWITCH -> {
                startActivity<ConfigCurtainSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false", "version" to version)
            }
        }
    }

    private fun startToRecoverDevices() {
        LogUtils.v("zcl------找回controlMeshName:${lastUser?.controlMeshName}")
        disposableTimer = Observable.timer(10, TimeUnit.SECONDS).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe {
                    hideLoadingDialog()
                    disposableFind?.dispose()
                    skipeType()
                }
        val scanFilter = com.polidea.rxandroidble2.scan.ScanFilter.Builder().setDeviceName(lastUser?.controlMeshName).build()
        val scanSettings = ScanSettings.Builder().build()
        disposableFind = RecoverMeshDeviceUtil.rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .observeOn(Schedulers.io())
                .map { //解析数据
                    val data = RecoverMeshDeviceUtil.parseData(it)
                    data
                }
                .filter {
                    addDevicesToDb(it)   //当保存数据库成功时，才发射onNext
                }
                .map { deviceInfo ->
                    mAddedDevices.add(ScannedDeviceItem(deviceInfo, deviceInfo.deviceName))
                    if (mAddDeviceType != DeviceType.GATE_WAY)
                        scanning_num.text = getString(R.string.title_scanned_device_num, mAddedDevices.size)
                    RecoverMeshDeviceUtil.count
                }
                .timeout(RecoverMeshDeviceUtil.SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS) {
                    it.onComplete()                     //如果过了指定时间，还搜不到缺少的设备，就完成
                }
                .observeOn(AndroidSchedulers.mainThread()).subscribe({
                    skipeAndDisposable()
                }, {
                    skipeAndDisposable()
                })
    }

    private fun addDevicesToDb(deviceInfo: DeviceInfo): Boolean {
        var isExist: Boolean = true//默认存在与服务器不添加
        if (meshList.contains(deviceInfo.meshAddress)) {
            val productUUID = deviceInfo.productUUID
            isExist = DBUtils.isDeviceExist(deviceInfo.meshAddress)
            if (!isExist) {
                meshList.remove(deviceInfo.meshAddress)
                when (productUUID) {
                    DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB -> {
                        val dbLightNew = DbLight()
                        dbLightNew.productUUID = productUUID
                        dbLightNew.connectionStatus = 0
                        dbLightNew.updateIcon()
                        dbLightNew.belongGroupId = DBUtils.groupNull?.id
                        dbLightNew.color = 0
                        dbLightNew.colorTemperature = 0
                        dbLightNew.meshAddr = deviceInfo.meshAddress

                        val string = if (productUUID == DeviceType.LIGHT_RGB) TelinkLightApplication.getApp().getString(R.string.normal_light)
                        else
                            TelinkLightApplication.getApp().getString(R.string.rgb_light)

                        dbLightNew.name = string + dbLightNew.meshAddr
                        dbLightNew.macAddr = deviceInfo.macAddress
                        dbLightNew.sixMac = deviceInfo.sixByteMacAddress
                        DBUtils.saveLight(dbLightNew, false)
                        LogUtils.d(String.format("create meshAddress=  %x", dbLightNew.meshAddr))
                        RecoverMeshDeviceUtil.count++
                        LogUtils.v("zcl找回------找回----------${RecoverMeshDeviceUtil.count}----普通灯$dbLightNew---")
                    }
                    DeviceType.SMART_RELAY -> {
                        val relay = DbConnector()
                        relay.productUUID = productUUID
                        relay.connectionStatus = 0
                        relay.updateIcon()
                        relay.belongGroupId = DBUtils.groupNull?.id
                        relay.color = 0
                        relay.meshAddr = deviceInfo.meshAddress
                        relay.name = TelinkLightApplication.getApp().getString(R.string.relay) + relay.meshAddr
                        relay.macAddr = deviceInfo.macAddress
                        DBUtils.saveConnector(relay, false)
                        LogUtils.d("create = $relay  " + relay.meshAddr)
                        RecoverMeshDeviceUtil.count++
                        LogUtils.v("zcl找回-------------${RecoverMeshDeviceUtil.count}-------relay----")
                    }

                    DeviceType.SMART_CURTAIN -> {
                        val curtain = DbCurtain()
                        curtain.productUUID = productUUID
                        curtain.connectionStatus = 0
                        curtain.updateIcon()
                        curtain.belongGroupId = DBUtils.groupNull?.id
                        curtain.meshAddr = deviceInfo.meshAddress
                        curtain.name = TelinkLightApplication.getApp().getString(R.string.curtain) + curtain.meshAddr
                        curtain.macAddr = deviceInfo.macAddress
                        DBUtils.saveCurtain(curtain, false)
                        LogUtils.d("create = $curtain  " + curtain.meshAddr)
                        RecoverMeshDeviceUtil.count++
                        LogUtils.v("zcl找回------------${RecoverMeshDeviceUtil.count}--------curtain---")
                    }
                    /*       DeviceType.NIGHT_LIGHT, DeviceType.SENSOR -> {
                               val sensor = DbSensor()
                               sensor.productUUID = productUUID
                               sensor.belongGroupId = DBUtils.groupNull?.id
                               sensor.meshAddr = deviceInfo.meshAddress
                               sensor.name = TelinkLightApplication.getApp().getString(R.string.device_name) + sensor.meshAddr
                               sensor.macAddr = deviceInfo.macAddress
                               DBUtils.saveSensor(sensor, false)
                               RecoverMeshDeviceUtil.count++
                               LogUtils.v("zcl找回--------------${RecoverMeshDeviceUtil.count}------sensor---")
                           }
                           DeviceType.DOUBLE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.SMART_CURTAIN_SWITCH, DeviceType.SCENE_SWITCH
                               , DeviceType.NORMAL_SWITCH2 -> {
                               val switch = DbSwitch()
                               switch.productUUID = productUUID
                               switch.belongGroupId = DBUtils.groupNull?.id
                               switch.meshAddr = deviceInfo.meshAddress
                               switch.name = TelinkLightApplication.getApp().getString(R.string.device_name) + switch.meshAddr
                               switch.macAddr = deviceInfo.macAddress
                               DBUtils.saveSwitch(switch, false)
                               RecoverMeshDeviceUtil.count++
                               LogUtils.v("zcl找回------------${RecoverMeshDeviceUtil.count}-------普通-switch-")
                           }
                           DeviceType.EIGHT_SWITCH -> {
                               val switch = DbEightSwitch()
                               switch.productUUID = productUUID
                               switch.meshAddr = deviceInfo.meshAddress
                               switch.name = TelinkLightApplication.getApp().getString(R.string.device_name) + switch.meshAddr
                               switch.macAddr = deviceInfo.macAddress
                               DBUtils.saveEightSwitch(switch, false)
                               RecoverMeshDeviceUtil.count++
                               LogUtils.v("zcl找回-----------${RecoverMeshDeviceUtil.count}--------8k-switch--")
                           }*/
                }
            }
        }
        return !isExist
    }

    private fun skipeAndDisposable() {
        disposableTimer?.dispose()
        hideLoadingDialog()
        skipeType()
    }

    @SuppressLint("CheckResult")
    private fun skipeBatchActivity() {
        hideLoadingDialog()
        if (Constant.IS_ROUTE_MODE) {//获取扫描数据
            getRouterScanResult()
        } else {
            val intent = Intent(this, BatchGroupFourDeviceActivity::class.java)
            DBUtils.saveRegion(lastMyRegion, true)
            var meshAddress = 0
            for (item in mAddedDevices) {
                val deviceInfo = item.deviceInfo
                when {
                    meshAddress == 0 -> meshAddress = deviceInfo.meshAddress
                    meshAddress < item.deviceInfo.meshAddress -> meshAddress = deviceInfo.meshAddress
                }
                mAddedDevicesInfos.add(item.deviceInfo)
            }
            lastMyRegion.lastGenMeshAddr = meshAddress
            if (!isFastDoubleClick(1000))
                skipeActivity(intent)
        }

        LogUtils.v("zcl------扫描设备类型$mAddDeviceType------------扫描个数${mAddedDevices.size}----${DBUtils.getAllCurtains()}")
    }

    @SuppressLint("CheckResult")
    private fun getRouterScanResult() {
        RouterModel.getRouteScanningResult()?.subscribe({ it ->
            LogUtils.v("zcl-----------收到路由获取服务器扫描结果-------$it")
            val data = it.data
            if (data != null && data.status == 0) {
                mAddedDevicesInfos.clear()
                when {
                    data.scannedData.isNotEmpty() -> makeDeviceAndSkipe(data)
                    else -> tellServerCanClear(false)
                }
                lastUser?.let {
                    SyncDataPutOrGetUtils.syncGetDataStart(it, object : SyncCallback {
                        override fun start() {}
                        override fun complete() {
                            LogUtils.v("zcl-----------收到路由下载数据成功-------")
                        }

                        override fun error(msg: String?) {}
                    })
                }
            } else {//如果还在扫描则重新计算超时时间
                if (isFinisAc)
                    finish()
                scanFail()
            }
        }, {
            LogUtils.v("zcl-----------$it-------")
        })
    }

    private fun makeDeviceAndSkipe(data: Data) {
        showLoadingDialog(resources.getString(R.string.please_wait))
        data.scannedData.forEach { x ->
            val deviceInfo = DeviceInfo()
            deviceInfo.id = x.id
            deviceInfo.index = x.index
            deviceInfo.sixByteMacAddress = x.macAddr
            deviceInfo.macAddress = x.macAddr
            deviceInfo.macAddress = x.macAddr
            deviceInfo.meshAddress = x.meshAddr
            deviceInfo.meshUUID = x.meshUUID
            deviceInfo.deviceName = x.name
            deviceInfo.productUUID = x.productUUID
            deviceInfo.uid = x.uid
            deviceInfo.firmwareRevision = x.version
            deviceInfo.colorTemperature = x.colorTemperature
            deviceInfo.color = x.color
            deviceInfo.brightness = x.brightness
            deviceInfo.boundMac = x.boundMac
            deviceInfo.belongRegionId = x.belongRegionId
            deviceInfo.belongGroupId = x.belongGroupId.toLong()
            mAddedDevicesInfos.add(deviceInfo)
        }
        LogUtils.v("zcl-----------收到路由重新包装deviceInfo个数-------${mAddedDevicesInfos.size}")
        tellServerCanClear(true)
    }

    @SuppressLint("CheckResult")
    private fun tellServerCanClear(isScanSucess: Boolean) {
        val subscribe = RouterModel.routeScanClear()
                ?.subscribe({
                    LogUtils.v("zcl-----------收到路由告诉服务器确认结果可以清除-------$it")
                    if (isScanSucess) {
                        val intent: Intent
                        when (mAddDeviceType) {
                            98 -> {
                                intent = Intent(this, SensorDeviceDetailsActivity::class.java)
                                intent.putExtra(Constant.DEVICE_TYPE, Constant.INSTALL_SENSOR)
                                getAndFinish()
                            }
                            99 -> {
                                intent = Intent(this, SwitchDeviceDetailsActivity::class.java)
                                intent.putExtra(Constant.DEVICE_TYPE, Constant.INSTALL_SWITCH)
                                getAndFinish()
                            }
                            else -> {
                                intent = Intent(this, BatchGroupFourDeviceActivity::class.java)
                                skipeActivity(intent)
                            }
                        }
                    } else scanFail()
                }) {
                    ToastUtils.showShort(it.message)
                }
    }

    private fun skipeActivity(intent: Intent) {
        intent.putParcelableArrayListExtra(Constant.DEVICE_NUM, mAddedDevicesInfos)
        intent.putExtra(Constant.DEVICE_TYPE, mAddDeviceType)
        startActivity(intent)
        hideLoadingDialog()
        finish()
    }

    private fun skipeGw() {
        hideLoadingDialog()
        TelinkLightApplication.getApp().isConnectGwBle = true
        val intent = Intent(this@DeviceScanningNewActivity, GwLoginActivity::class.java)
        intent.putExtra("data", dbGw)
        SharedPreferencesHelper.putBoolean(this, Constant.IS_GW_CONFIG_WIFI, false)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableFind?.dispose()
        disposableTimer?.dispose()
        mConnectDisposal?.dispose()
        isFinisAc = true
        routerStopScan()
        disposeAllSubscribe()
        mApplication?.removeEventListener(this)
        SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
    }

    private fun startMeshTimeoutTimer() {
        disposableUpMeshTimer?.dispose()
        disposableUpMeshTimer = Observable.timer(12000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (meshUpdateType == 0L)
                        runOnUiThread {
                            stopScanTimer()
                            //TelinkLightService.Instance()?.mAdapter?.stop()/此处导致整个扫描中断
                            retryScan()
                        }
                }
    }
}