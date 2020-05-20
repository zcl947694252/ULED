package com.dadoutek.uled.pir

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.light.NightLightGroupRecycleViewAdapter
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.ItemGroup
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.othersview.InstructionsForUsActivity
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.light.DeviceInfo
import kotlinx.android.synthetic.main.activity_pir_new.*
import kotlinx.android.synthetic.main.activity_pir_new.trigger_time_text
import kotlinx.android.synthetic.main.template_radiogroup.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * 创建者     ZCL
 * 创建时间   2020/5/18 14:38
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class PirConfigActivity : TelinkBaseActivity(), View.OnClickListener {
    private val REQUEST_CODE_CHOOSE: Int = 1000
    private var isConfirm: Boolean = false
    private var mDeviceInfo: DeviceInfo? = null

    /**
     * 1 代表分 0代表秒
     */
    private var timeUnitType: Int = 1

    /**
     * 0 开 1关 2自定义
     */
    private var triggerAfterShow: Int = 0

    /**
     * 0全天    1白天   2夜晚
     */
    private var triggerKey: Int = 0
    private var popupWindow: PopupWindow? = null
    private var popAdapter: PopupWindowAdapter? = null
    private var isGroupMode = true
    private var showGroupList: MutableList<ItemGroup> = mutableListOf()
    private var bottomGvAdapter: NightLightGroupRecycleViewAdapter = NightLightGroupRecycleViewAdapter(R.layout.activity_night_light_groups_item, showGroupList)
    private val popDataList = mutableListOf<ItemCheckBean>()
    private val listTriggerTimes = mutableListOf<ItemCheckBean>()
    private val listTriggerAfterShow = mutableListOf<ItemCheckBean>()
    private val listTimeUnit = mutableListOf<ItemCheckBean>()
    private val listSelectTimes = mutableListOf<ItemCheckBean>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pir_new)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        top_rg_ly.setOnCheckedChangeListener { _, checkedId ->
            isGroupMode = checkedId == color_mode_rb.id
            changeGroupType(checkedId == R.id.color_mode_rb)
            when (checkedId) {
                R.id.color_mode_rb -> {
                    changeGroupType(true)
                }
                R.id.gradient_mode_rb -> {
                    changeGroupType(false)
                }
            }
        }
        pir_config_switch.setOnCheckedChangeListener { _, checkedId ->
            val byteArrayOf = if (checkedId == R.id.pir_config_switch_open)
                byteArrayOf(2, 0, 0, 0, 0, 0, 0, 0)//0打开
            else
                byteArrayOf(2, 1, 0, 0, 0, 0, 0, 0)//1关闭
            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT,
                    mDeviceInfo!!.meshAddress, byteArrayOf)
        }
        pir_config_see_help.setOnClickListener(this)
        pir_config_trigger_conditions.setOnClickListener(this)
        pir_config_trigger_after.setOnClickListener(this)
        pir_config_time_type.setOnClickListener(this)
        //pir_config_overtime_down_time.setOnClickListener(this)
        pir_config_choose_scene.setOnClickListener(this)
        pir_config_choose_group.setOnClickListener(this)
        bottomGvAdapter.onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.imgDelete) adapter.remove(position)
        }
    }

    private fun changeGroupType(b: Boolean) {
        if (b) {
            pir_config_choose_group_ly.visibility = View.VISIBLE
            pir_config_choose_scene_ly.visibility = View.GONE
            pir_config_trigger_after_ly.visibility = View.VISIBLE
            pir_config_trigger_after_view.visibility = View.VISIBLE
        } else {
            pir_config_choose_group_ly.visibility = View.GONE
            pir_config_choose_scene_ly.visibility = View.VISIBLE
            pir_config_trigger_after_ly.visibility = View.GONE
            pir_config_trigger_after_view.visibility = View.GONE
        }
    }

    private fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        val version = intent.getStringExtra("version")
        isNewPir(version)
        pir_confir_tvPSVersion.text = version
        isConfirm = mDeviceInfo?.isConfirm == 1//等于1代表是重新配置
        var lightGroup = DBUtils.allGroups

        for (i in lightGroup!!.indices) {
            val element = ItemGroup()
            element.gpName = "组i"
            showGroupList.add(element)
        }

        bottomGvAdapter?.notifyDataSetChanged()

        getTriggerTimeProide()
        getTriggerAfterShow()
        getTimeUnite()
        getSelectTimes()
    }

    private fun isNewPir(version: String?) {
        /* if (TelinkApplication.getInstance().connectDevice != null) {
             pir_confir_progress_ly.visibility = View.GONE
             pir_confir_tvPSVersion.text = version
             var version = pir_confir_tvPSVersion.text.toString()
             var num: String //N-3.1.1
             if (version.contains("N")) {
                 num = version.substring(2, 3)
                 if ("" != num && num != "-" && num.toDouble() >= 3.0) {
                     isGone()
                     isVisibility()//显示3.0新的人体感应器
                 }
             } else if (version.contains("PR")) {
                 isGone()
                 isVisibility()//显示3.0新的人体感应器
             }
         } else {
             LogUtils.d("device isn't connected, auto connect it")
             autoConnectSensor()
         }*/
    }

    private fun getSelectTimes() {
        listSelectTimes.add(ItemCheckBean(getString(R.string.ten_second), true))
        listSelectTimes.add(ItemCheckBean(getString(R.string.twenty_second), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.thirty_second), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.forty_second), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.fifty_second), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.one_minute), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.two_minute), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.three_minute), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.four_minute), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.five_minute), false))
    }

    private fun getTimeUnite() {
        listTimeUnit.add(ItemCheckBean(getString(R.string.minute), true))
        listTimeUnit.add(ItemCheckBean(getString(R.string.second), false))
    }

    private fun getTriggerAfterShow() {
        listTriggerAfterShow.add(ItemCheckBean(getString(R.string.light_on), true))
        listTriggerAfterShow.add(ItemCheckBean(getString(R.string.light_off), false))
        listTriggerAfterShow.add(ItemCheckBean(getString(R.string.custom_brightness), false))
    }

    private fun getTriggerTimeProide() {
        listTriggerTimes.add(ItemCheckBean(getString(R.string.all_day), false))
        listTriggerTimes.add(ItemCheckBean(getString(R.string.day_time), false))
        listTriggerTimes.add(ItemCheckBean(getString(R.string.night), false))
    }

    private fun initView() {
        toolbar.title = getString(R.string.human_body)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        color_mode_rb.text = getString(R.string.group_mode)
        gradient_mode_rb.text = getString(R.string.scene_mode)
        color_mode_rb.isChecked = true
        makePopView()
        changeGroupType(true)
        makeGrideView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == Activity.RESULT_OK) {
            if (isGroupMode) {
                val arrayList = data?.getParcelableArrayListExtra<Parcelable>("data") as ArrayList<CheckItemBean>
                arrayList.forEach {
                    val gp = DBUtils.getGroupByID(it.id)
                    gp?.let { itGp ->
                        val newItemGroup = ItemGroup()
                        newItemGroup.brightness = 50
                        newItemGroup.temperature = 50
                        newItemGroup.color = R.color.white
                        newItemGroup.checked = true
                        newItemGroup.enableCheck = true
                        newItemGroup.gpName = itGp.name
                        newItemGroup.groupAddress = itGp.meshAddr
                        showGroupList.add(newItemGroup)
                    }

                }
            } else {
                val arrayList = data?.getParcelableArrayListExtra<Parcelable>("data") as ArrayList<DbScene>
                arrayList.forEach {
                    val scene = DBUtils.getSceneByID(it.id)
                    scene?.let { itscene ->
                        val newItemGroup = ItemGroup()
                        newItemGroup.brightness = 50
                        newItemGroup.temperature = 50
                        newItemGroup.color = R.color.white
                        newItemGroup.checked = true
                        newItemGroup.enableCheck = true
                        newItemGroup.gpName = itscene.name
                        showGroupList.add(newItemGroup)
                    }

                }
            }
            bottomGvAdapter.notifyDataSetChanged()
        }
    }

    @RequiresApi(Build.VERSION_CODES.CUPCAKE)
    @SuppressLint("SetTextI18n")
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.pir_config_trigger_conditions -> {//触发条件
                popDataList.clear()
                popDataList.addAll(listTriggerTimes)
                popAdapter?.notifyDataSetChanged()
                popAdapter?.setOnItemClickListener { _, _, position ->
                    triggerKey = position
                    changeTriggerTimeChecked(position, 0)
                    pir_config_triggering_conditions_text.text = popDataList[position].title
                    popupWindow?.dismiss()
                }
                popupWindow?.showAsDropDown(pir_config_trigger_conditions)
            }

            R.id.pir_config_trigger_after -> {//触发后
                popDataList.clear()
                popDataList.addAll(listTriggerAfterShow)
                popAdapter?.notifyDataSetChanged()
                popAdapter?.setOnItemClickListener { _, _, position ->
                    triggerAfterShow = position
                    popupWindow?.dismiss()
                    when (position) {
                        0, 1 -> {
                            pir_config_triggering_conditions_text.text = popDataList[position].title
                            changeTriggerTimeChecked(position, 1)
                        }
                        2 -> setBrightness()
                    }
                }
                popupWindow?.showAsDropDown(pir_config_trigger_after)
            }

            R.id.pir_config_time_type -> {
                popDataList.clear()
                popDataList.addAll(listTimeUnit)
                popAdapter?.notifyDataSetChanged()
                popAdapter?.setOnItemClickListener { _, _, position ->
                    popupWindow?.dismiss()
                    pir_config_time_type_text.text = popDataList[position].title
                    timeUnitType = position
                }
                popupWindow?.showAsDropDown(pir_config_time_type)
            }

            /*  R.id.pir_config_overtime_down_time -> {
                  popDataList.clear()
                  popDataList.addAll(listSelectTimes)
                  popAdapter?.notifyDataSetChanged()
                  popAdapter?.setOnItemClickListener { _, _, position ->
                      popupWindow?.dismiss()
                      val title = popDataList[position].title
                      pir_config_overtime_down_time_tv.text = title
                      selectTime = (title.replace("s", "")).toInt()
                      LogUtils.v("zcl-----------传感器配置选择时间-------$selectTime")
                  }
                  popupWindow?.showAsDropDown(pir_config_overtime_down_time)
              }*/

            R.id.pir_config_choose_group, R.id.pir_config_choose_scene -> {
                val intent = Intent(this@PirConfigActivity, ChooseMoreGroupOrSceneActivity::class.java)
                if (v.id == R.id.pir_config_choose_group)
                    intent.putExtra(Constant.EIGHT_SWITCH_TYPE, 0)
                else
                    intent.putExtra(Constant.EIGHT_SWITCH_TYPE, 1)
                startActivityForResult(intent, REQUEST_CODE_CHOOSE)
            }

            R.id.pir_config_see_help -> {
                var intent = Intent(this@PirConfigActivity, InstructionsForUsActivity::class.java)
                startActivity(intent)
            }

            R.id.pir_config_btn -> {
                configDevice()
            }
        }
    }

    private fun configDevice() {
        var time = pir_config_overtime_tv.text.toString()
        when {
            TextUtils.isEmpty(time) -> {
                ToastUtils.showShort(getString(R.string.timeout_period_is_empty))
            }
            timeUnitType == 1 -> {//0 代表分 1代表秒
                when {
                    time.toInt() < 10 -> {
                        ToastUtils.showShort(getString(R.string.timeout_time_less_ten))
                        return
                    }
                    time.toInt() > 255 -> {
                        ToastUtils.showShort(getString(R.string.timeout_255))
                        return
                    }
                }
            }
            timeUnitType == 0 -> {
                when {
                    time.toInt() < 1 -> {
                        ToastUtils.showShort(getString(R.string.timeout_1m))
                        return
                    }
                    time.toInt() > 255 -> {
                        ToastUtils.showShort(getString(R.string.timeout_255_big))
                        return
                    }
                }
            }
            showGroupList.size == 0 -> {
                ToastUtils.showLong(getString(R.string.config_night_light_select_group))
            }
            else -> {//符合所有条件
                if (isGroupMode) {
                    //组地址选择功能
                    val paramsSetGroup = byteArrayOf(1, 2, 0, 0, 0, 0, 0, 0, 0)//1关闭 最多支持七个
                    showGroupList.forEach {
                        val lowAdd = it.groupAddress or 0xff
                        paramsSetGroup[paramsSetGroup.lastIndex] = lowAdd.toByte()
                    }
                    TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT, mDeviceInfo!!.meshAddress, paramsSetGroup)
                    //11固定1 12-13保留 14 持续时间 15最终亮度 16触发照度(条件) 17触发设置 最低位1是开 0是关 与开关命令反了 次低 1 分钟 0 秒
                    var triggerSetNum =0
                    when (triggerAfterShow){

                    }
                    //byteArrayOf(1,0,0,time.toInt().toByte(),50,triggerKey.toByte(),)
                } else {

                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setBrightness() {
        val textGp = EditText(this)
        textGp.inputType = InputType.TYPE_CLASS_NUMBER
        textGp.maxLines = 3
        StringUtils.initEditTextFilter(textGp)
        AlertDialog.Builder(this)
                .setTitle(R.string.target_brightness)
                .setView(textGp)
                .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                    var brightness = textGp.text.toString()
                    if (brightness == "")
                        brightness = "0"
                    var brin = brightness.toInt()
                    if (brin == 0) {
                        ToastUtils.showShort(getString(R.string.brightness_cannot))
                        return@setPositiveButton
                    }
                    if (brin > 100) {
                        ToastUtils.showShort(getString(R.string.brightness_cannot_be_greater_than))
                        return@setPositiveButton
                    }
                    trigger_time_text.text = textGp.text.toString() + "%"
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()

        GlobalScope.launch {
            delay(200)
            val inputManager = textGp.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.showSoftInput(textGp, 0)
        }
    }

    private fun changeTriggerTimeChecked(i: Int, isTriggerrTime: Int) {
        when (isTriggerrTime) {//0 触发条件
            0 -> {
                listTriggerTimes[0].checked = i == 0
                listTriggerTimes[1].checked = i == 1
                listTriggerTimes[2].checked = i == 2
            }
            1 -> {//触发后显示方式
                listTriggerAfterShow[0].checked = i == 0
                listTriggerAfterShow[1].checked = i == 1
                listTriggerAfterShow[2].checked = i == 2
            }
            2 -> {//超时单位
                listTriggerAfterShow[0].checked = i == 0
                listTriggerAfterShow[1].checked = i == 1
                listTriggerAfterShow[2].checked = i == 2
            }
        }
    }

    private fun makeGrideView() {
        pir_config_recyclerGroup.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        pir_config_recyclerGroup.layoutManager = GridLayoutManager(this, 3)
        bottomGvAdapter?.bindToRecyclerView(pir_config_recyclerGroup)
    }

    private fun makePopView() {
        var views = LayoutInflater.from(this).inflate(R.layout.pop_recycleview, null)
        popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow?.contentView = views
        var recycleView = views.findViewById<RecyclerView>(R.id.pop_recycle)
        popAdapter = PopupWindowAdapter(R.layout.pop_item_sigle, popDataList)
        recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recycleView.adapter = popAdapter
        popAdapter?.bindToRecyclerView(recycleView)
        popupWindow?.isFocusable = true
    }
}