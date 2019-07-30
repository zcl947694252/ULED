package com.dadoutek.uled.service

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.le.ScanFilter
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.Light
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.BleUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LeScanParameters
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
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Created by hejiajun on 2018/3/22.
 */

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1
class SendLightsInfo : Service(), EventListener<String> {

    internal var light: Light? = null
    private val bundle: Bundle? = null
    private var mTelinkLightService: TelinkLightService? = null
    private var retryConnectCount = 0
    private val connectFailedDeviceMacList: MutableList<String> = mutableListOf()
    private var mScanDisposal: Disposable? = null
    private var bestRSSIDevice: DeviceInfo? = null
    private var mApplication: TelinkLightApplication? = null
    private var mScanTimeoutDisposal: Disposable? = null
    private var mCheckRssiDisposal: Disposable? = null
    private var mConnectDisposal: Disposable? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
        //        bundle=intent.getExtras().getBundle("");
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        this.mApplication = this.application as TelinkLightApplication
        if (!LeBluetooth.getInstance().isSupport(applicationContext)) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show()
            ActivityUtils.finishAllActivities()
        } else {  //如果蓝牙没开，则弹窗提示用户打开蓝牙
            if (!LeBluetooth.getInstance().isEnabled) {
//                GlobalScope.launch(Dispatchers.Main) {
//                    var root = this@DeviceDetailAct.findViewById<RelativeLayout>(R.id.root)
//                    root.indefiniteSnackbar(R.string.openBluetooth, android.R.string.ok) {
//                        LeBluetooth.getInstance().enable(applicationContext)
//                    }
//                }
            } else {
                //如果位置服务没打开，则提示用户打开位置服务
                if (!BleUtils.isLocationEnable(this)) {
                    GlobalScope.launch(Dispatchers.Main) {
                        //                        showOpenLocationServiceDialog()
                    }
                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        //                        hideLocationServiceDialog()
                    }
                    mTelinkLightService = TelinkLightService.Instance()
                    if (TelinkLightApplication.getInstance().connectDevice == null) {
                        while (TelinkApplication.getInstance()?.serviceStarted == true) {
                            GlobalScope.launch(Dispatchers.Main) {
                                retryConnectCount = 0
                                connectFailedDeviceMacList.clear()
                                startScan()
                            }
                            break
                        }

                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            //                            scanPb?.visibility = View.GONE
                            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.CONNECT_STATE_SUCCESS_KEY, true);
                        }
                    }

                }
            }

        }
    }

    override fun onCreate() {
        super.onCreate()
//        this.mApplication = this.application as TelinkLightApplication
//        if (!LeBluetooth.getInstance().isSupport(applicationContext)) {
//            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show()
//            ActivityUtils.finishAllActivities()
//        } else {  //如果蓝牙没开，则弹窗提示用户打开蓝牙
//            if (!LeBluetooth.getInstance().isEnabled) {
////                GlobalScope.launch(Dispatchers.Main) {
////                    var root = this@DeviceDetailAct.findViewById<RelativeLayout>(R.id.root)
////                    root.indefiniteSnackbar(R.string.openBluetooth, android.R.string.ok) {
////                        LeBluetooth.getInstance().enable(applicationContext)
////                    }
////                }
//            } else {
//                //如果位置服务没打开，则提示用户打开位置服务
//                if (!BleUtils.isLocationEnable(this)) {
//                    GlobalScope.launch(Dispatchers.Main) {
////                        showOpenLocationServiceDialog()
//                    }
//                } else {
//                    GlobalScope.launch(Dispatchers.Main) {
////                        hideLocationServiceDialog()
//                    }
//                    mTelinkLightService = TelinkLightService.Instance()
//                    if (TelinkLightApplication.getInstance().connectDevice == null) {
//                        while (TelinkApplication.getInstance()?.serviceStarted == true) {
//                            GlobalScope.launch(Dispatchers.Main) {
//                                retryConnectCount = 0
//                                connectFailedDeviceMacList.clear()
//                                startScan()
//                            }
//                            break
//                        }
//
//                    } else {
//                        GlobalScope.launch(Dispatchers.Main) {
//                            //                            scanPb?.visibility = View.GONE
//                            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.CONNECT_STATE_SUCCESS_KEY, true);
//                        }
//                    }
//
//                }
//            }
//
//        }
    }

    @SuppressLint("CheckResult")
    private fun startScan() {
        //当App在前台时，才进行扫描。
        if (AppUtils.isAppForeground())
            if (!(mScanDisposal?.isDisposed ?: false)) {
               //"startScanLight_LightOfGroup")
//                mScanDisposal = RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
//                        Manifest.permission.BLUETOOTH_ADMIN)
//                        .subscribeOn(Schedulers.io())
//                        .subscribe {
//                            if (it) {
                TelinkLightService.Instance().idleMode(true)
                bestRSSIDevice = null   //扫描前置空信号最好设备。
                //扫描参数
                val account = DBUtils.lastUser?.account

                val scanFilters = java.util.ArrayList<ScanFilter>()
                val scanFilter = ScanFilter.Builder()
                        .setDeviceName(account)
                        .build()
                scanFilters.add(scanFilter)

                val params = LeScanParameters.create()
                if (!com.dadoutek.uled.util.AppUtils.isExynosSoc()) {
                    params.setScanFilters(scanFilters)
                }
                params.setMeshName(account)
                params.setOutOfMeshName(account)
                params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                params.setScanMode(false)

                addScanListeners()
                TelinkLightService.Instance().startScan(params)
                startCheckRSSITimer()

//                            } else {
//                                //没有授予权限
//                                DialogUtils.showNoBlePermissionDialog(this, {
//                                    retryConnectCount = 0
//                                    startScan()
//                                }, { finish() })
//                            }
//                        }
            }
    }

    private fun addScanListeners() {
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)
    }

    private fun startCheckRSSITimer() {
        mScanTimeoutDisposal?.dispose()
        val periodCount = SCAN_TIMEOUT_SECOND.toLong() - SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND
        Observable.intervalRange(1, periodCount, SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND, 1,
                TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Long?> {
                    override fun onComplete() {
                       //"onLeScanTimeout()")
                        onLeScanTimeout()
                    }

                    override fun onSubscribe(d: Disposable) {
                        mScanTimeoutDisposal = d
                    }

                    override fun onNext(t: Long) {
                        if (bestRSSIDevice != null) {
                            mScanTimeoutDisposal?.dispose()
                           //"connect device , mac = ${bestRSSIDevice?.macAddress}  rssi = ${bestRSSIDevice?.rssi}")
                            connect(bestRSSIDevice!!.macAddress)
                        }
                    }

                    override fun onError(e: Throwable) {
                        Log.d("SawTest", "error = $e")
                    }
                })
    }

    @SuppressLint("CheckResult")
    private fun connect(mac: String) {
        try {
//            mCheckRssiDisposal?.dispose()
//            mCheckRssiDisposal = RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
//                    Manifest.permission.BLUETOOTH_ADMIN)
//                    .subscribe {
//                        if (it) {
                            //授予了权限
                            if (TelinkLightService.Instance() != null) {
//                                progressBar?.visibility = View.VISIBLE
                                TelinkLightService.Instance().connect(mac, CONNECT_TIMEOUT)
                                startConnectTimer()
                            }
//                        } else {
//                            //没有授予权限
////                            DialogUtils.showNoBlePermissionDialog(this, { connect(mac) }, { finish() })
//                        }
//                    }
        } catch (e: Exception) {
            e.printStackTrace()
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

    private fun onLeScanTimeout() {
       //"onErrorReport: onLeScanTimeout")
//        if (mConnectSnackBar) {
//        indefiniteSnackbar(root, R.string.not_found_light, R.string.retry) {
//        TelinkLightService.Instance().idleMode(true)
//        LeBluetooth.getInstance().stopScan()
//        startScan()
//        }
//        } else {
        retryConnect()
//        }

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
//            if (!scanPb.isShown) {
//                retryConnectCount = 0
//                connectFailedDeviceMacList.clear()
//                startScan()
//            }

        }
    }

    private fun login() {
        val account = DBUtils.lastUser?.account
        val pwd = NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16)
        TelinkLightService.Instance().login(Strings.stringToBytes(account, 16)
                , Strings.stringToBytes(pwd, 16))
    }

    override fun performed(event: Event<String>) {

    }
}
