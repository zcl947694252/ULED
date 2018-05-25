package com.dadoutek.uled.activity

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import butterknife.ButterKnife
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.TelinkLightApplication
import com.dadoutek.uled.TelinkLightService
import com.dadoutek.uled.TelinkMeshErrorDealActivity
import com.dadoutek.uled.adapter.ViewPagerAdapter
import com.dadoutek.uled.fragments.DeviceListFragment
import com.dadoutek.uled.fragments.GroupListFragment
import com.dadoutek.uled.fragments.MeFragment
import com.dadoutek.uled.fragments.SceneFragment
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.util.BleUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.*
import com.telink.bluetooth.light.*
import com.telink.util.BuildUtils
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar
import java.util.concurrent.TimeUnit

class MainActivity : TelinkMeshErrorDealActivity(), EventListener<String> {
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
    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                UPDATE_LIST -> deviceFragment?.notifyDataSetChanged()
            }
        }
    }

    private val mDelayHandler = Handler()
    private val delay = 200

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)

                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        Log.d(TAG, "蓝牙开启")
                        TelinkLightService.Instance().idleMode(true)
                        autoConnect()
                    }
                    BluetoothAdapter.STATE_OFF -> Log.d(TAG, "蓝牙关闭")
                }
            }
        }
    }


    internal var PERMISSION_REQUEST_CODE = 0x10

    internal var mTimeoutBuilder: AlertDialog.Builder? = null

    //记录用户首次点击返回键的时间
    private var firstTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate")
        //TelinkLog.ENABLE = false;
//        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setContentView(R.layout.activity_main)
        ButterKnife.bind(this)


//        initBottomIconClickListener()

        this.mApplication = this.application as TelinkLightApplication

//        this.fragmentManager = this.getFragmentManager()

//        this.deviceFragment = FragmentFactory
//                .createFragment(R.id.tab_devices) as DeviceListFragment
//        this.groupFragment = FragmentFactory
//                .createFragment(R.id.tab_groups) as GroupListFragment
//        this.meFragment = FragmentFactory
//                .createFragment(R.id.tab_account) as MeFragment
//        this.sceneFragment = FragmentFactory
//                .createFragment(R.id.tab_scene) as SceneFragment
        //        this.tabs = (ConstraintLayout) this.findViewById(R.id.tabs);
        //        this.tabs.setOnCheckedChangeListener(this.checkedChangeListener);


        //        this.mApplication.doInit();

        TelinkLog.d("-------------------------------------------")
        TelinkLog.d(Build.MANUFACTURER)
        TelinkLog.d(Build.TYPE)
        TelinkLog.d(Build.BOOTLOADER)
        TelinkLog.d(Build.DEVICE)
        TelinkLog.d(Build.HARDWARE)
        TelinkLog.d(Build.SERIAL)
        TelinkLog.d(Build.BRAND)
        TelinkLog.d(Build.DISPLAY)
        TelinkLog.d(Build.FINGERPRINT)

        TelinkLog.d(Build.PRODUCT + ":" + Build.VERSION.SDK_INT + ":" + Build.VERSION.RELEASE + ":" + Build.VERSION.CODENAME + ":" + Build.ID)

        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
        registerReceiver(mReceiver, filter)

        initBottomNavigation()

//        checkPermission()

        if (savedInstanceState == null) {
//            initDefaultFragment()
        }
    }

    private fun initBottomNavigation() {
        deviceFragment = DeviceListFragment()
        groupFragment = GroupListFragment()
        sceneFragment = SceneFragment()
        meFragment = MeFragment()
        val fragments: List<Fragment> = listOf(groupFragment, sceneFragment, deviceFragment, meFragment)
        val vpAdapter = ViewPagerAdapter(supportFragmentManager, fragments)
        viewPager.adapter = vpAdapter
        //禁止所有动画效果
        bnve.enableAnimation(false);
        bnve.enableShiftingMode(false);
        bnve.enableItemShiftingMode(false);
        bnve.setupWithViewPager(viewPager)

    }


    override fun onStart() {

        super.onStart()

        Log.d(TAG, "onStart")

        val result = BuildUtils.assetSdkVersion("4.4")
        Log.d(TAG, " Version : $result")


        this.autoConnect()
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart")
    }

    override fun onPause() {
        super.onPause()
        //移除事件
        this.mApplication!!.removeEventListener(this)
        Log.d(TAG, "onPause")
        mConnectTimer?.dispose()
    }


    override fun onResume() {
        super.onResume()
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
        Log.d(TAG, "onStop")
        if (TelinkLightService.Instance() != null)
            TelinkLightService.Instance().disableAutoRefreshNotify()
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        unregisterReceiver(mReceiver)
        this.mApplication!!.doDestroy()
        this.mDelayHandler.removeCallbacksAndMessages(null)
        Lights.getInstance().clear()
        mDisposable.dispose()
    }


    fun addEventListeners() {
        // 监听各种事件
        this.mApplication!!.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication!!.addEventListener(NotificationEvent.ONLINE_STATUS, this)
        this.mApplication!!.addEventListener(NotificationEvent.GET_ALARM, this)
        this.mApplication!!.addEventListener(NotificationEvent.GET_DEVICE_STATE, this)
        this.mApplication!!.addEventListener(ServiceEvent.SERVICE_CONNECTED, this)
        this.mApplication!!.addEventListener(MeshEvent.OFFLINE, this)
        this.mApplication!!.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }


    private fun stopConnectTimer() {
        mConnectTimer?.dispose()
    }


    private var mConnectTimer: Disposable? = null
    private fun startConnectTimer() {
        //如果超过8s还没有变为连接中状态，则显示为超时
        mConnectTimer = Observable.timer(8000, TimeUnit.MILLISECONDS)
                .subscribe(Consumer {
                    indefiniteSnackbar(root, R.string.not_found_light, R.string.retry) {
                        TelinkLightService.Instance().idleMode(true)
                        autoConnect()
                    }
                })


    }

    /**
     * 自动重连
     */
    private fun autoConnect() {
        if (TelinkLightService.Instance() != null) {

            if (TelinkLightService.Instance().mode != LightAdapter.MODE_AUTO_CONNECT_MESH) {

//                ToastUtils.showLong(getString(R.string.connect_state))
                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

                if (this.mApplication!!.isEmptyMesh)
                    return

                //                Lights.getInstance().clear();
                this.mApplication!!.refreshLights()


                this.deviceFragment!!.notifyDataSetChanged()

                val mesh = this.mApplication!!.mesh

                if (TextUtils.isEmpty(mesh.name) || TextUtils.isEmpty(mesh.password)) {
                    TelinkLightService.Instance().idleMode(true)
                    return
                }

                //自动重连参数
                val connectParams = Parameters.createAutoConnectParameters()
                connectParams.setMeshName(mesh.name)
                connectParams.setPassword(mesh.password)
                connectParams.autoEnableNotification(true)

                // 之前是否有在做MeshOTA操作，是则继续
                if (mesh.isOtaProcessing) {
                    connectParams.setConnectMac(mesh.otaDevice.mac)
                    saveLog("Action: AutoConnect:" + mesh.otaDevice.mac)
                } else {
                    saveLog("Action: AutoConnect:NULL")
                }
                //自动重连
                TelinkLightService.Instance().autoConnect(connectParams)
                startConnectTimer();

                indefiniteSnackbar(root, getString(R.string.scanning_devices))
                runOnUiThread { progressBar?.visibility = View.VISIBLE }


            }

            //刷新Notify参数
            val refreshNotifyParams = Parameters.createRefreshNotifyParameters()
            refreshNotifyParams.setRefreshRepeatCount(2)
            refreshNotifyParams.setRefreshInterval(2000)
            //开启自动刷新Notify
            TelinkLightService.Instance().autoRefreshNotify(refreshNotifyParams)
        }
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


    private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                runOnUiThread {
                    if (progressBar?.visibility != View.GONE)
                        progressBar?.visibility = View.GONE
                }
//                ToastUtils.showLong(getString(R.string.connect_success))
                launch(UI) {
                    kotlinx.coroutines.experimental.delay(200)
                    snackbar(root, R.string.connect_success)
                }

                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, true)
                val connectDevice = this.mApplication!!.connectDevice
                if (connectDevice != null) {
                    this.connectMeshAddress = connectDevice.meshAddress
                    if (TelinkLightService.Instance().mode == LightAdapter.MODE_AUTO_CONNECT_MESH) {
                        mHandler.postDelayed({ TelinkLightService.Instance().sendCommandNoResponse(0xE4.toByte(), 0xFFFF, byteArrayOf()) }, (3 * 1000).toLong())
                    }

                    if (TelinkLightApplication.getApp().mesh.isOtaProcessing && foreground) {
                        startActivity(Intent(this, OTAUpdateActivity::class.java)
                                .putExtra(OTAUpdateActivity.INTENT_KEY_CONTINUE_MESH_OTA, OTAUpdateActivity.CONTINUE_BY_PREVIOUS))
                    }
                }
            }
            LightAdapter.STATUS_CONNECTING -> {
                stopConnectTimer()
                indefiniteSnackbar(root, getString(R.string.connect_state))
            }
            LightAdapter.STATUS_LOGOUT ->
                //                this.showToast("disconnect");
                onLogout()

            LightAdapter.STATUS_ERROR_N -> onNError(event)
            else -> {
            }
        }//                this.showToast("login");
    }

    private fun onNError(event: DeviceEvent) {

        ToastUtils.showLong(getString(R.string.connect_fail))
        SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

        TelinkLightService.Instance().idleMode(true)
        TelinkLog.d("DeviceScanningActivity#onNError")

        val builder = AlertDialog.Builder(this)
        builder.setMessage("当前环境:Android7.0!连接重试:" + " 3次失败!")
        builder.setNegativeButton("confirm") { dialog, which -> dialog.dismiss() }
        builder.setCancelable(false)
        builder.show()
    }

    private fun onLogout() {
        //如果超过8s还没有连接上，则显示为超时
        if (progressBar.visibility == View.VISIBLE) {
            runOnUiThread(Runnable {
                indefiniteSnackbar(root, R.string.connect_failed_if_there_are_lights, R.string.retry) {
                    autoConnect()
                }
            })
        }

        val lights = Lights.getInstance().get()
        for (light in lights) {
            light.status = ConnectionStatus.OFFLINE
            light.updateIcon()
        }
        runOnUiThread { deviceFragment?.notifyDataSetChanged() }
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
    @Synchronized
    private fun onOnlineStatusNotify(event: NotificationEvent) {
        TelinkLog.i("MainActivity#onOnlineStatusNotify#Thread ID : " + Thread.currentThread().id)
        val notificationInfoList: List<OnlineStatusNotificationParser.DeviceNotificationInfo>?

        notificationInfoList = event.parse() as List<OnlineStatusNotificationParser.DeviceNotificationInfo>

        if (notificationInfoList == null || notificationInfoList.size <= 0)
            return

        for (notificationInfo in notificationInfoList) {

            val meshAddress = notificationInfo.meshAddress
            val brightness = notificationInfo.brightness
            Log.d("Saw", "meshAddress = " + meshAddress + "  reserve = " + notificationInfo.reserve +
                    " status = " + notificationInfo.status + " connectionStatus = " + notificationInfo.connectionStatus.value)
            if (checkIsLight(notificationInfo)) {
//                var light = this.deviceFragment.getDevice(meshAddress) as Lights
                var light: Light? = lights.getByMeshAddress(meshAddress)
                if (light == null) {
                    light = Light()
                                        this.deviceFragment.addDevice(light);
                }

                light.meshAddress = meshAddress
                light.brightness = brightness
                light.status = notificationInfo.connectionStatus

                if (light.meshAddress == this.connectMeshAddress) {
                    light.textColor = this.resources.getColor(
                            R.color.primary)
                } else {
                    light.textColor = this.resources.getColor(
                            R.color.black)
                }

                lights.add(light)
                light.updateIcon()
            }

        }

        mHandler.obtainMessage(UPDATE_LIST).sendToTarget()
    }

    private fun onServiceConnected(event: ServiceEvent) {
        mDisposable.add(RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN).subscribe { granted ->
            if (granted!!) {
                this.autoConnect()
            } else {
                val dialog = AlertDialog.Builder(this)
                dialog.setMessage(resources.getString(R.string.scan_tip))
                dialog.setPositiveButton(R.string.btn_ok) { dialog1, which -> autoConnect() }
                dialog.setNegativeButton(R.string.btn_cancel) { dialog12, which -> ActivityUtils.finishAllActivities(true) }
            }
        })
    }

    private fun onServiceDisconnected(event: ServiceEvent) {

    }

    private fun onMeshOffline(event: MeshEvent) {
        val lights = Lights.getInstance().get()
        val it = lights.iterator()
        while (it.hasNext()) {
            val light = it.next()
            light.status = ConnectionStatus.OFFLINE
            light.updateIcon()
            it.remove()
        }
        //        for (Light light : lights) {
        ////            light.status = ConnectionStatus.OFFLINE;
        ////            light.updateIcon();
        //              lights.remove(light);
        //        }
        this.deviceFragment!!.notifyDataSetChanged()

        if (TelinkLightApplication.getApp().mesh.isOtaProcessing) {
            TelinkLightService.Instance().idleMode(true)
            if (mTimeoutBuilder == null) {
                mTimeoutBuilder = AlertDialog.Builder(this)
                mTimeoutBuilder!!.setTitle("AutoConnect Fail")
                mTimeoutBuilder!!.setMessage("Connect device:" + TelinkLightApplication.getApp().mesh.otaDevice.mac + " Fail, Quit? \nYES: quit MeshOTA process, NO: retry")
                mTimeoutBuilder!!.setNeutralButton(R.string.quite) { dialog, which ->
                    val mesh = TelinkLightApplication.getApp().mesh
                    mesh.otaDevice = null
                    mesh.saveOrUpdate(this@MainActivity)
                    autoConnect()
                    dialog.dismiss()
                }
                mTimeoutBuilder!!.setNegativeButton(R.string.retry) { dialog, which ->
                    autoConnect()
                    dialog.dismiss()
                }
                mTimeoutBuilder!!.setCancelable(false)
            }
            mTimeoutBuilder!!.show()
        }
    }


    /**
     * 事件处理方法
     *
     * @param event
     */
    override fun performed(event: Event<String>) {
        when (event.type) {
            NotificationEvent.ONLINE_STATUS -> this.onOnlineStatusNotify(event as NotificationEvent)

            NotificationEvent.GET_ALARM -> {
            }
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            MeshEvent.OFFLINE -> this.onMeshOffline(event as MeshEvent)
            ServiceEvent.SERVICE_CONNECTED -> this.onServiceConnected(event as ServiceEvent)
            ServiceEvent.SERVICE_DISCONNECTED -> this.onServiceDisconnected(event as ServiceEvent)
            NotificationEvent.GET_DEVICE_STATE -> onNotificationEvent(event as NotificationEvent)

            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                TelinkLog.d("MainActivity#performed#ERROR_REPORT: " + " stateCode-" + info.stateCode
                        + " errorCode-" + info.errorCode
                        + " deviceId-" + info.deviceId)
            }
        }//                this.onAlarmGet((NotificationEvent) event);
    }

    override fun onLocationEnable() {
        autoConnect()
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
