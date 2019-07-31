package com.dadoutek.uled.light

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.le.ScanFilter
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.util.DiffUtil
import android.support.v7.widget.*
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.connector.ScanningConnectorActivity
import com.dadoutek.uled.curtain.CurtainScanningNewActivity
import com.dadoutek.uled.group.BatchGroupActivity
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.rgb.RGBSettingActivity
import com.dadoutek.uled.rgb.RgbBatchGroupActivity
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.*
import com.telink.bluetooth.light.*
import com.telink.bluetooth.light.DeviceInfo
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.Strings
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_device_detail.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.indefiniteSnackbar
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by hejiajun on 2018/4/24.
 */

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1

class DeviceDetailAct : TelinkBaseActivity(), EventListener<String>, View.OnClickListener {

    private var type: Int? = null
    private lateinit var lightsData: MutableList<DbLight>

    private var inflater: LayoutInflater? = null

    private var adaper: DeviceDetailListAdapter? = null

    private val SCENE_MAX_COUNT = 16
    private var currentLight: DbLight? = null

    private var positionCurrent: Int = 0

//    private lateinit var lightList: MutableList<DbLight>

    private var canBeRefresh = true

//    private lateinit var group: DbGroup

    private val REQ_LIGHT_SETTING: Int = 0x01

    private var acitivityIsAlive = true

    private var mTelinkLightService: TelinkLightService? = null

    private var retryConnectCount = 0

    private var bestRSSIDevice: DeviceInfo? = null

    private val connectFailedDeviceMacList: MutableList<String> = mutableListOf()

    private var mConnectDisposal: Disposable? = null
    private var mScanDisposal: Disposable? = null
    private var mScanTimeoutDisposal: Disposable? = null
    private var mCheckRssiDisposal: Disposable? = null
    private var mNotFoundSnackBar: Snackbar? = null
    private var mApplication: TelinkLightApplication? = null

    private var install_device: TextView? = null
    private var create_group: TextView? = null
    private var create_scene: TextView? = null

    private var isRgbClick = false

    private var installId = 0

    private lateinit var stepOneText: TextView
    private lateinit var stepTwoText: TextView
    private lateinit var stepThreeText: TextView
    private lateinit var switchStepOne: TextView
    private lateinit var switchStepTwo: TextView
    private lateinit var swicthStepThree: TextView

    private var connectMeshAddress: Int = 0

    private var lastOffset: Int = 0//距离

    private var lastPosition: Int = 0//第几个item

    private var sharedPreferences: SharedPreferences? = null

    override fun onPostResume() {
        super.onPostResume()
        addListeners()
    }


    private fun addListeners() {
        addScanListeners()
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        this.mApplication?.addEventListener(NotificationEvent.ONLINE_STATUS, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    override fun performed(event: Event<String>) {
        when (event.type) {
            LeScanEvent.LE_SCAN -> onLeScan(event as LeScanEvent)
//            LeScanEvent.LE_SCAN_TIMEOUT -> onLeScanTimeout()
//            LeScanEvent.LE_SCAN_COMPLETED -> onLeScanTimeout()
//            NotificationEvent.ONLINE_STATUS -> this.onOnlineStatusNotify(event as NotificationEvent)
            NotificationEvent.GET_ALARM -> {
            }
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
//            MeshEvent.OFFLINE -> this.onMeshOffline(event as MeshEvent)
            ServiceEvent.SERVICE_CONNECTED -> this.onServiceConnected(event as ServiceEvent)
            ServiceEvent.SERVICE_DISCONNECTED -> this.onServiceDisconnected(event as ServiceEvent)
//            NotificationEvent.GET_DEVICE_STATE -> onNotificationEvent(event as NotificationEvent)
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
            }
        }
    }

    private fun onServiceConnected(event: ServiceEvent) {
//        LogUtils.d("onServiceConnected")
    }

    private fun onServiceDisconnected(event: ServiceEvent) {
        LogUtils.d("onServiceDisconnected")
        TelinkLightApplication.getInstance().startLightService(TelinkLightService::class.java)
    }

    private fun isSwitch(uuid: Int): Boolean {
        return when (uuid) {
            DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SENSOR, DeviceType.NIGHT_LIGHT -> {
                LogUtils.d("This is switch")
                true
            }
            else -> {
                false
            }
        }
    }

    private fun onErrorReport(info: ErrorReportInfo) {
//        LogUtils.d("onErrorReport current device mac = ${bestRSSIDevice?.macAddress}")
        if (bestRSSIDevice != null) {
//            connectFailedDeviceMacList.add(bestRSSIDevice!!.macAddress)
        }
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        LogUtils.d("蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        LogUtils.d("无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        LogUtils.d("未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        LogUtils.d("未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        LogUtils.d("未建立物理连接")
                    }
                }
                retryConnect()

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        LogUtils.d("value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        LogUtils.d("read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        LogUtils.d("write login data 没有收到response")
                    }
                }
                retryConnect()

            }
        }
    }

    /**
     * 处理扫描事件
     *
     * @param event
     */
    @Synchronized
    private fun onLeScan(event: LeScanEvent) {
        val mesh = this.mApplication?.mesh
        val meshAddress = mesh?.generateMeshAddr()
        val deviceInfo: DeviceInfo = event.args

        Thread {
            val dbLight = DBUtils.getLightByMeshAddr(deviceInfo.meshAddress)
            if (dbLight != null && dbLight.macAddr == "0") {
                dbLight.macAddr = deviceInfo.macAddress
                DBUtils.updateLight(dbLight)
            }
        }.start()

        if (!isSwitch(deviceInfo.productUUID) && !connectFailedDeviceMacList.contains(deviceInfo.macAddress)) {
//            connect(deviceInfo.macAddress)
            if (bestRSSIDevice != null) {
                //扫到的灯的信号更好并且没有连接失败过就把要连接的灯替换为当前扫到的这个。
                if (deviceInfo.rssi > bestRSSIDevice?.rssi ?: 0) {
                    LogUtils.d("changeToScene to device with better RSSI  new meshAddr = ${deviceInfo.meshAddress} rssi = ${deviceInfo.rssi}")
                    bestRSSIDevice = deviceInfo
                }
            } else {
                LogUtils.d("RSSI  meshAddr = ${deviceInfo.meshAddress} rssi = ${deviceInfo.rssi}")
                bestRSSIDevice = deviceInfo
            }

        }

    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {

                TelinkLightService.Instance().enableNotification()
                TelinkLightService.Instance().updateNotification()
                GlobalScope.launch(Dispatchers.Main) {
                    stopConnectTimer()
                    if (progressBar?.visibility != View.GONE)
                        progressBar?.visibility = View.GONE
                    delay(300)
                }

                val connectDevice = this.mApplication?.connectDevice
                if (connectDevice != null) {
                    this.connectMeshAddress = connectDevice.meshAddress
                }
//                scanPb.visibility = View.GONE
//                adapter?.notifyDataSetChanged()
                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, true)
            }
            LightAdapter.STATUS_LOGOUT -> {
                retryConnect()
            }
            LightAdapter.STATUS_CONNECTING -> {
                Log.d("connectting", "444")
//                scanPb.visibility = View.VISIBLE
            }
            LightAdapter.STATUS_CONNECTED -> {
                if (!TelinkLightService.Instance().isLogin)
                    login()
                hideLoadingDialog()
            }
            LightAdapter.STATUS_ERROR_N -> onNError(event)
        }
    }

    private fun stopConnectTimer() {
        mConnectDisposal?.dispose()
    }

    private fun onNError(event: DeviceEvent) {
//        ToastUtils.showLong(getString(R.string.connect_fail))
        SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

        TelinkLightService.Instance().idleMode(true)
        TelinkLog.d("DeviceScanningActivity#onNError")

        val builder = AlertDialog.Builder(this)
        builder.setMessage("当前环境:Android7.0!连接重试:" + " 3次失败!")
        builder.setNegativeButton("confirm") { dialog, _ -> dialog.dismiss() }
        builder.setCancelable(false)
        builder.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_detail)
        type = this.intent.getIntExtra(Constant.DEVICE_TYPE, 0)
        inflater = this.layoutInflater
        this.mApplication = this.application as TelinkLightApplication

        lightsData = mutableListOf()
    }

    override fun onResume() {
        super.onResume()
        type = this.intent.getIntExtra(Constant.DEVICE_TYPE, 0)
        inflater = this.layoutInflater
        initDate()
        initView()
        initToolbar()
        scrollToPosition()
    }


    private fun initToolbar() {
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initView() {
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        this.mApplication?.addEventListener(NotificationEvent.ONLINE_STATUS, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
        val layoutmanager = LinearLayoutManager(this)
        recycleView!!.layoutManager = GridLayoutManager(this, 3)
//        val decoration = DividerItemDecoration(this!!, DividerItemDecoration .VERTICAL)
//        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this!!, R.color
//                .divider)))
//        recyclerView!!.addItemDecoration(decoration)
        //添加Item变化动画
        recycleView!!.itemAnimator = DefaultItemAnimator()


        recycleView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {

                super.onScrollStateChanged(recyclerView, newState)

                if (recyclerView.layoutManager != null) {

                    getPositionAndOffset()

                }

            }

        })

        adaper = DeviceDetailListAdapter(R.layout.device_detail_adapter, lightsData)
        adaper!!.onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
            currentLight = lightsData?.get(position)
            positionCurrent = position

            val opcode = Opcode.LIGHT_ON_OFF

            when (view.id) {
                R.id.img_light -> {
                    canBeRefresh = true
                    //todo 添加未连接提示
                    mTelinkLightService = TelinkLightService.Instance()
                    if (mTelinkLightService?.adapter?.mLightCtrl?.currentLight?.isConnected != true) {
                        while (TelinkApplication.getInstance()?.serviceStarted == true) {
                            GlobalScope.launch(Dispatchers.Main) {
                                retryConnectCount = 0
                                connectFailedDeviceMacList.clear()
                                if (TelinkApplication.getInstance()?.connectDevice == null)
                                    startScan(false)
                            }
                            break
                        }
                    } else {
                        if (currentLight!!.connectionStatus == ConnectionStatus.OFF.value) {
                            if (currentLight!!.productUUID == DeviceType.SMART_CURTAIN) {//开窗
                                Commander.openOrCloseCurtain(currentLight!!.meshAddr, true, false)
                            } else {
                                Commander.openOrCloseLights(currentLight!!.meshAddr, true)//开灯
                            }
                            currentLight!!.connectionStatus = ConnectionStatus.ON.value
                        } else {
                            if (currentLight!!.productUUID == DeviceType.SMART_CURTAIN) {
                                Commander.openOrCloseCurtain(currentLight!!.meshAddr, false, false)//关窗
                            } else {
                                Commander.openOrCloseLights(currentLight!!.meshAddr, false)//关灯
                            }
                            currentLight!!.connectionStatus = ConnectionStatus.OFF.value
                        }

                        when (type) {
                            Constant.INSTALL_NORMAL_LIGHT -> {
                                currentLight!!.updateIcon()
                            }

                            Constant.INSTALL_RGB_LIGHT -> {
                                currentLight!!.updateRgbIcon()
                            }
                        }
                        DBUtils.updateLight(currentLight!!)
                        runOnUiThread {
                            adapter?.notifyDataSetChanged()
                        }
                    }
                }

                R.id.tv_setting -> {
                    var intent = Intent(this@DeviceDetailAct, NormalSettingActivity::class.java)
                    if (currentLight?.productUUID == DeviceType.LIGHT_RGB) {
                        intent = Intent(this@DeviceDetailAct, RGBSettingActivity::class.java)
                        intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_LIGHT)
                    }
                    intent.putExtra(Constant.LIGHT_ARESS_KEY, currentLight)
                    intent.putExtra(Constant.GROUP_ARESS_KEY, currentLight!!.meshAddr)
                    intent.putExtra(Constant.LIGHT_REFRESH_KEY, Constant.LIGHT_REFRESH_KEY_OK)
                    startActivityForResult(intent, REQ_LIGHT_SETTING)
                }
            }
        }
        adaper!!.bindToRecyclerView(recycleView)

        when (type) {
            Constant.INSTALL_NORMAL_LIGHT -> {
                toolbar.title = getString(R.string.normal_light_title) + " (" + lightsData.size + ")"
                for (i in lightsData?.indices!!) {
                    lightsData!![i].updateIcon()
                }
            }

            Constant.INSTALL_RGB_LIGHT -> {
                toolbar.title = getString(R.string.rgb_light) + " (" + lightsData.size + ")"
                for (i in lightsData?.indices!!) {
                    lightsData!![i].updateRgbIcon()
                }
            }

            Constant.INSTALL_LIGHT_OF_CW -> {
                for (i in lightsData?.indices!!) {
                    lightsData!![i].updateIcon()
                }
            }

            Constant.INSTALL_LIGHT_OF_RGB -> {
                for (i in lightsData?.indices!!) {
                    lightsData!![i].updateRgbIcon()
                }
            }
        }

        install_device = findViewById(R.id.install_device)
        create_group = findViewById(R.id.create_group)
        create_scene = findViewById(R.id.create_scene)
        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)
        add_device_btn.setOnClickListener(this)

        /*     when (type) {
                 Constant.INSTALL_NORMAL_LIGHT -> {
                     toolbar.title = getString(R.string.normal_light_title) + " (" + lightsData.size + ")"
                 }
                 Constant.INSTALL_RGB_LIGHT -> {
                     toolbar.title = getString(R.string.rgb_light) + " (" + lightsData.size + ")"
                 }
             }*/
    }

    private val onClick = View.OnClickListener {
        var intent: Intent? = null
        //点击任何一个选项跳转页面都隐藏引导
//        val controller=guide2()
//            controller?.remove()
//        hidePopupMenu()
        when (it.id) {
            R.id.install_device -> {
                showInstallDeviceList()
            }
            R.id.create_group -> {
                dialog_device?.visibility = View.GONE
                if (TelinkLightApplication.getInstance().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    addNewGroup()
                }
            }
            R.id.create_scene -> {
                dialog_device?.visibility = View.GONE
                val nowSize = DBUtils.sceneList.size
                if (TelinkLightApplication.getInstance().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    if (nowSize >= SCENE_MAX_COUNT) {
                        ToastUtils.showLong(R.string.scene_16_tip)
                    } else {
                        val intent = Intent(this, NewSceneSetAct::class.java)
                        intent.putExtra(Constant.IS_CHANGE_SCENE, false)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun showInstallDeviceList() {
        dialog_device.visibility = View.GONE
//        callbackLinkMainActAndFragment?.showDeviceListDialog(isGuide,isRgbClick)
        showInstallDeviceList(isGuide, isRgbClick)
    }

    var installDialog: android.app.AlertDialog? = null
    var isGuide: Boolean = false
    var clickRgb: Boolean = false
    private fun showInstallDeviceList(isGuide: Boolean, clickRgb: Boolean) {
        this.clickRgb = clickRgb
        val view = View.inflate(this, R.layout.dialog_install_list, null)
        val close_install_list = view.findViewById<ImageView>(R.id.close_install_list)
        val install_device_recyclerView = view.findViewById<RecyclerView>(R.id.install_device_recyclerView)
        close_install_list.setOnClickListener { v -> installDialog?.dismiss() }

        val installList: ArrayList<InstallDeviceModel> = OtherUtils.getInstallDeviceList(this)

        val installDeviceListAdapter = InstallDeviceListAdapter(R.layout.item_install_device, installList)
        val layoutManager = LinearLayoutManager(this)
        install_device_recyclerView?.layoutManager = layoutManager
        install_device_recyclerView?.adapter = installDeviceListAdapter
        installDeviceListAdapter.bindToRecyclerView(install_device_recyclerView)
        val decoration = DividerItemDecoration(this,
                DividerItemDecoration
                        .VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color
                .divider)))
        //添加分割线
        install_device_recyclerView?.addItemDecoration(decoration)

        installDeviceListAdapter.onItemClickListener = onItemClickListenerInstallList

        installDialog = android.app.AlertDialog.Builder(this)
                .setView(view)
                .create()

        installDialog?.setOnShowListener {

        }

        if (isGuide) {
            installDialog?.setCancelable(false)
        }

        installDialog?.show()

        Thread {
            Thread.sleep(100)
            GlobalScope.launch(Dispatchers.Main) {
                //                guide3(install_device_recyclerView)
            }
        }.start()
    }

    val INSTALL_NORMAL_LIGHT = 0
    val INSTALL_RGB_LIGHT = 1
    val INSTALL_SWITCH = 2
    val INSTALL_SENSOR = 3
    val INSTALL_CURTAIN = 4
    val INSTALL_CONNECTOR = 5
    val onItemClickListenerInstallList = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
        var intent: Intent? = null
        //点击任何一个选项跳转页面都隐藏引导
//        val controller=guide2()
//            controller?.remove()
        isGuide = false
        installDialog?.dismiss()
//        hidePopupMenu()
        when (position) {
            INSTALL_NORMAL_LIGHT -> {
                installId = INSTALL_NORMAL_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this))
            }
            INSTALL_RGB_LIGHT -> {
                installId = INSTALL_RGB_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this))
            }
            INSTALL_CURTAIN -> {
                installId = INSTALL_CURTAIN
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this))
            }
            INSTALL_SWITCH -> {
                installId = INSTALL_SWITCH
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this))
                stepOneText.visibility = View.GONE
                stepTwoText.visibility = View.GONE
                stepThreeText.visibility = View.GONE
                switchStepOne.visibility = View.VISIBLE
                switchStepTwo.visibility = View.VISIBLE
                swicthStepThree.visibility = View.VISIBLE
            }
            INSTALL_SENSOR -> {
                installId = INSTALL_SENSOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this))
            }
            INSTALL_CONNECTOR -> {
                installId = INSTALL_CONNECTOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this))
            }
        }
    }

    private fun showInstallDeviceDetail(describe: String) {
        val view = View.inflate(this, R.layout.dialog_install_detail, null)
        val close_install_list = view.findViewById<ImageView>(R.id.close_install_list)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        stepOneText = view.findViewById<TextView>(R.id.step_one)
        stepTwoText = view.findViewById<TextView>(R.id.step_two)
        stepThreeText = view.findViewById<TextView>(R.id.step_three)
        switchStepOne = view.findViewById<TextView>(R.id.switch_step_one)
        switchStepTwo = view.findViewById<TextView>(R.id.switch_step_two)
        swicthStepThree = view.findViewById<TextView>(R.id.switch_step_three)
        val install_tip_question = view.findViewById<TextView>(R.id.install_tip_question)
        val search_bar = view.findViewById<Button>(R.id.search_bar)
        close_install_list.setOnClickListener(dialogOnclick)
        btnBack.setOnClickListener(dialogOnclick)
        search_bar.setOnClickListener(dialogOnclick)
        install_tip_question.text = describe
        install_tip_question.movementMethod = ScrollingMovementMethod.getInstance()
        installDialog = android.app.AlertDialog.Builder(this)
                .setView(view)
                .create()

        installDialog?.setOnShowListener {

        }
        installDialog?.show()
    }

    private val dialogOnclick = View.OnClickListener {
        var medressData = 0
        var allData = DBUtils.allLight
        var sizeData = DBUtils.allLight.size
        if (sizeData != 0) {
            var lightData = allData[sizeData - 1]
            medressData = lightData.meshAddr
        }

        when (it.id) {
            R.id.close_install_list -> {
                installDialog?.dismiss()
            }
            R.id.search_bar -> {
                when (installId) {
                    INSTALL_NORMAL_LIGHT -> {
                        if (medressData < 254) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, false)
                            intent.putExtra(Constant.TYPE_VIEW, Constant.LIGHT_KEY)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_RGB_LIGHT -> {
                        if (medressData < 254) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                            intent.putExtra(Constant.TYPE_VIEW, Constant.RGB_LIGHT_KEY)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_CURTAIN -> {
                        if (medressData < 254) {
                            intent = Intent(this, CurtainScanningNewActivity::class.java)
                            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                            intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_SWITCH -> startActivity(Intent(this, ScanningSwitchActivity::class.java))
                    INSTALL_SENSOR -> startActivity(Intent(this, ScanningSensorActivity::class.java))
                    INSTALL_CONNECTOR -> {
                        if (medressData < 254) {
                            intent = Intent(this, ScanningConnectorActivity::class.java)
                            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                            intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                }
            }
            R.id.btnBack -> {
                installDialog?.dismiss()
                showInstallDeviceList(isGuide, clickRgb)
            }
        }
    }

    private fun addNewGroup() {
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(DBUtils.getDefaultNewGroupName())
        //设置光标默认在最后
        textGp.setSelection(textGp.getText().toString().length)
        android.app.AlertDialog.Builder(this)
                .setTitle(R.string.create_new_group)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        //往DB里添加组数据
                        DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, DBUtils.groupList, Constant.DEVICE_TYPE_DEFAULT_ALL, this)
//                        callbackLinkMainActAndFragment?.changeToGroup()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.add_device_btn -> addDeviceLight()
        }
    }

    private fun addDeviceLight() {
        when (type) {
            Constant.INSTALL_NORMAL_LIGHT -> {
                intent = Intent(this, DeviceScanningNewActivity::class.java)
                intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, false)
                intent.putExtra(Constant.TYPE_VIEW, Constant.LIGHT_KEY)
                startActivityForResult(intent, 0)
            }
            Constant.INSTALL_RGB_LIGHT -> {
                intent = Intent(this, DeviceScanningNewActivity::class.java)
                intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                intent.putExtra(Constant.TYPE_VIEW, Constant.RGB_LIGHT_KEY)
                startActivityForResult(intent, 0)
            }
            Constant.INSTALL_LIGHT_OF_CW -> {
                intent = Intent(this, DeviceScanningNewActivity::class.java)
                intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, false)
                intent.putExtra(Constant.TYPE_VIEW, Constant.LIGHT_KEY)
                startActivityForResult(intent, 0)
            }
            Constant.INSTALL_LIGHT_OF_RGB -> {
                intent = Intent(this, DeviceScanningNewActivity::class.java)
                intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                intent.putExtra(Constant.TYPE_VIEW, Constant.RGB_LIGHT_KEY)
                startActivityForResult(intent, 0)
            }
        }
    }

    private fun initDate() {
        lightsData = ArrayList()
        when (type) {
            Constant.INSTALL_NORMAL_LIGHT -> {
                var all_light_data = DBUtils.getAllNormalLight()
                if (all_light_data.size > 0) {
                    var list_group: ArrayList<DbLight> = ArrayList()
                    var no_group: ArrayList<DbLight> = ArrayList()
                    for (i in all_light_data.indices) {
                        if (StringUtils.getLightName(all_light_data[i]) == TelinkLightApplication.getInstance().getString(R.string.not_grouped)) {
                            no_group.add(all_light_data[i])
                        } else {
                            list_group.add(all_light_data[i])
                        }
                    }
                    if (no_group.size > 0) {
                        for (i in no_group.indices) {
                            lightsData.add(no_group[i])
                        }
                    }

                    if (list_group.size > 0) {
                        for (i in list_group.indices) {
                            lightsData.add(list_group[i])
                        }
                    }
                    toolbar!!.tv_function1.visibility = View.VISIBLE
                    recycleView.visibility = View.VISIBLE
                    no_device_relativeLayout.visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
                    var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
                    batchGroup.setText(R.string.batch_group)
                    batchGroup.setOnClickListener(View.OnClickListener {
                        val intent = Intent(this,
                                BatchGroupActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                        intent.putExtra("lightType", "all_light")
                        startActivity(intent)
                    })
                } else {
                    recycleView.visibility = View.GONE
                    no_device_relativeLayout.visibility = View.VISIBLE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                        if (dialog_device?.visibility == View.GONE) {
                            showPopupMenu()
                        } else {
//                            hidePopupMenu()
                        }
                    }
                }
            }
            Constant.INSTALL_RGB_LIGHT -> {
                var all_light_data = DBUtils.getAllRGBLight()
                if (all_light_data.size > 0) {
                    var list_group: ArrayList<DbLight> = ArrayList()
                    var no_group: ArrayList<DbLight> = ArrayList()
                    for (i in all_light_data.indices) {
                        if (StringUtils.getLightName(all_light_data[i]) == TelinkLightApplication.getInstance().getString(R.string.not_grouped)) {
                            no_group.add(all_light_data[i])
                        } else {
                            list_group.add(all_light_data[i])
                        }
                    }


                    if (no_group.size > 0) {
                        for (i in no_group.indices) {
                            lightsData.add(no_group[i])
                        }
                    }

                    if (list_group.size > 0) {
                        for (i in list_group.indices) {
                            lightsData.add(list_group[i])
                        }
                    }
                    toolbar!!.tv_function1.visibility = View.VISIBLE
                    recycleView.visibility = View.VISIBLE
                    no_device_relativeLayout.visibility = View.GONE
                    var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
                    batchGroup.setText(R.string.batch_group)
                    batchGroup.setOnClickListener(View.OnClickListener {
                        val intent = Intent(this,
                                RgbBatchGroupActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                        intent.putExtra("lightType", "all_light")
                        startActivity(intent)
                    })
                } else {
                    recycleView.visibility = View.GONE
                    no_device_relativeLayout.visibility = View.VISIBLE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                        if (dialog_device?.visibility == View.GONE) {
                            showPopupMenu()
                        } else {
//                            hidePopupMenu()
                        }
                    }
                }
            }
            Constant.INSTALL_LIGHT_OF_CW -> {
                var all_light_data = DBUtils.getAllNormalLight()
                if (all_light_data.size > 0) {
                    var list_group: ArrayList<DbLight> = ArrayList()
                    var no_group: ArrayList<DbLight> = ArrayList()
                    for (i in all_light_data.indices) {
                        if (StringUtils.getLightName(all_light_data[i]) == TelinkLightApplication.getInstance().getString(R.string.not_grouped)) {
                            no_group.add(all_light_data[i])
                        } else {
                            list_group.add(all_light_data[i])
                        }
                    }


                    if (no_group.size > 0) {
                        for (i in no_group.indices) {
                            lightsData.add(no_group[i])
                        }
                    }

                    if (list_group.size > 0) {
                        for (i in list_group.indices) {
                            lightsData.add(list_group[i])
                        }
                    }
                    var cwLightGroup = this.intent.getStringExtra("cw_light_name")
                    toolbar!!.tv_function1.visibility = View.VISIBLE
                    recycleView.visibility = View.VISIBLE
                    no_device_relativeLayout.visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
                    var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
                    batchGroup.setText(R.string.batch_group)
                    batchGroup.setOnClickListener(View.OnClickListener {
                        val intent = Intent(this,
                                BatchGroupActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                        intent.putExtra("lightType", "cw_light")
                        intent.putExtra("cw_light_group_name", cwLightGroup)
                        startActivity(intent)
                    })
                } else {
                    recycleView.visibility = View.GONE
                    no_device_relativeLayout.visibility = View.VISIBLE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                        if (dialog_device?.visibility == View.GONE) {
                            showPopupMenu()
                        } else {
//                            hidePopupMenu()
                        }
                    }
                }
            }

            Constant.INSTALL_LIGHT_OF_RGB -> {
                var all_light_data = DBUtils.getAllRGBLight()
                if (all_light_data.size > 0) {
                    var list_group: ArrayList<DbLight> = ArrayList()
                    var no_group: ArrayList<DbLight> = ArrayList()
                    for (i in all_light_data.indices) {
                        if (StringUtils.getLightName(all_light_data[i]) == TelinkLightApplication.getInstance().getString(R.string.not_grouped)) {
                            no_group.add(all_light_data[i])
                        } else {
                            list_group.add(all_light_data[i])
                        }
                    }


                    if (no_group.size > 0) {
                        for (i in no_group.indices) {
                            lightsData.add(no_group[i])
                        }
                    }

                    if (list_group.size > 0) {
                        for (i in list_group.indices) {
                            lightsData.add(list_group[i])
                        }
                    }
                    var rgbLightGroup = this.intent.getStringExtra("rgb_light_name")
                    toolbar!!.tv_function1.visibility = View.VISIBLE
                    recycleView.visibility = View.VISIBLE
                    no_device_relativeLayout.visibility = View.GONE
                    var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
                    batchGroup.setText(R.string.batch_group)
                    batchGroup.setOnClickListener(View.OnClickListener {
                        val intent = Intent(this,
                                RgbBatchGroupActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                        intent.putExtra("lightType", "rgb_light")
                        intent.putExtra("rgb_light_group_name", rgbLightGroup)
                        startActivity(intent)
                    })
                } else {
                    recycleView.visibility = View.GONE
                    no_device_relativeLayout.visibility = View.VISIBLE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                        if (dialog_device?.visibility == View.GONE) {
                            showPopupMenu()
                        } else {
//                            hidePopupMenu()
                        }
                    }
                }
            }
        }
//        lightList = ArrayList()
//        if (group.meshAddr == 0xffff) {
//            //            lightList = DBUtils.getAllLight();
////            lightList=DBUtils.getAllLight()
//            filter("", false)
//        } else {
//            lightsData = DBUtils.getLightByGroupID(group.id)
//        }
    }

    private fun showPopupMenu() {
        dialog_device?.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        canBeRefresh = false
        acitivityIsAlive = false
        mScanDisposal?.dispose()
        if (TelinkLightApplication.getInstance().connectDevice == null) {
            TelinkLightService.Instance().idleMode(true)
            LeBluetooth.getInstance().stopScan()
        }
//        this.mApplication?.removeEventListener(this)
    }

    private fun filter(groupName: String?, isSearch: Boolean) {
        val list = DBUtils.groupList
//        val nameList : ArrayList<String> = ArrayList()
        if (lightsData != null && lightsData!!.size > 0) {
            lightsData!!.clear()
        }

//        for(i in list.indices){
//            nameList.add(list[i].name)
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        notifyData()
        val isConnect = data?.getBooleanExtra("data", false) ?: false
//        if (isConnect) {
//            scanPb.visibility = View.VISIBLE
//        }

        Thread {
            //踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
            Thread.sleep(2500)
            if (this@DeviceDetailAct == null ||
                    this@DeviceDetailAct.isDestroyed ||
                    this@DeviceDetailAct.isFinishing || !acitivityIsAlive) {
            } else {
                autoConnect()
            }
        }.start()
    }

    fun notifyData() {
        if (lightsData.size > 0) {
            val mOldDatas: MutableList<DbLight>? = lightsData
            val mNewDatas: MutableList<DbLight>? = getNewData()
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return mOldDatas?.get(oldItemPosition)?.id?.equals(mNewDatas?.get
                    (newItemPosition)?.id) ?: false
                }

                override fun getOldListSize(): Int {
                    return mOldDatas?.size ?: 0
                }

                override fun getNewListSize(): Int {
                    return mNewDatas?.size ?: 0
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val beanOld = mOldDatas?.get(oldItemPosition)
                    val beanNew = mNewDatas?.get(newItemPosition)
                    return if (!beanOld?.name.equals(beanNew?.name)) {
                        return false//如果有内容不同，就返回false
                    } else true

                }
            }, true)
            adaper?.let { diffResult.dispatchUpdatesTo(it) }
            lightsData = mNewDatas!!
            when (type) {
                Constant.INSTALL_NORMAL_LIGHT -> {
                    toolbar.title = getString(R.string.normal_light_title) + " (" + lightsData.size + ")"
                }
                Constant.INSTALL_RGB_LIGHT -> {
                    toolbar.title = getString(R.string.rgb_light) + " (" + lightsData.size + ")"
                }
            }
            adaper!!.setNewData(lightsData)
        }
    }

    private fun getNewData(): MutableList<DbLight> {
//        if (currentLight!!.meshAddr == 0xffff) {
//            //            lightList = DBUtils.getAllLight();
////            lightList=DBUtils.getAllLight()
//            filter("", false)
//        } else {
        when (type) {
            Constant.INSTALL_NORMAL_LIGHT -> {
                lightsData = DBUtils.getAllNormalLight()
            }
            Constant.INSTALL_RGB_LIGHT -> {
                lightsData = DBUtils.getAllRGBLight()
            }
        }
//    }

//        if (currentLight!!.meshAddr == 0xffff) {
//            toolbar.title = getString(R.string.allLight) + " (" + lightsData.size + ")"
//        } else {
//            toolbar.title = (currentLight!!.name ?: "")
//        }
        return lightsData
    }

    fun autoConnect() {
        //检查是否支持蓝牙设备
        if (!LeBluetooth.getInstance().isSupport(applicationContext)) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show()
            ActivityUtils.finishAllActivities()
        } else {  //如果蓝牙没开，则弹窗提示用户打开蓝牙
            if (!LeBluetooth.getInstance().isEnabled) {
                GlobalScope.launch(Dispatchers.Main) {
                    var root = this@DeviceDetailAct.findViewById<RelativeLayout>(R.id.root)
                    root.indefiniteSnackbar(R.string.openBluetooth, android.R.string.ok) {
                        LeBluetooth.getInstance().enable(applicationContext)
                    }
                }
            } else {
                //如果位置服务没打开，则提示用户打开位置服务
                if (!BleUtils.isLocationEnable(this)) {
                    GlobalScope.launch(Dispatchers.Main) {
                        showOpenLocationServiceDialog()
                    }
                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        hideLocationServiceDialog()
                    }
                    mTelinkLightService = TelinkLightService.Instance()
                    if (TelinkLightApplication.getInstance().connectDevice == null) {
                        while (TelinkApplication.getInstance()?.serviceStarted == true) {
                            GlobalScope.launch(Dispatchers.Main) {
                                retryConnectCount = 0
                                connectFailedDeviceMacList.clear()
                                startScan(false)
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

    var locationServiceDialog: AlertDialog? = null
    fun showOpenLocationServiceDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.open_location_service)
        builder.setNegativeButton(getString(android.R.string.ok)) { dialog, which ->
            BleUtils.jumpLocationSetting()
        }
        locationServiceDialog = builder.create()
        locationServiceDialog?.setCancelable(false)
        locationServiceDialog?.show()
    }

    fun hideLocationServiceDialog() {
        locationServiceDialog?.hide()
    }

    @SuppressLint("CheckResult")
    private fun startScan(isRetry: Boolean) {
        //当App在前台时，才进行扫描。
        if (AppUtils.isAppForeground())
            if (acitivityIsAlive || mScanDisposal?.isDisposed != true) {
                LogUtils.d("startScanLight_LightOfGroup")

                // ToastUtils.showLong(getString(R.string.bluetooth_open_connet))
                if (!isRetry)
                    TmtUtils.midToastLong(this, getString(R.string.bluetooth_open_connet))

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

    @SuppressLint("CheckResult")
    private fun
            connect(mac: String) {
        try {
            mCheckRssiDisposal?.dispose()
            mCheckRssiDisposal = RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN)
                    .subscribe {
                        if (it) {
                            //授予了权限
                            if (TelinkLightService.Instance() != null) {
                                progressBar?.visibility = View.VISIBLE
                                TelinkLightService.Instance().connect(mac, CONNECT_TIMEOUT)
                            }
                        } else {
                            //没有授予权限
                            DialogUtils.showNoBlePermissionDialog(this, { connect(mac) }, { finish() })
                        }
                    }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 重试
     * 先扫描到信号比较好的，再连接。
     */
    private fun retryConnect() {
        if (retryConnectCount < MAX_RETRY_CONNECT_TIME) {
            retryConnectCount++
            if (TelinkLightService.Instance().adapter.mLightCtrl.currentLight?.isConnected != true)
                startScan(true)
            else
                login()
        } else {
            TelinkLightService.Instance().idleMode(true)
            retryConnectCount = 0
            connectFailedDeviceMacList.clear()
            startScan(true)
        }
    }

    private fun login() {
        val account = DBUtils.lastUser?.account
        val pwd = NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16)
        TelinkLightService.Instance().login(Strings.stringToBytes(account, 16)
                , Strings.stringToBytes(pwd, 16))
        ToastUtil.showToast(this, getString(R.string.connect_success))
        hideLoadingDialog()
    }


    /**
     * 扫描不到任何设备了
     * （扫描结束）
     */
    private fun onLeScanTimeout() {
        LogUtils.d("onErrorReport: onLeScanTimeout")
        TelinkLightService.Instance().idleMode(true)
        LeBluetooth.getInstance().stopScan()
        startScan(true)
    }

    override fun onStop() {
        super.onStop()
        this.mApplication!!.removeEventListener(this)
        if (TelinkLightService.Instance() != null)
            TelinkLightService.Instance().disableAutoRefreshNotify()
    }

    private fun getPositionAndOffset() {
        val layoutManager = recycleView!!.layoutManager as LinearLayoutManager?
        //获取可视的第一个view
        val topView = layoutManager!!.getChildAt(0)

        if (topView != null) {
            //获取与该view的顶部的偏移量
            lastOffset = topView.top
            //得到该View的数组位置
            lastPosition = layoutManager.getPosition(topView)
            sharedPreferences = getSharedPreferences("key", Activity.MODE_PRIVATE)
            val editor = sharedPreferences!!.edit()
            editor.putInt("lastOffset", lastOffset)
            editor.putInt("lastPosition", lastPosition)
            editor.commit()
        }

    }

    override fun onPause() {
        super.onPause()
        getPositionAndOffset()
        mScanTimeoutDisposal?.dispose()
        mConnectDisposal?.dispose()
        mNotFoundSnackBar?.dismiss()
        mScanTimeoutDisposal?.dispose()
        //移除事件
        this.mApplication?.removeEventListener(this)
        stopConnectTimer()
        mCheckRssiDisposal?.dispose()
    }


    /**
     * 让RecyclerView滚动到指定位置
     */
    private fun scrollToPosition() {
        sharedPreferences = getSharedPreferences("key", Activity.MODE_PRIVATE)
        lastOffset = sharedPreferences!!.getInt("lastOffset", 0)
        lastPosition = sharedPreferences!!.getInt("lastPosition", 0)
        if (recycleView!!.layoutManager != null && lastPosition >= 0) {
            (recycleView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(lastPosition, lastOffset)
        }
    }
}
