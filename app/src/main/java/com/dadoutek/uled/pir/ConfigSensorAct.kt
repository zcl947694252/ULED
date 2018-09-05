package com.dadoutek.uled.pir

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.communicate.Commander.addGroup
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.telink.bluetooth.light.DeviceInfo
import kotlinx.android.synthetic.main.activity_config_pir.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.design.snackbar

class ConfigSensorAct : TelinkBaseActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener {

    private lateinit var mDeviceInfo: DeviceInfo
    private lateinit var mGroups: List<DbGroup>
    private var mSelectGroupAddr: Int = 0xFF  //代表所有灯


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_pir)
        fabConfirm.setOnClickListener(this)
        initToolbar()
        initData()
    }

    private fun initToolbar() {
        toolbar.title = getString(R.string.install_sensor)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { finish() }

    }

    private fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        mGroups = DBUtils.allGroups
        val mGroupsName: ArrayList<String> = ArrayList()
        for (item in mGroups) {
            mGroupsName.add(item.name)
        }
        val groupsAdapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mGroupsName)
        spSelectGroup.adapter = groupsAdapter
        spSelectGroup.onItemSelectedListener = this
    }


    private fun configPir(groupAddr: Int, delayTime: Int, minBrightness: Int, triggerValue: Int) {
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
                triggerValue.toByte()
        )
        TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_PIR,
                mDeviceInfo.meshAddress,
                paramBytes)

        Thread.sleep(300)
    }

    private fun groupConvertSpecialValue(groupAddr: Int): Int {
        var groupid = 0;
        var groupByte = 0;
        if (groupAddr == 0xFFFF) {
            groupByte = 0;
        } else {
            groupid = groupAddr - 0x8001;
            groupByte = (0x1 shl groupid)
        }

//        console.log("groupid" + groupid + " Byte" + groupByte + "group" + group + "GROUPIDSS" + getActualGroup()[group].id);
        return groupByte;
    }


    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//        if (position == 0)
//            mSelectGroupAddr = 0xDF
//        else
        mSelectGroupAddr = mGroups[position].meshAddr
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
                }else if(tietMinimumBrightness.text.toString().toInt()>99){
                    ToastUtils.showLong(getString(R.string.max_tip_brightness))
                } else if(tietDelay.text.toString().toInt()>30){
                    ToastUtils.showLong(getString(R.string.time_max_tip))
                }else {
                    showLoadingDialog(getString(R.string.configuring_switch))
                    Thread {

                        val mApplication = this.application as TelinkLightApplication
                        val mesh = mApplication.getMesh()

                        configPir(mSelectGroupAddr,
                                tietDelay.text.toString().toInt(),
                                tietMinimumBrightness.text.toString().toInt(),
                                spTriggerLux.selectedItem.toString().toInt())
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

}