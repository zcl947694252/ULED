package com.dadoutek.uled.scene

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseQuickAdapter.OnItemChildClickListener
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbSensor
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.InstallDeviceModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.othersview.HumanBodySensorActivity
import com.dadoutek.uled.pir.ConfigSensorAct
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.BleUtils
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.MeshUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_lights_of_group.*
import kotlinx.android.synthetic.main.activity_sensor_device_details.*
import kotlinx.android.synthetic.main.template_loading_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivity
import java.util.concurrent.TimeUnit

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1

/**
 * 创建者     zcl
 * 创建时间   2019/7/27 16:35
 * 描述	      ${人体感应器}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 */
class SensorDeviceDetailsActivity : TelinkBaseActivity(), EventListener<String> {

    private var delete: TextView? = null
    private var group: TextView? = null
    private var set: ImageView? = null
    private var ota: TextView? = null
    private var rename: TextView? = null
    private var views: View? = null
    private var isClick: Int = 0//
    private var settingType: Int = 0 //0是正常连接 1是点击修改 2是点击删除
    private val NORMAL_SENSOR: Int = 0 //0是正常连接 1是点击修改 2是点击删除
    private val RECOVER_SENSOR: Int = 1 //0是正常连接 1是点击修改 2是点击删除
    private val RESET_SENSOR: Int = 2 //0是正常连接 1是点击修改 2是点击删除
    private val OTA_SENSOR: Int = 3//3是oat

    val INSTALL_NORMAL_LIGHT = 0
    val INSTALL_RGB_LIGHT = 1
    val INSTALL_SWITCH = 2
    val INSTALL_SENSOR = 3
    val INSTALL_CURTAIN = 4
    val INSTALL_CONNECTOR = 5
    var installDialog: android.app.AlertDialog? = null
    var isGuide: Boolean = false
    var clickRgb: Boolean = false
    private lateinit var sensorData: MutableList<DbSensor>
    private var adapter: SensorDeviceDetailsAdapter? = null
    private var mScanTimeoutDisposal: Disposable? = null
    private var mCheckRssiDisposal: Disposable? = null
    private var retryConnectCount = 0
    private var mTelinkLightService: TelinkLightService? = null
    private val connectFailedDeviceMacList: MutableList<String> = mutableListOf()
    private var currentLight: DbSensor? = null
    private var positionCurrent: Int = 0
    private var mConnectDevice: DeviceInfo? = null
    private var acitivityIsAlive = true
    private var mScanDisposal: Disposable? = null
    private var bestRSSIDevice: DeviceInfo? = null
    private var mApplication: TelinkLightApplication? = null
    private var mConnectDisposal: Disposable? = null
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
    private var popupWindow: PopupWindow? = null
    private val SCENE_MAX_COUNT = 16

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mApplication = this.application as TelinkLightApplication
        setContentView(R.layout.activity_sensor_device_details)
        addScanListeners()
    }

    override fun onResume() {
        super.onResume()
        initData()
        initView()
        popupWindow?.dismiss()
    }

    private fun initView() {
        recycleView!!.layoutManager = GridLayoutManager(this, 3)
        adapter = SensorDeviceDetailsAdapter(R.layout.sensor_detail_adapter, sensorData)
        adapter!!.bindToRecyclerView(recycleView)
        adapter!!.onItemChildClickListener = onItemChildClickListener

        install_device = findViewById(R.id.install_device)
        create_group = findViewById(R.id.create_group)
        create_scene = findViewById(R.id.create_scene)
        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)

        views = LayoutInflater.from(this).inflate(R.layout.popwindown_switch, null)
        group = views?.findViewById<TextView>(R.id.switch_group)
        ota = views?.findViewById<TextView>(R.id.ota)
        delete = views?.findViewById<TextView>(R.id.deleteBtn)
        rename = views?.findViewById<TextView>(R.id.rename)

        rename?.visibility = View.GONE
        ota?.visibility = View.VISIBLE
        group?.visibility = View.VISIBLE
        group?.text = getString(R.string.relocation)
        progressBar_sensor?.setOnClickListener {}

        add_device_btn.setOnClickListener { startActivity(Intent(this, ScanningSensorActivity::class.java)) }//添加设备
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { restartApplication()}
        toolbar.title = getString(R.string.sensor) + " (" + sensorData!!.size + ")"
    }

    private val onClick = View.OnClickListener {
        when (it.id) {
            R.id.install_device -> {
                showInstallDeviceList()
            }
            R.id.create_group -> {
                dialog_pir?.visibility = View.GONE
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    addNewGroup()
                }
            }
            R.id.create_scene -> {
                dialog_pir?.visibility = View.GONE
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
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }

    private fun showInstallDeviceList() {
        dialog_pir.visibility = View.GONE
        showInstallDeviceList(isGuide, isRgbClick)
    }

    private fun showInstallDeviceList(isGuide: Boolean, clickRgb: Boolean) {
        this.clickRgb = clickRgb
        val view = View.inflate(this, R.layout.dialog_install_list, null)
        val close_install_list = view.findViewById<ImageView>(R.id.close_install_list)
        val install_device_recyclerView = view.findViewById<RecyclerView>(R.id.install_device_recyclerView)
        close_install_list.setOnClickListener { installDialog?.dismiss() }

        val installList: java.util.ArrayList<InstallDeviceModel> = OtherUtils.getInstallDeviceList(this)

        val installDeviceListAdapter = InstallDeviceListAdapter(R.layout.item_install_device, installList)
        val layoutManager = LinearLayoutManager(this)
        install_device_recyclerView?.layoutManager = layoutManager
        install_device_recyclerView?.adapter = installDeviceListAdapter
        installDeviceListAdapter.bindToRecyclerView(install_device_recyclerView)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
        //添加分割线
        install_device_recyclerView?.addItemDecoration(decoration)

        installDeviceListAdapter.onItemClickListener = onItemClickListenerInstallList

        installDialog = android.app.AlertDialog.Builder(this).setView(view).create()

        if (isGuide)
            installDialog?.setCancelable(false)

        installDialog?.show()

        GlobalScope.launch(Dispatchers.Main) { delay(100) }
    }

    private val onItemClickListenerInstallList = BaseQuickAdapter.OnItemClickListener { _, view, position ->
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
        stepThreeText = view.findViewById(R.id.step_three)
        switchStepOne = view.findViewById(R.id.switch_step_one)
        switchStepTwo = view.findViewById(R.id.switch_step_two)
        swicthStepThree = view.findViewById(R.id.switch_step_three)
        val install_tip_question = view.findViewById<TextView>(R.id.install_tip_question)
        val search_bar = view.findViewById<Button>(R.id.search_bar)
        close_install_list.setOnClickListener(dialogOnclick)
        btnBack.setOnClickListener(dialogOnclick)
        search_bar.setOnClickListener(dialogOnclick)
        install_tip_question.text = describe
        install_tip_question.movementMethod = ScrollingMovementMethod.getInstance()
        installDialog = android.app.AlertDialog.Builder(this).setView(view).create()

        installDialog?.setOnShowListener {}
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
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_CURTAIN)
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

    private fun initData() {
        sensorData = DBUtils.getAllSensor()
//        LogUtils.e("zcl人体本地数据----------$sensorData")
        if (sensorData.size > 0) {
            recycleView.visibility = View.VISIBLE
            no_device_relativeLayout.visibility = View.GONE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
        } else {
            recycleView.visibility = View.GONE
            no_device_relativeLayout.visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                if (dialog_pir?.visibility == View.GONE) {
                    dialog_pir?.visibility = View.VISIBLE//showPopupMenu
                }
            }
        }
    }

    var onItemChildClickListener = OnItemChildClickListener { _, view, position ->
        currentLight = sensorData[position]
        positionCurrent = position
        if (view.id == R.id.tv_setting) {
            set = view!!.findViewById<ImageView>(R.id.tv_setting)

            popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            popupWindow!!.contentView = views
            popupWindow!!.isFocusable = true
            popupWindow!!.showAsDropDown(set, 40, -15)

            group?.setOnClickListener {
                settingType = RECOVER_SENSOR
                isClick = RECOVER_SENSOR
                TelinkLightService.Instance().idleMode(true)
                Observable.timer(1000, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            autoConnectSensor(true)//如果是断开状态直接重连不是就断开再重连
                        }

                popupWindow!!.dismiss()
            }

            delete?.setOnClickListener {
                //添加恢复出厂设置
                showResetConfirmDialog()
            }
            ota?.setOnClickListener {
                settingType = OTA_SENSOR
                isClick = OTA_SENSOR
                TelinkLightService.Instance().idleMode(true)
                Observable.timer(1000, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            autoConnectSensor(true)
                        }

                popupWindow!!.dismiss()
            }
        }
    }

    private fun showResetConfirmDialog() {
        var textView = TextView(this)
        textView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        textView.gravity = Gravity.CENTER
        textView.text = getString(R.string.delete_light_confirm)
        AlertDialog.Builder(this)
                .setTitle(R.string.factory_reset)
                .setView(textView)
                .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                    TelinkLightService.Instance()?.idleMode(true)
                    isClick = RESET_SENSOR

                    settingType = RESET_SENSOR
                    autoConnectSensor(true)

                    /*
                      val b = TelinkLightApplication.getApp().connectDevice == null
                    LogUtils.e("zcl", "zcl******$b")
                    if (TelinkLightApplication.getApp().connectDevice == null) {
                             settingType = RESET_SENSOR
                             autoConnectSensor()
                         } else {
                             TelinkLightService.Instance()?.idleMode(true)
                             settingType = NORMAL_SENSOR
                         }*/
                    popupWindow!!.dismiss()
                    progressBar_sensor.visibility = View.VISIBLE
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { _, _ ->
                    popupWindow!!.dismiss()
                }.show()
    }

    @SuppressLint("CheckResult")
    private fun autoConnectSensor(b: Boolean) {
        retryConnectCount++
        if (b)
            showLoadingDialog(getString(R.string.please_wait))
        LogUtils.e("zcl开始连接")
        //自动重连参数
        val connectParams = Parameters.createAutoConnectParameters()
        connectParams?.setMeshName(DBUtils.lastUser?.controlMeshName)
        connectParams?.setConnectMac(currentLight?.macAddr)
        connectParams?.setPassword(NetworkFactory.md5(NetworkFactory.md5(DBUtils.lastUser?.controlMeshName) + DBUtils.lastUser?.controlMeshName).substring(0, 16))
        connectParams?.autoEnableNotification(true)
        connectParams?.setTimeoutSeconds(5)
        progressBar_sensor.visibility = View.VISIBLE
        //连接，如断开会自动重连
        GlobalScope.launch {
            delay(1000)
            TelinkLightService.Instance()?.autoConnect(connectParams)
        }

        Observable.timer(15000, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe( {
            LeBluetooth.getInstance().stopScan()
            TelinkLightService.Instance()?.idleMode(true)
            hideLoadingDialog()
            progressBar_sensor.visibility =View.GONE
            ToastUtils.showShort(getString(R.string.connect_fail))
        },{})
    }

    /**
     * 恢复出厂设置
     */
    private fun resetSensor() {
        isClick = NORMAL_SENSOR
        val opcode = Opcode.KICK_OUT//发送恢复出厂命令
        //mesadddr发0就是代表只发送给直连灯也就是当前连接灯 也可以使用当前灯的mesAdd 如果使用mesadd 有几个pir就恢复几个
        TelinkLightService.Instance().sendCommandNoResponse(opcode, 0, null)
        LogUtils.e("zcl", "zcl******重启人体")
    }

    override fun performed(event: Event<String>) {
        when (event.type) {
            LeScanEvent.LE_SCAN_TIMEOUT -> {
                LogUtils.e("zcl", "zcl******LE_SCAN_TIMEOUT")
                progressBar_sensor.visibility = View.GONE
                hideLoadingDialog()
            }

            DeviceEvent.STATUS_CHANGED -> {
                var status = (event as DeviceEvent).args.status
                LogUtils.e("zcl", "zcl******STATUS_CHANGED$status")
                when (status) {
                    LightAdapter.STATUS_LOGIN -> {//3
                        progressBar_sensor.visibility = View.GONE
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                        LogUtils.e("zcl", "zcl***STATUS_LOGIN***$isClick")
                        when (isClick) {//重新配置
                            RECOVER_SENSOR -> Observable.timer(2, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                                    .subscribe {
                                        relocationSensor()
                                    }
                            OTA_SENSOR -> {//人体感应器ota
                                getVersion()
                            }
                            RESET_SENSOR -> resetSensor() //恢复出厂设置
                        }
                    }
                    /**
                     *settingType //0是正常连接 1是点击修改 2是点击删除3是oat   NORMAL_SENSOR = 0
                     *RECOVER_SENSOR = 1  RESET_SENSOR= 2   OTA_SENSOR = 3
                     * STATUS_CONNECTING = 0;STATUS_CONNECTED = 1;STATUS_LOGINING = 2;STATUS_LOGIN = 3;STATUS_LOGOUT = 4;
                     */
                    LightAdapter.STATUS_LOGOUT -> {//4
                        if (isClick != RESET_SENSOR)//恢复
                            retryConnect()

                        LogUtils.e("zcl", "zcl***STATUS_LOGOUT***$settingType-----$isClick-----")
                        progressBar_sensor.visibility = View.GONE

                        when (settingType) {
                            /*     NORMAL_SENSOR -> {// 0 断开其他灯的连接回调判断
                                     when (isClick) {//恢复出厂设置
                                         RESET_SENSOR -> if (TelinkLightApplication.getApp().connectDevice == null)
                                             autoConnectSensor()

                                         OTA_SENSOR -> {//OTA
                                             if (TelinkLightApplication.getApp().connectDevice == null)
                                                 autoConnectSensor()
                                         }
                                         RECOVER_SENSOR -> {//断联通知重新配置
                                             LogUtils.e("zcl", "zcl**重新配置****" + { TelinkLightApplication.getApp().connectDevice == null })
                                             relocationSensor()
                                         }
                                     }
                                 }*/
                            RESET_SENSOR -> {//恢复出厂设置成功后判断灯能扫描
                                Toast.makeText(this@SensorDeviceDetailsActivity, R.string.reset_factory_success, Toast.LENGTH_LONG).show()
                                DBUtils.deleteSensor(currentLight!!)
                                hideLoadingDialog()
                                notifyData()//重新设置传感器数量
                                settingType = NORMAL_SENSOR
                                if (mConnectDevice != null) {
                                    LogUtils.d(this.javaClass.simpleName, "mConnectDevice.meshAddress = " + mConnectDevice?.meshAddress)
                                    LogUtils.d(this.javaClass.simpleName, "light.getMeshAddr() = " + currentLight?.meshAddr)

                                    if (currentLight?.meshAddr == mConnectDevice?.meshAddress) {
                                        GlobalScope.launch {
                                            //踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
                                            delay(1000)
                                            if (this@SensorDeviceDetailsActivity == null || this@SensorDeviceDetailsActivity.isDestroyed ||
                                                    this@SensorDeviceDetailsActivity.isFinishing || !acitivityIsAlive) {
                                            } else autoConnectSensor(false)
                                        }
                                    } else {
                                        hideLoadingDialog()
                                    }
                                }
                            }
                        }

                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                    }
                }
            }
        }
    }

    private fun getVersion() {
        if (TelinkApplication.getInstance().connectDevice != null) {
            LogUtils.e("TAG", currentLight!!.meshAddr.toString())
            progressBar_sensor.visibility = View.GONE
            Commander.getDeviceVersion(currentLight!!.meshAddr, { s ->
                if ("" != s)
                    if (OtaPrepareUtils.instance().checkSupportOta(s)!!) {
                        currentLight!!.version = s
                        val intent = Intent(this@SensorDeviceDetailsActivity, OTAUpdateActivity::class.java)
                        intent.putExtra(Constant.OTA_MAC, currentLight?.macAddr)
                        intent.putExtra(Constant.OTA_MES_Add, currentLight?.meshAddr)
                        intent.putExtra(Constant.OTA_VERSION, currentLight?.version)
                        startActivity(intent)
                    } else {
                        ToastUtils.showShort(getString(R.string.version_disabled))
                        hideLoadingDialog()
                    }
            }, { hideLoadingDialog() })
            isClick = NORMAL_SENSOR
        }
    }

    /**
     * 重新配置 小心进入后断开
     */
    @SuppressLint("CheckResult")
    private fun relocationSensor() {
        val deviceInfo = DeviceInfo()
        currentLight?.let {
            deviceInfo.meshAddress = it.meshAddr
            deviceInfo.macAddress = it.macAddr
            deviceInfo.productUUID = it.productUUID
            deviceInfo.id = it.id.toString()
            deviceInfo.isConfirm = 1
        }
        isClick = NORMAL_SENSOR
        settingType = NORMAL_SENSOR
        hideLoadingDialog()
        if (deviceInfo.productUUID == DeviceType.SENSOR) {//老版本人体感应器
            startActivity<ConfigSensorAct>("deviceInfo" to deviceInfo)
        } else if (deviceInfo.productUUID == DeviceType.NIGHT_LIGHT) {//2.0
            startActivity<HumanBodySensorActivity>("deviceInfo" to deviceInfo, "update" to "1")
        }
    }


    private fun addScanListeners() {
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)//扫描jt
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)//超时jt
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)//结束jt
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)//设备状态JT
        this.mApplication?.addEventListener(DeviceEvent.CURRENT_CONNECT_CHANGED, this)//设备状态JT
    }

    /**
     * 重试
     * 先扫描到信号比较好的，再连接。
     */
    private fun retryConnect() {
        if (retryConnectCount < MAX_RETRY_CONNECT_TIME) {
            retryConnectCount++
            autoConnectSensor(true)
        } else {
            TelinkLightService.Instance().idleMode(true)
            if (!scanPb.isShown) {
                retryConnectCount = 0
                connectFailedDeviceMacList.clear()
            }
        }
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

    fun notifyData() {
        val mOldDatas: MutableList<DbSensor>? = sensorData
        val mNewDatas: MutableList<DbSensor>? = getNewData()
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
        adapter?.let { diffResult.dispatchUpdatesTo(it) }
        sensorData = mNewDatas!!
        toolbar.title = getString(R.string.sensor) + " (" + sensorData.size + ")"
        adapter!!.setNewData(sensorData)
        if (sensorData.size <= 0) {
            no_device_relativeLayout.visibility = View.VISIBLE
            recycleView.visibility = View.GONE
        } else {
            no_device_relativeLayout.visibility = View.GONE
            recycleView.visibility = View.VISIBLE
        }
    }

    private fun getNewData(): MutableList<DbSensor> {
        sensorData = DBUtils.getAllSensor()
        toolbar.title = (currentLight!!.name ?: "")
        return sensorData
    }

    override fun onDestroy() {
        super.onDestroy()
        acitivityIsAlive = false
        if (popupWindow != null && popupWindow!!.isShowing)
            popupWindow!!.dismiss()

        this.mApplication?.removeEventListeners()
    }

}
