package com.dadoutek.uled.util

import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.util.Log
import com.dadoutek.uled.model.Constants
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.DeviceInfo
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
const val SCAN_TIMEOUT_SECOND: Int = 20
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1

class ReconnectionBluetooth : EventListener<String> {

    private var application: Any? = null

    private var connectMeshAddress: Int = 0

    private var retryConnectCount = 0

    private var mConnectDisposal: Disposable? = null

    private var bestRSSIDevice: DeviceInfo? = null

    private var mApplication: TelinkLightApplication? = null

    private val connectFailedDeviceMacList: MutableList<String> = mutableListOf()

    private var mScanTimeoutDisposal: Disposable? = null

    private var mScanDisposal: Disposable? = null

    private var acitivityIsAlive = true

    public fun ReconnectionBluetooth(application: TelinkLightApplication){
        this.application =application
        this.mApplication = this.application as TelinkLightApplication
    }


    override fun performed(event: Event<String>) {
        when (event.type) {
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
        }
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {

                TelinkLightService.Instance()?.enableNotification()
                TelinkLightService.Instance()?.updateNotification()
                GlobalScope.launch(Dispatchers.Main) {
                    stopConnectTimer()
                    delay(300)
                }

                val connectDevice = this.mApplication?.connectDevice
                if (connectDevice != null) {
                    this.connectMeshAddress = connectDevice.meshAddress
                }

//                scanPb.visibility = View.GONE
//                adapter?.notifyDataSetChanged()
                SharedPreferencesHelper.putBoolean(mApplication, Constants.CONNECT_STATE_SUCCESS_KEY, true)
            }
            LightAdapter.STATUS_LOGOUT -> {
                retryConnect()
            }
            LightAdapter.STATUS_CONNECTING -> {
                Log.d("connectting", "444")
//                scanPb.visibility = View.VISIBLE
            }
            LightAdapter.STATUS_CONNECTED -> {
                if (TelinkLightService.Instance()!=null/*&&!TelinkLightService.Instance()!!.isLogin*/)
                    login()
            }
            LightAdapter.STATUS_ERROR_N -> onNError(event)
        }
    }

    private fun onNError(event: DeviceEvent) {
        SharedPreferencesHelper.putBoolean(mApplication, Constants.CONNECT_STATE_SUCCESS_KEY, false)

        TelinkLightService.Instance()?.idleMode(true)
        TelinkLog.d("DeviceScanningActivity#onNError")
    }

    private fun login() {
        val meshName = DBUtils.lastUser?.controlMeshName
        val pwd = NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16)
        TelinkLightService.Instance()?.login(Strings.stringToBytes(meshName, 16)
                , Strings.stringToBytes(pwd, 16))
    }

    private fun retryConnect() {
        if (retryConnectCount < MAX_RETRY_CONNECT_TIME) {
            retryConnectCount++
            if (TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight?.isConnected != true)
                startScan()
            else
                login()
        } else {
            TelinkLightService.Instance()?.idleMode(true)
            retryConnectCount = 0
            connectFailedDeviceMacList.clear()
            startScan()
        }
    }

    private fun stopConnectTimer() {
        mConnectDisposal?.dispose()
    }

    @SuppressLint("CheckResult")
    private fun startScan() {
        //当App在前台时，才进行扫描。
        if (acitivityIsAlive || !(mScanDisposal?.isDisposed ?: false)) {
           //("startScanLight_LightOfGroup")
            TelinkLightService.Instance()?.idleMode(true)
            bestRSSIDevice = null   //扫描前置空信号最好设备。
            //扫描参数
            val account = DBUtils.lastUser?.account

            val scanFilters = java.util.ArrayList<ScanFilter>()
            val scanFilter = ScanFilter.Builder()
                    .setDeviceName(account)
                    .build()
            scanFilters.add(scanFilter)

            val params = LeScanParameters.create()
           // if (!AppUtils.isExynosSoc)
                params.setScanFilters(scanFilters)

            params.setMeshName(account)
            params.setOutOfMeshName(account)
            params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
            params.setScanMode(false)

            addScanListeners()
            TelinkLightService.Instance()?.startScan(params)
            startCheckRSSITimer()


        }
    }

    private fun addScanListeners() {
//        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)
//        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
//        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)
    }

    private fun startCheckRSSITimer() {
        mScanTimeoutDisposal?.dispose()
        val periodCount = SCAN_TIMEOUT_SECOND.toLong() - SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND
        Observable.intervalRange(1, periodCount, SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND, 1,
                TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Long?> {
                    override fun onComplete() {
                       //("onLeScanTimeout()")
                        onLeScanTimeout()
                    }

                    override fun onSubscribe(d: Disposable) {
                        mScanTimeoutDisposal = d
                    }

                    override fun onNext(t: Long) {
                        if (bestRSSIDevice != null) {
                            mScanTimeoutDisposal?.dispose()
                           //("connect device , mac = ${bestRSSIDevice?.macAddress}  rssi = ${bestRSSIDevice?.rssi}")
                            connect(bestRSSIDevice!!.macAddress)
                        }
                    }

                    override fun onError(e: Throwable) {
                        Log.d("SawTest", "error = $e")

                    }
                })
    }

    private fun onLeScanTimeout() {
       //("onErrorReport: onLeScanTimeout")
//        if (mConnectSnackBar) {
        startScan()
    }

    @SuppressLint("CheckResult")
    private fun connect(mac: String) {
        //授予了权限
        if (TelinkLightService.Instance() != null) {
            TelinkLightService.Instance()?.connect(mac, CONNECT_TIMEOUT)
            startConnectTimer()

        }

    }

    private fun startConnectTimer() {
        mConnectDisposal?.dispose()
        mConnectDisposal = Observable.timer(CONNECT_TIMEOUT.toLong(), TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                },{})
    }


}

