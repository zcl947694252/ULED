package com.dadoutek.uled.scene

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.method.ScrollingMovementMethod
import android.util.Log
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
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbSensor
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.othersview.HumanBodySensorActivity
import com.dadoutek.uled.pir.ConfigSensorAct
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.MeshUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_lights_of_group.*
import kotlinx.android.synthetic.main.activity_sensor_device_details.*
import kotlinx.android.synthetic.main.template_loading_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivity
import java.util.*
import java.util.concurrent.TimeUnit


private const val MAX_RETRY_CONNECT_TIME = 5

/**
 * 描述	      ${人体感应器列表}$
 */
class SensorDeviceDetailsActivity : TelinkBaseActivity(), EventListener<String> {
    private var disposable: Disposable? = null
    private val CONNECT_SENSOR_TIMEOUT: Long = 20000        //ms

    private var connectSensorTimeoutDisposable: Disposable? = null
    private lateinit var deviceInfo: DeviceInfo
    private var delete: TextView? = null
    private var group: TextView? = null
    private var set: ImageView? = null
    private var ota: TextView? = null
    private var rename: TextView? = null
    private var views: View? = null
    private var isClick: Int = 10//
    private var settingType: Int = 0 //0是正常连接 1是点击修改 2是点击删除
    private val NORMAL_SENSOR: Int = 0 //0是正常连接 1是点击修改 2是点击删除
    private val RECOVER_SENSOR: Int = 1 //0是正常连接 1是点击修改 2是点击删除
    private val RESET_SENSOR: Int = 2 //0是正常连接 1是点击修改 2是点击删除
    private val OTA_SENSOR: Int = 3//3是oat
    private val SENSOR_FINISH: Int = 4//4代表操作完成

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
    private var retryConnectCount = 0
    private val connectFailedDeviceMacList: MutableList<String> = mutableListOf()
    private var currentLight: DbSensor? = null
    private var positionCurrent: Int = 0
    private var mConnectDevice: DeviceInfo? = null
    private var acitivityIsAlive = true
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
    private var popupWindow: PopupWindow? = null
    private val SCENE_MAX_COUNT = 100
    private var compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mApplication = this.application as TelinkLightApplication
        setContentView(R.layout.activity_sensor_device_details)
        addScanListeners()
    }

    override fun onResume() {
        super.onResume()
        if (compositeDisposable.isDisposed) {
            compositeDisposable = CompositeDisposable()
        }
        initData()
        initView()
        popupWindow?.dismiss()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.dispose()
        LogUtils.d("connectSensorTimeoutDisposable?.dispose()")
        connectSensorTimeoutDisposable?.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        acitivityIsAlive = false
        if (popupWindow != null && popupWindow!!.isShowing)
            popupWindow!!.dismiss()
        compositeDisposable.dispose()
        this.mApplication?.removeEventListener(this)
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
        delete?.text = getString(R.string.delete)

        rename?.visibility = View.GONE
        ota?.visibility = View.VISIBLE
        group?.visibility = View.VISIBLE
        group?.text = getString(R.string.relocation)
        progressBar_sensor?.setOnClickListener {}

        add_device_btn.setOnClickListener {
            val lastUser = DBUtils.lastUser
            lastUser?.let {
                if (it.id.toString() != it.last_authorizer_user_id)
                    ToastUtils.showShort(getString(R.string.author_region_warm))
                else {
                    startActivity(Intent(this, ScanningSensorActivity::class.java))
                    doFinish()
                }
            }
        }//添加设备
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { doFinish() }
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
                        connectSensorTimeoutDisposable?.dispose()
                        disposable?.dispose()
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

        val installList: ArrayList<InstallDeviceModel> = OtherUtils.getInstallDeviceList(this)

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
                    INSTALL_SWITCH -> {
                        //intent = Intent(this, DeviceScanningNewActivity::class.java)
                        //intent.putExtra(Constant.DEVICE_TYPE, DeviceType.NORMAL_SWITCH)
                        //startActivityForResult(intent, 0)
                        startActivity(Intent(this, ScanningSwitchActivity::class.java))
                    }
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
        setScanningMode(true)
        if (sensorData.size > 0) {
            recycleView.visibility = View.VISIBLE
            no_device_relativeLayout.visibility = View.GONE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
        } else {
            recycleView.visibility = View.GONE
            no_device_relativeLayout.visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                val lastUser = DBUtils.lastUser
                lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showShort(getString(R.string.author_region_warm))
                    else {
                        if (dialog_pir?.visibility == View.GONE) {
                            dialog_pir?.visibility = View.VISIBLE//showPopupMenu
                        }
                    }
                }

            }
        }
    }

    var onItemChildClickListener = OnItemChildClickListener { _, view, position ->
        currentLight = sensorData[position]
        positionCurrent = position
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showShort(getString(R.string.author_region_warm))
            else {

                if (view.id == R.id.tv_setting) {
                    set = view!!.findViewById<ImageView>(R.id.tv_setting)

                    popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    popupWindow!!.contentView = views
                    popupWindow!!.isFocusable = true
                    popupWindow!!.showAsDropDown(set, 40, -15)
                    group?.setOnClickListener {
                        settingType = RECOVER_SENSOR
                        isClick = RECOVER_SENSOR
                        TelinkLightService.Instance()?.idleMode(true)
                        disposable?.dispose()
                        disposable = Observable.timer(1000, TimeUnit.MILLISECONDS)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    autoConnectSensor(true)//如果是断开状态直接重连不是就断开再重连
                                }
                         compositeDisposable.add(disposable!!)
                        popupWindow?.dismiss()
                    }

                    delete?.setOnClickListener {
                        //添加恢复出厂设置
                        //showResetConfirmDialog()
                        popupWindow?.dismiss()
                        var deleteSensor = sensorData[position]
                        AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_switch_confirm)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    DBUtils.deleteSensor(deleteSensor)
                                    notifyData()
                                    NetworkFactory.getApi()
                                            .deleteSensor(DBUtils.lastUser?.token, deleteSensor.id.toInt())
                                            .compose(NetworkTransformer())
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe({
                                                LogUtils.v("zcl-----删除服务器内传感器$it")
                                                Toast.makeText(this@SensorDeviceDetailsActivity, R.string.delete_switch_success, Toast.LENGTH_LONG).show()
                                            }, {})

                                    if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(deleteSensor.meshAddr)) {
                                        TelinkLightApplication.getApp().mesh.saveOrUpdate(this)
                                    }
                                    if (mConnectDevice != null) {
                                        Log.d(this.javaClass.simpleName, "mConnectDevice.meshAddress = " + mConnectDevice?.meshAddress)
                                        Log.d(this.javaClass.simpleName, "light.getMeshAddr() = " + currentLight?.meshAddr)
                                        if (deleteSensor.meshAddr == mConnectDevice?.meshAddress) {
                                            GlobalScope.launch {
                                                delay(2500)//踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
                                                if (this@SensorDeviceDetailsActivity == null ||
                                                        this@SensorDeviceDetailsActivity.isDestroyed ||
                                                        this@SensorDeviceDetailsActivity.isFinishing || !acitivityIsAlive) {
                                                } else
                                                    autoConnectSensor(true)
                                            }
                                        }
                                    }

                                    LogUtils.v("zc-----DBUtils.getAllSensor()删除后" + DBUtils.getAllSensor())
                                }
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show()

                    }
                    ota?.setOnClickListener {
                        settingType = OTA_SENSOR
                        isClick = OTA_SENSOR
                        val instance = TelinkLightService.Instance()
                        instance?.idleMode(true)
                        disposableTimer?.dispose()
                        disposableTimer = Observable.timer(1000, TimeUnit.MILLISECONDS)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    autoConnectSensor(true)
                                }
                        compositeDisposable.add(disposableTimer!!)

                        popupWindow!!.dismiss()
                    }
                }
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
        GlobalScope.launch(Dispatchers.Main) {
            if (b)
                showLoadingDialog(getString(R.string.please_wait))
        }
        LogUtils.e("zcl开始连接")
        //自动重连参数
        val connectParams = Parameters.createAutoConnectParameters()
        connectParams?.setMeshName(DBUtils.lastUser?.controlMeshName)
        connectParams?.setConnectMac(currentLight?.macAddr)
        connectParams?.setPassword(NetworkFactory.md5(NetworkFactory.md5(DBUtils.lastUser?.controlMeshName) + DBUtils.lastUser?.controlMeshName).substring(0, 16))
        connectParams?.autoEnableNotification(true)
        connectParams?.setTimeoutSeconds(CONNECT_SENSOR_TIMEOUT.toInt())
        progressBar_sensor.visibility = View.VISIBLE
        //连接，如断开会自动重连
        GlobalScope.launch {
            delay(1000)
            TelinkLightService.Instance()?.autoConnect(connectParams)
        }

        //确保上一个订阅被取消了
        connectSensorTimeoutDisposable?.dispose()
        connectSensorTimeoutDisposable = Observable.timer(CONNECT_SENSOR_TIMEOUT, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe({
            LeBluetooth.getInstance().stopScan()
            TelinkLightService.Instance()?.idleMode(true)
            hideLoadingDialog()
            progressBar_sensor.visibility = View.GONE
            ToastUtils.showShort(getString(R.string.connect_fail))
        }, {
            LogUtils.e(it)
        })
        compositeDisposable.add(connectSensorTimeoutDisposable!!)
    }


    /**
     * 恢复出厂设置
     */
    private fun resetSensor() {
        isClick = SENSOR_FINISH
        val opcode = Opcode.KICK_OUT//发送恢复出厂命令
        //mesadddr发0就是代表只发送给直连灯也就是当前连接灯 也可以使用当前灯的mesAdd 如果使用mesadd 有几个pir就恢复几个
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, 0, null)
        LogUtils.e("zcl", "zcl******重启人体")
        connectSensorTimeoutDisposable?.dispose()
    }

    override fun performed(event: Event<String>) {
        when (event.type) {
            LeScanEvent.LE_SCAN_TIMEOUT -> {
                LogUtils.e("zcl", "zcl******LE_SCAN_TIMEOUT")
                progressBar_sensor.visibility = View.GONE
                hideLoadingDialog()
            }

            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReportNormal(info)
            }

            DeviceEvent.STATUS_CHANGED -> {
                var status = (event as DeviceEvent).args.status
//                LogUtils.e("zcl", "zcl******STATUS_CHANGED$status")
                when (status) {
                    LightAdapter.STATUS_LOGIN -> {//3
                        progressBar_sensor.visibility = View.GONE
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
//                        LogUtils.e("zcl", "zcl***STATUS_LOGIN***$isClick")
                        when (isClick) {//重新配置
                            RECOVER_SENSOR -> {
                                disposable?.dispose()
                                disposable = Observable.timer(2, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                                        .subscribe {
                                            relocationSensor()
                                        }
                                disposableTimer?.dispose()
                            }
                            OTA_SENSOR -> {//人体感应器ota
                                getVersion(true)
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
                        if (isClick != RESET_SENSOR&&isClick!=SENSOR_FINISH )//恢复
                            retryConnect()

//                        LogUtils.e("zcl", "zcl***STATUS_LOGOUT***$settingType-----$isClick-----")
                        progressBar_sensor.visibility = View.GONE

                        when (settingType) {
                            RESET_SENSOR -> {//恢复出厂设置成功后判断灯能扫描
                                Toast.makeText(this@SensorDeviceDetailsActivity, R.string.reset_factory_success, Toast.LENGTH_LONG).show()
                                connectSensorTimeoutDisposable?.dispose()
                                DBUtils.deleteSensor(currentLight!!)
                                hideLoadingDialog()
                                notifyData()//重新设置传感器数量
                                DeviceType
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
                                            } else
                                                autoConnectSensor(false)
                                        }
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

    private fun getVersion(isOTA: Boolean) {
        if (TelinkApplication.getInstance().connectDevice != null) {
            LogUtils.e("TAG", currentLight!!.meshAddr.toString())
            progressBar_sensor.visibility = View.GONE
            connectSensorTimeoutDisposable?.dispose()

            val disposable = Commander.getDeviceVersion(currentLight!!.meshAddr)
                    .subscribe(
                            { s ->
                                hideLoadingDialog()
                                if ("" != s){
                                    if (isOTA) {
                                        currentLight!!.version = s
                                        var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_DEVELOPER_MODE, false)
                                        if (isBoolean) {
                                            transformView()
                                        } else {
                                            if (OtaPrepareUtils.instance().checkSupportOta(s)!!) {
                                                OtaPrepareUtils.instance().gotoUpdateView(this@SensorDeviceDetailsActivity, s, otaPrepareListner)
                                            } else {
                                                ToastUtils.showShort(getString(R.string.version_disabled))
                                                hideLoadingDialog()
                                            }
                                        }
                                    } else {
                                        if (deviceInfo.productUUID == DeviceType.SENSOR) {//老版本人体感应器
                                            startActivity<ConfigSensorAct>("deviceInfo" to deviceInfo, "version" to s)
                                        } else if (deviceInfo.productUUID == DeviceType.NIGHT_LIGHT) {//2.0
                                            startActivity<HumanBodySensorActivity>("deviceInfo" to deviceInfo, "update" to "1", "version" to s)
                                        }
                                        doFinish()
                                    }
                                    isClick = SENSOR_FINISH
                                }
                            },
                            {
                                hideLoadingDialog()
                            }
                    )
        }
    }

    private var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {

        override fun downLoadFileStart() {
            showLoadingDialog(getString(R.string.get_update_file))
        }

        override fun startGetVersion() {
            showLoadingDialog(getString(R.string.verification_version))
        }

        override fun getVersionSuccess(s: String) {
            hideLoadingDialog()
        }

        override fun getVersionFail() {
            ToastUtils.showLong(R.string.verification_version_fail)
            hideLoadingDialog()
        }


        override fun downLoadFileSuccess() {
            hideLoadingDialog()
            transformView()
        }

        override fun downLoadFileFail(message: String) {
            hideLoadingDialog()
            ToastUtils.showLong(R.string.download_pack_fail)
        }
    }

    private fun transformView() {
        connectSensorTimeoutDisposable?.dispose()
        disposable?.dispose()
        val intent = Intent(this@SensorDeviceDetailsActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.OTA_MAC, currentLight?.macAddr)
        intent.putExtra(Constant.OTA_MES_Add, currentLight?.meshAddr)
        intent.putExtra(Constant.OTA_VERSION, currentLight?.version)
        intent.putExtra(Constant.OTA_TYPE, DeviceType.SENSOR)
        startActivity(intent)
        finish()
    }


    /**
     * 重新配置 小心进入后断开
     */
    @SuppressLint("CheckResult")
    private fun relocationSensor() {
        deviceInfo = DeviceInfo()
        currentLight?.let {
            deviceInfo.meshAddress = it.meshAddr
            deviceInfo.macAddress = it.macAddr
            deviceInfo.productUUID = it.productUUID
            deviceInfo.id = it.id.toString()
            deviceInfo.isConfirm = 1
        }
        settingType = NORMAL_SENSOR
        hideLoadingDialog()
        getVersion(false)
    }


    private fun addScanListeners() {
        this.mApplication?.removeEventListener(this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)//扫描jt
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)//超时jt
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)//结束jt
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)//设备状态JT
        this.mApplication?.addEventListener(DeviceEvent.CURRENT_CONNECT_CHANGED, this)//设备状态JT
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)//设备状态JT
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
            TelinkLightService.Instance()?.idleMode(true)

            if (scanPb != null && !scanPb.isShown) {
                retryConnectCount = 0
                connectFailedDeviceMacList.clear()
            }
        }
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
    private fun doFinish() {
        connectSensorTimeoutDisposable?.dispose()
        disposable?.dispose()
        finish()
    }

}
