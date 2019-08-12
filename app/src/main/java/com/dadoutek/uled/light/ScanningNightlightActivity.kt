package com.dadoutek.uled.light

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.os.Bundle
import android.view.MenuItem
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.BuildConfig
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.AppUtils
import com.dd.processbutton.iml.ActionProcessButton
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
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.startActivity
import java.util.concurrent.TimeUnit

class ScanningNightlightActivity :TelinkBaseActivity(), EventListener<String> {
    private val SCAN_TIMEOUT_SECOND: Int = 8
    private val CONNECT_TIMEOUT_SECONDS: Int = 5
    private val MAX_RETRY_CONNECT_TIME = 3

    private var mRetryConnectCount: Int = 0

    private var mDeviceInfo: DeviceInfo? = null

    private var connectDisposable: Disposable? = null

    private lateinit var mApplication: TelinkLightApplication

    private var scanDisposable: Disposable? = null
    private var mDeviceMeshName: String = Constant.PIR_SWITCH_MESH_NAME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning_light_light)
        this.mApplication = this.application as TelinkLightApplication
        TelinkLightService.Instance()?.idleMode(true)
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
        supportActionBar?.title = getString(R.string.light_light_title)
    }

    private fun initListener() {
        progressBtn.onClick {
            mRetryConnectCount = 0
            startScan()
        }
    }

    private fun getScanFilters(): MutableList<ScanFilter>{
        val scanFilters = ArrayList<ScanFilter>()

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
                    if(!AppUtils.isExynosSoc()){
                        params.setScanFilters(getScanFilters())
                    }
                    //把当前的mesh设置为out_of_mesh，这样也能扫描到已配置过的设备
                    params.setOutOfMeshName(mesh.name)
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

               //("pir开始扫描")
                progressBtn.setMode(ActionProcessButton.Mode.ENDLESS)   //设置成intermediate的进度条
                progressBtn.progress = 50   //在2-99之间随便设一个值，进度条就会开始动
            } else {

            }
        }
    }

    private fun retryConnect() {
        mRetryConnectCount++
       //("reconnect time = $mRetryConnectCount")
        if (mRetryConnectCount > MAX_RETRY_CONNECT_TIME) {
            showConnectFailed()
        } else {
            if (TelinkLightApplication.getInstance().connectDevice == null)
                connect()   //重新进行连接
            else
                login()   //重新进行登录
        }
    }

    override fun onResume() {
        super.onResume()
        progressBtn.progress = 0
    }

    override fun onPause() {
        super.onPause()
        connectDisposable?.dispose()
        scanDisposable?.dispose()
        this.mApplication.removeEventListener(this)
    }

    override fun performed(event: Event<String>?) {
        when (event?.type) {
            LeScanEvent.LE_SCAN -> this.onLeScan(event as LeScanEvent)
//            LeScanEvent.LE_SCAN_TIMEOUT -> this.onLeScanTimeout()
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
//            LeScanEvent.LE_SCAN_COMPLETED -> this.onLeScanTimeout()
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
            }

//            NotificationEvent.GET_GROUP -> this.onGetGroupEvent(event as NotificationEvent)
//            MeshEvent.ERROR -> this.onMeshEvent(event as MeshEvent)
        }
    }

    private fun onErrorReport(info: ErrorReportInfo) {
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        //"蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        //"无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        //"未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        //"未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        //"未建立物理连接")

                    }
                }
               //("onError retry")
                retryConnect()

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        //"value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        //"read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        //"write login data 没有收到response")
                    }
                }
               //("onError login")
                retryConnect()

            }
        }
    }

    private fun onLeScanTimeout() {
       //("onLeScanTimeout")
        GlobalScope.launch(Dispatchers.Main){
            progressBtn.progress = -1   //控件显示Error状态
            progressBtn.text = getString(R.string.not_find_pir)
        }
    }


    private fun onDeviceStatusChanged(deviceEvent: DeviceEvent) {
        val deviceInfo = deviceEvent.args
        mDeviceMeshName = deviceInfo.meshName

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                onLogin()
               //("登陆成功")
            }
            LightAdapter.STATUS_LOGOUT -> {
//                onLoginFailed()
            }

            LightAdapter.STATUS_CONNECTED -> {
                connectDisposable?.dispose()
                login()
               //("开始登陆")
            }
        }

    }

    private fun login() {
        val mesh = TelinkLightApplication.getApp().mesh
        val pwd: String
        if (mDeviceMeshName == Constant.PIR_SWITCH_MESH_NAME) {
            pwd = mesh.factoryPassword.toString()
        }else{
            pwd = NetworkFactory.md5(NetworkFactory.md5(mDeviceMeshName) + mDeviceMeshName)
                    .substring(0, 16)
        }
        TelinkLightService.Instance()?.login(Strings.stringToBytes(mDeviceMeshName, 16)
                , Strings.stringToBytes(pwd, 16))

    }

    private fun onLogin() {
        TelinkLightService.Instance()?.enableNotification()
        mApplication.removeEventListener(this)
        connectDisposable?.dispose()
        scanDisposable?.dispose()
        progressBtn.progress = 100  //进度控件显示成完成状态
        if (mDeviceInfo?.productUUID == DeviceType.NIGHT_LIGHT) {
            startActivity<ConfigNightlightActivity>("deviceInfo" to mDeviceInfo!!)
        }
    }


    private fun showConnectFailed() {
        mApplication.removeEventListener(this)
        TelinkLightService.Instance()?.idleMode(true)

       //("showConnectFailed")
        progressBtn.progress = -1    //控件显示Error状态
        progressBtn.text = getString(R.string.connect_failed)
    }


    private fun connect() {
        Thread {

            mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this@ScanningNightlightActivity)
            mApplication.addEventListener(ErrorReportEvent.ERROR_REPORT, this@ScanningNightlightActivity)
            TelinkLightService.Instance()?.connect(mDeviceInfo?.macAddress, CONNECT_TIMEOUT_SECONDS)
           //("开始连接")
        }.start()

        GlobalScope.launch(Dispatchers.Main) {
            connectDisposable?.dispose()    //取消掉上一个超时计时器
            connectDisposable = Observable.timer(CONNECT_TIMEOUT_SECONDS.toLong(), TimeUnit
                    .SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                       //("timeout retryConnect")
                        retryConnect()
                    }
        }

    }

    private fun doFinish() {
        this.mApplication.removeEventListener(this)
        TelinkLightService.Instance()?.idleMode(true)
        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
    }


    override fun onBackPressed() {
        this.doFinish()
    }

    private fun onLeScan(leScanEvent: LeScanEvent) {
        val mesh = this.mApplication.mesh
        val meshAddress = Constant.SWITCH_PIR_ADDRESS

        if (meshAddress == -1) {
            this.doFinish()
            return
        }

        val MAX_RSSI = 81
        when (leScanEvent.args.productUUID) {
            DeviceType.NIGHT_LIGHT -> {
                if(leScanEvent.args.rssi<MAX_RSSI){
                    scanDisposable?.dispose()
                    LeBluetooth.getInstance().stopScan()
                    mDeviceInfo = leScanEvent.args
                    connect()
                    progressBtn.text = getString(R.string.connecting)
                }else{
                    ToastUtils.showLong(getString(R.string.rssi_low))
                }
            }
        }
    }
}