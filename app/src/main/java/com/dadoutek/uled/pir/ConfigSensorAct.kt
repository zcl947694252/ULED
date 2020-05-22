package com.dadoutek.uled.pir

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.DbModel.DbSensor
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.MeshAddressGenerator
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
import kotlinx.android.synthetic.main.activity_config_pir.*
import kotlinx.android.synthetic.main.template_loading_progress.*
import kotlinx.android.synthetic.main.template_radiogroup.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.design.snackbar

/**
 * 老版本人体感应器设置详情
 */
class ConfigSensorAct : TelinkBaseActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener, EventListener<String> {
    private lateinit var mScenes: List<DbScene>
    private var version: String=""
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
        telinkApplication = this.application as TelinkApplication
        initToolbar()
        initData()
        initView()
        initListener()
        if (TelinkLightApplication.getApp().connectDevice == null)
            autoConnectSensor()
        val version = intent.getStringExtra("version")
        getVersion(version)
    }

    private fun initListener() {
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
        showLoadingDialog(getString(R.string.connecting))
    }

    private fun getVersion(version: String) {
        val versionNum = Integer.parseInt(StringUtils.versionResolution(version, 1))
        versionLayoutPS.visibility = View.VISIBLE
        tvPSVersion.text = version
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
        toolbar.title = getString(R.string.sensor_title)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { doFinish() }
    }

    private fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        version = intent.getStringExtra("version")
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

    private fun configureComplete() {
        doFinish()
    }

    override fun onBackPressed() {
        doFinish()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fabConfirm -> {
                if (tietDelay.text?.isEmpty() != false || tietMinimumBrightness.text?.isEmpty() !=
                        false) {
                    snackbar(configPirRoot, getString(R.string.params_cannot_be_empty))
                } else if (tietMinimumBrightness.text.toString().toInt() > 99) {
                    ToastUtils.showLong(getString(R.string.max_tip_brightness))
                } else if (tietDelay.text.toString().toInt() > 255) {
                    ToastUtils.showLong(getString(R.string.time_max_tip))
                } else {
                    showLoadingDialog(getString(R.string.configuring_sensor))
                    Thread {
                        val mode = getModeValue()

                        configPir(mSelectGroupSceneAddr,
                                tietDelay.text.toString().toInt(),
                                tietMinimumBrightness.text.toString().toInt(),
                                spTriggerLux.selectedItem.toString().toInt(), mode)
                        Thread.sleep(300)

                       mDeviceInfo.meshAddress = MeshAddressGenerator().meshAddress
                        Commander.updateMeshName(newMeshAddr =  mDeviceInfo.meshAddress,
                                successCallback = {
                                    hideLoadingDialog()
                                    saveSensor()
                                    configureComplete()

                                },
                                failedCallback = {
                                    snackbar(configPirRoot, getString(R.string.pace_fail))
                                    hideLoadingDialog()
                                    TelinkLightService.Instance()?.idleMode(true)
                                    TelinkLightService.Instance()?.disconnect()
                                })
                    }.start()

                }
            }
        }
    }

    private fun saveSensor() {
        var dbSensor = DbSensor()

        var isConfirm = mDeviceInfo.isConfirm == 1
        if (isConfirm) {
            dbSensor.index = mDeviceInfo.id.toInt()
            if ("none" != mDeviceInfo.id)
                dbSensor.id = mDeviceInfo.id.toLong()
        } else {//如果不是重新配置就保存进服务器
            DBUtils.saveSensor(dbSensor, isConfirm)
            dbSensor.index = dbSensor.id.toInt()//拿到新的服务器id
        }


        dbSensor.controlGroupAddr = mSelectGroupSceneAddr.toString()
        dbSensor.macAddr = mDeviceInfo.macAddress
        dbSensor.version = version
        dbSensor.meshAddr = mDeviceInfo.meshAddress
        // dbSensor.meshAddr = Constant.SWITCH_PIR_ADDRESS
        dbSensor.productUUID = mDeviceInfo.productUUID
        dbSensor.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID, this) + mDeviceInfo!!.meshAddress

        DBUtils.saveSensor(dbSensor, isConfirm)//保存进服务器

        dbSensor = DBUtils.getSensorByID(dbSensor.id)!!

        DBUtils.recordingChange(dbSensor.id,
                DaoSessionInstance.getInstance().dbSensorDao.tablename,
                Constant.DB_ADD)
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
    }
}
