package com.dadoutek.uled.pir

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.GuideUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.light.DeviceInfo
import kotlinx.android.synthetic.main.activity_config_pir.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.design.snackbar

class ConfigSensorAct : TelinkBaseActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener{

    private lateinit var mDeviceInfo: DeviceInfo
    private lateinit var mGroups: List<DbGroup>
    private var builder:com.app.hubert.guide.core.Builder?=null
    private var mGroupsName: ArrayList<String>?=null
    private var mSelectGroupAddr: Int = 0xFF  //代表所有灯
    private var isSupportModeSelect = false
    private var isSupportDelayUnitSelect = false

    private var modeStartUpMode=0
    private var modeDelayUnit=0
    private var modeSwitchMode=0

    private val MODE_START_UP_MODE_OPEN=0
    private val MODE_DELAY_UNIT_SECONDS=0
    private val MODE_SWITCH_MODE_MOMENT=0

    private val MODE_START_UP_MODE_CLOSE=1
    private val MODE_DELAY_UNIT_MINUTE=2
    private val MODE_SWITCH_MODE_GRADIENT=4

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
        spDelayUnit.onItemSelectedListener=this
        spSwitchMode.onItemSelectedListener=this
    }

    private fun initOnLayoutListener() {
        val view = getWindow().getDecorView()
        val viewTreeObserver = view.getViewTreeObserver()
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                lazyLoad()
            }
        })
    }

    fun lazyLoad() {
        val guide1=tietDelay
        val guide2=tietMinimumBrightness
        val guide3= spTriggerLux
        val guide4= spSelectGroup

        builder= GuideUtils.guideBuilder(this@ConfigSensorAct,Constant.TAG_ConfigSensorAct)
        builder?.addGuidePage(GuideUtils.addGuidePage(guide1,R.layout.view_guide_simple_bottom,getString(R.string.config_pir_guide_1)))
        builder?.addGuidePage(GuideUtils.addGuidePage(guide2,R.layout.view_guide_simple_bottom,getString(R.string.config_pir_guide_2)))
        builder?.addGuidePage(GuideUtils.addGuidePage(guide4,R.layout.view_guide_simple_bottom,getString(R.string.config_pir_guide_4)))
    }

    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            dstAdress = mDeviceInfo.meshAddress
//            initOnLayoutListener()
            lazyLoad()
            Commander.getDeviceVersion(dstAdress,
                    successCallback = {
                        val versionNum = Integer.parseInt(StringUtils.versionResolution(it, 1))
                        versionLayoutPS.visibility = View.VISIBLE
                        tvPSVersion.text = it
                        isSupportModeSelect = (it ?: "").contains("PS")

                        if (isSupportModeSelect) {
                            tvSelectStartupMode.visibility = View.VISIBLE
                            spSelectStartupMode.visibility = View.VISIBLE

                            builder?.addGuidePage(GuideUtils.addGuidePage(spSelectStartupMode,R.layout.view_guide_simple_bottom,getString(R.string.config_pir_guide_5)))
                            if(versionNum>=113){
                                isSupportDelayUnitSelect=true
                                tvSwitchMode.visibility =View.VISIBLE
                                spSwitchMode.visibility=View.VISIBLE
                                tvDelayUnit.visibility=View.VISIBLE
                                spDelayUnit.visibility=View.VISIBLE
                                builder?.addGuidePage(GuideUtils.addGuidePage(spSwitchMode,R.layout.view_guide_simple_bottom,getString(R.string.config_pir_guide_6)))
                                builder?.addGuidePage(GuideUtils.addGuidePage(spDelayUnit,R.layout.view_guide_simple_bottom,getString(R.string.config_pir_guide_7)))
                            }
                        }
                        builder?.addGuidePage(GuideUtils.addGuidePage(fabConfirm,R.layout.view_guide_simple_jump_left_tobottom,getString(R.string.config_pir_guide_8)))
                        builder?.show()
                    },
                    failedCallback = {
                        builder?.addGuidePage(GuideUtils.addGuidePage(fabConfirm,R.layout.view_guide_simple_jump_left_tobottom,getString(R.string.config_pir_guide_8)))
                        builder?.show()
                        versionLayoutPS.visibility = View.GONE
                    })
        } else {
            dstAdress = 0
        }
    }

    private fun initToolbar() {
        toolbar.title = getString(R.string.install_sensor)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { finish() }

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
        LogUtils.d("delayTime = $delayTime  minBrightness = $minBrightness  " +
                "   triggerValue = $triggerValue")
//        val spGroup = groupConvertSpecialValue(groupAddr)
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
        TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_PIR,
                mDeviceInfo.meshAddress,
                paramBytes)

        Thread.sleep(300)
    }

    private fun groupConvertSpecialValue(groupAddr: Int): Int {
        var groupid = 0
        var groupByte = 0
        if (groupAddr == 0xFFFF) {
            groupByte = 0
        } else {
            groupid = groupAddr - 0x8001
            groupByte = (0x1 shl groupid)
        }

//        console.log("groupid" + groupid + " Byte" + groupByte + "group" + group + "GROUPIDSS" + getActualGroup()[group].id);
        return groupByte
    }


    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//        if (position == 0)
//            mSelectGroupAddr = 0xDF
//        else
        when(parent?.id){
            R.id.spSelectGroup ->{
                mSelectGroupAddr = mGroups[position].meshAddr
            }
            R.id.spSelectStartupMode ->{
                if(position == 0){//开灯
                    tietMinimumBrightness?.setText("0")
                    modeStartUpMode=MODE_START_UP_MODE_OPEN
                }else if(position == 1){//关灯
                    tietMinimumBrightness?.setText("99")
                    modeStartUpMode=MODE_START_UP_MODE_CLOSE
                }
            }
            R.id.spSwitchMode ->{
                 if(position==0){
                     modeSwitchMode=MODE_SWITCH_MODE_MOMENT
                 }else{
                     modeSwitchMode=MODE_SWITCH_MODE_GRADIENT
                 }
            }
            R.id.spDelayUnit ->{
                if(position==0){
                    tilDelay.hint = getString(R.string.delay_minute)
                    modeDelayUnit=MODE_DELAY_UNIT_MINUTE
                }else{
                    tilDelay.hint = getString(R.string.delay_seconds)
                    modeDelayUnit=MODE_DELAY_UNIT_SECONDS
                }
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        mSelectGroupAddr = 0xFF
    }

    private fun doFinish() {
        TelinkLightService.Instance().idleMode(true)
        TelinkLightService.Instance().disconnect()
        finish()
    }

    private fun configureComplete() {
        TelinkLightService.Instance().idleMode(true)
        TelinkLightService.Instance().disconnect()
        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
    }

    override fun onBackPressed() {
        doFinish()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fabConfirm -> {
                if (tietDelay.text.isEmpty() || tietMinimumBrightness.text.isEmpty()) {
                    snackbar(configPirRoot, getString(R.string.params_cannot_be_empty))
                } else if (tietMinimumBrightness.text.toString().toInt() > 99) {
                    ToastUtils.showLong(getString(R.string.max_tip_brightness))
                } else if (tietDelay.text.toString().toInt() > 30) {
                    ToastUtils.showLong(getString(R.string.time_max_tip))
                } else {
                    showLoadingDialog(getString(R.string.configuring_switch))
                    Thread {

                        val mApplication = this.application as TelinkLightApplication
                        val mesh = mApplication.getMesh()

                        val mode=getModeValue()

                        configPir(mSelectGroupAddr,
                                tietDelay.text.toString().toInt(),
                                tietMinimumBrightness.text.toString().toInt(),
                                spTriggerLux.selectedItem.toString().toInt(), mode)
                        Thread.sleep(300)
//
//                        addGroup(mDeviceInfo.meshAddress, mSelectGroupAddr,
//                                { LogUtils.d("success") },
//                                { LogUtils.d("failed") })
//                        Thread.sleep(300)

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
        LogUtils.d("FINAL_VALUE$modeStartUpMode-$modeDelayUnit-$modeSwitchMode")
        LogUtils.d("FINAL_VALUE"+(modeStartUpMode or modeDelayUnit or modeSwitchMode))
        return modeStartUpMode or modeDelayUnit or modeSwitchMode
    }
}
