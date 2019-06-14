package com.dadoutek.uled.rgb

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.*
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.LightGroupingActivity
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbDiyGradient
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_device_setting.*
import kotlinx.android.synthetic.main.activity_rgb_gradient.*
import kotlinx.android.synthetic.main.activity_rgb_group_setting.*
import kotlinx.android.synthetic.main.activity_rgb_group_setting.dynamic_rgb
import kotlinx.android.synthetic.main.activity_rgb_group_setting.normal_rgb
import kotlinx.android.synthetic.main.activity_rgb_setting.*
import kotlinx.android.synthetic.main.fragment_rgb_group_setting.*
import kotlinx.android.synthetic.main.fragment_rgb_group_setting.color_b
import kotlinx.android.synthetic.main.fragment_rgb_group_setting.color_g
import kotlinx.android.synthetic.main.fragment_rgb_group_setting.color_picker
import kotlinx.android.synthetic.main.fragment_rgb_group_setting.color_r
import kotlinx.android.synthetic.main.fragment_rgb_group_setting.diy_color_recycler_list_view
import kotlinx.android.synthetic.main.fragment_rgb_group_setting.ll_b
import kotlinx.android.synthetic.main.fragment_rgb_group_setting.ll_g
import kotlinx.android.synthetic.main.fragment_rgb_group_setting.ll_r
import kotlinx.android.synthetic.main.toolbar.*
import top.defaults.colorpicker.ColorObserver
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

class RGBSettingActivity : TelinkBaseActivity(), EventListener<String>, View.OnTouchListener {

    private var mApplication: TelinkLightApplication? = null
    private var stopTracking = false
    private var presetColors: MutableList<ItemColorPreset>? = null
    private var colorSelectDiyRecyclerViewAdapter: ColorSelectDiyRecyclerViewAdapter? = null
    private var group: DbGroup? = null
    private var mApp: TelinkLightApplication? = null
    private var mConnectTimer: Disposable? = null
    private var isLoginSuccess = false
    private var connectTimes = 0
    private var localVersion: String? = null
    private var light: DbLight? = null
    private var gpAddress: Int = 0
    private var fromWhere: String? = null
    private var dataManager: DataManager? = null
    private val mDisposable = CompositeDisposable()
    private var mRxPermission: RxPermissions? = null
    private val remove: Button? = null
    private val dialog: AlertDialog? = null
    private var manager: DataManager? = null
    private var mConnectDevice: DeviceInfo? = null
    private var currentShowGroupSetPage = true

    private var isDiyMode = true

    private var isPresetMode = true

    private var diyPosition: Int = 0

    private var isDelete = false

    private var dstAddress: Int = 0
    private var firstLightAddress: Int = 0
    private var currentShowIsDiy = false

    var type = Constant.TYPE_GROUP
    var speed = 50
    var positionState = 0

    private var buildInModeList: ArrayList<ItemRgbGradient>? = null
    private var diyGradientList: MutableList<DbDiyGradient>? = null
    private var rgbGradientAdapter: RGBGradientAdapter? = null
    private var rgbDiyGradientAdapter: RGBDiyGradientAdapter? = null
    private var applyDisposable: Disposable? = null

    internal var downTime: Long = 0//Button被按下时的时间
    internal var thisTime: Long = 0//while每次循环时的时间
    internal var onBtnTouch = false//Button是否被按下
    internal var tvValue = 0//TextView中的值

    private var redColor: Int? = null
    private var greenColor: Int? = null
    private var blueColor: Int? = null

    private var isTrue: Boolean = true

    fun removeGroup() {
        AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(this)).setMessage(R.string.delete_group_confirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    this.showLoadingDialog(getString(R.string.deleting))

                    deleteGroup(DBUtils.getLightByGroupID(group!!.id), group!!,
                            successCallback = {
                                this.hideLoadingDialog()
                                this.setResult(Constant.RESULT_OK)
                                this.finish()
                            },
                            failedCallback = {
                                this.hideLoadingDialog()
                                ToastUtils.showShort(R.string.move_out_some_lights_in_group_failed)
                            })
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    fun remove() {
        AlertDialog.Builder(Objects.requireNonNull<Activity>(this)).setMessage(R.string.delete_light_confirm)
                .setPositiveButton(android.R.string.ok) { dialog, which ->

                    if (TelinkLightService.Instance().adapter.mLightCtrl.currentLight != null && TelinkLightService.Instance().adapter.mLightCtrl.currentLight.isConnected) {
                        val opcode = Opcode.KICK_OUT
                        TelinkLightService.Instance().sendCommandNoResponse(opcode, light!!.meshAddr, null)
                        DBUtils.deleteLight(light!!)
                        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(light!!.meshAddr)) {
                            TelinkLightApplication.getApp().mesh.saveOrUpdate(this!!)
                        }
                        if (mConnectDevice != null) {
                            Log.d(this!!.javaClass.simpleName, "mConnectDevice.meshAddress = " + mConnectDevice!!.meshAddress)
                            Log.d(this!!.javaClass.simpleName, "light.getMeshAddr() = " + light!!.meshAddr)
                            if (light!!.meshAddr == mConnectDevice!!.meshAddress) {
                                this!!.setResult(Activity.RESULT_OK, Intent().putExtra("data", true))
                            }
                        }
                        this!!.finish()


                    } else {
                        ToastUtils.showLong("当前处于未连接状态，重连中。。。")
                        this!!.finish()
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    private fun updateGroup() {
        val intent = Intent(this,
                LightGroupingActivity::class.java)
        intent.putExtra("light", light)
        intent.putExtra(Constant.TYPE_VIEW, Constant.LIGHT_KEY)
        intent.putExtra("uuid", light!!.productUUID)
        intent.putExtra("belongId", light!!.belongGroupId)
        intent.putExtra("gpAddress", light!!.meshAddr)
        Log.d("addLight", light!!.productUUID.toString() + "," + light!!.meshAddr)
        startActivity(intent)
    }

    private fun checkPermission() {
//        if(light!!.macAddr.length<16){
//            ToastUtils.showLong(getString(R.string.bt_error))
//        }else{
        mDisposable.add(
                mRxPermission!!.request(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe { granted ->
                    if (granted!!) {
                        var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getInstance(), Constant.IS_DEVELOPER_MODE, false)
                        if (isBoolean) {
                            transformView()
                        } else {
                            OtaPrepareUtils.instance().gotoUpdateView(this@RGBSettingActivity, localVersion, otaPrepareListner)
                        }
                    } else {
                        ToastUtils.showLong(R.string.update_permission_tip)
                    }
                })
//        }
    }

    private fun renameLight() {
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(light?.name)
        textGp.setSelection(textGp.getText().toString().length)
        android.app.AlertDialog.Builder(this@RGBSettingActivity)
                .setTitle(R.string.rename)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        light?.name = textGp.text.toString().trim { it <= ' ' }
                        DBUtils.updateLight(light!!)
                        toolbar.title = light?.name
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }

    internal var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {

        override fun downLoadFileStart() {
            showLoadingDialog(getString(R.string.get_update_file))
        }

        override fun startGetVersion() {
            showLoadingDialog(getString(R.string.verification_version))
        }

        override fun getVersionSuccess(s: String) {
            ToastUtils.showLong(R.string.verification_version_success)
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
        val intent = Intent(this@RGBSettingActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.UPDATE_LIGHT, light)
        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rgb_group_setting)
        initType()
    }

    private fun initType() {
        val type = intent.getStringExtra(Constant.TYPE_VIEW)
        if (type == Constant.TYPE_GROUP) {
            currentShowGroupSetPage = true
            initToolbarGroup()
            initDataGroup()
            initViewGroup()
            addEventListeners()
            this.mApp = this.application as TelinkLightApplication?
        } else {
            currentShowGroupSetPage = false
            initToolbar()
            initView()
            getVersion()
        }
    }

    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            Commander.getDeviceVersion(light!!.meshAddr, { s ->
                localVersion = s
                if (toolbar.title != null) {
                    if (OtaPrepareUtils.instance().checkSupportOta(localVersion)!!) {
//                        toolbar.title!!.visibility = View.VISIBLE
                        lightVersion.visibility = View.VISIBLE
                        lightVersion.text = getString(R.string.firware_version, localVersion)
                        light!!.version = localVersion
//                        tvOta!!.visibility = View.VISIBLE
                    } else {
//                        toolbar.title!!.visibility = View.GONE
                        lightVersion!!.visibility = View.VISIBLE
                        lightVersion!!.text = resources.getString(R.string.firmware_version, localVersion)
                        light!!.version = localVersion
//                        tvOta!!.visibility = View.GONE
                    }
                }
                null
            }, {
                if (toolbar.title != null) {
//                    toolbar.title!!.visibility = View.GONE
                    lightVersion.visibility = View.VISIBLE
                    tvOta!!.visibility = View.GONE
                }
                null
            })
        } else {
            dstAdress = 0
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        this.light = this.intent.extras!!.get(Constant.LIGHT_ARESS_KEY) as DbLight
        this.fromWhere = this.intent.getStringExtra(Constant.LIGHT_REFRESH_KEY)
        this.gpAddress = this.intent.getIntExtra(Constant.GROUP_ARESS_KEY, 0)
        mApplication = this.application as TelinkLightApplication
        dataManager = DataManager(this, mApplication!!.mesh.name, mApplication!!.mesh.password)
        tvRename.visibility = View.GONE
        toolbar.title = light?.name

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
//        btnStopGradient.visibility = View.VISIBLE
        btnStopGradient.setOnClickListener(this.clickListener)
        buildInModeList = ArrayList()
        val presetGradientList = resources.getStringArray(R.array.preset_gradient)
        for (i in 0..10) {
            var item = ItemRgbGradient()
            item.name = presetGradientList[i]
            buildInModeList?.add(item)
        }

        diyGradientList = DBUtils.diyGradientList
//        sbBrightness_add!!.setOnClickListener(this.clickListener)
//        sbBrightness_less!!.setOnClickListener(this.clickListener)
//        sb_w_bright_add!!.setOnClickListener(this.clickListener)
//        sb_w_bright_less!!.setOnClickListener(this.clickListener)
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

        mRxPermission = RxPermissions(this)

        sbBrightness!!.max = 100

        color_picker.reset()
        color_picker.subscribe(colorObserver)
        this.color_picker!!.setOnTouchListener(this)

        mConnectDevice = TelinkLightApplication.getInstance().connectDevice

        presetColors = SharedPreferencesHelper.getObject(this, Constant.PRESET_COLOR) as? MutableList<ItemColorPreset>
        if (presetColors == null) {
            presetColors = ArrayList()
            for (i in 0..4) {
                val itemColorPreset = ItemColorPreset()
                itemColorPreset.color = OtherUtils.getCreateInitColor(i)
                presetColors!!.add(itemColorPreset)
            }
        }

        diy_color_recycler_list_view!!.layoutManager = GridLayoutManager(this, 4)
        colorSelectDiyRecyclerViewAdapter = ColorSelectDiyRecyclerViewAdapter(R.layout.color_select_diy_item, presetColors)
        colorSelectDiyRecyclerViewAdapter!!.onItemChildClickListener = diyOnItemChildClickListener
        colorSelectDiyRecyclerViewAdapter!!.onItemChildLongClickListener = diyOnItemChildLongClickListener
        colorSelectDiyRecyclerViewAdapter!!.bindToRecyclerView(diy_color_recycler_list_view)

        sbBrightness!!.progress = light!!.brightness
        sbBrightness_num!!.text = light!!.brightness.toString() + "%"

        if (sbBrightness!!.progress >= 100) {
            sbBrightness_add.isEnabled = false
            sbBrightness_less.isEnabled = true
        } else if (sbBrightness!!.progress <= 0) {
            sbBrightness_less.isEnabled = false
            sbBrightness_add.isEnabled = true
        } else {
            sbBrightness_less.isEnabled = true
            sbBrightness_add.isEnabled = true
        }

        var w = ((light?.color ?: 0) and 0xff000000.toInt()) shr 24
        var r = Color.red(light?.color ?: 0)
        var g = Color.green(light?.color ?: 0)
        var b = Color.blue(light?.color ?: 0)
        if (w == -1) {
            w = 0
        }

        color_r.text = r.toString()
        color_g.text = g.toString()
        color_b.text = b.toString()

        sb_w_bright_num.text = w.toString() + "%"
        sb_w_bright.progress = w

        if (sb_w_bright.progress >= 100) {
            sb_w_bright_add.isEnabled = false
            sb_w_bright_less.isEnabled = true
        } else if (sb_w_bright.progress <= 0) {
            sb_w_bright_less.isEnabled = false
            sb_w_bright_add.isEnabled = true
        } else {
            sb_w_bright_less.isEnabled = true
            sb_w_bright_add.isEnabled = true
        }

        color_picker.setInitialColor((light?.color ?: 0 and 0xffffff) or 0xff000000.toInt())
        sbBrightness!!.setOnSeekBarChangeListener(barChangeListener)
        sb_w_bright.setOnSeekBarChangeListener(barChangeListener)
    }

    private fun lessBrightness(event: MotionEvent?) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            //                    tvValue = Integer.parseInt(textView.getText().toString());
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handler_brightness_less.obtainMessage()
                            msg.arg1 = tvValue
                            handler_brightness_less.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                Thread.sleep(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }

                        }
                    }
                }
            }
            t.start()
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
            //                    tvValue = Integer.parseInt(textView.getText().toString());
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handler_brightness_add.obtainMessage()
                            msg.arg1 = tvValue
                            handler_brightness_add.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                Thread.sleep(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }

                        }
                    }
                }
            }
            t.start()
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
            //                    tvValue = Integer.parseInt(textView.getText().toString());
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handler_less.obtainMessage()
                            msg.arg1 = tvValue
                            handler_less.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                Thread.sleep(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }

                        }
                    }
                }
            }
            t.start()
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
            //                    tvValue = Integer.parseInt(textView.getText().toString());
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handler.obtainMessage()
                            msg.arg1 = tvValue
                            handler.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                Thread.sleep(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }

                        }
                    }
                }
            }
            t.start()
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
            if (sb_w_bright.progress > 100) {
                sb_w_bright_add.isEnabled = false
                stopTracking = false
                onBtnTouch = false
            } else if (sb_w_bright.progress == 100) {
                sb_w_bright_add.isEnabled = false
                sb_w_bright_num.text = sb_w_bright.progress.toString() + "%"
                stopTracking = false
                onBtnTouch = false
            } else {
                sb_w_bright_add.isEnabled = true
                stopTracking = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler_brightness_add = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            sbBrightness.progress++
            if (sbBrightness.progress > 100) {
                sbBrightness_add.isEnabled = false
                stopTracking = false
                onBtnTouch = false
            } else if (sbBrightness.progress == 100) {
                sbBrightness_add.isEnabled = false
                sbBrightness_num.text = sbBrightness.progress.toString() + "%"
                stopTracking = false
                onBtnTouch = false
            } else {
                sbBrightness_add.isEnabled = true
                stopTracking = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler_brightness_less = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            sbBrightness.progress--
            if (sbBrightness.progress < 0) {
                sbBrightness_less.isEnabled = false
                stopTracking = false
                onBtnTouch = false
            } else if (sbBrightness.progress == 0) {
                sbBrightness_less.isEnabled = false
                sbBrightness_num.text = sbBrightness.progress.toString() + "%"
                stopTracking = false
                onBtnTouch = false
            } else {
                sbBrightness_less.isEnabled = true
                stopTracking = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler_less = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            sb_w_bright.progress--
            if (sb_w_bright.progress < 0) {
                sb_w_bright_less.isEnabled = false
                stopTracking = false
                onBtnTouch = false
            } else if (sb_w_bright.progress == 0) {
                sb_w_bright_less.isEnabled = false
                sb_w_bright_num.text = sb_w_bright.progress.toString() + "%"
                stopTracking = false
                onBtnTouch = false
            } else {
                sb_w_bright_less.isEnabled = true
                stopTracking = true
            }
        }
    }


    private fun initToolbar() {
        toolbar.title = ""
        toolbar.inflateMenu(R.menu.menu_rgb_light_setting)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
    }

    private fun initToolbarGroup() {
        toolbar.title = ""
        toolbar.inflateMenu(R.menu.menu_rgb_group_setting)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (currentShowGroupSetPage) {
            getMenuInflater().inflate(R.menu.menu_rgb_group_setting, menu)
        } else {
            getMenuInflater().inflate(R.menu.menu_rgb_light_setting, menu)
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
            R.id.update_group -> updateGroup()
            R.id.btn_remove -> remove()
            R.id.dynamic_rgb -> toRGBGradientView()
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

            R.id.mode_diy_layout -> {
                changeToDiyPage()
            }
            R.id.mode_preset_layout -> {
                changeToBuildInPage()
            }

            R.id.btnAdd -> {
                transAddAct()
            }

            R.id.ll_g -> {
                var dialog = InputRGBColorDialog(this, R.style.Dialog, color_r.text.toString(), color_g.text.toString(), color_b.text.toString(), InputRGBColorDialog.RGBColorListener { red, green, blue ->
                    redColor = red.toInt()
                    greenColor = green.toInt()
                    blueColor = blue.toInt()

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
//        currentShowIsDiy=false
        if (isPresetMode) {
            buildInButton.setTextColor(resources.getColor(R.color.blue_background))
            buildInButton_image.setImageResource(R.drawable.icon_selected_rgb)
            layoutModePreset.visibility = View.VISIBLE
            diyButton.setTextColor(resources.getColor(R.color.black_three))
            diyButton_image.setImageResource(R.drawable.icon_unselected_rgb)
            isPresetMode = false
        } else {
            buildInButton.setTextColor(resources.getColor(R.color.black_three))
            buildInButton_image.setImageResource(R.drawable.icon_unselected_rgb)
            layoutModePreset.visibility = View.GONE
            diyButton.setTextColor(resources.getColor(R.color.black_three))
            diyButton_image.setImageResource(R.drawable.icon_unselected_rgb)
            isPresetMode = true
        }
        applyPresetView()
    }

    private fun applyPresetView() {
//        this.sbSpeed!!.progress = speed
//        tvSpeed.text = getString(R.string.speed_text, speed.toString())
//        this.sbSpeed!!.setOnSeekBarChangeListener(this.barChangeListener)
        val layoutmanager = LinearLayoutManager(this)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        builtInModeRecycleView!!.layoutManager = layoutmanager
        this.rgbGradientAdapter = RGBGradientAdapter(R.layout.item_gradient_mode, buildInModeList)
        builtInModeRecycleView?.itemAnimator = DefaultItemAnimator()
//        builtInModeRecycleView?.setOnTouchListener { v, event ->
////            mDetector?.onTouchEvent(event)
//            false
//        }
        val decoration = DividerItemDecoration(this,
                DividerItemDecoration
                        .VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color
                .divider)))
        //添加分割线
        builtInModeRecycleView?.addItemDecoration(decoration)
        rgbGradientAdapter!!.onItemChildClickListener = onItemChildClickListener
        rgbGradientAdapter!!.bindToRecyclerView(builtInModeRecycleView)
    }

    private var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->

        when (view!!.getId()) {
            R.id.gradient_mode_on -> {
                //应用内置渐变
                applyDisposable?.dispose()
                applyDisposable = Observable.timer(50, TimeUnit.MILLISECONDS, Schedulers.io())
                        .subscribe {
                            for (i in 0..2) {
                                stopGradient()
                                Thread.sleep(50)
                            }
                            positionState = position + 1
                            Commander.applyGradient(dstAddress, positionState, speed, firstLightAddress, successCallback = {}, failedCallback = {})
                        }
                buildInModeList!![position].select = true

                for (i in buildInModeList!!.indices) {
                    if (i != position) {
                        if (buildInModeList!![i].select) {
                            buildInModeList!![i].select = false
                        }
                    }
                }
                rgbGradientAdapter!!.notifyDataSetChanged()
            }

            R.id.gradient_mode_off -> {
                buildInModeList!![position].select = false
                rgbGradientAdapter!!.notifyItemChanged(position)
                stopGradient()
            }

            R.id.gradient_mode_set -> {
                var dialog = SpeedDialog(this, speed, R.style.Dialog, SpeedDialog.OnSpeedListener {
                    speed = it
                    stopGradient()
                    Thread.sleep(200)
                    Commander.applyGradient(dstAddress, positionState, speed, firstLightAddress, successCallback = {}, failedCallback = {})
                })
                dialog.show()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        diyGradientList = DBUtils.diyGradientList
        this.rgbDiyGradientAdapter = RGBDiyGradientAdapter(R.layout.activity_diy_gradient_item, diyGradientList, isDelete)
        isDelete = false
        rgbDiyGradientAdapter!!.changeState(isDelete)
        toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.VISIBLE
        toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
        toolbar!!.title = getString(R.string.dynamic_gradient)
        rgbDiyGradientAdapter!!.notifyDataSetChanged()
        setDate()
        isDiyMode = true
        changeToDiyPage()
    }

    private fun setDate() {
        for (i in diyGradientList!!.indices) {
            if (diyGradientList!![i].isSelected) {
                diyGradientList!![i].isSelected = false
            }
        }
    }

    private fun changeToDiyPage() {
//        currentShowIsDiy=true
//        diyButton.setBackgroundColor(resources.getColor(R.color.primary))
//        diyButton.setTextColor(resources.getColor(R.color.white))
//        buildInButton.setBackgroundColor(resources.getColor(R.color.white))
//        buildInButton.setTextColor(resources.getColor(R.color.primary))
//        layoutModeDiy.visibility = View.VISIBLE
//        layoutModePreset.visibility = View.GONE
        if (isDiyMode) {
            diyButton.setTextColor(resources.getColor(R.color.blue_background))
            diyButton_image.setImageResource(R.drawable.icon_selected_rgb)
            layoutModeDiy.visibility = View.VISIBLE
            buildInButton.setTextColor(resources.getColor(R.color.black_three))
            buildInButton_image.setImageResource(R.drawable.icon_unselected_rgb)
            isDiyMode = false
        } else {
            diyButton.setTextColor(resources.getColor(R.color.black_three))
            diyButton_image.setImageResource(R.drawable.icon_unselected_rgb)
            layoutModeDiy.visibility = View.GONE
            buildInButton.setTextColor(resources.getColor(R.color.black_three))
            buildInButton_image.setImageResource(R.drawable.icon_unselected_rgb)
            isDiyMode = true
        }
        applyDiyView()
    }

    private fun applyDiyView() {
        val layoutmanager = LinearLayoutManager(this)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        builtDiyModeRecycleView!!.layoutManager = layoutmanager
        this.rgbDiyGradientAdapter = RGBDiyGradientAdapter(R.layout.activity_diy_gradient_item, diyGradientList, isDelete)
        builtDiyModeRecycleView?.itemAnimator = DefaultItemAnimator()
//        builtDiyModeRecycleView?.setOnTouchListener { v, event ->
//            mDetector?.onTouchEvent(event)
//            false
//        }
        val decoration = DividerItemDecoration(this,
                DividerItemDecoration
                        .VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color
                .black_ee)))
        //添加分割线
        builtDiyModeRecycleView?.addItemDecoration(decoration)
        rgbDiyGradientAdapter!!.onItemChildClickListener = onItemChildClickListenerDiy
        rgbDiyGradientAdapter!!.onItemLongClickListener = this.onItemChildLongClickListenerDiy
        rgbDiyGradientAdapter!!.bindToRecyclerView(builtDiyModeRecycleView)

//        rgbDiyGradientAdapter!!.onItemChildClickListener = onItemChildDiyClickListener
    }

    private var onItemChildClickListenerDiy = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->

        when (view!!.getId()) {
            R.id.diy_mode_on -> {
                //应用自定义渐变
                Thread {
                    stopGradient()
                    Thread.sleep(200)
                    Commander.applyDiyGradient(dstAddress, diyGradientList!![position].id.toInt(),
                            diyGradientList!![position].speed, firstLightAddress, successCallback = {}, failedCallback = {})

                }.start()
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
            }

            R.id.diy_mode_off -> {

                Commander.closeGradient(dstAddress, diyGradientList!![position].id.toInt(), diyGradientList!![position].speed, successCallback = {}, failedCallback = {})
                diyGradientList!![position].select = false
                rgbDiyGradientAdapter!!.notifyItemChanged(position)
                DBUtils.updateGradient(diyGradientList!![position])

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
        rgbDiyGradientAdapter!!.changeState(isDelete)
        refreshData()
        return@OnItemLongClickListener true
    }


    private fun refreshData() {
        rgbDiyGradientAdapter!!.notifyDataSetChanged()
        toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.VISIBLE
        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.GONE
        toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
        toolbar!!.title = ""

        deleteDiyGradient()
    }

    private fun deleteDiyGradient() {
        var batchGroup = toolbar.findViewById<ImageView>(R.id.img_function2)

        batchGroup.setOnClickListener(View.OnClickListener {

            val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
            builder.setMessage(getString(R.string.delete_model))
            builder.setPositiveButton(android.R.string.ok) { dialog, which ->
                for (i in diyGradientList!!.indices) {
                    if (diyGradientList!![i].isSelected) {
                        startDeleteGradientCmd(diyGradientList!![i].id)
                        DBUtils.deleteGradient(diyGradientList!![i])
                        DBUtils.deleteColorNodeList(DBUtils.getColorNodeListByDynamicModeId(diyGradientList!![i].id!!))
                    }
                }
                diyGradientList = DBUtils.diyGradientList
                rgbDiyGradientAdapter!!.setNewData(diyGradientList)
            }
            builder.setNeutralButton(R.string.cancel) { dialog, which -> }
            builder.create().show()
        })


//        batchGroup.setOnClickListener(View.OnClickListener {
//            for(i in diyGradientList!!.indices){
//                if(diyGradientList!![i].isSelected){
//                    startDeleteGradientCmd(diyGradientList!![i].id)
//                    DBUtils.deleteGradient(diyGradientList!![i])
//                    DBUtils.deleteColorNodeList(DBUtils.getColorNodeListByDynamicModeId(diyGradientList!![i].id!!))
//                    rgbDiyGradientAdapter!!.notifyDataSetChanged()
//                }
//            }
//        })
    }

    private fun startDeleteGradientCmd(id: Long) {
        Commander.deleteGradient(dstAddress, id.toInt(), {}, {})
    }

    fun stopGradient() {
        Commander.closeGradient(dstAddress, positionState, speed, successCallback = {}, failedCallback = {})
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
                    toolbar.title = getString(R.string.allLight)
                } else {
                    toolbar.title = group?.name
                }
            }
            isDelete = false
            rgbDiyGradientAdapter!!.changeState(isDelete)
            rgbDiyGradientAdapter!!.notifyDataSetChanged()
            setDate()
            toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
            toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
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
            toolbar.title = light?.name
            isDelete = false
            rgbDiyGradientAdapter!!.changeState(isDelete)
            rgbDiyGradientAdapter!!.notifyDataSetChanged()
            setDate()
            toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
            toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
            isDiyMode = false
            changeToDiyPage()
        }
    }


    private fun setColorPicker() {
        val r = redColor!!
        val g = greenColor!!
        val b = blueColor!!
        color_r?.text = r.toString()
        color_g?.text = g.toString()
        color_b?.text = b.toString()
        var w = sb_w_bright.progress
//
        val color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b
//        val color = presetColors?.get(position)?.color
//        var brightness = light!!.brightness
        var ws = (color!! and 0xff000000.toInt()) shr 24
        val red = (color!! and 0xff0000) shr 16
        val green = (color and 0x00ff00) shr 8
        val blue = color and 0x0000ff

        color_picker.setInitialColor((color and 0xffffff) or 0xff000000.toInt())
        val showBrightness = w
        var showW = ws
        Thread {

            try {

                if (w!! > Constant.MAX_VALUE) {
                    w = 100
                }
                if (ws > Constant.MAX_VALUE) {
                    ws = Constant.MAX_VALUE
                }
                if (ws == -1) {
                    ws = 0
                    showW = 0
                }

                var addr = 0
                if (currentShowGroupSetPage) {
                    addr = group?.meshAddr!!
                } else {
                    addr = light?.meshAddr!!
                }

                val opcode: Byte = Opcode.SET_LUM
                val opcodeW: Byte = Opcode.SET_W_LUM

                val paramsW: ByteArray = byteArrayOf(ws.toByte())
                val params: ByteArray = byteArrayOf(w!!.toByte())
                TelinkLightService.Instance().sendCommandNoResponse(opcodeW, addr!!, paramsW)

                Thread.sleep(80)
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)

                Thread.sleep(80)
                changeColor(red.toByte(), green.toByte(), blue.toByte(), true)

                if (currentShowGroupSetPage) {
                    group?.brightness = showBrightness!!
                    group?.color = color
                } else {
                    light?.brightness = showBrightness!!
                    light?.color = color
                }

                LogUtils.d("changedff2" + opcode + "--" + addr + "--" + brightness)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()

        sbBrightness?.progress = showBrightness!!
        sb_w_bright_num.text = showBrightness.toString() + "%"
        if (w != -1) {
            sb_w_bright_num.text = showW.toString() + "%"
            sb_w_bright.progress = showW
        } else {
            sb_w_bright_num.text = "0%"
            sb_w_bright.progress = 0
        }
//        scrollView?.setBackgroundColor(color)
        color_r?.text = red.toString()
        color_g?.text = green.toString()
        color_b?.text = blue.toString()
    }


    private val menuItemClickListener = Toolbar.OnMenuItemClickListener { item ->
        when (item?.itemId) {
            R.id.toolbar_delete_group -> {
                removeGroup()
            }
            R.id.toolbar_rename_group -> {
                renameGp()
            }
            R.id.toolbar_rename_light -> {
                renameLight()
            }
            R.id.toolbar_reset -> {
                remove()
            }
            R.id.toolbar_update_group -> {
                updateGroup()
            }
            R.id.toolbar_ota -> {
                updateOTA()
            }
        }
        true
    }

    private fun updateOTA() {
        if (lightVersion.text != null && lightVersion.text != " ") {
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
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
                    if (currentShowGroupSetPage) {
                        if (group != null) {
                            if (group!!.meshAddr == 0xffff) {
                                toolbar.title = getString(R.string.allLight)
                            } else {
                                toolbar.title = group?.name
                            }
                        }
                    } else {
                        toolbar.title = light?.name
                    }
                } else {
                    finish()
                }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        if (currentShowGroupSetPage) {
            DBUtils.updateGroup(group!!)
            updateLights(group!!.color, "rgb_color", group!!)
        } else {
            DBUtils.updateLight(light!!)
        }
    }

    fun addEventListeners() {
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        this.mApplication?.addEventListener(NotificationEvent.ONLINE_STATUS, this)
////        this.mApplication?.addEventListener(NotificationEvent.GET_ALARM, this)
//        this.mApplication?.addEventListener(NotificationEvent.GET_DEVICE_STATE, this)
//        this.mApplication?.addEventListener(ServiceEvent.SERVICE_CONNECTED, this)
////        this.mApplication?.addEventListener(MeshEvent.OFFLINE, this)
//        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    override fun performed(event: Event<String>?) {
        when (event?.type) {
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
        }
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                hideLoadingDialog()
                isLoginSuccess = true
                mConnectTimer?.dispose()
            }
            LightAdapter.STATUS_LOGOUT -> {
                autoConnect()
                mConnectTimer = Observable.timer(15, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                        .subscribe { aLong ->
                            com.blankj.utilcode.util.LogUtils.d("STATUS_LOGOUT")
//                            showLoadingDialog()
                            ToastUtils.showLong(getString(R.string.connect_failed))
                            finish()
                        }
            }
        }
    }

//    private fun showIsConnect

    /**
     * 自动重连
     */
    private fun autoConnect() {

        if (TelinkLightService.Instance() != null) {

            if (TelinkLightService.Instance().mode != LightAdapter.MODE_AUTO_CONNECT_MESH) {

                ToastUtils.showLong(getString(R.string.connecting))
                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

                if (this.mApp!!.isEmptyMesh())
                    return

                //                Lights.getInstance().clear();
                this.mApp?.refreshLights()

                val mesh = this.mApp?.getMesh()

                if (TextUtils.isEmpty(mesh?.name) || TextUtils.isEmpty(mesh?.password)) {
                    TelinkLightService.Instance().idleMode(true)
                    return
                }

                val account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                        Constant.DB_NAME_KEY, "dadou")

                //自动重连参数
                val connectParams = Parameters.createAutoConnectParameters()
                connectParams.setMeshName(mesh?.name)
                if (SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(), Constant.USER_TYPE, Constant.USER_TYPE_OLD) == Constant.USER_TYPE_NEW) {
                    connectParams.setPassword(NetworkFactory.md5(
                            NetworkFactory.md5(mesh?.password) + account))
                } else {
                    connectParams.setPassword(mesh?.password)
                }
                connectParams.autoEnableNotification(true)

                // 之前是否有在做MeshOTA操作，是则继续
                if (mesh!!.isOtaProcessing) {
                    connectParams.setConnectMac(mesh?.otaDevice!!.mac)
                }
                //自动重连
                TelinkLightService.Instance().autoConnect(connectParams)
            }

            //刷新Notify参数
            val refreshNotifyParams = Parameters.createRefreshNotifyParameters()
            refreshNotifyParams.setRefreshRepeatCount(2)
            refreshNotifyParams.setRefreshInterval(2000)
            //开启自动刷新Notify
            TelinkLightService.Instance().autoRefreshNotify(refreshNotifyParams)
        }
    }

    private fun initDataGroup() {
        this.mApplication = this.application as TelinkLightApplication
        this.group = this.intent.extras!!.get("group") as DbGroup
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        v?.parent?.requestDisallowInterceptTouchEvent(true)
        LogUtils.d("--------")
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initViewGroup() {
        if (group != null) {
            if (group!!.meshAddr == 0xffff) {
                toolbar.title = getString(R.string.allLight)
            } else {
                toolbar.title = group?.name
            }
        }

        color_picker.isLongClickable = true
        this.color_picker!!.setOnTouchListener(this)
        color_picker.isEnabled = true

        dynamic_rgb.setOnClickListener(this.clickListener)
        ll_r.setOnClickListener(this.clickListener)
        ll_g.setOnClickListener(this.clickListener)
        ll_b.setOnClickListener(this.clickListener)
        normal_rgb.setOnClickListener(this.clickListener)
        btnAdd.setOnClickListener(this.clickListener)
        mode_preset_layout.setOnClickListener(this.clickListener)
        mode_diy_layout.setOnClickListener(this.clickListener)

        buildInModeList = ArrayList()
        val presetGradientList = resources.getStringArray(R.array.preset_gradient)
        for (i in 0..10) {
            var item = ItemRgbGradient()
            item.name = presetGradientList[i]
            buildInModeList?.add(item)
        }

        diyGradientList = DBUtils.diyGradientList

//        sbBrightness_add!!.setOnTouchListener { v, event ->
//
//            true
//        }
//        sbBrightness_less!!.setOnClickListener(this.clickListener)
//        sb_w_bright_add!!.setOnClickListener(this.clickListener)
//        sb_w_bright_less!!.setOnClickListener(this.clickListener)

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

//        dynamicRgb?.setOnClickListener(this)

        presetColors = SharedPreferencesHelper.getObject(this, Constant.PRESET_COLOR) as? MutableList<ItemColorPreset>
        if (presetColors == null) {
            presetColors = java.util.ArrayList<ItemColorPreset>()
            for (i in 0..4) {
                val itemColorPreset = ItemColorPreset()
                itemColorPreset.color = OtherUtils.getCreateInitColor(i)
                presetColors?.add(itemColorPreset)
            }
        }

        diy_color_recycler_list_view?.layoutManager = GridLayoutManager(this, 4) as RecyclerView.LayoutManager?
        colorSelectDiyRecyclerViewAdapter = ColorSelectDiyRecyclerViewAdapter(R.layout.color_select_diy_item, presetColors)
        colorSelectDiyRecyclerViewAdapter?.onItemChildClickListener = diyOnItemChildClickListener
        colorSelectDiyRecyclerViewAdapter?.onItemChildLongClickListener = diyOnItemChildLongClickListener
        colorSelectDiyRecyclerViewAdapter?.bindToRecyclerView(diy_color_recycler_list_view)

        sbBrightness!!.progress = group!!.brightness
        sbBrightness_num.text = group!!.brightness.toString() + "%"

        if (sbBrightness!!.progress >= 100) {
            sbBrightness_add.isEnabled = false
            sbBrightness_less.isEnabled = true
        } else if (sbBrightness!!.progress <= 0) {
            sbBrightness_less.isEnabled = false
            sbBrightness_add.isEnabled = true
        } else {
            sbBrightness_less.isEnabled = true
            sbBrightness_add.isEnabled = true
        }

        var w = ((group?.color ?: 0) and 0xff000000.toInt()) shr 24
        var r = Color.red(group?.color ?: 0)
        var g = Color.green(group?.color ?: 0)
        var b = Color.blue(group?.color ?: 0)
        if (w == -1) {
            w = 0
        }
        sb_w_bright_num.text = w.toString() + "%"
        sb_w_bright.progress = w

        color_r.text = r.toString()
        color_g.text = g.toString()
        color_b.text = b.toString()

        if (sb_w_bright.progress >= 100) {
            sb_w_bright_add.isEnabled = false
            sb_w_bright_less.isEnabled = true
        } else if (sb_w_bright.progress <= 0) {
            sb_w_bright_less.isEnabled = false
            sb_w_bright_add.isEnabled = true
        } else {
            sb_w_bright_less.isEnabled = true
            sb_w_bright_add.isEnabled = true
        }

        sbBrightness!!.setOnSeekBarChangeListener(barChangeListener)
        sb_w_bright.setOnSeekBarChangeListener(barChangeListener)
        color_picker.reset()
        color_picker.subscribe(colorObserver)
        color_picker.setInitialColor((group?.color ?: 0 and 0xffffff) or 0xff000000.toInt())
        checkGroupIsSystemGroup()
    }

    private var diyOnItemChildClickListener: BaseQuickAdapter.OnItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        val color = presetColors?.get(position)?.color
        var brightness = presetColors?.get(position)?.brightness
        var w = (color!! and 0xff000000.toInt()) shr 24
        val red = (color!! and 0xff0000) shr 16
        val green = (color and 0x00ff00) shr 8
        val blue = color and 0x0000ff

        color_picker.setInitialColor((color and 0xffffff) or 0xff000000.toInt())
        val showBrightness = brightness
        var showW = w
        Thread {

            try {

                if (brightness!! > Constant.MAX_VALUE) {
                    brightness = Constant.MAX_VALUE
                }
                if (w > Constant.MAX_VALUE) {
                    w = Constant.MAX_VALUE
                }
                if (w == -1) {
                    w = 0
                    showW = 0
                }

                var addr = 0
                if (currentShowGroupSetPage) {
                    addr = group?.meshAddr!!
                } else {
                    addr = light?.meshAddr!!
                }

                val opcode: Byte = Opcode.SET_LUM
                val opcodeW: Byte = Opcode.SET_W_LUM

                val paramsW: ByteArray = byteArrayOf(w.toByte())
                val params: ByteArray = byteArrayOf(brightness!!.toByte())
                TelinkLightService.Instance().sendCommandNoResponse(opcodeW, addr!!, paramsW)

                Thread.sleep(80)
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)

                Thread.sleep(80)
                changeColor(red.toByte(), green.toByte(), blue.toByte(), true)

                if (currentShowGroupSetPage) {
                    group?.brightness = showBrightness!!
                    group?.color = color
                } else {
                    light?.brightness = showBrightness!!
                    light?.color = color
                }

                LogUtils.d("changedff2" + opcode + "--" + addr + "--" + brightness)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()

        sbBrightness?.progress = showBrightness!!
        sb_w_bright_num.text = showBrightness.toString() + "%"
        if (w != -1) {
            sb_w_bright_num.text = showW.toString() + "%"
            sb_w_bright.progress = showW
        } else {
            sb_w_bright_num.text = "0%"
            sb_w_bright.progress = 0
        }
//        scrollView?.setBackgroundColor(color)
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
//                textView!!.text = group!!.brightness.toString() + "%"
                textView?.setChecked(true, 0xff000000.toInt() or group!!.color)
                SharedPreferencesHelper.putObject(this, Constant.PRESET_COLOR, presetColors)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
//                textView!!.text = group!!.brightness.toString() + "%"
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
            if (lightList != null) {
                if (lightList.size > 0) {
                    firstLightAddress = lightList[0].meshAddr
                }
            }
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
//        if (currentShowGroupSetPage) {
//            val intent = Intent(this, RGBGradientActivity::class.java)
//            intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_GROUP)
//            intent.putExtra(Constant.TYPE_VIEW_ADDRESS, group?.meshAddr)
//            overridePendingTransition(0, 0)
//            startActivityForResult(intent, 0)
//        } else {
//            val intent = Intent(this, RGBGradientActivity::class.java)
//            intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_LIGHT)
//            intent.putExtra(Constant.TYPE_VIEW_ADDRESS, light!!.meshAddr)
//            startActivityForResult(intent, 0)
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mConnectTimer?.dispose()
        this.mApplication?.removeEventListener(this)
        mDisposable.dispose()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(Constant.RESULT_OK)
            finish()
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
            LogUtils.d("seekBarstop" + seekBar.progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            stopTracking = false
            this.preTime = System.currentTimeMillis()
            this.onValueChange(seekBar, seekBar.progress, true)
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                       fromUser: Boolean) {
            val currentTime = System.currentTimeMillis()

            onValueChangeView(seekBar, progress, true)
            if (currentTime - this.preTime > this.delayTime) {
                this.onValueChange(seekBar, progress, true)
                this.preTime = currentTime
            }
        }

        private fun onValueChangeView(view: View, progress: Int, immediate: Boolean) {
            if (view === sbBrightness) {
                sbBrightness_num.text = progress.toString() + "%"
            } else if (view === sb_w_bright) {
                sb_w_bright_num.text = progress.toString() + "%"
            }
        }

        private fun onValueChange(view: View, progress: Int, immediate: Boolean) {

            var addr = 0
            if (currentShowGroupSetPage) {
                addr = group!!.meshAddr
            } else {
                addr = light!!.meshAddr
            }

            val opcode: Byte
            val params: ByteArray
            var brightness = 0
            var w = 0
            if (view == sbBrightness) {
                if (progress > Constant.MAX_VALUE) {
                    brightness = Constant.MAX_VALUE
                } else {
                    brightness = progress
                }
                opcode = Opcode.SET_LUM
                params = byteArrayOf(brightness.toByte())
                if (currentShowGroupSetPage) {
                    group!!.brightness = progress
                    if (group!!.brightness >= 100) {
                        sbBrightness_add.isEnabled = false
                        sbBrightness_less.isEnabled = true
                    } else if (group!!.brightness <= 0) {
                        sbBrightness_less.isEnabled = false
                        sbBrightness_add.isEnabled = true
                    } else {
                        sbBrightness_less.isEnabled = true
                        sbBrightness_add.isEnabled = true
                    }
                } else {
                    light!!.brightness = progress
                    if (light!!.brightness >= 100) {
                        sbBrightness_add.isEnabled = false
                        sbBrightness_less.isEnabled = true
                    } else if (light!!.brightness <= 0) {
                        sbBrightness_less.isEnabled = false
                        sbBrightness_add.isEnabled = true
                    } else {
                        sbBrightness_less.isEnabled = true
                        sbBrightness_add.isEnabled = true
                    }
                }

                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)

                if (stopTracking) {
                    if (currentShowGroupSetPage) {
                        DBUtils.updateGroup(group!!)
                        updateLights(progress, "brightness", group!!)
                    } else {
                        DBUtils.updateLight(light!!)
                    }
                }
            } else if (view == sb_w_bright) {
                opcode = Opcode.SET_W_LUM
                if (progress > Constant.MAX_VALUE) {
                    w = Constant.MAX_VALUE
                } else {
                    w = progress
                }
                params = byteArrayOf(w.toByte())
                var color = 0
                if (currentShowGroupSetPage) {
                    color = group?.color!!
                    if (progress >= 100) {
                        sb_w_bright_add.isEnabled = false
                        sb_w_bright_less.isEnabled = true
                    } else if (progress <= 0) {
                        sb_w_bright_less.isEnabled = false
                        sb_w_bright_add.isEnabled = true
                    } else {
                        sb_w_bright_add.isEnabled = true
                        sb_w_bright_less.isEnabled = true
                    }
                } else {
                    color = light?.color!!
                    if (progress >= 100) {
                        sb_w_bright_add.isEnabled = false
                        sb_w_bright_less.isEnabled = true
                    } else if (progress <= 0) {
                        sb_w_bright_less.isEnabled = false
                        sb_w_bright_add.isEnabled = true
                    } else {
                        sb_w_bright_add.isEnabled = true
                        sb_w_bright_less.isEnabled = true
                    }
                }

                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)

                if (stopTracking) {
                    val red = (color and 0xff0000) shr 16
                    val green = (color and 0x00ff00) shr 8
                    val blue = color and 0x0000ff
                    val w = progress
                    if (currentShowGroupSetPage) {
                        group?.color = (w shl 24) or (red shl 16) or (green shl 8) or blue
                    } else {
                        light?.color = (w shl 24) or (red shl 16) or (green shl 8) or blue
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

    private fun updateLights(progress: Int, type: String, group: DbGroup) {
        Thread {
            var lightList: MutableList<DbLight> = ArrayList()

            if (group.meshAddr == 0xffff) {
                //            lightList = DBUtils.getAllLight();
                val list = DBUtils.groupList
                for (j in list.indices) {
                    lightList.addAll(DBUtils.getLightByGroupID(list[j].id))
                }
            } else {
                lightList = DBUtils.getLightByGroupID(group.id)
            }

            for (dbLight: DbLight in lightList) {
                if (type == "brightness") {
                    dbLight.brightness = progress
                } else if (type == "colorTemperature") {
                    dbLight.colorTemperature = progress
                } else if (type == "rgb_color") {
                    dbLight.color = progress
                }
                DBUtils.updateLight(dbLight)
            }
        }.start()
    }

    private val colorObservers = ColorObserver { color, fromUser ->
        val r = redColor!!
        val g = greenColor!!
        val b = blueColor!!
        color_r?.text = r.toString()
        color_g?.text = g.toString()
        color_b?.text = b.toString()
        val w = sb_w_bright.progress

        val color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b
//        val color =
        var fromUser = true
        Log.d("", "onColorSelected: " + Integer.toHexString(color))
        if (fromUser) {
//            scrollView?.setBackgroundColor(0xff000000.toInt() or color)
            if (r == 0 && g == 0 && b == 0) {
            } else {
                Thread {
                    if (currentShowGroupSetPage) {
                        group?.color = color
                    } else {
                        light?.color = color
                    }

                    changeColor(r.toByte(), g.toByte(), b.toByte(), false)

                }.start()
            }
        }
    }

    private val colorObserver = ColorObserver { color, fromUser ->
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        color_r?.text = r.toString()
        color_g?.text = g.toString()
        color_b?.text = b.toString()
        val w = sb_w_bright.progress

        val color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b
//        val color =
        Log.d("", "onColorSelected: " + Integer.toHexString(color))
        if (fromUser) {
//            scrollView?.setBackgroundColor(0xff000000.toInt() or color)
            if (r == 0 && g == 0 && b == 0) {
            } else {
                Thread {
                    if (currentShowGroupSetPage) {
                        group?.color = color
                    } else {
                        light?.color = color
                    }

                    changeColor(r.toByte(), g.toByte(), b.toByte(), false)

                }.start()
            }
        }
//                val r = Color.red(color)
//        val g = Color.green(color)
//        val b = Color.blue(color)
//        color_r?.text = r.toString()
//        color_g?.text = g.toString()
//        color_b?.text = b.toString()
//        val w = sb_w_bright.progress
//
//        val color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b
//        val color =
//        Log.d("", "onColorSelected: " + Integer.toHexString(color))
//        if (isColor) {
//            val r = Color.red(color)
//            val g = Color.green(color)
//            val b = Color.blue(color)
//            color_r?.text = r.toString()
//            color_g?.text = g.toString()
//            color_b?.text = b.toString()
//            val w = sb_w_bright.progress
//
//            val color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b
////            scrollView?.setBackgroundColor(0xff000000.toInt() or color)
//            if (r == 0 && g == 0 && b == 0) {
//            } else {
//                Thread {
//                    if (currentShowGroupSetPage) {
//                        group?.color = color
//                    } else {
//                        light?.color = color
//                    }
//
//                    changeColor(r.toByte(), g.toByte(), b.toByte(), false)
//
//                }.start()
//            }
//        } else {
//            if (redColor != null) {
//                val r = redColor!!
//                val g = greenColor!!
//                val b = blueColor!!
//                color_r?.text = r.toString()
//                color_g?.text = g.toString()
//                color_b?.text = b.toString()
//                val w = sb_w_bright.progress
//
//                val color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b
//
//                if (r == 0 && g == 0 && b == 0) {
//                } else {
//                    Thread {
//                        if (currentShowGroupSetPage) {
//                            group?.color = color
//                        } else {
//                            light?.color = color
//                        }
//
//                        changeColor(r.toByte(), g.toByte(), b.toByte(), false)
//
//                    }.start()
//                }
//            }
//        }
    }

    private val colorEnvelopeListener = ColorEnvelopeListener { envelope, fromUser ->

    }

    private fun changeColor(R: Byte, G: Byte, B: Byte, isOnceSet: Boolean) {

        var red = R
        var green = G
        var blue = B

        var addr = 0
        if (currentShowGroupSetPage) {
            addr = group?.meshAddr!!
        } else {
            addr = light?.meshAddr!!
        }

        val opcode = Opcode.SET_TEMPERATURE

        val params = byteArrayOf(0x04, red, green, blue)

        val logStr = String.format("R = %x, G = %x, B = %x", red, green, blue)
        Log.d("RGBCOLOR", logStr)

        if (isOnceSet) {
//            for(i in 0..3){
            Thread.sleep(50)
            TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)
//            }
        } else {
            TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)
        }

//        color_picker.setInitialColor((light?.color ?: 0
//        and 0xffffff) or 0xff000000.toInt())
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
                                light.belongGroupId = DBUtils.groupNull!!.id
                                DBUtils.updateLight(light)
                                lights.remove(light)
                                //修改分组成功后删除场景信息。
                                deleteAllSceneByLightAddr(light.meshAddr)
                                Thread.sleep(100)
                                if (lights.count() == 0) {
                                    //所有灯都删除了分组
                                    DBUtils.deleteGroupOnly(group)
                                    this?.runOnUiThread {
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
                    this?.runOnUiThread {
                        failedCallback.invoke()
                    }
                    LogUtils.d("retry delete group timeout")
                }
            } else {
                DBUtils.deleteGroupOnly(group)
                this?.runOnUiThread {
                    successCallback.invoke()
                }
            }
        }.start()

    }

    private fun getRelatedSceneIds(groupAddress: Int): List<Long> {
        val sceneIds = java.util.ArrayList<Long>()
        val dbSceneList = DBUtils.sceneList
        sceneLoop@ for (dbScene in dbSceneList) {
            val dbActions = DBUtils.getActionsBySceneId(dbScene.id)
            for (action in dbActions) {
                if (groupAddress == action.groupAddr || 0xffff == action.groupAddr) {
                    sceneIds.add(dbScene.id)
                    continue@sceneLoop
                }
            }
        }
        return sceneIds
    }

    /**
     * 删除指定灯里的所有场景
     *
     * @param lightMeshAddr 灯的mesh地址
     */
    private fun deleteAllSceneByLightAddr(lightMeshAddr: Int) {
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val params: ByteArray
        params = byteArrayOf(0x00, 0xff.toByte())
        TelinkLightService.Instance().sendCommandNoResponse(opcode, lightMeshAddr, params)
    }

    private fun renameGp() {
        val textGp = EditText(this)
        textGp.setText(group?.name)
        StringUtils.initEditTextFilter(textGp)
        textGp.setSelection(textGp.getText().toString().length)
        android.app.AlertDialog.Builder(this@RGBSettingActivity)
                .setTitle(R.string.rename)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        var name = textGp.text.toString().trim { it <= ' ' }
                        var canSave = true
                        val groups = DBUtils.allGroups
                        for (i in groups.indices) {
                            if (groups[i].name == name) {
                                ToastUtils.showLong(TelinkLightApplication.getInstance().getString(R.string.repeat_name))
                                canSave = false
                                break
                            }
                        }

                        if (canSave) {
                            group?.name = textGp.text.toString().trim { it <= ' ' }
                            DBUtils.updateGroup(group!!)
                            toolbar.title = group?.name
                            dialog.dismiss()
                        }
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }
}
