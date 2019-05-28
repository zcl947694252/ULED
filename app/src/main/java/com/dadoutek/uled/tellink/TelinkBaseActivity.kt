package com.dadoutek.uled.tellink

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.LeBluetooth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.indefiniteSnackbar

import java.util.regex.Matcher
import java.util.regex.Pattern
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.ImageView
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.util.AppUtils
import com.dadoutek.uled.util.BluetoothConnectionFailedDialog
import com.dadoutek.uled.util.DialogUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LeScanParameters
import com.telink.util.Strings
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.toolbar
import java.util.ArrayList
import java.util.concurrent.TimeUnit

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1
open class TelinkBaseActivity : AppCompatActivity() {

    protected var toast: Toast? = null
    protected var foreground = false
    private var loadDialog: Dialog? = null

    private var bestRSSIDevice: DeviceInfo? = null

    private var mReceive: BluetoothStateBroadcastReceive? = null

    private var retryConnectCount = 0

    private var mApplication: TelinkLightApplication? = null

    private var mScanTimeoutDisposal: Disposable? = null

    private var mConnectDisposal: Disposable? = null

    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        foreground = true
        this.mApplication = this.application as TelinkLightApplication
        registerBluetoothReceiver()
    }

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
            if (toolbar != null) {
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener(View.OnClickListener {
                    var dialog = BluetoothConnectionFailedDialog(this, R.style.Dialog)
                    dialog.show()
                    retryConnect()
                })
            }
        } else {
            if (toolbar != null) {
                if (TelinkLightApplication.getInstance().connectDevice == null) {
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener(View.OnClickListener {
                        var dialog = BluetoothConnectionFailedDialog(this, R.style.Dialog)
                        dialog.show()
                        retryConnect()
                    })
                } else {
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_yse)
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
        //
        //        @SuppressLint("ResourceType") Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(this,
        //                R.animator.load_animation);

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

    fun endCurrentGuide() {}

    inner class BluetoothStateBroadcastReceive : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (toolbar != null) {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_yse)
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = false
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (toolbar != null) {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener(View.OnClickListener {
                            var dialog = BluetoothConnectionFailedDialog(context, R.style.Dialog)
                            dialog.show()
                            retryConnect()
                        })
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                    when (blueState) {
                        BluetoothAdapter.STATE_OFF -> {
                            if (toolbar != null) {
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener(View.OnClickListener {
                                    var dialog = BluetoothConnectionFailedDialog(context, R.style.Dialog)
                                    dialog.show()
                                    retryConnect()
                                })
                            }
                        }
                        BluetoothAdapter.STATE_ON -> {
                            if (toolbar != null) {
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_yse)
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = false
                            }
                        }
                    }
                }
            }
        }
    }

    private fun retryConnect() {
        if (retryConnectCount < MAX_RETRY_CONNECT_TIME) {
            retryConnectCount++
            if (TelinkLightService.Instance().adapter.mLightCtrl.currentLight?.isConnected != true)
                startScan()
            else
                login()
        } else {
            TelinkLightService.Instance().idleMode(true)
            startScan()

        }
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
                        if (!AppUtils.isExynosSoc()) {
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

    private fun addScanListeners() {
//        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)
//        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
//        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)
    }

    private fun login() {
        val account = DBUtils.lastUser?.account
        val pwd = NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16)
        TelinkLightService.Instance().login(Strings.stringToBytes(account, 16)
                , Strings.stringToBytes(pwd, 16))
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
                            connect(bestRSSIDevice!!.macAddress)
                        }
                    }

                    override fun onError(e: Throwable) {
                        Log.d("SawTest", "error = $e")

                    }
                })
    }

    private fun onLeScanTimeout() {
        com.dadoutek.uled.util.LogUtils.d("onErrorReport: onLeScanTimeout")
//        if (mConnectSnackBar) {
        startScan()
    }
//        } else {
//            retryConnect()
//        }

    @SuppressLint("CheckResult")
    private fun connect(mac: String) {
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
                        DialogUtils.showNoBlePermissionDialog(this, { connect(mac) }, { finish() })
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

}

