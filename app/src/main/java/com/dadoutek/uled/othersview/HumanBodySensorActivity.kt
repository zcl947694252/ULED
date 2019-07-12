package com.dadoutek.uled.othersview

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.PopupWindow
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.light.NightLightEditGroupAdapter
import com.dadoutek.uled.light.NightLightGroupRecycleViewAdapter
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbSensor
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.ToastUtil
import com.telink.TelinkApplication
import com.telink.bluetooth.light.DeviceInfo
import kotlinx.android.synthetic.main.activity_config_light_light.*
import kotlinx.android.synthetic.main.huuman_body_sensor.*
import kotlinx.android.synthetic.main.huuman_body_sensor.edit_data_view_layout
import kotlinx.android.synthetic.main.huuman_body_sensor.recyclerView_select_group_list_view
import kotlinx.android.synthetic.main.huuman_body_sensor.tvPSVersion
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.snackbar
import java.util.*
import kotlin.collections.ArrayList

class HumanBodySensorActivity : TelinkBaseActivity(), View.OnClickListener {

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
    private var modeTriggerCondition = 0

    private val MODE_START_UP_MODE_OPEN = 0
    private val MODE_DELAY_UNIT_SECONDS = 0
    private val MODE_SWITCH_MODE_MOMENT = 0

    private val MODE_START_UP_MODE_CLOSE = 1
    private val MODE_DELAY_UNIT_MINUTE = 2
    private val MODE_SWITCH_MODE_GRADIENT = 4

    private val MODE_START_UP_MODE_CUSTOMIZE = 3

    private var isFinish: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.huuman_body_sensor)
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        initToolbar()
        initData()
        initView()
        getVersion()
    }

    private fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        showCheckListData = DBUtils.allGroups

        var lightGroup = DBUtils.allGroups

        showCheckListData!!.clear()

        for (i in lightGroup.indices) {
            if (lightGroup[i].deviceType != Constant.DEVICE_TYPE_CURTAIN) {
                showCheckListData!!.add(lightGroup[i])
            }
        }

        for (i in showCheckListData!!.indices) {
            showCheckListData!![i].checked = false
        }
        showGroupList = ArrayList<ItemGroup>()
    }

    private fun initToolbar() {
        toolbar.title = getString(R.string.human_body)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            if (isFinish) {
                sensor_three.visibility = View.VISIBLE
                edit_data_view_layout.visibility = View.GONE
                toolbar.title = getString(R.string.human_body)
                tv_function1.visibility = View.GONE
                isFinish = false
            } else {
                doFinish()
            }
        }

    }

    private fun doFinish() {
        TelinkLightService.Instance().idleMode(true)
        TelinkLightService.Instance().disconnect()
        finish()
    }

    private fun initView() {
        tv_function1.text = getString(R.string.btn_ok)
        tv_function1.setOnClickListener(this)
        triggering_conditions.setOnClickListener(this)
        trigger_time.setOnClickListener(this)
        time_type.setOnClickListener(this)
        brightness_change.setOnClickListener(this)
        choose_group.setOnClickListener(this)
        sensor_update.setOnClickListener(this)
    }

    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            dstAdress = mDeviceInfo.meshAddress
            Commander.getDeviceVersion(dstAdress,
                    successCallback = {
                        //                        versionLayoutPS.visibility = View.VISIBLE
                        tvPSVersion.text = it
                        var version = tvPSVersion.text.toString()
                        var num = version.substring(2, 3)
                        if (num.toDouble() >= 3.0) {

                        }
                    },
                    failedCallback = {
                        //                        versionLayoutPS.visibility = View.GONE
                    })
        } else {
            dstAdress = 0
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.triggering_conditions -> {
                var views = LayoutInflater.from(this).inflate(R.layout.popwindown_triggering_conditions, null)
                var set = findViewById<ConstraintLayout>(R.id.triggering_conditions)
                var popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                popupWindow.contentView = views
                popupWindow.isFocusable = true
                popupWindow.showAsDropDown(set)

                var any = views.findViewById<ConstraintLayout>(R.id.any_environment)
                var darker = views.findViewById<ConstraintLayout>(R.id.darker_environment)
                var veryDark = views.findViewById<ConstraintLayout>(R.id.very_dark_environment)

                when {
                    triggering_conditions_text.text.toString() == getString(R.string.any_environment) -> {
                        any.setBackgroundResource(R.color.blue_background)
                    }
                    triggering_conditions_text.text.toString() == getString(R.string.dark_environment) -> {
                        darker.setBackgroundResource(R.color.blue_background)
                    }
                    triggering_conditions_text.text.toString() == getString(R.string.very_dark_environment) -> {
                        veryDark.setBackgroundResource(R.color.blue_background)
                    }
                }

                any.setOnClickListener(View.OnClickListener {
                    triggering_conditions_text.text = getString(R.string.any_environment)
                    modeTriggerCondition = 0
                    popupWindow.dismiss()
                })

                darker.setOnClickListener(View.OnClickListener {
                    triggering_conditions_text.text = getString(R.string.dark_environment)
                    modeTriggerCondition = 1
                    popupWindow.dismiss()
                })

                veryDark.setOnClickListener(View.OnClickListener {
                    triggering_conditions_text.text = getString(R.string.very_dark_environment)
                    modeTriggerCondition = 2
                    popupWindow.dismiss()
                })

            }

            R.id.trigger_time -> {
                var views = LayoutInflater.from(this).inflate(R.layout.popwindow_trigger_time, null)
                var set = findViewById<ConstraintLayout>(R.id.trigger_time)
                var popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                popupWindow.contentView = views
                popupWindow.isFocusable = true
                popupWindow.showAsDropDown(set)

                var lightOn = views.findViewById<ConstraintLayout>(R.id.light_on)
                var lightOff = views.findViewById<ConstraintLayout>(R.id.light_off)
                var brightness = views.findViewById<ConstraintLayout>(R.id.customBrightness)

                when {
                    trigger_time_text.text.toString() == getString(R.string.light_on) -> {
                        lightOn.setBackgroundResource(R.color.blue_background)
                    }
                    trigger_time_text.text.toString() == getString(R.string.light_off) -> {
                        lightOff.setBackgroundResource(R.color.blue_background)
                    }
                    trigger_time_text.text.toString() != getString(R.string.light_on) && trigger_time_text.text.toString() != getString(R.string.light_off) -> {
                        brightness.setBackgroundResource(R.color.blue_background)
                    }
                }

                lightOn.setOnClickListener(View.OnClickListener {
                    trigger_time_text.text = getString(R.string.light_on)
                    modeStartUpMode = MODE_START_UP_MODE_OPEN
                    popupWindow.dismiss()
                })

                lightOff.setOnClickListener(View.OnClickListener {
                    trigger_time_text.text = getString(R.string.light_off)
                    modeStartUpMode = MODE_START_UP_MODE_CLOSE
                    popupWindow.dismiss()
                })

                brightness.setOnClickListener(View.OnClickListener {
                    popupWindow.dismiss()
                    val textGp = EditText(this)
                    textGp.inputType = InputType.TYPE_CLASS_NUMBER
                    textGp.maxLines = 3
                    StringUtils.initEditTextFilter(textGp)
                    AlertDialog.Builder(this)
                            .setTitle(R.string.target_brightness)
                            .setView(textGp)

                            .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->

                                var brin = textGp.text.toString().toInt()

                                if (brin == 0) {
                                    ToastUtil.showToast(this, "亮度不能为0")
                                    return@setPositiveButton
                                }

                                if (brin > 100) {
                                    ToastUtil.showToast(this, "亮度不能大于100")
                                    return@setPositiveButton
                                }

                                trigger_time_text.text = textGp.text.toString() + "%"
                                modeStartUpMode = MODE_START_UP_MODE_CUSTOMIZE
                                dialog.dismiss()
                            }
                            .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()

                    val timer = Timer()
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            val inputManager = textGp.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputManager.showSoftInput(textGp, 0)
                        }
                    }, 200)
                })

            }

            R.id.time_type -> {
                var views = LayoutInflater.from(this).inflate(R.layout.popwindow_time_type, null)
                var set = findViewById<ConstraintLayout>(R.id.time_type)
                var popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                popupWindow.contentView = views
                popupWindow.isFocusable = true
                popupWindow.showAsDropDown(set)

                var minute = views.findViewById<ConstraintLayout>(R.id.minute)
                var second = views.findViewById<ConstraintLayout>(R.id.second)

                when {
                    time_type_text.text.toString() == getString(R.string.minute) -> {
                        minute.setBackgroundResource(R.color.blue_background)
                    }
                    time_type_text.text.toString() == getString(R.string.second) -> {
                        second.setBackgroundResource(R.color.blue_background)
                    }
                }

                minute.setOnClickListener(View.OnClickListener {
                    time_type_text.text = getString(R.string.minute)
                    modeDelayUnit = MODE_DELAY_UNIT_MINUTE
                    popupWindow.dismiss()
                })

                second.setOnClickListener(View.OnClickListener {
                    time_type_text.text = getString(R.string.second)
                    modeDelayUnit = MODE_DELAY_UNIT_SECONDS
                    popupWindow.dismiss()
                })
            }

            R.id.brightness_change -> {
                var views = LayoutInflater.from(this).inflate(R.layout.popwindow_brightness_change, null)
                var set = findViewById<ConstraintLayout>(R.id.brightness_change)
                var popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                popupWindow.contentView = views
                popupWindow.isFocusable = true
                popupWindow.showAsDropDown(set)

                var moment = views.findViewById<ConstraintLayout>(R.id.moment)
                var gradient = views.findViewById<ConstraintLayout>(R.id.gradient)

                when {
                    brightness_change_text.text.toString() == getString(R.string.moment) -> {
                        moment.setBackgroundResource(R.color.blue_background)
                    }
                    brightness_change_text.text.toString() == getString(R.string.gradient) -> {
                        gradient.setBackgroundResource(R.color.blue_background)
                    }
                }

                moment.setOnClickListener(View.OnClickListener {
                    brightness_change_text.text = getString(R.string.moment)
                    modeSwitchMode = MODE_SWITCH_MODE_MOMENT
                    popupWindow.dismiss()
                })

                gradient.setOnClickListener(View.OnClickListener {
                    brightness_change_text.text = getString(R.string.gradient)
                    modeSwitchMode = MODE_SWITCH_MODE_GRADIENT
                    popupWindow.dismiss()
                })
            }

            R.id.choose_group -> {
                isFinish = true
                tv_function1.visibility = View.VISIBLE
                sensor_three.visibility = View.GONE
                edit_data_view_layout.visibility = View.VISIBLE
                toolbar.title = getString(R.string.select_group)

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
                        R.layout.night_light_sensor_adapter, showCheckListData!!)
                val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
                decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
                //添加分割线
                recyclerView_select_group_list_view.addItemDecoration(decoration)
                nightLightEditGroupAdapter?.bindToRecyclerView(recyclerView_select_group_list_view)
                nightLightEditGroupAdapter?.onItemClickListener = onItemClickListenerCheck
            }

            R.id.tv_function1 -> {
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

            R.id.sensor_update -> {
                configDevice()
            }
        }
    }

    private fun configDevice() {
        var time = editText.text.toString()

        if (time == "") {
            ToastUtil.showToast(this, "超时时间为空")
            return
        }

        if (time_type_text.text.toString() == getString(R.string.second)) {
            if (time.toInt() < 10) {
                ToastUtil.showToast(this, "超时时间不能小于10秒钟")
                return
            }

            if (time.toInt() > 255) {
                ToastUtil.showToast(this, "超时时间不能大于255秒钟")
                return
            }
        } else if (time_type_text.text.toString() == getString(R.string.minute)) {
            if (time.toInt() < 1) {
                ToastUtil.showToast(this, "超时时间不能小于1分钟")
                return
            }

            if (time.toInt() > 255) {
                ToastUtil.showToast(this, "超时时间不能大于255分钟")
                return
            }
        }

        if (showGroupList?.size == 0) {
            ToastUtils.showLong(getString(R.string.config_night_light_select_group))
            return
        }

        Thread {
            val mApplication = this.application as TelinkLightApplication
            val mesh = mApplication.getMesh()

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

        }.start()


//        finish()
    }

    private fun configureComplete() {
        saveSensor()
        TelinkLightService.Instance().idleMode(true)
        TelinkLightService.Instance().disconnect()
        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
    }

    private fun saveSensor() {
//        var sensor = DBUtils.getAllSensor()
//        if (sensor.size > 0) {
//            for (i in sensor.indices) {
//                if (mDeviceInfo.macAddress == sensor[i].macAddr) {
//                    var dbSensor: DbSensor = DbSensor()
//                    dbSensor.macAddr = mDeviceInfo.macAddress
//                    dbSensor.id = sensor[i].id
//                    dbSensor.productUUID = mDeviceInfo.productUUID
//                    dbSensor.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID)
//                    DBUtils.updateSensor(dbSensor)
//                } else {
//                    var dbSensor: DbSensor = DbSensor()
//                    DBUtils.saveSensor(dbSensor, false)
//                    dbSensor.controlGroupAddr = getControlGroup()
//                    dbSensor.index = dbSensor.id.toInt()
//                    dbSensor.macAddr = mDeviceInfo.macAddress
//                    dbSensor.meshAddr = Constant.SWITCH_PIR_ADDRESS
//                    dbSensor.productUUID = mDeviceInfo.productUUID
//                    dbSensor.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID)
//
//                    DBUtils.saveSensor(dbSensor, false)
//
//                    dbSensor = DBUtils.getSensorByID(dbSensor.id)!!
//
//                    DBUtils.recordingChange(dbSensor.id,
//                            DaoSessionInstance.getInstance().dbSensorDao.tablename,
//                            Constant.DB_ADD)
//                }
//            }
//
//        } else {


//        if (isUpdate == "1") {
//            var sensor = DBUtils.getAllSensor()
//            if (sensor.size > 0) {
//                for (i in sensor.indices) {
//                    if (mDeviceInfo.macAddress == sensor[i].macAddr) {
//                        var dbSensor: DbSensor = DbSensor()
//                        dbSensor.macAddr = mDeviceInfo.macAddress
//                        dbSensor.id = sensor[i].id
//                        dbSensor.productUUID = mDeviceInfo.productUUID
//                        dbSensor.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID)
//                        DBUtils.updateSensor(dbSensor)
//
//                    }
//                }
//            }
//        } else {
        var dbSensor: DbSensor = DbSensor()
        DBUtils.saveSensor(dbSensor, false)
        dbSensor.controlGroupAddr = getControlGroup()
        dbSensor.index = dbSensor.id.toInt()
        dbSensor.macAddr = mDeviceInfo.macAddress
        dbSensor.meshAddr = Constant.SWITCH_PIR_ADDRESS
        dbSensor.productUUID = mDeviceInfo.productUUID
        dbSensor.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID)

        DBUtils.saveSensor(dbSensor, false)

        dbSensor = DBUtils.getSensorByID(dbSensor.id)!!

        DBUtils.recordingChange(dbSensor.id,
                DaoSessionInstance.getInstance().dbSensorDao.tablename,
                Constant.DB_ADD)

//        }
//        }

    }

    private fun getControlGroup(): String? {
        var controlGroupListStr = ""
        for (j in showGroupList!!.indices) {
            if (j == 0) {
                controlGroupListStr = showGroupList!![j].groupAress.toString()
            } else {
                controlGroupListStr += "," + showGroupList!![j].groupAress.toString()
            }
        }
        return controlGroupListStr
    }


    private fun configNewlight() {
//        val spGroup = groupConvertSpecialValue(groupAddr)
        val groupH: Byte = (mSelectGroupAddr shr 8 and 0xff).toByte()
        val timeH: Byte = (selectTime shr 8 and 0xff).toByte()
        val timeL: Byte = (selectTime and 0xff).toByte()
        var mode = getModeValue()
        var paramBytes: ByteArray? = null
//        val paramBytes = byteArrayOf(
//                switchMode.toByte(), 0x00, 0x00,
//                tiet_Delay.text.toString().toInt().toByte(),
//                tietMinimumBrightness.text.toString().toInt().toByte(),
////                triggerLux.toByte(),
//                mode.toByte()
//        )

        when {
            trigger_time_text.text.toString() == getString(R.string.light_on) -> paramBytes = byteArrayOf(
                    switchMode.toByte(), 0x00, 0x00,
                    editText.text.toString().toInt().toByte(),
                    0x00,
                    modeTriggerCondition.toByte(),
                    mode.toByte()
            )
            trigger_time_text.text.toString() == getString(R.string.light_off) -> paramBytes = byteArrayOf(
                    switchMode.toByte(), 0x00, 0x00,
                    editText.text.toString().toInt().toByte(),
                    0x00,
                    modeTriggerCondition.toByte(),
                    mode.toByte()
            )
            else -> {
                var str = trigger_time_text.text.toString()
                var st = str.substring(0,str.indexOf("%"))
                paramBytes = byteArrayOf(
                        switchMode.toByte(), 0x00, 0x00,
                        editText.text.toString().toInt().toByte(),
                        st.toInt().toByte(),
                        modeTriggerCondition.toByte(),
                        mode.toByte()
                )
            }
        }

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

    private fun showDataListView() {
        isFinish = false
        toolbar.title = getString(R.string.human_body)
        sensor_three.visibility = View.VISIBLE
        edit_data_view_layout.visibility = View.GONE
        tv_function1.visibility = View.GONE

        val layoutmanager = GridLayoutManager(this, 3)
        recyclerGroup.layoutManager = layoutmanager as RecyclerView.LayoutManager?
        this.nightLightGroupRecycleViewAdapter = NightLightGroupRecycleViewAdapter(
                R.layout.activity_night_light_groups_item, showGroupList)
//        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
//        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
//        //添加分割线
//        recyclerViewNightLightGroups.addItemDecoration(decoration)
        nightLightGroupRecycleViewAdapter?.bindToRecyclerView(recyclerGroup)
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

    internal var onItemClickListenerCheck: BaseQuickAdapter.OnItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
        val item = showCheckListData!!.get(position)
        if (item.enableCheck) {
            showCheckListData!!.get(position).checked = !item.checked
            changeCheckedViewData()
            adapter?.notifyDataSetChanged()
        }
    }

    private fun getModeValue(): Int {
        LogUtils.d("FINAL_VALUE$modeStartUpMode-$modeDelayUnit-$modeSwitchMode")
        LogUtils.d("FINAL_VALUE" + (modeStartUpMode or modeDelayUnit or modeSwitchMode))
        return modeStartUpMode or modeDelayUnit or modeSwitchMode
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
}
