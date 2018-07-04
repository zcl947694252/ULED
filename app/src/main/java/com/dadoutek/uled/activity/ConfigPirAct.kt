package com.dadoutek.uled.activity

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.TelinkBaseActivity
import com.dadoutek.uled.TelinkLightService
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.communicate.Commander.addGroup
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.Opcode
import com.telink.bluetooth.light.DeviceInfo
import kotlinx.android.synthetic.main.activity_config_pir.*
import org.jetbrains.anko.design.snackbar

class ConfigPirAct : TelinkBaseActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener {

    private lateinit var mDeviceInfo: DeviceInfo
    private lateinit var mGroups: List<DbGroup>
    private var mSelectGroupAddr: Int = 0xFF  //代表所有灯


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_pir)
        fabConfirm.setOnClickListener(this)
        initData()
    }

    private fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        mGroups = DBUtils.getAllGroups()
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


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fabConfirm -> {
                if (tietDelay.text.isEmpty() || tietMinimumBrightness.text.isEmpty()) {
                    snackbar(configPirRoot, getString(R.string.params_cannot_be_empty))
                } else {
                    Thread {
                        configPir(mSelectGroupAddr,
                                tietDelay.text.toString().toInt(),
                                tietMinimumBrightness.text.toString().toInt(),
                                spTriggerLux.selectedItem.toString().toInt())

                        addGroup(mDeviceInfo.meshAddress, mSelectGroupAddr,
                                { LogUtils.d("success") },
                                { LogUtils.d("failed") })


                        Commander.updateMeshName(mDeviceInfo,
                                { ActivityUtils.finishToActivity(MainActivity::class.java, false, true) },
                                { snackbar(configPirRoot, getString(R.string.pace_fail)) })
                    }.start()

                }
            }
        }
    }

}
