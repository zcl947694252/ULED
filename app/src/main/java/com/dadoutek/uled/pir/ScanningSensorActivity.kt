package com.dadoutek.uled.pir

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.BuildConfig
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.HumanBodySensorActivity
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.MeshAddressGenerator
import com.dd.processbutton.iml.ActionProcessButton
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
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
import kotlinx.android.synthetic.main.activity_scanning_sensor.*
import kotlinx.android.synthetic.main.template_scanning_device.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.startActivity
import java.util.concurrent.TimeUnit

/**
 * 人体感应器扫描新设备/已连接设备
 */
class ScanningSensorActivity : TelinkBaseActivity(), EventListener<String> {
    private var launch: Job? = null
    private var isSearchedDevice: Boolean = false
    private val SCAN_TIMEOUT_SECOND: Int = 20
    private val CONNECT_TIMEOUT_SECONDS: Int = 30
    private val MAX_RETRY_CONNECT_TIME = 3
    private var mRetryConnectCount: Int = 0
    private var getVersionRetryMaxCount = 2
    private var getVersionRetryCount = 0
    private var mDeviceInfo: DeviceInfo? = null
    private var connectDisposable: Disposable? = null
    private lateinit var mApplication: TelinkLightApplication
    private var scanDisposable: Disposable? = null
    private var mDeviceMeshName: String = Constant.PIR_SWITCH_MESH_NAME
    private var isSupportInstallOldDevice = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning_sensor)
        this.mApplication = this.application as TelinkLightApplication
        TelinkLightService.Instance()?.idleMode(true)
        TelinkLightService.Instance()?.disconnect()
        initView()
        initListener()
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

    private fun initView() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.sensor_title)
        scanning_device_ly.visibility = View.GONE
    }

    private fun initListener() {
        setNewScan()
        device_stop_scan.setOnClickListener {
            if (!isSearchedDevice)
                scanFail()
            else
                ToastUtils.showLong(getString(R.string.connecting_tip))
        }
        progressBtn.onClick {
            setNewScan()
        }
    }

    //扫描失败处理方法
    private fun scanFail() {
        showToast(getString(R.string.scan_end))
        doFinish()
    }


    private fun setNewScan() {
        mRetryConnectCount = 0
        isSupportInstallOldDevice = false
        startScan()
    }

    private fun startAnimation() {
        device_lottieAnimationView?.playAnimation()
        start_scanning_sensor_ly?.visibility = View.GONE
        scanning_device_ly.visibility = View.VISIBLE
    }


    private fun closeAnimal() {
        device_lottieAnimationView.cancelAnimation()
        start_scanning_sensor_ly.visibility = View.GONE
        scanning_device_ly.visibility = View.GONE
    }


    private fun getScanFilters(): MutableList<ScanFilter> {
        val scanFilters = ArrayList<ScanFilter>()
        scanFilters.add(ScanFilter.Builder()
                .setManufacturerData(Constant.VENDOR_ID,
                        byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.SENSOR.toByte()),
                        byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte()))
                .build())
        scanFilters.add(ScanFilter.Builder()
                .setManufacturerData(Constant.VENDOR_ID,
                        byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.NIGHT_LIGHT.toByte()),
                        byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte()))
                .build())
        return scanFilters
    }

    @SuppressLint("CheckResult")
    private fun startScan() {

        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN).subscribe { granted ->
            if (granted) {
                startAnimation()
                Thread {
                    TelinkLightService.Instance()?.idleMode(true)
                    val mesh = mApplication.mesh
                    //扫描参数
                    val params = LeScanParameters.create()

                    if (BuildConfig.DEBUG) {
                        params.setMeshName(Constant.PIR_SWITCH_MESH_NAME)
                    } else {
                        params.setMeshName(mesh.factoryName)
                    }
                   // if (!AppUtils.isExynosSoc)
                        params.setScanFilters(getScanFilters())

                    //把当前的mesh设置为out_of_mesh，这样也能扫描到已配置过的设备
                    if (isSupportInstallOldDevice) {
                        params.setMeshName(mesh.name)
                        params.setOutOfMeshName(mesh.name)
                    }

                    params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                    params.setScanMode(false)


                    this.mApplication.addEventListener(LeScanEvent.LE_SCAN, this)
                    this.mApplication.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)

                    TelinkLightService.Instance()?.startScan(params)

                    scanDisposable?.dispose()
                    scanDisposable = Observable.timer(SCAN_TIMEOUT_SECOND.toLong(), TimeUnit
                            .SECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                onLeScanTimeout()
                            }
                }.start()

                LogUtils.e("zcl pir开始扫描")
                progressBtn.setMode(ActionProcessButton.Mode.ENDLESS)   //设置成intermediate的进度条
                progressBtn.progress = 50   //在2-99之间随便设一个值，进度条就会开始动
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        launch?.cancel()
        this.mApplication?.removeEventListener(this)
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

    override fun onResume() {
        super.onResume()
        progressBtn.progress = 0
        disableConnectionStatusListener()//停止base内部的设备变化监听 不让其自动创建对象否则会重复
    }

    override fun onPause() {
        super.onPause()
        connectDisposable?.dispose()
        scanDisposable?.dispose()
        this.mApplication.removeEventListener(this)
    }

    override fun performed(event: Event<String>?) {
        event ?: return
//        LogUtils.e("zcl**********************Event${event.type}")
        when (event.type) {
            LeScanEvent.LE_SCAN -> this.onLeScan(event as LeScanEvent)
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
                showToast(getString(R.string.scan_end))
                doFinish()
            }
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

    private fun onLeScanTimeout() {
        LogUtils.e("zcl onLeScanTimeout")
        GlobalScope.launch(Dispatchers.Main) {
            progressBtn.progress = -1   //控件显示Error状态
            progressBtn.text = getString(R.string.not_find_pir)
            ToastUtils.showLong(R.string.not_find_pir)
            doFinish()
        }
    }


    private fun onDeviceStatusChanged(deviceEvent: DeviceEvent) {
        val deviceInfo: DeviceInfo = deviceEvent.args
        mDeviceMeshName = deviceInfo.meshName
        LogUtils.e("zcl**********************$deviceInfo")

        when (deviceInfo.status) {
            LightAdapter.STATUS_CONNECTING -> {//0
                LogUtils.e("zcl 链接中")
            }

            LightAdapter.STATUS_LOGIN -> {//3
                connectDisposable?.dispose()
                onLogin()

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
        val pwd: String
        pwd = if (mDeviceMeshName == Constant.PIR_SWITCH_MESH_NAME) {
            mesh.factoryPassword.toString()
        } else {
            NetworkFactory.md5(NetworkFactory.md5(mDeviceMeshName) + mDeviceMeshName).substring(0, 16)
        }
        LogUtils.d("zcl开始连接${mDeviceInfo?.macAddress}-----------$pwd---------${DBUtils.lastUser?.controlMeshName}")
        TelinkLightService.Instance()?.login(Strings.stringToBytes(mDeviceMeshName, 16), Strings.stringToBytes(pwd, 16))
    }

    private fun onLogin() {
        TelinkLightService.Instance()?.enableNotification()
        mApplication.removeEventListener(this)
        connectDisposable?.dispose()
        scanDisposable?.dispose()
        isSearchedDevice = false

        progressBtn.progress = 100  //进度控件显示成完成状态

        LogUtils.e("zcl人体扫描登录跳转前" + DBUtils.getAllSensor())

        getVersion()
    }

    @SuppressLint("CheckResult")
    private fun getVersion() {
        var dstAdress: Int
        if (TelinkApplication.getInstance().connectDevice != null&&mDeviceInfo?.meshAddress!=null) {
            dstAdress = mDeviceInfo?.meshAddress!!
            Commander.getDeviceVersion(dstAdress)
                     .subscribe(
                             {
                                 closeAnimal()
                                 finish()
                                 when {
                                     mDeviceInfo?.productUUID == DeviceType.SENSOR -> startActivity<ConfigSensorAct>("deviceInfo" to mDeviceInfo!!,"version" to it)
                                     mDeviceInfo?.productUUID == DeviceType.NIGHT_LIGHT -> startActivity<HumanBodySensorActivity>("deviceInfo" to mDeviceInfo!!, "update" to "0","version" to it)
                                     else -> ToastUtils.showLong(getString(R.string.scan_end))
                                 }
                             },
                             {
                                 getVersionRetryCount++
                                 if (getVersionRetryCount <= getVersionRetryMaxCount) {
                                     getVersion()
                                 }else{
ToastUtils.showLong(getString(R.string.get_version_fail))
                                     finish()
                                 }
                                 LogUtils.e("zcl配置传感器前失败----$it")
                             }
                     )

        } else {
            ToastUtils.showLong(getString(R.string.get_version_fail))
            doFinish()
        }
    }


    private fun showConnectFailed() {
        mApplication.removeEventListener(this)
        hideLoadingDialog()
        LogUtils.e("zcl  showConnectFailed")
        ToastUtils.showLong(getString(R.string.connect_fail))
        progressBtn.progress = -1    //控件显示Error状态
        progressBtn.text = getString(R.string.connect_fail)
        isSearchedDevice = false
        doFinish()
    }


    private fun connect() {
        //showLoadingDialog(resources.getString(R.string.connecting_tip))
       // closeAnimal()
        mApplication.removeEventListener(this)
        mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        mApplication.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
        progressBtn.text = getString(R.string.connecting)
        device_stop_scan.text = getString(R.string.connecting_tip)
        scanDisposable?.dispose()

        launch?.cancel()
        launch=  GlobalScope.launch {
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

    private fun doFinish() {
        closeAnimal()
        this.mApplication.removeEventListener(this)
        if (TelinkLightService.Instance() != null) {
            TelinkLightService.Instance()?.idleMode(true)
        }
        if (ActivityUtils.isActivityExistsInStack(MainActivity::class.java))
            ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
        else {
            LogUtils.d("MainActivity doesn't exist in stack")
            ToastUtils.showLong("MainActivity doesn't exist in stack")
            finish()
        }
    }


    override fun onBackPressed() {
        this.doFinish()
    }

    private fun onLeScan(leScanEvent: LeScanEvent) {
        isSearchedDevice = false
        //val meshAddress = Constant.SWITCH_PIR_ADDRESS
        val meshAddress = MeshAddressGenerator().meshAddress

        if (meshAddress == -1) {
            this.doFinish()
            return
        }

        val MAX_RSSI = 81
        when (leScanEvent.args.productUUID) {
            DeviceType.SENSOR, DeviceType.NIGHT_LIGHT -> {
                if (leScanEvent.args.rssi < MAX_RSSI) {
                    scanDisposable?.dispose()
                    LeBluetooth.getInstance().stopScan()
                    mDeviceInfo = leScanEvent.args
                    isSearchedDevice = true
                    connect()
                } else {
                    ToastUtils.showLong(getString(R.string.rssi_low))
                }
            }
        }
    }
}
