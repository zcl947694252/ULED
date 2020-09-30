package com.dadoutek.uled.pir

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbScene
import com.dadoutek.uled.model.dbModel.DbSensor
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.othersview.SelectDeviceTypeActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.MeshAddressGenerator
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_config_pir.*
import kotlinx.android.synthetic.main.template_loading_progress.*
import kotlinx.android.synthetic.main.template_radiogroup.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.snackbar
import java.util.*
import kotlin.collections.ArrayList

/**
 * 老版本人体感应器设置详情
 */
class ConfigSensorAct : TelinkBaseActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener, EventListener<String> {
    private var connectDispose: Disposable? = null
    private var disposableReset: Disposable? = null
    private var currentSensor: DbSensor? = null
    private var isReConfirm: Boolean = false
    private var renameEditText: EditText? = null
    private var fiRename: MenuItem? = null
    private var fiVersion: MenuItem? = null
    private lateinit var mScenes: List<DbScene>
    private var version: String = ""
    private var isGroupMode: Boolean = true
    private lateinit var telinkApplication: TelinkApplication
    private lateinit var mDeviceInfo: DeviceInfo
    private lateinit var mGroups: List<DbGroup>
    private var mGroupScenesName: ArrayList<String>? = null
    private var mSelectGroupSceneAddr: Int = 0xFF  //代表所有灯
    private var isSupportModeSelect = false
    private var isSupportDelayUnitSelect = false
    private var modeStartUpMode = 0
    private var modeDelayUnit = 0
    private var modeSwitchMode = 0
    private val MODE_START_UP_MODE_OPEN = 0
    private val MODE_DELAY_UNIT_SECONDS = 0
    private val MODE_SWITCH_MODE_MOMENT = 0
    private val MODE_START_UP_MODE_CLOSE = 1
    private val MODE_DELAY_UNIT_MINUTE = 2
    private val MODE_SWITCH_MODE_GRADIENT = 4
    private val groupSceneAdapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mGroupScenesName)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_pir)
        top_rg_ly.visibility = View.GONE
        telinkApplication = this.application as TelinkApplication
        initToolbar()
        initData()
        initView()
        makePopuwindow()
        initListener()
        if (TelinkLightApplication.getApp().connectDevice == null)
            autoConnectSensor()
        val version = intent.getStringExtra("version")
        getVersion(version)
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
        AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_switch_confirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (currentSensor == null)
                        ToastUtils.showShort(getString(R.string.invalid_data))
                    else {
                        showLoadingDialog(getString(R.string.please_wait))
                        disposableReset = Commander.resetDevice(currentSensor!!.meshAddr, true)
                                .subscribe({
                                  //  deleteData()
                                }, {
                                  GlobalScope.launch(Dispatchers.Main){
                                    /*    showDialogHardDelete?.dismiss()
                                      showDialogHardDelete = android.app.AlertDialog.Builder(this).setMessage(R.string.delete_device_hard_tip)
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
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
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
        if (isBoolean) {
            transformView()
        } else {
            if (OtaPrepareUtils.instance().checkSupportOta(version)!!) {
                OtaPrepareUtils.instance().gotoUpdateView(this@ConfigSensorAct, version, object : OtaPrepareListner {
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
        val intent = Intent(this@ConfigSensorAct, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.OTA_MAC, currentSensor?.macAddr)
        intent.putExtra(Constant.OTA_MES_Add, currentSensor?.meshAddr)
        intent.putExtra(Constant.OTA_VERSION, currentSensor?.version)
        intent.putExtra(Constant.OTA_TYPE, DeviceType.SENSOR)
        startActivity(intent)
        finish()
    }

    private fun initListener() {
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        top_rg_ly.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.color_mode_rb -> {
                    isGroupMode = true
                    getGroupName()
                    tvSelectGroupScene.text = getString(R.string.choose_group)
                    color_mode_rb.setTextColor(getColor(R.color.blue_text))
                    gradient_mode_rb.setTextColor(getColor(R.color.gray9))
                }
                R.id.gradient_mode_rb -> {
                    isGroupMode = false
                    getSceneName()
                    tvSelectGroupScene.text = getString(R.string.choose_scene)
                    color_mode_rb.setTextColor(getColor(R.color.gray9))
                    gradient_mode_rb.setTextColor(getColor(R.color.blue_text))
                }
            }
        }
        fabConfirm.setOnClickListener(this)

        telinkApplication.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
        telinkApplication.addEventListener(LeScanEvent.LE_SCAN, this)//扫描jt
        telinkApplication.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)//超时jt
        telinkApplication.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)//结束jt
        telinkApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this)//设备状态JT
        telinkApplication.addEventListener(DeviceEvent.CURRENT_CONNECT_CHANGED, this)//设备状态JT
    }


    override fun performed(event: Event<String>) {
        when (event.type) {
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                hideLoadingDialog()
                onErrorReportNormal(info)
            }
            LeScanEvent.LE_SCAN -> {
                progressBar_sensor.visibility = View.VISIBLE
            }
            LeScanEvent.LE_SCAN_TIMEOUT -> {
                progressBar_sensor.visibility = View.GONE
                hideLoadingDialog()
            }
            LeScanEvent.LE_SCAN_COMPLETED -> {
                progressBar_sensor.visibility = View.GONE
            }

            DeviceEvent.CURRENT_CONNECT_CHANGED -> Log.e("zcl", "zcl******CURRENT_CONNECT_CHANGED")

            DeviceEvent.STATUS_CHANGED -> {
                hideLoadingDialog()
                progressBar_sensor.visibility = View.GONE
                var status = (event as DeviceEvent).args.status
                Log.e("zcl", "zcl******STATUS_CHANGED$status")
                when (status) {
                    LightAdapter.STATUS_LOGIN -> {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                        Log.e("zcl", "zcl***STATUS_LOGIN***")
                    }
                    LightAdapter.STATUS_LOGOUT -> {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                        Log.e("zcl", "zcl***STATUS_LOGOUT***----------")
                        autoConnectSensor()
                    }
                }
            }
        }
    }


    private fun initView() {
        spSelectGroupScene.adapter = groupSceneAdapter
        spSelectGroupScene.onItemSelectedListener = this
        spSelectStartupMode.onItemSelectedListener = this
        spDelayUnit.onItemSelectedListener = this
        spSwitchMode.onItemSelectedListener = this
    }

    private fun autoConnectSensor() {
        if (Constant.IS_ROUTE_MODE) return
        //自动重连参数
        val connectParams = Parameters.createAutoConnectParameters()
        connectParams.setMeshName(DBUtils.lastUser?.controlMeshName)
        connectParams.setConnectMac(mDeviceInfo.macAddress)
        connectParams.setPassword(NetworkFactory.md5(NetworkFactory.md5(DBUtils.lastUser?.controlMeshName) + DBUtils.lastUser?.controlMeshName).substring(0, 16))
        connectParams.autoEnableNotification(true)

        //连接，如断开会自动重连
        Thread { TelinkLightService.Instance()?.autoConnect(connectParams) }.start()
        //刷新Notify参数
        val refreshNotifyParams = Parameters.createRefreshNotifyParameters()
        refreshNotifyParams.setRefreshRepeatCount(3)
        refreshNotifyParams.setRefreshInterval(1000)
        //开启自动刷新Notify
        TelinkLightService.Instance()?.autoRefreshNotify(refreshNotifyParams)
        showLoadingDialog(getString(R.string.connecting_tip))
    }

    private fun getVersion(version: String) {
        val versionNum = Integer.parseInt(StringUtils.versionResolution(version, 1))
        versionLayoutPS.visibility = View.VISIBLE
        tvPSVersion.text = version
        this.version = version
        isSupportModeSelect = (version).contains("PS")

        if (isSupportModeSelect) {
            tvSelectStartupMode.visibility = View.VISIBLE
            spSelectStartupMode.visibility = View.VISIBLE

            if (versionNum >= 113) {
                isSupportDelayUnitSelect = true
                tvSwitchMode.visibility = View.VISIBLE
                spSwitchMode.visibility = View.VISIBLE
                tvDelayUnit.visibility = View.VISIBLE
                spDelayUnit.visibility = View.VISIBLE
            }
        }
    }

    private fun initToolbar() {
        toolbarTv.text = getString(R.string.sensor)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { configReturn() }
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val moreIcon = ContextCompat.getDrawable(toolbar.context, R.drawable.abc_ic_menu_overflow_material)
        if (moreIcon != null) {
            moreIcon.setColorFilter(ContextCompat.getColor(toolbar.context, R.color.black), PorterDuff.Mode.SRC_ATOP)
            toolbar.overflowIcon = moreIcon
        }
    }

    private fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        version = intent.getStringExtra("version")
        isReConfirm = mDeviceInfo.isConfirm == 1
        if (isReConfirm){
            currentSensor = DBUtils.getSensorByMeshAddr(mDeviceInfo.meshAddress)
            toolbarTv.text = currentSensor?.name
        }
        if (version == null)
            version = mDeviceInfo.firmwareRevision
        mGroupScenesName = ArrayList()
        getGroupName()
    }

    private fun getSceneName() {
        mScenes = DBUtils.sceneAll
        mGroupScenesName?.clear()
        for (item in mScenes)
            mGroupScenesName!!.add(item.name)

        groupSceneAdapter.notifyDataSetChanged()
    }

    private fun getGroupName() {
        mGroups = DBUtils.allGroups
        mGroupScenesName?.clear()
        for (item in mGroups) {
            when (item.deviceType) {
                Constant.DEVICE_TYPE_CONNECTOR, Constant.DEVICE_TYPE_LIGHT_RGB,
                Constant.DEVICE_TYPE_LIGHT_NORMAL, Constant.DEVICE_TYPE_NO -> {
                    if (item.deviceCount > 0 || item.deviceType == Constant.DEVICE_TYPE_NO)
                        mGroupScenesName!!.add(item.name)
                }
            }
        }
        groupSceneAdapter.notifyDataSetChanged()
    }

    private fun configPir(groupAddr: Int, delayTime: Int, minBrightness: Int, triggerValue: Int, mode: Int) {
        val groupH: Byte = (groupAddr shr 8 and 0xff).toByte()
        val groupL: Byte = (groupAddr and 0xff).toByte()
        val paramBytes = if (isGroupMode)
            byteArrayOf(0x01, groupH, groupL, delayTime.toByte(), minBrightness.toByte(), triggerValue.toByte(), mode.toByte())
        else
            byteArrayOf(0x01, groupH, groupL, delayTime.toByte(), minBrightness.toByte(), triggerValue.toByte(), mode.toByte())

        TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_PIR, mDeviceInfo.meshAddress, paramBytes)
        Thread.sleep(300)
    }


    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent?.id) {
            R.id.spSelectGroupScene -> {
                mSelectGroupSceneAddr = if (isGroupMode)
                    mGroups[position].meshAddr
                else
                    mScenes[position].id.toInt()

            }
            R.id.spSelectStartupMode -> {
                if (position == 0) {//开灯
                    tietMinimumBrightness?.setText("0")
                    modeStartUpMode = MODE_START_UP_MODE_OPEN
                } else if (position == 1) {//关灯
                    tietMinimumBrightness?.setText("99")
                    modeStartUpMode = MODE_START_UP_MODE_CLOSE
                }
            }
            R.id.spSwitchMode -> {
                modeSwitchMode = if (position == 0) {
                    MODE_SWITCH_MODE_MOMENT
                } else {
                    MODE_SWITCH_MODE_GRADIENT
                }
            }
            R.id.spDelayUnit -> {
                if (position == 0) {
                    tilDelay.hint = getString(R.string.delay_minute)
                    modeDelayUnit = MODE_DELAY_UNIT_MINUTE
                } else {
                    tilDelay.hint = getString(R.string.delay_seconds)
                    modeDelayUnit = MODE_DELAY_UNIT_SECONDS
                }
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        mSelectGroupSceneAddr = 0xFF
    }

    private fun doFinish() {
        TelinkLightService.Instance()?.idleMode(true)
        TelinkLightService.Instance()?.disconnect()
        finish()
    }

    private fun configReturn() {
        if (!isReConfirm)
            AlertDialog.Builder(this)
                    .setMessage(getString(R.string.config_return))
                    .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                        startActivity(Intent(this@ConfigSensorAct, SelectDeviceTypeActivity::class.java))
                        finish()
                    }
                    .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()
        else
            doFinish()
    }

    override fun onBackPressed() {
        configReturn()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fabConfirm -> {
                if (tietDelay.text?.isEmpty() != false || tietMinimumBrightness.text?.isEmpty() != false) {
                    snackbar(configPirRoot, getString(R.string.params_cannot_be_empty))
                } else if (tietMinimumBrightness.text.toString().toInt() > 99) {
                    ToastUtils.showLong(getString(R.string.max_tip_brightness))
                } else if (tietDelay.text.toString().toInt() > 255) {
                    ToastUtils.showLong(getString(R.string.time_max_tip))
                } else {
                    Thread {
                        val mode = getModeValue()
                        if (Constant.IS_ROUTE_MODE) return@Thread
                        if (TelinkLightApplication.getApp().connectDevice == null) {
                            showLoadingDialog(getString(R.string.connecting_tip))
                            connectDispose = connect(mDeviceInfo.meshAddress, true)?.subscribe({
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.connect_success))
                            }, {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.connect_fail))
                            })
                            return@Thread
                        }
                        showLoadingDialog(getString(R.string.configuring_sensor))
                        configPir(mSelectGroupSceneAddr,
                                tietDelay.text.toString().toInt(),
                                tietMinimumBrightness.text.toString().toInt(),
                                spTriggerLux.selectedItem.toString().toInt(), mode)
                        Thread.sleep(300)
                        mDeviceInfo.meshAddress = MeshAddressGenerator().meshAddress.get()
                        Commander.updateMeshName(newMeshAddr = mDeviceInfo.meshAddress,
                                successCallback = {
                                    hideLoadingDialog()
                                    saveSensor()
                                },
                                failedCallback = {
                                    snackbar(configPirRoot, getString(R.string.config_fail))
                                    hideLoadingDialog()
                                    TelinkLightService.Instance()?.idleMode(true)
                                    TelinkLightService.Instance()?.disconnect()
                                })
                    }.start()

                }
            }
        }
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

        renameConfirm?.setOnClickListener {
            // 获取输入框的内容
            if (StringUtils.compileExChar(renameEditText?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                dbSensor.name = renameEditText?.text.toString().trim { it <= ' ' }
                DBUtils.saveSensor(dbSensor, false)
                if (!this.isFinishing)
                    renameDialog.dismiss()
            }
        }


        renameCancel?.setOnClickListener {
            if (!this.isFinishing)
                renameDialog?.dismiss()
        }

    }

    private fun saveSensor() {
        var dbSensor = DbSensor()

        if (isReConfirm) {
            dbSensor.index = mDeviceInfo.id
            if (100000000!= mDeviceInfo.id)
                dbSensor.id = mDeviceInfo.id.toLong()
        } else {//如果不是重新配置就保存进服务器
            DBUtils.saveSensor(dbSensor, isReConfirm)
            dbSensor.index = dbSensor.id.toInt()//拿到新的服务器id
        }


        dbSensor.controlGroupAddr = mSelectGroupSceneAddr.toString()
        dbSensor.macAddr = mDeviceInfo.macAddress
        dbSensor.version = version
        dbSensor.meshAddr = mDeviceInfo.meshAddress
        // dbSensor.meshAddr = Constant.SWITCH_PIR_ADDRESS
        dbSensor.productUUID = mDeviceInfo.productUUID
        dbSensor.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID, this) + mDeviceInfo!!.meshAddress

        DBUtils.saveSensor(dbSensor, isReConfirm)//保存进服务器

        dbSensor = DBUtils.getSensorByID(dbSensor.id)!!

        DBUtils.recordingChange(dbSensor.id, DaoSessionInstance.getInstance().dbSensorDao.tablename, Constant.DB_ADD)
        if (!isReConfirm)
            showRenameDialog(dbSensor)
        else
            doFinish()
    }


    private fun getModeValue(): Int {
        LogUtils.d("FINAL_VALUE$modeStartUpMode-$modeDelayUnit-$modeSwitchMode")
        LogUtils.d("FINAL_VALUE" + (modeStartUpMode or modeDelayUnit or modeSwitchMode))
        return modeStartUpMode or modeDelayUnit or modeSwitchMode
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        TelinkLightApplication.getApp().removeEventListener(this)
        TelinkLightService.Instance()?.idleMode(true)
        disposableReset?.dispose()
        connectDispose?.dispose()
    }
}
