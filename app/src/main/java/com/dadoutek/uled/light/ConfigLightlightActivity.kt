package com.dadoutek.uled.light

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Point
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.GroupListAdapter
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.ItemGroup
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.GuideUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.light.DeviceInfo
import kotlinx.android.synthetic.main.activity_config_light_light.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.design.snackbar

class ConfigLightlightActivity :TelinkBaseActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener{
    private lateinit var mDeviceInfo: DeviceInfo
    private lateinit var mGroups: List<DbGroup>
    private var mGroupsName: ArrayList<String>?=null
    private var mSelectGroupAddr: Int = 0xFF  //代表所有灯
    private val CMD_OPEN_LIGHT=0X01
    private val CMD_CLOSE_LIGHT=0X00
    private val CMD_CONTROL_GROUP=0X02
    private var switchMode=0X00
    lateinit var secondsList:Array<String>
    lateinit var minuteList:Array<String>
    private var selectTime=0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_light_light)
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
        spDelayUnit.onItemSelectedListener=this
        spDelay.onItemSelectedListener=this
        spSwitchMode.onItemSelectedListener=this
        secondsList=resources.getStringArray(R.array.light_light_seconds_list)
        minuteList=resources.getStringArray(R.array.light_light_minute_list)
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, minuteList)
        spDelay.adapter = adapter
    }

    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            dstAdress = mDeviceInfo.meshAddress
            Commander.getDeviceVersion(dstAdress,
                    successCallback = {
                        versionLayoutPS.visibility = View.VISIBLE
                        tvPSVersion.text = it
                    },
                    failedCallback = {
                        versionLayoutPS.visibility = View.GONE
                    })
        } else {
            dstAdress = 0
        }
    }

    private fun initToolbar() {
        toolbar.title = getString(R.string.sensor_title)
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


    private fun configLightlight() {
//        val spGroup = groupConvertSpecialValue(groupAddr)
        val groupH: Byte = (mSelectGroupAddr shr 8 and 0xff).toByte()
        val groupL: Byte = (mSelectGroupAddr and 0xff).toByte()
        val timeH: Byte = (selectTime shr 8 and 0xff).toByte()
        val timeL: Byte = (selectTime and 0xff).toByte()
        val paramBytes = byteArrayOf(
                DeviceType.LIGHT_LIGHT.toByte(),
                switchMode.toByte(),timeL,timeH
        )
        val paramBytesGroup = byteArrayOf(
                DeviceType.LIGHT_LIGHT.toByte(),CMD_CONTROL_GROUP.toByte(),groupL
        )

        LogUtils.d("groupL="+groupL+"")

        TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT,
                mDeviceInfo.meshAddress,
                paramBytes)

        Thread.sleep(300)

        if(groupL!=(0xff).toByte()){
            TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT,
                    mDeviceInfo.meshAddress,
                    paramBytesGroup)
        }

        Thread.sleep(300)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when(parent?.id){
            R.id.spSelectGroup ->{
                mSelectGroupAddr = mGroups[position].meshAddr
            }
            R.id.spDelayUnit ->{
                if(position==0){
                    val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, minuteList)
                    spDelay.adapter = adapter
                }else{
                    val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, secondsList)
                    spDelay.adapter = adapter
                }
            }
            R.id.spDelay ->{
                val time=(spDelay.selectedItem as String).toInt()
                if(position==0){
                    selectTime=time*60
                }else{
                    selectTime=time
                }
            }
            R.id.spSwitchMode->{
                if(position==0){
                    switchMode=CMD_OPEN_LIGHT
                }else{
                    switchMode=CMD_CLOSE_LIGHT
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
                    showLoadingDialog(getString(R.string.configuring_switch))
                    Thread {

                        val mApplication = this.application as TelinkLightApplication
                        val mesh = mApplication.getMesh()

                        configLightlight()
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