package com.dadoutek.uled.connector

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.*
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbConnector
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.rgb.RGBSettingActivity
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.BleUtils
import com.dadoutek.uled.util.DialogUtils
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.StringUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.light.ConnectionStatus
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LeScanParameters
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.MeshUtils.DEVICE_ADDRESS_MAX
import com.telink.util.Strings
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_connector_device_detail.*
import kotlinx.android.synthetic.main.activity_lights_of_group.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.indefiniteSnackbar
import java.util.concurrent.TimeUnit

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1

/**
 * 蓝牙接收器列表
 */
class ConnectorDeviceDetailActivity : TelinkBaseActivity(), EventListener<String>, View.OnClickListener {

    private var type: Int? = null

    private lateinit var lightsData: MutableList<DbConnector>

    private var inflater: LayoutInflater? = null

    private var adaper: DeviceDetailConnectorAdapter? = null

    private var currentLight: DbConnector? = null

    private var positionCurrent: Int = 0

//    private lateinit var lightList: MutableList<DbLight>

    private var canBeRefresh = true

//    private lateinit var group: DbGroup

    private val REQ_LIGHT_SETTING: Int = 0x01

    private var acitivityIsAlive = true


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

    private val SCENE_MAX_COUNT = 16

    override fun performed(event: Event<String>?) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connector_device_detail)
        type = this.intent.getIntExtra(Constant.DEVICE_TYPE, 0)
        inflater = this.layoutInflater
        initDate()
        initView()
    }

    override fun onResume() {
        super.onResume()
        inflater = this.layoutInflater
        disableConnectionStatusListener()
        initDate()
        initView()
    }

    private fun initView() {
        mApplication?.removeEventListener(this)
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
        recycleView!!.layoutManager = GridLayoutManager(this, 3)
        recycleView!!.itemAnimator = DefaultItemAnimator()
        adaper = DeviceDetailConnectorAdapter(R.layout.device_detail_adapter, lightsData)
        adaper!!.onItemChildClickListener = onItemChildClickListener
        adaper!!.bindToRecyclerView(recycleView)
        for (i in lightsData?.indices!!) {
            lightsData!![i].updateIcon()
        }
        install_device = findViewById(R.id.install_device)
        create_group = findViewById(R.id.create_group)
        create_scene = findViewById(R.id.create_scene)
        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)

        add_device_btn.setOnClickListener(this)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        toolbar.title = getString(R.string.relay) + " (" + lightsData.size + ")"

    }

    private val onClick = View.OnClickListener {
        when (it.id) {
            R.id.install_device -> {
                showInstallDeviceList()
            }
            R.id.create_group -> {
                dialog_relay?.visibility = View.GONE
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    addNewGroup()
                }
            }
            R.id.create_scene -> {
                dialog_relay?.visibility = View.GONE
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
                        DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, Constant.DEVICE_TYPE_DEFAULT_ALL)
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }

    private fun showInstallDeviceList() {
        dialog_relay.visibility = View.GONE
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

        val installList: java.util.ArrayList<InstallDeviceModel> = OtherUtils.getInstallDeviceList(this)

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
        stepOneText = view.findViewById(R.id.step_one)
        stepTwoText = view.findViewById(R.id.step_two)
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
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_RGB_LIGHT -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_RGB)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_CURTAIN -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_CURTAIN)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_SWITCH -> {
                        startActivity(Intent(this, ScanningSwitchActivity::class.java))
                    }
                    INSTALL_SENSOR -> startActivity(Intent(this, ScanningSensorActivity::class.java))
                    INSTALL_CONNECTOR -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_RELAY)       //connector也叫relay
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

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.add_device_btn -> addDevice()
        }
    }

    private fun addDevice() {
        intent = Intent(this, DeviceScanningNewActivity::class.java)
        intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_RELAY)
        startActivityForResult(intent, 0)
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentLight = lightsData?.get(position)
        positionCurrent = position
        Opcode.LIGHT_ON_OFF
        when {
            view.id == R.id.img_light -> {
                if (TelinkLightService.Instance()?.isLogin == false) {
                    autoConnect()
                } else {
                    canBeRefresh = true
                    if (currentLight!!.connectionStatus == ConnectionStatus.OFF.value) {
                        if (currentLight!!.productUUID == DeviceType.SMART_CURTAIN) {
                            Commander.openOrCloseCurtain(currentLight!!.meshAddr, true, false)
                        } else {
                            Commander.openOrCloseLights(currentLight!!.meshAddr, true)
                        }

                        currentLight!!.connectionStatus = ConnectionStatus.ON.value
                    } else {
                        if (currentLight!!.productUUID == DeviceType.SMART_CURTAIN) {
                            Commander.openOrCloseCurtain(currentLight!!.meshAddr, false, false)
                        } else {
                            Commander.openOrCloseLights(currentLight!!.meshAddr, false)
                        }
                        currentLight!!.connectionStatus = ConnectionStatus.OFF.value
                    }

                    currentLight!!.updateIcon()
                    DBUtils.updateConnector(currentLight!!)
                    runOnUiThread {
                        adapter?.notifyDataSetChanged()
                    }
                }
            }

            view.id == R.id.tv_setting -> {
                var intent = Intent(this@ConnectorDeviceDetailActivity, ConnectorSettingActivity::class.java)
                if (currentLight?.productUUID == DeviceType.LIGHT_RGB) {
                    intent = Intent(this@ConnectorDeviceDetailActivity, RGBSettingActivity::class.java)
                    intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_LIGHT)
                }
                intent.putExtra(Constant.LIGHT_ARESS_KEY, currentLight)
                intent.putExtra(Constant.GROUP_ARESS_KEY, currentLight!!.meshAddr)
                intent.putExtra(Constant.LIGHT_REFRESH_KEY, Constant.LIGHT_REFRESH_KEY_OK)
                startActivityForResult(intent, REQ_LIGHT_SETTING)
            }
            else -> ToastUtils.showShort(R.string.reconnecting)
        }
    }

    private fun initDate() {
        lightsData = ArrayList()
        when (type) {
            Constant.INSTALL_CONNECTOR -> {
                var all_light_data = DBUtils.getAllRelay()
                if (all_light_data.size > 0) {
                    var list_group: ArrayList<DbConnector> = ArrayList()
                    var no_group: ArrayList<DbConnector> = ArrayList()
                    for (i in all_light_data.indices) {
                        if (StringUtils.getConnectorName(all_light_data[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
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
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
                    batchGroup.setText(R.string.batch_group)
                    batchGroup.visibility = View.GONE
                    batchGroup.setOnClickListener {
                        val intent = Intent(this,
                                ConnectorBatchGroupActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                        intent.putExtra("relayType", "all_relay")
                        startActivity(intent)
                    }
                } else {
                    recycleView.visibility = View.GONE
                    no_device_relativeLayout.visibility = View.VISIBLE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                        if (dialog_relay?.visibility == View.GONE) {
                            showPopupMenu()
                        }
                    }
                }
            }

            Constant.INSTALL_RELAY_OF -> {
                var all_light_data = DBUtils.getAllRelay()
                if (all_light_data.size > 0) {
                    var list_group: ArrayList<DbConnector> = ArrayList()
                    var no_group: ArrayList<DbConnector> = ArrayList()
                    for (i in all_light_data.indices) {
                        if (StringUtils.getConnectorName(all_light_data[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
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
                    var relayGroup = this.intent.getStringExtra("relay_name")
                    var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
                    batchGroup.setText(R.string.batch_group)
                    batchGroup.visibility = View.GONE
                    batchGroup.setOnClickListener {
                        val intent = Intent(this,
                                ConnectorBatchGroupActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                        intent.putExtra("relayType", "group_relay")
                        intent.putExtra("relay_group_name", relayGroup)
                        startActivity(intent)
                    }
                } else {
                    recycleView.visibility = View.GONE
                    no_device_relativeLayout.visibility = View.VISIBLE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                        if (dialog_relay?.visibility == View.GONE) {
                            showPopupMenu()
                        }
                    }
                }
            }
        }
    }

    private fun showPopupMenu() {
        dialog_relay?.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        canBeRefresh = false
        acitivityIsAlive = false
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        notifyData()
        GlobalScope.launch {
            //踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
            delay(2500)
            if (this@ConnectorDeviceDetailActivity == null ||
                    this@ConnectorDeviceDetailActivity.isDestroyed ||
                    this@ConnectorDeviceDetailActivity.isFinishing || !acitivityIsAlive) {
            } else {
                autoConnect()
            }
        }
    }

    fun notifyData() {
        val mOldDatas: MutableList<DbConnector>? = lightsData
        val mNewDatas: MutableList<DbConnector>? = getNewData()
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
        toolbar.title = getString(R.string.relay) + " (" + lightsData.size + ")"
        adaper!!.setNewData(lightsData)

    }

    private fun getNewData(): MutableList<DbConnector> {
        lightsData = DBUtils.getAllRelay()
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
                    var root = this@ConnectorDeviceDetailActivity.findViewById<LinearLayout>(R.id.root)
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
                    if (TelinkLightApplication.getApp().connectDevice == null) {
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
                            scanPb?.visibility = View.GONE
                            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), Constant.CONNECT_STATE_SUCCESS_KEY, true);
                        }
                    }

                }
            }

        }
    }


    @SuppressLint("CheckResult")
    private fun startScan() {
        //当App在前台时，才进行扫描。
        if (AppUtils.isAppForeground())
            if (acitivityIsAlive || mScanDisposal?.isDisposed != true) {
                mScanDisposal = RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN)
                        .subscribeOn(Schedulers.io())
                        .subscribe {
                            if (it) {
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
                                if (!com.dadoutek.uled.util.AppUtils.isExynosSoc) {
                                    params.setScanFilters(scanFilters)
                                }
                                params.setMeshName(account)
                                params.setOutOfMeshName(account)
                                params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                                params.setScanMode(false)

                                addScanListeners()
                                TelinkLightService.Instance()?.startScan(params)
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
    }

    private fun addScanListeners() {
        this.mApplication?.removeEventListener(this)
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

    @SuppressLint("CheckResult")
    private fun connect(mac: String) {
        try {
            mCheckRssiDisposal?.dispose()
            mCheckRssiDisposal = RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN)
                    .subscribe {
                        if (it) {
                            //授予了权限
                            if (TelinkLightService.Instance() != null) {
                                progressBar?.visibility = View.VISIBLE
                                TelinkLightService.Instance()?.connect(mac, CONNECT_TIMEOUT)
                                startConnectTimer()
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

    private fun startConnectTimer() {
        mConnectDisposal?.dispose()
        mConnectDisposal = Observable.timer(CONNECT_TIMEOUT.toLong(), TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    retryConnect()
                }
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
            TelinkLightService.Instance()?.idleMode(true)
            if (!scanPb.isShown) {
                retryConnectCount = 0
                connectFailedDeviceMacList.clear()
                startScan()
            }

        }
    }

    private fun login() {
        val meshName = DBUtils.lastUser?.controlMeshName
        val pwd = NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16)
        TelinkLightService.Instance()?.login(Strings.stringToBytes(meshName, 16)
                , Strings.stringToBytes(pwd, 16))
    }

    /**
     * 扫描不到任何设备了
     * （扫描结束）
     */
    private fun onLeScanTimeout() {
        TelinkLightService.Instance()?.idleMode(true)
        LeBluetooth.getInstance().stopScan()
        startScan()
    }
}
