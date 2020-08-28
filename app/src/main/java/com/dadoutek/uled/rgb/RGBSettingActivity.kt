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
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbDiyGradient
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbLight
import com.dadoutek.uled.ota.OTAUpdateActivity
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
    private var addr: Int = 0
    private var lastTime: Long = 0
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
    private var mRxPermission: RxPermissions? = null
    private var mConnectDevice: DeviceInfo? = null
    val POSIONANDNUM = "POSIONANDNUM"
    private var currentShowGroupSetPage = true
    private var isExitGradient = false
    private var isDiyMode = true
    private var isPresetMode = true
    private var diyPosition: Int = 100
    private var isDelete = false
    private var dstAddress: Int = 0
    private var firstLightAddress: Int = 0
    var typeStr: String = Constant.TYPE_GROUP
    var speed = 50
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
    var isReset = false

    private fun removeGroup() {
        AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(this))
                .setMessage(getString(R.string.delete_group_confirm, group?.name))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    this.showLoadingDialog(getString(R.string.deleting))
                    deleteGroup(DBUtils.getLightByGroupID(group!!.id), group!!,
                            successCallback = {
                                this.hideLoadingDialog()
                                this.setResult(Constant.RESULT_OK)
                                LogUtils.e("zcl删除组后" + DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_RGB))
                                this.finish()
                            },
                            failedCallback = {
                                this.hideLoadingDialog()
                                ToastUtils.showLong(R.string.move_out_some_lights_in_group_failed)
                            })
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    fun remove() {
        AlertDialog.Builder(this).setMessage(getString(R.string.sure_delete_device2))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight != null && TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight.isConnected) {
                        val opcode = Opcode.KICK_OUT
                        showLoadingDialog(getString(R.string.please_wait))
                        val subscribe = Commander.resetDevice(light!!.meshAddr)
                                .subscribe({
                                    //deleteData()
                                }, {
                                    GlobalScope.launch(Dispatchers.Main) {
                                        /*   showDialogHardDelete?.dismiss()
                                         showDialogHardDelete = android.app.AlertDialog.Builder(this).setMessage(R.string.delete_device_hard_tip)
                                                 .setPositiveButton(android.R.string.ok) { _, _ ->
                                                     showLoadingDialog(getString(R.string.please_wait))
                                                     deleteData()
                                                 }
                                                 .setNegativeButton(R.string.btn_cancel, null)
                                                 .show()
                                         */
                                    }
                                })
                        deleteData()

                    } else {
                        ToastUtils.showLong(getString(R.string.bluetooth_open_connet))
                        this.finish()
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    fun deleteData() {
        hideLoadingDialog()
        DBUtils.deleteLight(light!!)
        isReset = true
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
            val instance = TelinkLightService.Instance()
            instance?.idleMode(true)
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
            finish()
        }, {
            ToastUtils.showShort(getString(R.string.grouping_fail))
        })
    }

    private fun checkPermission() {
        mDisposable.add(
                mRxPermission!!.request(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe { granted ->
                    isDirectConnectDevice(granted)
                })
    }

    private fun isDirectConnectDevice(granted: Boolean) {
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
        val intent = Intent(this@RGBSettingActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.UPDATE_LIGHT, light)
        intent.putExtra(Constant.OTA_MAC, light?.macAddr)
        intent.putExtra(Constant.OTA_MES_Add, light?.meshAddr)
        intent.putExtra(Constant.OTA_VERSION, light?.version)
        intent.putExtra(Constant.OTA_TYPE, DeviceType.LIGHT_RGB)
        startActivity(intent)
        finish()
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

        //lin = LayoutInflater.from(this).inflate(R.layout.template_add_help, null)
        main_add_device?.text = getString(R.string.mode_diy)
        main_go_help?.visibility = View.VISIBLE

        if (Constant.IS_OPEN_AUXFUN){
            ll_r.visibility = View.VISIBLE
            ll_g.visibility = View.VISIBLE
            ll_b.visibility = View.VISIBLE
        }else{
            ll_r.visibility = View.GONE
            ll_g.visibility = View.GONE
            ll_b.visibility = View.GONE
        }


        if (typeStr == Constant.TYPE_GROUP) {
            currentShowGroupSetPage = true
            initToolbarGroup()
            initDataGroup()
            initViewGroup()

            img_function1.setImageResource(R.drawable.icon_editor)
            img_function1.visibility = View.VISIBLE
            img_function1.setOnClickListener {
                renameGp()
            }
        } else {
            img_function1.visibility = View.GONE
            currentShowGroupSetPage = false
            initToolbar()
            initView()
            getVersion()
        }
    }

    @SuppressLint("CheckResult")
    private fun getVersion() {
        if (TelinkApplication.getInstance().connectDevice != null) {
            val subscribe = Commander.getDeviceVersion(light!!.meshAddr)
                    .subscribe({ s ->
                        if (!TextUtils.isEmpty(s)) {
                            localVersion = s
                            if (!TextUtils.isEmpty(localVersion)) {
                                light!!.version = localVersion
                                findItem?.title = localVersion
                            }
                            DBUtils.saveLight(light!!, false)
                        }
                        null
                    }, {
                        LogUtils.d(it)
                    })
        }
    }

    override fun onResume() {
        super.onResume()
        light?.let {
            rgb_switch.isChecked = it.connectionStatus == ConnectionStatus.ON.value
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun initView() {
        light = this.intent.extras!!.get(Constant.LIGHT_ARESS_KEY) as DbLight
        this.fromWhere = this.intent.getStringExtra(Constant.LIGHT_REFRESH_KEY)
        this.gpAddress = this.intent.getIntExtra(Constant.GROUP_ARESS_KEY, 0)
        dataManager = DataManager(this, mApplication!!.mesh.name, mApplication!!.mesh.password)
        tvRename.visibility = View.GONE
        toolbarTv.text = light?.name

        openOrClose(light?.connectionStatus == 1)

        rgb_switch.setOnCheckedChangeListener { _, isChecked ->
            if (light != null)
                openOrClose(isChecked)
        }

        localVersion = when {
            TextUtils.isEmpty(light!!.version) -> getString(R.string.number_no)
            else -> light!!.version
        }

        rgb_switch.isChecked = light!!.connectionStatus == ConnectionStatus.ON.value

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
        cb_brightness_enable.setOnClickListener(cbOnClickListener)
        cb_brightness_rgb_enable.setOnClickListener(cbOnClickListener)
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

        mRxPermission = RxPermissions(this)

        sbBrightness!!.max = 100

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

        val brightness = if (light!!.brightness >= 1) light!!.brightness else 1
        sbBrightness!!.progress = brightness
        sbBrightness_num!!.text = "$brightness%"

        when {
            sbBrightness!!.progress >= 100 -> {
                sbBrightness_add.isEnabled = false
                sbBrightness_less.isEnabled = true
            }
            sbBrightness!!.progress <= 1 -> {
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

        sb_w_bright_num.text = "$w%"
        sb_w_bright.progress = w

        when {
            sb_w_bright.progress >= 100 -> {
                sb_w_bright_add.isEnabled = false
                sb_w_bright_less.isEnabled = true
            }
            sb_w_bright.progress <= 1 -> {
                sb_w_bright_less.isEnabled = false
                sb_w_bright_add.isEnabled = true
            }
            else -> {
                sb_w_bright_less.isEnabled = true
                sb_w_bright_add.isEnabled = true
            }
        }

        color_picker.setInitialColor((light?.color ?: 1 and 0xffffff) or 0xff000000.toInt())
        sbBrightness!!.setOnSeekBarChangeListener(barChangeListener)
        sb_w_bright.setOnSeekBarChangeListener(barChangeListener)
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
        //rgbDiyGradientAdapter!!.onItemLongClickListener = this.onItemChildLongClickListenerDiy
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


    private fun openOrClose(currentLight: Boolean) {
        LogUtils.e("currentLight$currentLight")

        if (currentLight) {
            enableAllUI(true)
            if (typeStr == Constant.TYPE_GROUP) {
                addr = group!!.meshAddr
                group!!.connectionStatus = ConnectionStatus.ON.value
            } else {
                addr = light!!.meshAddr
                light!!.connectionStatus = ConnectionStatus.ON.value
            }

        } else {
            enableAllUI(false)
            if (typeStr == Constant.TYPE_GROUP) {
                addr = group!!.meshAddr
                group!!.connectionStatus = ConnectionStatus.OFF.value
            } else {
                addr = light!!.meshAddr
                light!!.connectionStatus = ConnectionStatus.OFF.value
            }

        }

        Thread {
            //Thread.sleep(300) // 延时3S  防止通信失败
            Commander.openOrCloseLights(addr, currentLight)
        }.start()


    }

    private val cbOnClickListener = OnClickListener {
        when (it.id) {
            R.id.cb_brightness_enable -> {
                if (cb_brightness_enable.isChecked) {
                    sb_w_bright.isEnabled = true
                    sb_w_bright_add.isEnabled = true
                    sb_w_bright_less.isEnabled = true
                    sendBrightnessMsg(sb_w_bright.progress)
                } else {
                    sb_w_bright.isEnabled = false
                    sb_w_bright_add.isEnabled = false
                    sb_w_bright_less.isEnabled = false
                    sendBrightnessMsg(0)
                }
            }
            R.id.cb_brightness_rgb_enable -> {
                if (cb_brightness_rgb_enable.isChecked) {
                    sbBrightness.isEnabled = true
                    sbBrightness_add.isEnabled = true
                    sbBrightness_less.isEnabled = true
                    sendColorMsg(sbBrightness.progress)
                } else {
                    sbBrightness.isEnabled = false
                    sbBrightness_add.isEnabled = false
                    sbBrightness_less.isEnabled = false
                    sendColorMsg(0)
                }
            }
        }
    }

    private fun enableAllUI(isEnabled: Boolean) {
        cb_brightness_enable.isClickable = isEnabled
        cb_brightness_rgb_enable.isClickable = isEnabled
        cb_brightness_rgb_enable.isChecked = isEnabled
        cb_brightness_enable.isChecked = isEnabled

        sb_w_bright.isEnabled = isEnabled
        sb_w_bright_add.isEnabled = isEnabled
        sb_w_bright_less.isEnabled = isEnabled

        sbBrightness.isEnabled = isEnabled
        sbBrightness_add.isEnabled = isEnabled
        sbBrightness_less.isEnabled = isEnabled

        cb_brightness_enable.isEnabled = isEnabled
        cb_brightness_rgb_enable.isEnabled = isEnabled

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
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            GlobalScope.launch {
                while (onBtnTouch) {
                    thisTime = System.currentTimeMillis()
                    if (thisTime - downTime >= 500) {
                        tvValue++
                        val msg = handler_brightness_less.obtainMessage()
                        msg.arg1 = tvValue
                        handler_brightness_less.sendMessage(msg)
                        Log.e("TAG_TOUCH", tvValue++.toString())
                        delay(100)
                    }
                }
            }

        } else if (event.action == MotionEvent.ACTION_UP) {
            onBtnTouch = false
            if (thisTime - downTime < 500) {
                tvValue++
                val msg = handler_brightness_less.obtainMessage()
                msg.arg1 = tvValue
                handler_brightness_less.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun addBrightness(event: MotionEvent?) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
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
                        delay(100)
                    }
                }
            }

        } else if (event.action == MotionEvent.ACTION_UP) {
            onBtnTouch = false
            if (thisTime - downTime < 500) {
                tvValue++
                val msg = handler_brightness_add.obtainMessage()
                msg.arg1 = tvValue
                handler_brightness_add.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun lessWhiteBright(event: MotionEvent?) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            GlobalScope.launch {
                while (onBtnTouch) {
                    thisTime = System.currentTimeMillis()
                    if (thisTime - downTime >= 500) {
                        tvValue++
                        val msg = handler_less.obtainMessage()
                        msg.arg1 = tvValue
                        handler_less.sendMessage(msg)
                        Log.e("TAG_TOUCH", tvValue++.toString())
                        delay(100)

                    }
                }
            }

        } else if (event.action == MotionEvent.ACTION_UP) {
            onBtnTouch = false
            if (thisTime - downTime < 500) {
                tvValue++
                val msg = handler_less.obtainMessage()
                msg.arg1 = tvValue
                handler_less.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun addWhiteBright(event: MotionEvent?) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            GlobalScope.launch {
                while (onBtnTouch) {
                    thisTime = System.currentTimeMillis()
                    if (thisTime - downTime >= 500) {
                        tvValue++
                        val msg = handler.obtainMessage()
                        msg.arg1 = tvValue
                        handler.sendMessage(msg)
                        Log.e("TAG_TOUCH", tvValue++.toString())
                        delay(100)
                    }
                }
            }
        } else if (event.action == MotionEvent.ACTION_UP) {
            onBtnTouch = false
            if (thisTime - downTime < 500) {
                tvValue++
                val msg = handler.obtainMessage()
                msg.arg1 = tvValue
                handler.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            sb_w_bright.progress++
            when {
                sb_w_bright.progress > 100 -> {
                    sb_w_bright_add.isEnabled = false
                    stopTracking = false
                    onBtnTouch = false
                }
                sb_w_bright.progress == 100 -> {
                    sb_w_bright_add.isEnabled = false
                    sb_w_bright_num.text = sb_w_bright.progress.toString() + "%"
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
            sbBrightness.progress++
            when {
                sbBrightness.progress > 100 -> {
                    sbBrightness_add.isEnabled = false
                    stopTracking = false
                    onBtnTouch = false
                }
                sbBrightness.progress == 100 -> {
                    sbBrightness_add.isEnabled = false
                    when {
                        currentShowGroupSetPage -> group?.brightness = sbBrightness.progress
                        else -> light?.brightness = sbBrightness.progress
                    }
                    sbBrightness_num.text = sbBrightness.progress.toString() + "%"
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
            sbBrightness.progress--
            when {
                sbBrightness.progress < 1 -> {
                    sbBrightness_less.isEnabled = false
                    stopTracking = false
                    onBtnTouch = false
                }
                sbBrightness.progress == 1 -> {
                    sbBrightness_less.isEnabled = false

                    when {
                        currentShowGroupSetPage -> group?.brightness = sbBrightness.progress
                        else -> light?.brightness = sbBrightness.progress
                    }

                    sbBrightness_num.text = sbBrightness.progress.toString() + "%"

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
            sb_w_bright.progress--
            when {
                sb_w_bright.progress < 1 -> {
                    sb_w_bright_less.isEnabled = false
                    stopTracking = false
                    onBtnTouch = false
                }
                sb_w_bright.progress == 1 -> {
                    sb_w_bright_less.isEnabled = false
                    sb_w_bright_num.text = sb_w_bright.progress.toString() + "%"
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


    private fun initToolbar() {
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
    }

    private fun initToolbarGroup() {
        toolbarTv.text = getString(R.string.select_group)
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        DBUtils.lastUser?.let {
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
                val lastUser = DBUtils.lastUser
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
            R.id.btnAdd, R.id.main_add_device -> {
                transAddAct()
            }
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
        when (view!!.id) {
            R.id.gradient_mode_on -> {
                //应用内置渐变
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

            R.id.gradient_mode_off -> {
                for (i in buildInModeList!!.indices) {
                    buildInModeList!![i].select = false
                }
                rgbGradientAdapter!!.notifyDataSetChanged()
                stopGradient()

                for (i in diyGradientList!!.indices) {
                    diyGradientList!![i].select = false
                    DBUtils.updateGradient(diyGradientList!![i])
                }
                rgbDiyGradientAdapter!!.notifyDataSetChanged()
            }

            R.id.gradient_mode_set -> {
                speed = postionAndNum?.speed ?: 0
                var dialog = SpeedDialog(this, speed, R.style.Dialog, SpeedDialog.OnSpeedListener {
                    GlobalScope.launch {
                        speed = it
                        stopGradient()
                        delay(200)
                        postionAndNum?.speed = speed
                        Commander.applyGradient(dstAddress, positionState, speed, firstLightAddress)
                    }
                })
                dialog.show()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodeNum) {
            var group = data?.getSerializableExtra(Constant.EIGHT_SWITCH_TYPE) as DbGroup
            if (light != null)
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
            if (clickPostion != 100 && diyPosition != 100) {
                diyGradientList!![diyPosition].isSelect = false//开关状态
                diyGradientList!![clickPostion].isSelect = true//开关状态
                diyPosition = clickPostion
            } else {
                if (diyPosition != 100)
                    diyGradientList!![diyPosition].isSelect = true//开关状态
            }
            isExitGradient = false
            isDiyMode = true
            changeToDiyPage()

            //应用自定义渐变
            if (diyPosition != 100)
                GlobalScope.launch {
                    stopGradient()
                    delay(200)
                    Commander.applyDiyGradient(dstAddress, diyGradientList!![diyPosition].id.toInt(),
                            diyGradientList!![diyPosition].speed, firstLightAddress)
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
        when (view!!.id) {
            R.id.diy_mode_on -> {
                postionAndNum?.position = 100
                //应用自定义渐变
                GlobalScope.launch {
                    stopGradient()
                    delay(200)
                    Commander.applyDiyGradient(dstAddress, diyGradientList!![position].id.toInt(),
                            diyGradientList!![position].speed, firstLightAddress)
                }

                diyPosition = position

                diyGradientList!![position].select = true

                for (i in diyGradientList!!.indices) {
                    if (i != position) {
                        if (diyGradientList!![i].select) {
                            diyGradientList!![i].select = false
                            DBUtils.updateGradient(diyGradientList!![i])
                        }
                    }
                }
                rgbDiyGradientAdapter!!.notifyDataSetChanged()

                for (i in buildInModeList!!.indices) {
                    buildInModeList!![i].select = false
                }
                rgbGradientAdapter!!.notifyDataSetChanged()
            }

            R.id.diy_mode_off -> {
                diyPosition = 100
                Commander.closeGradient(dstAddress, diyGradientList!![position].id.toInt(), diyGradientList!![position].speed)
                diyGradientList!![position].select = false
                rgbDiyGradientAdapter!!.notifyItemChanged(position)
                DBUtils.updateGradient(diyGradientList!![position])

                for (i in buildInModeList!!.indices) {
                    buildInModeList!![i].select = false
                }
                rgbGradientAdapter!!.notifyDataSetChanged()

            }

            R.id.diy_mode_set -> {
                val intent = Intent(this, SetDiyColorAct::class.java)
                intent.putExtra(Constant.IS_CHANGE_COLOR, true)
                intent.putExtra(Constant.GRADIENT_KEY, diyGradientList!![position])
                intent.putExtra(Constant.TYPE_VIEW_ADDRESS, dstAddress)
                startActivityForResult(intent, 0)
            }

            R.id.diy_selected -> {
                diyGradientList!![position].isSelected = !diyGradientList!![position].isSelected
            }
        }

    }

    var onItemChildLongClickListenerDiy = BaseQuickAdapter.OnItemLongClickListener { adapter, view, position ->
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
                if (diyGradientList!![i].isSelected) {
                    listSize.add(diyGradientList!![i])
                }
            }

            if (listSize.size > 0) {
                val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
                builder.setMessage(getString(R.string.delete_model))
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    showLoadingDialog(resources.getString(R.string.delete))
                    for (i in diyGradientList!!.indices) {
                        if (diyGradientList!![i].isSelected) {
                            startDeleteGradientCmd(diyGradientList!![i].id)
                            DBUtils.deleteGradient(diyGradientList!![i])
                            DBUtils.deleteColorNodeList(DBUtils.getColorNodeListByDynamicModeId(diyGradientList!![i].id!!))
                        }
                    }
                    diyGradientList = DBUtils.diyGradientList
                    rgbDiyGradientAdapter!!.setNewData(diyGradientList)
                    hideLoadingDialog()
                }
                builder.setNeutralButton(R.string.cancel) { dialog, which -> }
                builder.create().show()
            }
        }
    }

    private fun startDeleteGradientCmd(id: Long) {
        Commander.deleteGradient(dstAddress, id.toInt(), {}, {})
    }

    fun stopGradient() {
        Commander.closeGradient(dstAddress, positionState, speed)
    }


    private fun transAddAct() {
        if (currentShowGroupSetPage) {
            if (DBUtils.diyGradientList.size < 6) {
                val intent = Intent(this, SetDiyColorAct::class.java)
                intent.putExtra(Constant.IS_CHANGE_COLOR, false)
                intent.putExtra(Constant.TYPE_VIEW_ADDRESS, group!!.meshAddr)
                startActivityForResult(intent, 0)
            } else {
                ToastUtils.showLong(getString(R.string.add_gradient_limit))
            }
        } else {
            if (DBUtils.diyGradientList.size < 6) {
                val intent = Intent(this, SetDiyColorAct::class.java)
                intent.putExtra(Constant.IS_CHANGE_COLOR, false)
                intent.putExtra(Constant.TYPE_VIEW_ADDRESS, light!!.meshAddr)
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
            toolbar.menu.setGroupVisible(0, true)
            toolbar.menu.setGroupVisible(1, true)
            toolbar.menu.setGroupVisible(2, true)
            toolbar.menu.setGroupVisible(3, true)
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
                group!!.color and 0xff000000.toInt() shr 24
            else -> if (light == null)
                50
            else
                light!!.color and 0xff000000.toInt() shr 24
        }


        var color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b

        var ws = (color and 0xff000000.toInt()) shr 24

        val red = (color and 0xff0000) shr 16
        val green = (color and 0x00ff00) shr 8
        val blue = color and 0x0000ff

        color_picker.setInitialColor((color and 0xffffff) or 0xff000000.toInt())
        var showBrightness = when {
            currentShowGroupSetPage -> group?.brightness ?: 1
            else -> light?.brightness ?: 1
        }
        ws = if (ws > 0) ws else 1
        w = if (w > 0) w else 1

        var showW = ws

        GlobalScope.launch {
            // 使用协程替代thread看是否能解决溢出问题 delay想到与thread  所有内容要放入协程
            try {
                if (w > Constant.MAX_VALUE)
                    w = Constant.MAX_VALUE

                if (ws > Constant.MAX_VALUE)
                    ws = Constant.MAX_VALUE

                if (ws == -1) {
                    ws = 1
                    showW = 1
                }

                var addr: Int = if (currentShowGroupSetPage)
                    group?.meshAddr!!
                else
                    light?.meshAddr!!

                val opcode: Byte = Opcode.SET_LUM
                val opcodeW: Byte = Opcode.SET_W_LUM

                val paramsW: ByteArray = byteArrayOf(ws.toByte())
                val params: ByteArray = byteArrayOf(w.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcodeW, addr, paramsW)

                delay(80)
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
                delay(80)

                changeColor(red.toByte(), green.toByte(), blue.toByte(), true)

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
        showBrightness = if (showBrightness <= 1) 1 else showBrightness
        //亮度
        sbBrightness?.progress = showBrightness
        sbBrightness_num.text = "$showBrightness%"
        if (w != -1 && w >= 1) {//白光
            sb_w_bright_num.text = "$showW%"
            sb_w_bright.progress = showW
        } else {
            sb_w_bright_num.text = "1%"
            sb_w_bright.progress = 1
        }
        color_r?.text = red.toString()
        color_g?.text = green.toString()
        color_b?.text = blue.toString()
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

    private fun initDataGroup() {
        this.mApplication = this.application as TelinkLightApplication
        this.group = this.intent.extras!!.get("group") as DbGroup
        openOrClose(group?.connectionStatus == 1)
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun initViewGroup() {
        if (group != null) {
            if (group!!.meshAddr == 0xffff) {
                toolbarTv.text = getString(R.string.allLight)
            } else {
                toolbarTv.text = group?.name
            }
        }

//        color_picker.isLongClickable = true
        this.color_picker!!.setOnTouchListener(this)
//        color_picker.isEnabled = true

        rgb_switch.setOnCheckedChangeListener { _, isChecked ->
            if (group != null) {
                openOrClose(isChecked)
            }
        }
        rgb_switch.isChecked = group!!.connectionStatus == ConnectionStatus.ON.value

        dynamic_rgb.setOnClickListener(this.clickListener)
        ll_r.setOnClickListener(this.clickListener)
        ll_g.setOnClickListener(this.clickListener)
        ll_b.setOnClickListener(this.clickListener)
        normal_rgb.setOnClickListener(this.clickListener)
        btnAdd.setOnClickListener(this.clickListener)
        mode_preset_layout.setOnClickListener(this.clickListener)
        mode_diy_layout.setOnClickListener(this.clickListener)
        cb_brightness_enable.setOnClickListener(cbOnClickListener)
        cb_brightness_rgb_enable.setOnClickListener(cbOnClickListener)


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

        sbBrightness!!.progress = group!!.brightness
        sbBrightness_num.text = group!!.brightness.toString() + "%"

        when {
            sbBrightness!!.progress >= 100 -> {
                sbBrightness_add.isEnabled = false
                sbBrightness_less.isEnabled = true
            }
            sbBrightness!!.progress <= 0 -> {
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
        if (w == -1 || w < 1) {
            w = 1
        }
        sb_w_bright_num.text = "$w%"
        sb_w_bright.progress = w

        color_r.text = r.toString()
        color_g.text = g.toString()
        color_b.text = b.toString()

        when {
            sb_w_bright.progress >= 100 -> {
                sb_w_bright_add.isEnabled = false
                sb_w_bright_less.isEnabled = true
            }
            sb_w_bright.progress <= 1 -> {
                sb_w_bright_less.isEnabled = false
                sb_w_bright_add.isEnabled = true
            }
            else -> {
                sb_w_bright_less.isEnabled = true
                sb_w_bright_add.isEnabled = true
            }
        }

        sbBrightness!!.setOnSeekBarChangeListener(barChangeListener)
        sb_w_bright.setOnSeekBarChangeListener(barChangeListener)
        color_picker.reset()
        color_picker.subscribe(colorObserver)
        color_picker.setInitialColor((group?.color ?: 0 and 0xffffff) or 0xff000000.toInt())
        checkGroupIsSystemGroup()
    }

    private fun setDIyModeData() {
        buildInModeList.clear()
        val presetGradientList = resources.getStringArray(R.array.preset_gradient)
        presetGradientList.indices.forEach { i ->
            var item = ItemRgbGradient()
            item.name = presetGradientList[i]
            item.select = i == postionAndNum?.position//如果等于该postion则表示选中
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
        var showBrightness = if (brightness ?: 1 >= 1) brightness else 1
        var showW = if (w ?: 1 >= 1) w else 1

        GlobalScope.launch {
            try {
                if (brightness!! > Constant.MAX_VALUE) {
                    brightness = Constant.MAX_VALUE
                }
                if (w > Constant.MAX_VALUE) {
                    w = Constant.MAX_VALUE
                }
                if (w == -1 || w < 1) {
                    w = 1
                    showW = 1
                }

                var addr: Int = if (currentShowGroupSetPage) {
                    group?.meshAddr!!
                } else {
                    light?.meshAddr!!
                }

                val opcode: Byte = Opcode.SET_LUM
                val opcodeW: Byte = Opcode.SET_W_LUM
                w = if (w <= 1) 1 else w
                brightness = if (brightness!! < 1) 1 else brightness

                val paramsW: ByteArray = byteArrayOf(w.toByte())
                val params: ByteArray = byteArrayOf(brightness!!.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcodeW, addr!!, paramsW)

                delay(80)
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr!!, params)

                delay(80)
                changeColor(red.toByte(), green.toByte(), blue.toByte(), true)

                if (currentShowGroupSetPage) {
                    group?.brightness = showBrightness!!
                    group?.color = color
                } else {
                    light?.brightness = showBrightness!!
                    light?.color = color
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        showBrightness = if (showBrightness!! < 1) 1 else showBrightness
        showW = if (showW < 1) 1 else showW

        sbBrightness?.progress = showBrightness!!
        sb_w_bright_num.text = "$showBrightness%"
        if (w != -1 && w >= 1) {
            sb_w_bright_num.text = "$showW%"
            sb_w_bright.progress = showW
        } else {
            sb_w_bright_num.text = "1%"
            sb_w_bright.progress = 1
        }
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
            val lightList = DBUtils.getLightByGroupMesh(dstAddress)
            if (lightList != null)
                if (lightList.size > 0)
                    firstLightAddress = lightList[0].meshAddr

        } else {
            toolbar.menu.setGroupVisible(0, false)
            toolbar.menu.setGroupVisible(1, false)
            toolbar.menu.setGroupVisible(2, false)
            toolbar.menu.setGroupVisible(3, false)
            rgb_diy.visibility = View.VISIBLE
            rgb_set.visibility = View.GONE
            normal_rgb.setTextColor(resources.getColor(R.color.black_nine))
            dynamic_rgb.setTextColor(resources.getColor(R.color.blue_background))
            toolbar!!.title = getString(R.string.dynamic_gradient)
            dstAddress = light!!.meshAddr
            firstLightAddress = dstAddress
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
        private val delayTime = Constant.MAX_SCROLL_DELAY_VALUE

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            stopTracking = true
            this.onValueChange(seekBar, seekBar.progress, true)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            stopTracking = false
            this.preTime = System.currentTimeMillis()
            this.onValueChange(seekBar, seekBar.progress, true)
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                       fromUser: Boolean) {
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
            var progressNoZero = if (progress <= 0) 1 else progress
            if (view === sbBrightness) {
                sbBrightness_num.text = "$progressNoZero%"
                light?.brightness = progressNoZero
            } else if (view === sb_w_bright) {
                sb_w_bright_num.text = "$progressNoZero%"
                light?.let {
                    //保存颜色数据
                    var w = (progressNoZero and 0xff000000.toInt()) shr 24
                    var r = Color.red(it.color)
                    var g = Color.green(it.color)
                    var b = Color.blue(it.color)
                    it.color = (w shl 24) or (r shl 16) or (g shl 8) or b
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
            when (view) {
                sbBrightness -> {
                    brightness = when {
                        progress > Constant.MAX_VALUE -> Constant.MAX_VALUE
                        else -> /*if (progress>0) */progress
                    } /*else 1*/

                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(brightness.toByte())
                    when {
                        currentShowGroupSetPage -> {
                            group!!.brightness = progress
                            when {
                                group!!.brightness >= 100 -> {
                                    sbBrightness_add.isEnabled = false
                                    sbBrightness_less.isEnabled = true
                                }
                                group!!.brightness <= 1 -> {
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
                            light!!.brightness = progress
                            when {
                                light!!.brightness >= 100 -> {
                                    sbBrightness_add.isEnabled = false
                                    sbBrightness_less.isEnabled = true
                                }
                                light!!.brightness <= 1 -> {
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

                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)

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
                sb_w_bright -> {
                    opcode = Opcode.SET_W_LUM
                    w = if (progress > Constant.MAX_VALUE) {
                        Constant.MAX_VALUE
                    } else {
                        /* if (progress>0) */progress /*else 1*/
                    }
                    params = byteArrayOf(w.toByte())
                    var color: Int
                    when {
                        currentShowGroupSetPage -> {
                            color = group?.color!!
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
                            color = light?.color!!
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

                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)

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

    private fun reSetWhiteAndBrightness() {
        var opcode = Opcode.SET_LUM
        var opcodeW = Opcode.SET_W_LUM

        var addr: Int = if (currentShowGroupSetPage) {
            group!!.meshAddr
        } else {
            light!!.meshAddr
        }

        var brightness = sbBrightness.progress
        var params = byteArrayOf(brightness.toByte())
        GlobalScope.launch {
            delay(500)
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
            val w = sb_w_bright.progress
            var paramsW = byteArrayOf(w.toByte())
            delay(500)
            TelinkLightService.Instance()?.sendCommandNoResponse(opcodeW, addr, paramsW)
        }
    }

    private val colorObserver = ColorObserver { color, fromUser ->
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val w = sb_w_bright.progress

        val color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b

        color_r?.text = r.toString()
        color_g?.text = g.toString()
        color_b?.text = b.toString()
        Log.d("", "onColorSelected: " + Integer.toHexString(color))
        if (fromUser) {
            if (r == 0 && g == 0 && b == 0) {
            } else {
                if (currentShowGroupSetPage) {
                    group?.color = color
                } else {
                    light?.color = color
                }
                changeColor(r.toByte(), g.toByte(), b.toByte(), false)
            }
        }
    }

    private fun changeColor(R: Byte, G: Byte, B: Byte, isOnceSet: Boolean) {
        var red = R
        var green = G
        var blue = B

        var addr: Int = if (!currentShowGroupSetPage) {
            light?.meshAddr!!
        } else {
            group?.meshAddr!!
        }
        if (System.currentTimeMillis() - lastTime > 500)
            CoroutineScope(Dispatchers.IO).launch {
                val opcode = Opcode.SET_TEMPERATURE

                val params = byteArrayOf(0x04, red, green, blue)

                val logStr = String.format("R = %x, G = %x, B = %x", red, green, blue)
                Log.d("RGBCOLOR", logStr)
                if (isOnceSet) {
                    delay(50)
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr!!, params)
                } else {
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr!!, params)
                }
                lastTime = System.currentTimeMillis()
                reSetWhiteAndBrightness()
            }
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
        val addr: Int = if (currentShowGroupSetPage) {
            group!!.meshAddr
        } else {
            light!!.meshAddr
        }
        val params: ByteArray = byteArrayOf(brightness.toByte())

        TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.SET_W_LUM, addr, params, true)
    }

    private fun sendColorMsg(color: Int) {
        val addr: Int = if (currentShowGroupSetPage) {
            group!!.meshAddr
        } else {
            light!!.meshAddr
        }
        val params: ByteArray = byteArrayOf(color.toByte())

        TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.SET_W_LUM, addr, params, true)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        v?.parent?.requestDisallowInterceptTouchEvent(true)
        return false
    }
}
