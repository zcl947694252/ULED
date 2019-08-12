package com.dadoutek.uled.tellink

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.util.BluetoothConnectionFailedDialog
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.event.ServiceEvent
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1
open class TelinkBaseActivity : AppCompatActivity()  {

    private var connectMeshAddress: Int = 0


//    override fun performed(event: Event<String>) {
//        when (event.type) {
//            LeScanEvent.LE_SCAN -> onLeScan(event as LeScanEvent)
////            LeScanEvent.LE_SCAN_TIMEOUT -> onLeScanTimeout()
////            LeScanEvent.LE_SCAN_COMPLETED -> onLeScanTimeout()
////            NotificationEvent.ONLINE_STATUS -> this.onOnlineStatusNotify(event as NotificationEvent)
//            NotificationEvent.GET_ALARM -> {
//            }
//            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
////            MeshEvent.OFFLINE -> this.onMeshOffline(event as MeshEvent)
//            ServiceEvent.SERVICE_CONNECTED -> this.onServiceConnected(event as ServiceEvent)
//            ServiceEvent.SERVICE_DISCONNECTED -> this.onServiceDisconnected(event as ServiceEvent)
////            NotificationEvent.GET_DEVICE_STATE -> onNotificationEvent(event as NotificationEvent)
//            ErrorReportEvent.ERROR_REPORT -> {
//                val info = (event as ErrorReportEvent).args
//                onErrorReport(info)
//            }
//        }
//    }

//    private fun onErrorReport(info: ErrorReportInfo) {
////       //("onErrorReport current device mac = ${bestRSSIDevice?.macAddress}")
//        if (bestRSSIDevice != null) {
//            connectFailedDeviceMacList.add(bestRSSIDevice!!.macAddress)
//        }
//        when (info.stateCode) {
//            ErrorReportEvent.STATE_SCAN -> {
//                when (info.errorCode) {
//                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
//                       //("蓝牙未开启")
//                    }
//                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
//                       //("无法收到广播包以及响应包")
//                    }
//                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
//                       //("未扫到目标设备")
//                    }
//                }
//
//            }
//            ErrorReportEvent.STATE_CONNECT -> {
//                when (info.errorCode) {
//                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
//                       //("未读到att表")
//                    }
//                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
//                       //("未建立物理连接")
//                    }
//                }
//                retryConnect()
//
//            }
//            ErrorReportEvent.STATE_LOGIN -> {
//                when (info.errorCode) {
//                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
//                       //("value check失败： 密码错误")
//                    }
//                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
//                       //("read login data 没有收到response")
//                    }
//                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
//                       //("write login data 没有收到response")
//                    }
//                }
//                retryConnect()
//
//            }
//        }
//    }

    private fun onServiceConnected(event: ServiceEvent) {
//       //("onServiceConnected")
    }

    private fun onServiceDisconnected(event: ServiceEvent) {
       //("onServiceDisconnected")
        TelinkLightApplication.getInstance().startLightService(TelinkLightService::class.java)
    }

//    private fun onDeviceStatusChanged(event: DeviceEvent) {
//
//        val deviceInfo = event.args
//
//        when (deviceInfo.status) {
//            LightAdapter.STATUS_LOGIN -> {
//
//                TelinkLightService.Instance()?.).enableNotification()
//                TelinkLightService.Instance()?.).updateNotification()
//                GlobalScope.launch(Dispatchers.Main) {
//                    stopConnectTimer()
//                    if (progressBar?.visibility != View.GONE)
//                        progressBar?.visibility = View.GONE
//                    delay(300)
//                }
//
//                val connectDevice = this.mApplication?.connectDevice
//                if (connectDevice != null) {
//                    this.connectMeshAddress = connectDevice.meshAddress
//                }
//
////                scanPb.visibility = View.GONE
////                adapter?.notifyDataSetChanged()
//                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, true)
//            }
//            LightAdapter.STATUS_LOGOUT -> {
//                retryConnect()
//            }
//            LightAdapter.STATUS_CONNECTING -> {
//                Log.d("connectting", "444")
////                scanPb.visibility = View.VISIBLE
//            }
//            LightAdapter.STATUS_CONNECTED -> {
//                if (!TelinkLightService.Instance()?.).isLogin)
//                    login()
//            }
//            LightAdapter.STATUS_ERROR_N -> onNError(event)
//        }
//    }

    private fun onNError(event: DeviceEvent) {

//        ToastUtils.showLong(getString(R.string.connect_fail))
        SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

        TelinkLightService.Instance()?.idleMode(true)
        TelinkLog.d("DeviceScanningActivity#onNError")

        val builder = android.support.v7.app.AlertDialog.Builder(this)
        builder.setMessage("当前环境:Android7.0!连接重试:" + " 3次失败!")
        builder.setNegativeButton("confirm") { dialog, _ -> dialog.dismiss() }
        builder.setCancelable(false)
        builder.show()
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
                   //("changeToScene to device with better RSSI  new meshAddr = ${deviceInfo.meshAddress} rssi = ${deviceInfo.rssi}")
                    bestRSSIDevice = deviceInfo
                }
            } else {
               //("RSSI  meshAddr = ${deviceInfo.meshAddress} rssi = ${deviceInfo.rssi}")
                bestRSSIDevice = deviceInfo
            }

        }

    }

    private fun stopConnectTimer() {
        mConnectDisposal?.dispose()
    }

    private fun isSwitch(uuid: Int): Boolean {
        return when (uuid) {
            DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SENSOR, DeviceType.NIGHT_LIGHT -> {
               //("This is switch")
                true
            }
            else -> {
                false

            }
        }
    }

    protected var toast: Toast? = null
    protected var foreground = false
    private var loadDialog: Dialog? = null

    private var bestRSSIDevice: DeviceInfo? = null

    private var mReceive: BluetoothStateBroadcastReceive? = null

    private val connectFailedDeviceMacList: MutableList<String> = mutableListOf()

    private var retryConnectCount = 0

    private var mApplication: TelinkLightApplication? = null

    private var mScanTimeoutDisposal: Disposable? = null

    private var mConnectDisposal: Disposable? = null

    private var acitivityIsAlive = true

    private var mScanDisposal: Disposable? = null

    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        foreground = true
        this.mApplication = this.application as TelinkLightApplication
        registerBluetoothReceiver()
    }

//    fun addEventListeners() {
//        // 监听各种事件
//        addScanListeners()
//        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        this.mApplication?.addEventListener(NotificationEvent.ONLINE_STATUS, this)
////        this.mApplication?.addEventListener(NotificationEvent.GET_ALARM, this)
//        this.mApplication?.addEventListener(NotificationEvent.GET_DEVICE_STATE, this)
//        this.mApplication?.addEventListener(ServiceEvent.SERVICE_CONNECTED, this)
////        this.mApplication?.addEventListener(MeshEvent.OFFLINE, this)
//        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
//    }

    override fun onStart() {
        super.onStart()
//        registerBluetoothReceiver()
    }

    //增加全局监听蓝牙开启状态
    private fun showOpenBluetoothDialog(context: Context) {
        val dialogTip = AlertDialog.Builder(context)
        dialogTip.setMessage(R.string.openBluetooth)
        dialogTip.setPositiveButton(android.R.string.ok) { dialog, which ->
            LeBluetooth.getInstance().enable(applicationContext)
        }
        dialogTip.setCancelable(false)
        dialogTip.create().show()
    }

    override fun onPause() {
        super.onPause()
        foreground = false
    }

    private fun unregisterBluetoothReceiver() {
        if (mReceive != null) {
            unregisterReceiver(mReceive)
            mReceive = null
        }
    }

    private fun registerBluetoothReceiver() {
        if (mReceive == null) {
            mReceive = BluetoothStateBroadcastReceive()
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF")
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON")
        registerReceiver(mReceive, intentFilter)
    }


    override fun onResume() {
        super.onResume()
        foreground = true
        val blueadapter = BluetoothAdapter.getDefaultAdapter()
        if (blueadapter?.isEnabled == false) {
            showOpenBluetoothDialog(ActivityUtils.getTopActivity())
//            retryConnect()
            if (toolbar != null) {
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener(View.OnClickListener {
                    var dialog = BluetoothConnectionFailedDialog(this, R.style.Dialog)
                    dialog.show()
                })
            }
        } else {
            if (toolbar != null) {
                if (TelinkLightApplication.getInstance().connectDevice == null) {
//                    retryConnect()
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener {
                        var dialog = BluetoothConnectionFailedDialog(this, R.style.Dialog)
                        dialog.show()
                    }
                } else {
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = false
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        this.toast!!.cancel()
        this.toast = null
        unregisterBluetoothReceiver()
        mScanDisposal?.dispose()
    }

    fun showToast(s: CharSequence) {
        ToastUtils.showLong(s)
    }

    protected fun saveLog(log: String) {
        (application as TelinkLightApplication).saveLog(log)
    }

    fun showLoadingDialog(content: String) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.dialogview, null)

        val layout = v.findViewById<View>(R.id.dialog_view) as LinearLayout
        val tvContent = v.findViewById<View>(R.id.tvContent) as TextView
        tvContent.text = content

        //        ImageView spaceshipImage = (ImageView) v.findViewById(R.id.img);
        //        @SuppressLint("ResourceType") Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(this, R.animator.load_animation);
        //        spaceshipImage.startAnimation(hyperspaceJumpAnimation);

        if (loadDialog == null) {
            loadDialog = Dialog(this,
                    R.style.FullHeightDialog)
        }
        //loadDialog没显示才把它显示出来
        if (!loadDialog!!.isShowing) {
            loadDialog!!.setCancelable(false)
            loadDialog!!.setCanceledOnTouchOutside(false)
            loadDialog!!.setContentView(layout)
            if (!this.isDestroyed) {
                GlobalScope.launch(Dispatchers.Main) {
                    loadDialog!!.show()
                }
            }
        }
    }

    fun hideLoadingDialog() {
        GlobalScope.launch(Dispatchers.Main) {
            if (loadDialog != null && this.isActive) {
                loadDialog!!.dismiss()
            }
        }
    }

    fun compileExChar(str: String): Boolean {
        return StringUtils.compileExChar(str)
    }


    inner class BluetoothStateBroadcastReceive : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (toolbar != null) {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = false
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (toolbar != null) {
//                        retryConnect()
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener {
                            var dialog = BluetoothConnectionFailedDialog(context, R.style.Dialog)
                            dialog.show()
                        }
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                        BluetoothAdapter.STATE_OFF -> {
                            if (toolbar != null) {
//                                retryConnect()
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener {
                                    var dialog = BluetoothConnectionFailedDialog(context, R.style.Dialog)
                                    dialog.show()
                                }
                            }
                        }
                        BluetoothAdapter.STATE_ON -> {
                            if (toolbar != null) {
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = false
                            }
                        }
                    }
                }
            }
        }
    }

//    private fun retryConnect() {
//        if (retryConnectCount < MAX_RETRY_CONNECT_TIME) {
//            retryConnectCount++
//            if(TelinkLightService.Instance()?.).adapter.mLightCtrl.currentLight!=null){
//                if (TelinkLightService.Instance()?.).adapter.mLightCtrl.currentLight?.isConnected != true)
//                    startScan()
//                else
//                    login()
//            }
//        } else {
//            TelinkLightService.Instance()?.).idleMode(true)
//            startScan()
//
//        }
//    }

//    private fun retryConnect() {
//        if (retryConnectCount < MAX_RETRY_CONNECT_TIME) {
//            retryConnectCount++
//            if (TelinkLightService.Instance()?.).adapter.mLightCtrl.currentLight?.isConnected != true)
//                startScan()
//            else
//                login()
//        } else {
//            TelinkLightService.Instance()?.).idleMode(true)
//            if (!scanPb.isShown) {
//                retryConnectCount = 0
//                connectFailedDeviceMacList.clear()
//                startScan()
//            }
//
//        }
//    }

//    @SuppressLint("CheckResult")
//    private fun startScan() {
//        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
//                Manifest.permission.BLUETOOTH_ADMIN)
//                .subscribeOn(Schedulers.io())
//                .subscribe {
//                    if (it) {
//                        TelinkLightService.Instance()?.).idleMode(true)
//                        bestRSSIDevice = null   //扫描前置空信号最好设备。
//                        //扫描参数
//                        val account = DBUtils.lastUser?.account
//
//                        val scanFilters = ArrayList<ScanFilter>()
//                        val scanFilter = ScanFilter.Builder()
//                                .setDeviceName(account)
//                                .build()
//                        scanFilters.add(scanFilter)
//
//                        val params = LeScanParameters.create()
//                        if (!AppUtils.isExynosSoc()) {
//                            params.setScanFilters(scanFilters)
//                        }
//                        params.setMeshName(account)
//                        params.setOutOfMeshName(account)
//                        params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
//                        params.setScanMode(false)
//
//                        addScanListeners()
//                        TelinkLightService.Instance()?.).startScan(params)
//                        startCheckRSSITimer()
//
//                    } else {
//                        //没有授予权限
//                        DialogUtils.showNoBlePermissionDialog(this, {
//                            retryConnectCount = 0
//                            startScan()
//                        }, { finish() })
//                    }
//                }
//    }

//    @SuppressLint("CheckResult")
//    private fun startScan() {
//        //当App在前台时，才进行扫描。
//            if (acitivityIsAlive || !(mScanDisposal?.isDisposed ?: false)) {
//               //("startScanLight_LightOfGroup")
//                mScanDisposal = RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
//                        Manifest.permission.BLUETOOTH_ADMIN)
//                        .subscribeOn(Schedulers.io())
//                        .subscribe {
//                            if (it) {
//                                TelinkLightService.Instance()?.).idleMode(true)
//                                bestRSSIDevice = null   //扫描前置空信号最好设备。
//                                //扫描参数
//                                val account = DBUtils.lastUser?.account
//
//                                val scanFilters = java.util.ArrayList<ScanFilter>()
//                                val scanFilter = ScanFilter.Builder()
//                                        .setDeviceName(account)
//                                        .build()
//                                scanFilters.add(scanFilter)
//
//                                val params = LeScanParameters.create()
//                                if (!com.dadoutek.uled.util.AppUtils.isExynosSoc()) {
//                                    params.setScanFilters(scanFilters)
//                                }
//                                params.setMeshName(account)
//                                params.setOutOfMeshName(account)
//                                params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
//                                params.setScanMode(false)
//
//                                addScanListeners()
//                                TelinkLightService.Instance()?.).startScan(params)
//                                startCheckRSSITimer()
//
//
//                            } else {
//                                //没有授予权限
//                                DialogUtils.showNoBlePermissionDialog(this, {
//                                    retryConnectCount = 0
//                                    startScan()
//                                }, { finish() })
//                            }
//                        }
//            }
//    }

//    private fun addScanListeners() {
////        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)
////        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
////        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)
//    }
//
//    private fun login() {
//        val account = DBUtils.lastUser?.account
//        val pwd = NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16)
//        TelinkLightService.Instance()?.).login(Strings.stringToBytes(account, 16)
//                , Strings.stringToBytes(pwd, 16))
//    }

//    private fun startCheckRSSITimer() {
//        mScanTimeoutDisposal?.dispose()
//        val periodCount = SCAN_TIMEOUT_SECOND.toLong() - SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND
//        Observable.intervalRange(1, periodCount, SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND, 1,
//                TimeUnit.SECONDS, AndroidSchedulers.mainThread())
//                .subscribe(object : Observer<Long?> {
//                    override fun onComplete() {
//                       //("onLeScanTimeout()")
//                        onLeScanTimeout()
//                    }
//
//                    override fun onSubscribe(d: Disposable) {
//                        mScanTimeoutDisposal = d
//                    }
//
//                    override fun onNext(t: Long) {
//                        if (bestRSSIDevice != null) {
//                            mScanTimeoutDisposal?.dispose()
//                           //("connect device , mac = ${bestRSSIDevice?.macAddress}  rssi = ${bestRSSIDevice?.rssi}")
//                            connect(bestRSSIDevice!!.macAddress)
//                        }
//                    }
//
//                    override fun onError(e: Throwable) {
//                        Log.d("SawTest", "error = $e")
//
//                    }
//                })
//    }

//    fun onLeScanTimeout() {
//       //("onErrorReport: onLeScanTimeout")
////        if (mConnectSnackBar) {
//        startScan()
//    }
//        } else {
//            retryConnect()
//        }

//    @SuppressLint("CheckResult")
//    private fun connect(mac: String) {
//        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
//                Manifest.permission.BLUETOOTH_ADMIN)
//                .subscribe {
//                    if (it) {
//                        //授予了权限
//                        if (TelinkLightService.Instance()?.) != null) {
//                            progressBar?.visibility = View.VISIBLE
//                            TelinkLightService.Instance()?.).connect(mac, CONNECT_TIMEOUT)
//                            startConnectTimer()
//
//                        }
//                    } else {
//                        //没有授予权限
//                        DialogUtils.showNoBlePermissionDialog(this, { connect(mac) }, { finish() })
//                    }
//                }
//
//
//    }
//
//    private fun startConnectTimer() {
//        mConnectDisposal?.dispose()
//        mConnectDisposal = Observable.timer(CONNECT_TIMEOUT.toLong(), TimeUnit.SECONDS)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe {
////                    retryConnect()
//                }
//    }

}

