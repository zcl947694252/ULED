package com.dadoutek.uled.light

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbSensor
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.StringUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.light.DeviceInfo
import kotlinx.android.synthetic.main.activity_config_light_light.*
import kotlinx.android.synthetic.main.activity_config_light_light.configPirRoot
import kotlinx.android.synthetic.main.activity_config_light_light.fabConfirm
import kotlinx.android.synthetic.main.activity_config_light_light.spSwitchMode
import kotlinx.android.synthetic.main.activity_config_light_light.tietMinimumBrightness
import kotlinx.android.synthetic.main.activity_config_light_light.tvPSVersion
import kotlinx.android.synthetic.main.activity_config_light_light.versionLayoutPS
import kotlinx.android.synthetic.main.activity_config_pir.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.snackbar
import java.util.regex.Pattern

class ConfigNightlightActivity : TelinkBaseActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener {
    private lateinit var mDeviceInfo: DeviceInfo
    private var nightLightGroupRecycleViewAdapter: NightLightGroupRecycleViewAdapter? = null
    private var nightLightEditGroupAdapter: NightLightEditGroupAdapter? = null
    private var mSelectGroupAddr: Int = 0xFF  //代表所有灯
    private val CMD_OPEN_LIGHT = 0X01
    private val CMD_CLOSE_LIGHT = 0X00
    private val CMD_CONTROL_GROUP = 0X02
    private var switchMode = 0X00
    lateinit var secondsList: Array<String>
    private var selectTime = 0
    private var currentPageIsEdit = false

    private var showGroupList: MutableList<ItemGroup>? = null
    private var showCheckListData: MutableList<DbGroup>? = null

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
        setContentView(R.layout.activity_config_light_light)
        fabConfirm.setOnClickListener(this)
        initToolbar()
        initData()
        initView()
        getVersion()
    }

    private fun initView() {
        showDataListView()
        spDelay.onItemSelectedListener = this
        spSwitchMode.onItemSelectedListener = this
        sp_SwitchMode.onItemSelectedListener=this
        secondsList = resources.getStringArray(R.array.light_light_time_list)
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, secondsList)
        spDelay.adapter = adapter
        btnSelectGroup.setOnClickListener(this)
    }

    //显示数据列表页面
    private fun showDataListView() {
        currentPageIsEdit = false
        data_view_layout.visibility = View.VISIBLE
        edit_data_view_layout.visibility = View.GONE

        val layoutmanager = GridLayoutManager(this, 2)
        recyclerViewNightLightGroups.layoutManager = layoutmanager as RecyclerView.LayoutManager?
        this.nightLightGroupRecycleViewAdapter = NightLightGroupRecycleViewAdapter(
                R.layout.activity_night_light_groups_item, showGroupList)
//        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
//        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
//        //添加分割线
//        recyclerViewNightLightGroups.addItemDecoration(decoration)
        nightLightGroupRecycleViewAdapter?.bindToRecyclerView(recyclerViewNightLightGroups)
        nightLightGroupRecycleViewAdapter?.onItemChildClickListener = onItemChildClickListener
    }

    internal var onItemChildClickListener: BaseQuickAdapter.OnItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        when (view.id) {
            R.id.imgDelete -> delete(adapter, position)
        }
    }

    private fun delete(adapter: BaseQuickAdapter<Any, BaseViewHolder>, position: Int) {
        adapter.remove(position)
    }

    //显示配置数据页面
    private fun showEditListVew() {
        currentPageIsEdit = true
        data_view_layout.visibility = View.GONE
        edit_data_view_layout.visibility = View.VISIBLE

        showCheckListData = DBUtils.allGroups
        if (showGroupList!!.size != 0) {
            for (i in showCheckListData!!.indices) {
                for (j in showGroupList!!.indices) {
                    if (showCheckListData!![i].meshAddr == showGroupList!![j].groupAress) {
                        showCheckListData!![i].checked = true
                        break
                    } else if (j == showGroupList!!.size - 1 && showCheckListData!![i].meshAddr != showGroupList!![j].groupAress) {
                        showCheckListData!![i].checked = false
                    }
                }
            }
            changeCheckedViewData()
        } else {
            for (i in showCheckListData!!.indices) {
                showCheckListData!![i].enableCheck = true
                showCheckListData!![i].checked = false
            }
        }

        val layoutmanager = LinearLayoutManager(this)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        recyclerView_select_group_list_view.layoutManager = layoutmanager
        this.nightLightEditGroupAdapter = NightLightEditGroupAdapter(
                R.layout.activity_night_light_edit_group_item, showCheckListData!!)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
        //添加分割线
        recyclerView_select_group_list_view.addItemDecoration(decoration)
        nightLightEditGroupAdapter?.bindToRecyclerView(recyclerView_select_group_list_view)
        nightLightEditGroupAdapter?.onItemClickListener = onItemClickListenerCheck
    }

    internal var onItemClickListenerCheck: BaseQuickAdapter.OnItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
                val item = showCheckListData!!.get(position)
                if (item.enableCheck) {
                    showCheckListData!!.get(position).checked = !item.checked
                    changeCheckedViewData()
                    adapter?.notifyDataSetChanged()
                }
    }

    private fun changeCheckedViewData() {
        var isAllCanCheck = true
        for (i in showCheckListData!!.indices) {
            if (showCheckListData!![0].meshAddr == 0xffff && showCheckListData!![0].checked) {
                showCheckListData!![0].enableCheck = true
                if (showCheckListData!!.size > 1 && i > 0) {
                    showCheckListData!![i].enableCheck = false
                }
            } else {
                showCheckListData!![0].enableCheck = false
                if (showCheckListData!!.size > 1 && i > 0) {
                    showCheckListData!![i].enableCheck = true
                }

                if (i > 0 && showCheckListData!![i].checked) {
                    isAllCanCheck = false
                }
            }
        }
        if (isAllCanCheck) {
            showCheckListData!![0].enableCheck = true
        }
    }

    private fun saveCurrenEditResult() {
        val oldResultItemList = ArrayList<ItemGroup>()
        val newResultItemList = ArrayList<ItemGroup>()

        for (i in showCheckListData!!.indices) {
            if (showCheckListData!![i].checked) {
                if (showGroupList!!.size == 0) {
                    val newItemGroup = ItemGroup()
                    newItemGroup.brightness = 50
                    newItemGroup.temperature = 50
                    newItemGroup.color = R.color.white
                    newItemGroup.checked = true
                    newItemGroup.enableCheck = true
                    newItemGroup.gpName = showCheckListData!![i].name
                    newItemGroup.groupAress = showCheckListData!![i].meshAddr
                    newResultItemList.add(newItemGroup)
                } else {
                    for (j in showGroupList!!.indices) {
                        if (showCheckListData!![i].meshAddr == showGroupList!![j].groupAress) {
                            oldResultItemList.add(showGroupList!![j])
                            break
                        } else if (j == showGroupList!!.size - 1) {
                            val newItemGroup = ItemGroup()
                            newItemGroup.brightness = 50
                            newItemGroup.temperature = 50
                            newItemGroup.color = R.color.white
                            newItemGroup.checked = true
                            newItemGroup.enableCheck = true
                            newItemGroup.gpName = showCheckListData!![i].name
                            newItemGroup.groupAress = showCheckListData!![i].meshAddr
                            newResultItemList.add(newItemGroup)
                        }
                    }
                }
            }
        }
        showGroupList?.clear()
        showGroupList?.addAll(oldResultItemList)
        showGroupList?.addAll(newResultItemList)

        if (showGroupList!!.size > 8) {
            ToastUtils.showLong(getString(R.string.tip_night_light_group_limite_tip))
        } else {
            showDataListView()
        }
    }

    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            dstAdress = mDeviceInfo.meshAddress
            Commander.getDeviceVersion(dstAdress,
                    successCallback = {
                        versionLayoutPS.visibility = View.VISIBLE
                        tvPSVersion.text = it
                        var version=tvPSVersion.text.toString()
                        var num=version.substring(2,3)
                        if(num.toDouble()>=3.0){
                            tv_TriggerLux.visibility = View.VISIBLE
                            spTrigger_lux.visibility = View.VISIBLE
                            tietMinimumBrightness.visibility = View.VISIBLE
                            tilMinimum_Brightness.visibility = View.VISIBLE
                            sp_SwitchMode.visibility = View.VISIBLE
                            tv_SwitchMode.visibility = View.VISIBLE
                        }
                    },
                    failedCallback = {
                        versionLayoutPS.visibility = View.GONE
                    })
        } else {
            dstAdress = 0
        }
    }

    private fun initToolbar() {
        toolbar.title = getString(R.string.night_light_title)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { doFinish() }

    }

    private fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        showCheckListData = DBUtils.allGroups
        for (i in showCheckListData!!.indices) {
            showCheckListData!![i].checked = false
        }
        showGroupList = ArrayList<ItemGroup>()
    }


    private fun configLightlight() {
//        val spGroup = groupConvertSpecialValue(groupAddr)
        val groupH: Byte = (mSelectGroupAddr shr 8 and 0xff).toByte()

        val timeH: Byte = (selectTime shr 8 and 0xff).toByte()
        val timeL: Byte = (selectTime and 0xff).toByte()
        val paramBytes = byteArrayOf(
                DeviceType.NIGHT_LIGHT.toByte(),
                switchMode.toByte(), timeL, timeH
        )
        val paramBytesGroup: ByteArray
        paramBytesGroup = byteArrayOf(
                DeviceType.NIGHT_LIGHT.toByte(), CMD_CONTROL_GROUP.toByte(), 0, 0, 0, 0, 0, 0, 0, 0
        )

        var canSendGroup = true
        for (i in showGroupList!!.indices) {
            if (showGroupList!![i].groupAress == 0xffff) {
//                canSendGroup=false
                paramBytesGroup[i + 2] = 0xFF.toByte()
                break
            } else {
                val groupL: Byte = (showGroupList!![i].groupAress and 0xff).toByte()
                paramBytesGroup[i + 2] = groupL
                LogUtils.d("groupL=" + groupL + "" + "-----" + showGroupList!![i].groupAress)
            }
        }

        TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT,
                mDeviceInfo.meshAddress,
                paramBytes)

        Thread.sleep(300)

        if (canSendGroup) {
            TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT,
                    mDeviceInfo.meshAddress,
                    paramBytesGroup)
        }

        Thread.sleep(300)
    }

    private fun configNewlight() {
//        val spGroup = groupConvertSpecialValue(groupAddr)
        val groupH: Byte = (mSelectGroupAddr shr 8 and 0xff).toByte()
        val timeH: Byte = (selectTime shr 8 and 0xff).toByte()
        val timeL: Byte = (selectTime and 0xff).toByte()
        var mode= getModeValue()
        val paramBytes = byteArrayOf(
                DeviceType.NIGHT_LIGHT.toByte(),
                switchMode.toByte(), 0x00,0x00,
                selectTime.toByte(),
                tietMinimumBrightness.text.toString().toInt().toByte(),
                spTrigger_lux.selectedItem.toString().toInt().toByte(),
                mode.toByte()
        )
        val paramBytesGroup: ByteArray
        paramBytesGroup = byteArrayOf(
                DeviceType.NIGHT_LIGHT.toByte(), CMD_CONTROL_GROUP.toByte(), 0, 0, 0, 0, 0, 0, 0, 0
        )

        var canSendGroup = true
        for (i in showGroupList!!.indices) {
            if (showGroupList!![i].groupAress == 0xffff) {
//                canSendGroup=false
                paramBytesGroup[i + 2] = 0xFF.toByte()
                break
            } else {
                val groupL: Byte = (showGroupList!![i].groupAress and 0xff).toByte()
                paramBytesGroup[i + 2] = groupL
                LogUtils.d("groupL=" + groupL + "" + "-----" + showGroupList!![i].groupAress)
            }
        }

        if (canSendGroup) {
            TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT,
                    mDeviceInfo.meshAddress,
                    paramBytesGroup)
        }

        Thread.sleep(300)

        TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT,
                mDeviceInfo.meshAddress,
                paramBytes)

        Thread.sleep(300)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent?.id) {
            R.id.spDelay -> {
                val time = getTime(spDelay.selectedItem as String)
                if (position > 4) {
                    selectTime = time
                    modeDelayUnit =MODE_DELAY_UNIT_MINUTE
                } else {
                    selectTime = time
                    modeDelayUnit =MODE_DELAY_UNIT_SECONDS
                }
            }
            R.id.spSwitchMode -> {
//                if (position == 0) {
//                    switchMode = CMD_OPEN_LIGHT
//                } else {
//                    switchMode = CMD_CLOSE_LIGHT
//                }
                var version = tvPSVersion.text.toString()
                var num = version.substring(2, 3)
                if (num.toDouble() >= 3.0) {
                    if (position == 0) {//开灯
                        tietMinimumBrightness?.setText("0")
                        modeStartUpMode = MODE_START_UP_MODE_OPEN
                    } else if (position == 1) {//关灯
                        tietMinimumBrightness?.setText("99")
                        modeStartUpMode = MODE_START_UP_MODE_CLOSE
                    }
                } else {
                    if (position == 0) {
                        switchMode = CMD_OPEN_LIGHT
                    } else {
                        switchMode = CMD_CLOSE_LIGHT
                    }
                }
            }
            R.id.sp_SwitchMode -> {
            if (position == 0) {
                modeSwitchMode = MODE_SWITCH_MODE_MOMENT
            } else {
                modeSwitchMode = MODE_SWITCH_MODE_GRADIENT
            }
            }
        }
    }

    private fun getTime(timeStr: String): Int {
        val str = timeStr
        val p = Pattern.compile("\\d+")
        val m = p.matcher(str)
        m.find()
        return m.group().toInt()
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
        saveSensor()
        TelinkLightService.Instance().idleMode(true)
        TelinkLightService.Instance().disconnect()
        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
    }

    private fun saveSensor() {
        var dbSensor : DbSensor = DbSensor()
        DBUtils.saveSensor(dbSensor,false)
        dbSensor.controlGroupAddr = getControlGroup()
        dbSensor.index = dbSensor.id.toInt()
        dbSensor.macAddr = mDeviceInfo.macAddress
        dbSensor.meshAddr = Constant.SWITCH_PIR_ADDRESS
        dbSensor.productUUID = mDeviceInfo.productUUID
        dbSensor.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID)

        DBUtils.saveSensor(dbSensor,false)

        dbSensor= DBUtils.getSensorByID(dbSensor.id)!!

        DBUtils.recordingChange(dbSensor.id,
                DaoSessionInstance.getInstance().dbSensorDao.tablename,
                Constant.DB_ADD)
    }

    private fun getControlGroup(): String? {
        var controlGroupListStr = ""
        for(j in showGroupList!!.indices){
            if(j==0){
                controlGroupListStr= showGroupList!![j].groupAress.toString()
            }else{
                controlGroupListStr+= ","+showGroupList!![j].groupAress.toString()
            }
        }
        return controlGroupListStr
    }

    override fun onBackPressed() {
        doFinish()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fabConfirm -> {
                if (currentPageIsEdit) {
                    stopEditMode()
                } else {
                    configDevice()
                }
            }
            R.id.btnSelectGroup -> {
                showEditListVew()
            }
        }
    }

    private fun stopEditMode() {
        saveCurrenEditResult()
    }

    private fun configDevice() {
        var version=tvPSVersion.text.toString()
            var num=version.substring(2,3)
            if(num.toDouble()>=3.0){
            if (tietMinimumBrightness.text?.isEmpty() != false) {
                snackbar(configPirRoot, getString(R.string.params_cannot_be_empty))
            } else if (tietMinimumBrightness.text.toString().toInt() > 99) {
                ToastUtils.showLong(getString(R.string.max_tip_brightness))
            } else {
                Thread {
                    val mApplication = this.application as TelinkLightApplication
                    val mesh = mApplication.getMesh()

                    if (showGroupList?.size != 0) {
                        GlobalScope.launch(Dispatchers.Main) {
                            showLoadingDialog(getString(R.string.configuring_switch))
                        }

                        configNewlight()
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
                    } else {
                        ToastUtils.showLong(getString(R.string.config_night_light_select_group))
                    }

                }.start()

            }
        }else{
            Thread {

                val mApplication = this.application as TelinkLightApplication
                val mesh = mApplication.getMesh()

                if (showGroupList?.size != 0) {
                    GlobalScope.launch(Dispatchers.Main) {
                        showLoadingDialog(getString(R.string.configuring_switch))
                    }

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
                } else {
                    ToastUtils.showLong(getString(R.string.config_night_light_select_group))
                }

            }.start()
        }
    }

    private fun getModeValue(): Int {
        LogUtils.d("FINAL_VALUE$modeStartUpMode-$modeDelayUnit-$modeSwitchMode")
        LogUtils.d("FINAL_VALUE" + (modeStartUpMode or modeDelayUnit or modeSwitchMode))
        return modeStartUpMode or modeDelayUnit or modeSwitchMode
    }
}