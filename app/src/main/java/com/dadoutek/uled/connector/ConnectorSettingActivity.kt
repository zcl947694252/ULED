package com.dadoutek.uled.connector

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
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbConnector
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.ota.OTAConnectorActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.DataManager
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.StringUtils
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
import kotlinx.android.synthetic.main.connector_device_setting.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*
import java.util.concurrent.TimeUnit

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1

class ConnectorSettingActivity : TelinkBaseActivity(), EventListener<String>, TextView.OnEditorActionListener {

    private var localVersion: String? = null
    private var light: DbConnector? = null
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
    private var isRenameState = false
    private var group: DbGroup? = null
    //    private var stopTracking = false
    private var connectTimes = 0
    private var currentShowPageGroup = true

    private val clickListener = OnClickListener { v ->
        when (v.id) {
//            R.id.tvOta ->{
//                if(isRenameState){
//                    saveName()
//                }else{
//                    checkPermission()
//                }
//            }
            R.id.btnRename -> {
                renameGp()
            }
            R.id.updateGroup -> {
                updateGroup()
            }
            R.id.btnRemove -> {
                remove()
            }
            R.id.btn_remove_group -> AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(this)).setMessage(R.string.delete_group_confirm)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        this.showLoadingDialog(getString(R.string.deleting))

                        deleteGroup(DBUtils.getConnectorByGroupID(group!!.id), group!!,
                                successCallback = {
                                    (this ).hideLoadingDialog()
                                    this?.setResult(Constant.RESULT_OK)
                                    this?.finish()
                                },
                                failedCallback = {
                                    (this ).hideLoadingDialog()
                                    ToastUtils.showShort(R.string.move_out_some_lights_in_group_failed)
                                })
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            R.id.btn_rename -> renameGroup()

            R.id.btnOTA -> {
                if (txtTitle.text.toString() != null && txtTitle.text.toString() != "") {
                    checkPermission()
                } else {
                    Toast.makeText(this, R.string.number_no, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun renameGroup() {
        val textGp = EditText(this)
        textGp.setText(group?.name)
        StringUtils.initEditTextFilter(textGp)
        textGp.setSelection(textGp.text.toString().length)
        android.app.AlertDialog.Builder(this@ConnectorSettingActivity)
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
                                ToastUtils.showLong(TelinkLightApplication.getApp().getString(R.string.repeat_name))
                                canSave = false
                                break
                            }
                        }

                        if (canSave) {
                            group?.name = textGp.text.toString().trim { it <= ' ' }
                            DBUtils.updateGroup(group!!)
                            relayName.text = group?.name
                            dialog.dismiss()
                        }
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }

    fun remove() {
        AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_light_confirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->

                    if (TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight != null && TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight.isConnected) {
                        val opcode = Opcode.KICK_OUT
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, light!!.meshAddr, null)
                        DBUtils.deleteConnector(light!!)
                        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(light!!.meshAddr)) {
                            TelinkLightApplication.getApp().mesh.saveOrUpdate(this)
                        }
                        if (mConnectDevice != null) {
                            Log.d(this.javaClass.simpleName, "mConnectDevice.meshAddress = " + mConnectDevice?.meshAddress)
                            Log.d(this.javaClass.simpleName, "light.getMeshAddr() = " + light?.meshAddr)
                            if (light?.meshAddr == mConnectDevice?.meshAddress) {
                                this.setResult(Activity.RESULT_OK, Intent().putExtra("data", true))
                            }
                        }
                        this.finish()
                    } else {
                        ToastUtils.showLong(getString(R.string.bluetooth_open_connet))
                        this.finish()
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }


    private fun updateGroup() {
        val intent = Intent(this,
                ConnectorGroupingActivity::class.java)
        if (light==null){
            ToastUtils.showShort(getString(R.string.please_connect_normal_light))
            TelinkLightService.Instance()?.idleMode(true)
            TelinkLightService.Instance()?.disconnect()
            return
        }
        intent.putExtra(Constant.TYPE_VIEW, Constant.LIGHT_KEY)
        intent.putExtra("light", light)
        intent.putExtra("gpAddress", gpAddress)
        intent.putExtra("uuid", light!!.productUUID)
        Log.d("addLight", light!!.productUUID.toString() + "," + light!!.meshAddr)
        startActivity(intent)
        this!!.finish()
    }


    /**
     * 删除组，并且把组里的灯的组也都删除。
     */
    private fun deleteGroup(lights: MutableList<DbConnector>, group: DbGroup, retryCount: Int = 0,
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
                                DBUtils.updateConnector(light)
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
                }
            } else {
                DBUtils.deleteGroupOnly(group)
                this?.runOnUiThread {
                    successCallback.invoke()
                }
            }
        }.start()

    }

    private fun deleteAllSceneByLightAddr(lightMeshAddr: Int) {
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val params: ByteArray = byteArrayOf(0x00, 0xff.toByte())
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, lightMeshAddr, params)
    }

    private fun saveName() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editTitle?.windowToken, 0)
        editTitle?.isFocusableInTouchMode = false
        editTitle?.isFocusable = false
        if (!currentShowPageGroup) {
            checkAndSaveName()
            isRenameState = false
//            tvOta.setText(R.string.ota)
        } else {
            checkAndSaveNameGp()
        }
    }

    private fun checkAndSaveName() {
        val name = editTitle?.text.toString().trim()
        if (compileExChar(name)) {
            Toast.makeText(this, R.string.rename_tip_check, Toast.LENGTH_SHORT).show()
//            editTitle.visibility=View.GONE
            relayName.visibility = View.VISIBLE
            relayName.text = light?.name
        } else {
//            editTitle.visibility=View.GONE
            relayName.visibility = View.VISIBLE
            relayName.text = name
            light?.name = name
            DBUtils.updateConnector(light!!)
        }
    }

    private fun checkAndSaveNameGp() {
        val name = editTitle?.text.toString().trim()
        if (compileExChar(name)) {
            Toast.makeText(this, R.string.rename_tip_check, Toast.LENGTH_SHORT).show()

//            editTitle.visibility=View.GONE
            relayName.visibility = View.VISIBLE
            relayName.text = group?.name
        } else {
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
//                editTitle.visibility=View.GONE
                relayName.visibility = View.VISIBLE
                relayName.text = name
                group?.name = name
                DBUtils.updateGroup(group!!)
            }
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
                        var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_DEVELOPER_MODE, false)
                        if (isBoolean) {
                            transformView()
                        } else {
                            OtaPrepareUtils.instance().gotoUpdateView(this@ConnectorSettingActivity, localVersion, otaPrepareListner)
                        }
                    } else {
                        ToastUtils.showLong(R.string.update_permission_tip)
                    }
                })
//        }
    }

    private var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {

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

    private fun transformView() {
        val intent = Intent(this@ConnectorSettingActivity, OTAConnectorActivity::class.java)
        intent.putExtra(Constant.UPDATE_LIGHT, light)
        startActivity(intent)
        finish()
    }

    private fun renameGp() {
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(light?.name)
        textGp.setSelection(textGp.text.toString().length)
        android.app.AlertDialog.Builder(this@ConnectorSettingActivity)
                .setTitle(R.string.rename)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        light?.name = textGp.text.toString().trim { it <= ' ' }
                        DBUtils.updateConnector(light!!)
                        relayName.text = light?.name
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }

    private fun onErrorReport(info: ErrorReportInfo) {
        LogUtils.e("onErrorReport current device mac ")
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        // ("蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        // ("无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        // ("未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        // ("未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        //("未建立物理连接")
                    }
                }

                autoConnect()

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        //  ("value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        //("read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        // ("write login data 没有收到response")
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
                mConnectTimer?.dispose()
                mConnectTimer = Observable.timer(15, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                        .subscribe({
                            LogUtils.d("STATUS_LOGOUT")
                            showLoadingDialog(getString(R.string.connect_fail))
                            finish()
                        },{})
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_connector_setting)
        initType()
        this.mApplication = this.application as TelinkLightApplication
    }

    override fun onResume() {
        super.onResume()
        addEventListeners()
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

    fun addEventListeners() {
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        this.mApplication?.addEventListener(NotificationEvent.ONLINE_STATUS, this)
////        this.mApplication?.addEventListener(NotificationEvent.GET_ALARM, this)
//        this.mApplication?.addEventListener(NotificationEvent.GET_DEVICE_STATE, this)
//        this.mApplication?.addEventListener(ServiceEvent.SERVICE_CONNECTED, this)
////        this.mApplication?.addEventListener(MeshEvent.OFFLINE, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        doWhichOperation(actionId)
        return true
    }

    override fun performed(event: Event<String>?) {
        when (event?.type) {
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            ErrorReportEvent.ERROR_REPORT -> onErrorReport((event as ErrorReportEvent).args)
        }
    }

    private fun doWhichOperation(actionId: Int) {
        when (actionId) {
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_NONE -> {
                saveName()
                if (currentShowPageGroup) {
                    tvRename.visibility = View.GONE
                }
            }
        }
    }

    private fun initType() {
        val type = intent.getStringExtra(Constant.TYPE_VIEW)
        if (type == Constant.TYPE_GROUP) {
            currentShowPageGroup = true
            show_light_btn.visibility = View.GONE
            show_group_btn.visibility = View.VISIBLE
            initDataGroup()
            initViewGroup()
        } else {
            currentShowPageGroup = false
            show_light_btn.visibility = View.VISIBLE
            show_group_btn.visibility = View.GONE
//            initToolbarLight()
            initViewLight()
            getVersion()
        }
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
        this.light = this.intent.extras!!.get(Constant.LIGHT_ARESS_KEY) as DbConnector
        this.fromWhere = this.intent.getStringExtra(Constant.LIGHT_REFRESH_KEY)
        this.gpAddress = this.intent.getIntExtra(Constant.GROUP_ARESS_KEY, 0)
        txtTitle.visibility = View.VISIBLE
        txtTitle!!.text = ""
//        editTitle!!.setText(light?.name)
//        editTitle!!.visibility=View.GONE
        relayName.visibility = View.VISIBLE
        relayName.setText(light?.name)

//        tvOta!!.setOnClickListener(this.clickListener)
        updateGroup.setOnClickListener(this.clickListener)
        btnRemove.setOnClickListener(this.clickListener)
        btnRename.setOnClickListener(clickListener)
        btnOTA.setOnClickListener(clickListener)
        mRxPermission = RxPermissions(this)

//        this.sbBrightness?.max = 100
//        this.sbTemperature?.max = 100

//        this.colorPicker.setOnColorChangeListener(this.colorChangedListener);
        mConnectDevice = TelinkLightApplication.getApp().connectDevice
//        sbBrightness?.progress = light!!.brightness
//        tvBrightness.text = getString(R.string.device_setting_brightness, light?.brightness.toString() + "")
//        sbTemperature?.progress = light!!.colorTemperature
//        tvTemperature.text = getString(R.string.device_setting_temperature, light?.colorTemperature.toString() + "")

//        sendInitCmd(light.getBrightness(),light.getColorTemperature());

    }

    private fun initToolbarLight() {
        toolbar.title = ""
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initDataGroup() {
        this.mApplication = this.application as TelinkLightApplication
        this.group = this.intent.extras!!.get("group") as DbGroup
    }

    private fun initViewGroup() {
//        editTitle.visibility=View.GONE
        relayName.visibility = View.VISIBLE
        if (group != null) {
            if (group!!.meshAddr == 0xffff) {
//                editTitle!!.setText(getString(R.string.allLight))
                relayName!!.text = getString(R.string.allLight)
            } else {
//                editTitle!!.setText(group!!.name)
                relayName!!.text = group!!.name
            }
        }

        txtTitle.visibility = View.GONE

//        toolbar.title=""
//        setSupportActionBar(toolbar)
//        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        btn_remove_group?.setOnClickListener(clickListener)
        btn_rename?.setOnClickListener(clickListener)

//        checkGroupIsSystemGroup()
//        sbBrightness!!.progress = group!!.brightness
//        tvBrightness.text = getString(R.string.device_setting_brightness, group!!.brightness.toString() + "")
//        sbTemperature!!.progress = group!!.colorTemperature
//        tvTemperature!!.text = getString(R.string.device_setting_temperature, group!!.colorTemperature.toString() + "")


    }

    //所有灯控分组暂标为系统默认分组不做修改处理
//    private fun checkGroupIsSystemGroup() {
//        if (group!!.meshAddr == 0xFFFF) {
//            show_group_btn!!.visibility=View.GONE
//        }
//    }

    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            Commander.getDeviceVersion(light!!.meshAddr, { s ->
                localVersion = s
                if (txtTitle != null) {
                    if (OtaPrepareUtils.instance().checkSupportOta(localVersion)!!) {
                        txtTitle!!.visibility = View.VISIBLE
                        txtTitle!!.text = resources.getString(R.string.firmware_version, localVersion)
                        light!!.version = localVersion
//                        tvOta!!.visibility = View.VISIBLE
                    } else {
                        txtTitle!!.visibility = View.VISIBLE
                        txtTitle!!.text = resources.getString(R.string.firmware_version, localVersion)
                        light!!.version = localVersion
//                        tvOta!!.visibility = View.GONE
                    }
                }
                null
            }, {
                if (txtTitle != null) {
//                    txtTitle!!.visibility = View.GONE
//                    tvOta!!.visibility = View.GONE
                }
                null
            })
        } else {
            dstAdress = 0
        }
    }

    /**
     * 自动重连
     */
    private fun autoConnect() {

        if (TelinkLightService.Instance() != null) {

            if (TelinkLightService.Instance()?.mode != LightAdapter.MODE_AUTO_CONNECT_MESH) {

                ToastUtils.showLong(getString(R.string.connecting))


                if (this.mApp?.isEmptyMesh != false)
                    return


                val mesh = this.mApp?.mesh

                if (TextUtils.isEmpty(mesh?.name) || TextUtils.isEmpty(mesh?.password)) {
                    TelinkLightService.Instance()?.idleMode(true)
                    return
                }

                //自动重连参数
                val connectParams = Parameters.createAutoConnectParameters()
                connectParams.setMeshName(DBUtils.lastUser?.controlMeshName)
                connectParams.setPassword(NetworkFactory.md5(NetworkFactory.md5(DBUtils.lastUser?.controlMeshName) + DBUtils.lastUser?.controlMeshName))

                connectParams.autoEnableNotification(true)

                // 之前是否有在做MeshOTA操作，是则继续
                if (mesh!!.isOtaProcessing) {
                    connectParams.setConnectMac(mesh?.otaDevice!!.mac)
                }
                //自动重连
                TelinkLightService.Instance()?.autoConnect(connectParams)
            }

            //刷新Notify参数
            val refreshNotifyParams = Parameters.createRefreshNotifyParameters()
            refreshNotifyParams.setRefreshRepeatCount(2)
            refreshNotifyParams.setRefreshInterval(2000)
            //开启自动刷新Notify
            TelinkLightService.Instance()?.autoRefreshNotify(refreshNotifyParams)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(Constant.RESULT_OK)
            finish()
            false
        } else {
            super.onKeyDown(keyCode, event)
        }
    }
}
