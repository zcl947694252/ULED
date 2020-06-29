package com.dadoutek.uled.ota

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.PowerManager
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.Mesh
import com.dadoutek.uled.model.OtaDevice
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.FileSelectActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.tellink.TelinkMeshErrorDealActivity
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.event.NotificationEvent
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.OtaDeviceInfo
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.Strings
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_ota_update.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 升级页面
 * 思路：
 * 1.由于demo中没有对设备持久化操作，只有通过online_status 来添加和更新设备，
 * 而online_status返回的数据中只有meshAddress能判断设备唯一性
 * 在OTA升级过程中会保存此时现在的所有设备信息（onlineLights），
 * 如果是从MainActivity页面跳转而来（1.自动连接的设备有上报MeshOTA进度信息；2主动连接之前本地保存的设备），需要读取一次版本信息\n
 * 并初始化onlineLights
 * 1. [//onNotificationEvent(NotificationEvent)]；
 * 2. [//onDeviceStatusChanged(DeviceEvent)][；
 *
 * 在开始OTA或者MeshOTA之前都会获取当前设备的OTA状态信息 [OTAUpdateActivity.sendGetDeviceOtaStateCommand],
 * \n\t 并通过 [OTAUpdateActivity.onNotificationEvent]返回状态， 处理不同模式下的不同状态
 * 在continue MeshOTA和MeshOTA模式下 [OTAUpdateActivity.MODE_CONTINUE_MESH_OTA],[OTAUpdateActivity.MODE_MESH_OTA]
 *
 * 校验通过后，会开始动作
 * Action Start by choose correct bin file!
 * Created by Administrator on 2017/4/20.
 */
class OTAUpdateSwitchActivity : TelinkMeshErrorDealActivity(), EventListener<String> {
    private var mode = MODE_IDLE
    private var mWakeLock: PowerManager.WakeLock? = null
    private var mFirmwareData: ByteArray? = null
    private var onlineLights: MutableList<DbSwitch>? = null
    private var dbSwitch: DbSwitch? = null
    private val mesh: Mesh? = null
    private var mPath: String? = null
    private var mTimeFormat: SimpleDateFormat? = null
    private var successCount = 0
    internal var user: DbUser? = null
    private var otaProgress: TextView? = null
    private var toolbar: Toolbar? = null
    private var meshOtaProgress: TextView? = null
    private var tv_log: TextView? = null
    private var tv_version: TextView? = null
    private var sv_log: ScrollView? = null
    private var mSendataDisposal: Disposable? = null
    private val TIME_OUT_SENDDATA: Long = 10
    private var OTA_IS_HAVEN_START = false

    private val delayHandler = Handler()
    @SuppressLint("HandlerLeak")
    private val visibleHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            (msg.obj as View).visibility = msg.what
        }
    }
    @SuppressLint("HandlerLeak")
    private val msgHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MSG_OTA_PROGRESS -> {
                    if (100 > msg.obj as Int)
                        btn_start_update.setText(R.string.updating)
                    otaProgress!!.text = getString(R.string.progress_ota, msg.obj.toString())
                    progress_view.progress = msg.obj as Int
                }
                MSG_MESH_OTA_PROGRESS -> {
                    meshOtaProgress!!.text = getString(R.string.progress_mesh_ota, msg.obj.toString())
                    progress_view.progress = msg.obj as Int
                }

                MSG_LOG -> if (SharedPreferencesUtils.isDeveloperModel()) {
                    sv_log!!.visibility = View.VISIBLE
                    val time = mTimeFormat!!.format(Calendar.getInstance().timeInMillis)
                    tv_log!!.append("\n" + time + ":" + msg.obj.toString())
                    sv_log!!.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                    BluetoothAdapter.STATE_ON -> {
                        run {
                            log("蓝牙打开")
                            TelinkLightService.Instance().idleMode(true)
                            TelinkLightService.Instance().disconnect()
                            LeBluetooth.getInstance().stopScan()
                        }
                        run {
                            log("蓝牙关闭")
                            ToastUtils.showLong(R.string.tip_phone_ble_off)
                            TelinkLightService.Instance().idleMode(true)
                            TelinkLightService.Instance().disconnect()
                            showUpdateFailView()
                        }
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        log("蓝牙关闭")
                        ToastUtils.showLong(R.string.tip_phone_ble_off)
                        TelinkLightService.Instance().idleMode(true)
                        TelinkLightService.Instance().disconnect()
                        showUpdateFailView()
                    }
                }
            }
        }
    }
    private var otaStateTimeout = 0
    private val OTA_STATE_TIMEOUT_MAX = 3
    private val deviceOtaStateTimeoutTask = object : Runnable {
        override fun run() {
            if (otaStateTimeout < OTA_STATE_TIMEOUT_MAX) {
                val opcode = 0xC7.toByte()
                val address = 0x0000
                val params = byteArrayOf(0x20, 0x05)
                TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                        params)
                log("SendCommand 0xC7 getDeviceOtaState")
                otaStateTimeout++
                delayHandler.postDelayed(this, 3000)
            } else {
                log("SendCommand 0xC7 getDeviceOtaState fail")
                delayHandler.removeCallbacks(this)
                if (mode == MODE_OTA) {
                    startOTA()
                } else if (mode == MODE_MESH_OTA) {
                    sendStartMeshOTACommand()
                } else if (mode == MODE_CONTINUE_MESH_OTA) {
                    sendGetVersionCommand()
                }
            }
        }
    }
    private val versionDevices = ArrayList<Int>()
    private var retryCount = 0
    private val getVersionTask = Runnable {
        if (versionDevices.size == onlineLights!!.size || retryCount >= 2) {
            retryCount = 0
            if (mode == MODE_IDLE) {
                start()
            } else if (mode == MODE_CONTINUE_MESH_OTA || mode == MODE_MESH_OTA) {
                if (hasLow()) {
                    sendStartMeshOTACommand()
                } else {
                    log("No device need OTA! Stop")
                    doFinish()
                }
            }
        } else {
            retryCount++
            log("get version retry")
            sendGetDeviceOtaStateCommand()
        }
    }

    private var mFileVersion: String? = null

    internal var mCancelBuilder: AlertDialog.Builder? = null

    internal var mScanDisposal: Disposable? = null

    internal var connectStart = false

    internal var mConnectDisposal: Disposable? = null

    private var connectRetryCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ota_update)
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
        registerReceiver(mReceiver, filter)
        user = DBUtils.lastUser

        if (user == null || TextUtils.isEmpty(user!!.controlMeshName) || TextUtils.isEmpty(user!!.controlMeshPwd)) {
            toast("Mesh Error!")
            finish()
            return
        }

        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        addEventListener()

        select.setOnClickListener { chooseFile() }
        btn_start_update.setOnClickListener { beginToOta() }
    }

    private fun initData() {
        dbSwitch = intent.getSerializableExtra(Constant.UPDATE_LIGHT) as DbSwitch
        log("current-light-mesh" + dbSwitch!!.meshAddr)
        onlineLights = ArrayList()
        onlineLights!!.add(dbSwitch!!)
        log("onlineLights:" + onlineLights!!.size)

        TelinkLightService.Instance()?.idleMode(true)
        LeBluetooth.getInstance().stopScan()
        startScan()
    }

    @SuppressLint("WakelockTimeout")
    override fun onResume() {
        super.onResume()
        if (mWakeLock != null)
            mWakeLock!!.acquire()
    }

    @SuppressLint("InvalidWakeLockTag", "SimpleDateFormat")
    private fun initView() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock")
        //addevenlisntener

        TelinkLightService.Instance().enableNotification()
        sv_log = findViewById<View>(R.id.sv_log) as ScrollView
        tv_version = findViewById<View>(R.id.tv_version) as TextView
        mTimeFormat = SimpleDateFormat("HH:mm:ss.S")


        toolbar = findViewById(R.id.toolbar)
        initToolbar()

        otaProgress = findViewById<View>(R.id.progress_ota) as TextView
        meshOtaProgress = findViewById<View>(R.id.progress_mesh_ota) as TextView
        tv_log = findViewById<View>(R.id.tv_log) as TextView
    }

    private fun initToolbar() {
        toolbarTv!!.setText(R.string.ota_update_title)
        toolbar?.setNavigationIcon(R.drawable.icon_return)
        toolbar?.setNavigationOnClickListener { finish() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onLocationEnable() {}

    private fun addEventListener() {
        TelinkLightApplication.getApp().addEventListener(LeScanEvent.LE_SCAN, this)
        TelinkLightApplication.getApp().addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
        TelinkLightApplication.getApp().addEventListener(DeviceEvent.STATUS_CHANGED, this)
        TelinkLightApplication.getApp().addEventListener(NotificationEvent.GET_DEVICE_STATE, this)
    }


    /**
     * Action start: after get versions
     * hasHigh Confirm action OTA or MeshOTA
     * hasLow Confirm OTA needed
     */
    private fun start() {
        var hasHigh = false
        var hasLow = false
        for (light in onlineLights!!) {
            if (light.version == null || light.version == "") continue
            val compare = compareVersion(light.version, mFileVersion)
            if (compare == 0) {
                hasHigh = true
            } else if (compare == 1) {
                hasLow = true
            }
        }

        if (hasLow) {
            this.mode = MODE_OTA

            val curMeshAddress = TelinkLightApplication.getApp().connectDevice.meshAddress
            val light = getLightByMeshAddress(curMeshAddress)
            if (light != null && compareVersion(light.version, mFileVersion) == 1) {
                sendGetDeviceOtaStateCommand()
            } else {
                startScan()
            }
        } else {
            text_info.visibility = View.VISIBLE
            text_info.setText(R.string.the_last_version)
            btn_start_update.visibility = View.GONE
            log("No device need OTA! Idle")
            select.isEnabled = true
            this.mode = MODE_IDLE
        }
    }


    // 获取本地设备OTA状态信息
    private fun sendGetDeviceOtaStateCommand() {
        otaStateTimeout = 0
        delayHandler.post(deviceOtaStateTimeoutTask)
    }

    private fun sendGetVersionCommand() {
        versionDevices.clear()
        val opcode = 0xC7.toByte()
        val address = dbSwitch!!.meshAddr
        val params = byteArrayOf(0x20, 0x00)
        TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                params)
        log("SendCommand 0xC7 getVersion")
        // 转发次数 * () * interval + 500
        if (this.mode != MODE_COMPLETE)
            delayHandler.postDelayed(getVersionTask, (0x20 * 2 * 40 + 500).toLong())
    }

    private fun hasLow(): Boolean {
        var hasLow = false
        for (light in onlineLights!!) {
            if (light.version == null || light.version == "") continue
            if (compareVersion(light.version, mFileVersion) == 1) {
                hasLow = true
            }
        }

        return hasLow
    }


    fun connectDevice(mac: String) {
        log("connectDevice :$mac")
        btn_start_update.setText(R.string.start_connect)
        TelinkLightService.Instance().connect(mac, TIME_OUT_CONNECT)
    }

    private fun login() {
        log("login")
        val meshName = user!!.controlMeshName
        val pwd = NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16)
        TelinkLightService.Instance().login(Strings.stringToBytes(meshName, 16), Strings.stringToBytes(pwd, 16))
    }

    override fun onPause() {
        super.onPause()
        if (mWakeLock != null) {
            mWakeLock!!.acquire()
        }
        stopConnectTimer()
        stopScanTimer()
    }

    override fun onStop() {
        super.onStop()
        LeBluetooth.getInstance().stopScan()
    }

    override fun onDestroy() {
        super.onDestroy()
        TelinkLog.i("OTAUpdate#onStop#removeEventListener")
        unregisterReceiver(mReceiver)
        TelinkLightApplication.getApp().removeEventListener(this)
        if (this.delayHandler != null) {
            this.delayHandler.removeCallbacksAndMessages(null)
        }
        TelinkLightApplication.getApp().removeEventListener(this)
    }

    private fun updateSuccess() {
        doFinish()
        text_info.visibility = View.VISIBLE
        text_info.setText(R.string.updateSuccess)
        btn_start_update.visibility = View.GONE

        ToastUtils.showLong(R.string.exit_update)
        Handler().postDelayed({ finish() }, 2000)
    }

    private fun doFinish() {
        this.mode = MODE_COMPLETE
        val mesh = TelinkLightApplication.getApp().mesh
        mesh.otaDevice = null
        mesh.saveOrUpdate(this)
        log("Finish: Success Count : $successCount")
        if (successCount == 0) {
            startScan()
        }
    }

    private fun log(log: String) {
        msgHandler.obtainMessage(MSG_LOG, log).sendToTarget()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun finish() {
        super.finish()
        if (this.mode == MODE_MESH_OTA || this.mode == MODE_CONTINUE_MESH_OTA) {
            this.sendStopMeshOTACommand()
        }
        this.mode = MODE_COMPLETE
        TelinkLightService.Instance().idleMode(true)
        TelinkLog.i("OTAUpdate#onStop#removeEventListener")
        TelinkLightApplication.getApp().removeEventListener(this)
    }

    fun back() {
        if (this.mode == MODE_COMPLETE || this.mode == MODE_IDLE) {
            finish()
        } else {
            if (mCancelBuilder == null) {
                mCancelBuilder = AlertDialog.Builder(this)
                mCancelBuilder!!.setTitle(getString(R.string.warning))
                mCancelBuilder!!.setMessage(getString(R.string.is_exit_ota))
                mCancelBuilder!!.setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    sendStopMeshOTACommand()
                    val mesh = TelinkLightApplication.getApp().mesh
                    mesh.otaDevice = null
                    mesh.saveOrUpdate(this@OTAUpdateSwitchActivity)
                    dialog.dismiss()
                    finish()
                }
                mCancelBuilder!!.setNegativeButton(getString(android.R.string.cancel)
                ) { dialog, which -> dialog.dismiss() }
            }
            mCancelBuilder!!.show()
        }
    }

    override fun onBackPressed() {
        back()
    }

    @SuppressLint("SetTextI18n")
    private fun parseFile() {
        try {
            val version = ByteArray(4)
            val stream = FileInputStream(mPath)
            val length = stream.available()
            mFirmwareData = ByteArray(length)
            stream.read(mFirmwareData)

            stream.close()
            System.arraycopy(mFirmwareData!!, 2, version, 0, 4)
            mFileVersion = String(version)
        } catch (e: Exception) {
            mFileVersion = null
            mFirmwareData = null
            mPath = null
        }

        if (mFileVersion == null) {
            Toast.makeText(this, "File parse error!", Toast.LENGTH_SHORT).show()
            this.mPath = null
            mFileVersion = null
            tvFile.text = getString(R.string.select_file, "NULL")
            tv_version!!.text = "File parse error!"
        } else {
            tv_version!!.text = "File Version: " + mFileVersion!!
            tv_version!!.visibility = View.GONE

            local_version.visibility = View.VISIBLE
            local_version.text = getString(R.string.local_version, dbSwitch!!.version)
            server_version.visibility = View.VISIBLE
            server_version.text = getString(R.string.server_version, StringUtils.versionResolutionURL(mPath, 2))

            select.isEnabled = false
            text_info.visibility = View.GONE
            btn_start_update.visibility = View.VISIBLE
            btn_start_update.isClickable = true
        }
    }

    private fun beginToOta() {
        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbSwitch!!.meshAddr)
            startOTA()
        else
            startScan()
    }

    private fun getLightByMeshAddress(meshAddress: Int): DbSwitch? {
        if (onlineLights == null || onlineLights!!.size == 0) return null
        for (light in onlineLights!!) {
            if (light.meshAddr == meshAddress) {
                return light
            }
        }
        return null
    }

    private fun chooseFile() {
        startActivityForResult(Intent(this, FileSelectActivity::class.java), REQUEST_CODE_CHOOSE_FILE)
    }

    /**
     * 是否可升级
     * 0:最新， 1：可升级， -n：null
     */
    fun compareVersion(lightVersion: String, newVersion: String?): Int {
        //        return lightVersion.equals(newVersion) ? 0 : 1;
        return 1
    }

    /**
     * ****************************************泰凌微升级逻辑********************************************
     */

    /**
     * action startScan
     */
    @Synchronized
    private fun startScan() {
        val scanFilters = ArrayList<ScanFilter>()
        val scanFilterBuilder = ScanFilter.Builder()
        scanFilterBuilder.setDeviceName(user!!.controlMeshName)
        if (dbSwitch!!.macAddr.length > 16) {
            scanFilterBuilder.setDeviceAddress(dbSwitch!!.macAddr)
        }
        val scanFilter = scanFilterBuilder.build()
        scanFilters.add(scanFilter)
        btn_start_update?.setText(R.string.start_scan)
        TelinkLightService.Instance().idleMode(true)
        val params = Parameters.createScanParameters()
        //if (!AppUtils.isExynosSoc)
            params.setScanFilters(scanFilters)

        params.setMeshName(user!!.name)
        params.setTimeoutSeconds(TIME_OUT_SCAN)
        if (dbSwitch!!.macAddr.length > 16) {
            params.setScanMac(dbSwitch!!.macAddr)
        }
        TelinkLightService.Instance().startScan(params)
        startScanTimer()
        log("startScan ")
    }

    @SuppressLint("CheckResult")
    private fun startScanTimer() {
        if (mScanDisposal != null) {
            mScanDisposal!!.dispose()
        }
        mScanDisposal = Observable.timer(TIME_OUT_SCAN.toLong(), TimeUnit.SECONDS).subscribe { onScanTimeout() }
    }

    private fun stopScanTimer() {
        if (mScanDisposal != null) {
            mScanDisposal!!.dispose()
        }
    }

    private fun startOTA() {
        this.runOnUiThread {
            text_info.visibility = View.GONE
            btn_start_update.visibility = View.VISIBLE
            btn_start_update.setText(R.string.updating)
        }

        this.mode = MODE_OTA
        visibleHandler.obtainMessage(View.GONE, otaProgress).sendToTarget()

        if (TelinkLightApplication.getApp().connectDevice != null) {
            OTA_IS_HAVEN_START = true
            TelinkLightService.Instance().startOta(mFirmwareData)
        } else {
            startScan()
        }
        log("startOTA ")
    }

    override fun performed(event: Event<String>) {
        when (event.type) {
            LeScanEvent.LE_SCAN -> onLeScan(event as LeScanEvent)
            LeScanEvent.LE_SCAN_COMPLETED -> log("scan complete")
            LeScanEvent.LE_SCAN_TIMEOUT -> {
                log("scan TIMEOUT")
                onScanTimeout()
            }
            DeviceEvent.STATUS_CHANGED -> onDeviceEvent(event as DeviceEvent)
            NotificationEvent.GET_DEVICE_STATE -> onNotificationEvent(event as NotificationEvent)
        }
    }

    private fun onLeScan(event: LeScanEvent) {
        val deviceInfo = event.args
        Log.e("ota progress", "LE_SCAN : " + deviceInfo.macAddress)
        log("on scan : " + deviceInfo.macAddress)

        GlobalScope.launch {
            val dbSwitch = DBUtils.getLightByMeshAddr(deviceInfo.meshAddress)
            if (dbSwitch != null && dbSwitch.macAddr == "0") {
                dbSwitch.macAddr = deviceInfo.macAddress
                DBUtils.updateLight(dbSwitch)
            }
        }

        if (this.mode == MODE_OTA) {
            val light = getLightByMeshAddress(deviceInfo.meshAddress)
            if (light != null && light.meshAddr == dbSwitch!!.meshAddr && compareVersion(light.version, mFileVersion) == 1) {
                log("onLeScan" + "connectDevice1")
                connectDevice(deviceInfo.macAddress)
            }
        } else if (this.mode == MODE_IDLE) {
            if (dbSwitch!!.meshAddr == deviceInfo.meshAddress) {
                log("onLeScan" + "connectDevice2")
                stopScanTimer()
                if (!connectStart) {
                    LeBluetooth.getInstance().stopScan()
                    connectDevice(deviceInfo.macAddress)
                    connectRetryCount = 1
                }
                connectStart = true
            }
        }
    }


    private fun showUpdateFailView() {
        runOnUiThread {
            text_info.visibility = View.VISIBLE
            text_info.setText(R.string.update_fail)
            select.isEnabled = true
            mode = MODE_IDLE
            stopScanTimer()
            LeBluetooth.getInstance().stopScan()
            stopConnectTimer()
            if (OTA_IS_HAVEN_START) {
                btn_start_update.visibility = View.GONE
                btn_start_update.isClickable = false
                TelinkLightApplication.getApp().removeEventListener(this)
            } else {
                btn_start_update.text = getString(R.string.re_upgrade)
                btn_start_update.visibility = View.VISIBLE
                btn_start_update.isClickable = true
            }
        }
    }

    fun onScanTimeout() {
        stopScanTimer()
        LeBluetooth.getInstance().stopScan()
        this.mode = MODE_COMPLETE
        val mesh = TelinkLightApplication.getApp().mesh
        mesh.otaDevice = null
        mesh.saveOrUpdate(this)
        log("Finish: Success Count : $successCount")
        showUpdateFailView()
    }

    private fun onNotificationEvent(event: NotificationEvent) {
        // 解析版本信息
        val data = event.args.params
        if (data[0] == NotificationEvent.DATA_GET_VERSION) {
            val version = Strings.bytesToString(Arrays.copyOfRange(data, 1, 5))

            val meshAddress = event.args.src
            //            meshAddress = src & 0xFF;
            if (meshAddress == dbSwitch!!.meshAddr && !versionDevices.contains(meshAddress)) {
                versionDevices.add(meshAddress)
            }

            TelinkLog.w(" src:$meshAddress get version success: $version")
            log("getVersion:" + Integer.toHexString(meshAddress) + "  version:" + version)
            for (light in onlineLights!!) {
                if (light.meshAddr == meshAddress) {
                    //                        log("version: " + version + " -- light version:" + light.version + " --mode: " + this.mode);
                    if (this.mode == MODE_COMPLETE) {
                        if (version != light.version) {
                            successCount++
                        }
                    }
                    light.version = version
                }
            }
        } else if (data[0] == NotificationEvent.DATA_GET_OTA_STATE) {
            delayHandler.removeCallbacks(deviceOtaStateTimeoutTask)
            val otaState = data[1].toInt()
            log("OTA State response--$otaState")

            if (mSendataDisposal != null) {
                mSendataDisposal!!.dispose()
            }
            mSendataDisposal = Observable.timer(TIME_OUT_SENDDATA, TimeUnit.SECONDS).subscribe { aLong ->
                if (progress_view.progress <= 0) {
                    showUpdateFailView()
                }
            }
            if (otaState == NotificationEvent.OTA_STATE_IDLE.toInt()) {
                if (this.mode == MODE_OTA) {
                    startOTA()
                }
            } else {
                log("OTA State response: Busy!!! Stopped!--$otaState")
                doFinish()
            }
        }

    }

    private fun startConnectTimer() {
        if (mConnectDisposal != null) {
            mConnectDisposal!!.dispose()
        }
        mConnectDisposal = Observable.timer(TIME_OUT_CONNECT.toLong(), TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { aLong ->
                    if (TelinkLightApplication.getApp().connectDevice == null) {
                        connectDevice(dbSwitch!!.macAddr)
                    } else {
                        login()
                    }
                }
    }

    private fun stopConnectTimer() {
        if (mConnectDisposal != null) {
            mConnectDisposal!!.dispose()
        }
    }

    private fun onDeviceEvent(event: DeviceEvent) {
        val status = event.args.status
        when (status) {
            LightAdapter.STATUS_LOGOUT -> {
                TelinkLog.i("OTAUpdate#STATUS_LOGOUT")
                log("logout +connectRetryCount=$connectRetryCount")

                if (connectRetryCount > 0) {
                    if (connectRetryCount >= 3) {
                        showUpdateFailView()
                        stopConnectTimer()
                    } else {
                        startConnectTimer()
                    }
                }
                connectRetryCount++
            }

            LightAdapter.STATUS_LOGIN -> {
                TelinkLog.i("OTAUpdate#STATUS_LOGIN")
                log("login success")
                LeBluetooth.getInstance().stopScan()
                stopConnectTimer()
                connectRetryCount = 0
                if (this.mode == MODE_COMPLETE) return
                TelinkLightService.Instance().enableNotification()
                startOTA()
            }

            LightAdapter.STATUS_CONNECTED -> {
                log("connected")
                if (this.mode != MODE_COMPLETE)
                    login()
            }

            LightAdapter.STATUS_OTA_PROGRESS -> {
                val deviceInfo = event.args as OtaDeviceInfo
                msgHandler.obtainMessage(MSG_OTA_PROGRESS, deviceInfo.progress).sendToTarget()
            }

            LightAdapter.STATUS_OTA_COMPLETED -> {
                log("OTA complete")
                msgHandler.obtainMessage(MSG_OTA_PROGRESS, 100).sendToTarget()
                val deviceInfo_1 = event.args
                for (light in onlineLights!!) {
                    if (light.meshAddr == deviceInfo_1.meshAddress) {
                        light.version = mFileVersion
                    }
                }

                successCount++
                if (onlineLights!!.size <= successCount) {
                    updateSuccess()
                }
            }

            LightAdapter.STATUS_OTA_FAILURE -> {
                log("OTA fail")
                showUpdateFailView()
                progress_view.progress = 0
                if (this.mode == MODE_COMPLETE) {
                    log("OTA FAIL COMPLETE")
                    return
                }
            }
        }//                startScan();
    }

    private fun sendStartMeshOTACommand() {
        val account = user!!.controlMeshName
        val pwd = NetworkFactory.md5(NetworkFactory.md5(account) + account)

        mesh!!.otaDevice = OtaDevice()
        val curDevice = TelinkLightApplication.getApp().connectDevice
        mesh.otaDevice!!.mac = curDevice.macAddress
        mesh.otaDevice!!.meshName = account
        mesh.otaDevice!!.meshPwd = pwd
        mesh.saveOrUpdate(this)

        visibleHandler.obtainMessage(View.VISIBLE, meshOtaProgress).sendToTarget()
        //        meshOtaProgress.setVisibility(View.VISIBLE);
        val opcode = 0xC6.toByte()
        val address = 0x0000
        val params = byteArrayOf(0xFF.toByte(), 0xFF.toByte())
        TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                params)
        log("SendCommand 0xC6 startMeshOTA")
    }

    // stop
    private fun sendStopMeshOTACommand() {
        val opcode = 0xC6.toByte()
        val address = 0xFFFF
        val params = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                params)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //第一个是传递标记监听 第二个是回传标记监听
        if (requestCode == REQUEST_CODE_CHOOSE_FILE && resultCode == Activity.RESULT_OK) {
            val b = data!!.extras //data为B中回传的Intent
            mPath = b!!.getString("path")//str即为回传的值
            tvFile.text = mPath
            parseFile()
            Log.e("zcl", "返回数据是:$requestCode---$resultCode---mPath-$mPath")
        }
    }

    companion object {
        private val MODE_IDLE = 1
        private val MODE_OTA = 2
        private val MODE_MESH_OTA = 4
        private val MODE_CONTINUE_MESH_OTA = 8
        private val MODE_COMPLETE = 16

        val INTENT_KEY_CONTINUE_MESH_OTA = "com.telink.bluetooth.light.INTENT_KEY_CONTINUE_MESH_OTA"
        // 有进度状态上报 时跳转进入的
        val CONTINUE_BY_REPORT = 0x21

        // 继续之前的OTA操作，连接指定设备
        val CONTINUE_BY_PREVIOUS = 0x22
        private val REQUEST_CODE_CHOOSE_FILE = 11

        private val MSG_OTA_PROGRESS = 11
        private val MSG_MESH_OTA_PROGRESS = 12
        private val MSG_LOG = 13
        private val TIME_OUT_SCAN = 20
        private val TIME_OUT_CONNECT = 15
    }
}
