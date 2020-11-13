package com.dadoutek.uled.pir

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.text.InputType
import android.text.TextUtils
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.PopupWindow
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.NightLightEditGroupAdapter
import com.dadoutek.uled.light.NightLightGroupRecycleViewAdapter
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbSensor
import com.dadoutek.uled.network.ConfigurationBean
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.othersview.SelectDeviceTypeActivity
import com.dadoutek.uled.router.RouterOtaActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.MeshAddressGenerator
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
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
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.huuman_body_sensor.*
import kotlinx.android.synthetic.main.template_loading_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.singleLine
import org.jetbrains.anko.startActivity
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 创建者     zcl
 * 创建时间   2019/8/29 16:03
 * 描述	      ${人体感应器版本设置界面}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${}$
 */
class HumanBodySensorActivity : TelinkBaseActivity(), View.OnClickListener, EventListener<String> {
    private var currentSensor: DbSensor? = null
    private var disposableReset: Disposable? = null
    private var version: String = ""
    private var disposable: Disposable? = null
    private var isReConfirm: Boolean = false
    private lateinit var mDeviceInfo: DeviceInfo
    private val CMD_OPEN_LIGHT = 0X01
    private val CMD_CLOSE_LIGHT = 0X00
    private val CMD_CONTROL_GROUP = 0X02
    private var switchMode = 0X01
    private var selectTime = 10
    private var fiRename: MenuItem? = null
    private var fiVersion: MenuItem? = null
    private var renameEditText: EditText? = null

    //底部组适配器
    private var showGroupList: MutableList<ItemGroup>? = null
    private var nightLightGroupGrideAdapter: NightLightGroupRecycleViewAdapter? = null

    //显示选择分组下拉的数据 选择组适配器
    private var showCheckListData: MutableList<DbGroup> = mutableListOf()

    //private var nightLightEditGroupAdapter: NightLightEditGroupAdapter = NightLightEditGroupAdapter(R.layout.select_more_item, showCheckListData)
    private var nightLightEditGroupAdapter: NightLightEditGroupAdapter = NightLightEditGroupAdapter(R.layout.template_batch_small_item, showCheckListData)
    private var modeStartUpMode = 0
    private var modeDelayUnit = 2
    private var modeSwitchMode = 0
    private var modeTriggerCondition = 0
    private val MODE_START_UP_MODE_OPEN = 0
    private val MODE_DELAY_UNIT_SECONDS = 0
    private val MODE_SWITCH_MODE_MOMENT = 0
    private val MODE_START_UP_MODE_CLOSE = 1
    private val MODE_DELAY_UNIT_MINUTE = 2
    private val MODE_SWITCH_MODE_GRADIENT = 4
    private var retryConnectCount = 0
    private var isFinish: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtils.v("zcl直连灯地址${TelinkLightApplication.getApp().connectDevice?.meshAddress}")
        setContentView(R.layout.huuman_body_sensor)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        initToolbar()
        initView()
        makePopuwindow()
        initData()
        initListener()
    }

    private fun makePopuwindow() {
        popReNameView = View.inflate(this, R.layout.pop_rename, null)
        renameEditText = popReNameView?.findViewById(R.id.pop_rename_edt)
        renameCancel = popReNameView?.findViewById(R.id.pop_rename_cancel)
        renameConfirm = popReNameView?.findViewById(R.id.pop_rename_confirm)

        renameDialog = Dialog(this)
        renameDialog!!.setContentView(popReNameView)
        renameDialog!!.setCanceledOnTouchOutside(false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        DBUtils.lastUser?.let {
            if (it.id.toString() == it.last_authorizer_user_id) {
                menuInflater.inflate(R.menu.menu_rgb_light_setting, menu)
                fiRename = menu?.findItem(R.id.toolbar_f_rename)
                fiRename?.isVisible = isReConfirm
                fiVersion = menu?.findItem(R.id.toolbar_f_version)
                if (TextUtils.isEmpty(version))
                    version = getString(R.string.number_no)
                fiVersion?.title = version
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    private val menuItemClickListener = Toolbar.OnMenuItemClickListener { item ->
        when (item?.itemId) {
            R.id.toolbar_f_rename -> showRenameDialog(currentSensor!!)
            R.id.toolbar_f_ota -> goOta()
            R.id.toolbar_f_delete -> deleteDevice()
        }
        true
    }

    private fun deleteDevice() {
        //mesadddr发0就是代表只发送给直连灯也就是当前连接灯 也可以使用当前灯的mesAdd 如果使用mesadd 有几个pir就恢复几个
        val deleteSwitchConfirm = getString(R.string.delete_switch_confirm)
        AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(deleteSwitchConfirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (currentSensor == null)
                        ToastUtils.showShort(getString(R.string.invalid_data))
                    else {
                        if (Constant.IS_ROUTE_MODE)
                            routerDeviceResetFactory(currentSensor!!.macAddr, currentSensor!!.meshAddr, 98, "deleteHum")
                        else {
                            showLoadingDialog(getString(R.string.please_wait))
                            disposableReset = Commander.resetDevice(currentSensor!!.meshAddr, true)
                                    .subscribe({
                                        //  deleteData()
                                    }, {
                                        GlobalScope.launch(Dispatchers.Main) {/*showDialogHardDelete?.dismiss()
                                    showDialogHardDelete = android.app.AlertDialog.Builder(this@HumanBodySensorActivity).setMessage(R.string.delete_device_hard_tip)
                                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                                showLoadingDialog(getString(R.string.please_wait))
                                                deleteData()
                                            }
                                            .setNegativeButton(R.string.btn_cancel, null)
                                            .show()*/
                                        }
                                    })
                            deleteData()
                        }
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    override fun tzRouteResetFactoryBySelf(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到deleteHum路由通知-------$cmdBean")
        if (cmdBean.ser_id == "deleteHum") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            if (cmdBean.status == 0) {
                deleteData()
            } else {
                ToastUtils.showShort(getString(R.string.reset_factory_fail))
            }
        }
    }

    fun deleteData() {
        hideLoadingDialog()
        ToastUtils.showShort(getString(R.string.reset_factory_success))
        DBUtils.deleteSensor(currentSensor!!)
        TelinkLightService.Instance()?.idleMode(true)
        doFinish()
    }

    private fun goOta() {
        var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_DEVELOPER_MODE, false)
        if (isBoolean|| Constant.IS_ROUTE_MODE) {
            transformView()
        } else {
            if (OtaPrepareUtils.instance().checkSupportOta(version)!!) {
                OtaPrepareUtils.instance().gotoUpdateView(this@HumanBodySensorActivity, version, object : OtaPrepareListner {
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
                })
            } else {
                ToastUtils.showLong(getString(R.string.version_disabled))
                hideLoadingDialog()
            }
        }

    }

    private fun transformView() {
        when {
            !isSuportOta(currentSensor?.version) -> ToastUtils.showShort(getString(R.string.dissupport_ota))
            isMostNew(currentSensor?.version) -> ToastUtils.showShort(getString(R.string.the_last_version))
            else -> {
        if (Constant.IS_ROUTE_MODE){
            startActivity<RouterOtaActivity>("deviceMeshAddress" to currentSensor!!.meshAddr,
                    "deviceType" to currentSensor!!.productUUID, "deviceMac" to currentSensor!!.macAddr,
                    "version" to currentSensor!!.version )
        finish()}
        else {
            val intent = Intent(this@HumanBodySensorActivity, OTAUpdateActivity::class.java)
            intent.putExtra(Constant.OTA_MAC, currentSensor?.macAddr)
            intent.putExtra(Constant.OTA_MES_Add, currentSensor?.meshAddr)
            intent.putExtra(Constant.OTA_VERSION, currentSensor?.version)
            intent.putExtra(Constant.OTA_TYPE, DeviceType.SENSOR)
            startActivity(intent)
            finish()
        }
    }}}

    private fun setAdapters() {
        scene_gp_bottom_list.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        scene_gp_bottom_list.adapter = nightLightEditGroupAdapter
        nightLightEditGroupAdapter.bindToRecyclerView(scene_gp_bottom_list)

        nightLightEditGroupAdapter.setOnItemClickListener { adapter, view, position ->
            val item = showCheckListData[position]
            when {
                item.checked -> {//t状态
                    item.checked = !item.checked
                }
                else -> {//f状态下
                    if (position == 0 && item.meshAddr == 0xffff) {//65535
                        setFrist()
                    } else {
                        item.checked = true
                        if (position != 0)
                            showCheckListData[0].checked = false
                        else
                            setFrist()
                    }
                }
            }
            adapter?.notifyDataSetChanged()
        }

        recyclerGroup.layoutManager = GridLayoutManager(this, 3)
        recyclerGroup.adapter = nightLightGroupGrideAdapter
        nightLightGroupGrideAdapter?.bindToRecyclerView(recyclerGroup)
        nightLightGroupGrideAdapter?.onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.imgDelete) delete(adapter, position)
        }
    }

    private fun initListener() {
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        tv_function1.setOnClickListener(this)
        triggering_conditions.setOnClickListener(this)
        trigger_time.setOnClickListener(this)
        time_type.setOnClickListener(this)
        brightness_change.setOnClickListener(this)
        choose_group.setOnClickListener(this)
        sensor_update.setOnClickListener(this)
        old_time.setOnClickListener(this)
        trigger_mode.setOnClickListener(this)
        TelinkLightApplication.getApp().addEventListener(DeviceEvent.STATUS_CHANGED, this)
        TelinkLightApplication.getApp().addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    override fun onDestroy() {
        super.onDestroy()
            if (Constant.IS_ROUTE_MODE)
                currentSensor?.let {
                    routerConnectSensor(it, 1, "connectSensor")
                }
        disposableReset?.dispose()
        TelinkLightApplication.getApp().removeEventListener(this)
        TelinkLightService.Instance()?.idleMode(true)
        TelinkLightService.Instance()?.disconnect()
    }

    override fun performed(event: Event<String>?) {
        when (event?.type) {
            DeviceEvent.STATUS_CHANGED -> {
                val deviceEvent = event as DeviceEvent
                val deviceInfo = deviceEvent.args
                when (deviceInfo.status) {
                    LightAdapter.STATUS_LOGIN -> {
                        disposable?.dispose()
                        hideLoadingDialog()
                        image_bluetooth.setImageResource(R.drawable.icon_bluetooth)
                    }

                    LightAdapter.STATUS_LOGOUT -> {
                        autoConnectSensor()
                        image_bluetooth.setImageResource(R.drawable.bluetooth_no)
                    }
                }
            }
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
            }
        }
    }


    private fun onErrorReport(info: ErrorReportInfo) {
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> LogUtils.e("蓝牙未开启")
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> LogUtils.e("无法收到广播包以及响应包")
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> LogUtils.e("未扫到目标设备")
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> LogUtils.e("未读到att表")
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> LogUtils.e("未建立物理连接")
                }
            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> LogUtils.e("value check失败： 密码错误")
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> LogUtils.e("read login data 没有收到response")
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> LogUtils.e("write login data 没有收到response")
                }
                LogUtils.e("onError login")
            }
        }
    }

    private fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        version = intent.getStringExtra("version")
        getVersion(version)
        isReConfirm = mDeviceInfo.isConfirm == 1//等于1代表是重新配置
        if (isReConfirm) {
            currentSensor = DBUtils.getSensorByMeshAddr(mDeviceInfo.meshAddress)
            toolbarTv.text = currentSensor?.name
        }
        // showCheckListData = DBUtils.allGroups
        var lightGroup = DBUtils.allGroups

        showCheckListData.clear()

        for (i in lightGroup.indices) {
            when (lightGroup[i].deviceType) {
                Constant.DEVICE_TYPE_CONNECTOR, Constant.DEVICE_TYPE_LIGHT_RGB, Constant.DEVICE_TYPE_LIGHT_NORMAL, Constant.DEVICE_TYPE_NO -> {
                    if (lightGroup[i].deviceCount > 0 || lightGroup[i].deviceType == Constant.DEVICE_TYPE_NO)
                        showCheckListData!!.add(lightGroup[i])
                }
            }
        }
        nightLightEditGroupAdapter.notifyDataSetChanged()

        showGroupList = ArrayList()

        for (i in showCheckListData!!.indices) {
            val dbGroup = showCheckListData!![i]
            dbGroup.checked = i == 0 && dbGroup.meshAddr == 0xffff
            if (dbGroup.checked) {
                toolbarTv.text = dbGroup.name
                showGroupList.let {
                    if (it?.size == 0) {
                        val newItemGroup = ItemGroup()
                        newItemGroup.brightness = 50
                        newItemGroup.temperature = 50
                        newItemGroup.color = R.color.white
                        newItemGroup.checked = true
                        newItemGroup.enableCheck = true
                        newItemGroup.gpName = showCheckListData!![0].name
                        newItemGroup.groupAddress = showCheckListData!![0].meshAddr
                        it.add(newItemGroup)
                        showDataListView()
                    }
                }
            }
        }
    }

    private fun initToolbar() {
        toolbarTv.text = getString(R.string.sensor)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val moreIcon = ContextCompat.getDrawable(toolbar.context, R.drawable.abc_ic_menu_overflow_material)
        if (moreIcon != null) {
            moreIcon.setColorFilter(ContextCompat.getColor(toolbar.context, R.color.black), PorterDuff.Mode.SRC_ATOP)
            toolbar.overflowIcon = moreIcon
        }
        toolbar.setNavigationOnClickListener {
            if (isFinish) {
                sensor_three.visibility = View.VISIBLE
                scene_gp_bottom_list.visibility = View.GONE
                toolbarTv.text = getString(R.string.sensor)
                tv_function1.visibility = View.GONE
                isFinish = false
            } else
                configReturn()
        }
    }

    private fun doFinish() {
        disposable?.dispose()
        TelinkLightService.Instance()?.idleMode(true)
        if (ActivityUtils.isActivityExistsInStack(MainActivity::class.java))
            ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
        else {
            finish()
        }
    }

    private fun initView() {
        tv_function1.text = getString(R.string.confirm)
        setAdapters()
    }

    private fun getVersion(version: String) {
        if (TelinkApplication.getInstance().connectDevice != null && !Constant.IS_ROUTE_MODE) {
            hideLoadingDialog()
            tvPSVersion.text = version
            var version = tvPSVersion.text.toString()
            var num: String //N-3.1.1
            when {
                version.contains("N") -> {
                    num = version.substring(2, 3)
                    if ("" != num && num != "-" && num.toDouble() >= 3.0) {
                        isGone()
                        isVisibility()//显示3.0新的人体感应器
                    }
                }
                version.contains("PR") -> {
                    isGone()
                    isVisibility()//显示3.0新的人体感应器
                }
            }
        } else {
            LogUtils.d("device isn't connected, auto connect it")
            autoConnectSensor()
        }
    }

    private fun isGone() {
        constraintLayout30.visibility = View.GONE
        view30.visibility = View.GONE
        constraintLayout31.visibility = View.GONE
        view31.visibility = View.GONE
    }

    private fun isVisibility() {
        triggering_model.visibility = View.VISIBLE
        view15.visibility = View.VISIBLE
        trigger_after_ly.visibility = View.VISIBLE
        view16.visibility = View.VISIBLE
        overtime_ly.visibility = View.VISIBLE
        view17.visibility = View.VISIBLE
        light_brighress_mode.visibility = View.VISIBLE
        view18.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.triggering_conditions -> {
                var views = LayoutInflater.from(this).inflate(R.layout.popwindown_triggering_conditions, null)
                var set = findViewById<ConstraintLayout>(R.id.triggering_conditions)
                var popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                popupWindow.contentView = views
                popupWindow.isFocusable = true
                popupWindow.showAsDropDown(set)

                var any = views.findViewById<ConstraintLayout>(R.id.any_environment)
                var darker = views.findViewById<ConstraintLayout>(R.id.darker_environment)
                var veryDark = views.findViewById<ConstraintLayout>(R.id.very_dark_environment)

                when {
                    triggering_conditions_text.text.toString() == getString(R.string.all_day) -> any.setBackgroundResource(R.color.blue_background)
                    triggering_conditions_text.text.toString() == getString(R.string.day_time) -> darker.setBackgroundResource(R.color.blue_background)
                    triggering_conditions_text.text.toString() == getString(R.string.night) -> veryDark.setBackgroundResource(R.color.blue_background)
                }

                any.setOnClickListener {
                    triggering_conditions_text.text = getString(R.string.all_day)
                    modeTriggerCondition = 0
                    popupWindow.dismiss()
                }

                darker.setOnClickListener {
                    triggering_conditions_text.text = getString(R.string.day_time)
                    modeTriggerCondition = 1
                    popupWindow.dismiss()
                }

                veryDark.setOnClickListener {
                    triggering_conditions_text.text = getString(R.string.night)
                    modeTriggerCondition = 2
                    popupWindow.dismiss()
                }

            }

            R.id.trigger_time -> {
                var views = LayoutInflater.from(this).inflate(R.layout.popwindow_trigger_time, null)
                var set = findViewById<ConstraintLayout>(R.id.trigger_time)
                var popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                popupWindow.contentView = views
                popupWindow.isFocusable = true
                popupWindow.showAsDropDown(set)

                var lightOn = views.findViewById<ConstraintLayout>(R.id.light_on)
                var lightOff = views.findViewById<ConstraintLayout>(R.id.light_off)
                var brightness = views.findViewById<ConstraintLayout>(R.id.customBrightness)

                when {
                    trigger_time_text.text.toString() == getString(R.string.light_on) -> lightOn.setBackgroundResource(R.color.blue_background)
                    trigger_time_text.text.toString() == getString(R.string.light_off) -> lightOff.setBackgroundResource(R.color.blue_background)
                    trigger_time_text.text.toString() != getString(R.string.light_on) && trigger_time_text.text.toString() != getString(R.string.light_off) -> {
                        brightness.setBackgroundResource(R.color.blue_background)
                    }
                }

                lightOn.setOnClickListener {
                    trigger_time_text.text = getString(R.string.light_on)
                    modeStartUpMode = MODE_START_UP_MODE_OPEN
                    popupWindow.dismiss()
                }

                lightOff.setOnClickListener {
                    trigger_time_text.text = getString(R.string.light_off)
                    modeStartUpMode = MODE_START_UP_MODE_CLOSE
                    popupWindow.dismiss()
                }

                brightness.setOnClickListener {
                    popupWindow.dismiss()
                    val textGp = EditText(this)
                    textGp.inputType = InputType.TYPE_CLASS_NUMBER
                    textGp.singleLine = true
                    StringUtils.initEditTextFilter(textGp)
                    AlertDialog.Builder(this)
                            .setTitle(R.string.target_brightness)
                            .setView(textGp)
                            .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                                var brightness = textGp.text.toString()
                                if (brightness == "")
                                    brightness = "0"
                                var brin = brightness.toInt()
                                if (brin == 0) {
                                    ToastUtils.showShort(getString(R.string.brightness_cannot))
                                    return@setPositiveButton
                                }
                                if (brin > 100) {
                                    ToastUtils.showShort(getString(R.string.brightness_cannot_be_greater_than))
                                    return@setPositiveButton
                                }
                                trigger_time_text.text = textGp.text.toString() + "%"
                                modeStartUpMode = MODE_START_UP_MODE_OPEN
                                dialog.dismiss()
                            }
                            .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()

                    val timer = Timer()
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            val inputManager = textGp.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputManager.showSoftInput(textGp, 0)
                        }
                    }, 200)
                }

            }

            R.id.time_type -> {
                var views = LayoutInflater.from(this).inflate(R.layout.popwindow_time_type, null)
                var set = findViewById<ConstraintLayout>(R.id.time_type)
                var popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                popupWindow.contentView = views
                popupWindow.isFocusable = true
                popupWindow.showAsDropDown(set)

                var minute = views.findViewById<ConstraintLayout>(R.id.minute)
                var second = views.findViewById<ConstraintLayout>(R.id.second)

                when {
                    time_type_text.text.toString() == getString(R.string.minute) -> minute.setBackgroundResource(R.color.blue_background)
                    time_type_text.text.toString() == getString(R.string.second) -> second.setBackgroundResource(R.color.blue_background)
                }

                minute.setOnClickListener {
                    time_type_text.text = getString(R.string.minute)
                    modeDelayUnit = MODE_DELAY_UNIT_MINUTE
                    popupWindow.dismiss()
                }

                second.setOnClickListener {
                    time_type_text.text = getString(R.string.second)
                    modeDelayUnit = MODE_DELAY_UNIT_SECONDS
                    popupWindow.dismiss()
                }
            }

            R.id.brightness_change -> {
                var views = LayoutInflater.from(this).inflate(R.layout.popwindow_brightness_change, null)
                var set = findViewById<ConstraintLayout>(R.id.brightness_change)
                var popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                popupWindow.contentView = views
                popupWindow.isFocusable = true
                popupWindow.showAsDropDown(set)

                var moment = views.findViewById<ConstraintLayout>(R.id.moment)
                var gradient = views.findViewById<ConstraintLayout>(R.id.gradient)

                when {
                    brightness_change_text.text.toString() == getString(R.string.moment) -> moment.setBackgroundResource(R.color.blue_background)
                    brightness_change_text.text.toString() == getString(R.string.gradient) -> gradient.setBackgroundResource(R.color.blue_background)
                }

                moment.setOnClickListener {
                    brightness_change_text.text = getString(R.string.moment)
                    modeSwitchMode = MODE_SWITCH_MODE_MOMENT
                    popupWindow.dismiss()
                }

                gradient.setOnClickListener {
                    brightness_change_text.text = getString(R.string.gradient)
                    modeSwitchMode = MODE_SWITCH_MODE_GRADIENT
                    popupWindow.dismiss()
                }
            }

            R.id.old_time -> {
                var views = LayoutInflater.from(this).inflate(R.layout.popwindow_choose_time, null)
                var set = findViewById<ConstraintLayout>(R.id.old_time)
                var popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                popupWindow.contentView = views
                popupWindow.isFocusable = true
                popupWindow.showAsDropDown(set)

                var tenSec = views.findViewById<ConstraintLayout>(R.id.ten_second)
                var twentySec = views.findViewById<ConstraintLayout>(R.id.twenty_second)
                var thirtySec = views.findViewById<ConstraintLayout>(R.id.thirty_second)
                var fortySec = views.findViewById<ConstraintLayout>(R.id.forty_second)
                var fiftySec = views.findViewById<ConstraintLayout>(R.id.fifty_second)
                var oneMin = views.findViewById<ConstraintLayout>(R.id.one_minute)
                var twoMin = views.findViewById<ConstraintLayout>(R.id.two_minute)
                var threeMin = views.findViewById<ConstraintLayout>(R.id.three_minute)
                var fourMin = views.findViewById<ConstraintLayout>(R.id.four_minute)
                var fiveMin = views.findViewById<ConstraintLayout>(R.id.five_minute)

                when {
                    time_text.text.toString() == getString(R.string.ten_second) -> tenSec.setBackgroundResource(R.color.blue_background)
                    time_text.text.toString() == getString(R.string.twenty_second) -> twentySec.setBackgroundResource(R.color.blue_background)
                    time_text.text.toString() == getString(R.string.thirty_second) -> thirtySec.setBackgroundResource(R.color.blue_background)
                    time_text.text.toString() == getString(R.string.forty_second) -> fortySec.setBackgroundResource(R.color.blue_background)
                    time_text.text.toString() == getString(R.string.fifty_second) -> fiftySec.setBackgroundResource(R.color.blue_background)
                    time_text.text.toString() == getString(R.string.one_minute) -> oneMin.setBackgroundResource(R.color.blue_background)
                    time_text.text.toString() == getString(R.string.two_minute) -> twoMin.setBackgroundResource(R.color.blue_background)
                    time_text.text.toString() == getString(R.string.three_minute) -> threeMin.setBackgroundResource(R.color.blue_background)
                    time_text.text.toString() == getString(R.string.four_minute) -> fourMin.setBackgroundResource(R.color.blue_background)
                    time_text.text.toString() == getString(R.string.five_minute) -> fiveMin.setBackgroundResource(R.color.blue_background)
                }

                tenSec.setOnClickListener {
                    time_text.text = getString(R.string.ten_second)
                    selectTime = 10
                    popupWindow.dismiss()
                }

                twentySec.run {

                    when {
                        time_text.text.toString() == getString(R.string.ten_second) -> {
                            tenSec.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.twenty_second) -> {
                            twentySec.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.thirty_second) -> {
                            thirtySec.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.forty_second) -> {
                            fortySec.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.fifty_second) -> {
                            fiftySec.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.one_minute) -> {
                            oneMin.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.two_minute) -> {
                            twoMin.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.three_minute) -> {
                            threeMin.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.four_minute) -> {
                            fourMin.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.five_minute) -> {
                            fiveMin.setBackgroundResource(R.color.blue_background)
                        }
                    }

                    tenSec.setOnClickListener {
                        time_text.text = getString(R.string.ten_second)
                        selectTime = 10
                        popupWindow.dismiss()
                    }

                    twentySec.setOnClickListener {
                        time_text.text = getString(R.string.twenty_second)
                        selectTime = 20
                        popupWindow.dismiss()
                    }
                }

                thirtySec.setOnClickListener {
                    time_text.text = getString(R.string.thirty_second)
                    selectTime = 30
                    popupWindow.dismiss()
                }

                fortySec.setOnClickListener {
                    time_text.text = getString(R.string.forty_second)
                    selectTime = 40
                    popupWindow.dismiss()
                }

                fiftySec.setOnClickListener {
                    time_text.text = getString(R.string.fifty_second)
                    selectTime = 50
                    popupWindow.dismiss()
                }

                oneMin.setOnClickListener {
                    time_text.text = getString(R.string.one_minute)
                    selectTime = 1 * 60
                    popupWindow.dismiss()
                }

                twoMin.setOnClickListener {
                    time_text.text = getString(R.string.two_minute)
                    selectTime = 2 * 60
                    popupWindow.dismiss()
                }

                threeMin.setOnClickListener {
                    time_text.text = getString(R.string.three_minute)
                    selectTime = 3 * 60
                    popupWindow.dismiss()
                }

                fourMin.setOnClickListener {
                    time_text.text = getString(R.string.four_minute)
                    selectTime = 4 * 50
                    popupWindow.dismiss()
                }

                fiveMin.setOnClickListener {
                    time_text.text = getString(R.string.five_minute)
                    selectTime = 5 * 60
                    popupWindow.dismiss()
                }
            }

            R.id.trigger_mode -> {
                var views = LayoutInflater.from(this).inflate(R.layout.popwindow_trigger_mode, null)
                var set = findViewById<ConstraintLayout>(R.id.trigger_mode)
                var popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                popupWindow.contentView = views
                popupWindow.isFocusable = true
                popupWindow.showAsDropDown(set)

                var turnOn = views.findViewById<ConstraintLayout>(R.id.turn_on)
                var turnOff = views.findViewById<ConstraintLayout>(R.id.turn_off)

                when {
                    trigger_mode_text.text.toString() == getString(R.string.light_on) -> {
                        turnOn.setBackgroundResource(R.color.blue_background)
                    }

                    trigger_mode_text.text.toString() == getString(R.string.light_off) -> {
                        turnOff.setBackgroundResource(R.color.blue_background)
                    }
                }

                turnOn.setOnClickListener {
                    trigger_mode_text.text = getString(R.string.light_on)
                    switchMode = CMD_OPEN_LIGHT
                    popupWindow.dismiss()
                }

                turnOff.setOnClickListener {
                    trigger_mode_text.text = getString(R.string.light_off)
                    switchMode = CMD_CLOSE_LIGHT
                    popupWindow.dismiss()
                }
            }

            R.id.choose_group -> {
                isFinish = true

                tv_function1.visibility = View.VISIBLE
                sensor_three.visibility = View.GONE
                scene_gp_bottom_list.visibility = View.VISIBLE

                showCheckListData.let {
                    if (showGroupList!!.size != 0) {
                        for (i in it.indices)//0-1
                            loop@ for (j in showGroupList!!.indices)//0-1-3
                                when {
                                    it[i].meshAddr == showGroupList!![j].groupAddress -> {
                                        it[i].checked = true
                                        break@loop //j = 0-1-2   3-1=2
                                    }
                                    j == showGroupList!!.size - 1 && it[i].meshAddr != showGroupList!![j].groupAddress -> {
                                        it[i].checked = false
                                    }
                                }
                        changeCheckedViewData()
                    } else {
                        for (i in it.indices) {
                            it[i].isCheckedInGroup = true
                            it[i].checked = i == 0
                        }
                    }
                    nightLightEditGroupAdapter.notifyDataSetChanged()
                }
            }

            R.id.tv_function1 -> {
                val oldResultItemList = ArrayList<ItemGroup>()
                val newResultItemList = ArrayList<ItemGroup>()

                for (i in showCheckListData.indices) {
                    if (showCheckListData[i].checked) {
                        if (showGroupList!!.size == 0) {
                            val newItemGroup = ItemGroup()
                            newItemGroup.brightness = 50
                            newItemGroup.temperature = 50
                            newItemGroup.color = R.color.white
                            newItemGroup.checked = true
                            newItemGroup.enableCheck = true
                            newItemGroup.gpName = showCheckListData!![i].name
                            newItemGroup.groupAddress = showCheckListData!![i].meshAddr
                            newResultItemList.add(newItemGroup)
                        } else {
                            for (j in showGroupList!!.indices) {
                                if (showCheckListData!![i].meshAddr == showGroupList!![j].groupAddress) {
                                    oldResultItemList.add(showGroupList!![j])
                                    break
                                } else if (j == showGroupList!!.size - 1) {
                                    val newItemGroup = ItemGroup()
                                    newItemGroup.brightness = 50
                                    newItemGroup.temperature = 50
                                    newItemGroup.color = R.color.white
                                    newItemGroup.checked = true
                                    newItemGroup.enableCheck = true
                                    newItemGroup.gpName = showCheckListData!![i].name
                                    newItemGroup.groupAddress = showCheckListData!![i].meshAddr
                                    newResultItemList.add(newItemGroup)
                                }
                            }
                        }
                    }
                }

                showGroupList?.clear()
                showGroupList?.addAll(oldResultItemList)
                showGroupList?.addAll(newResultItemList)

                if (showGroupList!!.size > 8)
                    ToastUtils.showLong(getString(R.string.tip_night_light_group_limite_tip))
                else
                    showDataListView()
            }

            R.id.sensor_update -> configDevice()
        }
    }

    private fun setFrist() {
        for (i in showCheckListData!!.indices)
            showCheckListData!![i].checked = i == 0 //选中全部
    }

    private fun configDevice() {
        var version = tvPSVersion.text.toString()
        var num: String //N-3.1.1
        when {
            version.contains("N") -> {
                num = version.substring(2, 3)
                if ("" != num && num.toDouble() >= 3.0)
                    setThreeVersionOrPr()
                else
                    setBlowThreeVersion()
            }
            version.contains("PR") -> setThreeVersionOrPr()
        }
    }

    private fun setBlowThreeVersion() {
        if (showGroupList?.size == 0) {
            ToastUtils.showLong(getString(R.string.config_night_light_select_group))
            return
        }
        val device = TelinkLightApplication.getApp().connectDevice
        if (device != null && device.macAddress == mDeviceInfo.macAddress) {
            CoroutineScope(Dispatchers.IO).launch {
                runOnUiThread { showLoadingDialog(getString(R.string.please_wait)) }
                LogUtils.e("zcl人体版本中" + DBUtils.getAllSensor())
                configLightlight()
                Thread.sleep(300)
                if (!isReConfirm)//新创建进行更新
                    mDeviceInfo.meshAddress = MeshAddressGenerator().meshAddress.get()
                Commander.updateMeshName(newMeshAddr = mDeviceInfo!!.meshAddress,
                        successCallback = {
                            hideLoadingDialog()
                            saveSensor()
                        },
                        failedCallback = {
                            snackbar(sensor_root, getString(R.string.config_fail))
                            hideLoadingDialog()
                            TelinkLightService.Instance()?.idleMode(true)
                        })

            }
        } else {
            ToastUtils.showLong(getString(R.string.connect_fail))
            autoConnectSensor()
        }
    }

    private fun setThreeVersionOrPr() {
        var time = editText.text.toString()
        if (time == "") {
            ToastUtils.showShort(getString(R.string.timeout_period_is_empty))
            return
        }

        when {
            time_type_text.text.toString() == getString(R.string.second) -> {
                if (time.toInt() < 10) {
                    ToastUtils.showShort(getString(R.string.timeout_time_less_ten))
                    return
                }

                if (time.toInt() > 255) {
                    ToastUtils.showShort(getString(R.string.timeout_255))
                    return
                }
            }
            time_type_text.text.toString() == getString(R.string.minute) -> {
                if (time.toInt() < 1) {
                    ToastUtils.showShort(getString(R.string.timeout_1m))
                    return
                }

                if (time.toInt() > 255) {
                    ToastUtils.showShort(getString(R.string.timeout_255_big))
                    return
                }
            }
        }

        if (showGroupList?.size == 0) {
            ToastUtils.showLong(getString(R.string.config_night_light_select_group))
            return
        }

        GlobalScope.launch {
            configNewlight()
        }
    }

    private fun configLightlight() {
        val timeH: Byte = (selectTime shr 8 and 0xff).toByte()
        val timeL: Byte = (selectTime and 0xff).toByte()
        val paramBytes = byteArrayOf(DeviceType.NIGHT_LIGHT.toByte(), switchMode.toByte(), timeL, timeH)
        val paramBytesGroup = byteArrayOf(DeviceType.NIGHT_LIGHT.toByte(), CMD_CONTROL_GROUP.toByte(), 0, 0, 0, 0, 0, 0, 0, 0)

        var canSendGroup = true
        val listMesh = mutableListOf<Int>()
        var brightness = 0
        for (i in showGroupList!!.indices) {
            listMesh.add(showGroupList!![i].groupAddress)
            brightness = showGroupList!![i].brightness
            if (showGroupList!![i].groupAddress == 0xffff) {
                paramBytesGroup[i + 2] = 0xFF.toByte()
                break
            } else {
                val groupL: Byte = (showGroupList!![i].groupAddress and 0xff).toByte()
                paramBytesGroup[i + 2] = groupL
            }
        }
        if (Constant.IS_ROUTE_MODE) {
            /*
            //timeUnitType: Int = 0// 1 代表分 0代表秒   triggerAfterShow: Int = 0//0 开 1关 2自定义
            // triggerKey: Int = 0//0全天    1白天   2夜晚
            //mode	是	int	0群组，1场景   condition	是	int	触发条件。0全天，1白天，2夜晚
            //durationTimeUnit	是	int	持续时间单位。0秒，1分钟   durationTimeValue	是	int	持续时间
            //action	否	int	触发时执行逻辑。0开，1关，2自定义亮度。仅在群组模式下需要该配置
            //brightness	否	int	自定义亮度值。仅在群组模式下需要该配置
            //groupMeshAddrs	否	list	配置组meshAddr，可多个。仅在群组模式下需要该配置
            //sid	否	int	配置场景id。仅在场景模式下需要该配置
            */
            var action = switchMode
            routerConfigSensor(mDeviceInfo.meshAddress.toLong(), ConfigurationBean(action, 0, brightness, 0, selectTime, listMesh, 0, 0), "configHumBody")
        } else {
            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT, mDeviceInfo.meshAddress, paramBytes)
            Thread.sleep(300)
            if (canSendGroup)
                TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT, mDeviceInfo.meshAddress, paramBytesGroup)
            Thread.sleep(300)
        }
    }

    override fun tzRouterConfigSensorRecevice(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由configHumBody通知-------$cmdBean")
        if (cmdBean.ser_id == "configHumBody") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            if (cmdBean.status == 0) {
                SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, object : SyncCallback {
                    override fun start() {}

                    override fun complete() {
                        val dbsensor = DBUtils.getSensorByID(currentSensor!!.id)
                        if (!isReConfirm)
                            showRenameDialog(dbsensor!!)
                        else
                            doFinish()
                    }

                    override fun error(msg: String?) {
                        ToastUtils.showShort(msg)
                    }
                })
            } else {

            }
        }
    }

    private fun saveSensor() {
        var dbSensor = DbSensor()

        val allSensor = DBUtils.getAllSensor()
        LogUtils.e("zcl---$allSensor")
        if (isReConfirm) {
            dbSensor.index = mDeviceInfo.id
            if (100000000 != mDeviceInfo.id)
                dbSensor.id = mDeviceInfo.id.toLong()
        } else {//如果不是重新配置就保存进服务器
            DBUtils.saveSensor(dbSensor, isReConfirm)
            dbSensor.index = dbSensor.id.toInt()//拿到新的服务器id
        }

        dbSensor.controlGroupAddr = getControlGroup()
        dbSensor.macAddr = mDeviceInfo.macAddress
        dbSensor.version = version
        dbSensor.meshAddr = mDeviceInfo.meshAddress
        //dbSensor.meshAddr = Constant.SWITCH_PIR_ADDRESS
        dbSensor.productUUID = mDeviceInfo.productUUID
        dbSensor.name = getString(R.string.sensor) + dbSensor.meshAddr

        DBUtils.saveSensor(dbSensor, isReConfirm)//保存进服务器

        dbSensor = DBUtils.getSensorByID(dbSensor.id)!!

        DBUtils.recordingChange(dbSensor.id, DaoSessionInstance.getInstance().dbSensorDao.tablename, Constant.DB_ADD)

        if (!isReConfirm)
            showRenameDialog(dbSensor)
        else
            doFinish()
    }

    @SuppressLint("SetTextI18n")
    fun showRenameDialog(dbSensor: DbSensor) {
        StringUtils.initEditTextFilter(renameEditText)
        if (dbSensor.name != "" && dbSensor.name != null)
            renameEditText?.setText(dbSensor.name)
        else
            renameEditText?.setText(StringUtils.getSwitchPirDefaultName(dbSensor.productUUID, this) + "-" + DBUtils.getAllSwitch().size)
        renameEditText?.setSelection(renameEditText?.text.toString().length)

        if (!this.isFinishing) {
            renameDialog.dismiss()
            runOnUiThread {
                renameDialog.show()
            }
        }

        renameConfirm?.setOnClickListener {  // 获取输入框的内容
            if (StringUtils.compileExChar(renameEditText?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                val trim = renameEditText?.text.toString().trim { it <= ' ' }
                if (Constant.IS_ROUTE_MODE)
                    routerUpdateSensorName(dbSensor.id, trim)
                else {
                    dbSensor.name = trim
                    DBUtils.saveSensor(dbSensor, false)
                }
                if (!this.isFinishing)
                    renameDialog.dismiss()
            }
        }
        renameCancel?.setOnClickListener {
            if (!this.isFinishing)
                renameDialog?.dismiss()
        }
    }

    private fun getControlGroup(): String? {
        var controlGroupListStr = ""
        for (j in showGroupList!!.indices) {
            if (j == 0) {
                controlGroupListStr = showGroupList!![j].groupAddress.toString()
            } else {
                controlGroupListStr += "," + showGroupList!![j].groupAddress.toString()
            }
        }
        return controlGroupListStr
    }

    private fun configNewlight() {
        var mode = getModeValue()
        var paramBytes: ByteArray? = null
        when {
            trigger_time_text.text.toString() == getString(R.string.light_on) -> paramBytes = byteArrayOf(
                    switchMode.toByte(), 0x00, 0x00,
                    editText.text.toString().toInt().toByte(),
                    0x00,
                    modeTriggerCondition.toByte(),
                    mode.toByte(), 0x00, 0x00, 0x00
            )
            trigger_time_text.text.toString() == getString(R.string.light_off) -> paramBytes = byteArrayOf(
                    switchMode.toByte(), 0x00, 0x00,
                    editText.text.toString().toInt().toByte(), 0x00,
                    modeTriggerCondition.toByte(),
                    mode.toByte(), 0x00, 0x00, 0x00
            )
            else -> {
                var str = trigger_time_text.text.toString()
                var st = str.substring(0, str.indexOf("%"))
                paramBytes = byteArrayOf(
                        switchMode.toByte(), 0x00, 0x00,
                        editText.text.toString().toInt().toByte(),
                        st.toInt().toByte(),
                        modeTriggerCondition.toByte(),
                        mode.toByte(), 0x00, 0x00, 0x00
                )
            }
        }

        val paramBytesGroup = byteArrayOf(
                DeviceType.NIGHT_LIGHT.toByte(), CMD_CONTROL_GROUP.toByte(), 0, 0, 0, 0, 0, 0, 0, 0
        )

        var canSendGroup = true
        for (i in showGroupList!!.indices) {
            if (showGroupList!![i].groupAddress == 0xffff) {
                paramBytesGroup[i + 2] = 0xFF.toByte()
                break
            } else {
                val groupL: Byte = (showGroupList!![i].groupAddress and 0xff).toByte()
                paramBytesGroup[i + 2] = groupL
            }
        }
        val device = TelinkLightApplication.getApp().connectDevice
        if (device != null && device.macAddress == mDeviceInfo.macAddress) {
            GlobalScope.launch(Dispatchers.Main) {
                showLoadingDialog(getString(R.string.please_wait))
            }
            val address = device.meshAddress
            //val address = mDeviceInfo.meshAddress
            //此处不能使用mes地址 应当使用0x00代表直连灯 所以用mes的时候断联后可以成功因为灯可能变为了直连灯
            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT, 0x00, paramBytes)


            LogUtils.v("zcl配置传感器-------------${editText.text}-----------${byteToHex(paramBytes)}------------${byteToHex(paramBytesGroup)}")

            Thread.sleep(500)
            if (canSendGroup) {
                TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT,
                        address, paramBytesGroup)
            }
            Thread.sleep(3000)

            Commander.updateMeshName(
                    successCallback = {
                        this@HumanBodySensorActivity.runOnUiThread {
                            hideLoadingDialog()
                        }
                        saveSensor()
                        TelinkLightService.Instance()?.idleMode(true)
                    },
                    failedCallback = {
                        this@HumanBodySensorActivity.runOnUiThread {
                            snackbar(sensor_root, getString(R.string.config_fail))
                            hideLoadingDialog()
                        }
                        TelinkLightService.Instance()?.idleMode(true)
                    })

        } else {
            ToastUtils.showLong(getString(R.string.connect_fail))
            autoConnectSensor()
        }
    }

    /**
     * byte数组转hex
     */
    fun byteToHex(bytes: ByteArray): String {
        var strHex = ""
        val sb = StringBuilder("")
        for (n in bytes.indices) {
            strHex = Integer.toHexString(bytes[n].toInt() and 0xFF)
            sb.append(if (strHex.length == 1) "0$strHex" else strHex) // 每个字节由两个字符表示，位数不够，高位补0
        }
        return sb.toString().trim { it <= ' ' }
    }

    /*  private fun showDataListView() {
          isFinish = false
          toolbarTv.text = getString(R.string.human_body)
          sensor_three.visibility = View.VISIBLE
          recyclerView_select_group_list_view.visibility = View.GONE
          tv_function1.visibility = View.GONE
      }*/
    /**
     * 显示已选中分组
     */
    private fun showDataListView() {
        isFinish = false
        toolbarTv.text = getString(R.string.sensor)
        sensor_three.visibility = View.VISIBLE
        scene_gp_bottom_list.visibility = View.GONE
        tv_function1.visibility = View.GONE

        recyclerGroup.layoutManager = GridLayoutManager(this, 3)
        //this.nightLightGroupGrideAdapter = NightLightGroupRecycleViewAdapter(R.layout.activity_night_light_groups_item, showGroupList)
        this.nightLightGroupGrideAdapter = NightLightGroupRecycleViewAdapter(R.layout.template_batch_small_item, showGroupList)

        nightLightGroupGrideAdapter?.bindToRecyclerView(recyclerGroup)
        nightLightGroupGrideAdapter?.onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
            when (view.id) {
                R.id.imgDelete -> delete(adapter, position)
            }
        }
    }


    private fun delete(adapter: BaseQuickAdapter<Any, BaseViewHolder>, position: Int) {
        adapter.remove(position)
    }

    private fun getModeValue(): Int {
        return modeStartUpMode or modeDelayUnit or modeSwitchMode
    }

    private fun changeCheckedViewData() {
        var isAllCanCheck = true
        for (i in showCheckListData!!.indices) {
            if (showCheckListData!![0].meshAddr == 0xffff && showCheckListData!![0].checked) {//选中全部分组
                showCheckListData!![0].isCheckedInGroup = true

                if (showCheckListData!!.size > 1 && i > 0)
                    showCheckListData!![i].isCheckedInGroup = false

            } else {
                showCheckListData!![0].isCheckedInGroup = false
                if (showCheckListData!!.size > 1 && i > 0)
                    showCheckListData!![i].isCheckedInGroup = true

                if (i > 0 && showCheckListData!![i].checked)
                    isAllCanCheck = false
            }
        }
        if (isAllCanCheck)
            showCheckListData!![0].isCheckedInGroup = true
    }

    @SuppressLint("CheckResult")
    private fun autoConnectSensor() {
        if (Constant.IS_ROUTE_MODE) return
        retryConnectCount++
        showLoadingDialog(getString(R.string.connecting_tip))
        //自动重连参数
        val connectParams = Parameters.createAutoConnectParameters()
        val name: String? = if (isReConfirm)
            DBUtils.lastUser?.controlMeshName
        else
            Constant.DEFAULT_MESH_FACTORY_NAME
        connectParams?.setMeshName(name)

        connectParams?.setConnectMac(mDeviceInfo.macAddress)
        val substring: String = if (isReConfirm)
            NetworkFactory.md5(NetworkFactory.md5(DBUtils.lastUser?.controlMeshName) + DBUtils.lastUser?.controlMeshName).substring(0, 16)
        else
            Constant.DEFAULT_MESH_FACTORY_PASSWORD

        connectParams?.setPassword(substring)
        LogUtils.d("zcl开始连接${mDeviceInfo.macAddress}-----------$substring---------$name----")
        connectParams?.autoEnableNotification(true)
        connectParams?.setTimeoutSeconds(10)
        progressBar_sensor?.visibility = View.VISIBLE
        //连接，如断开会自动重连
        GlobalScope.launch {
            delay(100)
            TelinkLightService.Instance()?.autoConnect(connectParams)
        }
        disposable?.dispose()
        disposable = Observable.timer(15000, TimeUnit.MILLISECONDS) .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe({
            LeBluetooth.getInstance().stopScan()
            TelinkLightService.Instance()?.idleMode(true)
            hideLoadingDialog()
            progressBar_sensor?.visibility = View.GONE
            ToastUtils.showLong(getString(R.string.connect_fail))
        }, {})
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            configReturn()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun configReturn() {
        if (!isReConfirm)
            AlertDialog.Builder(this)
                    .setMessage(getString(R.string.config_return))
                    .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                        startActivity(Intent(this@HumanBodySensorActivity, SelectDeviceTypeActivity::class.java))
                        finish()
                    }
                    .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()
        else
            doFinish()
    }
}
