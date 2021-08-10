package com.dadoutek.uled.switches

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.device.model.DeviceItem
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.DeviceType.DOUBLE_SWITCH
import com.dadoutek.uled.model.DeviceType.EIGHT_SWITCH
import com.dadoutek.uled.model.DeviceType.FOUR_SWITCH
import com.dadoutek.uled.model.DeviceType.NORMAL_SWITCH
import com.dadoutek.uled.model.DeviceType.NORMAL_SWITCH2
import com.dadoutek.uled.model.DeviceType.SCENE_SWITCH
import com.dadoutek.uled.model.DeviceType.SIX_SWITCH
import com.dadoutek.uled.model.DeviceType.SMART_CURTAIN_SWITCH
import com.dadoutek.uled.othersview.adapter.SwitchTypeAdapter
import com.dadoutek.uled.util.StringUtils
import kotlinx.android.synthetic.main.activity_select_device_type.*
import kotlinx.android.synthetic.main.activity_select_switch.*
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.template_recycleview.template_recycleView
import kotlinx.android.synthetic.main.toolbar.*

/**
 * 选择开关界面
 */
class SelectSwitchActivity : TelinkBaseActivity() {

    private val deviceTypeList = mutableListOf<DeviceItem>()
    private val deviceAdapter = SwitchTypeAdapter(R.layout.select_device_type_item, deviceTypeList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_switch)
        initView()
        initData()
        initListener()
    }

    fun initData() {
        deviceTypeList.clear()
        deviceTypeList.add(DeviceItem(getString(R.string.normal_swith), 0, DeviceType.NORMAL_SWITCH)) // 普通触摸开关
        deviceTypeList.add(DeviceItem(getString(R.string.normal_swith2), 0, DeviceType.NORMAL_SWITCH2)) // 普通面板光能开关
        deviceTypeList.add(DeviceItem(getString(R.string.eight_switch), 0, DeviceType.EIGHT_SWITCH)) // 八键开关
        deviceTypeList.add(DeviceItem(getString(R.string.double_switch), 0, DeviceType.DOUBLE_SWITCH)) // 双组开关
        deviceTypeList.add(DeviceItem(getString(R.string.scene_switch), 0, DeviceType.SCENE_SWITCH)) // 场景开关
        deviceTypeList.add(DeviceItem(getString(R.string.curtain_switch), 0, DeviceType.SMART_CURTAIN_SWITCH)) // 窗帘开关
        deviceTypeList.add(DeviceItem(getString(R.string.four_switch),0,DeviceType.FOUR_SWITCH))
        deviceTypeList.add(DeviceItem(getString(R.string.six_switch),0,DeviceType.SIX_SWITCH))
        deviceAdapter.notifyDataSetChanged()
    }

    fun initView() {
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        image_bluetooth.visibility = View.GONE
        toolbarTv.text = getString(R.string.select_switch)
        select_switch_recyclerView.layoutManager = GridLayoutManager(this@SelectSwitchActivity, 4)
        select_switch_recyclerView.adapter = deviceAdapter
    }

    fun initListener() {
        see_help_select.setOnClickListener {
            seeHelpe("#add-and-configure")
        }
        deviceAdapter.setOnItemClickListener { _, _, position ->
            when (position) {
                0 -> {
                    switchId = NORMAL_SWITCH
                }
                1 -> {
                    switchId = NORMAL_SWITCH2
                }
                2 -> {
                    switchId = EIGHT_SWITCH
                }
                3 -> {
                    switchId = DOUBLE_SWITCH
                }
                4 -> {
                    switchId = SCENE_SWITCH
                }
                5 -> {
                    switchId = SMART_CURTAIN_SWITCH
                }
                6 -> {
                    switchId = FOUR_SWITCH
                }
                7 -> {
                    switchId = SIX_SWITCH
                }
                else -> {
                    switchId = INSTALL_SWITCH
                }
            }
            installId = INSTALL_SWITCH
            showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.switch_title))
            stepOneText?.visibility = View.GONE
            stepTwoText?.visibility = View.GONE
            stepThreeText?.visibility = View.GONE
            switchStepOne?.visibility = View.VISIBLE
            switchStepTwo?.visibility = View.VISIBLE
            swicthStepThree?.visibility = View.VISIBLE
        }
    }
}