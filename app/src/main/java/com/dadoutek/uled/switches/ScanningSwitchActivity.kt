package com.dadoutek.uled.switches

import android.Manifest
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.BuildConfig
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dd.processbutton.iml.ActionProcessButton
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LeScanParameters
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_scanning_switch.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.startActivity
import java.util.concurrent.TimeUnit

class ScanningSwitchActivity : AppCompatActivity(), EventListener<String> {
    private val SCAN_TIMEOUT_SECOND: Int = 5
    private val CONNECT_TIMEOUT_SECONDS: Int = 5
    private val MAX_RETRY_CONNECT_TIME = 3

    private var mRetryConnectCount: Int = 0
    private var mConnected: Boolean = false
    private var mScanned: Boolean = false

    private var mDeviceInfo: DeviceInfo? = null

    private var connectTimer: Disposable? = null

    private lateinit var mApplication: TelinkLightApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning_switch)
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
        supportActionBar?.title = getString(R.string.install_switch)
    }

    private fun initListener() {
        progressBtn.onClick {
            mRetryConnectCount = 0
            mScanned = false
            mConnected = false
            startScan()
        }
    }


    private fun startScan() {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN).subscribe { granted ->
            if (granted) {
                TelinkLightService.Instance().idleMode(true)
                val mesh = mApplication.mesh
                //扫描参数
                val params = LeScanParameters.create()
                if (BuildConfig.DEBUG) {
                    params.setMeshName(Constant.TEST_MESH_NAME)
                } else {
                    params.setMeshName(mesh.factoryName)
                }

                params.setOutOfMeshName(Constant.OUT_OF_MESH_NAME)
                params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                params.setScanMode(false)

                this.mApplication.addEventListener(LeScanEvent.LE_SCAN, this)
                this.mApplication.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)

                TelinkLightService.Instance()?.startScan(params)

                progressBtn.setMode(ActionProcessButton.Mode.ENDLESS)   //设置成intermediate的进度条
                progressBtn.progress = 50   //在2-99之间随便设一个值，进度条就会开始动

                launch(UI) {
                    delay(SCAN_TIMEOUT_SECOND.toLong(), TimeUnit.SECONDS)
                    if (!mScanned) {
                        onLeScanTimeout()
                    }
                }
            } else {

            }
        }

    }


    override fun onPause() {
        super.onPause()
        this.mApplication.removeEventListener(this)
    }

    override fun performed(event: Event<String>?) {
        when (event?.type) {
            LeScanEvent.LE_SCAN -> this.onLeScan(event as LeScanEvent)
            LeScanEvent.LE_SCAN_TIMEOUT -> this.onLeScanTimeout()
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            LeScanEvent.LE_SCAN_COMPLETED -> this.onLeScanTimeout()
            ErrorReportEvent.ERROR_REPORT -> this.onErrorReport(event as ErrorReportEvent)
//            NotificationEvent.GET_GROUP -> this.onGetGroupEvent(event as NotificationEvent)
//            MeshEvent.ERROR -> this.onMeshEvent(event as MeshEvent)
        }
    }

    private fun onErrorReport(event: ErrorReportEvent) {
        val info = event.args
        Log.d("Saw", "ScanningActivity#performed#ERROR_REPORT: " + " stateCode-" + info.stateCode
                + " errorCode-" + info.errorCode
                + " deviceId-" + info.deviceId)
    }

    private fun onLeScanTimeout() {
        progressBtn.progress = -1   //控件显示Error状态
        progressBtn.text = getString(R.string.not_found_switch)
    }


    /**
     * 登录，实际上就是BLE连接成功之后的一些数据交互和notify enable.
     */
    private fun onLoginFailed() {
        connectTimer?.dispose()
        mApplication.removeEventListener(this)
        mRetryConnectCount++
        LogUtils.d("reconnect time = $mRetryConnectCount")
        if (mRetryConnectCount > MAX_RETRY_CONNECT_TIME) {
            showConnectFailed()
        } else {
            connect()   //重新进行连接
        }
    }

    private fun onDeviceStatusChanged(deviceEvent: DeviceEvent) {
        val deviceInfo = deviceEvent.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                onLogin()
            }
            LightAdapter.STATUS_LOGOUT -> {
                onLoginFailed()
            }

        }

    }

    private fun onLogin() {
        connectTimer?.dispose()
        mApplication.removeEventListener(this)
        mConnected = true
        progressBtn.progress = 100  //进度控件显示成完成状态
        if (mDeviceInfo?.productUUID == DeviceType.NORMAL_SWITCH ||
                mDeviceInfo?.productUUID == DeviceType.NORMAL_SWITCH2) {
            startActivity<ConfigNormalSwitchActivity>("deviceInfo" to mDeviceInfo!!)
        } else if (mDeviceInfo?.productUUID == DeviceType.SCENE_SWITCH) {
            startActivity<ConfigSceneSwitchActivity>("deviceInfo" to mDeviceInfo!!)
        }
    }


    private fun showConnectFailed() {
        progressBtn.progress = -1    //控件显示Error状态
        progressBtn.text = getString(R.string.connect_failed)
        LogUtils.d("connected failed")
    }


    private fun connect() {
        launch(UI) {
            mConnected = false

            val mesh = TelinkLightApplication.getApp().mesh
            //自动重连参数
            val connectParams = Parameters.createAutoConnectParameters()
            if (BuildConfig.DEBUG) {
                connectParams.setMeshName(Constant.TEST_MESH_NAME)
            } else {
                connectParams.setMeshName(mesh.factoryName)
            }
            connectParams.setPassword(mesh.factoryPassword)
            connectParams.autoEnableNotification(true)
            connectParams.setConnectMac(mDeviceInfo?.macAddress)
            connectParams.setTimeoutSeconds(CONNECT_TIMEOUT_SECONDS)

            mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this@ScanningSwitchActivity)
            TelinkLightService.Instance().autoConnect(connectParams)
            connectTimer = Observable.timer(CONNECT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                    .subscribe { onLoginFailed() }
//            TelinkLightService.Instance()?.connect(mDeviceInfo?.macAddress, TIMEOUTSECONDS)
//            delay(TIMEOUTSECONDS.toLong(), TimeUnit.SECONDS)
//            if (!mConnected) {
//                showConnectFailed()
//            }
        }

    }

    private fun doFinish() {
        this.mApplication.removeEventListener(this)
        TelinkLightService.Instance().idleMode(true)
        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
    }

    override fun onBackPressed() {
        this.doFinish()
    }

    private fun onLeScan(leScanEvent: LeScanEvent) {
        val mesh = this.mApplication.mesh
        val meshAddress = mesh.generateMeshAddr()

        if (meshAddress == -1) {
//            this.showToast(getString(R.string.much_lamp_tip))
            this.doFinish()
            return
        }
        //更新参数
        val params = Parameters.createUpdateParameters()
        if (BuildConfig.DEBUG) {
            params.setOldMeshName(Constant.TEST_MESH_NAME)
        } else {
            params.setOldMeshName(mesh.factoryName)
        }
        params.setOldPassword(mesh.factoryPassword)
        params.setNewMeshName(mesh.name)
        val account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                Constant.DB_NAME_KEY, "dadou")
        if (SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                        Constant.USER_TYPE, Constant.USER_TYPE_OLD) == Constant.USER_TYPE_NEW) {
            params.setNewPassword(NetworkFactory.md5(
                    NetworkFactory.md5(mesh?.password) + account))
        } else {
            params.setNewPassword(mesh?.password)
        }

        Log.d("Saw", "onLeScan leScanEvent.args.productUUID = " + leScanEvent.args.productUUID)
        if (!mScanned)
            when (leScanEvent.args.productUUID) {
                DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2 -> {
                    mScanned = true
                    LeBluetooth.getInstance().stopScan()
                    mDeviceInfo = leScanEvent.args
                    params.setUpdateDeviceList(mDeviceInfo)
                    connect()
                    progressBtn.text = getString(R.string.connecting)
                }

                else -> {
                    LogUtils.d("Switch UUID = ${leScanEvent.args.productUUID}")
                    TelinkLightService.Instance()?.idleMode(true)
                }
            }
    }
}
