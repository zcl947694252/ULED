package com.dadoutek.uled.switches

import android.Manifest
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.TelinkLightApplication
import com.dadoutek.uled.TelinkLightService
import com.dadoutek.uled.intf.NetworkFactory
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.SharedPreferencesHelper
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
import com.telink.util.Strings
import kotlinx.android.synthetic.main.activity_scanning_switch.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.startActivity
import java.util.concurrent.TimeUnit

class ScanningSwitchActivity : AppCompatActivity(), EventListener<String> {
    private val SCAN_TIMEOUT_SECOND: Int = 10

    private lateinit var mApplication: TelinkLightApplication
    private var mRetryLoginCount: Int = 0
    private var mRetryConnectCount: Int = 0
    private var mConnected: Boolean = false
    private var mScanned: Boolean = false
    private var mLogged: Boolean = false

    private var mDeviceInfo: DeviceInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning_switch)
        this.mApplication = this.application as TelinkLightApplication
        initView()
        initListener()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                finish()
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
            if (progressBtn.progress <= 0) {
                mRetryLoginCount = 0
                mRetryConnectCount = 0
                mScanned = false
                mConnected = false
                mLogged = false
                startScan()
            }
        }

    }

    private fun addEventListener() {
        this.mApplication.addEventListener(LeScanEvent.LE_SCAN, this)
        this.mApplication.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
        this.mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this)
    }

    private fun handleIfSupportBle() {
        //检查是否支持蓝牙设备
        if (!LeBluetooth.getInstance().isSupport(applicationContext)) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show()
            this.finish()
            return
        }

        if (!LeBluetooth.getInstance().isEnabled) {
            LeBluetooth.getInstance().enable(applicationContext)
        }
    }


    private fun startScan() {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN).subscribe { granted ->
            if (granted) {
                handleIfSupportBle()
                TelinkLightService.Instance()?.idleMode(true)
                val mesh = mApplication.mesh
                //扫描参数
                val params = LeScanParameters.create()
                params.setMeshName(mesh.factoryName)
                params.setOutOfMeshName(Constant.OUT_OF_MESH_NAME)
                params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                params.setScanMode(false)
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


    override fun onResume() {
        super.onResume()
        addEventListener()
    }

    override fun onPause() {
        super.onPause()
        this.mApplication.removeEventListener(this)
    }

    override fun performed(event: Event<String>?) {
        when (event?.getType()) {
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
    private fun login() {
        if (mRetryLoginCount > 3) {
            onLoginFailed()
        } else {
            val mesh = mApplication.mesh
            val LOGIN_TIMEOUT: Int = 15 //设一个比较长的超时，防止卡住
            mLogged = false
            val loginResult = TelinkLightService.Instance()?.login(Strings.stringToBytes(mesh.factoryName, 16),
                    Strings.stringToBytes(mesh.factoryPassword, 16))
            launch(UI) {
                delay(LOGIN_TIMEOUT.toLong(), TimeUnit.SECONDS) //超时后执行下方代码
                //如果在设定的时间之后还没有登录成功，则会执行onLoginFailed()
                if (!mLogged)
                    onLoginFailed()
            }
            if (loginResult == false) {  //直接返回false就说明没有连接
                connect()   //重新进行连接
            }
            mRetryLoginCount++
        }
    }

    private fun onDeviceStatusChanged(deviceEvent: DeviceEvent) {
        val deviceInfo = deviceEvent.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_CONNECTED -> {
                mConnected = true
                //连接成功后，进行login
                login()

            }
            LightAdapter.STATUS_LOGIN -> {
                mLogged = true
                progressBtn.progress = 100  //进度控件显示成完成状态
//                launch(UI) {
//                    delay(100, TimeUnit.MILLISECONDS)
                if (mDeviceInfo?.productUUID == DeviceType.NORMAL_SWITCH ||
                        mDeviceInfo?.productUUID == DeviceType.NORMAL_SWITCH2) {
                    startActivity<SelectGroupForSwitchActivity>("deviceInfo" to mDeviceInfo)
                } else {
                    startActivity<ConfigSceneSwitchActivity>("deviceInfo" to mDeviceInfo)
                }
//                }
            }
            LightAdapter.STATUS_LOGOUT -> {
                //重试，进行login
                login()
            }

        }

    }

    private fun onConnectFailed() {
        progressBtn.progress = -1    //控件显示Error状态
        progressBtn.text = getString(R.string.connect_failed)
        LogUtils.d("connect failed")
    }

    private fun onLoginFailed() {
        progressBtn.progress = -1    //控件显示Error状态
        progressBtn.text = getString(R.string.connect_failed)
        LogUtils.d("login failed")
    }


    private fun connect() {
        val TIMEOUTSECONDS: Int = 15
        launch(UI) {
            mConnected = false
            TelinkLightService.Instance()?.connect(mDeviceInfo?.macAddress, TIMEOUTSECONDS)
            delay(TIMEOUTSECONDS.toLong(), TimeUnit.SECONDS)
            if (!mConnected) {
                onConnectFailed()
            }
        }

    }


    private fun onLeScan(leScanEvent: LeScanEvent) {
        val mesh = this.mApplication.mesh
        val meshAddress = mesh.deviceAddress

        if (meshAddress == -1) {
//            this.showToast(getString(R.string.much_lamp_tip))
            this.finish()
            return
        }
        //更新参数
        val params = Parameters.createUpdateParameters()
        params.setOldMeshName(mesh.factoryName)
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
                DeviceType.NORMAL_SWITCH -> {
                    mScanned = true
                    LeBluetooth.getInstance().stopScan()
                    mDeviceInfo = leScanEvent.args
                    params.setUpdateDeviceList(mDeviceInfo)
                    connect()
                    progressBtn.text = getString(R.string.connecting)
                }
                DeviceType.SCENE_SWITCH -> {
                    mScanned = true
                    LeBluetooth.getInstance().stopScan()
                    mDeviceInfo = leScanEvent.args
                    params.setUpdateDeviceList(mDeviceInfo)
                    connect()
                    progressBtn.text = getString(R.string.connecting)
                }

                DeviceType.NORMAL_SWITCH2 -> {
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
//                //如果扫到的不是以上两种设备，就重新进行扫描
//                if (mRetryLoginCount > 3) {
//                    progressBtn.progress = -1    //控件显示Error状态
//                    progressBtn.text = getString(R.string.connect_failed)
//                } else {
//                    startScan()
//                    mRetryLoginCount++
//                }
                }
            }
    }
}
