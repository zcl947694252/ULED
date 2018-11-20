package com.dadoutek.uled.light

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewTreeObserver
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView

import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.LightGroupingActivity
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.DataManager
import com.dadoutek.uled.util.GuideUtils
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.widget.ColorPicker
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.ErrorReportInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_device_setting.*
import kotlinx.android.synthetic.main.fragment_device_setting.*
import kotlinx.android.synthetic.main.fragment_rgb_device_setting.*
import java.util.*
import java.util.concurrent.TimeUnit

class NormalDeviceSettingActivity : TelinkBaseActivity(), EventListener<String> {
    private var localVersion: String? = null

    private var light: DbLight? = null
    private val mDisposable = CompositeDisposable()
    private var mRxPermission: RxPermissions? = null
    var gpAddress: Int = 0
    var fromWhere: String? = null
    private var colorPicker: ColorPicker? = null
    private val dialog: AlertDialog? = null
    private var mApp: TelinkLightApplication? = null
    private var manager: DataManager? = null
    private var mConnectDevice: DeviceInfo? = null
    private var mConnectTimer: Disposable? = null
    private var isLoginSuccess = false
    private var mApplication: TelinkLightApplication? = null

    private val clickListener = OnClickListener { v ->
        when(v.id){
            R.id.img_header_menu_left ->{
                finish()
            }
            R.id.tvOta ->{
                checkPermission()
            }
            R.id.btn_rename ->{
                val intent = Intent(this, RenameLightActivity::class.java)
                intent.putExtra("light", light)
                startActivity(intent)
                this!!.finish()
            }
            R.id.updateGroup ->{
                updateGroup()
            }
            R.id.btnRemove ->{
                remove()
            }
        }
    }

    fun addEventListeners() {
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        this.mApplication?.addEventListener(NotificationEvent.ONLINE_STATUS, this)
////        this.mApplication?.addEventListener(NotificationEvent.GET_ALARM, this)
//        this.mApplication?.addEventListener(NotificationEvent.GET_DEVICE_STATE, this)
//        this.mApplication?.addEventListener(ServiceEvent.SERVICE_CONNECTED, this)
////        this.mApplication?.addEventListener(MeshEvent.OFFLINE, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    override fun onStop() {
        super.onStop()
        mConnectTimer?.dispose()
        this.mApplication?.removeEventListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mConnectTimer?.dispose()
        mDisposable.dispose()
        this.mApplication?.removeEventListener(this)
    }

    private fun updateGroup() {
        val intent = Intent(this,
                LightGroupingActivity::class.java)
        intent.putExtra("light", light)
        intent.putExtra("gpAddress", gpAddress)
        startActivity(intent)
       this!!.finish()
    }

    override fun performed(event: Event<String>?) {
        when (event?.type) {
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            ErrorReportEvent.ERROR_REPORT -> onErrorReport((event as ErrorReportEvent).args)
        }
    }

    internal var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {
        override fun startGetVersion() {
            showLoadingDialog(getString(R.string.verification_version))
        }

        override fun getVersionSuccess(s: String) {
            //            ToastUtils.showLong(R.string.verification_version_success);
            hideLoadingDialog()
        }

        override fun getVersionFail() {
            ToastUtils.showLong(R.string.verification_version_fail)
            hideLoadingDialog()
        }


        override fun downLoadFileSuccess() {
            transformView()
        }

        override fun downLoadFileFail(message: String) {
            ToastUtils.showLong(R.string.download_pack_fail)
        }
    }

    private fun checkPermission() {
        mDisposable.add(
                mRxPermission!!.request(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe { granted ->
                    if (granted!!) {
                        OtaPrepareUtils.instance().gotoUpdateView(this@NormalDeviceSettingActivity, localVersion, otaPrepareListner)
                    } else {
                        ToastUtils.showLong(R.string.update_permission_tip)
                    }
                })
    }

    private fun transformView() {
        val intent = Intent(this@NormalDeviceSettingActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.UPDATE_LIGHT, light)
        startActivity(intent)
        finish()
    }

    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            Commander.getDeviceVersion(light!!.meshAddr, { s ->
                localVersion = s
                if (txtTitle != null) {
                    if (OtaPrepareUtils.instance().checkSupportOta(localVersion)!!) {
                        txtTitle!!.visibility = View.VISIBLE
                        txtTitle!!.text = localVersion
                        light!!.version = localVersion
                        tvOta!!.visibility = View.VISIBLE
                    } else {
                        txtTitle!!.visibility = View.GONE
                        tvOta!!.visibility = View.GONE
                    }
                }
                null
            }, {
                if (txtTitle != null) {
                    txtTitle!!.visibility = View.GONE
                    tvOta!!.visibility = View.GONE
                }
                null
            })
        } else {
            dstAdress = 0
        }
    }

    private fun onErrorReport(info: ErrorReportInfo) {
//        LogUtils.d("onErrorReport current device mac = ${bestRSSIDevice?.macAddress}")
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        com.dadoutek.uled.util.LogUtils.d("蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        com.dadoutek.uled.util.LogUtils.d("无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        com.dadoutek.uled.util.LogUtils.d("未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        com.dadoutek.uled.util.LogUtils.d("未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        com.dadoutek.uled.util.LogUtils.d("未建立物理连接")
                    }
                }

                autoConnect()

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        com.dadoutek.uled.util.LogUtils.d("value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        com.dadoutek.uled.util.LogUtils.d("read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        com.dadoutek.uled.util.LogUtils.d("write login data 没有收到response")
                    }
                }

                autoConnect()

            }
        }
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                hideLoadingDialog()
                isLoginSuccess = true
                mConnectTimer?.dispose()
            }
            LightAdapter.STATUS_LOGOUT -> {
                autoConnect()
                mConnectTimer = Observable.timer(15, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                        .subscribe { aLong ->
                            com.blankj.utilcode.util.LogUtils.d("STATUS_LOGOUT")
                            showLoadingDialog(getString(R.string.connect_failed))
                            finish()
                        }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setContentView(R.layout.activity_device_setting)
        initView()
        getVersion()
        this.mApplication = this.application as TelinkLightApplication
    }

    private fun initView() {
        this.mApp = this.application as TelinkLightApplication?
        manager = DataManager(mApp, mApp!!.mesh.name, mApp!!.mesh.password)
        this.light = this.intent.extras!!.get(Constant.LIGHT_ARESS_KEY) as DbLight
        this.fromWhere = this.intent.getStringExtra(Constant.LIGHT_REFRESH_KEY)
        this.gpAddress = this.intent.getIntExtra(Constant.GROUP_ARESS_KEY, 0)
        txtTitle!!.text = ""
        img_header_menu_left.setOnClickListener(this.clickListener)

        tvOta!!.setOnClickListener(this.clickListener)
        updateGroup.setOnClickListener(this.clickListener)
        btnRemove.setOnClickListener(this.clickListener)
        mRxPermission = RxPermissions(this)

        this.sbBrightness?.max = 100
        this.sbTemperature?.max = 100

        this.colorPicker = findViewById(R.id.color_picker)
//        this.colorPicker.setOnColorChangeListener(this.colorChangedListener);
        mConnectDevice = TelinkLightApplication.getInstance().connectDevice

        btnRename.visibility = View.GONE
        sbBrightness?.progress = light!!.brightness
        tvBrightness.text = getString(R.string.device_setting_brightness, light?.brightness.toString() + "")
        sbTemperature?.progress = light!!.colorTemperature
        tvTemperature.text = getString(R.string.device_setting_temperature, light?.colorTemperature.toString() + "")

//        sendInitCmd(light.getBrightness(),light.getColorTemperature());

        this.sbBrightness?.setOnSeekBarChangeListener(this.barChangeListener)
        this.sbTemperature?.setOnSeekBarChangeListener(this.barChangeListener)
    }

    private val barChangeListener = object : SeekBar.OnSeekBarChangeListener {

        private var preTime: Long = 0
        private val delayTime = 30

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            this.onValueChange(seekBar, seekBar.progress, true,true)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            this.preTime = System.currentTimeMillis()
            this.onValueChange(seekBar, seekBar.progress, true,false)
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                       fromUser: Boolean) {
            val currentTime = System.currentTimeMillis()

            if (currentTime - this.preTime > this.delayTime) {
                this.onValueChange(seekBar, progress, true,false)
                this.preTime = currentTime
            }
        }

        private fun onValueChange(view: View, progress: Int, immediate: Boolean,isStopTracking:
        Boolean) {

            val addr = light?.meshAddr
            val opcode: Byte
            val params: ByteArray
            if (view === sbBrightness) {
                //                progress += 5;
                //                Log.d(TAG, "onValueChange: "+progress);
                tvBrightness.text = getString(R.string.device_setting_brightness, progress.toString() + "")
                opcode = Opcode.SET_LUM
                params = byteArrayOf(progress.toByte())

                light?.brightness = progress
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)
                if(isStopTracking){
                    DBUtils.updateLight(light!!)
                }
            } else if (view === sbTemperature) {

                opcode = Opcode.SET_TEMPERATURE
                params = byteArrayOf(0x05, progress.toByte())
                tvTemperature.text = getString(R.string.device_setting_temperature, progress.toString() + "")

                light?.colorTemperature = progress
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)
                if(isStopTracking){
                    DBUtils.updateLight(light!!)
                }
            }
        }
    }

    private fun sendInitCmd(brightness: Int, colorTemperature: Int) {
        val addr = light?.meshAddr
        var opcode: Byte
        var params: ByteArray
        //                progress += 5;
        //                Log.d(TAG, "onValueChange: "+progress);
        opcode = Opcode.SET_LUM
        params = byteArrayOf(brightness.toByte())
        light?.brightness = brightness
        TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)

        try {
            Thread.sleep(200)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            opcode = Opcode.SET_TEMPERATURE
            params = byteArrayOf(0x05, colorTemperature.toByte())
            light?.colorTemperature = colorTemperature
            TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params)
        }
    }

    fun remove() {
        AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_light_confirm)
                .setPositiveButton(R.string.btn_ok) { dialog, which ->

                    if (TelinkLightService.Instance().adapter.mLightCtrl.currentLight.isConnected) {
                        val opcode = Opcode.KICK_OUT
                        TelinkLightService.Instance().sendCommandNoResponse(opcode, light!!.getMeshAddr(), null)
                        DBUtils.deleteLight(light!!)
                        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(light!!.getMeshAddr())) {
                            TelinkLightApplication.getApp().mesh.saveOrUpdate(this)
                        }
                        if (mConnectDevice != null) {
                            Log.d(this.javaClass.getSimpleName(), "mConnectDevice.meshAddress = " + mConnectDevice?.meshAddress)
                            Log.d(this.javaClass.getSimpleName(), "light.getMeshAddr() = " + light?.getMeshAddr())
                            if (light?.meshAddr == mConnectDevice?.meshAddress) {
                                this.setResult(Activity.RESULT_OK, Intent().putExtra("data", true))
                            }
                        }
                        this.finish()


                    } else {
                        ToastUtils.showLong("当前处于未连接状态，重连中。。。")
                        this.finish()
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    /**
     * 自动重连
     */
    private fun autoConnect() {

        if (TelinkLightService.Instance() != null) {

            if (TelinkLightService.Instance().mode != LightAdapter.MODE_AUTO_CONNECT_MESH) {

                ToastUtils.showLong(getString(R.string.connecting))
                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

                if (this.mApp!!.isEmptyMesh())
                    return

                //                Lights.getInstance().clear();
                this.mApp?.refreshLights()

                val mesh = this.mApp?.getMesh()

                if (TextUtils.isEmpty(mesh?.name) || TextUtils.isEmpty(mesh?.password)) {
                    TelinkLightService.Instance().idleMode(true)
                    return
                }

                val account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                        Constant.DB_NAME_KEY, "dadou")

                //自动重连参数
                val connectParams = Parameters.createAutoConnectParameters()
                connectParams.setMeshName(mesh?.name)
                if (SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(), Constant.USER_TYPE, Constant.USER_TYPE_OLD) == Constant.USER_TYPE_NEW) {
                    connectParams.setPassword(NetworkFactory.md5(
                            NetworkFactory.md5(mesh?.password) + account))
                } else {
                    connectParams.setPassword(mesh?.password)
                }
                connectParams.autoEnableNotification(true)

                // 之前是否有在做MeshOTA操作，是则继续
                if (mesh!!.isOtaProcessing) {
                    connectParams.setConnectMac(mesh?.otaDevice!!.mac)
                }
                //自动重连
                TelinkLightService.Instance().autoConnect(connectParams)
            }

            //刷新Notify参数
            val refreshNotifyParams = Parameters.createRefreshNotifyParameters()
            refreshNotifyParams.setRefreshRepeatCount(2)
            refreshNotifyParams.setRefreshInterval(2000)
            //开启自动刷新Notify
            TelinkLightService.Instance().autoRefreshNotify(refreshNotifyParams)
        }
    }
}
