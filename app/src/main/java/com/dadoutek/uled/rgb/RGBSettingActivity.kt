package com.dadoutek.uled.rgb

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.*
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.RouteGetVerBean
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DBUtils.lastUser
import com.dadoutek.uled.model.dbModel.DbDiyGradient
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbLight
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.GroupBodyBean
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.router.DelGradientBodyBean
import com.dadoutek.uled.router.RouterOtaActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.router.bean.RouteGroupingOrDelBean
import com.dadoutek.uled.switches.ChooseGroupOrSceneActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.light.ConnectionStatus
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_rgb_gradient.*
import kotlinx.android.synthetic.main.activity_rgb_group_setting.*
import kotlinx.android.synthetic.main.template_add_help.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.startActivity
import top.defaults.colorpicker.ColorObserver
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * 创建者     zcl
 * 创建时间   2019/8/27 16:14
 * 描述	      ${RGB灯设置界面}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${
 */
class RGBSettingActivity : TelinkBaseActivity(), View.OnTouchListener {
    private var positionStateOff: Int = 0
    private var clickDiy: Boolean = false
    private var color: Int = 0
    private var sendProgress: Int = 1
    private var deviceType: Int = DeviceType.LIGHT_RGB
    private var addr: Int = 0
    private var fiChangeGp: MenuItem? = null
    private var findItem: MenuItem? = null
    private val requestCodeNum: Int = 1000
    private var mConnectDeviceDisposable: Disposable? = null
    private var clickPostion: Int = 100
    private var postionAndNum: ItemRgbGradient? = null
    private var mApplication: TelinkLightApplication? = null
    private var stopTracking = false
    private var presetColors: MutableList<ItemColorPreset>? = null
    private var colorSelectDiyRecyclerViewAdapter: ColorSelectDiyRecyclerViewAdapter? = null
    private var group: DbGroup? = null
    private var mConnectTimer: Disposable? = null
    private var localVersion: String? = null
    private var light: DbLight? = null
    private var gpAddress: Int = 0
    private var fromWhere: String? = null
    private var dataManager: DataManager? = null
    private val mDisposable = CompositeDisposable()
    private var mConnectDevice: DeviceInfo? = null
    val POSIONANDNUM = "POSIONANDNUM"
    private var isExitGradient = false
    private var isDiyMode = false
    private var isPresetMode = true
    private var diyPosition: Int = 100
    private var isDelete = false
    private var dstAddress: Int = 0
    private var firstLightAddress: Int = 0
    var typeStr: String = Constant.TYPE_GROUP
    var speed = 1
    var positionState = 0
    private var buildInModeList: ArrayList<ItemRgbGradient> = ArrayList()
    private var diyGradientList: MutableList<DbDiyGradient> = ArrayList()
    private var rgbGradientAdapter: RGBGradientAdapter = RGBGradientAdapter(R.layout.item_gradient_mode, buildInModeList)
    private var rgbDiyGradientAdapter: RGBDiyGradientAdapter = RGBDiyGradientAdapter(R.layout.diy_gradient_item, diyGradientList, isDelete)
    private var applyDisposable: Disposable? = null
    private var downTime: Long = 0//Button被按下时的时间
    private var thisTime: Long = 0//while每次循环时的时间
    internal var onBtnTouch = false//Button是否被按下
    private var tvValue = 0//TextView中的值
    private var redColor: Int? = null
    private var greenColor: Int? = null
    private var blueColor: Int? = null
    private var mConnectDisposal: Disposable? = null
    private var mScanDisposal: Disposable? = null
    private var mScanTimeoutDisposal: Disposable? = null
    private var mCheckRssiDisposal: Disposable? = null
    private var acitivityIsAlive = true
    private var swOpen = true

    @SuppressLint("StringFormatInvalid")
    private fun removeGroup() {
        AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(this))
                .setMessage(getString(R.string.delete_group_confirm, group?.name))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    this.showLoadingDialog(getString(R.string.deleting))
                    if (Constant.IS_ROUTE_MODE) {
                        routeDeleteGroup("delRGBGp", group!!)
                    } else {
                        deleteGroup(DBUtils.getLightByGroupID(group!!.id), group!!,
                                successCallback = {
                                    deleteGpSuccess()
                                },
                                failedCallback = {
                                    this.hideLoadingDialog()
                                    ToastUtils.showLong(R.string.move_out_some_lights_in_group_failed)
                                })
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }


    override fun deleteGpSuccess() {
        SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
        this.hideLoadingDialog()
        this.setResult(Constant.RESULT_OK)
        LogUtils.e("zcl删除组后" + DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_RGB))
        this.finish()
    }

    private fun routerConfigBrightnesssOrColorTemp(brightness: Boolean) = when {
        brightness -> {//亮度
            when {
                currentShowGroupSetPage && group != null -> routeConfigBriGpOrLight(group!!.meshAddr, 97, sendProgress, "gpBri")
                else -> routeConfigBriGpOrLight(light!!.meshAddr, light!!.productUUID, sendProgress, "rgbBri")
            }
        }
        else -> {
            when {
                currentShowGroupSetPage && group != null -> routeConfigTempGpOrLight(group!!.meshAddr, 97, sendProgress, "gpTem")
                else -> routeConfigTempGpOrLight(light!!.meshAddr, light!!.productUUID, sendProgress, "rgbTem")
            }
        }
    }

    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    override fun tzRouterDelGroupResult(routerGroup: RouteGroupingOrDelBean?) {
        LogUtils.v("zcl-----------收到路由删组通知-------${routerGroup}")
        disposableTimer?.dispose()
        if (routerGroup?.ser_id == "delRGBGp") {
            hideLoadingDialog()
            if (routerGroup?.finish) {
                val gp = DBUtils.getGroupByID(routerGroup.targetGroupId.toLong())
                when (routerGroup?.status) {
                    0 -> {
                        deleteGpSuccess()
                        ToastUtils.showShort(getString(R.string.delete_group_success))
                    }
                    1 -> ToastUtils.showShort(getString(R.string.delete_group_some_fail))
                    -1 -> ToastUtils.showShort(getString(R.string.delete_gp_fail))
                }
            } else {
                ToastUtils.showShort(getString(R.string.router_del_gp, routerGroup?.succeedNow?.size))
            }
        }
    }

    fun remove() {
        AlertDialog.Builder(this).setMessage(getString(R.string.sure_delete_device2))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (TelinkLightService.Instance()?.adapter?.mLightCtrl?.currentLight != null && TelinkLightService.Instance()?.adapter!!
                                    .mLightCtrl.currentLight.isConnected || Constant.IS_ROUTE_MODE) {

                        if (Constant.IS_ROUTE_MODE) {
                            routerDeviceResetFactory(light!!.macAddr, light!!.meshAddr, light!!.productUUID, "rgbFactory")
                        } else {
                            showLoadingDialog(getString(R.string.please_wait))
                            val subscribe = Commander.resetDevice(light!!.meshAddr)
                                    .subscribe({  //deleteData()
                                    }, {
                                        GlobalScope.launch(Dispatchers.Main) {
                                            /*  showDialogHardDelete?.dismiss()
                                             showDialogHardDelete = android.app.AlertDialog.Builder(this).setMessage(R.string.delete_device_hard_tip)
                                                     .setPositiveButton(android.R.string.ok) { _, _ ->
                                                         showLoadingDialog(getString(R.string.please_wait))
                                                         deleteData()
                                                     }
                                                     .setNegativeButton(R.string.btn_cancel, null)
                                                     .show() */
                                        }
                                    })
                            deleteData()
                        }

                    } else {
                        ToastUtils.showLong(getString(R.string.bluetooth_open_connet))
                        this.finish()
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }


    private fun deleteData() {
        hideLoadingDialog()
        DBUtils.deleteLight(light!!)
        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(light!!.meshAddr))
            TelinkLightApplication.getApp().mesh.saveOrUpdate(this!!)

        if (mConnectDevice != null) {
            Log.d(this.javaClass.simpleName, "mConnectDevice.meshAddress = " + mConnectDevice!!.meshAddress)
            if (light!!.meshAddr == mConnectDevice!!.meshAddress)
                this.setResult(Activity.RESULT_OK, Intent().putExtra("data", true))
        }
        SyncDataPutOrGetUtils.syncPutDataStart(this, object : SyncCallback {
            override fun start() {}

            override fun complete() {}

            override fun error(msg: String?) {}
        })
        this.finish()
    }

    private fun updateGroup() {
        val intent = Intent(this, ChooseGroupOrSceneActivity::class.java)
        val bundle = Bundle()
        bundle.putInt(Constant.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
        bundle.putInt(Constant.DEVICE_TYPE, Constant.DEVICE_TYPE_LIGHT_RGB.toInt())
        intent.putExtras(bundle)
        startActivityForResult(intent, requestCodeNum)

        if (light == null) {
            ToastUtils.showLong(getString(R.string.please_connect_normal_light))
            TelinkLightService.Instance()?.idleMode(true)
            return
        }

        light?.let {
            intent.putExtra(Constant.TYPE_VIEW, Constant.LIGHT_KEY)
            intent.putExtra("uuid", it.productUUID)
            intent.putExtra("gpAddress", it.meshAddr)
            Log.d("addLight", it.productUUID.toString() + "," + it.meshAddr)
        }
    }


    private fun updateGroupResult(light: DbLight, group: DbGroup) {
        Commander.addGroup(light.meshAddr, group.meshAddr, {
            group.deviceType = light.productUUID.toLong()
            light.hasGroup = true
            light.belongGroupId = group.id
            light.name = light.name
            DBUtils.updateLight(light)
            ToastUtils.showShort(getString(R.string.grouping_success_tip))
            if (group != null)
                DBUtils.updateGroup(group!!)//更新组类型
        }, {
            ToastUtils.showShort(getString(R.string.grouping_fail))
        })
    }

    private fun checkPermission() {
        mDisposable.add(
                RxPermissions(this)!!.request(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            isDirectConnectDevice(it)
                        }, {

                        }))
    }

    private fun isDirectConnectDevice(granted: Boolean) {
        when {
            !isSuportOta(light?.version) -> ToastUtils.showShort(getString(R.string.dissupport_ota))
            isMostNew(light?.version) -> ToastUtils.showShort(getString(R.string.the_last_version))
            else -> {
                if (Constant.IS_ROUTE_MODE) {
                    startActivity<RouterOtaActivity>("deviceMeshAddress" to light!!.meshAddr, "deviceType" to light!!.productUUID,
                            "deviceMac" to light!!.macAddr, "version" to light!!.version)
                    finish()
                } else {
                    var isBoolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_DEVELOPER_MODE, false)
                    if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == light?.meshAddr) {
                        if (granted!!) {
                            if (isBoolean) {
                                transformView()
                            } else {
                                OtaPrepareUtils.instance().gotoUpdateView(this@RGBSettingActivity, localVersion, otaPrepareListner)
                            }
                        } else {
                            ToastUtils.showLong(R.string.update_permission_tip)
                        }
                    } else {
                        showLoadingDialog(getString(R.string.please_wait))
                        TelinkLightService.Instance()?.idleMode(true)
                        mConnectDeviceDisposable = Observable.timer(800, TimeUnit.MILLISECONDS)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .flatMap {
                                    connect(light!!.meshAddr, macAddress = light!!.macAddr)
                                }
                                ?.subscribe(
                                        {
                                            hideLoadingDialog()
                                            if (isBoolean) {
                                                transformView()
                                            } else {
                                                OtaPrepareUtils.instance().gotoUpdateView(this@RGBSettingActivity, localVersion, otaPrepareListner)
                                            }
                                        }
                                        ,
                                        {
                                            hideLoadingDialog()
                                            runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                                            LogUtils.d(it)
                                        })
                    }
                }
            }
        }
    }

    private fun renameLight() {
        if (!TextUtils.isEmpty(light?.name))
            renameEt?.setText(light?.name)
        renameEt?.setSelection(renameEt?.text.toString().length)

        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }

        renameConfirm?.setOnClickListener {    // 获取输入框的内容
            if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                light?.name = renameEt?.text.toString().trim { it <= ' ' }
                DBUtils.updateLight(light!!)
                toolbarTv.text = light?.name
                renameDialog?.dismiss()
            }
        }
    }

    var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {
        override fun downLoadFileStart() {
            showLoadingDialog(getString(R.string.get_update_file))
        }

        override fun startGetVersion() {
            showLoadingDialog(getString(R.string.verification_version))
        }

        override fun getVersionSuccess(s: String) {
            ToastUtils.showLong(R.string.verification_version_success)
            transformView()
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
        mConnectDeviceDisposable?.dispose()
        when {
            !isSuportOta(light?.version) -> ToastUtils.showShort(getString(R.string.dissupport_ota))
            isMostNew(light?.version) -> ToastUtils.showShort(getString(R.string.the_last_version))
            else -> {

                if (Constant.IS_ROUTE_MODE) {
                    startActivity<RouterOtaActivity>("deviceMeshAddress" to light!!.meshAddr,
                            "deviceType" to light!!.productUUID, "deviceMac" to light!!.macAddr)
                    finish()
                } else {
                    val intent = Intent(this@RGBSettingActivity, OTAUpdateActivity::class.java)
                    intent.putExtra(Constant.UPDATE_LIGHT, light)
                    intent.putExtra(Constant.OTA_MAC, light?.macAddr)
                    intent.putExtra(Constant.OTA_MES_Add, light?.meshAddr)
                    intent.putExtra(Constant.OTA_VERSION, light?.version)
                    intent.putExtra(Constant.OTA_TYPE, DeviceType.LIGHT_RGB)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rgb_group_setting)
        this.mApplication = this.application as TelinkLightApplication
        val s = SharedPreferencesHelper.getString(this, POSIONANDNUM, "")
        postionAndNum = ItemRgbGradient()
        if (s != null && "" != s) {
            val split = s.split("-")
            if (split.size >= 2) {
                postionAndNum!!.position = split[0].toInt()
                postionAndNum!!.speed = split[1].toInt()
            }
            LogUtils.e("zcl", "zcl渐变设置******" + postionAndNum.toString())
        }
        initType()
    }

    private fun initType() {
        mApplication = application as TelinkLightApplication
        typeStr = intent.getStringExtra(Constant.TYPE_VIEW)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val moreIcon = ContextCompat.getDrawable(toolbar.context, R.drawable.abc_ic_menu_overflow_material)
        if (moreIcon != null) {
            moreIcon.setColorFilter(ContextCompat.getColor(toolbar.context, R.color.black), PorterDuff.Mode.SRC_ATOP)
            toolbar.overflowIcon = moreIcon
        }
        main_add_device?.text = getString(R.string.mode_diy)
        main_go_help?.visibility = View.VISIBLE
        when {
            Constant.IS_OPEN_AUXFUN -> rgbVisiblity(View.VISIBLE)
            else -> rgbVisiblity(View.GONE)
        }


        /*--------------------群组与灯具分支-------------*/
        currentShowGroupSetPage = typeStr == Constant.TYPE_GROUP
        when {
            currentShowGroupSetPage -> {
                group = intent.extras!!.get("group") as DbGroup
                toolbarTv.text = getString(R.string.select_group)
                img_function1.visibility = View.VISIBLE
                img_function1.setOnClickListener { renameGp() }
                img_function1.setImageResource(R.drawable.icon_editor)
                when {
                    group != null -> {
                        when (group!!.meshAddr) {
                            0xffff -> toolbarTv.text = getString(R.string.allLight)
                            else -> toolbarTv.text = group?.name
                        }
                    }
                }
            }
            else -> {
                light = intent.extras!!.get(Constant.LIGHT_ARESS_KEY) as DbLight
                img_function1.visibility = View.GONE
                tvRename.visibility = View.GONE
                toolbarTv.text = light?.name
            }
        }

        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        rgb_switch.setOnClickListener { swLight(true) }

        when {
            currentShowGroupSetPage -> initViewGroup()
            else -> {
                initView()
                getVersion()
            }
        }
    }

    private fun setSwIcon() {
        var status = when {
            currentShowGroupSetPage -> group?.connectionStatus
            else -> light?.connectionStatus
        }
        when (status) {
            1 -> rgb_switch.setImageResource(R.drawable.icon_light_open)
            else -> rgb_switch.setImageResource(R.drawable.icon_light_close)
        }
    }

    private fun swLight(isClick: Boolean) {
        var connectionStatus = if (currentShowGroupSetPage) group?.connectionStatus else light?.connectionStatus
        var status = when {
            isClick -> if (connectionStatus == 1) 0 else 1
            else -> connectionStatus ?: 1
        }

        val meshAddr = if (currentShowGroupSetPage) group!!.meshAddr else light!!.meshAddr
        val productUUID = if (currentShowGroupSetPage) 97 else 6

        when {
            Constant.IS_ROUTE_MODE -> {//meshType普通灯 = 4 彩灯 = 6 连接器 = 5 组 = 97 0关1开
                swOpen = status == 1
                routeOpenOrCloseBase(meshAddr, productUUID, status, "rgbSwitch")
            }
            else -> openOrClose(status == 1)
        }
    }

    private fun rgbVisiblity(visible: Int) {
        ll_r.visibility = visible
        ll_g.visibility = visible
        ll_b.visibility = visible
    }

    @SuppressLint("CheckResult")
    private fun getVersion() {
        if (TelinkApplication.getInstance().connectDevice != null || Constant.IS_ROUTE_MODE) {
            when {
                Constant.IS_ROUTE_MODE -> routerGetVersion(mutableListOf(light?.meshAddr ?: 0), 6, "rgbVersion")
                else -> Commander.getDeviceVersion(light!!.meshAddr)
                        .subscribe({ s ->
                            updateVersion(s)
                            null
                        }, {
                            LogUtils.d(it)
                        })
            }
        }
    }

    private fun updateVersion(s: String?) {
        if (!TextUtils.isEmpty(s)) {
            localVersion = s
            if (!TextUtils.isEmpty(localVersion)) {
                light!!.version = localVersion
                findItem?.title = localVersion
            }
            DBUtils.saveLight(light!!, false)
        }
    }


    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun initView() {
        this.fromWhere = this.intent.getStringExtra(Constant.LIGHT_REFRESH_KEY)
        this.gpAddress = this.intent.getIntExtra(Constant.GROUP_ARESS_KEY, 0)
        dataManager = DataManager(this, mApplication!!.mesh.name, mApplication!!.mesh.password)

        localVersion = when {
            TextUtils.isEmpty(light!!.version) -> getString(R.string.number_no)
            else -> light!!.version
        }

        tvRename!!.setOnClickListener(this.clickListener)
        tvOta!!.setOnClickListener(this.clickListener)
        dynamic_rgb!!.setOnClickListener(this.clickListener)
        ll_r.setOnClickListener(this.clickListener)
        ll_g.setOnClickListener(this.clickListener)
        ll_b.setOnClickListener(this.clickListener)
        normal_rgb.setOnClickListener(this.clickListener)
        btnAdd.setOnClickListener(this.clickListener)
        mode_preset_layout.setOnClickListener(this.clickListener)
        mode_diy_layout.setOnClickListener(this.clickListener)
        btnStopGradient.setOnClickListener(this.clickListener)
        cb_white_enable.setOnClickListener(cbOnClickListener)
        cb_brightness_enable.setOnClickListener(cbOnClickListener)
        main_add_device?.setOnClickListener(clickListener)
        main_go_help?.setOnClickListener(clickListener)

        sb_w_bright_add!!.setOnTouchListener { _, event ->
            addWhiteBright(event)
            true
        }
        sb_w_bright_less!!.setOnTouchListener { _, event ->
            lessWhiteBright(event)
            true
        }
        sbBrightness_add.setOnTouchListener { _, event ->
            addBrightness(event)
            true
        }
        sbBrightness_less.setOnTouchListener { _, event ->
            lessBrightness(event)
            true
        }


        rgb_sbBrightness!!.max = 100
        rgb_white_seekbar!!.max = 100

        color_picker.reset()
        color_picker.subscribe(colorObserver)

        mConnectDevice = TelinkLightApplication.getApp().connectDevice

        presetColors = SharedPreferencesHelper.getObject(this, Constant.PRESET_COLOR) as? MutableList<ItemColorPreset>
        if (presetColors == null) {
            presetColors = ArrayList()
            for (i in 0..4) {
                val itemColorPreset = ItemColorPreset()
                itemColorPreset.color = OtherUtils.getCreateInitColor(i)
                presetColors!!.add(itemColorPreset)
            }
        } else {
            if (presetColors!!.size > 4) {
                presetColors = presetColors!!.subList(0, 4)
            } else if (presetColors!!.size < 4) {
                for (i in 0 until 4) {
                    val itemColorPreset = ItemColorPreset()
                    itemColorPreset.color = OtherUtils.getCreateInitColor(i)
                    if (presetColors!!.size < 5)
                        presetColors?.add(itemColorPreset)
                    else
                        break
                }
            }
        }

        diy_color_recycler_list_view!!.layoutManager = GridLayoutManager(this, 4)
        colorSelectDiyRecyclerViewAdapter = ColorSelectDiyRecyclerViewAdapter(R.layout.color_select_diy_item, presetColors)
        colorSelectDiyRecyclerViewAdapter!!.onItemChildClickListener = diyOnItemChildClickListener
        colorSelectDiyRecyclerViewAdapter!!.onItemChildLongClickListener = diyOnItemChildLongClickListener
        colorSelectDiyRecyclerViewAdapter!!.bindToRecyclerView(diy_color_recycler_list_view)

        setColorMode()
        setDiyColorMode()

        val brightness = isZeroOrHundred(light!!.brightness)
        rgb_sbBrightness!!.progress = brightness
        sbBrightness_num!!.text = "$brightness%"

        when {
            rgb_sbBrightness!!.progress >= 100 -> {
                sbBrightness_add.isEnabled = false
                sbBrightness_less.isEnabled = true
            }
            rgb_sbBrightness!!.progress <= 1 -> {
                sbBrightness_less.isEnabled = false
                sbBrightness_add.isEnabled = true
            }
            else -> {
                sbBrightness_less.isEnabled = true
                sbBrightness_add.isEnabled = true
            }
        }

        var w = ((light?.color ?: 1) and 0xff000000.toInt()) shr 24
        var r = Color.red(light?.color ?: 0)
        var g = Color.green(light?.color ?: 0)
        var b = Color.blue(light?.color ?: 0)
        if (w == -1 || w < 1) {
            w = 1
        }

        color_r.text = r.toString()
        color_g.text = g.toString()
        color_b.text = b.toString()

        w = when {
            w <= 0 -> 1
            w > 100 -> 100
            else -> w
        }

        sb_w_bright_num.text = "$w%"
        rgb_white_seekbar.progress = w

        when {
            rgb_white_seekbar.progress >= 100 -> {
                sb_w_bright_add.isEnabled = false
                sb_w_bright_less.isEnabled = true
            }
            rgb_white_seekbar.progress <= 1 -> {
                sb_w_bright_less.isEnabled = false
                sb_w_bright_add.isEnabled = true
            }
            else -> {
                sb_w_bright_less.isEnabled = true
                sb_w_bright_add.isEnabled = true
            }
        }

        color_picker.setInitialColor((light?.color ?: 1 and 0xffffff) or 0xff000000.toInt())
        rgb_sbBrightness!!.setOnSeekBarChangeListener(barChangeListener)
        rgb_white_seekbar.setOnSeekBarChangeListener(barChangeListener)

        swLight(false)
    }

    private fun setDiyColorMode() {
        diyGradientList.clear()
        diyGradientList.addAll(DBUtils.diyGradientList)
        builtDiyModeRecycleView!!.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        //渐变模式自定义
        rgbDiyGradientAdapter = RGBDiyGradientAdapter(R.layout.diy_gradient_item, diyGradientList, isDelete)
        builtDiyModeRecycleView?.itemAnimator = DefaultItemAnimator()
        val decorations = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decorations.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.black_ee)))
        //添加分割线
        builtDiyModeRecycleView?.addItemDecoration(decorations)
        // rgbDiyGradientAdapter!!.addFooterView(lin)
        rgbDiyGradientAdapter.onItemChildClickListener = onItemChildClickListenerDiy
        rgbDiyGradientAdapter!!.onItemLongClickListener = this.onItemChildLongClickListenerDiy
        rgbDiyGradientAdapter.bindToRecyclerView(builtDiyModeRecycleView)
    }

    private fun setColorMode() {
        setDIyModeData()
        builtInModeRecycleView!!.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        //渐变标准模式  添加分割线
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
        builtInModeRecycleView?.addItemDecoration(decoration)
        rgbGradientAdapter = RGBGradientAdapter(R.layout.item_gradient_mode, buildInModeList)
        rgbGradientAdapter.onItemChildClickListener = onItemChildClickListener
        rgbGradientAdapter.bindToRecyclerView(builtInModeRecycleView)
    }


    private fun openOrClose(isOpen: Boolean) {
        LogUtils.e("currentLight$isOpen")
        afterSendOpenOrClose(isOpen)
        swOpen = isOpen
        CoroutineScope(Dispatchers.IO).launch {
            Commander.openOrCloseLights(addr, isOpen)
        }
    }

    private fun afterSendOpenOrClose(isOpen: Boolean) {
        enableAllUI(isOpen)
        when {
            isOpen -> {
                if (currentShowGroupSetPage) {
                    addr = group!!.meshAddr
                    group!!.connectionStatus = ConnectionStatus.ON.value
                } else {
                    addr = light!!.meshAddr
                    light!!.connectionStatus = ConnectionStatus.ON.value
                }
            }
            else -> {
                if (currentShowGroupSetPage) {
                    addr = group!!.meshAddr
                    group!!.connectionStatus = ConnectionStatus.OFF.value
                } else {
                    addr = light!!.meshAddr
                    light!!.connectionStatus = ConnectionStatus.OFF.value
                }
            }
        }
        when {
            currentShowGroupSetPage -> DBUtils.saveGroup(group!!, false)
            else -> DBUtils.saveLight(light!!, false)
        }
        setSwIcon()
    }

    private val cbOnClickListener = OnClickListener {
        when (it.id) {
            R.id.cb_white_enable -> {
                if (cb_white_enable.isChecked) {
                    whiteCheckUi(true)
                    sendWhiteNum(rgb_white_seekbar.progress)
                } else {
                    whiteCheckUi(false)
                    sendWhiteNum(0)
                }
            }
            R.id.cb_brightness_enable -> {
                if (cb_brightness_enable.isChecked) {
                    briCheckUi(true)
                    sendBrightnessMsg(rgb_sbBrightness.progress)
                } else {
                    briCheckUi(false)
                    sendBrightnessMsg(0)
                }
            }
        }
    }

    private fun whiteCheckUi(b: Boolean) {
        rgb_white_seekbar.isEnabled = b
        sb_w_bright_add.isEnabled = b
        sb_w_bright_less.isEnabled = b
    }

    private fun enableAllUI(isEnabled: Boolean) {
        cb_white_enable.isClickable = isEnabled
        cb_brightness_enable.isClickable = isEnabled
        cb_brightness_enable.isChecked = isEnabled
        cb_white_enable.isChecked = isEnabled

        rgb_white_seekbar.isEnabled = isEnabled
        sb_w_bright_add.isEnabled = isEnabled
        sb_w_bright_less.isEnabled = isEnabled

        rgb_sbBrightness.isEnabled = isEnabled
        sbBrightness_add.isEnabled = isEnabled
        sbBrightness_less.isEnabled = isEnabled

        cb_white_enable.isEnabled = isEnabled
        cb_brightness_enable.isEnabled = isEnabled

        ll_r.isEnabled = isEnabled
        ll_g.isEnabled = isEnabled
        ll_b.isEnabled = isEnabled

        if (isEnabled) {
            colorSelectDiyRecyclerViewAdapter?.onItemChildClickListener = diyOnItemChildClickListener
            colorSelectDiyRecyclerViewAdapter?.onItemChildLongClickListener = diyOnItemChildLongClickListener
            color_picker.isDispatchTouchEvent = true

        } else {
            colorSelectDiyRecyclerViewAdapter?.onItemChildClickListener = null
            colorSelectDiyRecyclerViewAdapter?.onItemChildLongClickListener = null
            color_picker.isDispatchTouchEvent = false
        }

        val paint = Paint()
        val colorMatrix = ColorMatrix()

        if (isEnabled) {
            colorMatrix.setSaturation(1f)
        } else {
            // 让界面变灰色
            colorMatrix.setSaturation(0.1f)
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        rgb_set.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
    }


    private fun lessBrightness(event: MotionEvent?) {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            if (Constant.IS_ROUTE_MODE)
                                routerConfigBrightnesssOrColorTemp(true)
                            val msg = handler_brightness_less.obtainMessage()
                            msg.arg1 = tvValue
                            handler_brightness_less.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            delay(100)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    tvValue++
                    if (Constant.IS_ROUTE_MODE)
                        routerConfigBrightnesssOrColorTemp(true)
                    val msg = handler_brightness_less.obtainMessage()
                    msg.arg1 = tvValue
                    handler_brightness_less.sendMessage(msg)
                }
            }
            MotionEvent.ACTION_CANCEL -> onBtnTouch = false
        }
    }

    private fun addBrightness(event: MotionEvent?) {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handler_brightness_add.obtainMessage()
                            msg.arg1 = tvValue
                            handler_brightness_add.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            if (Constant.IS_ROUTE_MODE)
                                routerConfigBrightnesssOrColorTemp(true)
                            delay(100)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    tvValue++
                    if (Constant.IS_ROUTE_MODE)
                        routerConfigBrightnesssOrColorTemp(true)
                    val msg = handler_brightness_add.obtainMessage()
                    msg.arg1 = tvValue
                    handler_brightness_add.sendMessage(msg)
                }
            }
            MotionEvent.ACTION_CANCEL -> onBtnTouch = false
        }
    }

    private fun lessWhiteBright(event: MotionEvent?) {
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            if (Constant.IS_ROUTE_MODE)
                                if (currentShowGroupSetPage)
                                    routeConfigWhiteGpOrLight(group?.meshAddr ?: 0, 97, tvValue, "rgbwhite")
                                else
                                    routeConfigWhiteGpOrLight(light?.meshAddr ?: 0, (light?.productUUID ?: 0).toInt(), tvValue, "rgbwhite")
                            val msg = handler_less.obtainMessage()
                            msg.arg1 = tvValue
                            handler_less.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            delay(100)

                        }
                    }
                }

            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    tvValue++
                    if (Constant.IS_ROUTE_MODE)
                        if (currentShowGroupSetPage)
                            routeConfigWhiteGpOrLight(group?.meshAddr ?: 0, 97, tvValue, "rgbwhite")
                        else
                            routeConfigWhiteGpOrLight(light?.meshAddr ?: 0, (light?.productUUID ?: 0).toInt(), tvValue, "rgbwhite")
                    val msg = handler_less.obtainMessage()
                    msg.arg1 = tvValue
                    handler_less.sendMessage(msg)
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> onBtnTouch = false
        }
    }

    private fun addWhiteBright(event: MotionEvent?) {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            if (Constant.IS_ROUTE_MODE)
                                if (currentShowGroupSetPage)
                                    routeConfigWhiteGpOrLight(group?.meshAddr ?: 0, 97, tvValue, "rgbwhite")
                                else
                                    routeConfigWhiteGpOrLight(light?.meshAddr ?: 0, (light?.productUUID ?: 0).toInt(), tvValue, "rgbwhite")
                            val msg = handler.obtainMessage()
                            msg.arg1 = tvValue
                            handler.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            delay(100)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    tvValue++
                    if (Constant.IS_ROUTE_MODE)
                        if (currentShowGroupSetPage)
                            routeConfigWhiteGpOrLight(group?.meshAddr ?: 0, 97, tvValue, "rgbwhite")
                        else
                            routeConfigWhiteGpOrLight(light?.meshAddr ?: 0, (light?.productUUID ?: 0).toInt(), tvValue, "rgbwhite")
                    val msg = handler.obtainMessage()
                    msg.arg1 = tvValue
                    handler.sendMessage(msg)
                }
            }
            MotionEvent.ACTION_CANCEL -> onBtnTouch = false
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            rgb_white_seekbar.progress++
            when {
                rgb_white_seekbar.progress > 100 -> {
                    briCheckUi(false)
                    sb_w_bright_num.text = "100%"
                }
                rgb_white_seekbar.progress == 100 -> {
                    sb_w_bright_add.isEnabled = false
                    sb_w_bright_num.text = rgb_white_seekbar.progress.toString() + "%"
                    stopTracking = false
                    onBtnTouch = false
                }
                else -> {
                    sb_w_bright_add.isEnabled = true
                    stopTracking = true
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler_brightness_add = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            rgb_sbBrightness.progress++
            when {
                rgb_sbBrightness.progress > 100 -> {
                    briCheckUi(false)
                }
                rgb_sbBrightness.progress == 100 -> {
                    sbBrightness_add.isEnabled = false
                    if (!Constant.IS_ROUTE_MODE)
                        when {
                            currentShowGroupSetPage -> group?.brightness = rgb_sbBrightness.progress
                            else -> light?.brightness = rgb_sbBrightness.progress
                        }

                    rgb_sbBrightness.progress = isZeroOrHundred(rgb_sbBrightness.progress)
                    sbBrightness_num.text = rgb_sbBrightness.progress.toString() + "%"
                    stopTracking = false
                    onBtnTouch = false
                }
                else -> {
                    sbBrightness_add.isEnabled = true
                    stopTracking = true
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler_brightness_less = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            rgb_sbBrightness.progress--
            when {
                rgb_sbBrightness.progress < 1 -> {
                    briCheckUi(false)
                }
                rgb_sbBrightness.progress == 1 -> {
                    sbBrightness_less.isEnabled = false
                    if (!Constant.IS_ROUTE_MODE)
                        when {
                            currentShowGroupSetPage -> group?.brightness = rgb_sbBrightness.progress
                            else -> light?.brightness = rgb_sbBrightness.progress
                        }
                    rgb_sbBrightness.progress = isZeroOrHundred(rgb_sbBrightness.progress)
                    sbBrightness_num.text = rgb_sbBrightness.progress.toString() + "%"

                    stopTracking = false
                    onBtnTouch = false
                }
                else -> {
                    sbBrightness_less.isEnabled = true
                    stopTracking = true
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler_less = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            rgb_white_seekbar.progress--
            when {
                rgb_white_seekbar.progress < 1 -> {
                    briCheckUi(false)
                    sb_w_bright_num.text = "1%"
                }
                rgb_white_seekbar.progress == 1 -> {
                    sb_w_bright_less.isEnabled = false
                    sb_w_bright_num.text = rgb_white_seekbar.progress.toString() + "%"
                    stopTracking = false
                    onBtnTouch = false
                }
                else -> {
                    sb_w_bright_less.isEnabled = true
                    stopTracking = true
                }
            }
        }
    }

    private fun briCheckUi(b: Boolean) {
        rgb_sbBrightness.isEnabled = b
        sbBrightness_add.isEnabled = b
        sbBrightness_less.isEnabled = b
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        lastUser?.let {
            if (it.id.toString() == it.last_authorizer_user_id)
                if (currentShowGroupSetPage) {
                    //  menuInflater.inflate(R.menu.menu_rgb_group_setting, menu)
                    //toolbar.menu?.findItem(R.id.toolbar_batch_gp)?.isVisible = false
                    // toolbar.menu?.findItem(R.id.toolbar_delete_device)?.isVisible = false
                } else {
                    menuInflater.inflate(R.menu.menu_rgb_light_setting, menu)
                    findItem = menu?.findItem(R.id.toolbar_f_version)
                    fiChangeGp = menu?.findItem(R.id.toolbar_fv_change_group)
                    fiChangeGp?.isVisible = true
                    findItem!!.title = localVersion
                }
        }
        return super.onCreateOptionsMenu(menu)
    }

    private val clickListener = OnClickListener { v ->
        when (v.id) {
            R.id.btn_remove_group -> removeGroup()
            R.id.btn_rename -> {
                renameGp()
            }
            R.id.img_header_menu_left -> finish()
            R.id.tvOta -> checkPermission()
            // R.id.update_group -> updateGroup()
            // R.id.btn_remove -> remove()
            R.id.dynamic_rgb -> {
                val lastUser = lastUser
                lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showLong(getString(R.string.author_region_warm))
                    else
                        toRGBGradientView()
                }
            }
            R.id.normal_rgb -> toNormalView()
            R.id.tvRename -> renameLight()
            R.id.ll_r -> {
                var dialog = InputRGBColorDialog(this, R.style.Dialog, color_r.text.toString(), color_g.text.toString(), color_b.text.toString(), InputRGBColorDialog.RGBColorListener { red, green, blue ->
                    redColor = red.toInt()
                    greenColor = green.toInt()
                    blueColor = blue.toInt()

                    setColorPicker()
                })
                dialog.show()
                dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

            }
            R.id.mode_diy_layout -> {//自定义模式
                changeToDiyPage()
            }
            R.id.mode_preset_layout -> {//内置模式按钮
                changeToBuildInPage()
            }
            R.id.main_go_help -> {
                seeHelpe("#color-light")
            }
            R.id.btnAdd -> transAddAct()
            R.id.main_add_device -> transAddAct()

            R.id.ll_g -> {
                var dialog = InputRGBColorDialog(this, R.style.Dialog, color_r.text.toString(), color_g.text.toString(), color_b.text.toString(), InputRGBColorDialog.RGBColorListener { red, green, blue ->
                    redColor = red.toInt()
                    greenColor = green.toInt()
                    blueColor = blue.toInt()
                    /**
                     * 设置颜色选择器颜色处
                     */
                    setColorPicker()

                })
                dialog.show()
                dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

            }
            R.id.ll_b -> {
                var dialog = InputRGBColorDialog(this, R.style.Dialog, color_r.text.toString(), color_g.text.toString(), color_b.text.toString(), InputRGBColorDialog.RGBColorListener { red, green, blue ->
                    redColor = red.toInt()
                    greenColor = green.toInt()
                    blueColor = blue.toInt()

                    setColorPicker()
                })
                dialog.show()

                dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            }
        }
    }

    private fun changeToBuildInPage() {
        if (isPresetMode) {
            buildInButton.setTextColor(resources.getColor(R.color.blue_background))
            buildInButton_image.setImageResource(R.drawable.icon_selected_rgb)
            builtInModeRecycleView.visibility = View.VISIBLE
            isPresetMode = false
        } else {
            buildInButton.setTextColor(resources.getColor(R.color.black_three))
            buildInButton_image.setImageResource(R.drawable.icon_unselected_rgb)
            builtInModeRecycleView.visibility = View.GONE
            isPresetMode = true
        }
        applyPresetView()
    }

    private fun applyPresetView() {
        setColorMode()
        setDiyColorMode()
    }

    private var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { _, view, position ->
        clickPostion = position
        clickDiy = false
        when (view!!.id) {
            R.id.gradient_mode_on -> {
                applyModeGradient(position)
            }

            R.id.gradient_mode_off -> {
                positionStateOff = position + 1
                if (!Constant.IS_ROUTE_MODE) {
                    stopGradient()
                    systemGradientStop()
                } else {
                    routerGradientStop("stopGradient")
                }
            }

            R.id.gradient_mode_set -> {
                speed = postionAndNum?.speed ?: 1
                var dialog = SpeedDialog(this, speed, R.style.Dialog, SpeedDialog.OnSpeedListener {
                    //if (!Constants.IS_ROUTE_MODE)
                    GlobalScope.launch {//速度是全局的 不发送命令保存速度
                        speed = it
                        // stopGradient()
                        delay(200)
                        positionState = position + 1
                        postionAndNum?.speed = speed
                        if (positionState == (position + 1))
                            if (Constant.IS_ROUTE_MODE)
                                routerSystemGradientApply(buildInModeList[clickPostion].id + 1, speed, "systemApply")
                            else {
                                Commander.applyGradient(dstAddress, positionState, speed, firstLightAddress)
                            }
                    }
                    if (!Constant.IS_ROUTE_MODE)
                        systemGradientApply(clickPostion)
                })
                dialog.show()
            }
        }

    }

    private fun applyModeGradient(position: Int) {
        isPresetMode = true
        isDiyMode = false
        //应用内置渐变
        if (clickPostion != 100)
            if (!Constant.IS_ROUTE_MODE) {
                applyDisposable?.dispose()
                applyDisposable = Observable.timer(50, TimeUnit.MILLISECONDS, Schedulers.io())
                        .subscribe {
                            GlobalScope.launch {
                                for (i in 0..2) {
                                    stopGradient()
                                    delay(50)
                                }
                            }
                            positionState = position + 1
                            Commander.applyGradient(dstAddress, positionState, speed, firstLightAddress)
                        }
                postionAndNum?.position = position
                systemGradientApply(clickPostion)
            } else {
                routerSystemGradientApply(buildInModeList[clickPostion].id + 1, speed, "systemApply")
            }
    }

    private fun systemGradientStop() {
        for (i in buildInModeList!!.indices)
            buildInModeList!![i].select = false

        rgbGradientAdapter!!.notifyDataSetChanged()

        for (i in diyGradientList!!.indices) {
            diyGradientList!![i].select = false
            DBUtils.updateGradient(diyGradientList!![i])
        }
        rgbDiyGradientAdapter!!.notifyDataSetChanged()
    }

    private fun systemGradientApply(position: Int) {
        for (i in buildInModeList!!.indices) {
            buildInModeList!![i].select = i == position
        }

        rgbGradientAdapter!!.notifyDataSetChanged()

        for (i in diyGradientList!!.indices) {
            diyGradientList!![i].select = false
            DBUtils.updateGradient(diyGradientList!![i])
        }
        rgbDiyGradientAdapter!!.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodeNum) {
            var group = data?.getSerializableExtra(Constant.EIGHT_SWITCH_TYPE) as DbGroup
            if (light != null)
                if (Constant.IS_ROUTE_MODE) {
                    val bodyBean = GroupBodyBean(mutableListOf(light!!.meshAddr), light!!.productUUID, "rgbGp", group.meshAddr)
                    routerChangeGpDevice(bodyBean)
                } else
                    updateGroupResult(light!!, group)
        } else {
            diyGradientList.clear()
            diyGradientList.addAll(DBUtils.diyGradientList)

            isDelete = false
            rgbDiyGradientAdapter!!.changeState(isDelete)
            toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
            toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.VISIBLE
            toolbar!!.title = getString(R.string.dynamic_gradient)
            rgbDiyGradientAdapter!!.notifyDataSetChanged()
            setDate()
            /*  when {
                  isDiyMode -> applyDiyGradient(clickPostion)
                  else -> applyModeGradient(clickPostion)
              }*/
            /*  if (clickPostion != 100 && diyPosition != 100) {
                  diyGradientList!![diyPosition].isSelect = false//开关状态
                  diyGradientList!![clickPostion].isSelect = true//开关状态
                  diyPosition = clickPostion
              } else {
                  if (diyPosition != 100)
                      diyGradientList!![diyPosition].isSelect = true//开关状态
              }*/
            //isExitGradient = false
            //isDiyMode = true
            /* changeToDiyPage()

             //应用自定义渐变
             if (diyPosition != 100)
                 GlobalScope.launch {
                     stopGradient()
                     delay(200)
                   ///  Commander.applyDiyGradient(dstAddress, diyGradientList!![diyPosition].id.toInt(), diyGradientList!![diyPosition].speed, firstLightAddress)
                 }*/
        }
    }

    @SuppressLint("StringFormatMatches")
    override fun tzRouterGroupResult(bean: RouteGroupingOrDelBean?) {
        if (bean?.ser_id == "rgbGp") {
            LogUtils.v("zcl-----------收到路由普通灯分组通知-------$bean")
            disposableRouteTimer?.dispose()
            if (bean?.finish) {
                hideLoadingDialog()
                when (bean?.status) {
                    -1 -> ToastUtils.showShort(getString(R.string.group_failed))
                    0, 1 -> {
                        if (bean?.status == 0) ToastUtils.showShort(getString(R.string.grouping_success_tip)) else ToastUtils.showShort(getString(R.string.group_some_fail))
                        SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, object : SyncCallback {
                            override fun start() {}
                            override fun complete() {}
                            override fun error(msg: String?) {}
                        })
                    }
                }
            }
        }
    }

    private fun setDate() {
        for (i in diyGradientList!!.indices)
            if (diyGradientList!![i].isSelected)
                diyGradientList!![i].isSelected = false
    }

    private fun changeToDiyPage() {
        if (isDiyMode) {
            diyButton.setTextColor(resources.getColor(R.color.blue_background))
            diyButton_image.setImageResource(R.drawable.icon_selected_rgb)
            builtDiyModeRecycleView.visibility = View.VISIBLE
            isDiyMode = false
        } else {
            diyButton.setTextColor(resources.getColor(R.color.black_three))
            diyButton_image.setImageResource(R.drawable.icon_unselected_rgb)
            builtDiyModeRecycleView.visibility = View.VISIBLE
            isDiyMode = true
        }
        applyDiyView()
    }

    private fun applyDiyView() {
        builtDiyModeRecycleView!!.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rgbDiyGradientAdapter = RGBDiyGradientAdapter(R.layout.diy_gradient_item, diyGradientList, isDelete)
        builtDiyModeRecycleView?.itemAnimator = DefaultItemAnimator()
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.black_ee)))
        //添加分割线
        builtDiyModeRecycleView?.addItemDecoration(decoration)
        rgbDiyGradientAdapter!!.onItemChildClickListener = onItemChildClickListenerDiy
        rgbDiyGradientAdapter!!.onItemLongClickListener = this.onItemChildLongClickListenerDiy
        rgbDiyGradientAdapter!!.bindToRecyclerView(builtDiyModeRecycleView)
    }

    private var onItemChildClickListenerDiy = BaseQuickAdapter.OnItemChildClickListener { _, view, position ->
        clickPostion = position
        clickDiy = true
        when (view!!.id) {
            R.id.diy_mode_on -> {
                //应用自定义渐变
                applyDiyGradient(position)
            }

            R.id.diy_mode_off -> {
                if (!Constant.IS_ROUTE_MODE) {
                    Commander.closeGradient(dstAddress, diyGradientList!![clickPostion].id.toInt(), diyGradientList!![clickPostion].speed)
                    diyGradientCloseResult(clickPostion)
                } else
                    routerGradientStop("stopGradient")
            }

            R.id.diy_mode_set -> {
                val intent = Intent(this, SetDiyColorAct::class.java)
                intent.putExtra(Constant.IS_CHANGE_COLOR, true)
                intent.putExtra(Constant.GRADIENT_KEY, diyGradientList!![clickPostion])
                dstAddress = if (currentShowGroupSetPage) group!!.meshAddr else light!!.meshAddr
                intent.putExtra(Constant.TYPE_VIEW_ADDRESS, dstAddress)
                intent.putExtra(Constant.DEVICE_TYPE, deviceType)
                startActivityForResult(intent, 0)
            }

            R.id.diy_selected -> {
                diyGradientList!![clickPostion].isSelected = !diyGradientList!![clickPostion].isSelected
            }
        }

    }

    private fun applyDiyGradient(position: Int) {
        isPresetMode = false
        isDiyMode = true
        if (clickPostion != 100)
            if (!Constant.IS_ROUTE_MODE) {
                GlobalScope.launch {
                    stopGradient()
                    delay(200)
                    Commander.applyDiyGradient(dstAddress, diyGradientList!![clickPostion].id.toInt(),
                            diyGradientList!![position].speed, firstLightAddress)
                }
                diyOpenGradientResult(clickPostion)
            } else {
                routerDiyGradientApply(diyGradientList[clickPostion].id.toInt(), "diyModeApply")
            }
    }

    private fun diyOpenGradientResult(position: Int) {
        postionAndNum?.position = 100
        diyPosition = position
        diyGradientList!![position].select = true
        for (i in diyGradientList!!.indices) {
            if (i != position && diyGradientList!![i].select) {
                diyGradientList!![i].select = false
                DBUtils.updateGradient(diyGradientList!![i])
            }
        }
        rgbDiyGradientAdapter!!.notifyDataSetChanged()

        for (i in buildInModeList!!.indices)
            buildInModeList!![i].select = false
        rgbGradientAdapter!!.notifyDataSetChanged()
    }

    private fun diyGradientCloseResult(position: Int) {
        diyPosition = 100
        diyGradientList!![position].select = false
        rgbDiyGradientAdapter!!.notifyItemChanged(position)
        DBUtils.updateGradient(diyGradientList!![position])

        for (i in buildInModeList!!.indices)
            buildInModeList!![i].select = false

        rgbGradientAdapter!!.notifyDataSetChanged()
    }

    private var onItemChildLongClickListenerDiy = BaseQuickAdapter.OnItemLongClickListener { _, _, _ ->
        isDelete = true
        isExitGradient = true
        rgbDiyGradientAdapter!!.changeState(isDelete)
        refreshData()
        return@OnItemLongClickListener true
    }

    private fun refreshData() {
        rgbDiyGradientAdapter!!.notifyDataSetChanged()
        toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.VISIBLE
        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.GONE
        toolbar!!.title = ""

        deleteDiyGradient()
    }

    private fun deleteDiyGradient() {
        var batchGroup = toolbar.findViewById<ImageView>(R.id.img_function2)

        batchGroup.setOnClickListener {
            var listSize: ArrayList<DbDiyGradient>? = null
            listSize = ArrayList()

            for (i in diyGradientList!!.indices) {
                if (diyGradientList!![i].isSelected)
                    listSize.add(diyGradientList!![i])
            }

            if (listSize.size > 0) {
                val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
                builder.setMessage(getString(R.string.delete_model))
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    showLoadingDialog(resources.getString(R.string.delete))
                    if (Constant.IS_ROUTE_MODE) {
                        val mutableListOf = mutableListOf<Int>()
                        diyGradientList.forEach {
                            mutableListOf.add(it.id.toInt())
                        }
                        RouterModel.routerDelGradient(DelGradientBodyBean(mutableListOf, dstAddress, deviceType, "delGradient"))?.subscribe({
                            //    "errorCode": 90018,"该设备不存在，请重新刷新数据"    "errorCode": 90008, "以下路由没有上线，无法删除自定义渐变"   "errorCode": 90004, "账号下区域下没有路由，无法操作"
                            //    "errorCode": 90007, "该组不存在，无法操作"    "errorCode": 90005,"以下路由没有上线，无法删除自定义渐变"
                            LogUtils.v("zcl-----------收到路由删除渐变请求-------$it")
                            when (it.errorCode) {
                                0 -> {
                                    disposableTimer?.dispose()
                                    disposableTimer = Observable.timer(1500, TimeUnit.MILLISECONDS)
                                            .subscribe {
                                                ToastUtils.showShort(getString(R.string.del_gradient_fail))
                                                hideLoadingDialog()
                                            }
                                }
                                90018 -> {
                                    DBUtils.deleteLocalData()
                                    //ToastUtils.showShort(getString(R.string.device_not_exit))
                                    SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
                                    finish()
                                }
                                90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                                else -> ToastUtils.showShort(it.message)
                            }
                        }, {
                            ToastUtils.showShort(it.message)
                        })
                    } else {
                        bleDelGradient(true)
                    }
                }
                builder.setNeutralButton(R.string.cancel) { _, _ -> }
                builder.create().show()
            }
        }
    }

    private fun bleDelGradient(isSendCommend: Boolean) {
        for (i in diyGradientList!!.indices) {
            if (diyGradientList!![i].isSelected) {
                if (isSendCommend)
                    startDeleteGradientCmd(diyGradientList!![i].id)
                DBUtils.deleteGradient(diyGradientList!![i])
                DBUtils.deleteColorNodeList(DBUtils.getColorNodeListByDynamicModeId(diyGradientList!![i].id!!))
            }
        }
        diyGradientList = DBUtils.diyGradientList
        rgbDiyGradientAdapter!!.setNewData(diyGradientList)
        hideLoadingDialog()
    }

    private fun startDeleteGradientCmd(id: Long) {
        Commander.deleteGradient(dstAddress, id.toInt(), {}, {})
    }

    fun stopGradient() {
        Commander.closeGradient(dstAddress, positionStateOff, speed)
    }


    private fun transAddAct() {
        if (currentShowGroupSetPage) {
            if (DBUtils.diyGradientList.size < 6) {
                val intent = Intent(this, SetDiyColorAct::class.java)
                intent.putExtra(Constant.IS_CHANGE_COLOR, false)
                intent.putExtra(Constant.TYPE_VIEW_ADDRESS, group!!.meshAddr)
                intent.putExtra(Constant.DEVICE_TYPE, deviceType)
                startActivityForResult(intent, 0)
            } else {
                ToastUtils.showLong(getString(R.string.add_gradient_limit))
            }
        } else {
            if (DBUtils.diyGradientList.size < 6) {
                val intent = Intent(this, SetDiyColorAct::class.java)
                intent.putExtra(Constant.IS_CHANGE_COLOR, false)
                intent.putExtra(Constant.TYPE_VIEW_ADDRESS, light!!.meshAddr)
                intent.putExtra(Constant.DEVICE_TYPE, deviceType)
                startActivityForResult(intent, 0)
            } else {
                ToastUtils.showLong(getString(R.string.add_gradient_limit))
            }
        }

    }

    private fun toNormalView() {
        if (currentShowGroupSetPage) {
            toolbar.menu.setGroupVisible(0, true)
            toolbar.menu.setGroupVisible(1, true)
            rgb_diy.visibility = View.GONE
            rgb_set.visibility = View.VISIBLE
            dynamic_rgb.setTextColor(resources.getColor(R.color.black_nine))
            normal_rgb.setTextColor(resources.getColor(R.color.blue_background))
            if (group != null) {
                if (group!!.meshAddr == 0xffff) {
                    toolbarTv.text = getString(R.string.allLight)
                } else {
                    toolbarTv.text = group?.name
                }
            }

            if (isDelete) {
                isDelete = false
                rgbDiyGradientAdapter!!.changeState(isDelete)
                rgbDiyGradientAdapter!!.notifyDataSetChanged()
                setDate()
            }
            toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
            toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.VISIBLE
            isDiyMode = false
            changeToDiyPage()
        } else {
            //toolbar.menu.setGroupVisible(0, true)
            //toolbar.menu.setGroupVisible(1, true)
            //toolbar.menu.setGroupVisible(2, true)
            //toolbar.menu.setGroupVisible(3, true)
            rgb_diy.visibility = View.GONE
            rgb_set.visibility = View.VISIBLE
            dynamic_rgb.setTextColor(resources.getColor(R.color.black_nine))
            normal_rgb.setTextColor(resources.getColor(R.color.blue_background))
            toolbarTv.text = light?.name
            if (isDelete) {
                isDelete = false
                rgbDiyGradientAdapter!!.changeState(isDelete)
                rgbDiyGradientAdapter!!.notifyDataSetChanged()
                setDate()
            }
            toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
            toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.VISIBLE
            isDiyMode = false
            //changeToDiyPage()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setColorPicker() {
        val r = redColor!!
        val g = greenColor!!
        val b = blueColor!!
        color_r?.text = r.toString()
        color_g?.text = g.toString()
        color_b?.text = b.toString()
        //两个0 是一个字节 也就是8bit 右移8
        var w: Int
        w = when {
            currentShowGroupSetPage -> if (group == null)
                50
            else
                (group!!.color and 0xff000000.toInt()) shr 24
            else -> if (light == null)
                50
            else
                (light!!.color and 0xff000000.toInt()) shr 24
        }


        var color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b

        var ws = (color and 0xff000000.toInt()) shr 24//白色

        val red = (color and 0xff0000) shr 16
        val green = (color and 0x00ff00) shr 8
        val blue = color and 0x0000ff
        color_picker.setInitialColor((color and 0xffffff) or 0xff000000.toInt())
        var showBrightness = when {
            currentShowGroupSetPage -> group?.brightness ?: 1
            else -> light?.brightness ?: 1
        }

        showBrightness = isZeroOrHundred(showBrightness)
        ws = isZeroOrHundred(ws)
        w = isZeroOrHundred(w)


        GlobalScope.launch {
            // 使用协程替代thread看是否能解决溢出问题 delay想到与thread  所有内容要放入协程
            try {
                //sendWhiteAndBri(ws, w)
                changeColor(red, green, blue, true)
                if (!Constant.IS_ROUTE_MODE)
                    if (currentShowGroupSetPage) {
                        group?.brightness = showBrightness
                        group?.color = color
                    } else {
                        light?.brightness = showBrightness
                        light?.color = color
                    }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        //亮度
        rgb_sbBrightness?.progress = showBrightness
        sbBrightness_num.text = "$showBrightness%"
        if (w != -1 && w >= 1) {//白光
            sb_w_bright_num.text = "$ws%"
            rgb_white_seekbar.progress = ws
        } else {
            sb_w_bright_num.text = "1%"
            rgb_white_seekbar.progress = 1
        }
        color_r?.text = red.toString()
        color_g?.text = green.toString()
        color_b?.text = blue.toString()
    }

    private suspend fun sendWhiteAndBri(whiteNUm: Int, bri: Int) {
        var addr: Int = if (currentShowGroupSetPage)
            group?.meshAddr!!
        else
            light?.meshAddr!!


        GlobalScope.launch {
            val opcodeWhite: Byte = Opcode.SET_W_LUM//设置白色
            val paramsWhite: ByteArray = byteArrayOf(whiteNUm.toByte())
            TelinkLightService.Instance()?.sendCommandNoResponse(opcodeWhite, addr, paramsWhite)
            delay(500)

            val opcodeBri: Byte = Opcode.SET_LUM//亮度bri
            val params: ByteArray = byteArrayOf(bri.toByte())
            TelinkLightService.Instance()?.sendCommandNoResponse(opcodeBri, addr, params)
            delay(500)
        }
    }


    private val menuItemClickListener = Toolbar.OnMenuItemClickListener { item ->
        when (item?.itemId) {
            R.id.toolbar_batch_gp -> removeGroup()
            R.id.toolbar_on_line -> renameGp()
            R.id.toolbar_f_rename -> renameLight()
            R.id.toolbar_f_delete -> remove()
            R.id.toolbar_fv_change_group -> updateGroup()
            R.id.toolbar_f_ota -> updateOTA()
        }
        true
    }

    private fun updateOTA() {
        if (findItem?.title != null && findItem?.title != " ") {
            checkPermission()
        } else {
            Toast.makeText(this, R.string.number_no, Toast.LENGTH_LONG).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                if (isDelete) {
                    isDelete = false
                    rgbDiyGradientAdapter!!.changeState(isDelete)
                    rgbDiyGradientAdapter!!.notifyDataSetChanged()
                    setDate()
                    toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.VISIBLE
                    isExitGradient = false
                    if (currentShowGroupSetPage) {
                        if (group != null) {
                            if (group!!.meshAddr == 0xffff) {
                                toolbarTv.text = getString(R.string.allLight)
                            } else {
                                toolbar!!.title = getString(R.string.dynamic_gradient)
                            }
                        }
                    } else {
                        toolbar!!.title = getString(R.string.dynamic_gradient)
                    }
                } else {
                    finish()
                }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        mScanTimeoutDisposal?.dispose()
        mConnectDisposal?.dispose()
        mScanTimeoutDisposal?.dispose()
        //移除事件
        mCheckRssiDisposal?.dispose()
        /*  暂时去掉 防止重新加回列表
         if (!isReset){
           if (currentShowGroupSetPage) {
               DBUtils.updateGroup(group!!)
               updateLights(group!!.color, "rgb_color", group!!)
           } else {
               DBUtils.updateLight(light!!)
               LogUtils.e("彩灯 onPasue更细后"+DBUtils.getAllRGBLight())
           }*/

    }


    private fun isSwitch(uuid: Int): Boolean {
        return when (uuid) {
            DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SENSOR, DeviceType.NIGHT_LIGHT -> true
            else -> false
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun initViewGroup() {
//        color_picker.isLongClickable = true
        this.color_picker!!.setOnTouchListener(this)
//        color_picker.isEnabled = true
        /*    rgb_switch.setOnCheckedChangeListener { _, isChecked ->
                if (group != null) {
                    when {
                        Constants.IS_ROUTE_MODE -> {//meshType普通灯 = 4 彩灯 = 6 连接器 = 5 组 = 97 0关1开
                            var status = if (isChecked) 1 else 0
                            routeOpenOrCloseBase(group!!.meshAddr, 97, status, "rgbSwitch")
                        }
                        else -> openOrClose(isChecked)
                    }
                }
            }*/
        dynamic_rgb.setOnClickListener(this.clickListener)
        ll_r.setOnClickListener(this.clickListener)
        ll_g.setOnClickListener(this.clickListener)
        ll_b.setOnClickListener(this.clickListener)
        normal_rgb.setOnClickListener(this.clickListener)
        btnAdd.setOnClickListener(this.clickListener)
        main_add_device?.setOnClickListener(clickListener)
        mode_preset_layout.setOnClickListener(this.clickListener)
        mode_diy_layout.setOnClickListener(this.clickListener)
        cb_white_enable.setOnClickListener(cbOnClickListener)
        cb_brightness_enable.setOnClickListener(cbOnClickListener)


        setColorMode()
        setDiyColorMode()

        sb_w_bright_add!!.setOnTouchListener { v, event ->
            addWhiteBright(event)
            true
        }
        sb_w_bright_less!!.setOnTouchListener { v, event ->
            lessWhiteBright(event)
            true
        }
        sbBrightness_add.setOnTouchListener { v, event ->
            addBrightness(event)
            true
        }
        sbBrightness_less.setOnTouchListener { v, event ->
            lessBrightness(event)
            true
        }


        presetColors = SharedPreferencesHelper.getObject(this, Constant.PRESET_COLOR) as? MutableList<ItemColorPreset>
        if (presetColors == null) {
            presetColors = java.util.ArrayList()
            for (i in 0 until 4) {
                val itemColorPreset = ItemColorPreset()
                itemColorPreset.color = OtherUtils.getCreateInitColor(i)
                presetColors?.add(itemColorPreset)
            }
        } else {
            if (presetColors!!.size > 4) {
                presetColors = presetColors!!.subList(0, 4)
            } else if (presetColors!!.size < 4) {
                for (i in 0 until 4) {
                    val itemColorPreset = ItemColorPreset()
                    itemColorPreset.color = OtherUtils.getCreateInitColor(i)
                    if (presetColors!!.size < 5)
                        presetColors?.add(itemColorPreset)
                    else
                        break
                }
            }
        }

        diy_color_recycler_list_view?.layoutManager = GridLayoutManager(this, 4)
        colorSelectDiyRecyclerViewAdapter = ColorSelectDiyRecyclerViewAdapter(R.layout.color_select_diy_item, presetColors)
        colorSelectDiyRecyclerViewAdapter?.onItemChildClickListener = diyOnItemChildClickListener
        colorSelectDiyRecyclerViewAdapter?.onItemChildLongClickListener = diyOnItemChildLongClickListener
        colorSelectDiyRecyclerViewAdapter?.bindToRecyclerView(diy_color_recycler_list_view)

        rgb_sbBrightness!!.progress = isZeroOrHundred(group!!.brightness)
        sbBrightness_num.text = rgb_sbBrightness!!.progress.toString() + "%"

        when {
            rgb_sbBrightness!!.progress >= 100 -> {
                sbBrightness_add.isEnabled = false
                sbBrightness_less.isEnabled = true
            }
            rgb_sbBrightness!!.progress <= 0 -> {
                sbBrightness_less.isEnabled = false
                sbBrightness_add.isEnabled = true
            }
            else -> {
                sbBrightness_less.isEnabled = true
                sbBrightness_add.isEnabled = true
            }
        }

        var w = ((group?.color ?: 1) and 0xff000000.toInt()) shr 24
        var r = Color.red(group?.color ?: 0)
        var g = Color.green(group?.color ?: 0)
        var b = Color.blue(group?.color ?: 0)

        w = isZeroOrHundred(w)
        sb_w_bright_num.text = "$w%"
        rgb_white_seekbar.progress = w

        color_r.text = r.toString()
        color_g.text = g.toString()
        color_b.text = b.toString()

        when {
            rgb_white_seekbar.progress >= 100 -> {
                sb_w_bright_add.isEnabled = false
                sb_w_bright_less.isEnabled = true
            }
            rgb_white_seekbar.progress <= 1 -> {
                sb_w_bright_less.isEnabled = false
                sb_w_bright_add.isEnabled = true
            }
            else -> {
                sb_w_bright_less.isEnabled = true
                sb_w_bright_add.isEnabled = true
            }
        }

        rgb_sbBrightness!!.setOnSeekBarChangeListener(barChangeListener)
        rgb_white_seekbar.setOnSeekBarChangeListener(barChangeListener)
        color_picker.reset()
        color_picker.subscribe(colorObserver)
        color_picker.setInitialColor((group?.color ?: 0 and 0xffffff) or 0xff000000.toInt())
        checkGroupIsSystemGroup()
        swLight(false)
    }

    private fun setDIyModeData() {
        buildInModeList.clear()
        val presetGradientList = resources.getStringArray(R.array.preset_gradient)
        presetGradientList.indices.forEach { i ->
            var item = ItemRgbGradient()
            item.name = presetGradientList[i]
            item.select = false /*i == postionAndNum?.position*///如果等于该postion则表示选中
            buildInModeList?.add(item)
        }
        LogUtils.v("zcl-----------添加完毕-------${buildInModeList.size}")
    }

    @SuppressLint("SetTextI18n")
    private var diyOnItemChildClickListener: BaseQuickAdapter.OnItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        val color = presetColors?.get(position)?.color
        var brightness = presetColors?.get(position)?.brightness
        var w = (color!! and 0xff000000.toInt()) shr 24
        val red = (color!! and 0xff0000) shr 16
        val green = (color and 0x00ff00) shr 8
        val blue = color and 0x0000ff

        color_picker.setInitialColor((color and 0xffffff) or 0xff000000.toInt())

        GlobalScope.launch {
            try {

                var addr: Int = when {
                    currentShowGroupSetPage -> group?.meshAddr!!
                    else -> light?.meshAddr!!
                }

                val opcode: Byte = Opcode.SET_LUM
                val opcodeW: Byte = Opcode.SET_W_LUM
                w = isZeroOrHundred(w)
                brightness = isZeroOrHundred(brightness ?: 0)

                val paramsW: ByteArray = byteArrayOf(w.toByte())
                val params: ByteArray = byteArrayOf(brightness!!.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcodeW, addr!!, paramsW)

                delay(80)
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr!!, params)

                delay(80)
                changeColor(red, green, blue, true)
                if (!Constant.IS_ROUTE_MODE)
                    if (currentShowGroupSetPage) {
                        group?.brightness = brightness!!
                        group?.color = color
                    } else {
                        light?.brightness = brightness!!
                        light?.color = color
                    }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }


        rgb_sbBrightness?.progress = brightness!!
        sb_w_bright_num.text = "$brightness%"
        sb_w_bright_num.text = "$w%"

        color_r?.text = red.toString()
        color_g?.text = green.toString()
        color_b?.text = blue.toString()
    }

    @SuppressLint("SetTextI18n")
    internal var diyOnItemChildLongClickListener: BaseQuickAdapter.OnItemChildLongClickListener = BaseQuickAdapter.OnItemChildLongClickListener { adapter, view, position ->
        if (currentShowGroupSetPage) {
            presetColors?.get(position)!!.color = group!!.color
            presetColors?.get(position)!!.brightness = group!!.brightness
        } else {
            presetColors?.get(position)!!.color = light!!.color
            presetColors?.get(position)!!.brightness = light!!.brightness
        }
        val textView = adapter.getViewByPosition(position, R.id.btn_diy_preset) as Dot?
        if (currentShowGroupSetPage) {
            try {
                textView?.setChecked(true, 0xff000000.toInt() or group!!.color)
                SharedPreferencesHelper.putObject(this, Constant.PRESET_COLOR, presetColors)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                textView?.setChecked(true, 0xff000000.toInt() or light!!.color)
                SharedPreferencesHelper.putObject(this, Constant.PRESET_COLOR, presetColors)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        true
    }

    private fun toRGBGradientView() {
        if (currentShowGroupSetPage) {
            toolbar.menu.setGroupVisible(0, false)
            toolbar.menu.setGroupVisible(1, false)
            rgb_diy.visibility = View.VISIBLE
            rgb_set.visibility = View.GONE
            normal_rgb.setTextColor(resources.getColor(R.color.black_nine))
            dynamic_rgb.setTextColor(resources.getColor(R.color.blue_background))
            toolbar!!.title = getString(R.string.dynamic_gradient)
            dstAddress = group!!.meshAddr
            deviceType = 97
            val lightList = DBUtils.getLightByGroupMesh(dstAddress)
            if (lightList != null && lightList.size > 0)
                firstLightAddress = lightList[0].meshAddr
        } else {
            // toolbar.menu.setGroupVisible(0, false)
            //toolbar.menu.setGroupVisible(1, false)
            // toolbar.menu.setGroupVisible(2, false)
            //toolbar.menu.setGroupVisible(3, false)
            rgb_diy.visibility = View.VISIBLE
            rgb_set.visibility = View.GONE
            normal_rgb.setTextColor(resources.getColor(R.color.black_nine))
            dynamic_rgb.setTextColor(resources.getColor(R.color.blue_background))
            toolbar!!.title = getString(R.string.dynamic_gradient)
            dstAddress = light!!.meshAddr
            deviceType = DeviceType.LIGHT_RGB
            firstLightAddress = dstAddress
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableTimer?.dispose()
        mConnectTimer?.dispose()
        mDisposable.dispose()
        mConnectDeviceDisposable?.dispose()
        acitivityIsAlive = false
        mScanDisposal?.dispose()
        val s = postionAndNum?.position.toString() + "-" + postionAndNum?.speed
        SharedPreferencesHelper.putString(this, POSIONANDNUM, s)
        LogUtils.e("zcl渐变设置保存" + postionAndNum.toString() + "---------" + s)
        if (TelinkLightApplication.getApp().connectDevice == null) {
            TelinkLightService.Instance()?.idleMode(true)
            LeBluetooth.getInstance().stopScan()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isExitGradient) {
                isExitGradient = false
                isDelete = false
                rgbDiyGradientAdapter!!.changeState(isDelete)
                rgbDiyGradientAdapter!!.notifyDataSetChanged()
                setDate()
                toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.VISIBLE
                toolbar!!.title = getString(R.string.dynamic_gradient)
            } else {
                setResult(Constant.RESULT_OK)
                finish()
            }
            return false
        } else {
            return super.onKeyDown(keyCode, event)
        }
    }

    private val barChangeListener = object : SeekBar.OnSeekBarChangeListener {
        private var preTime: Long = 0
        private val delayTime = if (Constant.IS_ROUTE_MODE) 300 else Constant.MAX_SCROLL_DELAY_VALUE

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            stopTracking = true
            this.onValueChange(seekBar, seekBar.progress, true)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            stopTracking = false
            this.preTime = System.currentTimeMillis()
            if (!Constant.IS_ROUTE_MODE)
                this.onValueChange(seekBar, seekBar.progress, true)
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            val currentTime = System.currentTimeMillis()

            onValueChangeView(seekBar, progress)
            if (currentTime - this.preTime > this.delayTime) {
                this.onValueChange(seekBar, progress, true)
                this.preTime = currentTime
            }
        }

        /**
         * 此处判断是白光还是亮度 使用view判断
         */
        @SuppressLint("SetTextI18n")
        private fun onValueChangeView(view: View, progress: Int) {
            var progressNoZero = when {
                progress <= 0 -> 1
                progress > 100 -> 100
                else -> progress
            }
            when {
                view === rgb_sbBrightness -> {
                    sbBrightness_num.text = "$progressNoZero%"
                    if (!Constant.IS_ROUTE_MODE)
                        light?.brightness = progressNoZero
                }
                view === rgb_white_seekbar -> {
                    sb_w_bright_num.text = "$progressNoZero%"
                    if (!Constant.IS_ROUTE_MODE)
                        light?.let {
                            //保存颜色数据
                            var w = (progressNoZero and 0xff000000.toInt()) shr 24
                            var r = Color.red(it.color)
                            var g = Color.green(it.color)
                            var b = Color.blue(it.color)
                            it.color = (w shl 24) or (r shl 16) or (g shl 8) or b
                            DBUtils.saveLight(it, false)
                        }
                }
            }
        }

        /**
         * 亮度白光监听
         */
        private fun onValueChange(view: View, progress: Int, immediate: Boolean) {
            var addr: Int = when {
                currentShowGroupSetPage -> group!!.meshAddr
                else -> light!!.meshAddr
            }

            var opcode: Byte
            var params: ByteArray
            var brightness = 1
            var w = 1
            sendProgress = progress
            when (view) {
                rgb_sbBrightness -> {
                    brightness = isZeroOrHundred(progress)
                    sendProgress = brightness
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(brightness.toByte())
                    if (!Constant.IS_ROUTE_MODE)
                        if (currentShowGroupSetPage) group!!.brightness = progress else light!!.brightness = progress
                    setBriAddOrMinusBtn(progress)
                    if (Constant.IS_ROUTE_MODE) {
                        routerConfigBrightnesssOrColorTemp(true)
                    } else {
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)
                        afterStopBri(progress)
                    }
                }
                rgb_white_seekbar -> {
                    opcode = Opcode.SET_W_LUM
                    w = isZeroOrHundred(progress)
                    sendProgress = w
                    params = byteArrayOf(w.toByte())
                    var color: Int = if (currentShowGroupSetPage) group?.color!! else light?.color!!
                    setWhiteAddOrMinusbtn(progress)
                    if (Constant.IS_ROUTE_MODE) {
                        var meshAddr = if (currentShowGroupSetPage) group?.meshAddr!! else light?.meshAddr!!
                        var meshType = when {
                            currentShowGroupSetPage -> 97
                            else -> light?.productUUID ?: 0
                        }
                        routeConfigWhiteGpOrLight(meshAddr, meshType, sendProgress, "rgbwhite")
                    } else {
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)
                        afterStopSendWhite(color, progress)
                    }
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    open fun routeConfigWhiteGpOrLight(meshAddr: Int, deviceType: Int, white: Int, serId: String) {
        LogUtils.v("zcl----------- zcl-----------发送路由调白色参数-------$white-------")
        var gpColor = if (currentShowGroupSetPage)
            DBUtils.getGroupByID(meshAddr.toLong())?.color ?: 0
        else
            DBUtils.getLightByID(meshAddr.toLong())?.color ?: 0

        val red = (gpColor and 0xff0000) shr 16
        val green = (gpColor and 0x00ff00) shr 8
        val blue = gpColor and 0x0000ff

        color = (white shl 24) or (red shl 16) or (green shl 8) or blue
        RouterModel.routeConfigWhiteNum(meshAddr, deviceType, color, serId)?.subscribe({
            //    "errorCode": 90018"该设备不存在，请重新刷新数据"    "errorCode": 90008,"该设备没有绑定路由，无法操作"
            //    "errorCode": 90007,"该组不存在，请重新刷新数据    "errorCode": 90005"message": "该设备绑定的路由没在线"
            when (it.errorCode) {
                0 -> {
                    //showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.config_white_fail))
                            }
                }
                90018 -> {
                    DBUtils.deleteLocalData()
                    //ToastUtils.showShort(getString(R.string.device_not_exit))
                    SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
                    finish()
                }
                90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                else -> ToastUtils.showShort(it.message)
            }

        }) {
            ToastUtils.showShort(it.message)
        }
    }

    @SuppressLint("CheckResult")
    private fun routerSystemGradientApply(id: Int, speed: Int, serId: String) {
        /**
         * id	是	int	自定义渐变id
         * ser_id	是	string	app会话id，推送时回传
         * meshAddr	是	int	目标meshAddr
         * meshType	是	int	mesh地址类型   meshType 彩灯 = 6 组 = 97
         */
        var meshType = if (currentShowGroupSetPage) 97 else 6
        var meshAddr = if (currentShowGroupSetPage) group!!.meshAddr else light!!.meshAddr
        val subscribe = RouterModel.routerApplySystemGradient(id, meshAddr, meshType, speed, serId)?.subscribe({
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.gradient_apply_fail))
                            }
                }
                90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                90018 -> {
                    DBUtils.deleteLocalData()
                    //ToastUtils.showShort(getString(R.string.device_not_exit))
                    SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
                    finish()
                }
                90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    private fun routerDiyGradientApply(id: Int, serId: String) {
        /**
         * id	是	int	自定义渐变id
         * ser_id	是	string	app会话id，推送时回传
         * meshAddr	是	int	目标meshAddr
         * meshType	是	int	mesh地址类型   meshType 彩灯 = 6 组 = 97
         */
        var meshType = if (currentShowGroupSetPage) 97 else 6
        var meshAddr = if (currentShowGroupSetPage) group!!.meshAddr else light!!.meshAddr
        val subscribe = RouterModel.routerApplyDiyGradient(id, meshAddr, meshType, serId)?.subscribe({
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.gradient_apply_fail))
                            }
                }
                90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                90018 -> {
                    DBUtils.deleteLocalData()
                    //ToastUtils.showShort(getString(R.string.device_not_exit))
                    SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
                    finish()
                }
                90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    private fun routerGradientStop(serId: String) {
        /**
         * id	是	int	自定义渐变id
         * ser_id	是	string	app会话id，推送时回传
         * meshAddr	是	int	目标meshAddr
         * meshType	是	int	mesh地址类型   meshType 彩灯 = 6 组 = 97
         */
        var meshType = if (currentShowGroupSetPage) 97 else 6
        var meshAddr = if (currentShowGroupSetPage) group!!.meshAddr else light!!.meshAddr
        val subscribe = RouterModel.routeStopDynamic(meshAddr, meshType, serId)?.subscribe({
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.gradient_stop_fail))
                            }
                }
                90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                90018 -> {
                    DBUtils.deleteLocalData()
                    //ToastUtils.showShort(getString(R.string.device_not_exit))
                    SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
                    finish()
                }
                90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    override fun tzRouterSysGradientApply(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl------收到路由应用系统渐变通知------------$cmdBean--")
        if (cmdBean.ser_id == "systemApply") {
            disposableRouteTimer?.dispose()
            when (cmdBean.status) {
                0 -> {
                    systemGradientApply(clickPostion)
                    hideLoadingDialog()
                }
                else -> {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.gradient_apply_fail))
                }
            }
        }
    }

    override fun tzRouterGradientStop(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl------收到路由应用渐变通知------------$cmdBean--")
        if (cmdBean.ser_id == "stopGradient") {
            disposableRouteTimer?.dispose()
            when (cmdBean.status) {
                0 -> {
                    if (!clickDiy)
                        systemGradientStop()
                    else
                        diyGradientCloseResult(clickPostion)
                    hideLoadingDialog()
                }
                else -> {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.gradient_stop_fail))
                }
            }
        }
    }

    override fun tzRouterGradientApply(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl------收到路由应用渐变通知------------$cmdBean--")
        if (cmdBean.ser_id == "diyModeApply") {
            disposableRouteTimer?.dispose()
            when (cmdBean.status) {
                0 -> {
                    diyOpenGradientResult(clickPostion)
                    hideLoadingDialog()
                }
                else -> {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.gradient_apply_fail))
                }
            }
        }
    }

    override fun tzRouterConfigWhite(cmdBean: CmdBodyBean) {
        disposableRouteTimer?.dispose()
        if (cmdBean.ser_id == "rgbwhite") {
            LogUtils.v("zcl------收到路由配置白光灯通知------------$cmdBean---$color---$sendProgress")
            when (cmdBean.status) {
                0 -> {
                    afterStopSendWhite(color, sendProgress)
                    hideLoadingDialog()
                }
                else -> {
                    hideLoadingDialog()
                    var ws = if (currentShowGroupSetPage) (group?.color!! and 0xff000000.toInt()) shr 24 else (light?.color!! and 0xff000000.toInt()) shr 24
                    rgb_white_seekbar.progress = ws
                    //sb_w_bright_num.text="$ws%"
                    ToastUtils.showShort(getString(R.string.config_white_fail))
                }
            }
        }
    }

    private fun setBriAddOrMinusBtn(progress: Int) {
        when {
            currentShowGroupSetPage -> {
                when {
                    progress >= 100 -> {
                        sbBrightness_add.isEnabled = false
                        sbBrightness_less.isEnabled = true
                    }
                    progress <= 1 -> {
                        sbBrightness_less.isEnabled = false
                        sbBrightness_add.isEnabled = true
                    }
                    else -> {
                        sbBrightness_less.isEnabled = true
                        sbBrightness_add.isEnabled = true
                    }
                }
            }
            else -> {
                when {
                    progress >= 100 -> {
                        sbBrightness_add.isEnabled = false
                        sbBrightness_less.isEnabled = true
                    }
                    progress <= 1 -> {
                        sbBrightness_less.isEnabled = false
                        sbBrightness_add.isEnabled = true
                    }
                    else -> {
                        sbBrightness_less.isEnabled = true
                        sbBrightness_add.isEnabled = true
                    }
                }
            }
        }
    }

    private fun setWhiteAddOrMinusbtn(progress: Int) {
        when {
            currentShowGroupSetPage -> {
                when {
                    progress >= 100 -> {
                        sb_w_bright_add.isEnabled = false
                        sb_w_bright_less.isEnabled = true
                    }
                    progress <= 1 -> {
                        sb_w_bright_less.isEnabled = false
                        sb_w_bright_add.isEnabled = true
                    }
                    else -> {
                        sb_w_bright_add.isEnabled = true
                        sb_w_bright_less.isEnabled = true
                    }
                }
            }
            else -> {
                when {
                    progress >= 100 -> {
                        sb_w_bright_add.isEnabled = false
                        sb_w_bright_less.isEnabled = true
                    }
                    progress <= 1 -> {
                        sb_w_bright_less.isEnabled = false
                        sb_w_bright_add.isEnabled = true
                    }
                    else -> {
                        sb_w_bright_add.isEnabled = true
                        sb_w_bright_less.isEnabled = true
                    }
                }
            }
        }
    }

    private fun afterStopBri(progress: Int) {
        when {
            stopTracking -> {
                if (currentShowGroupSetPage) {
                    DBUtils.updateGroup(group!!)
                    updateLights(progress, "brightness", group!!)
                } else {
                    DBUtils.updateLight(light!!)
                }
            }
        }
    }

    private fun afterStopSendWhite(color: Int, progress: Int) {
        if (stopTracking) {
            val red = (color and 0xff0000) shr 16
            val green = (color and 0x00ff00) shr 8
            val blue = color and 0x0000ff
            if (currentShowGroupSetPage) {
                group?.color = (progress shl 24) or (red shl 16) or (green shl 8) or blue
            } else {
                light?.color = (progress shl 24) or (red shl 16) or (green shl 8) or blue
            }
            if (currentShowGroupSetPage) {
                DBUtils.updateGroup(group!!)
                updateLights(progress, "colorTemperature", group!!)
            } else {
                DBUtils.updateLight(light!!)
            }
        }
    }

    private fun updateLights(progress: Int, type: String, group: DbGroup) {
        Thread {
            var lightList: MutableList<DbLight> = ArrayList()

            if (group.meshAddr == 0xffff) {
                val list = DBUtils.groupList
                for (j in list.indices) {
                    lightList.addAll(DBUtils.getLightByGroupID(list[j].id))
                }
            } else {
                lightList = DBUtils.getLightByGroupID(group.id)
            }

            for (dbLight: DbLight in lightList) {
                when (type) {
                    "brightness" -> dbLight.brightness = progress
                    "colorTemperature" -> dbLight.colorTemperature = progress
                    "rgb_color" -> dbLight.color = progress
                }
                DBUtils.updateLight(dbLight)
            }
        }.start()
    }

    private val colorObserver = ColorObserver { color, fromUser ->
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val w = rgb_white_seekbar.progress

        val color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b

        color_r?.text = r.toString()
        color_g?.text = g.toString()
        color_b?.text = b.toString()
        Log.d("", "onColorSelected: " + Integer.toHexString(color))
        if (fromUser) {
            if (r == 0 && g == 0 && b == 0) {
            } else {
                if (!Constant.IS_ROUTE_MODE)
                    if (currentShowGroupSetPage) {
                        group?.color = color
                    } else {
                        light?.color = color
                    }
                changeColor(r, g, b, false)
            }
        }
    }

    private fun changeColor(r: Int, G: Int, B: Int, isOnceSet: Boolean) {
        var red = r.toByte()
        var green = G.toByte()
        var blue = B.toByte()

        var white = when {
            currentShowGroupSetPage -> if (group == null) 50 else (group!!.color and 0xff000000.toInt()) shr 24
            else -> if (light == null) 50 else light!!.color and 0xff000000.toInt() shr 24
        }

        var meshAddr: Int = if (!currentShowGroupSetPage) light?.meshAddr!! else group?.meshAddr!!
        var deviceType: Int = if (!currentShowGroupSetPage) light?.productUUID!! else 97
        color = (white shl 24) or (r shl 16) or (G shl 8) or B

        if (System.currentTimeMillis() - lastTime > 500)
            if (Constant.IS_ROUTE_MODE)//路由发送色盘之不用发送白光 亮度 色温等 白光在color内已经存在
                routerConfigRGBNum(meshAddr, deviceType, color)
            else
                CoroutineScope(Dispatchers.IO).launch {
                    val opcode = Opcode.SET_TEMPERATURE
                    //0x04 代表rgb
                    val params = byteArrayOf(0x04, red, green, blue)

                    val logStr = String.format("R = %x, G = %x, B = %x", red, green, blue)
                    Log.d("RGBCOLOR", logStr)
                    if (isOnceSet) {
                        delay(50)
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, meshAddr!!, params)
                    } else {
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, meshAddr!!, params)
                    }
                    if (Constant.IS_ROUTE_MODE)
                        routerConfigBrightnesssOrColorTemp(false)
                    lastTime = System.currentTimeMillis()
                    sendWhiteAndBri(rgb_white_seekbar.progress, rgb_sbBrightness.progress)
                }
    }

    @SuppressLint("CheckResult")
    private fun routerConfigRGBNum(meshAddr: Int, deviceType: Int, color: Int) {
        lastTime = System.currentTimeMillis()
        RouterModel.routeConfigRGBNum(meshAddr, deviceType, color, "setRGB")?.subscribe({
            LogUtils.v("zcl-----------收到路由调节色盘成功-------")
            when (it.errorCode) {
                0 -> {
                    // showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.congfig_rgb_fail))
                            }
                }
                90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                90018 -> {
                    DBUtils.deleteLocalData()
                    //ToastUtils.showShort(getString(R.string.device_not_exit))
                    SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
                    finish()
                }
                90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    //所有灯控分组暂标为系统默认分组不做修改处理
    private fun checkGroupIsSystemGroup() {
        if (group!!.meshAddr == 0xFFFF) {
        }
    }

    /**
     * 删除组，并且把组里的灯的组也都删除。
     */
    private fun deleteGroup(lights: MutableList<DbLight>, group: DbGroup, retryCount: Int = 0,
                            successCallback: () -> Unit, failedCallback: () -> Unit) {
        Thread {

            if (lights.count() != 0) {
                val maxRetryCount = 3
                if (retryCount <= maxRetryCount) {
                    val light = lights[0]
                    val lightMeshAddr = light.meshAddr
                    Commander.deleteGroup(lightMeshAddr,
                            successCallback = {
                                light.belongGroupId = DBUtils.groupNull!!.id//该等所在组
                                DBUtils.updateLight(light)
                                lights.remove(light)

                                //修改分组成功后删除场景信息。
                                deleteAllSceneByLightAddr(light.meshAddr)
                                Thread.sleep(100)
                                if (lights.count() == 0) {
                                    //所有灯都删除了分组
                                    DBUtils.deleteGroupOnly(group)
                                    this.runOnUiThread {
                                        successCallback.invoke()
                                    }
                                } else {
                                    //还有灯要删除分组
                                    deleteGroup(lights, group,
                                            successCallback = successCallback,
                                            failedCallback = failedCallback)
                                }
                            },
                            failedCallback = {
                                deleteGroup(lights, group, retryCount = retryCount + 1,
                                        successCallback = successCallback,
                                        failedCallback = failedCallback)
                            })
                } else {    //超过了重试次数
                    this.runOnUiThread {
                        failedCallback.invoke()
                    }
                }
            } else {
                DBUtils.deleteGroupOnly(group)
                this.runOnUiThread {
                    successCallback.invoke()
                }
            }
        }.start()

    }

    /**
     * 删除指定灯里的所有场景
     *
     * @param lightMeshAddr 灯的mesh地址
     */
    private fun deleteAllSceneByLightAddr(lightMeshAddr: Int) {
        val opcode = Opcode.SCENE_ADD_OR_DEL

        val params: ByteArray = byteArrayOf(0x00, 0xff.toByte())
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, lightMeshAddr, params)
    }

    private fun renameGp() {
        StringUtils.initEditTextFilter(renameEt)
        if (!TextUtils.isEmpty(group?.name))
            renameEt?.setText(group?.name)
        renameEt?.setSelection(renameEt?.text.toString().length)

        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }

        renameConfirm?.setOnClickListener {    // 获取输入框的内容
            if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                var name = renameEt?.text.toString().trim { it <= ' ' }
                var canSave = true
                val groups = DBUtils.allGroups
                for (i in groups.indices) {
                    if (groups[i].name == name) {
                        ToastUtils.showLong(TelinkLightApplication.getApp().getString(R.string.repeat_name))
                        canSave = false
                        break
                    }
                }

                if (canSave) {
                    group?.name = renameEt?.text.toString().trim { it <= ' ' }
                    DBUtils.updateGroup(group!!)
                    toolbarTv.text = group?.name
                    renameDialog.dismiss()
                }
            }
        }
    }

    private fun sendBrightnessMsg(brightness: Int) {
        val addr: Int = when {
            currentShowGroupSetPage -> group!!.meshAddr
            else -> light!!.meshAddr
        }
        if (Constant.IS_ROUTE_MODE) {
            sendProgress = brightness
            routerConfigBrightnesssOrColorTemp(true)
        } else {
            val params: ByteArray = byteArrayOf(brightness.toByte())
            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.SET_LUM, addr, params, true)
        }
    }

    private fun sendWhiteNum(color: Int) {
        val addr: Int = if (currentShowGroupSetPage) {
            group!!.meshAddr
        } else {
            light!!.meshAddr
        }
        if (Constant.IS_ROUTE_MODE) {
            if (currentShowGroupSetPage)
                routeConfigWhiteGpOrLight(group?.meshAddr ?: 0, 97, color, "rgbwhite")
            else
                routeConfigWhiteGpOrLight(light?.meshAddr ?: 0, (light?.productUUID ?: 0).toInt(), color, "rgbwhite")
        } else {
            val params: ByteArray = byteArrayOf(color.toByte())
            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.SET_W_LUM, addr, params, true)
        }

    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        v?.parent?.requestDisallowInterceptTouchEvent(true)
        return false
    }

    override fun tzRouterConfigRGB(cmdBean: CmdBodyBean) {

        hideLoadingDialog()
        if (cmdBean.ser_id == "setRGB") {
            LogUtils.v("zcl------收到路由调节色盘通知------------$cmdBean")
            disposableRouteTimer?.dispose()
            if (cmdBean.status == 0) {
                color_picker.setInitialColor((color and 0xffffff) or 0xff000000.toInt())
                if (currentShowGroupSetPage) {
                    group?.color = color
                    DBUtils.saveGroup(group!!, false)
                } else {
                    light?.color = color
                    DBUtils.saveLight(light!!, false)
                }
            } else {
                if (currentShowGroupSetPage)
                    color_picker.setInitialColor((group!!.color and 0xffffff) or 0xff000000.toInt())
                else
                    color_picker.setInitialColor((light!!.color and 0xffffff) or 0xff000000.toInt())
                ToastUtils.showShort(getString(R.string.congfig_rgb_fail))
            }
        }
    }

    override fun tzRouterConfigBriOrTemp(cmdBean: CmdBodyBean, isBri: Int) {
        LogUtils.v("zcl------收到路由配置亮度灯通知------------$cmdBean")
        disposableRouteTimer?.dispose()
        if (cmdBean.status == 0) {
            if (currentShowGroupSetPage) {
                when (isBri) {
                    0 -> group?.brightness = sendProgress
                    else -> group?.colorTemperature = sendProgress
                }
                if (group != null)
                    DBUtils.saveGroup(group!!, false)
            } else {
                when (isBri) {
                    0 -> light?.brightness = sendProgress
                    else -> light?.colorTemperature = sendProgress
                }
                DBUtils.saveLight(light!!, false)
            }
            afterStopBri(sendProgress)
            hideLoadingDialog()
        } else {
            hideLoadingDialog()
            if (currentShowGroupSetPage) {
                when (isBri) {
                    0 -> rgb_sbBrightness.progress = group?.brightness ?: 1
                    else -> rgb_sbBrightness.progress = group?.colorTemperature ?: 1
                }
                if (sendProgress == 0)
                    if (isBri == 0) {
                        cb_white_enable.isChecked = !cb_white_enable.isChecked
                        whiteCheckUi(cb_white_enable.isChecked)
                    } else {
                        cb_brightness_enable.isChecked = !cb_brightness_enable.isChecked
                    }
                setWhiteAddOrMinusbtn(group?.brightness ?: 1)//恢复UI原状
            } else {
                when (isBri) {
                    0 -> rgb_sbBrightness.progress = light?.brightness ?: 1
                    else -> rgb_sbBrightness.progress = light?.colorTemperature ?: 1
                }
                setWhiteAddOrMinusbtn(light?.brightness ?: 1)
            }
            when (isBri) {
                0 -> ToastUtils.showShort(getString(R.string.config_bri_fail))
                else -> ToastUtils.showShort(getString(R.string.config_color_temp_fail))
            }
        }
    }

    override fun tzRouterOpenOrClose(cmdBean: CmdBodyBean) {
        hideLoadingDialog()
        disposableRouteTimer?.dispose()
        when (cmdBean.ser_id) {
            "rgbSwitch" -> {
                LogUtils.v("zcl------收到路由开关灯通知--------$swOpen----$cmdBean")
                when (cmdBean.status) {
                    0 -> afterSendOpenOrClose(swOpen)
                    else -> ToastUtils.showShort(getString(R.string.open_faile))
                }
            }
        }
    }

    override fun tzRouterAddOrDelOrUpdateGradientRecevice(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由渐变相关通知------删除3010-$cmdBean")
        if (cmdBean.cmd == Cmd.tzRouteDelGradient)
            if (cmdBean.finish) {
                hideLoadingDialog()
                disposableTimer?.dispose()
                if (cmdBean.status == 0) {
                    bleDelGradient(false)
                } else {
                    ToastUtils.showShort(getString(R.string.del_gradient_fail))
                }
            }
    }

    override fun tzRouterResetFactory(cmdBean: CmdBodyBean) {
        if (cmdBean.ser_id == "rgbFactory") {
            LogUtils.v("zcl-----------收到路由恢复出厂得到通知-------$cmdBean")
            hideLoadingDialog()
            disposableRouteTimer?.dispose()
            if (cmdBean.status == 0)
                deleteData()
            else
                ToastUtils.showShort(getString(R.string.reset_factory_fail))
        }
    }

    override fun tzRouterUpdateVersionRecevice(routerVersion: RouteGetVerBean?) {
        if (routerVersion?.ser_id == "rgbVersion") {
            LogUtils.v("zcl-----------收到路由调节速度通知-------$routerVersion")
            hideLoadingDialog()
            disposableRouteTimer?.dispose()
            when (routerVersion.status) {
                0 -> {
                    SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
                    val light = DBUtils.getLightByMeshAddr(light!!.meshAddr)
                    updateVersion(light?.version)
                }
                else -> {
                    ToastUtils.showShort(getString(R.string.get_version_fail))
                }
            }
        }
    }

    override fun toString(): String {
        return "RGBSettingActivity(color=$color, sendProgress=$sendProgress, disposableTimer=$disposableTimer, deviceType=$deviceType, addr=$addr, lastTime=$lastTime, fiChangeGp=$fiChangeGp, findItem=$findItem, requestCodeNum=$requestCodeNum, mConnectDeviceDisposable=$mConnectDeviceDisposable, clickPostion=$clickPostion, postionAndNum=$postionAndNum, mApplication=$mApplication, stopTracking=$stopTracking, presetColors=$presetColors, colorSelectDiyRecyclerViewAdapter=$colorSelectDiyRecyclerViewAdapter, group=$group, mConnectTimer=$mConnectTimer, localVersion=$localVersion, light=$light, gpAddress=$gpAddress, fromWhere=$fromWhere, dataManager=$dataManager, mDisposable=$mDisposable, mConnectDevice=$mConnectDevice, POSIONANDNUM='$POSIONANDNUM', currentShowGroupSetPage=$currentShowGroupSetPage, isExitGradient=$isExitGradient, isDiyMode=$isDiyMode, isPresetMode=$isPresetMode, diyPosition=$diyPosition, isDelete=$isDelete, dstAddress=$dstAddress, firstLightAddress=$firstLightAddress, typeStr='$typeStr', speed=$speed, positionState=$positionState, buildInModeList=$buildInModeList, diyGradientList=$diyGradientList, rgbGradientAdapter=$rgbGradientAdapter, rgbDiyGradientAdapter=$rgbDiyGradientAdapter, applyDisposable=$applyDisposable, downTime=$downTime, thisTime=$thisTime, onBtnTouch=$onBtnTouch, tvValue=$tvValue, redColor=$redColor, greenColor=$greenColor, blueColor=$blueColor, mConnectDisposal=$mConnectDisposal, mScanDisposal=$mScanDisposal, mScanTimeoutDisposal=$mScanTimeoutDisposal, mCheckRssiDisposal=$mCheckRssiDisposal, acitivityIsAlive=$acitivityIsAlive, otaPrepareListner=$otaPrepareListner, cbOnClickListener=$cbOnClickListener, handler=$handler, handler_brightness_add=$handler_brightness_add, handler_brightness_less=$handler_brightness_less, handler_less=$handler_less, clickListener=$clickListener, onItemChildClickListener=$onItemChildClickListener, onItemChildClickListenerDiy=$onItemChildClickListenerDiy, onItemChildLongClickListenerDiy=$onItemChildLongClickListenerDiy, menuItemClickListener=$menuItemClickListener, diyOnItemChildClickListener=$diyOnItemChildClickListener, diyOnItemChildLongClickListener=$diyOnItemChildLongClickListener, barChangeListener=$barChangeListener, colorObserver=$colorObserver)"
    }

}