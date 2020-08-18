package com.dadoutek.uled.othersview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
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
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.allenliu.versionchecklib.v2.AllenVersionChecker
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.AppUtils
import com.dadoutek.uled.mqtt.IGetMessageCallBack
import com.dadoutek.uled.mqtt.MqttService
import com.dadoutek.uled.mqtt.MyServiceConnection
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.device.DeviceFragment
import com.dadoutek.uled.fragment.MeFragment
import com.dadoutek.uled.group.GroupListFragment
import com.dadoutek.uled.intf.CallbackLinkMainActAndFragment
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.Constant.DEFAULT_MESH_FACTORY_NAME
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.httpModel.RegionModel
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.model.httpModel.UpdateModel
import com.dadoutek.uled.model.httpModel.UserModel
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.region.bean.RegionBean
import com.dadoutek.uled.scene.SceneFragment
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.event.NotificationEvent
import com.telink.bluetooth.event.ServiceEvent
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 首页  修改mes那么后  如果旧有用户不登陆 会报错直接崩溃 因为调用后的mesname是空的
 *
 * 首页设备
 */
class MainActivity : TelinkBaseActivity(), EventListener<String>, CallbackLinkMainActAndFragment, IGetMessageCallBack {
    private lateinit var serviceConnection: MyServiceConnection
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
    private var mConnectDisposal: Disposable? = null
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
                    BluetoothAdapter.STATE_OFF -> { }
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
        //MqttManger.initMqtt()
        serviceConnection = MyServiceConnection()
        serviceConnection?.setIGetMessageCallBack(this)
        val intent = Intent(this, MqttService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        //if (TelinkLightApplication.getApp().mStompManager?.mStompClient?.isConnected != true)
        //TelinkLightApplication.getApp().initStompClient()

        Constant.IS_ROUTE_MODE = SharedPreferencesHelper.getBoolean(this,Constant.ROUTE_MODE,false)

        if (Constant.isDebug) {//如果是debug模式可以切换 并且显示
            when (SharedPreferencesHelper.getInt(this, Constant.IS_TECK, 0)) {
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
        main_toast.setOnClickListener {}
        initBottomNavigation()
        checkVersionAvailable()
        showLoadingDialog(getString(R.string.please_wait))
        getScanResult()
        getRegionList()
        getAllStatus()
    }

    @SuppressLint("CheckResult")
    private fun getAllStatus() {
        UserModel.getModeStatus()?.subscribe({
            Constant.IS_ROUTE_MODE = it.mode==1//0蓝牙，1路由
            Constant.IS_OPEN_AUXFUN = it.auxiliaryFunction
        },{
            Constant.IS_ROUTE_MODE = false
            Constant.IS_OPEN_AUXFUN = false
        })
    }

    private fun getScanResult() {
        val timeDisposable = Observable.timer(1500, TimeUnit.MILLISECONDS).subscribe { hideLoadingDialog() }
        val subscribe = RouterModel.routeScanningResult()?.subscribe({
            //status	int	状态。0扫描结束，1仍在扫描
            if (it.data!=null&&it.data.status==1){
                val intent = Intent(this@MainActivity, DeviceScanningNewActivity::class.java)
                intent.putExtra(Constant.DEVICE_TYPE, it)
                startActivity(intent)
            }
        }, {
            getScanResult()
            timeDisposable?.dispose()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Activity.RESULT_OK && requestCode == REQUEST_ENABLE_BT)
            ToastUtils.showShort(getString(R.string.open_blutooth_tip))
    }

    @SuppressLint("CheckResult")
    private fun getRegionList() {
        val list = mutableListOf<String>()
        if (netWorkCheck(this))
            RegionModel.get()?.subscribe(object : NetworkObserver<MutableList<RegionBean>?>() {
                override fun onNext(t: MutableList<RegionBean>) {
                    for (i in t) {
                        i.controlMesh?.let { it -> list.add(it) }
                    }
                    SharedPreferencesUtils.saveRegionNameList(list)

                }
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
                        .subscribe(object : NetworkObserver<ResponseVersionAvailable>() {
                            override fun onNext(s: ResponseVersionAvailable) {
                                if (!s.isUsable) {
                                    syncDataAndExit()
                                }
                                SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), "isShowDot", s.isUsable)
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                                //ToastUtils.showLong(R.string.get_server_version_fail)
                            }
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
        mConnectDisposal?.dispose()
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
    }

    override fun onPostResume() {
        super.onPostResume()
        deviceFragment.refreshView()
        groupFragment.refreshView()
        sceneFragment.refreshView()
        autoConnect()
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
                RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            if (TelinkLightApplication.getApp().connectDevice == null) {
                                val deviceTypes = mutableListOf(DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD,
                                        DeviceType.LIGHT_RGB, DeviceType.SMART_RELAY, DeviceType.SMART_CURTAIN)
                                val size = DBUtils.getAllCurtains().size + DBUtils.allLight.size + DBUtils.allRely.size
                                if (size > 0) {
                                    ToastUtils.showLong(R.string.connecting_tip)
                                    mConnectDisposal?.dispose()
                                    mConnectDisposal = connect(deviceTypes = deviceTypes, fastestMode = true, retryTimes = 10)
                                            ?.subscribe(
                                                    {//找回有效设备
                                                        //RecoverMeshDeviceUtil.addDevicesToDb(it)
                                                        onLogin()
                                                    },
                                                    {
                                                        LogUtils.d("connect failed, reason = $it")
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
        mConnectDisposal?.dispose()
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
        mConnectDisposal?.dispose()
        AllenVersionChecker.getInstance().cancelAllMission(this)
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




