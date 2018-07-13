package com.dadoutek.uled.othersview

import android.Manifest
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.group.GroupListFragment
import com.dadoutek.uled.light.DeviceListFragment
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.scene.SceneFragment
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.tellink.TelinkMeshErrorDealActivity
import com.dadoutek.uled.util.BleUtils
import com.dadoutek.uled.util.DialogUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.*
import com.telink.bluetooth.light.*
import com.telink.bluetooth.light.DeviceInfo
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar
import java.util.concurrent.TimeUnit

class MainActivity : TelinkMeshErrorDealActivity(), EventListener<String> {
    private var bestRSSIDevice: DeviceInfo? = null
    private val MAX_RETRY_CONNECT_TIME = 3
    private val CONNECT_TIMEOUT = 10
    private val SCAN_TIMEOUT_SECOND: Int = 10
    val CHECK_RSSI_TIMEOUT: Long = 1500

    private val loadDialog: Dialog? = null
    //    private var fragmentManager: FragmentManager? = null
    private lateinit var deviceFragment: DeviceListFragment
    private lateinit var groupFragment: GroupListFragment
    private lateinit var meFragment: MeFragment
    private lateinit var sceneFragment: SceneFragment

    private var mContent: Fragment? = null
    private val lights = Lights.getInstance()
    //防止内存泄漏
    internal var mDisposable = CompositeDisposable()


    private var mApplication: TelinkLightApplication? = null

    private var connectMeshAddress: Int = 0

    private val mDelayHandler = Handler()
    private var mConnectingSnackbar: Snackbar? = null   //用来判断要不要重试的,如果跑到过CONNECTING的状态才重试

    private var retryConnectCount = 0
    private var snackbarScanning: Snackbar? = null
    private var snackbarNotFoundLight: Snackbar? = null

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)

                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        Log.d(TAG, getString(R.string.open_blutooth))
                        TelinkLightService.Instance().idleMode(true)
                        startScan()
                    }
                    BluetoothAdapter.STATE_OFF -> Log.d(TAG, getString(R.string.close_bluetooth))
                }
            }
        }
    }


    internal var PERMISSION_REQUEST_CODE = 0x10

    internal var mTimeoutBuilder: AlertDialog.Builder? = null

    //记录用户首次点击返回键的时间
    private var firstTime: Long = 0

    private var mWakeLock: PowerManager.WakeLock? = null

    override fun onStart() {
        super.onStart()
        initConnect()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock")

        this.setContentView(R.layout.activity_main)
        this.mApplication = this.application as TelinkLightApplication
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
        registerReceiver(mReceiver, filter)

        initBottomNavigation()
    }

    private fun initConnect() {
        val list: List<DbLight> = DBUtils.getAllLight()
        for (i in list.indices) {
            list[i].connectionStatus = 2
            DBUtils.updateLight(list[i])
        }
    }

    private fun initBottomNavigation() {
        deviceFragment = DeviceListFragment()
        groupFragment = GroupListFragment()
        sceneFragment = SceneFragment()
        meFragment = MeFragment()
        val fragments: List<Fragment> = listOf(groupFragment, sceneFragment, meFragment)
        val vpAdapter = ViewPagerAdapter(supportFragmentManager, fragments)
        viewPager.adapter = vpAdapter
        //禁止所有动画效果
        bnve.enableAnimation(false);
        bnve.enableShiftingMode(false);
        bnve.enableItemShiftingMode(false);
        bnve.setupWithViewPager(viewPager)
    }

    override fun onPause() {
        super.onPause()

        snackbarScanning?.dismiss()
        progressBar.visibility = View.GONE

        //移除事件
        this.mApplication!!.removeEventListener(this)
        stopConnectTimer()

        if (mWakeLock != null) {
            mWakeLock?.acquire()
        }
    }


    override fun onResume() {
        super.onResume()

        if (mWakeLock != null) {
            mWakeLock?.acquire()
        }

        addEventListeners()
        //检查是否支持蓝牙设备
        if (!LeBluetooth.getInstance().isSupport(applicationContext)) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show()
            ActivityUtils.finishAllActivities()
        } else {  //如果蓝牙没开，则弹窗提示用户打开蓝牙
            if (!LeBluetooth.getInstance().isEnabled) {
                indefiniteSnackbar(root, R.string.openBluetooth, R.string.btn_ok) {
                    LeBluetooth.getInstance().enable(applicationContext)
                }
            } else {
                //如果位置服务没打开，则提示用户打开位置服务
                if (!BleUtils.isLocationEnable(this)) {
                    ToastUtils.showShort(R.string.open_location_service)

                    indefiniteSnackbar(root, R.string.open_location_service, R.string.btn_ok) {
                        BleUtils.jumpLocationSetting()
                    }
                } else {
                    if (TelinkLightApplication.getInstance().connectDevice == null) {
                        while (TelinkApplication.getInstance()?.serviceStarted == true) {
                            launch(UI) {
                                startScan()
                            }
                            break;
                        }

                    } else {
                        progressBar?.visibility = View.GONE
                        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.CONNECT_STATE_SUCCESS_KEY, true);
                    }

                }
            }

        }


        val deviceInfo = this.mApplication!!.connectDevice

        if (deviceInfo != null) {
            this.connectMeshAddress = this.mApplication!!.connectDevice.meshAddress and 0xFF
        }

    }

    override fun onStop() {
        super.onStop()
        if (TelinkLightService.Instance() != null)
            TelinkLightService.Instance().disableAutoRefreshNotify()
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
        this.mDelayHandler.removeCallbacksAndMessages(null)
        Lights.getInstance().clear()
        mDisposable.dispose()
    }


    fun addEventListeners() {
        // 监听各种事件
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN, this);
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this);
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this);
        this.mApplication!!.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication!!.addEventListener(NotificationEvent.ONLINE_STATUS, this)
//        this.mApplication!!.addEventListener(NotificationEvent.GET_ALARM, this)
        this.mApplication!!.addEventListener(NotificationEvent.GET_DEVICE_STATE, this)
        this.mApplication!!.addEventListener(ServiceEvent.SERVICE_CONNECTED, this)
//        this.mApplication!!.addEventListener(MeshEvent.OFFLINE, this)
        this.mApplication!!.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }


    private var mConnectTimer: CompositeDisposable = CompositeDisposable()


    private fun startConnectTimer() {
        //如果超过 delaySeconds 还没有变为连接中状态，则显示为超时
        Observable.timer(CONNECT_TIMEOUT.toLong(), TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (mConnectingSnackbar != null) {
                        retryConnect()
                    } else {
                        progressBar.visibility = View.GONE
                        if (snackbarNotFoundLight == null) {
                            indefiniteSnackbar(root, R.string.not_found_light, R.string.retry) {
                                TelinkLightService.Instance().idleMode(true)
                                LeBluetooth.getInstance().stopScan()
                                startScan()
                            }
                        }

                    }
                }, {
                    LogUtils.d("timer error = ${it.message}")
                }, {
                    //                    LogUtils.d("onComplete")
                }, {
                    //                    LogUtils.d("onSubscribe")
                    mConnectTimer.add(it)
                })
    }

    private fun stopConnectTimer() {
//        LogUtils.d("mConnectTimer?.dispose() mConnectTimer = ${mConnectTimer.size()}")
        mConnectTimer.clear()
    }


    /**
     * 自动重连
     */
    private fun connect(mac: String) {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN).subscribe(Consumer {
            if (it) {
                //授予了权限
                if (TelinkLightService.Instance() != null) {
                    progressBar?.visibility = View.VISIBLE
                    mConnectingSnackbar = null
                    TelinkLightService.Instance().adapter.mode = LightAdapter.MODE_IDLE

                    SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

                    val mesh = this.mApplication?.mesh

                    val account = DBUtils.getLastUser().account
                    //自动重连参数
                    val connectParams = Parameters.createAutoConnectParameters()
                    connectParams.setMeshName(account)
                    connectParams.setPassword(NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16))
//                    LogUtils.d("connect pwd = ${NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16)}")
                    connectParams.autoEnableNotification(true)
                    connectParams.setTimeoutSeconds(CONNECT_TIMEOUT)
                    connectParams.setConnectMac(mac)

                    TelinkLightService.Instance().idleMode(true)
                    TelinkLightService.Instance().autoConnect(connectParams)
                    startConnectTimer()

                    mConnectingSnackbar = indefiniteSnackbar(root, getString(R.string.connecting))

                }
            } else {
                //没有授予权限
                DialogUtils.showNoBlePermissionDialog(this, { connect(mac) }, { finish() })
            }
        })
    }

/*
    private fun switchContent(from: Fragment?, to: Fragment?) {

        if (this.mContent !== to) {
            this.mContent = to

            val transaction = this.fragmentManager!!
                    .beginTransaction()

            if (!to!!.isAdded) {
                transaction.hide(from).add(R.id.content, to)
            } else {
                transaction.hide(from).show(to)
            }

            transaction.commit()
        }
    }
*/

    private fun startScan() {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN).subscribe {
            if (it) {
                bestRSSIDevice = null
                //扫描参数
                val account = DBUtils.getLastUser().account
                val params = LeScanParameters.create()
                params.setMeshName(account)
                params.setOutOfMeshName(account)
                params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                params.setScanMode(false)
                TelinkLightService.Instance().startScan(params)
                startCheckRSSITimer()

                snackbarNotFoundLight = null
                snackbarScanning = indefiniteSnackbar(root, getString(R.string.scanning_devices))

            } else {
                //没有授予权限
                DialogUtils.showNoBlePermissionDialog(this, { startScan() }, { finish() })
            }
        }
    }

    private fun startCheckRSSITimer() {
        Observable.timer(CHECK_RSSI_TIMEOUT, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (bestRSSIDevice != null) {
                        LogUtils.d("connect to meshAddr = ${bestRSSIDevice!!.meshAddress}  rssi = ${bestRSSIDevice!!.rssi}")
                        LeBluetooth.getInstance().stopScan()
                        connect(bestRSSIDevice!!.macAddress)
                    } else {
                        startCheckRSSITimer()
                    }
                }
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                launch(UI) {
                    retryConnectCount = 0

                    stopConnectTimer()
                    if (progressBar?.visibility != View.GONE)
                        progressBar?.visibility = View.GONE
                    snackbar(root, R.string.connect_success)
                }


                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, true)
                val connectDevice = this.mApplication!!.connectDevice
                if (connectDevice != null) {
                    this.connectMeshAddress = connectDevice.meshAddress
                    if (TelinkLightService.Instance().mode == LightAdapter.MODE_AUTO_CONNECT_MESH) {
//                        mHandler.postDelayed({
//                            TelinkLightService.Instance().sendCommandNoResponse(0xE4.toByte(),
//                                    0xFFFF, byteArrayOf())
//                        }, (3 * 1000).toLong())

                    }

                    if (TelinkLightApplication.getApp().mesh.isOtaProcessing && foreground) {
                        startActivity(Intent(this, OTAUpdateActivity::class.java)
                                .putExtra(OTAUpdateActivity.INTENT_KEY_CONTINUE_MESH_OTA, OTAUpdateActivity.CONTINUE_BY_PREVIOUS))
                    }
                }
            }
            LightAdapter.STATUS_CONNECTING, LightAdapter.STATUS_CONNECTED -> {
//                runOnUiThread {
//                    if (mConnectingSnackbar == null)
//                        mConnectingSnackbar = indefiniteSnackbar(root, getString(R.string.connecting))
//
//                }

            }
            LightAdapter.STATUS_LOGOUT -> {
                onLogout()
            }

            LightAdapter.STATUS_ERROR_N -> onNError(event)


        }//                this.showToast("login");
    }

    private fun onNError(event: DeviceEvent) {

//        ToastUtils.showLong(getString(R.string.connect_fail))
        SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

        TelinkLightService.Instance().idleMode(true)
        TelinkLog.d("DeviceScanningActivity#onNError")

        val builder = AlertDialog.Builder(this)
        builder.setMessage("当前环境:Android7.0!连接重试:" + " 3次失败!")
        builder.setNegativeButton("confirm") { dialog, which -> dialog.dismiss() }
        builder.setCancelable(false)
        builder.show()
    }

    /**
     * 重试
     * 先扫描到信号比较好的，再连接。
     */
    private fun retryConnect() {
        stopConnectTimer()
        if (retryConnectCount < MAX_RETRY_CONNECT_TIME) {
            retryConnectCount++
            startScan()
        } else {
            TelinkLightService.Instance().idleMode(true)

            if (snackbarNotFoundLight == null) {
                indefiniteSnackbar(root, R.string.not_found_light, R.string.retry) {
                    startScan()
                }
            }

        }
    }


    private fun onLogout() {
        LogUtils.d("onLogout")
        retryConnect()
    }

    private fun onAlarmGet(notificationEvent: NotificationEvent) {
        val info = GetAlarmNotificationParser.create().parse(notificationEvent.args)
        if (info != null)
            TelinkLog.d("alarm info index: " + info.index)


    }


    /**
     * 检查是不是灯
     */
    private fun checkIsLight(info: OnlineStatusNotificationParser.DeviceNotificationInfo?): Boolean {
        if (info != null) {
            when (info.reserve) {
                UpdateStatusDeviceType.OLD_NORMAL_SWITCH.toInt() -> return false
                UpdateStatusDeviceType.OLD_NORMAL_SWITCH2.toInt() -> return false
            //                case DeviceType.NORMAL_SWITCH2:
            //                    return false;
            //                case DeviceType.SCENE_SWITCH:
            //                    return false;
                else -> return true
            }
        }
        return true
    }


    /**
     * 处理[NotificationEvent.ONLINE_STATUS]事件
     */
    private fun onOnlineStatusNotify(event: NotificationEvent) {
        val notificationInfoList: List<OnlineStatusNotificationParser.DeviceNotificationInfo>?

        notificationInfoList = event.parse() as List<OnlineStatusNotificationParser.DeviceNotificationInfo>

        if (notificationInfoList.isEmpty()) {
            LogUtils.d("notificationInfoList is empty")
            return
        }

        for (notificationInfo: OnlineStatusNotificationParser.DeviceNotificationInfo in notificationInfoList) {

            val meshAddress = notificationInfo.meshAddress
            val brightness = notificationInfo.brightness
            val connectionStatus = notificationInfo.connectionStatus
//            Log.d("Saw", "meshAddress = " + meshAddress + "  reserve = " + notificationInfo.reserve +
//                    " status = " + notificationInfo.status + " connectionStatus = " + notificationInfo.connectionStatus.value)
            if (checkIsLight(notificationInfo)) {
                val dbLight = DBUtils.getLightByMeshAddr(meshAddress)
                if (dbLight != null) {
                    dbLight.connectionStatus = connectionStatus.value
                    dbLight.updateIcon()
                    DBUtils.updateLight(dbLight)
                    runOnUiThread { deviceFragment.notifyDataSetChanged() }
                } else {
                    if(connectionStatus!=ConnectionStatus.OFFLINE){
                        val dbLightNew = DbLight()
                        dbLightNew.setConnectionStatus(connectionStatus.value)
                        dbLightNew.updateIcon()
                        dbLightNew.belongGroupId = DBUtils.getGroupNull().id
                        dbLightNew.brightness = brightness
                        dbLightNew.colorTemperature = 0
                        dbLightNew.meshAddr = meshAddress
                        dbLightNew.name = getString(R.string.allLight)
                        dbLightNew.macAddr = "0"
                        DBUtils.saveLight(dbLightNew, false)

                        com.dadoutek.uled.util.LogUtils.d("creat_light"+dbLightNew.meshAddr)
                    }
                }
            }

        }

    }

    private fun onServiceConnected(event: ServiceEvent) {
//        LogUtils.d("onServiceConnected")
    }

    private fun onServiceDisconnected(event: ServiceEvent) {
        LogUtils.d("onServiceDisconnected")
        TelinkLightApplication.getInstance().startLightService(TelinkLightService::class.java)
    }


    /**
     * 事件处理方法
     *
     * @param event
     */
    override fun performed(event: Event<String>) {
        when (event.type) {
            LeScanEvent.LE_SCAN -> onLeScan(event as LeScanEvent)
            LeScanEvent.LE_SCAN_TIMEOUT -> onLeScanTimeout()
            LeScanEvent.LE_SCAN_COMPLETED -> onLeScanTimeout()
            NotificationEvent.ONLINE_STATUS -> this.onOnlineStatusNotify(event as NotificationEvent)
            NotificationEvent.GET_ALARM -> {
            }
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
//            MeshEvent.OFFLINE -> this.onMeshOffline(event as MeshEvent)
            ServiceEvent.SERVICE_CONNECTED -> this.onServiceConnected(event as ServiceEvent)
            ServiceEvent.SERVICE_DISCONNECTED -> this.onServiceDisconnected(event as ServiceEvent)
            NotificationEvent.GET_DEVICE_STATE -> onNotificationEvent(event as NotificationEvent)
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
            }
        }
    }


    /**
     * 扫描不到任何设备了
     * （扫描结束）
     */
    private fun onLeScanTimeout() {

        if (snackbarNotFoundLight == null) {
            indefiniteSnackbar(root, R.string.not_found_light, R.string.retry) {
                TelinkLightService.Instance().idleMode(true)
                LeBluetooth.getInstance().stopScan()
                startScan()
            }
        }

    }

    private fun isSwitch(uuid: Int): Boolean{
        var result = false
        when (uuid) {
            DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2 -> {
                com.dadoutek.uled.util.LogUtils.d("This is switch")
                result = true
            }
            else -> {
                result = false
            }
        }
        return result
    }

    /**
     * 处理扫描事件
     *
     * @param event
     */
    @Synchronized
    private fun onLeScan(event: LeScanEvent) {
        val mesh = this.mApplication?.mesh
        val meshAddress = mesh?.generateMeshAddr()
        val deviceInfo: DeviceInfo = event.args


        if (!isSwitch(deviceInfo.productUUID)){
            if (bestRSSIDevice != null) {
                if (deviceInfo.rssi > bestRSSIDevice!!.rssi) {
                    LogUtils.d("change to device with better RSSI  new meshAddr = ${deviceInfo.meshAddress} rssi = ${deviceInfo.rssi}")
                    bestRSSIDevice = deviceInfo
                }
            } else {
                bestRSSIDevice = deviceInfo
            }

        }

    }

    override fun onLocationEnable() {
//        connect()
    }

    private fun onErrorReport(info: ErrorReportInfo) {
//        retryConnect()
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        LogUtils.d("蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        LogUtils.d("无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        LogUtils.d("未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        LogUtils.d("未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        LogUtils.d("未建立物理连接")

                    }
                }

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        LogUtils.d("value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        LogUtils.d("read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        LogUtils.d("write login data 没有收到response")
                    }

                }

            }
        }
    }


    private fun onNotificationEvent(event: NotificationEvent) {
        if (!foreground) return
        // 解析版本信息
        val data = event.args.params
        if (data[0] == NotificationEvent.DATA_GET_MESH_OTA_PROGRESS) {
            TelinkLog.w("mesh ota progress: " + data[1])
            val progress = data[1].toInt()
            if (progress != 100) {
                startActivity(Intent(this, OTAUpdateActivity::class.java)
                        .putExtra(OTAUpdateActivity.INTENT_KEY_CONTINUE_MESH_OTA, OTAUpdateActivity.CONTINUE_BY_REPORT)
                        .putExtra("progress", progress))
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
            val secondTime = System.currentTimeMillis()
            if (secondTime - firstTime > 2000) {
                Toast.makeText(this@MainActivity, R.string.click_double_exit, Toast.LENGTH_SHORT).show()
                firstTime = secondTime
                return true
            } else {
//                System.exit(0)
                ActivityUtils.finishAllActivities(true)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {

        private val TAG = MainActivity::class.java.simpleName

        private val UPDATE_LIST = 0

        fun getAlarm() {
            TelinkLightService.Instance().sendCommandNoResponse(0xEC.toByte(), 0x0000, byteArrayOf(0x10, 0x00.toByte()))
        }
    }


}
