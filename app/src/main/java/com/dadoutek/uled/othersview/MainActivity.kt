package com.dadoutek.uled.othersview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.allenliu.versionchecklib.v2.AllenVersionChecker
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.AppUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.device.DeviceFragment
import com.dadoutek.uled.fragment.MeFragment
import com.dadoutek.uled.group.GroupListFragment
import com.dadoutek.uled.group.GroupOTAListActivity
import com.dadoutek.uled.intf.CallbackLinkMainActAndFragment
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Constant.DEFAULT_MESH_FACTORY_NAME
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Lights
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.httpModel.RegionModel
import com.dadoutek.uled.model.httpModel.UpdateModel
import com.dadoutek.uled.model.httpModel.UserModel
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.mqtt.IGetMessageCallBack
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.router.RouterOtaActivity
import com.dadoutek.uled.scene.SceneFragment
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.event.NotificationEvent
import com.telink.bluetooth.event.ServiceEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LeScanParameters
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.Strings
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivity
import java.util.ArrayList
import java.util.concurrent.TimeUnit

/**
 * 首页  修改mes那么后  如果旧有用户不登陆 会报错直接崩溃 因为调用后的mesname是空的
 *
 * 首页设备
 */

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1

class MainActivity : TelinkBaseActivity(), EventListener<String>, CallbackLinkMainActAndFragment, IGetMessageCallBack {
    private var mTelinkLightService: TelinkLightService? = null
    private var mConnectDisposal: Disposable? = null
    private var mScanTimeoutDisposal: Disposable? = null
    private val connectFailedDeviceMacList: MutableList<String> = mutableListOf()
    private var bestRSSIDevice: DeviceInfo? = null

    private val REQUEST_ENABLE_BT: Int = 1200
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mApp: TelinkLightApplication? = null
    private val mCompositeDisposable = CompositeDisposable()
    private var disposableCamera: Disposable? = null
    private lateinit var deviceFragment: DeviceFragment
    private lateinit var groupFragment: GroupListFragment
    private lateinit var meFragment: MeFragment
    private lateinit var sceneFragment: SceneFragment

    //防止内存泄漏
    internal var mDisposable = CompositeDisposable()
    private var connectMeshAddress: Int = 0
    private val mDelayHandler = Handler()
    private var retryConnectCount = 0

    //记录用户首次点击返回键的时间
    private var firstTime: Long = 0

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                    BluetoothAdapter.STATE_ON -> {
                        retryConnectCount = 0
                        autoConnect()
                    }
                    BluetoothAdapter.STATE_OFF -> {
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_main)
        mApp = this.application as TelinkLightApplication
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (LeBluetooth.getInstance().isSupport(applicationContext) && mBluetoothAdapter?.isEnabled == false)
            mBluetoothAdapter?.enable()
        //MqttManger.initMqtt() bindService()
        //if (TelinkLightApplication.getApp().mStompManager?.mStompClient?.isConnected != true)
        //TelinkLightApplication.getApp().initStompClient()

        if (Constant.isDebug) {//如果是debug模式可以切换 并且显示
            when (SharedPreferencesHelper.getInt(this, Constant.IS_SMART, 0)) {
                0 -> DEFAULT_MESH_FACTORY_NAME = "dadousmart"
                1 -> DEFAULT_MESH_FACTORY_NAME = "dadoutek"
                2 -> DEFAULT_MESH_FACTORY_NAME = "dadourd"
            }
            Constant.PIR_SWITCH_MESH_NAME = DEFAULT_MESH_FACTORY_NAME
            main_toast.visibility = VISIBLE
        } else {
            main_toast.visibility = GONE
        }
        main_toast.text = DEFAULT_MESH_FACTORY_NAME
        main_toast.setOnClickListener { getBin() }

        LogUtils.v("zcl---改变参数meshName-------$DEFAULT_MESH_FACTORY_NAME----改变参数url----${Constant.BASE_URL}")
        main_toast.text = DEFAULT_MESH_FACTORY_NAME
        main_toast.setOnClickListener { getBin() }
        initBottomNavigation()
        checkVersionAvailable()

         Constant.IS_ROUTE_MODE = SharedPreferencesHelper.getBoolean(this, Constant.ROUTE_MODE, false)
         //Constant.IS_ROUTE_MODE = false
        if (Constant.IS_ROUTE_MODE)
            getScanResult()//获取扫描状态

        getRouterStatus()
        getRegionList()
        getAllStatus()
        LogUtils.v("zcl---获取状态------${Constant.IS_ROUTE_MODE}--------${SharedPreferencesHelper.getBoolean(this, Constant.ROUTE_MODE, false)}-")
    }


    @SuppressLint("CheckResult")
    private fun getBin() {
        //showLoadingDialog(getString(R.string.please_wait))
        NetworkFactory.getApi().binList
                .subscribeOn(Schedulers.io())
                .compose(NetworkTransformer())
                .observeOn(AndroidSchedulers.mainThread()).subscribe({ it ->
                    LogUtils.v("zcl获取服务器bin-----------$it-------")
                    hideLoadingDialog()
                    TelinkLightApplication.mapBin = it
                    /*  it.forEach {
                          val split = it.split("=")
                          if (split.size >= 2 && !TextUtils.isEmpty(split[0]) && !TextUtils.isEmpty(split[1]))
                              mapBin[split[0]] = split[1].toInt()
                      }*/
                }, {
                    hideLoadingDialog()
                    //ToastUtils.showShort(getString(R.string.get_bin_fail))
                    //finish()
                })
    }

    @SuppressLint("CheckResult")
    private fun getRouterStatus() {
        if (Constant.IS_ROUTE_MODE)
            RouterModel.routerStatus()
                    ?.subscribe({
                        LogUtils.v("zcl-----------收到路由总状态-------$it")
                        when (it.data.status) {
                            Constant.ROUTER_OTA_ING -> {
                                val data = it.data.data
                                val productUUID = data.productUUID
                                val lastOtaType = data.op
                                val otaData = data.otaData
                                if (otaData.isNotEmpty()) {
                                    val otaDataOne = otaData[0]
                                    LogUtils.v("zcl-----------收到路由状态-------$otaData")
                                    when {
                                        otaData.size <= 1 -> {
                                            when (lastOtaType) {
                                                3 -> startActivity<RouterOtaActivity>("deviceMeshAddress" to 100000, "deviceType" to
                                                        productUUID, "deviceMac" to otaDataOne.macAddr, "isOTAing" to 1, "version" to otaDataOne!!.fromVersion)
                                                1 -> {
                                                    var meshAddr = when (productUUID) {
                                                        DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> DBUtils.getLightByID(otaDataOne.id.toLong())?.meshAddr
                                                        DeviceType.SMART_CURTAIN -> DBUtils.getCurtainByID(otaDataOne.id.toLong())?.meshAddr
                                                        DeviceType.SMART_RELAY -> DBUtils.getConnectorByID(otaDataOne.id.toLong())?.meshAddr
                                                        else -> DBUtils.getLightByID(otaDataOne.id.toLong())?.meshAddr
                                                    }
                                                    startActivity<RouterOtaActivity>("deviceMeshAddress" to meshAddr, "deviceType" to
                                                            productUUID, "deviceMac" to otaDataOne.macAddr, "isOTAing" to 1, "version" to otaDataOne!!.fromVersion)
                                                }
                                            }
                                        }
                                        else -> {
                                            val productUUID = productUUID
                                            when (lastOtaType) {
                                                0 -> startActivity<GroupOTAListActivity>("DeviceType" to productUUID, "isOTAing" to 1)
                                                2 -> {
                                                    var gpId = when (productUUID) {
                                                        DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                                                            DBUtils.getLightByID(otaDataOne.id.toLong())?.belongGroupId
                                                        }
                                                        DeviceType.SMART_CURTAIN -> {
                                                            DBUtils.getCurtainByID(otaDataOne.id.toLong())?.belongGroupId
                                                        }
                                                        DeviceType.SMART_RELAY -> {
                                                            DBUtils.getConnectorByID(otaDataOne.id.toLong())?.belongGroupId
                                                        }
                                                        else -> DBUtils.getLightByID(otaDataOne.id.toLong())?.belongGroupId
                                                    }
                                                    val groupByID = DBUtils.getGroupByID(gpId ?: 0)
                                                    startActivity<GroupOTAListActivity>("group" to groupByID!!, "DeviceType" to DeviceType.LIGHT_RGB, "isOTAing" to 1)
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                            Constant.ROUTER_SCANNING, Constant.ROUTER_SCAN_END -> {
                                val intent = Intent(this@MainActivity, DeviceScanningNewActivity::class.java)
                                intent.putExtra(Constant.DEVICE_TYPE, it.data.data.scanType)
                                startActivity(intent)
                            }
                        }
                    }, {
                        ToastUtils.showShort(it.message)
                    })
    }

    @SuppressLint("CheckResult")
    private fun getAllStatus() {
        if (Constant.IS_ROUTE_MODE)
            UserModel.getModeStatus()?.subscribe({
                LogUtils.v("zcl-----------获取状态服务器返回-------$it")
                Constant.IS_ROUTE_MODE = it.mode == 1//0蓝牙，1路由
                if (Constant.IS_ROUTE_MODE)
                    TelinkLightService.Instance().idleMode(true)
                Constant.IS_OPEN_AUXFUN = it.auxiliaryFunction
                SharedPreferencesHelper.putBoolean(this, Constant.ROUTE_MODE, Constant.IS_ROUTE_MODE)
                changeDisplayImgOnToolbar(TelinkLightApplication.getApp().connectDevice != null)
            }, {
                Constant.IS_OPEN_AUXFUN = false
            })
    }

    private fun getScanResult() {
        showLoadingDialog(getString(R.string.please_wait))
        val timeDisposable = Observable.timer(1500, TimeUnit.MILLISECONDS).subscribe { hideLoadingDialog() }
        val subscribe = RouterModel.getRouteScanningResult()?.subscribe({
            //status	int	状态。0扫描结束，1仍在扫描
            if (it?.data != null && it.data.status == 1) {
                val intent = Intent(this@MainActivity, DeviceScanningNewActivity::class.java)
                intent.putExtra(Constant.DEVICE_TYPE, it)
                startActivity(intent)
            }
        }, {
            ToastUtils.showShort(it.message)
            timeDisposable?.dispose()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Activity.RESULT_OK && requestCode == REQUEST_ENABLE_BT && !Constant.IS_ROUTE_MODE)
            ToastUtils.showShort(getString(R.string.open_blutooth_tip))
    }

    @SuppressLint("CheckResult")
    private fun getRegionList() {
        val list = mutableListOf<String>()
        if (netWorkCheck(this))
            RegionModel.get()?.subscribe({
                for (i in it) {
                    i.controlMesh?.let { it -> list.add(it) }
                }
                SharedPreferencesUtils.saveRegionNameList(list)
            }, {
            })
    }


    var syncCallback: SyncCallback = object : SyncCallback {
        override fun start() {
            showLoadingDialog(getString(R.string.tip_start_sync))
        }

        override fun complete() {
            hideLoadingDialog()
            AlertDialog.Builder(this@MainActivity)
                    .setCancelable(false)
                    .setMessage(getString(R.string.version_disabled))
                    .setPositiveButton(R.string.exit) { dialog, which ->
                        LogOutAndExitApp()
                    }
                    .show()
        }

        override fun error(msg: String) {
            hideLoadingDialog()
            LogUtils.d("upload data failed msg = $msg")
            AlertDialog.Builder(this@MainActivity)
                    .setCancelable(false)
                    .setMessage(getString(R.string.version_disabled))
                    .setPositiveButton(R.string.exit) { _, _ ->
                        LogOutAndExitApp()
                    }

        }
    }

    private fun LogOutAndExitApp() {
        SharedPreferencesHelper.putBoolean(this@MainActivity, Constant.IS_LOGIN, false)
        mApp?.removeEventListeners()
        TelinkLightService.Instance()?.idleMode(true)
        //重启app并杀死原进程
        ActivityUtils.finishAllActivities(true)
        ActivityUtils.startActivity(SplashActivity::class.java)
    }

    //防止viewpager嵌套fragment,fragment放置后台时间过长,fragment被系统回收了
    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        try {
            if (ev?.action == MotionEvent.ACTION_DOWN) {
                when (bnve.currentItem) {
                    0 -> deviceFragment.myPopViewClickPosition(ev.x, ev.y)
                    1 -> groupFragment.myPopViewClickPosition(ev.x, ev.y)
                    2 -> sceneFragment.myPopViewClickPosition(ev.x, ev.y)
                }
            }
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        return super.dispatchTouchEvent(ev)
    }


    override fun showDeviceListDialog(isGuide: Boolean, isClickRgb: Boolean) {
        showInstallDeviceList(isGuide, isClickRgb)
    }

    private fun syncDataAndExit() {
        if (!NetWorkUtils.isNetworkAvalible(this)) {
            LogUtils.d(getString(R.string.net_disconnect_tip_message))
        } else {
            SyncDataPutOrGetUtils.syncPutDataStart(this, syncCallback)
        }
    }

    /**
     * 检查App版本是否可用  报用户不存在  版本失败
     */
    private fun checkVersionAvailable() {
        var version = packageName(this)
        if (netWorkCheck(this))
            UpdateModel.run {
                isVersionAvailable(0, version)
                        .subscribe({
                            if (!it.isUsable) {
                                syncDataAndExit()
                            }
                            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), "isShowDot", it.isUsable)
                        }, {
                            //ToastUtils.showLong(R.string.get_server_version_fail)
                        })
            }
    }


    private fun packageName(context: Context): String {
        val manager = context.packageManager
        var name: String? = null
        try {
            val info = manager.getPackageInfo(context.packageName, 0)
            name = info.versionName
        } catch (e: NameNotFoundException) {
            e.printStackTrace()
        }
        return name!!
    }


    /**
     * 初始化底部的navigation按钮
     */
    private fun initBottomNavigation() {
        deviceFragment = DeviceFragment()
        groupFragment = GroupListFragment()
        sceneFragment = SceneFragment()
        meFragment = MeFragment()
        val fragments: List<Fragment> = listOf(deviceFragment, groupFragment, sceneFragment, meFragment)
        val vpAdapter = ViewPagerAdapter(supportFragmentManager, fragments)
        viewPager.adapter = vpAdapter
        viewPager.offscreenPageLimit = 3    //屏幕外的page数量限制调整为3
        //禁止所有动画效果
        bnve.enableAnimation(false)
        bnve.enableShiftingMode(false)
        bnve.enableItemShiftingMode(false)
        bnve.setupWithViewPager(viewPager)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {}

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {}

            override fun onPageSelected(p0: Int) {
                val intent = Intent("isDelete")
                intent.putExtra("isDelete", "true")
                LocalBroadcastManager.getInstance(this@MainActivity).sendBroadcast(intent)
            }
        })
    }


    //切换到场景Fragment
    fun transScene() {
        bnve.currentItem = 2
    }

    //切换到组控制Fragment
    private fun tranHome() {
        bnve.currentItem = 1
    }

    override fun onPause() {
        super.onPause()
        disableConnectionStatusListener()
        mCompositeDisposable.clear()
        mConnectDisposable?.dispose()
        progressBar.visibility = GONE
        try {
            this.unregisterReceiver(mReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopConnectTimer()
    }


    override fun onResume() {
        super.onResume()
        checkVersionAvailable()
        //检测service是否为空，为空则重启
        if (TelinkLightService.Instance() == null)
            mApp?.startLightService(TelinkLightService::class.java)
        //startToRecoverDevices()
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
        registerReceiver(mReceiver, filter)
        getBin()

         Constant.IS_ROUTE_MODE = SharedPreferencesHelper.getBoolean(this, Constant.ROUTE_MODE, false)
        //Constant.IS_ROUTE_MODE = false
    }

    override fun onPostResume() {
        super.onPostResume()
        deviceFragment.refreshView()
        groupFragment.refreshView()
        sceneFragment.refreshView()
        autoConnect()
//        mTelinkLightService = TelinkLightService.Instance()
//        if (mTelinkLightService?.adapter?.mLightCtrl?.currentLight?.isConnected != true) {
//            while (TelinkApplication.getInstance()?.serviceStarted == true) {
//                GlobalScope.launch(Dispatchers.Main) {
//                    retryConnectCount = 0
//                    connectFailedDeviceMacList.clear()
//                    startScan()
//                }
//                break
//            }
//
//        }
    }


    override fun changeToScene() {
        transScene()
    }

    override fun changeToGroup() {
        tranHome()
    }


    private fun onLogin() {
        GlobalScope.launch(Dispatchers.Main) {
            stopConnectTimer()
            if (progressBar?.visibility != GONE)
                progressBar?.visibility = GONE
        }

        val connectDevice = mApp?.connectDevice
        if (connectDevice != null) {
            this.connectMeshAddress = connectDevice.meshAddress
        }
    }

    /**
     * 处理扫描事件
     *
     * @param event
     */
    @Synchronized
    private fun onLeScan(event: LeScanEvent) {
        val mesh = mApp?.mesh
        val deviceInfo: DeviceInfo = event.args

        Thread {
            val dbLight = DBUtils.getLightByMeshAddr(deviceInfo.meshAddress)
            if (dbLight != null && dbLight.macAddr == "0") {
                dbLight.macAddr = deviceInfo.macAddress
                DBUtils.updateLight(dbLight)
            }
        }.start()

        if (!isSwitch(deviceInfo.productUUID) && !connectFailedDeviceMacList.contains(deviceInfo.macAddress)) {
//            connect(deviceInfo.macAddress)
            if (bestRSSIDevice != null) {
                //扫到的灯的信号更好并且没有连接失败过就把要连接的灯替换为当前扫到的这个。
                if (deviceInfo.rssi > bestRSSIDevice?.rssi ?: 0) {
                    LogUtils.d("changeToScene to device with better RSSI  new meshAddr = ${deviceInfo.meshAddress} rssi = ${deviceInfo.rssi}")
                    bestRSSIDevice = deviceInfo
                }
            } else {
                LogUtils.d("RSSI  meshAddr = ${deviceInfo.meshAddress} rssi = ${deviceInfo.rssi}")
                bestRSSIDevice = deviceInfo
            }

        }

    }

    private fun isSwitch(uuid: Int): Boolean {
        return when (uuid) {
            DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SENSOR, DeviceType.NIGHT_LIGHT -> {
                LogUtils.d("This is switch")
                true
            }
            else -> {
                false

            }
        }
    }

    private fun startCheckRSSITimer() {
        mScanTimeoutDisposal?.dispose()
        val periodCount = SCAN_TIMEOUT_SECOND.toLong() - SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND
        Observable.intervalRange(1, periodCount, SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND, 1,
                TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Long?> {
                    override fun onComplete() {
                        LogUtils.d("onLeScanTimeout()")
                        onLeScanTimeout()
                    }

                    override fun onSubscribe(d: Disposable) {
                        mScanTimeoutDisposal = d
                    }

                    override fun onNext(t: Long) {
                        if (bestRSSIDevice != null) {
                            mScanTimeoutDisposal?.dispose()
                            LogUtils.d("connect device , mac = ${bestRSSIDevice?.macAddress}  rssi = ${bestRSSIDevice?.rssi}")
                            connect2(bestRSSIDevice!!.macAddress)
                        }
                    }

                    override fun onError(e: Throwable) {
                        Log.d("SawTest", "error = $e")

                    }
                })
    }

    @SuppressLint("CheckResult")
    private fun connect2(mac: String) {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN)
                .subscribe {
                    if (it) {
                        //授予了权限
                        if (TelinkLightService.Instance() != null) {
                            progressBar?.visibility = View.VISIBLE
                            TelinkLightService.Instance().connect(mac, CONNECT_TIMEOUT)
                            startConnectTimer()
                        }
                    } else {
                        //没有授予权限
                        DialogUtils.showNoBlePermissionDialog(this, { connect2(mac) }, { finish() })
                    }
                }
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
            retryConnectCount = 0
            connectFailedDeviceMacList.clear()
            startScan()
        }
    }


    private fun login() {
        val account = DBUtils.lastUser?.account
        val pwd = NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16)
        TelinkLightService.Instance().login(Strings.stringToBytes(account, 16)
                , Strings.stringToBytes(pwd, 16))
    }

    private fun onLeScanTimeout() {
        LogUtils.d("onErrorReport: onLeScanTimeout")
        retryConnectCount = 0
        connectFailedDeviceMacList.clear()
        startScan()
    }

    private fun addScanListeners() {
        this.mApp?.addEventListener(LeScanEvent.LE_SCAN, this)
        this.mApp?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
        this.mApp?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)
    }

    @SuppressLint("CheckResult")
    private fun startScan() {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN)
                .subscribeOn(Schedulers.io())
                .subscribe {
                    if (it) {
                        TelinkLightService.Instance().idleMode(true)
                        bestRSSIDevice = null   //扫描前置空信号最好设备。
                        //扫描参数
                        val account = DBUtils.lastUser?.account

                        val scanFilters = ArrayList<ScanFilter>()
                        val scanFilter = ScanFilter.Builder()
                                .setDeviceName(account)
                                .build()
                        scanFilters.add(scanFilter)

                        val params = LeScanParameters.create()
                        if (!com.dadoutek.uled.util.AppUtils.isExynosSoc) {
                            params.setScanFilters(scanFilters)
                        }
                        params.setMeshName(account)
                        params.setOutOfMeshName(account)
                        params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                        params.setScanMode(false)

                        addScanListeners()
                        TelinkLightService.Instance().startScan(params)
                        startCheckRSSITimer()
                    } else {
                        //没有授予权限
                        DialogUtils.showNoBlePermissionDialog(this, {
                            retryConnectCount = 0
                            startScan()
                        }, { finish() })
                    }
                }
    }


    @SuppressLint("CheckResult")
    fun autoConnect() {
        // if (LeBluetooth.getInstance().isSupport(applicationContext))
        //LeBluetooth.getInstance().enable(applicationContext)    //如果没打开蓝牙，就提示用户打开
        //如果位置服务没打开，则提示用户打开位置服务，bleScan必须
        if (!BleUtils.isLocationEnable(this)) {
            showOpenLocationServiceDialog()
        } else {
            hideLocationServiceDialog()
            if (TelinkApplication.getInstance()?.serviceStarted == true) {
                if (!this@MainActivity.isFinishing)
                    RxPermissions(this@MainActivity).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                if (TelinkLightApplication.getApp().connectDevice == null) {
                                    val deviceTypes = mutableListOf(DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD,
                                            DeviceType.LIGHT_RGB, DeviceType.SMART_RELAY, DeviceType.SMART_CURTAIN)
                                    val size = DBUtils.getAllCurtains().size + DBUtils.allLight.size + DBUtils.allRely.size
                                    if (size > 0) {
                                        //ToastUtils.showLong(R.string.connecting_tip)
                                        mConnectDisposable?.dispose()
                                        mConnectDisposable = connect(deviceTypes = deviceTypes, fastestMode = false, retryTimes = 5)
                                                ?.subscribe({//找回有效设备
                                                    //RecoverMeshDeviceUtil.addDevicesToDb(it)
                                                    onLogin()
                                                },
                                                        {
                                                            LogUtils.v("zcl-----------连接失败继续连接-------")
                                                            if (!Constant.IS_ROUTE_MODE)
                                                                autoConnect()
                                                            LogUtils.d("TelinkBluetoothSDK connect failed, reason = ${it.message}---${it.localizedMessage}")
                                                        }
                                                )
                                    } else {
                                        ToastUtils.showShort(getString(R.string.no_connect_device))
                                    }
                                }
                            }, { LogUtils.d(it) })
            } else {
                mApp?.startLightService(TelinkLightService::class.java)
                autoConnect()
            }
        }

        val deviceInfo = mApp?.connectDevice
        if (deviceInfo != null)
            this.connectMeshAddress = (mApp?.connectDevice?.meshAddress ?: 0x00) and 0xFF

    }

    private fun stopConnectTimer() {
        mConnectDisposable?.dispose()
    }


    override fun onStop() {
        super.onStop()
        TelinkLightService.Instance()?.disableAutoRefreshNotify()
        installDialog?.dismiss()
    }


    override fun onDestroy() {
        super.onDestroy()
        TelinkLightApplication.getApp().releseStomp()
        //移除事件
        TelinkLightService.Instance()?.idleMode(true)
        this.mDelayHandler.removeCallbacksAndMessages(null)
        Lights.getInstance().clear()
        mDisposable.dispose()
        disposableCamera?.dispose()
        mCompositeDisposable.dispose()
        mConnectDisposable?.dispose()
        AllenVersionChecker.getInstance().cancelAllMission(this)
        //解绑服务
    }

    private fun onServiceConnected(event: ServiceEvent) {}

    private fun onServiceDisconnected(event: ServiceEvent) {
        LogUtils.d("onServiceDisconnected")
        TelinkLightApplication.getApp().startLightService(TelinkLightService::class.java)
    }


    /**
     * 事件处理方法
     *
     * @param event
     */
    override fun performed(event: Event<String>) {
        when (event.type) {
            NotificationEvent.GET_ALARM -> {
            }
            ServiceEvent.SERVICE_CONNECTED -> {
                this.onServiceConnected(event as ServiceEvent)
            }
            ServiceEvent.SERVICE_DISCONNECTED -> this.onServiceDisconnected(event as ServiceEvent)
            NotificationEvent.GET_DEVICE_STATE -> onNotificationEvent(event as NotificationEvent)
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReportNormal(info)
            }
        }
    }


    private fun onNotificationEvent(event: NotificationEvent) {
        if (!AppUtils.isAppForeground()) return
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
            var isDelete = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_DELETE, false)
            if (isDelete) {
                val intent = Intent("isDelete")
                intent.putExtra("isDelete", "true")
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                return true
            } else {
                if (secondTime - firstTime > 2000) {
                    Toast.makeText(this@MainActivity, R.string.click_double_exit, Toast.LENGTH_SHORT).show()
                    firstTime = secondTime
                    return true
                } else {
                    LogUtils.d("App id = ${ProcessUtils.getCurrentProcessName()}")
                    LogUtils.d("activity stack size = ${ActivityUtils.getActivityList()}")
                    setResult(Activity.RESULT_FIRST_USER)
                    TelinkLightApplication.getApp().releseStomp()
                    //ActivityUtils.finishAllActivities(true)
                    TelinkApplication.getInstance().removeEventListeners()
                    //TelinkLightApplication.getApp().doDestroy()
                    finish()
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun setMessage(cmd: Int, extCmd: Int, message: String) {
    }
}




