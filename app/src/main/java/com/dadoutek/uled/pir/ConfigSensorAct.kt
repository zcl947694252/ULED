package com.dadoutek.uled.pir

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DBUtils.recordingChange
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbSensor
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.StringUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.light.DeviceInfo
import kotlinx.android.synthetic.main.activity_config_pir.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.design.snackbar

class ConfigSensorAct : TelinkBaseActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener {

    private lateinit var mDeviceInfo: DeviceInfo
    private lateinit var mGroups: List<DbGroup>
    private var mGroupsName: ArrayList<String>? = null
    private var mSelectGroupAddr: Int = 0xFF  //代表所有灯
    private var isSupportModeSelect = false
    private var isSupportDelayUnitSelect = false
    private var getVersionRetryMaxCount = 2
    private var getVersionRetryCount = 0

    private var modeStartUpMode = 0
    private var modeDelayUnit = 0
    private var modeSwitchMode = 0

    private val MODE_START_UP_MODE_OPEN = 0
    private val MODE_DELAY_UNIT_SECONDS = 0
    private val MODE_SWITCH_MODE_MOMENT = 0

    private val MODE_START_UP_MODE_CLOSE = 1
    private val MODE_DELAY_UNIT_MINUTE = 2
    private val MODE_SWITCH_MODE_GRADIENT = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_pir)
        fabConfirm.setOnClickListener(this)
        initToolbar()
        initData()
        initView()
        getVersion()
    }

    private fun initView() {
        val groupsAdapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mGroupsName)
        spSelectGroup.adapter = groupsAdapter
        spSelectGroup.onItemSelectedListener = this
        spSelectStartupMode.onItemSelectedListener = this
        spDelayUnit.onItemSelectedListener = this
        spSwitchMode.onItemSelectedListener = this
    }


    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            dstAdress = mDeviceInfo.meshAddress
            Commander.getDeviceVersion(dstAdress,
                    successCallback = {
                        val versionNum = Integer.parseInt(StringUtils.versionResolution(it, 1))
                        versionLayoutPS.visibility = View.VISIBLE
                        tvPSVersion.text = it
                        isSupportModeSelect = (it ?: "").contains("PS")

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
                    },
                    failedCallback = {
                        versionLayoutPS.visibility = View.GONE
                        getVersionRetryCount++
                        if (getVersionRetryCount <= getVersionRetryMaxCount) {
                            getVersion()
                        }
                    })
        } else {
            ToastUtils.showLong(R.string.device_not_connected)
        }
    }

    private fun initToolbar() {
        toolbar.title = getString(R.string.sensor_title)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { doFinish() }

    }

    private fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        mGroups = DBUtils.allGroups
        mGroupsName = ArrayList()
        for (item in mGroups) {
            mGroupsName!!.add(item.name)
        }
    }


    private fun configPir(groupAddr: Int, delayTime: Int, minBrightness: Int, triggerValue: Int, mode: Int) {
        val groupH: Byte = (groupAddr shr 8 and 0xff).toByte()
        val groupL: Byte = (groupAddr and 0xff).toByte()
        val paramBytes = byteArrayOf(
                0x01,
                groupH, groupL,
                delayTime.toByte(),
                minBrightness.toByte(),
                triggerValue.toByte(),
                mode.toByte()
        )
        TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_PIR,
                mDeviceInfo.meshAddress,
                paramBytes)

        Thread.sleep(300)
    }



    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent?.id) {
            R.id.spSelectGroup -> {
                mSelectGroupAddr = mGroups[position].meshAddr
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
                if (position == 0) {
                    modeSwitchMode = MODE_SWITCH_MODE_MOMENT
                } else {
                    modeSwitchMode = MODE_SWITCH_MODE_GRADIENT
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
        mSelectGroupAddr = 0xFF
    }

    private fun doFinish() {
       TelinkLightService.Instance()?.idleMode(true)
       TelinkLightService.Instance()?.disconnect()
        finish()
    }

    private fun configureComplete() {
        saveSensor()
       TelinkLightService.Instance()?.idleMode(true)
       TelinkLightService.Instance()?.disconnect()
        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
    }

    private fun saveSensor() {
        var dbSensor : DbSensor? = DbSensor()
        DBUtils.saveSensor(dbSensor,false)
        dbSensor!!.controlGroupAddr = getControlGroup()
        dbSensor.index = dbSensor.id.toInt()
        dbSensor.macAddr = mDeviceInfo.macAddress
        dbSensor.meshAddr = Constant.SWITCH_PIR_ADDRESS
        dbSensor.productUUID = mDeviceInfo.productUUID
        dbSensor.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID)
        DBUtils.saveSensor(dbSensor,false)

        dbSensor=DBUtils.getSensorByMacAddr(mDeviceInfo.macAddress)
                    recordingChange(dbSensor!!.id,
                    DaoSessionInstance.getInstance().dbSensorDao.tablename,
                    Constant.DB_ADD)
    }

    private fun getControlGroup(): String? {
        return mSelectGroupAddr.toString()
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
                    showLoadingDialog(getString(R.string.configuring_switch))
                    Thread {
                        val mode = getModeValue()

                        configPir(mSelectGroupAddr,
                                tietDelay.text.toString().toInt(),
                                tietMinimumBrightness.text.toString().toInt(),
                                spTriggerLux.selectedItem.toString().toInt(), mode)
                        Thread.sleep(300)

                        Commander.updateMeshName(
                                successCallback = {
                                    hideLoadingDialog()
                                    configureComplete()
                                },
                                failedCallback = {
                                    snackbar(configPirRoot, getString(R.string.pace_fail))
                                    hideLoadingDialog()
                                })
                    }.start()

                }
            }
        }
    }

    private fun getModeValue(): Int {
        return modeStartUpMode or modeDelayUnit or modeSwitchMode
    }
}
