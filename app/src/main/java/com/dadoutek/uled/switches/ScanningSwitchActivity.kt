package com.dadoutek.uled.switches

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Constant.VENDOR_ID
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.DialogUtils
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
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_scanning_switch.*
import kotlinx.android.synthetic.main.template_scanning_device.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.startActivity
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val MAX_RETRY_CONNECT_TIME = 1

/**
 * 描述	      ${搜索连接开关}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${为类添加标识}$
 */
class ScanningSwitchActivity : TelinkBaseActivity(), EventListener<String> {

    private var isSeachedDevice: Boolean = false
    private var connectDisposable: Disposable? = null
    private lateinit var mApplication: TelinkLightApplication
    private var mRxPermission: RxPermissions? = null
    private var mDeviceMeshName: String = Constant.PIR_SWITCH_MESH_NAME
    private var bestRSSIDevice: DeviceInfo? = null
    private var mScanTimeoutDisposal: Disposable? = null
    private var mConnectDisposal: Disposable? = null
    private var retryConnectCount = 0
    private var isSupportInstallOldDevice = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning_switch)
        this.mApplication = this.application as TelinkLightApplication
        mRxPermission = RxPermissions(this)
        initView()
        initListener()
        readyConnection()
        startScan()
    }

    private var mIntervalCheckConnection: Disposable? = null

    @SuppressLint("CheckResult")
    private fun readyConnection() {
        val b = TelinkLightService.Instance() != null && !TelinkLightService.Instance()!!.isLogin
        mIntervalCheckConnection = Observable.interval(0, 200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //如果已经断连或者检测超过10次，直接完成。
                    if (b || it > 2) {
                        onInited()
                    }
                }, {})

        TelinkLightService.Instance()?.idleMode(true)
        TelinkLightService.Instance()?.disconnect()
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
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.switch_title)
        scanning_device_ly.visibility = View.GONE
        retryConnectCount = 0
        isSupportInstallOldDevice = false
    }

    private fun initListener() {
        mApplication.removeEventListener(this)
        mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this@ScanningSwitchActivity)

        device_stop_scan.setOnClickListener {
            if (!isSeachedDevice)
                scanFail()
            else
                ToastUtils.showShort(getString(R.string.connecting_tip))
        }

        progressBtn.onClick {
            retryConnectCount = 0
            isSupportInstallOldDevice = false
            startScan()
        }
    }

    //扫描失败处理方法
    private fun scanFail() {
        showToast(getString(R.string.scan_end))
        closeAnimation()
        stopConnectTimer()
        doFinish()
    }

    private fun getScanFilters(): MutableList<ScanFilter> {
        val scanFilters = ArrayList<ScanFilter>()

        scanFilters.add(ScanFilter.Builder()
                .setManufacturerData(VENDOR_ID,
                        byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.NORMAL_SWITCH.toByte()),
                        byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte()))
                .build())

        scanFilters.add(ScanFilter.Builder()
                .setManufacturerData(VENDOR_ID,
                        byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.SCENE_SWITCH.toByte()),
                        byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte()))
                .build())
        scanFilters.add(ScanFilter.Builder()
                .setManufacturerData(VENDOR_ID,
                        byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.NORMAL_SWITCH2.toByte()),
                        byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte()))
                .build())
        scanFilters.add(ScanFilter.Builder()
                .setManufacturerData(VENDOR_ID,
                        byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.SMART_CURTAIN_SWITCH.toByte()),
                        byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte()))
                .build())
        return scanFilters
    }

    @SuppressLint("CheckResult")
    private fun startScan() {
        startAnimation()
        LeBluetooth.getInstance().stopScan()
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN).subscribe({ granted ->
            if (granted) {

                bestRSSIDevice = null   //扫描前置空信号最好设备。
                TelinkLightService.Instance()?.idleMode(true)
                val mesh = mApplication.mesh

                //扫描参数
                val params = LeScanParameters.create()
                    params.setMeshName(mesh.factoryName)


//                if (!AppUtils.isExynosSoc) {
////                    params.setScanFilters(getScanFilters())
//                }
                params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                params.setScanMode(false)

                this.mApplication.addEventListener(LeScanEvent.LE_SCAN, this)
                this.mApplication.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)

                startCheckRSSITimer()
                TelinkLightService.Instance()?.startScan(params)

                progressBtn.setMode(ActionProcessButton.Mode.ENDLESS)   //设置成intermediate的进度条
                progressBtn.progress = 50   //在2-99之间随便设一个值，进度条就会开始动
            }
        }, {})
    }

    private fun startAnimation() {
        scanning_device_ly.visibility = View.VISIBLE
        switch_add_device_btn.visibility = View.GONE
        device_lottieAnimationView.playAnimation()
        isSeachedDevice = false
    }

    private fun closeAnimation() {
        scanning_device_ly.visibility = View.GONE
        switch_add_device_btn.visibility = View.GONE
        device_lottieAnimationView.cancelAnimation()
    }

    private fun startCheckRSSITimer() {
        mScanTimeoutDisposal?.dispose()
        val periodCount = SCAN_TIMEOUT_SECOND.toLong() - SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND
        Observable.intervalRange(1, periodCount, SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND, 1,
                TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Long?> {
                    override fun onComplete() {
                        onLeScanTimeout()
                    }

                    override fun onSubscribe(d: Disposable) {
                        mScanTimeoutDisposal = d
                    }

                    override fun onNext(t: Long) {
                        if (bestRSSIDevice != null) {
                            mScanTimeoutDisposal?.dispose()
                            connect(bestRSSIDevice!!.macAddress)
                        }
                    }

                    override fun onError(e: Throwable) {
                        Log.d("SawTest", "error = $e")
                    }
                })
    }


    override fun onResume() {
        super.onResume()
        progressBtn.progress = 0
        disableConnectionStatusListener()//停止base内部的设备变化监听 不让其自动创建对象否则会重复
    }

    override fun onPause() {
        super.onPause()
        mScanTimeoutDisposal?.dispose()
        connectDisposable?.dispose()
        mConnectDisposal?.dispose()
        this.mApplication.removeEventListener(this)
        stopConnectTimer()
    }

    override fun performed(event: Event<String>?) {
        when (event?.type) {
            LeScanEvent.LE_SCAN -> this.onLeScan(event as LeScanEvent)
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
                closeAnimation()
                doFinish()
            }
        }
    }

    private fun onErrorReport(info: ErrorReportInfo) {
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        LogUtils.e("蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        LogUtils.e("无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        LogUtils.e("未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        LogUtils.e("未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        LogUtils.e("未建立物理连接")

                    }
                }
                retryConnect()

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        LogUtils.e("value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        LogUtils.e("read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        LogUtils.e("write login data 没有收到response")
                    }
                }
                LogUtils.e("onError login")
                retryConnect()

            }
        }
    }

    private fun onLeScanTimeout() {
        GlobalScope.launch(Dispatchers.Main) {
            retryConnectCount = 0

            progressBtn.progress = -1   //控件显示Error状态
            progressBtn.text = getString(R.string.not_found_switch)
        }
    }


    private fun onDeviceStatusChanged(deviceEvent: DeviceEvent) {
        val deviceInfo = deviceEvent.args
        mDeviceMeshName = deviceInfo.meshName
        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                onLogin()
                stopConnectTimer()
            }
            LightAdapter.STATUS_LOGOUT -> {
                GlobalScope.launch(Dispatchers.Main) {
                    if (!mIsInited)
                        onInited()
                }
            }

            LightAdapter.STATUS_CONNECTED -> {
                connectDisposable?.dispose()
                login()
            }
        }

    }

    private fun onInited() {
        mIntervalCheckConnection?.dispose()
        mIsInited = true
    }

    private fun login() {
        isSeachedDevice = false
        val mesh = TelinkLightApplication.getApp().mesh
        val pwd: String

        pwd = if (mDeviceMeshName == Constant.PIR_SWITCH_MESH_NAME) {
            mesh.factoryPassword.toString()
        } else {
            val meshName = DBUtils.lastUser?.controlMeshName
            NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16)
        }
        TelinkLightService.Instance()?.login(Strings.stringToBytes(mDeviceMeshName, 16)
                , Strings.stringToBytes(pwd, 16))
    }

    private fun onLogin() {
        TelinkLightService.Instance()?.enableNotification()
        mApplication.removeEventListener(this)
        connectDisposable?.dispose()
        mScanTimeoutDisposal?.dispose()

        progressBtn.progress = 100  //进度控件显示成完成状态

        closeAnimation()
        hideLoadingDialog()
        if (bestRSSIDevice?.productUUID == DeviceType.NORMAL_SWITCH ||
                bestRSSIDevice?.productUUID == DeviceType.NORMAL_SWITCH2) {
            startActivity<ConfigNormalSwitchActivity>("deviceInfo" to bestRSSIDevice!!,"group" to "false")
        } else if (bestRSSIDevice?.productUUID == DeviceType.SCENE_SWITCH) {
            startActivity<ConfigSceneSwitchActivity>("deviceInfo" to bestRSSIDevice!!,"group" to "false")
        } else if (bestRSSIDevice?.productUUID == DeviceType.SMART_CURTAIN_SWITCH) {
            startActivity<ConfigCurtainSwitchActivity>("deviceInfo" to bestRSSIDevice!!,"group" to "false")
        }
        finish()
    }


    private fun showConnectFailed() {
        hideLoadingDialog()
        mApplication.removeEventListener(this)
        TelinkLightService.Instance()?.idleMode(true)

        progressBtn.progress = -1    //控件显示Error状态
        progressBtn.text = getString(R.string.connect_failed)
        doFinish()
    }

    @SuppressLint("CheckResult")
    private fun connect(mac: String) {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN)
                .subscribe {
                    if (it) {
                        showLoadingDialog(getString(R.string.connecting))
                        closeAnimation()
                        switch_add_device_btn.visibility = View.GONE
                        isSeachedDevice = true
                        //授予了权限
                        if (TelinkLightService.Instance() != null) {
                            mApplication.removeEventListener(this)
                            mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this@ScanningSwitchActivity)
                            mApplication.addEventListener(ErrorReportEvent.ERROR_REPORT, this@ScanningSwitchActivity)
                            this.mApplication.addEventListener(LeScanEvent.LE_SCAN, this)
                            this.mApplication.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
                            TelinkLightService.Instance()?.connect(mac, CONNECT_TIMEOUT)
                            startConnectTimer()

                            progressBtn.text = getString(R.string.connecting)
                        }
                    } else {
                        //没有授予权限
                        DialogUtils.showNoBlePermissionDialog(this, { connect(mac) }, { finish() })
                    }
                }
    }

    private fun stopConnectTimer() {
        mConnectDisposal?.dispose()
        progressBtn.progress = 0
    }

    private fun startConnectTimer() {
        isSeachedDevice = false
        mConnectDisposal?.dispose()
        mConnectDisposal = Observable.timer(CONNECT_TIMEOUT.toLong(), TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ retryConnect() }, {})
    }

    /**
     * 重试
     * 先扫描到信号比较好的，再连接。
     */
    private fun retryConnect() {
        if (retryConnectCount < MAX_RETRY_CONNECT_TIME) {
            retryConnectCount++
            if (TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight?.isConnected != true)
                startScan()
            else
                login()
        } else {
            retryConnectCount = 0
            TelinkLightService.Instance()?.idleMode(true)
            showConnectFailed()
        }
    }

    private fun doFinish() {
        this.mApplication.removeEventListener(this)
        TelinkLightService.Instance()?.idleMode(true)
        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
        closeAnimation()
        hideLoadingDialog()
    }


    override fun onBackPressed() {
        this.doFinish()
    }

    private fun onLeScan(leScanEvent: LeScanEvent) {
        val meshAddress = Constant.SWITCH_PIR_ADDRESS
        val deviceInfo: DeviceInfo = leScanEvent.args
        if (meshAddress == -1) {
            this.doFinish()
            return
        }
        when (leScanEvent.args.productUUID) {
            DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SMART_CURTAIN_SWITCH -> {
                val MAX_RSSI = 81
                if (leScanEvent.args.rssi < MAX_RSSI) {
                    if (bestRSSIDevice != null) {
                        //扫到的灯的信号更好并且没有连接失败过就把要连接的灯替换为当前扫到的这个。
                        if (deviceInfo.rssi > bestRSSIDevice?.rssi ?: 0) {
                            LogUtils.e("changeToScene to device with better RSSI  new meshAddr = ${deviceInfo.meshAddress} rssi = ${deviceInfo.rssi}")
                            bestRSSIDevice = deviceInfo
                        }
                    } else {
                        LogUtils.e("RSSI  meshAddr = ${deviceInfo.meshAddress} rssi = ${deviceInfo.rssi}")
                        bestRSSIDevice = deviceInfo
                    }
                } else {
                    ToastUtils.showLong(getString(R.string.rssi_low))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        this.mApplication?.removeEventListener(this)
    }
}
