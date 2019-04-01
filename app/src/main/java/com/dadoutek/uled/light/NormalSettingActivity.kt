package com.dadoutek.uled.light

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.blankj.utilcode.util.LogUtils

import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.LightGroupingActivity
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.DataManager
import com.dadoutek.uled.util.OtaPrepareUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
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

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_device_setting.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*
import java.util.concurrent.TimeUnit

class NormalSettingActivity : TelinkBaseActivity(), EventListener<String>, TextView.OnEditorActionListener{
    private var localVersion: String? = null
    private var light: DbLight? = null
    private val mDisposable = CompositeDisposable()
    private var mRxPermission: RxPermissions? = null
    var gpAddress: Int = 0
    var fromWhere: String? = null
    private val dialog: AlertDialog? = null
    private var mApp: TelinkLightApplication? = null
    private var manager: DataManager? = null
    private var mConnectDevice: DeviceInfo? = null
    private var mConnectTimer: Disposable? = null
    private var isLoginSuccess = false
    private var mApplication: TelinkLightApplication? = null
    private var isRenameState=false
    private var group: DbGroup? = null
    //    private var stopTracking = false
    private var connectTimes = 0
    private var currentShowPageGroup=true

    private val clickListener = OnClickListener { v ->
        when(v.id){
            R.id.tvOta ->{
                if(isRenameState){
                    saveName()
                }else{
                    checkPermission()
                }
            }
            R.id.btnRename ->{
                renameGp()
            }
            R.id.updateGroup ->{
                updateGroup()
            }
            R.id.btnRemove ->{
                remove()
            }
            R.id.btn_remove_group -> AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(this)).setMessage(R.string.delete_group_confirm)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        (this as NormalSettingActivity).showLoadingDialog(getString(R.string.deleting))

                        deleteGroup(DBUtils.getLightByGroupID(group!!.id), group!!,
                                successCallback = {
                                    (this as NormalSettingActivity).hideLoadingDialog()
                                    this?.setResult(Constant.RESULT_OK)
                                    this?.finish()
                                },
                                failedCallback = {
                                    (this as NormalSettingActivity).hideLoadingDialog()
                                    ToastUtils.showShort(R.string.move_out_some_lights_in_group_failed)
                                })
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            R.id.btn_rename -> renameGroup()
        }
    }

    private fun renameGroup() {
//        val intent = Intent(this, RenameActivity::class.java)
//        intent.putExtra("group", group)
//        startActivity(intent)
//        this?.finish()
        editTitle.visibility=View.VISIBLE
        titleCenterName.visibility=View.GONE
        editTitle?.setFocusableInTouchMode(true)
        editTitle?.setFocusable(true)
        editTitle?.requestFocus()
        editTitle.setSelection(editTitle.getText().toString().length)
        tvRename.visibility = View.VISIBLE
        tvRename.setText(android.R.string.ok)
        tvRename.setOnClickListener {
            saveName()
            tvRename.visibility = View.GONE
        }
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editTitle, InputMethodManager.SHOW_FORCED)
        editTitle?.setOnEditorActionListener(this)
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
    
    fun addEventListeners() {
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        this.mApplication?.addEventListener(NotificationEvent.ONLINE_STATUS, this)
////        this.mApplication?.addEventListener(NotificationEvent.GET_ALARM, this)
//        this.mApplication?.addEventListener(NotificationEvent.GET_DEVICE_STATE, this)
//        this.mApplication?.addEventListener(ServiceEvent.SERVICE_CONNECTED, this)
////        this.mApplication?.addEventListener(MeshEvent.OFFLINE, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    override fun onStop() {
        super.onStop()
        mConnectTimer?.dispose()
        this.mApplication?.removeEventListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mConnectTimer?.dispose()
        mDisposable.dispose()
        this.mApplication?.removeEventListener(this)
    }

    private fun updateGroup() {
        val intent = Intent(this,
                LightGroupingActivity::class.java)
        intent.putExtra(Constant.TYPE_VIEW,Constant.LIGHT_KEY)
        intent.putExtra("light", light)
        intent.putExtra("gpAddress", gpAddress)
        intent.putExtra("uuid",light!!.productUUID)
        intent.putExtra("belongId",light!!.belongGroupId)
        startActivity(intent)
       this!!.finish()
    }

    override fun performed(event: Event<String>?) {
        when (event?.type) {
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            ErrorReportEvent.ERROR_REPORT -> onErrorReport((event as ErrorReportEvent).args)
        }
    }

    internal var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {

        override fun downLoadFileStart() {
            showLoadingDialog(getString(R.string.get_update_file))
        }

        override fun startGetVersion() {
            showLoadingDialog(getString(R.string.verification_version))
        }

        override fun getVersionSuccess(s: String) {
            //            ToastUtils.showLong(.string.verification_version_success);
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

    private fun checkPermission() {
//        if(light!!.macAddr.length<16){
//            ToastUtils.showLong(getString(R.string.bt_error))
//        }else{
            mDisposable.add(
                    mRxPermission!!.request(Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe { granted ->
                        if (granted!!) {
                            OtaPrepareUtils.instance().gotoUpdateView(this@NormalSettingActivity, localVersion, otaPrepareListner)
//                          transformView()
                        } else {
                            ToastUtils.showLong(R.string.update_permission_tip)
                        }
                    })
//        }
    }

    private fun transformView() {
        val intent = Intent(this@NormalSettingActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.UPDATE_LIGHT, light)
        startActivity(intent)
        finish()
    }

    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            Commander.getDeviceVersion(light!!.meshAddr, { s ->
                localVersion = s
                if (txtTitle != null) {
                    if (OtaPrepareUtils.instance().checkSupportOta(localVersion)!!) {
                        txtTitle!!.visibility = View.VISIBLE
                        txtTitle!!.text = resources.getString(R.string.firmware_version,localVersion)
                        light!!.version = localVersion
                        tvOta!!.visibility = View.VISIBLE
                    } else {
                        txtTitle!!.visibility = View.VISIBLE
                        txtTitle!!.text = resources.getString(R.string.firmware_version,localVersion)
                        light!!.version = localVersion
                        tvOta!!.visibility = View.GONE
                    }
                }
                null
            }, {
                if (txtTitle != null) {
                    txtTitle!!.visibility = View.GONE
                    tvOta!!.visibility = View.GONE
                }
                null
            })
        } else {
            dstAdress = 0
        }
    }

    private fun onErrorReport(info: ErrorReportInfo) {
//        LogUtils.d("onErrorReport current device mac = ${bestRSSIDevice?.macAddress}")
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        com.dadoutek.uled.util.LogUtils.d("蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        com.dadoutek.uled.util.LogUtils.d("无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        com.dadoutek.uled.util.LogUtils.d("未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        com.dadoutek.uled.util.LogUtils.d("未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        com.dadoutek.uled.util.LogUtils.d("未建立物理连接")
                    }
                }

                autoConnect()

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        com.dadoutek.uled.util.LogUtils.d("value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        com.dadoutek.uled.util.LogUtils.d("read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        com.dadoutek.uled.util.LogUtils.d("write login data 没有收到response")
                    }
                }

                autoConnect()

            }
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
                            showLoadingDialog(getString(R.string.connect_failed))
                            finish()
                        }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setContentView(R.layout.activity_device_setting)
        initType()
        this.mApplication = this.application as TelinkLightApplication
    }

    override fun onResume() {
        super.onResume()
        addEventListeners()
//        test()
    }

    private fun initType() {
        val type=intent.getStringExtra(Constant.TYPE_VIEW)
        if(type==Constant.TYPE_GROUP){
            currentShowPageGroup=true
            show_light_btn.visibility=View.GONE
            show_group_btn.visibility=View.VISIBLE
            initDataGroup()
            initViewGroup()
        }else{
            currentShowPageGroup=false
            show_light_btn.visibility=View.VISIBLE
            show_group_btn.visibility=View.GONE
            initToolbarLight()
            initViewLight()
            getVersion()
        }
    }

    private fun test(){
        for(i in 0..100){
            Thread{
                Thread.sleep(1000)
                getVersionTest()
            }.start()
        }
    }

    var count=0
    private fun getVersionTest() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            Commander.getDeviceVersion(light!!.meshAddr, { s ->
                localVersion = s
                if(!localVersion!!.startsWith("LC")){
                    ToastUtils.showLong("版本号出错："+localVersion)
                    txtTitle!!.visibility = View.VISIBLE
                    txtTitle!!.text = resources.getString(R.string.firmware_version,localVersion)
                    light!!.version = localVersion
                    tvOta!!.visibility = View.VISIBLE
                }else{
                    count++
                    ToastUtils.showShort("版本号正确次数："+localVersion)
                    txtTitle!!.visibility = View.GONE
                    tvOta!!.visibility = View.GONE
                }
                null
            }, {
                if (txtTitle != null) {
                    txtTitle!!.visibility = View.GONE
                    tvOta!!.visibility = View.GONE
                }
                null
            })
        } else {
            dstAdress = 0
        }
    }

    private fun initViewGroup() {
        editTitle.visibility=View.GONE
        titleCenterName.visibility=View.VISIBLE
        if (group != null) {
            if (group!!.meshAddr == 0xffff) {
                editTitle!!.setText(getString(R.string.allLight))
                titleCenterName!!.text = getString(R.string.allLight)
            } else {
                editTitle!!.setText(group!!.name)
                titleCenterName!!.text = group!!.name
            }
        }

        toolbar.title=""
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        btn_remove_group?.setOnClickListener(clickListener)
        btn_rename?.setOnClickListener(clickListener)

        checkGroupIsSystemGroup()
        sbBrightness!!.progress = group!!.brightness
        tvBrightness.text = getString(R.string.device_setting_brightness, group!!.brightness.toString() + "")
        sbTemperature!!.progress = group!!.colorTemperature
        tvTemperature!!.text = getString(R.string.device_setting_temperature, group!!.colorTemperature.toString() + "")


        this.sbBrightness!!.setOnSeekBarChangeListener(this.barChangeListener)
        this.sbTemperature!!.setOnSeekBarChangeListener(this.barChangeListener)
    }

    //所有灯控分组暂标为系统默认分组不做修改处理
    private fun checkGroupIsSystemGroup() {
        if (group!!.meshAddr == 0xFFFF) {
            show_group_btn!!.visibility=View.GONE
        }
    }

    private fun initDataGroup() {
        this.mApplication = this.application as TelinkLightApplication
        this.group = this.intent.extras!!.get("group") as DbGroup
    }

    private fun initToolbarLight() {
        toolbar.title = ""
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(editTitle?.getWindowToken(), 0)
                editTitle?.setFocusableInTouchMode(false)
                editTitle?.setFocusable(false)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initViewLight() {
        this.mApp = this.application as TelinkLightApplication?
        manager = DataManager(mApp, mApp!!.mesh.name, mApp!!.mesh.password)
        this.light = this.intent.extras!!.get(Constant.LIGHT_ARESS_KEY) as DbLight
        this.fromWhere = this.intent.getStringExtra(Constant.LIGHT_REFRESH_KEY)
        this.gpAddress = this.intent.getIntExtra(Constant.GROUP_ARESS_KEY, 0)
        txtTitle!!.text = ""
        editTitle!!.setText(light?.name)
        editTitle!!.visibility=View.GONE
        titleCenterName.visibility=View.VISIBLE
        titleCenterName.setText(light?.name)

        tvOta!!.setOnClickListener(this.clickListener)
        updateGroup.setOnClickListener(this.clickListener)
        btnRemove.setOnClickListener(this.clickListener)
        btnRename.setOnClickListener(clickListener)
        mRxPermission = RxPermissions(this)

        this.sbBrightness?.max = 100
        this.sbTemperature?.max = 100

//        this.colorPicker.setOnColorChangeListener(this.colorChangedListener);
        mConnectDevice = TelinkLightApplication.getInstance().connectDevice
        sbBrightness?.progress = light!!.brightness
        tvBrightness.text = getString(R.string.device_setting_brightness, light?.brightness.toString() + "")
        sbTemperature?.progress = light!!.colorTemperature
        tvTemperature.text = getString(R.string.device_setting_temperature, light?.colorTemperature.toString() + "")

//        sendInitCmd(light.getBrightness(),light.getColorTemperature());

        this.sbBrightness?.setOnSeekBarChangeListener(this.barChangeListener)
        this.sbTemperature?.setOnSeekBarChangeListener(this.barChangeListener)
    }

    private fun renameGp() {
//        val intent = Intent(this, RenameActivity::class.java)
//        intent.putExtra("group", group)
//        startActivity(intent)
//        this?.finish()
        editTitle.visibility=View.VISIBLE
        titleCenterName.visibility=View.GONE
        isRenameState=true
        tvOta.setText(android.R.string.ok)
        editTitle?.setFocusableInTouchMode(true)
        editTitle?.setFocusable(true)
        editTitle?.requestFocus()
        //设置光标默认在最后
        editTitle.setSelection(editTitle.getText().toString().length)
//        btn_sure_edit_rename.visibility = View.VISIBLE
//        btn_sure_edit_rename.setOnClickListener {
//            saveName()
//            btn_sure_edit_rename.visibility = View.GONE
//        }
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editTitle, InputMethodManager.SHOW_FORCED)
        editTitle?.setOnEditorActionListener(this)
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        doWhichOperation(actionId)
        return true
    }

    private fun doWhichOperation(actionId: Int) {
        when (actionId) {
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_NONE -> {
                saveName()
                if(currentShowPageGroup){
                    tvRename.visibility = View.GONE
                }
            }
        }
    }

    private fun saveName() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editTitle?.getWindowToken(), 0)
        editTitle?.setFocusableInTouchMode(false)
        editTitle?.setFocusable(false)
        if(!currentShowPageGroup){
            checkAndSaveName()
            isRenameState=false
            tvOta.setText(R.string.ota)
        }else{
            checkAndSaveNameGp()
        }
    }

    private fun checkAndSaveNameGp() {
        val name = editTitle?.text.toString().trim()
        if (compileExChar(name)) {
            Toast.makeText(this, R.string.rename_tip_check, Toast.LENGTH_SHORT).show()

            editTitle.visibility=View.GONE
            titleCenterName.visibility=View.VISIBLE
            titleCenterName.text = group?.name
        }
        else {
            var canSave=true
            val groups=DBUtils.allGroups
            for(i in groups.indices){
                if(groups[i].name==name){
                    ToastUtils.showLong(TelinkLightApplication.getInstance().getString(R.string.repeat_name))
                    canSave=false
                    break
                }
            }

            if(canSave){
                editTitle.visibility=View.GONE
                titleCenterName.visibility=View.VISIBLE
                titleCenterName.text = name
                group?.name = name
                DBUtils.updateGroup(group!!)
            }
        }
    }

    private fun checkAndSaveName() {
        val name = editTitle?.text.toString().trim()
        if (compileExChar(name)) {
            Toast.makeText(this, R.string.rename_tip_check, Toast.LENGTH_SHORT).show()
            editTitle.visibility=View.GONE
            titleCenterName.visibility=View.VISIBLE
            titleCenterName.text = light?.name
        }else{
            editTitle.visibility=View.GONE
            titleCenterName.visibility=View.VISIBLE
            titleCenterName.text = name
            light?.name=name
            DBUtils.updateLight(light!!)
        }
    }

    private val barChangeListener = object : SeekBar.OnSeekBarChangeListener {

        private var preTime: Long = 0
        private val delayTime = Constant.MAX_SCROLL_DELAY_VALUE

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            LogUtils.d("progress:_3__"+seekBar.progress)
            this.onValueChange(seekBar, seekBar.progress, true,true)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            LogUtils.d("progress:_1__"+seekBar.progress)
            this.preTime = System.currentTimeMillis()
            this.onValueChange(seekBar, seekBar.progress, true,false)
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                       fromUser: Boolean) {
            LogUtils.d("progress:_2__"+progress)
            val currentTime = System.currentTimeMillis()

            onValueChangeView(seekBar, progress, true,false)
            if (currentTime - this.preTime > this.delayTime) {
                this.onValueChange(seekBar, progress, true,false)
                this.preTime = currentTime
            }

        }

        private fun onValueChangeView(view: View, progress: Int, immediate: Boolean,isStopTracking:
        Boolean){
            if (view === sbBrightness) {
                tvBrightness.text = getString(R.string.device_setting_brightness, progress.toString() + "")
            } else if (view === sbTemperature) {
                tvTemperature.text = getString(R.string.device_setting_temperature, progress.toString() + "")
            }
        }

        private fun onValueChange(view: View, progress: Int, immediate: Boolean,isStopTracking:
        Boolean) {
            var addr = 0
            if(currentShowPageGroup){
                addr = group?.meshAddr!!
            }else{
                addr = light?.meshAddr!!
            }

            val opcode: Byte
            var params: ByteArray
            if (view === sbBrightness) {
                //                progress += 5;
                //                Log.d(TAG, "onValueChange: "+progress);
                opcode = Opcode.SET_LUM
                params = byteArrayOf(progress.toByte())

                if(currentShowPageGroup){
                    group?.brightness = progress
                }else{
                    light?.brightness = progress
                }

                if(progress>Constant.MAX_VALUE){
                    params = byteArrayOf(Constant.MAX_VALUE.toByte())
                    TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params)
                }else{
                    TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params)
                }

                if(isStopTracking){
                    if(currentShowPageGroup){
                        DBUtils.updateGroup(group!!)
                        updateLights(progress, "brightness", group!!)
                    }else{
                        DBUtils.updateLight(light!!)
                    }
                }
            } else if (view === sbTemperature) {

                opcode = Opcode.SET_TEMPERATURE
                params = byteArrayOf(0x05, progress.toByte())

                if(currentShowPageGroup){
                    group?.colorTemperature = progress
                }else{
                    light?.colorTemperature = progress
                }
               
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params)
                if(isStopTracking){
                    if(currentShowPageGroup){
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
                }
                DBUtils.updateLight(dbLight)
            }
        }.start()
    }

    private fun sendInitCmd(brightness: Int, colorTemperature: Int) {
        val addr = light?.meshAddr
        var opcode: Byte
        var params: ByteArray
        //                progress += 5;
        //                Log.d(TAG, "onValueChange: "+progress);
        opcode = Opcode.SET_LUM
        params = byteArrayOf(brightness.toByte())
        light?.brightness = brightness
        TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)

        try {
            Thread.sleep(200)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            opcode = Opcode.SET_TEMPERATURE
            params = byteArrayOf(0x05, colorTemperature.toByte())
            light?.colorTemperature = colorTemperature
            TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params)
        }
    }

    fun remove() {
        AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_light_confirm)
                .setPositiveButton(android.R.string.ok) { dialog, which ->

                    if (TelinkLightService.Instance().adapter.mLightCtrl.currentLight.isConnected) {
                        val opcode = Opcode.KICK_OUT
                        TelinkLightService.Instance().sendCommandNoResponse(opcode, light!!.getMeshAddr(), null)
                        DBUtils.deleteLight(light!!)
                        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(light!!.getMeshAddr())) {
                            TelinkLightApplication.getApp().mesh.saveOrUpdate(this)
                        }
                        if (mConnectDevice != null) {
                            Log.d(this.javaClass.getSimpleName(), "mConnectDevice.meshAddress = " + mConnectDevice?.meshAddress)
                            Log.d(this.javaClass.getSimpleName(), "light.getMeshAddr() = " + light?.getMeshAddr())
                            if (light?.meshAddr == mConnectDevice?.meshAddress) {
                                this.setResult(Activity.RESULT_OK, Intent().putExtra("data", true))
                            }
                        }
                        this.finish()


                    } else {
                        ToastUtils.showLong("当前处于未连接状态，重连中。。。")
                        this.finish()
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    /**
     * 自动重连
     */
    private fun autoConnect() {

        if (TelinkLightService.Instance() != null) {

            if (TelinkLightService.Instance().mode != LightAdapter.MODE_AUTO_CONNECT_MESH) {

                ToastUtils.showLong(getString(R.string.connecting))
                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

                if (this.mApp?.isEmptyMesh != false)
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(Constant.RESULT_OK)
            finish()
            return false
        } else {
            return super.onKeyDown(keyCode, event)
        }

    }
}
