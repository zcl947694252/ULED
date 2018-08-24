package com.dadoutek.uled.othersview

import android.Manifest
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
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import com.blankj.utilcode.util.ActivityUtils
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
import com.dadoutek.uled.util.LogUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.*
import com.telink.bluetooth.light.*
import com.telink.bluetooth.light.DeviceInfo
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.Strings
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar
import java.util.concurrent.TimeUnit

class MainActivity : TelinkMeshErrorDealActivity(), EventListener<String> {
    private var bestRSSIDevice: DeviceInfo? = null
    private val MAX_RETRY_CONNECT_TIME = 5
    private val CONNECT_TIMEOUT = 10
    private val SCAN_TIMEOUT_SECOND: Int = 10

    private lateinit var deviceFragment: DeviceListFragment
    private lateinit var groupFragment: GroupListFragment
    private lateinit var meFragment: MeFragment
    private lateinit var sceneFragment: SceneFragment

    //防止内存泄漏
    internal var mDisposable = CompositeDisposable()
    private var mApplication: TelinkLightApplication? = null
    private var connectMeshAddress: Int = 0
    private val mDelayHandler = Handler()
    private var retryConnectCount = 0

    private var mConnectSnackBar: Snackbar? = null
    private var mScanSnackBar: Snackbar? = null
    private var mNotFoundSnackBar: Snackbar? = null
    private var mConnectDisposal: Disposable? = null
    private var mScanTimeoutDisposal: Disposable? = null


    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)

                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        TelinkLightService.Instance().idleMode(true)
                        retryConnectCount = 0
                        startScan()
                    }
                    BluetoothAdapter.STATE_OFF -> {

                    }
                }
            }
        }
    }

    //记录用户首次点击返回键的时间
    private var firstTime: Long = 0

    private var mWakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        requestPermissions()
        initConnect()

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

    private fun requestPermissions() {
        val rxPermission = RxPermissions(this@MainActivity)
        rxPermission
                .requestEach(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                .subscribe { permission ->
                    if (permission.granted) {
                        // 用户已经同意该权限

                    } else if (permission.shouldShowRequestPermissionRationale) {
                        // 用户拒绝了该权限，没有选中『不再询问』（Never ask again）,那么下次再次启动时，还会提示请求权限的对话框

                    } else {
                        // 用户拒绝了该权限，并且选中『不再询问』

                    }
                }
    }

    private fun initConnect() {
        val list: List<DbLight> = DBUtils.allLight
        for (i in list.indices) {
            list[i].connectionStatus = 1
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
        bnve.enableAnimation(false)
        bnve.enableShiftingMode(false)
        bnve.enableItemShiftingMode(false)
        bnve.setupWithViewPager(viewPager)
    }

    override fun onPause() {
        super.onPause()
        mScanTimeoutDisposal?.dispose()
        mConnectDisposal?.dispose()
        mScanSnackBar?.dismiss()
        mNotFoundSnackBar?.dismiss()
        mConnectSnackBar?.dismiss()
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
        //检测service是否为空，为空则重启
        if (TelinkLightService.Instance() == null) {
            mApplication!!.startLightService(TelinkLightService::class.java)
        }

        if (mWakeLock != null) {
            mWakeLock?.acquire()
        }


    }

    override fun onPostResume() {
        super.onPostResume()
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
                    if (TelinkLightService.Instance().adapter.mLightCtrl.currentLight?.isConnected != true) {
                        while (TelinkApplication.getInstance()?.serviceStarted == true) {
                            launch(UI) {
                                retryConnectCount = 0
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

    private fun addScanListeners() {
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)
    }

    fun addEventListeners() {
        // 监听各种事件
        addScanListeners()
        this.mApplication!!.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication!!.addEventListener(NotificationEvent.ONLINE_STATUS, this)
//        this.mApplication!!.addEventListener(NotificationEvent.GET_ALARM, this)
        this.mApplication!!.addEventListener(NotificationEvent.GET_DEVICE_STATE, this)
        this.mApplication!!.addEventListener(ServiceEvent.SERVICE_CONNECTED, this)
//        this.mApplication!!.addEventListener(MeshEvent.OFFLINE, this)
        this.mApplication!!.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }


    private fun startConnectTimer() {
        mConnectDisposal?.dispose()
        mConnectDisposal = Observable.timer(CONNECT_TIMEOUT.toLong(), TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    retryConnect()
                }
    }

    private fun stopConnectTimer() {
        mConnectDisposal?.dispose()
    }

    private fun connect(mac: String) {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN).subscribe(Consumer {
            if (it) {
                //授予了权限
                if (TelinkLightService.Instance() != null) {
                    progressBar?.visibility = View.VISIBLE
//                    TelinkLightService.Instance().idleMode(false)

//                    SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)
//
//                    val mesh = this.mApplication?.mesh
//
//                    val account = DBUtils.getLastUser().account
//                    //自动重连参数
//                    val connectParams = Parameters.createAutoConnectParameters()
//                    connectParams.setMeshName(account)
//                    connectParams.setPassword(NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16))
////                    LogUtils.d("connect pwd = ${NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16)}")
//                    connectParams.autoEnableNotification(true)
//                    connectParams.setTimeoutSeconds(CONNECT_TIMEOUT)
//                    connectParams.setConnectMac(mac)

//                    TelinkLightService.Instance().autoConnect(connectParams)
                    TelinkLightService.Instance().connect(mac, CONNECT_TIMEOUT)
                    startConnectTimer()

                    if (mConnectSnackBar?.isShown != true)
                        mConnectSnackBar = indefiniteSnackbar(root, getString(R.string.connecting))

                }
            } else {
                //没有授予权限
                DialogUtils.showNoBlePermissionDialog(this, { connect(mac) }, { finish() })
            }
        })
    }


    private fun startScan() {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN).subscribe {
            if (it) {
                TelinkLightService.Instance().idleMode(true)
                bestRSSIDevice = null
                //扫描参数
                val account = DBUtils.lastUser!!.account
                val params = LeScanParameters.create()

                params.setMeshName(account)
                params.setOutOfMeshName(account)
                params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                params.setScanMode(true)

                addScanListeners()
                TelinkLightService.Instance().startScan(params)
//                startCheckRSSITimer()

                if (mConnectSnackBar?.isShown != true && mScanSnackBar?.isShown != true)
                    mScanSnackBar = indefiniteSnackbar(root, getString(R.string.scanning_devices))

            } else {
                //没有授予权限
                DialogUtils.showNoBlePermissionDialog(this, {
                    retryConnectCount = 0
                    startScan()
                }, { finish() })
            }
        }
    }


    private fun startCheckRSSITimer() {
        mScanTimeoutDisposal?.dispose()
        mScanTimeoutDisposal = Observable.timer(SCAN_TIMEOUT_SECOND.toLong(), TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (bestRSSIDevice != null) {
                        LeBluetooth.getInstance().stopScan()
                        connect(bestRSSIDevice!!.macAddress)
                    } else {
                        onScanTimeout()
                    }
                }
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                TelinkLightService.Instance().enableNotification()
                launch(UI) {
                    stopConnectTimer()
                    if (progressBar?.visibility != View.GONE)
                        progressBar?.visibility = View.GONE

                    mScanSnackBar?.dismiss()
                    mConnectSnackBar?.dismiss()
                    delay(300)
                    snackbar(root, R.string.connect_success)
                }

                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, true)
                val connectDevice = this.mApplication!!.connectDevice
                if (connectDevice != null) {
                    this.connectMeshAddress = connectDevice.meshAddress
                }
            }
            LightAdapter.STATUS_CONNECTED -> {
                login()
            }
            LightAdapter.STATUS_ERROR_N -> onNError(event)

        }
    }

    private fun login() {
        val account = DBUtils.lastUser!!.account
        val pwd = NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16)
        TelinkLightService.Instance().login(Strings.stringToBytes(account, 16)
                , Strings.stringToBytes(pwd, 16))
    }

    private fun onNError(event: DeviceEvent) {

//        ToastUtils.showLong(getString(R.string.connect_fail))
        SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

        TelinkLightService.Instance().idleMode(true)
        TelinkLog.d("DeviceScanningActivity#onNError")

        val builder = AlertDialog.Builder(this)
        builder.setMessage("当前环境:Android7.0!连接重试:" + " 3次失败!")
        builder.setNegativeButton("confirm") { dialog, _ -> dialog.dismiss() }
        builder.setCancelable(false)
        builder.show()
    }

    /**
     * 重试
     * 先扫描到信号比较好的，再连接。
     */
    private fun retryConnect() {
        if (retryConnectCount < MAX_RETRY_CONNECT_TIME) {
            retryConnectCount++
            if (TelinkLightService.Instance().adapter.mLightCtrl.currentLight?.isConnected != true)
                startScan()
            else
                login()
        } else {
            TelinkLightService.Instance().idleMode(true)
            if (mNotFoundSnackBar?.isShown != true) {
                mNotFoundSnackBar = indefiniteSnackbar(root,
                        R.string.not_found_light, R.string.retry) {
                    retryConnectCount = 0
                    startScan()
                }
            }

        }
    }


    /**
     * 检查是不是灯
     */
    private fun checkIsLight(info: OnlineStatusNotificationParser.DeviceNotificationInfo?): Boolean {
        if (info != null) {
            when (info.reserve) {
                UpdateStatusDeviceType.OLD_NORMAL_SWITCH.toInt() -> return false
                UpdateStatusDeviceType.OLD_NORMAL_SWITCH2.toInt() -> return false
                UpdateStatusDeviceType.OLD_NORMAL_SWITCH3.toInt() -> return false
                UpdateStatusDeviceType.OLD_NORMAL_PIR.toInt() -> return false
                else -> return true
            }
        }
        return true
    }


    /**
     * 处理[NotificationEvent.ONLINE_STATUS]事件
     */
    @Synchronized
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
                    DBUtils.updateLightLocal(dbLight)
                    runOnUiThread { deviceFragment.notifyDataSetChanged() }
                } else {
                    if (connectionStatus != ConnectionStatus.OFFLINE) {
                        val dbLightNew = DbLight()
                        dbLightNew.setConnectionStatus(connectionStatus.value)
                        dbLightNew.updateIcon()
                        dbLightNew.belongGroupId = DBUtils.groupNull!!.id
                        dbLightNew.brightness = brightness
                        dbLightNew.colorTemperature = 0
                        dbLightNew.meshAddr = meshAddress
                        dbLightNew.name = getString(R.string.allLight)
                        dbLightNew.macAddr = "0"
                        DBUtils.saveLight(dbLightNew, false)

                        com.dadoutek.uled.util.LogUtils.d("creat_light" + dbLightNew.meshAddr)
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
//            LeScanEvent.LE_SCAN_COMPLETED -> onLeScanTimeout()
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
        LogUtils.d("onErrorReport: onLeScanTimeout")
//        if (mConnectSnackBar) {
            indefiniteSnackbar(root, R.string.not_found_light, R.string.retry) {
                TelinkLightService.Instance().idleMode(true)
                LeBluetooth.getInstance().stopScan()
                startScan()
            }
//        } else {
//            retryConnect()
//        }

    }

    private fun onScanTimeout() {
        com.blankj.utilcode.util.LogUtils.d("onScanTimeout")
        retryConnect()
//        indefiniteSnackbar(root, R.string.not_found_light, R.string.retry) {
//            TelinkLightService.Instance().idleMode(true)
//            LeBluetooth.getInstance().stopScan()
//            startScan()
//        }
    }

    private fun isSwitch(uuid: Int): Boolean {
        return when (uuid) {
            DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2,DeviceType.SENSOR -> {
                LogUtils.d("This is switch")
                true
            }
            else -> {
                false

            }
        }
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

        if (!isSwitch(deviceInfo.productUUID)) {
            connect(deviceInfo.macAddress)
//            if (bestRSSIDevice != null) {
//                //扫到的灯的信号更好并且没有连接失败过就把要连接的灯替换为当前扫到的这个。
//                if (deviceInfo.rssi > bestRSSIDevice!!.rssi) {
//                    LogUtils.d("change to device with better RSSI  new meshAddr = ${deviceInfo.meshAddress} rssi = ${deviceInfo.rssi}")
//                    bestRSSIDevice = deviceInfo
//                }
//            } else {
//                bestRSSIDevice = deviceInfo
//            }

        }

    }

    private fun checkIsLightForScan(productUUID: Int): Boolean {
        return !(productUUID == DeviceType.NORMAL_SWITCH ||
                productUUID == DeviceType.NORMAL_SWITCH2 ||
                productUUID == DeviceType.SCENE_SWITCH ||
                productUUID == DeviceType.SENSOR)
    }

    override fun onLocationEnable() {
//        connect()
    }

    private fun onErrorReport(info: ErrorReportInfo) {
//        LogUtils.d("onErrorReport current device mac = ${bestRSSIDevice?.macAddress}")
//        if (bestRSSIDevice != null) {
//            connectFailedDeviceMacAddr.add(bestRSSIDevice!!.macAddress)
//        }

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
                retryConnect()

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
                retryConnect()

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


}
