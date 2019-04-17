package com.dadoutek.uled.rgb

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.widget.*
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.LightGroupingActivity
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.ItemColorPreset
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
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
import kotlinx.android.synthetic.main.fragment_rgb_group_setting.*
import kotlinx.android.synthetic.main.toolbar.*
import top.defaults.colorpicker.ColorObserver
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

class RGBSettingActivity : TelinkBaseActivity(), EventListener<String>,View.OnTouchListener {

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
    private var currentShowGroupSetPage=true

    fun removeGroup(){
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

                    if (TelinkLightService.Instance().adapter.mLightCtrl.currentLight.isConnected) {
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
        intent.putExtra(Constant.TYPE_VIEW,Constant.LIGHT_KEY)
        intent.putExtra("uuid",light!!.productUUID)
        intent.putExtra("belongId",light!!.belongGroupId)
        intent.putExtra("gpAddress", light!!.meshAddr)
        Log.d("addLight",light!!.productUUID.toString()+","+light!!.meshAddr)
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
                            var isBoolean: Boolean =SharedPreferencesHelper.getBoolean(TelinkLightApplication.getInstance(),Constant.IS_DEVELOPER_MODE,false)
                            if(isBoolean){
                                transformView()
                            }else {
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
                        light?.name=textGp.text.toString().trim { it <= ' ' }
                        DBUtils.updateLight(light!!)
                        toolbar.title=light?.name
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
        val type=intent.getStringExtra(Constant.TYPE_VIEW)
        if(type==Constant.TYPE_GROUP){
            currentShowGroupSetPage=true
            initToolbarGroup()
            initDataGroup()
            initViewGroup()
            addEventListeners()
            this.mApp = this.application as TelinkLightApplication?
        }else{
            currentShowGroupSetPage=false
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
                        lightVersion.visibility=View.VISIBLE
                        lightVersion.text = getString(R.string.firware_version,localVersion)
                        light!!.version = localVersion
                        tvOta!!.visibility = View.VISIBLE
                    } else {
//                        toolbar.title!!.visibility = View.GONE
                        lightVersion!!.visibility = View.VISIBLE
                        lightVersion!!.text =  resources.getString(R.string.firmware_version,localVersion)
                        light!!.version = localVersion
                        tvOta!!.visibility = View.GONE
                    }
                }
                null
            }, {
                if (toolbar.title != null) {
//                    toolbar.title!!.visibility = View.GONE
                    lightVersion.visibility=View.VISIBLE
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
        tvRename.visibility=View.GONE
        toolbar.title=light?.name

        tvRename!!.setOnClickListener(this.clickListener)
        tvOta!!.setOnClickListener(this.clickListener)
        dynamic_rgb!!.setOnClickListener(this.clickListener)

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

        diy_color_recycler_list_view!!.layoutManager = GridLayoutManager(this, 5)
        colorSelectDiyRecyclerViewAdapter = ColorSelectDiyRecyclerViewAdapter(R.layout.color_select_diy_item, presetColors)
        colorSelectDiyRecyclerViewAdapter!!.onItemChildClickListener = diyOnItemChildClickListener
        colorSelectDiyRecyclerViewAdapter!!.onItemChildLongClickListener = diyOnItemChildLongClickListener
        colorSelectDiyRecyclerViewAdapter!!.bindToRecyclerView(diy_color_recycler_list_view)

        sbBrightness!!.progress = light!!.brightness
        tv_brightness_rgb!!.text = getString(R.string.device_setting_brightness, light!!.brightness.toString() + "")

        var w = ((light?.color ?: 0) and 0xff000000.toInt()) shr 24
        var r=Color.red(light?.color?:0)
        var g=Color.green(light?.color?:0)
        var b=Color.blue(light?.color?:0)
        if(w==-1){
            w=0
        }

        color_r.text = r.toString()
        color_g.text = g.toString()
        color_b.text = b.toString()

        tv_brightness_w.text = getString(R.string.w_bright, w.toString() + "")
        sb_w_bright.progress = w
        color_picker.setInitialColor((light?.color?:0 and 0xffffff) or 0xff000000.toInt())
        sbBrightness!!.setOnSeekBarChangeListener(barChangeListener)
        sb_w_bright.setOnSeekBarChangeListener(barChangeListener)
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
        if(currentShowGroupSetPage){
            getMenuInflater().inflate(R.menu.menu_rgb_group_setting, menu)
        }else{
            getMenuInflater().inflate(R.menu.menu_rgb_light_setting, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    private val clickListener = OnClickListener { v ->
        when(v.id){
            R.id.btn_remove_group -> removeGroup()
            R.id.btn_rename -> {
                renameGp()
            }
            R.id.img_header_menu_left->finish()
            R.id.tvOta->checkPermission()
            R.id.update_group -> updateGroup()
            R.id.btn_remove -> remove()
            R.id.dynamic_rgb -> toRGBGradientView()
            R.id.tvRename -> renameLight()
        }
    }

    private val menuItemClickListener= Toolbar.OnMenuItemClickListener { item ->
        when(item?.itemId){
            R.id.toolbar_delete_group->{removeGroup()}
            R.id.toolbar_rename_group->{renameGp()}
            R.id.toolbar_rename_light->{renameLight()}
            R.id.toolbar_reset->{remove()}
            R.id.toolbar_update_group->{updateGroup()}
        }
        true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        if(currentShowGroupSetPage){
            DBUtils.updateGroup(group!!)
            updateLights(group!!.color, "rgb_color", group!!)
        }else{
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

        color_picker.isLongClickable=true
        this.color_picker!!.setOnTouchListener(this)
        color_picker.isEnabled=true

        dynamic_rgb.setOnClickListener(this.clickListener)

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

        diy_color_recycler_list_view?.layoutManager = GridLayoutManager(this, 5) as RecyclerView.LayoutManager?
        colorSelectDiyRecyclerViewAdapter = ColorSelectDiyRecyclerViewAdapter(R.layout.color_select_diy_item, presetColors)
        colorSelectDiyRecyclerViewAdapter?.onItemChildClickListener = diyOnItemChildClickListener
        colorSelectDiyRecyclerViewAdapter?.onItemChildLongClickListener = diyOnItemChildLongClickListener
        colorSelectDiyRecyclerViewAdapter?.bindToRecyclerView(diy_color_recycler_list_view)

        sbBrightness!!.progress = group!!.brightness
        tv_brightness_rgb.text = getString(R.string.device_setting_brightness, group!!.brightness.toString() + "")

        var w = ((group?.color ?: 0) and 0xff000000.toInt()) shr 24
        var r=Color.red(group?.color?:0)
        var g=Color.green(group?.color?:0)
        var b=Color.blue(group?.color?:0)
        if(w==-1){
            w=0
        }
        tv_brightness_w.text = getString(R.string.w_bright, w.toString() + "")
        sb_w_bright.progress = w

        color_r.text = r.toString()
        color_g.text = g.toString()
        color_b.text = b.toString()

        sbBrightness!!.setOnSeekBarChangeListener(barChangeListener)
        sb_w_bright.setOnSeekBarChangeListener(barChangeListener)
        color_picker.reset()
        color_picker.subscribe(colorObserver)
        color_picker.setInitialColor((group?.color?:0 and 0xffffff) or 0xff000000.toInt())
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
        val showBrightness=brightness
        var showW=w
        Thread {

        try {

            if(brightness!! > Constant.MAX_VALUE){
                brightness=Constant.MAX_VALUE
            }
            if(w>Constant.MAX_VALUE){
                w=Constant.MAX_VALUE
            }
            if(w==-1){
                w=0
                showW=0
            }

            var addr = 0
            if(currentShowGroupSetPage){
                addr = group?.meshAddr!!
            }else{
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

            if(currentShowGroupSetPage){
                group?.brightness = showBrightness!!
                group?.color = color
            }else{
                light?.brightness = showBrightness!!
                light?.color = color
            }

            LogUtils.d("changedff2" + opcode + "--" + addr + "--" + brightness)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        }.start()

        sbBrightness?.progress = showBrightness!!
        tv_brightness_rgb.text = getString(R.string.device_setting_brightness, showBrightness.toString() + "")
        if(w!=-1){
            tv_brightness_w.text = getString(R.string.w_bright, showW.toString() + "")
            sb_w_bright.progress=showW
        }else{
            tv_brightness_w.text = getString(R.string.w_bright, "0")
            sb_w_bright.progress=0
        }
//        scrollView?.setBackgroundColor(color)
        color_r?.text = red.toString()
        color_g?.text = green.toString()
        color_b?.text = blue.toString()
    }

    @SuppressLint("SetTextI18n")
    internal var diyOnItemChildLongClickListener: BaseQuickAdapter.OnItemChildLongClickListener = BaseQuickAdapter.OnItemChildLongClickListener { adapter, view, position ->
        if(currentShowGroupSetPage){
            presetColors?.get(position)!!.color = group!!.color
            presetColors?.get(position)!!.brightness = group!!.brightness
        }else{
            presetColors?.get(position)!!.color = light!!.color
            presetColors?.get(position)!!.brightness = light!!.brightness
        }
        val textView = adapter.getViewByPosition(position, R.id.btn_diy_preset) as TextView?
        try {
            textView!!.text = group!!.brightness.toString() + "%"
            textView.setBackgroundColor(0xff000000.toInt() or group!!.color)
            SharedPreferencesHelper.putObject(this, Constant.PRESET_COLOR, presetColors)
        }catch (e:Exception){
            e.printStackTrace()
        }
        true
    }

    private fun toRGBGradientView() {
        if(currentShowGroupSetPage){
            val intent = Intent(this, RGBGradientActivity::class.java)
            intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_GROUP)
            intent.putExtra(Constant.TYPE_VIEW_ADDRESS, group?.meshAddr)
            overridePendingTransition(0, 0)
            startActivityForResult(intent, 0)
        }else{
            val intent = Intent(this, RGBGradientActivity::class.java)
            intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_LIGHT)
            intent.putExtra(Constant.TYPE_VIEW_ADDRESS, light!!.meshAddr)
            startActivityForResult(intent, 0)
        }
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

        private fun onValueChangeView(view: View, progress: Int, immediate: Boolean){
            if (view === sbBrightness) {
                tv_brightness_rgb.text = getString(R.string.device_setting_brightness, progress.toString() + "")
            } else if (view === sb_w_bright) {
                tv_brightness_w.text = getString(R.string.w_bright, progress.toString() + "")
            }
        }

        private fun onValueChange(view: View, progress: Int, immediate: Boolean) {

            var addr = 0
            if(currentShowGroupSetPage){
                addr = group!!.meshAddr
            }else{
                addr = light!!.meshAddr
            }

            val opcode: Byte
            val params: ByteArray
            var brightness=0
            var w=0
            if (view == sbBrightness) {
                if(progress > Constant.MAX_VALUE){
                    brightness = Constant.MAX_VALUE
                }else{
                    brightness=progress
                }
                opcode = Opcode.SET_LUM
                params = byteArrayOf(brightness.toByte())
                if(currentShowGroupSetPage){
                    group!!.brightness = progress
                }else{
                    light!!.brightness = progress
                }

                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)

                if (stopTracking) {
                    if(currentShowGroupSetPage){
                        DBUtils.updateGroup(group!!)
                        updateLights(progress, "brightness", group!!)
                    }else{
                        DBUtils.updateLight(light!!)
                    }
                }
            } else if (view == sb_w_bright) {
                opcode = Opcode.SET_W_LUM
                if(progress>Constant.MAX_VALUE){
                    w=Constant.MAX_VALUE
                }else{
                    w=progress
                }
                params = byteArrayOf(w.toByte())
                var color = 0
                if(currentShowGroupSetPage){
                    color = group?.color!!
                }else{
                    color = light?.color!!
                }

                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)

                if (stopTracking) {
                    val red = (color and 0xff0000) shr 16
                    val green = (color and 0x00ff00) shr 8
                    val blue = color and 0x0000ff
                    val w = progress
                    if(currentShowGroupSetPage){
                        group?.color = (w shl 24) or (red shl 16) or (green shl 8) or blue
                    }else{
                        light?.color = (w shl 24) or (red shl 16) or (green shl 8) or blue
                    }
                    if(currentShowGroupSetPage){
                        DBUtils.updateGroup(group!!)
                        updateLights(progress, "colorTemperature", group!!)
                    }else{
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
                    if(currentShowGroupSetPage){
                        group?.color = color
                    }else{
                        light?.color=color
                    }

                    changeColor(r.toByte(), g.toByte(), b.toByte(), false)

                }.start()
            }
        }
    }
    
    private val colorEnvelopeListener = ColorEnvelopeListener { envelope, fromUser ->

    }

    private fun changeColor(R: Byte, G: Byte, B: Byte, isOnceSet: Boolean) {

        var red = R
        var green = G
        var blue = B

        var addr = 0
        if(currentShowGroupSetPage){
            addr = group?.meshAddr!!
        }else{
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
