package com.dadoutek.uled.light

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseQuickAdapter.OnItemChildClickListener
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.gateway.bean.GwStompBean
import com.dadoutek.uled.gateway.util.Base64Utils
import com.dadoutek.uled.group.BatchGroupFourDeviceActivity
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.HttpModel.GwModel
import com.dadoutek.uled.model.InstallDeviceModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.rgb.RGBSettingActivity
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.widget.RecyclerGridDecoration
import com.telink.bluetooth.light.ConnectionStatus
import com.telink.util.MeshUtils
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_device_detail.*
import kotlinx.android.synthetic.main.template_search_tool.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * 创建者     zcl
 * 创建时间   2019/8/28 17:34
 * 描述	      ${六种 冷暖灯列表 从首页设备fragment点击跳入 以及扫描设备按钮}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${}$
 */

class DeviceDetailAct : TelinkBaseActivity(), View.OnClickListener {
    private var disposableTimer: Disposable? = null
    private lateinit var allLightData: ArrayList<DbLight>
    private var directLight: DbLight? = null
    private var mConnectDisposable: Disposable? = null
    private var type: Int? = null
    private lateinit var lightsData: ArrayList<DbLight>
    private var inflater: LayoutInflater? = null
    private var adaper: DeviceDetailListAdapter? = null
    private val SCENE_MAX_COUNT = 100
    private var currentLight: DbLight? = null
    private var positionCurrent: Int = 0
    private var canBeRefresh = true
    private val REQ_LIGHT_SETTING: Int = 0x01
    private var acitivityIsAlive = true
    private var retryConnectCount = 0
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
    private lateinit var stepThreeTextSmall: TextView
    private var lastOffset: Int = 0//距离
    private var lastPosition: Int = 0//第几个item
    private var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_detail)
        type = this.intent.getIntExtra(Constant.DEVICE_TYPE, 0)
        inflater = this.layoutInflater
        this.mApplication = this.application as TelinkLightApplication
        lightsData = arrayListOf()
    }

    override fun onResume() {
        super.onResume()
        inflater = this.layoutInflater
        initData()
        initView()
        initToolbar()
        scrollToPosition()
        if (TelinkLightApplication.getApp().connectDevice == null)
            autoConnect()
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
        /* if (directLight != null && TelinkLightApplication.getApp().connectDevice != null) {直连灯逻辑
             device_detail_direct_item.visibility = View.VISIBLE
             device_detail_direct_name.text = directLight?.name
             directLight?.groupName = StringUtils.getLightGroupName(directLight)
             device_detail_direct_group_name.text = directLight?.groupName
             if (directLight?.groupName == null || directLight?.groupName == getString(R.string.not_grouped))
                 device_detail_direct_group_name.visibility = View.GONE
             else
                 device_detail_direct_group_name.visibility = View.VISIBLE
             getString(R.string.rename)
             setTopLightState()

             device_detail_direct_icon?.setOnClickListener { openOrClose(directLight!!) }
             device_detail_direct_name_ly?.setOnClickListener {
                 currentLight = directLight
                 goSetting()
             }
             device_detail_direct_go_arr?.setOnClickListener {
                 currentLight = directLight
                 goSetting()
             }

         } else {*/
        device_detail_direct_item?.visibility = View.GONE
        // }

        recycleView!!.layoutManager = GridLayoutManager(this, 2)
        recycleView!!.addItemDecoration(RecyclerGridDecoration(this, 2))

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
        //adaper = DeviceDetailListAdapter(R.layout.device_detail_adapter, lightsData)
        adaper = DeviceDetailListAdapter(R.layout.device_detail_item, lightsData)
        adaper!!.onItemChildClickListener = OnItemChildClickListener { adapter, view, position ->
            if (position < lightsData.size) {
                currentLight = lightsData[position]
                positionCurrent = position

                if (TelinkLightApplication.getApp().connectDevice == null) {
                    GlobalScope.launch(Dispatchers.Main) {
                        //ToastUtils.showLong(getString(R.string.connecting_tip))
                        retryConnectCount = 0
                        autoConnect()
                    }
                    sendToGw()

                } else {
                    when (view.id) {
                        R.id.device_detail_item_img_icon -> {
                            canBeRefresh = true
                            openOrClose(currentLight!!)

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

                        R.id.device_detail_item_name_ly, R.id.device_detail_item_arr -> {
                            goSetting()
                        }
                    }
                }
            }
        }



        adaper!!.bindToRecyclerView(recycleView)
        val size =/* if (directLight != null && TelinkLightApplication.getApp().connectDevice != null)
            lightsData.size + 1
        else*/
                lightsData.size

        when (type) {
            Constant.INSTALL_NORMAL_LIGHT -> {
                toolbar.title = getString(R.string.normal_light_title) + " (" + size + ")"
                for (i in lightsData.indices) {
                    lightsData[i].updateIcon()
                }
            }
            Constant.INSTALL_RGB_LIGHT -> {
                toolbar.title = getString(R.string.rgb_light) + " (" + size + ")"
                for (i in lightsData.indices) {
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
        search_clear.setOnClickListener {
            clearSearch()
        }
        search_btn.setOnClickListener {
            clearSearch()
        }
        search_no_result.visibility = View.GONE
        search_view.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (TextUtils.isEmpty(s)) {
                    // 清除ListView的过滤
                    lightsData.clear()
                    lightsData.addAll(allLightData)
                    search_clear.visibility = View.GONE
                    search_btn.setTextColor(getColor(R.color.gray_6))
                    search_no_result.visibility = View.GONE
                    adaper?.notifyDataSetChanged()
                } else {
                    // 使用用户输入的内容对ListView的列表项进行过滤
                    lightsData.clear()
                    search_btn.text = getString(R.string.cancel)
                    search_btn.setTextColor(getColor(R.color.gray_6))
                    search_clear.visibility = View.VISIBLE
                    val list = allLightData.filter { dbLight -> dbLight.name.contains(s.toString()) }
                    if (list.isEmpty()) {
                        search_no_result.visibility = View.VISIBLE
                    } else {
                        search_no_result.visibility = View.GONE
                        lightsData.addAll(list)
                        adaper?.notifyDataSetChanged()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun sendToGw() {
        val gateWay = DBUtils.getAllGateWay()
        if (gateWay.size>0)
        GwModel.getGwList()?.subscribe(object : NetworkObserver<List<DbGateway>?>() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onNext(t: List<DbGateway>) {
                TelinkLightApplication.getApp().offLine = true
                hideLoadingDialog()
                t.forEach { db ->
                    //网关在线状态，1表示在线，0表示离线
                    if (db.state == 1)
                        TelinkLightApplication.getApp().offLine = false
                }
                if (!TelinkLightApplication.getApp().offLine) {
                    disposableTimer?.dispose()
                    disposableTimer = Observable.timer(7000, TimeUnit.MILLISECONDS).subscribe {
                        hideLoadingDialog()
                        runOnUiThread { ToastUtils.showShort(getString(R.string.gate_way_offline)) }
                    }
                    val low = currentLight!!.meshAddr and 0xff
                    val hight = (currentLight!!.meshAddr shr 8) and 0xff
                    val gattBody = GwGattBody()
                    var gattPar: ByteArray = byteArrayOf()
                    if (currentLight!!.connectionStatus == ConnectionStatus.OFF.value) {
                        if (currentLight!!.productUUID == DeviceType.LIGHT_NORMAL || currentLight!!.productUUID == DeviceType.LIGHT_RGB
                                || currentLight!!.productUUID == DeviceType.LIGHT_NORMAL_OLD) {//开灯
                            gattPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, low.toByte(), hight.toByte(), Opcode.LIGHT_ON_OFF,
                                    0x11, 0x02, 0x01, 0x64, 0, 0, 0, 0, 0, 0, 0, 0)
                            gattBody.ser_id = Constant.SER_ID_LIGHT_ON
                        }
                    } else {
                        if (currentLight!!.productUUID == DeviceType.LIGHT_NORMAL || currentLight!!.productUUID == DeviceType.LIGHT_RGB
                                || currentLight!!.productUUID == DeviceType.LIGHT_NORMAL_OLD) {
                            gattPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, low.toByte(), hight.toByte(), Opcode.LIGHT_ON_OFF,
                                    0x11, 0x02, 0x00, 0x64, 0, 0, 0, 0, 0, 0, 0, 0)
                            gattBody.ser_id = Constant.SER_ID_LIGHT_OFF
                        }
                    }

                    val s = Base64Utils.encodeToStrings(gattPar)
                    gattBody.data = s
                    gattBody.cmd = Constant.CMD_MQTT_CONTROL
                    gattBody.meshAddr = currentLight!!.meshAddr
                    sendToServer(gattBody)
                } else {
                    ToastUtils.showShort(getString(R.string.gw_not_online))
                }
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                hideLoadingDialog()
                ToastUtils.showShort(getString(R.string.gw_not_online))
            }
        })
    }

    private fun sendToServer(gattBody: GwGattBody) {
        GwModel.sendDeviceToGatt(gattBody)?.subscribe(object : NetworkObserver<String?>() {
            override fun onNext(t: String) {
                disposableTimer?.dispose()
                LogUtils.v("zcl-----------远程控制-------$t")
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                disposableTimer?.dispose()
                ToastUtils.showShort(e.message)
                LogUtils.v("zcl-----------远程控制-------${e.message}")
            }
        })
    }

    private fun clearSearch() {
        search_btn.text = getString(R.string.search)
        search_btn.setTextColor(getColor(R.color.gray_6))
        search_view.setText("")
    }


    private fun setTopLightState() {
        when (type) {
            Constant.INSTALL_NORMAL_LIGHT, Constant.INSTALL_LIGHT_OF_CW -> {
                if (directLight?.status == ConnectionStatus.OFFLINE.value || directLight?.status == ConnectionStatus.OFF.value)
                    device_detail_direct_icon.setImageResource(R.drawable.icon_device_down)
                else if (directLight?.status == ConnectionStatus.ON.value)
                    device_detail_direct_icon.setImageResource(R.drawable.icon_device_open)
            }
            Constant.INSTALL_RGB_LIGHT, Constant.INSTALL_LIGHT_OF_RGB -> {
                if (directLight?.status == ConnectionStatus.OFFLINE.value || directLight?.status == ConnectionStatus.OFF.value)
                    device_detail_direct_icon.setImageResource(R.drawable.icon_rgblight_down)
                else if (directLight?.status == ConnectionStatus.ON.value)
                    device_detail_direct_icon.setImageResource(R.drawable.icon_rgblight)
            }
        }
    }

    private fun goSetting() {
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

    private fun openOrClose(currentLight: DbLight) {
        LogUtils.v("zcl点击后的灯$currentLight")
        this.currentLight = currentLight
        if (currentLight!!.connectionStatus == ConnectionStatus.OFF.value) {
            if (currentLight!!.productUUID == DeviceType.SMART_CURTAIN) {//开窗
                Commander.openOrCloseCurtain(currentLight!!.meshAddr, isOpen = true, isPause = false)
            } else {
                Commander.openOrCloseLights(currentLight!!.meshAddr, true)//开灯
            }
            this.currentLight!!.connectionStatus = ConnectionStatus.ON.value
        } else {
            if (currentLight!!.productUUID == DeviceType.SMART_CURTAIN) {
                Commander.openOrCloseCurtain(currentLight!!.meshAddr, isOpen = false, isPause = false)//关窗
            } else {
                Commander.openOrCloseLights(currentLight!!.meshAddr, false)//关灯
            }
            this.currentLight!!.connectionStatus = ConnectionStatus.OFF.value
        }
        setTopLightState()
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
//                    addNewGroup()
                    popMain.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
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

        installDialog?.setOnShowListener {}

        if (isGuide) installDialog?.setCancelable(false)

        installDialog?.show()
    }

    val INSTALL_NORMAL_LIGHT = 0
    val INSTALL_RGB_LIGHT = 1
    val INSTALL_SWITCH = 2
    val INSTALL_SENSOR = 3
    val INSTALL_CURTAIN = 4
    val INSTALL_CONNECTOR = 5
    val INSTALL_GATEWAY = 6
    private val onItemClickListenerInstallList = BaseQuickAdapter.OnItemClickListener { _, _, position ->
        isGuide = false
        installDialog?.dismiss()
        when (position) {
            INSTALL_GATEWAY -> {
                installId = INSTALL_GATEWAY
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_NORMAL_LIGHT -> {
                installId = INSTALL_NORMAL_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_RGB_LIGHT -> {
                installId = INSTALL_RGB_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_CURTAIN -> {
                installId = INSTALL_CURTAIN
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_SWITCH -> {
                installId = INSTALL_SWITCH
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)

                stepOneText.visibility = View.GONE
                stepTwoText.visibility = View.GONE
                stepThreeText.visibility = View.GONE
                switchStepOne.visibility = View.VISIBLE
                switchStepTwo.visibility = View.VISIBLE
                swicthStepThree.visibility = View.VISIBLE
            }
            INSTALL_SENSOR -> {
                installId = INSTALL_SENSOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_CONNECTOR -> {
                installId = INSTALL_CONNECTOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
        }
    }

    private fun showInstallDeviceDetail(describe: String, position: Int) {
        val view = View.inflate(this, R.layout.dialog_install_detail, null)
        val close_install_list = view.findViewById<ImageView>(R.id.close_install_list)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        stepOneText = view.findViewById(R.id.step_one)
        stepTwoText = view.findViewById(R.id.step_two)
        stepThreeText = view.findViewById(R.id.step_three)
        stepThreeTextSmall = view.findViewById(R.id.step_three_small)
        switchStepOne = view.findViewById(R.id.switch_step_one)
        switchStepTwo = view.findViewById(R.id.switch_step_two)
        swicthStepThree = view.findViewById(R.id.switch_step_three)
        val install_tip_question = view.findViewById<TextView>(R.id.install_tip_question)
        val search_bar = view.findViewById<Button>(R.id.search_bar)
        close_install_list.setOnClickListener(dialogOnclick)
        btnBack.setOnClickListener(dialogOnclick)
        search_bar.setOnClickListener(dialogOnclick)

        val title = view.findViewById<TextView>(R.id.textView5)
        if (position == INSTALL_NORMAL_LIGHT) {
            title.visibility = View.GONE
            install_tip_question.visibility = View.GONE
        } else {
            title.visibility = View.VISIBLE
            install_tip_question.visibility = View.VISIBLE
        }

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
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_RELAY)       //connector也叫relay
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_GATEWAY -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.GATE_WAY)
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
        textGp.setSelection(textGp.text.toString().length)
        android.app.AlertDialog.Builder(this)
                .setTitle(R.string.create_new_group)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showLong(getString(R.string.rename_tip_check))
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
            R.id.add_device_btn -> {
                val lastUser = DBUtils.lastUser
                lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showLong(getString(R.string.author_region_warm))
                    else {
                        addDeviceLight()
                    }
                }
            }
        }
    }

    private fun addDeviceLight() {
        when (type) {
            Constant.INSTALL_NORMAL_LIGHT -> {
                intent = Intent(this, DeviceScanningNewActivity::class.java)
                intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, false)
                intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
                startActivityForResult(intent, 0)
            }
            Constant.INSTALL_RGB_LIGHT -> {
                intent = Intent(this, DeviceScanningNewActivity::class.java)
                intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_RGB)
                startActivityForResult(intent, 0)
            }
        }
    }

    private fun initData() {
        setScanningMode(true)
        if (type != null) {
            lightsData = ArrayList()
            directLight = null
            when (type) {
                Constant.INSTALL_NORMAL_LIGHT -> {//普通灯列表
                    allLightData = DBUtils.getAllNormalLight()
                    if (allLightData.size > 0) {
                        for (i in allLightData.indices) {
                            /*  if (TelinkLightApplication.getApp().connectDevice != null && allLightData[i].meshAddr
                                      == TelinkLightApplication.getApp().connectDevice?.meshAddress)
                                  directLight = allLightData[i]
                              else {*/
                            val groupName = StringUtils.getLightGroupName(allLightData[i])
                            if (groupName != getString(R.string.not_grouped))
                                allLightData[i].groupName = groupName
                            else
                                allLightData[i].groupName = ""
                            lightsData.add(allLightData[i])
                            //}
                        }
                        lightsData = sortList(lightsData)


                        var batchGroup = changeHaveDeviceView()
                        batchGroup?.setOnClickListener {
                            if (TelinkLightApplication.getApp().connectDevice != null) {
                                val lastUser = DBUtils.lastUser
                                lastUser?.let {
                                    if (it.id.toString() != it.last_authorizer_user_id)
                                        ToastUtils.showLong(getString(R.string.author_region_warm))
                                    else {
                                        if (dialog_device?.visibility == View.GONE) {
                                            val intent = Intent(this, BatchGroupFourDeviceActivity::class.java)
                                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
                                            startActivity(intent)
                                        }
                                    }
                                }
                            } else {
                                autoConnect()
                            }
                        }
                    } else {
                        changeNoDeviceView()
                    }
                }
                Constant.INSTALL_RGB_LIGHT -> {//全彩灯
                    allLightData = DBUtils.getAllRGBLight()
                    lightsData.clear()
                    if (allLightData.size > 0) {
                        for (i in allLightData.indices) {
                            /* if (TelinkLightApplication.getApp().connectDevice != null && allLightData[i].meshAddr
                                     == TelinkLightApplication.getApp().connectDevice?.meshAddress)
                                 directLight = allLightData[i]
                             else {*/
                            val groupName = StringUtils.getLightGroupName(allLightData[i])
                            if (groupName != getString(R.string.not_grouped))
                                allLightData[i].groupName = groupName
                            else
                                allLightData[i].groupName = ""
                            lightsData.add(allLightData[i])

                            lightsData = sortList(lightsData)
                            // }
                        }

                        var batchGroup = changeHaveDeviceView()
                        batchGroup?.setOnClickListener {
                            //val intent = Intent(this, RgbBatchGroupActivity::class.java)
                            val lastUser = DBUtils.lastUser
                            lastUser?.let {
                                if (it.id.toString() != it.last_authorizer_user_id)
                                    ToastUtils.showLong(getString(R.string.author_region_warm))
                                else {
                                    if (dialog_device?.visibility == View.GONE) {
                                        val intent = Intent(this, BatchGroupFourDeviceActivity::class.java)
                                        intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_RGB)
                                        startActivity(intent)
                                    }
                                }
                            }
                        }
                    } else {
                        changeNoDeviceView()
                    }
                }
                Constant.INSTALL_LIGHT_OF_CW -> {
                    var allLightData = DBUtils.getAllNormalLight()
                    if (allLightData.size > 0) {
                        var listGroup: ArrayList<DbLight> = ArrayList()
                        var noGroup: ArrayList<DbLight> = ArrayList()
                        for (i in allLightData.indices) {
                            if (StringUtils.getLightGroupName(allLightData[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
                                noGroup.add(allLightData[i])
                            } else {
                                listGroup.add(allLightData[i])
                            }
                        }

                        if (noGroup.size > 0) {
                            for (i in noGroup.indices) {
                                lightsData.add(noGroup[i])
                            }
                        }

                        if (listGroup.size > 0) {
                            for (i in listGroup.indices) {
                                lightsData.add(listGroup[i])
                            }
                        }
                        var cwLightGroup = this.intent.getStringExtra("cw_light_name")
                        var batchGroup = changeHaveDeviceView()
                        batchGroup?.setOnClickListener {
                            if (TelinkLightApplication.getApp().connectDevice != null) {
                                val lastUser = DBUtils.lastUser
                                lastUser?.let {
                                    if (it.id.toString() != it.last_authorizer_user_id)
                                        ToastUtils.showLong(getString(R.string.author_region_warm))
                                    else {
                                        val intent = Intent(this, BatchGroupFourDeviceActivity::class.java)
                                        intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
                                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                                        intent.putExtra("lightType", "cw_light")
                                        intent.putExtra("cw_light_group_name", cwLightGroup)
                                        startActivity(intent)
                                    }
                                }
                            } else {
                                autoConnect()
                            }
                        }


                    } else {
                        changeNoDeviceView()
                    }
                }
                Constant.INSTALL_LIGHT_OF_RGB -> {
                    var allLightData = DBUtils.getAllRGBLight()
                    if (allLightData.size > 0) {
                        var listGroup: ArrayList<DbLight> = ArrayList()
                        var noGroup: ArrayList<DbLight> = ArrayList()
                        for (i in allLightData.indices) {
                            if (StringUtils.getLightGroupName(allLightData[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
                                noGroup.add(allLightData[i])
                            } else {
                                listGroup.add(allLightData[i])
                            }
                        }

                        if (noGroup.size > 0) {
                            for (i in noGroup.indices) {
                                lightsData.add(noGroup[i])
                            }
                        }

                        if (listGroup.size > 0) {
                            for (i in listGroup.indices) {
                                lightsData.add(listGroup[i])
                            }
                        }
                        var rgbLightGroup = this.intent.getStringExtra("rgb_light_name")
                        LogUtils.e("zcl---rgb_light_name----$rgbLightGroup")

                        var batchGroup = changeHaveDeviceView()
                        batchGroup?.setOnClickListener {
                            /*val intent = Intent(this, RgbBatchGroupActivity::class.java)
                                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                                        intent.putExtra("lightType", "rgb_light")
                                        intent.putExtra("rgb_light_group_name", rgbLightGroup)*/
                            val intent = Intent(this, BatchGroupFourDeviceActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_RGB)
                            startActivity(intent)
                        }
                    } else {
                        changeNoDeviceView()
                    }
                }
            }
        }
    }


    private fun changeHaveDeviceView(): TextView? {
        toolbar!!.tv_function1.visibility = View.VISIBLE
        device_detail_have_ly.visibility = View.VISIBLE
        no_device_relativeLayout.visibility = View.GONE
        var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
        toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
        batchGroup.setText(R.string.batch_group)
        batchGroup.visibility = View.VISIBLE
        return batchGroup
    }

    private fun changeNoDeviceView() {
        device_detail_have_ly.visibility = View.GONE
        no_device_relativeLayout.visibility = View.VISIBLE
        toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
        toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
        toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
            val lastUser = DBUtils.lastUser
            lastUser?.let {
                if (it.id.toString() != it.last_authorizer_user_id)
                    ToastUtils.showLong(getString(R.string.author_region_warm))
                else {
                    if (dialog_device?.visibility == View.GONE) {
                        showPopupMenu()
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
        disposableTimer?.dispose()
        mConnectDisposable?.dispose()
        disableConnectionStatusListener()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        notifyData()
        GlobalScope.launch {
            //踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
            delay(2000)
            val b = this@DeviceDetailAct == null || this@DeviceDetailAct.isDestroyed || this@DeviceDetailAct.isFinishing || !acitivityIsAlive
            if (!b)
                autoConnect()
        }
    }

    fun notifyData() {
        if (lightsData.size > 0) {
            val mOldDatas: MutableList<DbLight>? = lightsData
            val mNewDatas: ArrayList<DbLight>? = getNewData()

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

    private fun getNewData(): ArrayList<DbLight> {
        when (type) {
            Constant.INSTALL_NORMAL_LIGHT -> lightsData = DBUtils.getAllNormalLight()
            Constant.INSTALL_RGB_LIGHT -> lightsData = DBUtils.getAllRGBLight()
        }
        return lightsData
    }

    override fun onStop() {
        super.onStop()
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
        mConnectDisposable?.dispose()
        getPositionAndOffset()
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
    fun autoConnect() {
        val deviceTypes = mutableListOf(DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB)
        val size = DBUtils.getAllCurtains().size + DBUtils.allLight.size + DBUtils.allRely.size
        if (size > 0) {
            ToastUtils.showLong(getString(R.string.connecting_tip))
            mConnectDisposable?.dispose()
            mConnectDisposable = connect(deviceTypes = deviceTypes, fastestMode = true, retryTimes = 10)
                    ?.subscribe({
                        onLogin()
                    }, {
                        LogUtils.d("connect failed")
                    }
                    )
        }
    }
    private fun onLogin() {
        LogUtils.d("connection success")
    }

    override fun afterLogin() {
        super.afterLogin()
        initData()
        initView()
    }

    override fun afterLoginOut() {
        super.afterLoginOut()
        initData()
        initView()
    }

    private fun sortList(arr: java.util.ArrayList<DbLight>): java.util.ArrayList<DbLight> {
        var min: Int
        var temp: DbLight
        for (i in arr.indices) {//包括结束区间
            min = i
            for (j in i + 1 until arr.size) {//until不 不包括结束区间
                val jLight = arr[j]
                val mLight = arr[min]
                if (jLight != null && mLight != null)
                    if (jLight.belongGroupId < mLight.belongGroupId) {
                        min = j
                    }
            }
            if (arr[i].belongGroupId > arr[min].belongGroupId) {
                temp = arr[i]
                arr[i] = arr[min]
                arr[min] = temp
            }
        }
        return arr
    }


    override fun receviedGwCmd2500(gwStompBean: GwStompBean) {
        when(gwStompBean.ser_id.toInt()){
            Constant.SER_ID_LIGHT_ON->{
                LogUtils.v("zcl-----------远程控制开灯成功-------")
                hideLoadingDialog()
                lightsData[positionCurrent].connectionStatus = ConnectionStatus.ON.value
                lightsData[positionCurrent].updateIcon()
                adaper?.notifyDataSetChanged()
            }
            Constant.SER_ID_LIGHT_OFF->{
                LogUtils.v("zcl-----------远程控制关灯成功-------")
                hideLoadingDialog()
                lightsData[positionCurrent].connectionStatus = ConnectionStatus.OFF.value
                lightsData[positionCurrent].updateIcon()
                adaper?.notifyDataSetChanged()
            }
        }
    }

}
