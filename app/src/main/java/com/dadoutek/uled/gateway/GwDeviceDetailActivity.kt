package com.dadoutek.uled.gateway

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.*
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.adapter.GwDeviceItemAdapter
import com.dadoutek.uled.gateway.bean.ClearGwBean
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.Constant.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.HttpModel.GwModel
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.LightAdapter
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.MeshUtils.DEVICE_ADDRESS_MAX
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.empty_view.*
import kotlinx.android.synthetic.main.template_device_detail_list.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * 创建者     zcl
 * 创建时间   2020/3/13 16:27
 * 描述	     //从parmers内设置需要的类型从此处取出
 * {  List<Integer> targetDevices = mParams.getIntList(Parameters.PARAM_TARGET_DEVICE_TYPE);
 *如果有要连接的目标设备
 *if (targetDevices.size() > 0)
 *if (!targetDevices.contains(light.getProductUUID())) {    //如果目标设备list里不包含当前设备类型，就过滤掉，return false
 *return false;
 *
 *
 *}}
 *
 * 更新者    $author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}$
 */
class GwDeviceDetailActivity : TelinkBaseActivity(), View.OnClickListener, EventListener<String> {
    private var showDialogDeleteScond: AlertDialog? = null
    private var showDialogHardDelete: AlertDialog? = null
    private var showDialogDelete: AlertDialog? = null
    private var disposableFactoryTimer: Disposable? = null
    private var downloadDispoable: Disposable? = null
    private var removeBean: DbGateway? = null
    private var renameDialog: Dialog? = null
    private var renameConfirm: TextView? = null
    private var renameCancel: TextView? = null
    private var renameEditText: EditText? = null
    private var popReNameView: View? = null
    private var disposableTimer: Disposable? = null
    private var isRestSuccess: Boolean = false
    private lateinit var mApp: TelinkLightApplication
    private lateinit var popupWindow: PopupWindow
    private var disposableConnect: Disposable? = null
    private var disposable: Disposable? = null
    private var currentGw: DbGateway? = null
    private var type: Int? = null
    private val gateWayDataList: MutableList<DbGateway> = mutableListOf()
    private var adaper: GwDeviceItemAdapter? = GwDeviceItemAdapter(R.layout.device_detail_adapter, gateWayDataList, this)
    private var inflater: LayoutInflater? = null
    private var positionCurrent: Int = 0
    private var canBeRefresh = true
    private var acitivityIsAlive = true
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
    private lateinit var stepThreeTextSmall: TextView
    private val SCENE_MAX_COUNT = 100

    override fun onCreate(savedInstanceState: Bundle?) {//其他界面添加扫描网关待做
        super.onCreate(savedInstanceState)
        setContentView(R.layout.template_device_detail_list)
        type = this.intent.getIntExtra(DEVICE_TYPE, 0)
        inflater = this.layoutInflater

        initView()
        initData()

    }

    private fun initView() {
        makePop()
        disableConnectionStatusListener()
        this.mApp = this.application as TelinkLightApplication
        tv_function1.visibility = View.GONE
        recycleView!!.layoutManager = GridLayoutManager(this, 3)
        recycleView!!.itemAnimator = DefaultItemAnimator()
        adaper!!.onItemChildClickListener = onItemChildClickListener
        adaper!!.bindToRecyclerView(recycleView)
        makePopupWindow()
        for (i in gateWayDataList.indices)
            gateWayDataList!![i].updateIcon()

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
        toolbarTv.text = getString(R.string.Gate_way) + " (" + gateWayDataList.size + ")"
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
                    popMain.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
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
                        intent.putExtra(IS_CHANGE_SCENE, false)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun showInstallDeviceList() {
        dialog_relay.visibility = View.GONE
        showInstallDeviceList(isGuide, isRgbClick)
    }

    override fun onResume() {
        super.onResume()
        inflater = this.layoutInflater
        initData()
        this.mApp.removeEventListeners()
        this.mApp.addEventListener(DeviceEvent.STATUS_CHANGED, this)
    }

    override fun onPause() {
        super.onPause()
        disposableAll()
        this.mApp.removeEventListener(DeviceEvent.STATUS_CHANGED, this)
    }

    var installDialog: android.app.AlertDialog? = null
    var isGuide: Boolean = false
    var clickRgb: Boolean = false
    private fun showInstallDeviceList(isGuide: Boolean, clickRgb: Boolean) {
        this.clickRgb = clickRgb
        val view = View.inflate(this, R.layout.dialog_install_list, null)
        val closeInstallList = view.findViewById<ImageView>(R.id.close_install_list)
        val installDeviceRecyclerview = view.findViewById<RecyclerView>(R.id.install_device_recyclerView)
        closeInstallList.setOnClickListener { v -> installDialog?.dismiss() }

        val installList: ArrayList<InstallDeviceModel> = OtherUtils.getInstallDeviceList(this)

        val installDeviceListAdapter = InstallDeviceListAdapter(R.layout.item_install_device, installList)
        val layoutManager = LinearLayoutManager(this)
        installDeviceRecyclerview?.layoutManager = layoutManager
        installDeviceRecyclerview?.adapter = installDeviceListAdapter
        installDeviceListAdapter.bindToRecyclerView(installDeviceRecyclerview)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
        //添加分割线
        installDeviceRecyclerview?.addItemDecoration(decoration)
        installDeviceListAdapter.onItemClickListener = onItemClickListenerInstallList

        installDialog = android.app.AlertDialog.Builder(this).setView(view).create()

        if (isGuide)
            installDialog?.setCancelable(false)

        installDialog?.show()
    }

    private val onItemClickListenerInstallList = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
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
        val closeInstallList = view.findViewById<ImageView>(R.id.close_install_list)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        stepOneText = view.findViewById(R.id.step_one)
        stepTwoText = view.findViewById(R.id.step_two)
        stepThreeText = view.findViewById(R.id.step_three)
        stepThreeTextSmall = view.findViewById(R.id.step_three_small)
        switchStepOne = view.findViewById(R.id.switch_step_one)
        switchStepTwo = view.findViewById(R.id.switch_step_two)
        swicthStepThree = view.findViewById(R.id.switch_step_three)
        val title = view.findViewById<TextView>(R.id.textView5)
        val installTipQuestion = view.findViewById<TextView>(R.id.install_tip_question)
        val searchBar = view.findViewById<Button>(R.id.search_bar)
        closeInstallList.setOnClickListener(dialogOnclick)
        btnBack.setOnClickListener(dialogOnclick)
        searchBar.setOnClickListener(dialogOnclick)

        if (position == INSTALL_NORMAL_LIGHT) {
            title.visibility = View.GONE
            installTipQuestion.visibility = View.GONE
        } else {
            title.visibility = View.VISIBLE
            installTipQuestion.visibility = View.VISIBLE
        }
        if (position == INSTALL_SWITCH)
            stepThreeTextSmall.visibility = View.VISIBLE
        else
            stepThreeTextSmall.visibility = View.GONE



        installTipQuestion.text = describe
        installTipQuestion.movementMethod = ScrollingMovementMethod.getInstance()
        installDialog = android.app.AlertDialog.Builder(this)
                .setView(view)
                .create()
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
                            intent.putExtra(DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_RGB_LIGHT -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(DEVICE_TYPE, DeviceType.LIGHT_RGB)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_CURTAIN -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(DEVICE_TYPE, DeviceType.SMART_CURTAIN)
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
                            intent.putExtra(DEVICE_TYPE, DeviceType.SMART_RELAY)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_GATEWAY -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(DEVICE_TYPE, DeviceType.GATE_WAY)
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
            R.id.add_device_btn -> {
                val lastUser = DBUtils.lastUser
                lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showLong(getString(R.string.author_region_warm))
                    else {
                        addDevice()
                    }
                }
            }
        }
    }

    private fun addDevice() {//添加网关
        intent = Intent(this, DeviceScanningNewActivity::class.java)
        intent.putExtra(DEVICE_TYPE, DeviceType.GATE_WAY)
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { _, view, position ->
        currentGw = gateWayDataList[position]
        positionCurrent = position
        if (view.id == R.id.tv_setting) {
            val lastUser = DBUtils.lastUser
            lastUser?.let {
                if (it.id.toString() != it.last_authorizer_user_id)
                    ToastUtils.showLong(getString(R.string.author_region_warm))
                else {
                    val set = view!!.findViewById<ImageView>(R.id.tv_setting)
                    popupWindow.dismiss()
                    popupWindow.showAsDropDown(set)
                    LogUtils.v("zcl-----------获取广播mac-------${currentGw?.macAddr}")
                }
            }
        } else if (view.id == R.id.img_light) {
            //开关网关通过普通灯的连接状态发送
            if (TelinkLightApplication.getApp().connectDevice != null)
                sendOpenOrCloseGw(true)
            else
                getGw()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendOpenOrCloseGw(isBleConnect: Boolean) {
        //第是一位0x01代表开 默认开 0x00代表关
        currentGw?.let { it ->
            if (isBleConnect) {
                var labHeadPar: ByteArray = if (it.openTag == 1)//如果是开就执行关闭
                    byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
                else
                    byteArrayOf(0x01, 0, 0, 0, 0, 0, 0, 0)
                TelinkLightService.Instance().sendCommandResponse(Opcode.CONFIG_GW_SWITCH, it.meshAddr, labHeadPar, "1")
                gateWayDataList.forEach { it1 ->
                    if (it1.id == it.id) {
                        it1.openTag = if (it.openTag == 0) {
                            it.icon = R.drawable.icon_gw_open
                            1
                        } else {
                            it.icon = R.drawable.icon_gw_open
                            0
                        }
                    }
                }

                adaper!!.notifyDataSetChanged()
            } else {
                disposableTimer?.dispose()
                disposableTimer = Observable.timer(6500, TimeUnit.MILLISECONDS).subscribe {
                }

                var labHeadPar: ByteArray = if (it.openTag == 1)//如果是开就执行关闭
                    byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0, 0, Opcode.CONFIG_GW_SWITCH, 0x11, 0x02, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                else
                    byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0, 0, Opcode.CONFIG_GW_SWITCH, 0x11, 0x02, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

                LogUtils.v("zcl-----------发送到服务器开关网关-------$labHeadPar")
                val encoder = Base64.getEncoder()
                val s = encoder.encodeToString(labHeadPar)
                val gattBody = GwGattBody()
                gattBody.data = s
                gattBody.ser_id = GW_GATT_SWITCH
                gattBody.macAddr = currentGw?.macAddr
                sendToServer(gattBody)
            }
        }
    }

    private fun sendToServer(gattBody: GwGattBody) {
        GwModel.sendToGatt(gattBody)?.subscribe(object : NetworkObserver<String?>() {
            override fun onNext(t: String) {}
            override fun onError(e: Throwable) {
                super.onError(e)
            }
        })
    }

    private fun getGw() {
        TelinkLightApplication.getApp().offLine = false
        GwModel.getGwList()?.subscribe(object : NetworkObserver<List<DbGateway>?>() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onNext(t: List<DbGateway>) {
                hideLoadingDialog()
                t.forEach { db ->
                    if (db.meshAddr == currentGw!!.meshAddr) {//网关在线状态，1表示在线，0表示离线
                        val b = db.state == 0
                        TelinkLightApplication.getApp().offLine = b
                        if (!TelinkLightApplication.getApp().offLine) {
                            sendOpenOrCloseGw(false)
                        } else {
                            ToastUtils.showShort(getString(R.string.gw_not_online))
                        }
                    }
                }
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                hideLoadingDialog()
                ToastUtils.showShort(getString(R.string.gw_not_online))
            }
        })
    }

    private fun makePopupWindow() {
        val views = LayoutInflater.from(this).inflate(R.layout.popwindown_switch, null)
        popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow.contentView = views
        popupWindow.isFocusable = true
        val reConfig = views.findViewById<TextView>(R.id.switch_group)
        val ota = views.findViewById<TextView>(R.id.ota)
        val restFactory = views.findViewById<TextView>(R.id.deleteBtn)
        val rename = views.findViewById<TextView>(R.id.rename)
        val configGwNet = views.findViewById<TextView>(R.id.configGwNet)
        val deleteBtnNoFactory = views.findViewById<TextView>(R.id.deleteBtnNoFactory)

        reConfig.text = getString(R.string.config_gate_way)
        restFactory.text = getString(R.string.delete_device)
        deleteBtnNoFactory.text = getString(R.string.user_reset)
        configGwNet.visibility = View.VISIBLE
        deleteBtnNoFactory.visibility = View.VISIBLE

        deleteBtnNoFactory.setOnClickListener {
            popupWindow.dismiss()//删除网关

            showDialogDelete = AlertDialog.Builder(this).setTitle(getString(R.string.user_reset)).setMessage(R.string.user_reset_tip)
                    .setPositiveButton(android.R.string.ok) { _, _ ->

                        disposableTimer?.dispose()
                        disposableTimer = Observable.timer(5000, TimeUnit.MILLISECONDS)
                                .subscribe {
                                    showDialogDeleteScond = AlertDialog.Builder(this).setTitle(getString(R.string.user_reset)).setMessage(R.string.user_reset_tip)
                                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                                resetUserGwData()
                                            }
                                            .setNegativeButton(R.string.btn_cancel, null)
                                            .show()
                                }

                        var labHeadPar = byteArrayOf(0x01, 0, 0, 0, 0, 0, 0, 0)
                        TelinkLightService.Instance().sendCommandResponse(Opcode.CONFIG_GW_REST_FACTORY, currentGw?.meshAddr
                                ?: 0, labHeadPar, "1")
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
        }

        configGwNet.setOnClickListener {
            connectGw(1)//配置网络
        }

        reConfig.setOnClickListener {
            connectGw(0)//重新配置
        }
        rename.visibility = View.GONE
        rename.setOnClickListener {
            showRenameDialog()
        }

        ota.setOnClickListener {
            connectGw(2)//ota
        }
        restFactory.setOnClickListener {
            //恢复出厂设置
            showDialogDelete = AlertDialog.Builder(this).setMessage(R.string.delete_device_tip)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        connectGw(3)
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
        }
    }

    private fun resetUserGwData() {
        GwModel.clearGwData(currentGw!!.id)?.subscribe(object : NetworkObserver<ClearGwBean?>() {
            override fun onNext(t: ClearGwBean) {
                saveResetGwData(t)
                ToastUtils.showShort(getString(R.string.gw_user_reset_switch_success))
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                ToastUtils.showShort(e.message)
            }
        })
    }

    private fun saveResetGwData(t: ClearGwBean) {
        currentGw?.belongRegionId = t.belongRegionId
        currentGw?.id = t.id.toLong()
        currentGw?.macAddr = t.macAddr
        currentGw?.meshAddr = t.meshAddr
        currentGw?.name = t.name
        currentGw?.productUUID = t.productUUID
        currentGw?.state = t.state
        currentGw?.tags = t.tags.toString()
        currentGw?.type = t.type
        currentGw?.uid = t.uid
        currentGw?.version = t.version
        currentGw?.openTag = t.openTag
        DBUtils.saveGateWay(currentGw!!, false)
    }

    @SuppressLint("CheckResult")
    private fun getDeviceVersion(meshAddr: Int) {
        if (TelinkApplication.getInstance().connectDevice != null) {
            downloadDispoable = Commander.getDeviceVersion(meshAddr)
                    .subscribe(
                            { s: String ->
                                currentGw!!.version = s
                                DBUtils.saveGateWay(currentGw!!, true)
                                var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), IS_DEVELOPER_MODE, false)
                                if (isBoolean) {
                                    transformView()
                                } else {
                                    if (OtaPrepareUtils.instance().checkSupportOta(s)!!)
                                        OtaPrepareUtils.instance().gotoUpdateView(this@GwDeviceDetailActivity, s, otaPrepareListner)
                                    else
                                        ToastUtils.showShort(getString(R.string.version_disabled))
                                }
                                hideLoadingDialog()
                            }, {
                        hideLoadingDialog()
                        ToastUtils.showLong(getString(R.string.get_version_fail))
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
        disableConnectionStatusListener()

        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == currentGw?.meshAddr) {
            val intent = Intent(this@GwDeviceDetailActivity, OTAUpdateActivity::class.java)
            intent.putExtra(UPDATE_LIGHT, currentGw)
            intent.putExtra(OTA_MES_Add, currentGw?.meshAddr ?: 0)
            intent.putExtra(OTA_MAC, currentGw?.macAddr)
            intent.putExtra(OTA_VERSION, currentGw?.version)
            intent.putExtra(OTA_TYPE, DeviceType.GATE_WAY)

            startActivity(intent)
            finish()
        }
    }


    @SuppressLint("SetTextI18n")
    private fun showRenameDialog() {
        popupWindow.dismiss()
        StringUtils.initEditTextFilter(renameEditText)
        if (currentGw != null && currentGw?.name != "")
            renameEditText?.setText(currentGw?.name)

        renameEditText?.setSelection(renameEditText?.text.toString().length)
        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }
    }

    private fun connectGw(configType: Int) {
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(15,TimeUnit.SECONDS)
                .subscribe {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.connect_fail))
                }
        popupWindow.dismiss()
        if (currentGw != null) {
            TelinkLightService.Instance()?.idleMode(true)
            showLoadingDialog(getString(R.string.connecting))
            disposableConnect?.dispose()
            disposableConnect = connect(macAddress = currentGw?.macAddr, connectTimeOutTime = 15L)?.subscribe({
                disposable?.dispose()
                disposableTimer?.dispose()
                TelinkLightApplication.getApp().isConnectGwBle = true
                when (configType) {
                    0 -> onLogin(configType)
                    1 -> onLogin(configType)
                    2 -> getDeviceVersion(currentGw!!.meshAddr)
                    3 -> sendGwResetFactory()
                }//判断进入那个开关设置界面
            }, {
                hideLoadingDialog()
                if (configType == 0) {//重新配置为0
                    connectService()
                } else {
                    ToastUtils.showShort(getString(R.string.connect_fail))
                }
            })
        } else {
            LogUtils.d("currentGw = $currentGw")
        }
    }

    private fun connectService() {
        disposable?.dispose()
        showLoadingDialog(getString(R.string.please_wait))
        TelinkLightApplication.getApp().offLine = false
        GwModel.getGwList()?.subscribe(object : NetworkObserver<List<DbGateway>?>() {
            override fun onNext(t: List<DbGateway>) {
                hideLoadingDialog()
                t.forEach { db ->
                    if (db.meshAddr == currentGw!!.meshAddr) {//网关在线状态，1表示在线，0表示离线
                        val b = db.state == 0
                        TelinkLightApplication.getApp().offLine = b
                    }
                }
                if (!TelinkLightApplication.getApp().offLine) {
                    TelinkLightApplication.getApp().isConnectGwBle = false
                    val intent = Intent(this@GwDeviceDetailActivity, GwEventListActivity::class.java)
                    intent.putExtra("data", currentGw)
                    startActivity(intent)
                    finish()
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

    private fun sendGwResetFactory() {
        disposableFactoryTimer = Observable.timer(15000, TimeUnit.MILLISECONDS).subscribe {
            showDialogDelete?.dismiss()
            showDialogHardDelete = AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_device_hard_tip)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        showDialogHardDelete?.dismiss()
                        isRestSuccess = true
                        deleteGwData(isRestSuccess)
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
        }
        var labHeadPar = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        TelinkLightService.Instance().sendCommandResponse(Opcode.CONFIG_GW_REST_FACTORY, currentGw?.meshAddr ?: 0, labHeadPar, "1")
    }

    override fun performed(event: Event<String>?) {
        if (event is DeviceEvent) {
            this.onDeviceEvent(event)
        }
    }

    private fun onDeviceEvent(event: DeviceEvent) {
        when (event.type) {
            DeviceEvent.STATUS_CHANGED -> {
                when (event.args.status) {
                    LightAdapter.STATUS_SET_GW_COMPLETED -> {//Dadou   Dadoutek2018
                        val deviceInfo = event.args
                        if (deviceInfo.gwVoipState == GW_RESET_VOIP) {
                            showDialogHardDelete?.dismiss()
                            LogUtils.v("zcl-----------获取网关相关返回信息-------$deviceInfo")
                            isRestSuccess = true
                            deleteGwData(isRestSuccess)
                        } else if (deviceInfo.gwVoipState == GW_RESET_USER_VOIP) {
                            resetUserGwData()
                        }
                    }
                }
            }
        }
    }

    private fun deleteGwData(restSuccess: Boolean = false) {
        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(currentGw!!.meshAddr))
            TelinkLightApplication.getApp().mesh.saveOrUpdate(this)

        DBUtils.deleteGateway(currentGw!!)
        val gattBody = GwGattBody()
        gattBody.idList = mutableListOf(currentGw!!.id.toInt())

        GwModel.deleteGwList(gattBody)?.subscribe(object : NetworkObserver<String?>() {
            override fun onNext(t: String) {
                LogUtils.v("zcl-----网关删除成功返回-------------$t")
                GlobalScope.launch(Dispatchers.Main) {
                    if (restSuccess)
                        delay(10000)
                    notifyData()
                    Toast.makeText(this@GwDeviceDetailActivity, R.string.delete_switch_success, Toast.LENGTH_LONG).show()
                    hideLoadingDialog()
                }
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                ToastUtils.showShort(e.message)
                hideLoadingDialog()
                //清除网关的时候。已经从数据库删除了数据，此处也需要从数据库重新拿数据，更新到UI
                notifyData()
                LogUtils.v("zcl-----网关删除成功返回-------------${e.message}")
            }
        })
    }

    fun notifyData() {
        val mNewDatas: ArrayList<DbGateway> = getNewData()
        gateWayDataList.clear()
        gateWayDataList.addAll(mNewDatas)
        if (mNewDatas.size > 0) {
            recycleView.visibility = View.VISIBLE
            no_device_relativeLayout.visibility = View.GONE
        } else {
            recycleView.visibility = View.GONE
            no_device_relativeLayout.visibility = View.VISIBLE
        }
        adaper?.notifyDataSetChanged()
        toolbarTv.text = getString(R.string.Gate_way) + " (" + gateWayDataList.size + ")"
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return gateWayDataList[oldItemPosition].id?.equals(mNewDatas[newItemPosition].id)
                        ?: false
            }

            override fun getOldListSize(): Int {
                return gateWayDataList.size
            }

            override fun getNewListSize(): Int {
                return mNewDatas.size
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val beanOld = gateWayDataList[oldItemPosition]
                val beanNew = mNewDatas[newItemPosition]
                return if (beanOld.name != beanNew.name) {
                    return false//如果有内容不同，就返回false
                } else true
            }
        }, true)

    }

    private fun getNewData(): ArrayList<DbGateway> {
        val allGateWay = DBUtils.getAllGateWay()
        toolbar.title = (currentGw!!.name ?: "")
        return allGateWay
    }

    private fun onLogin(isConfigGw: Int) {
        disposable?.dispose()
        hideLoadingDialog()
        var intent: Intent? = null
        if (isConfigGw == 0) {//重新配置
            intent = Intent(this@GwDeviceDetailActivity, GwEventListActivity::class.java)
        }
        if (isConfigGw == 1) {//配置wifi
            intent = Intent(this@GwDeviceDetailActivity, GwLoginActivity::class.java)
            SharedPreferencesHelper.putBoolean(this, IS_GW_CONFIG_WIFI, true)
        }
        if (intent != null) {
            intent!!.putExtra("data", currentGw)
            startActivity(intent)
            finish()
        }
    }

    private fun initData() {
        setScanningMode(true)
        gateWayDataList.clear()
        val allDeviceData = DBUtils.getAllGateWay()
        if (allDeviceData.size > 0) {
            toolbar!!.tv_function1.visibility = View.VISIBLE
            recycleView.visibility = View.VISIBLE
            no_device_relativeLayout.visibility = View.GONE
            toolbar.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE

//            toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                val lastUser = DBUtils.lastUser
                lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showLong(getString(R.string.author_region_warm))
                    else {
                        if (dialog_relay?.visibility == View.GONE) {
                            showPopupMenu()
                        } else {
                            hidePopupMenu()
                        }
                    }
                }
            }
            gateWayDataList.addAll(allDeviceData)
            adaper?.notifyDataSetChanged()
        } else {
            recycleView.visibility = View.GONE
            no_device_relativeLayout.visibility = View.VISIBLE
            toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                val lastUser = DBUtils.lastUser
                lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showLong(getString(R.string.author_region_warm))
                    else {
                        if (dialog_relay?.visibility == View.GONE) {
                            showPopupMenu()
                        } else {
                            hidePopupMenu()
                        }
                    }
                }
            }
        }
        toolbarTv.text = getString(R.string.Gate_way)/* + " (" + gateWayDataList.size + ")"*/
    }


    private fun disposableAll() {
        mConnectDisposal?.dispose()
        disposableConnectTimer?.dispose()
        disposable?.dispose()
        disposableTimer?.dispose()
    }

    private fun showPopupMenu() {
        dialog_relay?.visibility = View.VISIBLE
    }

    private fun hidePopupMenu() {
        dialog_relay?.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableAll()
        showDialogDelete?.dismiss()
        showDialogHardDelete?.dismiss()
        this.mApp.removeEventListeners()
        canBeRefresh = false
        acitivityIsAlive = false
        installDialog?.dismiss()
        popupWindow.dismiss()
    }

    override fun receviedGwCmd2000(ser_id: String) {

        if (GW_GATT_SWITCH == ser_id?.toInt()) {
            currentGw?.openTag = if (currentGw?.openTag == 0) 1 else 0
            currentGw?.icon = if (currentGw?.openTag == 0) R.drawable.icon_gw_close else R.drawable.icon_gw_open
            DBUtils.saveGateWay(currentGw!!, false)
            updataUi()
            disposableTimer?.dispose()
        }
    }

    private fun makePop() {
        popReNameView = View.inflate(this, R.layout.pop_rename, null)
        renameEditText = popReNameView?.findViewById(R.id.pop_rename_edt)
        renameCancel = popReNameView?.findViewById(R.id.pop_rename_cancel)
        renameConfirm = popReNameView?.findViewById(R.id.pop_rename_confirm)
        renameConfirm?.setOnClickListener {
            // 获取输入框的内容
            if (StringUtils.compileExChar(renameEditText?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                currentGw?.name = renameEditText?.text.toString().trim { it <= ' ' }
                if (currentGw == null)
                    currentGw = DBUtils.getGatewayByMeshAddr(currentGw?.meshAddr ?: 0)
                if (currentGw != null)
                    DBUtils.saveGateWay(currentGw!!, true)
                else
                    ToastUtils.showLong(getString(R.string.rename_faile))

                if (this != null && !this.isFinishing)
                    renameDialog?.dismiss()
                LogUtils.v("zcl改名后-----------${DBUtils.getSwitchByMeshAddr(currentGw?.meshAddr ?: 0)?.name}")
            }
        }
        renameCancel?.setOnClickListener {
            if (this != null && !this.isFinishing)
                renameDialog?.dismiss()
        }

        renameDialog = Dialog(this)
        renameDialog!!.setContentView(popReNameView)
        renameDialog!!.setCanceledOnTouchOutside(false)

        renameDialog?.setOnDismissListener {
            currentGw?.name = renameEditText?.text.toString().trim { it <= ' ' }
            if (currentGw != null)
                DBUtils.saveGateWay(currentGw!!, true)
            updataUi()
        }
    }

    private fun updataUi() {
        for (gw in gateWayDataList) {
            if (gw.id == currentGw?.id) {
                removeBean = gw
                break
            }
        }
        gateWayDataList.remove(removeBean)
        gateWayDataList.add(0, currentGw!!)
        adaper?.notifyDataSetChanged()
    }

    fun myPopViewClickPosition(x: Float, y: Float) {
        if (x < dialog_relay?.left ?: 0 || y < dialog_relay?.top ?: 0 || y > dialog_relay?.bottom ?: 0) {
            if (dialog_relay?.visibility == View.VISIBLE) {
                Thread {
                    //避免点击过快点击到下层View
                    Thread.sleep(100)
                    GlobalScope.launch(Dispatchers.Main) {
                        hidePopupMenu()
                    }
                }.start()
            } else if (dialog_relay == null) {
                hidePopupMenu()
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        try {
            if (ev?.action == MotionEvent.ACTION_DOWN) {
                myPopViewClickPosition(ev.x, ev.y)
            }
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        return super.dispatchTouchEvent(ev)

    }

}
