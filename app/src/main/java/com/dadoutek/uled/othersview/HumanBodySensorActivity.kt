package com.dadoutek.uled.othersview

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.text.InputType
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
import com.chad.library.adapter.base.BaseQuickAdapter.OnItemClickListener
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

/**
 * 人体感应器
 */
class HumanBodySensorActivity : TelinkBaseActivity(), View.OnClickListener {

    private lateinit var mDeviceInfo: DeviceInfo

    private var nightLightGroupRecycleViewAdapter: NightLightGroupRecycleViewAdapter? = null
    private var nightLightEditGroupAdapter: NightLightEditGroupAdapter? = null

    private var mSelectGroupAddr: Int = 0xFF  //代表所有灯
    private val CMD_OPEN_LIGHT = 0X01
    private val CMD_CLOSE_LIGHT = 0X00
    private val CMD_CONTROL_GROUP = 0X02
    private var switchMode = 0X01
    lateinit var secondsList: Array<String>
    private var selectTime = 10
    private var currentPageIsEdit = false
    private var showGroupList: MutableList<ItemGroup>? = null
    /**
     * 显示选择分组下拉的数据
     */
    private var showCheckListData: MutableList<DbGroup>? = null
    private var modeStartUpMode = 0
    private var modeDelayUnit = 2
    private var modeSwitchMode = 0
    private var modeTriggerCondition = 0
    private val MODE_START_UP_MODE_OPEN = 0
    private val MODE_DELAY_UNIT_SECONDS = 0
    private val MODE_SWITCH_MODE_MOMENT = 0
    private val MODE_START_UP_MODE_CLOSE = 1
    private val MODE_DELAY_UNIT_MINUTE = 2
    private val MODE_SWITCH_MODE_GRADIENT = 4

    private var isFinish: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.huuman_body_sensor)
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        initToolbar()
        initData()
        getVersion()
        initView()
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

        showGroupList = ArrayList()

        for (i in showCheckListData!!.indices) {
            val dbGroup = showCheckListData!![i]
            dbGroup.checked = i == 0 && dbGroup.meshAddr == 0xffff
            if (dbGroup.checked) {
                toolbar.title = dbGroup.name
                showGroupList?.let {
                    if (it!!.size == 0) {
                        val newItemGroup = ItemGroup()
                        newItemGroup.brightness = 50
                        newItemGroup.temperature = 50
                        newItemGroup.color = R.color.white
                        newItemGroup.checked = true
                        newItemGroup.enableCheck = true
                        newItemGroup.gpName = showCheckListData!![0].name
                        newItemGroup.groupAress = showCheckListData!![0].meshAddr
                        it.add(newItemGroup)
                        showDataListView()
                    }
                }
            }
        }

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
        time.setOnClickListener(this)
        trigger_mode.setOnClickListener(this)
    }

    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            dstAdress = mDeviceInfo!!.meshAddress
            Commander.getDeviceVersion(dstAdress,
                    successCallback = {
                        //                        versionLayoutPS.visibility = View.VISIBLE
                        tvPSVersion.text = it
                        var version = tvPSVersion.text.toString()
                        var num = version.substring(2, 3)
                        if (num.toDouble() >= 3.0) {
                            isGone()
                            isVisibility()
                        }
                    },
                    failedCallback = {
                        //                        versionLayoutPS.visibility = View.GONE
                    })
        } else {
            dstAdress = 0
        }
    }

    private fun isGone() {
        constraintLayout30.visibility = View.GONE
        view30.visibility = View.GONE
        constraintLayout31.visibility = View.GONE
        view31.visibility = View.GONE
    }

    private fun isVisibility() {
        constraintLayout17.visibility = View.VISIBLE
        view15.visibility = View.VISIBLE
        constraintLayout18.visibility = View.VISIBLE
        view16.visibility = View.VISIBLE
        constraintLayout19.visibility = View.VISIBLE
        view17.visibility = View.VISIBLE
        constraintLayout21.visibility = View.VISIBLE
        view18.visibility = View.VISIBLE
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
                                    ToastUtil.showToast(this, getString(R.string.brightness_cannot))
                                    return@setPositiveButton
                                }

                                if (brin > 100) {
                                    ToastUtil.showToast(this, getString(R.string.brightness_cannot_be_greater_than))
                                    return@setPositiveButton
                                }

                                trigger_time_text.text = textGp.text.toString() + "%"
                                modeStartUpMode = MODE_START_UP_MODE_OPEN
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

                moment.setOnClickListener {
                    brightness_change_text.text = getString(R.string.moment)
                    modeSwitchMode = MODE_SWITCH_MODE_MOMENT
                    popupWindow.dismiss()
                }

                gradient.setOnClickListener {
                    brightness_change_text.text = getString(R.string.gradient)
                    modeSwitchMode = MODE_SWITCH_MODE_GRADIENT
                    popupWindow.dismiss()
                }
            }

            R.id.time -> {
                var views = LayoutInflater.from(this).inflate(R.layout.popwindow_choose_time, null)
                var set = findViewById<ConstraintLayout>(R.id.time)
                var popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                popupWindow.contentView = views
                popupWindow.isFocusable = true
                popupWindow.showAsDropDown(set)

                var tenSec = views.findViewById<ConstraintLayout>(R.id.ten_second)
                var twentySec = views.findViewById<ConstraintLayout>(R.id.twenty_second)
                var thirtySec = views.findViewById<ConstraintLayout>(R.id.thirty_second)
                var fortySec = views.findViewById<ConstraintLayout>(R.id.forty_second)
                var fiftySec = views.findViewById<ConstraintLayout>(R.id.fifty_second)
                var oneMin = views.findViewById<ConstraintLayout>(R.id.one_minute)
                var twoMin = views.findViewById<ConstraintLayout>(R.id.two_minute)
                var threeMin = views.findViewById<ConstraintLayout>(R.id.three_minute)
                var fourMin = views.findViewById<ConstraintLayout>(R.id.four_minute)
                var fiveMin = views.findViewById<ConstraintLayout>(R.id.five_minute)

                when {
                    time_text.text.toString() == getString(R.string.ten_second) -> {
                        tenSec.setBackgroundResource(R.color.blue_background)
                    }
                    time_text.text.toString() == getString(R.string.twenty_second) -> {
                        twentySec.setBackgroundResource(R.color.blue_background)
                    }
                    time_text.text.toString() == getString(R.string.thirty_second) -> {
                        thirtySec.setBackgroundResource(R.color.blue_background)
                    }
                    time_text.text.toString() == getString(R.string.forty_second) -> {
                        fortySec.setBackgroundResource(R.color.blue_background)
                    }
                    time_text.text.toString() == getString(R.string.fifty_second) -> {
                        fiftySec.setBackgroundResource(R.color.blue_background)
                    }
                    time_text.text.toString() == getString(R.string.one_minute) -> {
                        oneMin.setBackgroundResource(R.color.blue_background)
                    }
                    time_text.text.toString() == getString(R.string.two_minute) -> {
                        twoMin.setBackgroundResource(R.color.blue_background)
                    }
                    time_text.text.toString() == getString(R.string.three_minute) -> {
                        threeMin.setBackgroundResource(R.color.blue_background)
                    }
                    time_text.text.toString() == getString(R.string.four_minute) -> {
                        fourMin.setBackgroundResource(R.color.blue_background)
                    }
                    time_text.text.toString() == getString(R.string.five_minute) -> {
                        fiveMin.setBackgroundResource(R.color.blue_background)
                    }
                }

                tenSec.setOnClickListener {
                    time_text.text = getString(R.string.ten_second)
                    selectTime = 10
                    popupWindow.dismiss()
                }

                twentySec.run {

                    when {
                        time_text.text.toString() == getString(R.string.ten_second) -> {
                            tenSec.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.twenty_second) -> {
                            twentySec.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.thirty_second) -> {
                            thirtySec.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.forty_second) -> {
                            fortySec.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.fifty_second) -> {
                            fiftySec.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.one_minute) -> {
                            oneMin.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.two_minute) -> {
                            twoMin.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.three_minute) -> {
                            threeMin.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.four_minute) -> {
                            fourMin.setBackgroundResource(R.color.blue_background)
                        }
                        time_text.text.toString() == getString(R.string.five_minute) -> {
                            fiveMin.setBackgroundResource(R.color.blue_background)
                        }
                    }

                    tenSec.setOnClickListener {
                        time_text.text = getString(R.string.ten_second)
                        selectTime = 10
                        popupWindow.dismiss()
                    }

                    twentySec.setOnClickListener {
                        time_text.text = getString(R.string.twenty_second)
                        selectTime = 20
                        popupWindow.dismiss()
                    }
                }

                thirtySec.setOnClickListener {
                    time_text.text = getString(R.string.thirty_second)
                    selectTime = 30
                    popupWindow.dismiss()
                }

                fortySec.setOnClickListener {
                    time_text.text = getString(R.string.forty_second)
                    selectTime = 40
                    popupWindow.dismiss()
                }

                fiftySec.setOnClickListener {
                    time_text.text = getString(R.string.fifty_second)
                    selectTime = 50
                    popupWindow.dismiss()
                }

                oneMin.setOnClickListener {
                    time_text.text = getString(R.string.one_minute)
                    selectTime = 1 * 60
                    popupWindow.dismiss()
                }

                twoMin.setOnClickListener {
                    time_text.text = getString(R.string.two_minute)
                    selectTime = 2 * 60
                    popupWindow.dismiss()
                }

                threeMin.setOnClickListener {
                    time_text.text = getString(R.string.three_minute)
                    selectTime = 3 * 60
                    popupWindow.dismiss()
                }

                fourMin.setOnClickListener {
                    time_text.text = getString(R.string.four_minute)
                    selectTime = 4 * 50
                    popupWindow.dismiss()
                }

                fiveMin.setOnClickListener {
                    time_text.text = getString(R.string.five_minute)
                    selectTime = 5 * 60
                    popupWindow.dismiss()
                }
            }

            R.id.trigger_mode -> {
                var views = LayoutInflater.from(this).inflate(R.layout.popwindow_trigger_mode, null)
                var set = findViewById<ConstraintLayout>(R.id.trigger_mode)
                var popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                popupWindow.contentView = views
                popupWindow.isFocusable = true
                popupWindow.showAsDropDown(set)

                var turnOn = views.findViewById<ConstraintLayout>(R.id.turn_on)
                var turnOff = views.findViewById<ConstraintLayout>(R.id.turn_off)

                when {
                    trigger_mode_text.text.toString() == getString(R.string.light_on) -> {
                        turnOn.setBackgroundResource(R.color.blue_background)
                    }

                    trigger_mode_text.text.toString() == getString(R.string.light_off) -> {
                        turnOff.setBackgroundResource(R.color.blue_background)
                    }
                }

                turnOn.setOnClickListener {
                    trigger_mode_text.text = getString(R.string.light_on)
                    switchMode = CMD_OPEN_LIGHT
                    popupWindow.dismiss()
                }

                turnOff.setOnClickListener {
                    trigger_mode_text.text = getString(R.string.light_off)
                    switchMode = CMD_CLOSE_LIGHT
                    popupWindow.dismiss()
                }
            }

            R.id.choose_group -> {
                isFinish = true

                tv_function1.visibility = View.VISIBLE
                sensor_three.visibility = View.GONE
                edit_data_view_layout.visibility = View.VISIBLE

                showCheckListData?.let {
                    if (showGroupList!!.size != 0) {
                        for (i in it.indices)//0-1
                            for (j in showGroupList!!.indices)//0-1-3
                                if (it[i].meshAddr == showGroupList!![j].groupAress) {
                                    it[i].checked = true
                                    break //j = 0-1-2   3-1=2
                                } else if (j == showGroupList!!.size - 1 && it[i].meshAddr != showGroupList!![j].groupAress) {
                                    it[i].checked = false
                                }
                        changeCheckedViewData()
                    } else {
                        for (i in it.indices) {
                            it[i].enableCheck = true
                            it[i].checked = i == 0
                        }
                    }

                    recyclerView_select_group_list_view.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
                    nightLightEditGroupAdapter = NightLightEditGroupAdapter(R.layout.night_light_sensor_adapter, it)
                    //添加分割线
                    val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
                    decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
                    recyclerView_select_group_list_view.addItemDecoration(decoration)

                    nightLightEditGroupAdapter?.bindToRecyclerView(recyclerView_select_group_list_view)
                    nightLightEditGroupAdapter?.onItemClickListener = OnItemClickListener { adapter, _, position ->
                        val item = it[position]
                        if (item.checked) {//t状态
                            item.checked = !item.checked
                        } else {//f状态下
                            if (position == 0 && item.meshAddr == 0xffff) {//65535
                                setFrist()
                            } else {
                                item.checked = true
                                if (position != 0) {
                                    it[0].checked = false
                                } else {
                                    setFrist()
                                }
                            }
                        }
                        adapter?.notifyDataSetChanged()
                        /* if (item.enableCheck) {
                             item.checked = !item.checked
                             changeCheckedViewData()
                             adapter?.notifyDataSetChanged()
                         }*/
                    }
                }
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

    private fun setFrist() {
        for (i in showCheckListData!!.indices) {
            showCheckListData!![i].checked = i == 0 //选中全部
        }
    }

    private fun configDevice() {
        var version = tvPSVersion.text.toString()
        var num = version.substring(2, 3)
        if (num.toDouble() >= 3.0) {
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
        } else {
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
//       finish()
    }

    private fun configLightlight() {
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
//        }}

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
                var st = str.substring(0, str.indexOf("%"))
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

    /**
     * 显示已选中分组
     */
    private fun showDataListView() {
        isFinish = false
        toolbar.title = getString(R.string.human_body)
        sensor_three.visibility = View.VISIBLE
        edit_data_view_layout.visibility = View.GONE
        tv_function1.visibility = View.GONE

        recyclerGroup.layoutManager = GridLayoutManager(this, 3)
        this.nightLightGroupRecycleViewAdapter = NightLightGroupRecycleViewAdapter(
                R.layout.activity_night_light_groups_item, showGroupList)

        nightLightGroupRecycleViewAdapter?.bindToRecyclerView(recyclerGroup)
        nightLightGroupRecycleViewAdapter?.onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
            when (view.id) {
                R.id.imgDelete -> delete(adapter, position)
            }
        }
    }

    private fun delete(adapter: BaseQuickAdapter<Any, BaseViewHolder>, position: Int) {
        adapter.remove(position)
    }


    private fun getModeValue(): Int {
        LogUtils.d("FINAL_VALUE$modeStartUpMode-$modeDelayUnit-$modeSwitchMode")
        LogUtils.d("FINAL_VALUE" + (modeStartUpMode or modeDelayUnit or modeSwitchMode))
        return modeStartUpMode or modeDelayUnit or modeSwitchMode
    }

    private fun changeCheckedViewData() {
        var isAllCanCheck = true
        for (i in showCheckListData!!.indices) {
            if (showCheckListData!![0].meshAddr == 0xffff && showCheckListData!![0].checked) {//选中全部分组
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
