package com.dadoutek.uled.switches

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.telink.TelinkApplication
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.AppUtils
import com.dadoutek.uled.util.MeshAddressGenerator
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.ErrorReportInfo
import com.telink.bluetooth.light.LeScanParameters
import com.telink.bluetooth.light.LightAdapter
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.Strings
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_scanning_switch.*
import kotlinx.android.synthetic.main.empty_box_view.*
import kotlinx.android.synthetic.main.template_lottie_animation.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.startActivity
import java.util.ArrayList
import java.util.concurrent.TimeUnit

/**
 * 描述	      ${搜索连接开关}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${为类添加标识}$
 */
class ScanningSwitchActivity() : TelinkBaseActivity(), EventListener<String> {
    private val scanTimeoutTime: Int = 24
    private var mRetryConnectCount: Int = 0
    private val MAX_RETRY_CONNECT_TIME = 2
    private var connectDisposable: Disposable? = null
    private var launch: Job? = null
    private var scanDisposable: Disposable? = null
    private var mDeviceInfo: DeviceInfo? = null
    private var mDeviceMeshName: String? = null
    private var count: Int = 0
    private lateinit var mApplication: TelinkLightApplication
    private var mConnectDisposal: Disposable? = null
    private var retryConnectCount = 0
    private val SCAN_TIMEOUT_SECOND: Int = 20
    private val CONNECT_TIMEOUT_SECONDS: Int = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning_switch)
        this.mApplication = this.application as TelinkLightApplication
        TelinkLightApplication.isLoginAccount = false
        initView()
        initListener()
        startScan()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableTimer?.dispose()
        TelinkLightApplication.isLoginAccount = true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                doFinish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private var mIsInited: Boolean = false

    private fun initView() {
        mIsInited = false
        toolbarTv?.text = getString(R.string.switch_title)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            if (isScanning) {
                cancelf.isClickable = true
                confirmf.isClickable = true
                popFinish.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
            } else {
                finish()
            }
        }
        retryConnectCount = 0
    }

    private fun initListener() {
        cancelf.setOnClickListener { popFinish?.dismiss() }
        confirmf.setOnClickListener {
            popFinish?.dismiss()
            stopConnectTimer()
            closeAnimation()
            doFinish()
        }
        btn_stop_scan.setOnClickListener {
            if (isScanning) {
                scanFail()
                doFinish()
            } else
                startScan()
        }
        scanning_num.setOnClickListener {
            if (isScanning)
                seeHelpe("#QA8")
        }
    }

    override fun performed(event: Event<String>?) {
        event ?: return
        LogUtils.e("zcl***************扫描*******Event${event.type}")
        when (event.type) {
            LeScanEvent.LE_SCAN -> this.onLeScan(event as LeScanEvent)
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
                showToast(getString(R.string.scan_end))
                doFinish()
            }
            LeScanEvent.LE_SCAN_TIMEOUT -> {
                scanFail()
            }

        }
    }

    private fun onDeviceStatusChanged(deviceEvent: DeviceEvent) {
        val deviceInfo= deviceEvent.args
        mDeviceMeshName = deviceInfo.meshName
        LogUtils.e("zcl*********扫描连接变化 ******deviceInfo*******$deviceInfo---")
        LogUtils.e("zcl*********扫描连接变化 ********mDeviceInfo*****$mDeviceInfo--")

        when (deviceInfo.status) {
            LightAdapter.STATUS_CONNECTING -> {//0
                LogUtils.e("zcl 链接中")
            }

            LightAdapter.STATUS_LOGIN -> {//3
                connectDisposable?.dispose()
                onLogin(deviceInfo)
            }

            LightAdapter.STATUS_LOGOUT -> {//4
                LogUtils.e("zcl 链接失败")
                retryConnect()
            }

            LightAdapter.STATUS_CONNECTED -> {//11
                LogUtils.e("zcl 开始登陆")
                login()
            }
        }
    }

    private fun login() {
        LogUtils.e("zcl**********************进入登录$")
        val mesh = TelinkLightApplication.getApp().mesh
        val pwd = if (mDeviceMeshName == Constant.PIR_SWITCH_MESH_NAME)
            mesh.factoryPassword.toString()
        // NetworkFactory.md5(NetworkFactory.md5(mDeviceMeshName) + mDeviceMeshName).substring(0, 16)
        else
            NetworkFactory.md5(NetworkFactory.md5(mDeviceMeshName) + mDeviceMeshName).substring(0, 16)

        LogUtils.d("zcl开始连接${mDeviceInfo?.macAddress}-----------$pwd---------${DBUtils.lastUser?.controlMeshName}")
        TelinkLightService.Instance()?.login(Strings.stringToBytes(mDeviceMeshName, 16), Strings.stringToBytes(pwd, 16))
    }

    private fun onLeScan(leScanEvent: LeScanEvent) {
        setScanningMode(true)
        //val meshAddress = Constant.SWITCH_PIR_ADDRESS
        val meshAddress = MeshAddressGenerator().meshAddress.get()
        LogUtils.v("zcl-----------传感器扫描-------${leScanEvent.args.macAddress}")
        if (meshAddress == -1) {
            this.doFinish()
            return
        }

        val MAX_RSSI = 81
        when (leScanEvent.args.productUUID) {
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.DOUBLE_SWITCH,
            DeviceType.SCENE_SWITCH, DeviceType.EIGHT_SWITCH, DeviceType.SMART_CURTAIN_SWITCH -> {
                if (leScanEvent.args.rssi < MAX_RSSI) {
                    scanDisposable?.dispose()
                    LeBluetooth.getInstance().stopScan()
                    mDeviceInfo = leScanEvent.args
                    connect2()
                } else {
                    ToastUtils.showLong(getString(R.string.rssi_low))
                }
            }
        }
    }

    private fun connect2() {
        if (Constant.IS_ROUTE_MODE) return
        mApplication.removeEventListener(this)
        mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        mApplication.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
        btn_stop_scan.text = getString(R.string.connecting_tip)
        scanDisposable?.dispose()

        launch?.cancel()
        launch = GlobalScope.launch {
            delay(200)
            TelinkLightService.Instance()?.connect(mDeviceInfo?.macAddress, CONNECT_TIMEOUT_SECONDS)
        }
        LogUtils.d("zcl开始连接${mDeviceInfo?.macAddress}--------------------${DBUtils.lastUser?.controlMeshName}")

        connectDisposable?.dispose()    //取消掉上一个超时计时器
        connectDisposable = Observable.timer(CONNECT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    LogUtils.e("zcl timeout retryConnect")
                    retryConnect()
                }
    }

    private fun onErrorReport(info: ErrorReportInfo) {
        LogUtils.e("zcl**********************onErrorReport${info.stateCode}")
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        LogUtils.e("zcl 蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        LogUtils.e("zcl 无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        LogUtils.e("zcl  未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        LogUtils.e("zcl 未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        LogUtils.e("zcl 未建立物理连接")

                    }
                }
                LogUtils.e("zcl onError retry")
                retryConnect()

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        LogUtils.e("zcl value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        LogUtils.e("zcl read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        LogUtils.e("zcl write login data 没有收到response")
                    }
                }
                LogUtils.e("zcl onError login")
                retryConnect()
            }
        }
    }

    private fun retryConnect() {
        mRetryConnectCount++
        LogUtils.e("zcl reconnect time = $mRetryConnectCount")
        if (mRetryConnectCount > MAX_RETRY_CONNECT_TIME) {
            showConnectFailed()
        } else {
            if (TelinkLightApplication.getApp().connectDevice == null)
                connect()   //重新进行连接
            else
                login()   //重新进行登录
        }
    }

    private fun showConnectFailed() {
        mApplication.removeEventListener(this)
        hideLoadingDialog()
        LogUtils.e("zcl  showConnectFailed")
        ToastUtils.showLong(getString(R.string.connect_fail))
        btn_stop_scan.text = getString(R.string.scan_retry)
    }

    //扫描失败处理方法
    private fun scanFail() {
        scanning_num.text = getString(R.string.see_help)
        stopConnectTimer()
        closeAnimation()
        //  doFinish()
        btn_stop_scan.text = getString(R.string.scan_retry)
        image_no_group.visibility = View.VISIBLE
    }

    private fun startAnimation() {
        image_no_group.visibility = View.GONE
        setScanningMode(true)
        lottieAnimationView?.playAnimation()
        lottieAnimationView?.visibility = View.VISIBLE
    }


    private fun closeAnimation() {
        setScanningMode(false)
        lottieAnimationView?.cancelAnimation()
        lottieAnimationView?.visibility = View.GONE
        hideLoadingDialog()
    }


    @SuppressLint("CheckResult")
    private fun startScan() {
        btn_stop_scan.text = getString(R.string.stop_scan)
           RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                   Manifest.permission.BLUETOOTH_ADMIN).subscribe({ granted ->
               if (granted) {
                   startAnimation()
                   Thread {
                       TelinkLightService.Instance()?.idleMode(true)
                       TelinkLightService.Instance()?.disconnect()
                       val mesh = mApplication?.mesh
                       //扫描参数
                       val params = LeScanParameters.create()
                       if (!AppUtils.isExynosSoc)
                           params.setScanFilters(getSwitchFilters())

                       params.setMeshName(mesh.factoryName)
                       params.setOutOfMeshName(Constant.OUT_OF_MESH_NAME)
                       params.setTimeoutSeconds(scanTimeoutTime)
                       params.setScanMode(true)

                       mApplication.removeEventListener(this)
                       mApplication.addEventListener(LeScanEvent.LE_SCAN, this)
                       mApplication.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
                       TelinkApplication.getInstance().isConnect=false
                       Thread.sleep(1500)
                       TelinkLightService.Instance()?.startScan(params)


                       scanDisposable?.dispose()
                       scanDisposable = Observable.timer(SCAN_TIMEOUT_SECOND.toLong(), TimeUnit.SECONDS)
                               .subscribeOn(Schedulers.io())
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe {
                                   onLeScanTimeout()
                               }

                   }.start()
                   LogUtils.e("zcl 开关开始扫描")
               }
           }, {})

       /* startAnimation()
        TelinkLightService.Instance()?.idleMode(true)
        TelinkLightService.Instance()?.disconnect()
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(15000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val deviceTypes = mutableListOf(DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2,
                            DeviceType.SCENE_SWITCH, DeviceType.DOUBLE_SWITCH, DeviceType.SMART_CURTAIN_SWITCH, DeviceType.EIGHT_SWITCH)
                    connect(meshName = Constant.DEFAULT_MESH_FACTORY_NAME, meshPwd = Constant.DEFAULT_MESH_FACTORY_PASSWORD,
                            retryTimes = 3, deviceTypes = deviceTypes, fastestMode = true)
                            ?.subscribeOn(Schedulers.io())
                            ?.observeOn(AndroidSchedulers.mainThread())
                            ?.subscribe({
                                mDeviceInfo = it
                                onLogin(it)
                            }, {
                                scanFail()
                            })
                }*/

    }


    private fun onLeScanTimeout() {
        LogUtils.e("zcl onLeScanTimeout")
        GlobalScope.launch(Dispatchers.Main) {
            ToastUtils.showLong(R.string.not_find_pir)
        }
        btn_stop_scan.text = getString(R.string.scan_retry)
        closeAnimation()
    }

    private fun getSwitchFilters(): MutableList<ScanFilter> {
        val scanFilters = ArrayList<ScanFilter>()
        scanFilters.add(ScanFilter.Builder().setManufacturerData(Constant.VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.NORMAL_SWITCH.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        scanFilters.add(ScanFilter.Builder().setManufacturerData(Constant.VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.NORMAL_SWITCH2.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        scanFilters.add(ScanFilter.Builder().setManufacturerData(Constant.VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.EIGHT_SWITCH.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        scanFilters.add(ScanFilter.Builder().setManufacturerData(Constant.VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.DOUBLE_SWITCH.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        scanFilters.add(ScanFilter.Builder().setManufacturerData(Constant.VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.SCENE_SWITCH.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        scanFilters.add(ScanFilter.Builder().setManufacturerData(Constant.VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.SMART_CURTAIN_SWITCH.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        return scanFilters
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


    override fun onResume() {
        super.onResume()
        stopTimerUpdate()
        disableConnectionStatusListener()//停止base内部的设备变化监听 不让其自动创建对象否则会重复
    }

    override fun onPause() {
        super.onPause()
        mConnectDisposal?.dispose()
        stopConnectTimer()
    }

    private fun onLogin(deviceInfo: DeviceInfo) {
        mDeviceInfo = deviceInfo
        count + 1
        showLoadingDialog(getString(R.string.please_wait))
        if (mDeviceInfo != null) {
            mApplication.removeEventListener(this)
            scanDisposable?.dispose()
/*
            val meshAddress = mDeviceInfo!!.meshAddress
            val mac = mDeviceInfo!!.sixByteMacAddress.split(":")
            if (mac != null && mac.size >= 6) {
                val mac1 = Integer.valueOf(mac[2], 16)
                val mac2 = Integer.valueOf(mac[3], 16)
                val mac3 = Integer.valueOf(mac[4], 16)
                val mac4 = Integer.valueOf(mac[5], 16)

                val instance = Calendar.getInstance()
                val second = instance.get(Calendar.SECOND).toByte()
                val minute = instance.get(Calendar.MINUTE).toByte()
                val hour = instance.get(Calendar.HOUR_OF_DAY).toByte()
                val day = instance.get(Calendar.DAY_OF_MONTH).toByte()
                val byteArrayOf = byteArrayOf((meshAddress and 0xFF).toByte(), (meshAddress shr 8 and 0xFF).toByte(),
                        mac1.toByte(), mac2.toByte(), mac3.toByte(), mac4.toByte(),second,minute,hour,day)
                TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.TIME_ZONE, meshAddress, byteArrayOf)
            }*/
            LogUtils.v("zcl-----------扫描等候获取版本-------${mDeviceInfo!!.meshAddress}")
            val disposable = Commander.getDeviceVersion(mDeviceInfo!!.meshAddress, retryTimes = 2)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ version ->
                        LogUtils.v("zcl-----------扫描等候获取版本------version-$version")
                        if (version != null && version != "") {
                            skipSwitch(version)
                            finish()
                        } else {
                            val version1 = mDeviceInfo?.firmwareRevision ?: ""
                            if (TextUtils.isEmpty(version1))
                                skipSwitch(version1)
                            else
                                skipSwitch(version1)
                            finish()
                        }
                        closeAnimation()
                    }, {
                        LogUtils.v("zcl-----------扫描等候获取版本失败------version-")
                        //showToast(getString(R.string.get_version_fail))
                        closeAnimation()
                        val version1 = mDeviceInfo?.firmwareRevision ?: ""
                        if (TextUtils.isEmpty(version1))
                        //ToastUtils.showLong(getString(R.string.get_version_fail))
                            skipSwitch(version1)
                        else
                            skipSwitch(version1)
                        finish()
                    })

        }
    }

    private fun skipSwitch(version: String) {
        when (mDeviceInfo?.productUUID) {
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2 -> {
                startActivity<ConfigNormalSwitchActivity>("deviceInfo" to mDeviceInfo!!, "group" to "false", "version" to version)
            }
            DeviceType.DOUBLE_SWITCH -> {
                startActivity<DoubleTouchSwitchActivity>("deviceInfo" to mDeviceInfo!!, "group" to "false", "version" to version)
            }
            DeviceType.SCENE_SWITCH -> {
                if (version.contains(DeviceType.EIGHT_SWITCH_VERSION))
                    startActivity<ConfigEightSwitchActivity>("deviceInfo" to mDeviceInfo!!, "group" to "false", "version" to version)
                else
                    startActivity<ConfigSceneSwitchActivity>("deviceInfo" to mDeviceInfo!!, "group" to "false", "version" to version)
            }

            DeviceType.EIGHT_SWITCH -> {
                startActivity<ConfigEightSwitchActivity>("deviceInfo" to mDeviceInfo!!, "group" to "false", "version" to version)
            }
            DeviceType.SMART_CURTAIN_SWITCH -> {
                startActivity<ConfigCurtainSwitchActivity>("deviceInfo" to mDeviceInfo!!, "group" to "false", "version" to version)
            }
        }
    }


    private fun stopConnectTimer() {
        mConnectDisposal?.dispose()
    }


    private fun doFinish() {
        TelinkLightService.Instance()?.idleMode(true)
        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
        hideLoadingDialog()
    }


    override fun onBackPressed() {
        doFinish()
    }

}
