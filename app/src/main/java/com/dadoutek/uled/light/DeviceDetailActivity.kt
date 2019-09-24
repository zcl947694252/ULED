package com.dadoutek.uled.light

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.util.DiffUtil
import android.support.v7.widget.*
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.BatchGroupActivity
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.InstallDeviceModel
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.rgb.RGBSettingActivity
import com.dadoutek.uled.rgb.RgbBatchGroupActivity
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.BleUtils
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.*
import com.telink.bluetooth.light.*
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.MeshUtils
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_device_detail.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * 创建者     zcl
 * 创建时间   2019/8/28 17:34
 * 描述	      ${六种 设备列表 从首页设备fragment点击跳入 以及扫描设备按钮}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}$
 */

private const val MAX_RETRY_CONNECT_TIME = 5

class DeviceDetailAct : TelinkBaseActivity(), EventListener<String>, View.OnClickListener {
    private var type: Int? = null
    private lateinit var lightsData: MutableList<DbLight>
    private var inflater: LayoutInflater? = null
    private var adaper: DeviceDetailListAdapter? = null
    private val SCENE_MAX_COUNT = 16
    private var currentLight: DbLight? = null
    private var positionCurrent: Int = 0
    private var canBeRefresh = true
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
    private var isLinked: Boolean = false//是否是本灯连接

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_detail)
        type = this.intent.getIntExtra(Constant.DEVICE_TYPE, 0)
        inflater = this.layoutInflater
        this.mApplication = this.application as TelinkLightApplication

        lightsData = mutableListOf()
    }
    
    override fun onPostResume() {
        super.onPostResume()
        addListeners()
    }


    private fun addListeners() {
        addScanListeners()
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    override fun performed(event: Event<String>) {
        when (event.type) {
            LeScanEvent.LE_SCAN -> onLeScan(event as LeScanEvent)
            LeScanEvent.LE_SCAN_TIMEOUT -> LogUtils.e("zcl连接超时了")
            LeScanEvent.LE_SCAN_COMPLETED -> LogUtils.e("zcl连接结束了")
            NotificationEvent.GET_ALARM -> {
            }
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            ServiceEvent.SERVICE_CONNECTED -> this.onServiceConnected(event as ServiceEvent)
            ServiceEvent.SERVICE_DISCONNECTED -> this.onServiceDisconnected(event as ServiceEvent)
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
            }
        }
    }

    private fun onServiceConnected(event: ServiceEvent) {}

    private fun onServiceDisconnected(event: ServiceEvent) {
        TelinkLightApplication.getApp().startLightService(TelinkLightService::class.java)
    }

    private fun isSwitch(uuid: Int): Boolean {
        return when (uuid) {
            DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SENSOR, DeviceType.NIGHT_LIGHT -> true
            else -> false
        }
    }

    private fun onErrorReport(info: ErrorReportInfo) {

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
     * 处理扫描设备详情
     * @param event
     */
    @Synchronized
    private fun onLeScan(event: LeScanEvent) {
        val deviceInfo: DeviceInfo = event.args

        Thread {
            val dbLight = DBUtils.getLightByMeshAddr(deviceInfo.meshAddress)
            if (dbLight != null && dbLight.macAddr == "0") {
                dbLight.macAddr = deviceInfo.macAddress
                DBUtils.updateLight(dbLight)
            }
        }.start()

        if (!isSwitch(deviceInfo.productUUID)) {
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
                TelinkLightService.Instance()?.enableNotification()
                TelinkLightService.Instance()?.updateNotification()
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
                //连接成功


            }
            LightAdapter.STATUS_LOGOUT -> {
                retryConnect()
            }
            LightAdapter.STATUS_CONNECTING -> {
                Log.d("connectting", "444")
            }
            LightAdapter.STATUS_CONNECTED -> {
                TelinkLightService.Instance() ?: return
                if (!TelinkLightService.Instance()!!.isLogin)
                    autoConnect(true)
                hideLoadingDialog()
            }
            LightAdapter.STATUS_ERROR_N -> onNError(event)
        }
    }

    private fun stopConnectTimer() {
        mConnectDisposal?.dispose()
    }

    private fun onNError(event: DeviceEvent) {

        TelinkLightService.Instance()?.idleMode(true)
        TelinkLog.d("DeviceScanningActivity#onNError")

        val builder = AlertDialog.Builder(this)
        builder.setMessage("当前环境:Android7.0!连接重试:" + " 3次失败!")
        builder.setNegativeButton("confirm") { dialog, _ -> dialog.dismiss() }
        builder.setCancelable(false)
        builder.show()
    }


    override fun onResume() {
        super.onResume()
       // type = this.intent.getIntExtra(Constant.DEVICE_TYPE, 0)
        inflater = this.layoutInflater
        initData()
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
        recycleView!!.layoutManager = GridLayoutManager(this, 3)
        recycleView!!.itemAnimator = DefaultItemAnimator()

        recycleView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (recyclerView.layoutManager != null)
                    getPositionAndOffset()
            }
        })

        /**
         *单个设备列表适配器页面设置
         */
        adaper = DeviceDetailListAdapter(R.layout.device_detail_adapter, lightsData)
        adaper!!.onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
            currentLight = lightsData[position]
            positionCurrent = position

            mTelinkLightService = TelinkLightService.Instance()
            if (mTelinkLightService?.adapter?.mLightCtrl?.currentLight?.isConnected != true) {
                while (TelinkApplication.getInstance()?.serviceStarted == true) {
                    GlobalScope.launch(Dispatchers.Main) {
                        retryConnectCount = 0
                        connectFailedDeviceMacList.clear()
                        TelinkLightService.Instance().idleMode(true)
                        LeBluetooth.getInstance().stopScan()
                        if (TelinkApplication.getInstance()?.connectDevice == null){
                            autoConnect(false)
                        }
                        LogUtils.e("zcl设备列表页重连开始扫描设备" + DBUtils.lastUser?.controlMeshName)
                    }
                    break
                }
            } else
                when (view.id) {
                    R.id.img_light -> {
                        canBeRefresh = true
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
                for (i in lightsData?.indices) {
                    lightsData[i].updateIcon()
                }
            }

            Constant.INSTALL_RGB_LIGHT -> {
                toolbar.title = getString(R.string.rgb_light) + " (" + lightsData.size + ")"
                for (i in lightsData?.indices) {
                    lightsData[i].updateRgbIcon()
                }
            }

            Constant.INSTALL_LIGHT_OF_CW -> {
                for (i in lightsData?.indices) {
                    lightsData[i].updateIcon()
                }
            }

            Constant.INSTALL_LIGHT_OF_RGB -> {
                for (i in lightsData?.indices) {
                    lightsData[i].updateRgbIcon()
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
    }

    private val onClick = View.OnClickListener {
        when (it.id) {
            R.id.install_device -> {
                showInstallDeviceList()
            }
            R.id.create_group -> {
                dialog_device?.visibility = View.GONE
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    addNewGroup()
                }
            }
            R.id.create_scene -> {
                dialog_device?.visibility = View.GONE
                val nowSize = DBUtils.sceneList.size
                if (TelinkLightApplication.getApp().connectDevice == null) {
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
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
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

    }

    val INSTALL_NORMAL_LIGHT = 0
    val INSTALL_RGB_LIGHT = 1
    val INSTALL_SWITCH = 2
    val INSTALL_SENSOR = 3
    val INSTALL_CURTAIN = 4
    val INSTALL_CONNECTOR = 5
    private val onItemClickListenerInstallList = BaseQuickAdapter.OnItemClickListener { _, _, position ->
        isGuide = false
        installDialog?.dismiss()
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
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_RGB_LIGHT -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_RGB)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_CURTAIN -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_CURTAIN)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_SWITCH -> startActivity(Intent(this, ScanningSwitchActivity::class.java))
                    INSTALL_SENSOR -> startActivity(Intent(this, ScanningSensorActivity::class.java))
                    INSTALL_CONNECTOR -> {
                        if(medressData <= MeshUtils.DEVICE_ADDRESS_MAX){
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_RELAY)       //connector也叫relay
                            startActivityForResult(intent, 0)
                        }else{
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
        textGp.setSelection(textGp.text.toString().length)
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
                        DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, Constant.DEVICE_TYPE_DEFAULT_ALL)
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
                intent.putExtra(Constant.DEVICE_TYPE,  DeviceType.LIGHT_NORMAL)
                startActivityForResult(intent, 0)
            }
            Constant.INSTALL_RGB_LIGHT -> {
                intent = Intent(this, DeviceScanningNewActivity::class.java)
                intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                intent.putExtra(Constant.DEVICE_TYPE,  DeviceType.LIGHT_RGB)
                startActivityForResult(intent, 0)
            }
        }
    }

    private fun initData() {
        lightsData = ArrayList()
        when (type) {
            Constant.INSTALL_NORMAL_LIGHT -> {
                var all_light_data = DBUtils.getAllNormalLight()
                if (all_light_data.size > 0) {
                    var list_group: ArrayList<DbLight> = ArrayList()
                    var no_group: ArrayList<DbLight> = ArrayList()
                    for (i in all_light_data.indices) {
                        if (StringUtils.getLightName(all_light_data[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
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
                    batchGroup.visibility = View.GONE
                    batchGroup.setOnClickListener {
                        val intent = Intent(this,
                                BatchGroupActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                        intent.putExtra("lightType", "all_light")
                        startActivity(intent)
                    }
                } else {
                    recycleView.visibility = View.GONE
                    no_device_relativeLayout.visibility = View.VISIBLE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                        if (dialog_device?.visibility == View.GONE) {
                            showPopupMenu()
                        }
                    }
                }
            }
            Constant.INSTALL_RGB_LIGHT -> {
                var all_light_data = DBUtils.getAllRGBLight()
                lightsData.clear()
                if (all_light_data.size > 0) {
                    var list_group: ArrayList<DbLight> = ArrayList()
                    var no_group = ArrayList<DbLight>()
                    for (i in all_light_data.indices) {
                        if (StringUtils.getLightName(all_light_data[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
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
                    batchGroup.visibility = View.GONE
                    batchGroup.setOnClickListener {
                        val intent = Intent(this, RgbBatchGroupActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                        intent.putExtra("lightType", "all_light")
                        startActivity(intent)
                    }
                } else {
                    recycleView.visibility = View.GONE
                    no_device_relativeLayout.visibility = View.VISIBLE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                        if (dialog_device?.visibility == View.GONE)
                            showPopupMenu()
                    }
                }
            }
            Constant.INSTALL_LIGHT_OF_CW -> {
                var all_light_data = DBUtils.getAllNormalLight()
                if (all_light_data.size > 0) {
                    var list_group: ArrayList<DbLight> = ArrayList()
                    var no_group: ArrayList<DbLight> = ArrayList()
                    for (i in all_light_data.indices) {
                        if (StringUtils.getLightName(all_light_data[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
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
                    batchGroup.visibility = View.GONE
                    batchGroup.setOnClickListener {
                        val intent = Intent(this,
                                BatchGroupActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                        intent.putExtra("lightType", "cw_light")
                        intent.putExtra("cw_light_group_name", cwLightGroup)
                        startActivity(intent)
                    }
                } else {
                    recycleView.visibility = View.GONE
                    no_device_relativeLayout.visibility = View.VISIBLE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                        if (dialog_device?.visibility == View.GONE) {
                            showPopupMenu()
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
                        if (StringUtils.getLightName(all_light_data[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
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
                    LogUtils.e("zcl---rgb_light_name----$rgbLightGroup")
                    toolbar!!.tv_function1.visibility = View.VISIBLE
                    recycleView.visibility = View.VISIBLE
                    no_device_relativeLayout.visibility = View.GONE
                    var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
                    batchGroup.setText(R.string.batch_group)
                    batchGroup.visibility = View.GONE
                    batchGroup.setOnClickListener {
                        val intent = Intent(this, RgbBatchGroupActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                        intent.putExtra("lightType", "rgb_light")
                        intent.putExtra("rgb_light_group_name", rgbLightGroup)
                        startActivity(intent)
                    }
                } else {
                    recycleView.visibility = View.GONE
                    no_device_relativeLayout.visibility = View.VISIBLE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                        if (dialog_device?.visibility == View.GONE) {
                            showPopupMenu()
                        }
                    }
                }
            }
        }

    }

    private fun showPopupMenu() {
        dialog_device?.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        canBeRefresh = false
        acitivityIsAlive = false
        mScanDisposal?.dispose()
        if (TelinkLightApplication.getApp().connectDevice == null) {
            TelinkLightService.Instance()?.idleMode(true)
            LeBluetooth.getInstance().stopScan()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        notifyData()
        GlobalScope.launch {
            //踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
            delay(2000)
            val b = this@DeviceDetailAct == null || this@DeviceDetailAct.isDestroyed || this@DeviceDetailAct.isFinishing || !acitivityIsAlive
            if (!b)
                autoConnect(true)
        }
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
                    } else
                        true
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
        when (type) {
            Constant.INSTALL_NORMAL_LIGHT -> lightsData = DBUtils.getAllNormalLight()
            Constant.INSTALL_RGB_LIGHT -> lightsData = DBUtils.getAllRGBLight()
        }
        return lightsData
    }
    var locationServiceDialog: AlertDialog? = null
    fun showOpenLocationServiceDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.open_location_service)
        builder.setNegativeButton(getString(android.R.string.ok)) { _, _ ->
            BleUtils.jumpLocationSetting()
        }
        locationServiceDialog = builder.create()
        locationServiceDialog?.setCancelable(false)
        locationServiceDialog?.show()
    }

    fun hideLocationServiceDialog() {
        locationServiceDialog?.hide()
    }

    private fun addScanListeners() {
        this.mApplication?.removeEventListener(this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)
    }

    /**
     * 重试
     * 先扫描到信号比较好的，再连接。
     */
    private fun retryConnect() {
        if (retryConnectCount < MAX_RETRY_CONNECT_TIME) {
            retryConnectCount++
            autoConnect(false)
        } else {
            TelinkLightService.Instance()?.idleMode(true)
            retryConnectCount = 0
            connectFailedDeviceMacList.clear()
            autoConnect(false)
        }
    }

    override fun onStop() {
        super.onStop()
        this.mApplication!!.removeEventListener(this)
        if (TelinkLightService.Instance() != null)
            TelinkLightService.Instance()?.disableAutoRefreshNotify()
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
        mCheckRssiDisposal?.dispose()

        mNotFoundSnackBar?.dismiss()
        //移除事件
        this.mApplication?.removeEventListener(this)
        stopConnectTimer()
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


    /**
     * 自动重连
     */
    fun autoConnect(b: Boolean) {
        if (TelinkLightService.Instance() != null) {
            if (TelinkLightService.Instance()?.mode != LightAdapter.MODE_AUTO_CONNECT_MESH) {
                if (retryConnectCount > 5)
                    return
                retryConnectCount++
                if (!b)
                    ToastUtils.showLong(getString(R.string.connecting))


                if (this.mApplication?.isEmptyMesh != false)
                    return

                val mesh = this.mApplication?.mesh

                if (TextUtils.isEmpty(mesh?.name) || TextUtils.isEmpty(mesh?.password)) {
                    TelinkLightService.Instance()?.idleMode(true)
                    return
                }

                //自动重连参数
                val connectParams = Parameters.createAutoConnectParameters()
                connectParams.setMeshName(DBUtils.lastUser?.controlMeshName)
                connectParams.setPassword(NetworkFactory.md5(NetworkFactory.md5(DBUtils.lastUser?.controlMeshName) + DBUtils.lastUser?.controlMeshName))

                connectParams.autoEnableNotification(true)

                // 之前是否有在做MeshOTA操作，是则继续
                if (mesh!!.isOtaProcessing) {
                    connectParams.setConnectMac(mesh?.otaDevice!!.mac)
                }
                //自动重连
                TelinkLightService.Instance()?.autoConnect(connectParams)
            }

            //刷新Notify参数
            val refreshNotifyParams = Parameters.createRefreshNotifyParameters()
            refreshNotifyParams.setRefreshRepeatCount(2)
            refreshNotifyParams.setRefreshInterval(2000)
            //开启自动刷新Notify
            TelinkLightService.Instance()?.autoRefreshNotify(refreshNotifyParams)
        }
    }
}
