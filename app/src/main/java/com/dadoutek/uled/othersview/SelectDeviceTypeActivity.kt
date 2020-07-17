package com.dadoutek.uled.othersview

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.device.model.DeviceItem
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.othersview.adapter.DeviceTypeAdapter
import com.dadoutek.uled.util.StringUtils
import kotlinx.android.synthetic.main.activity_select_device_type.*
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*

class SelectDeviceTypeActivity : TelinkBaseActivity() {
    private val deviceTypeList = mutableListOf<DeviceItem>()
    private val deviceAdapter = DeviceTypeAdapter(R.layout.select_device_type_item, deviceTypeList)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_device_type)
        initView()
        initData()
        initListener()
    }
     fun initData() {
        deviceTypeList.clear()
        deviceTypeList.add(DeviceItem(getString(R.string.normal_light), 0, DeviceType.LIGHT_NORMAL))
        deviceTypeList.add(DeviceItem(getString(R.string.rgb_light), 0, DeviceType.LIGHT_RGB))
        deviceTypeList.add(DeviceItem(getString(R.string.switch_name), 0, DeviceType.NORMAL_SWITCH))
        deviceTypeList.add(DeviceItem(getString(R.string.sensor), 0, DeviceType.SENSOR))
        deviceTypeList.add(DeviceItem(getString(R.string.curtain), 0, DeviceType.SMART_CURTAIN))
        deviceTypeList.add(DeviceItem(getString(R.string.relay), 0, DeviceType.SMART_RELAY))
        deviceTypeList.add(DeviceItem(getString(R.string.Gate_way), 0, DeviceType.GATE_WAY))

        deviceAdapter.notifyDataSetChanged()
    }

     fun initView() {
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setNavigationIcon(R.drawable.icon_return)
        image_bluetooth.visibility = View.GONE
        toolbarTv.text = getString(R.string.add_device_new)
        template_recycleView.layoutManager = GridLayoutManager(this@SelectDeviceTypeActivity, 3)
        template_recycleView.adapter = deviceAdapter
    }


     fun initListener() {
         install_see_helpe.setOnClickListener {
             seeHelpe()
         }
        deviceAdapter.setOnItemClickListener { _, _, position ->
            when (position) {
                INSTALL_GATEWAY -> {
                    installId = INSTALL_GATEWAY
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position,getString(R.string.Gate_way))
                }
                INSTALL_NORMAL_LIGHT -> {
                    installId = INSTALL_NORMAL_LIGHT
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.normal_light))
                }
                INSTALL_RGB_LIGHT -> {
                    installId = INSTALL_RGB_LIGHT
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.rgb_light))
                }
                INSTALL_CURTAIN -> {
                    installId = INSTALL_CURTAIN
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.curtain))
                }
                INSTALL_SWITCH -> {
                    installId = INSTALL_SWITCH
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.switch_title))
                    stepOneText?.visibility = View.GONE
                    stepTwoText?.visibility = View.GONE
                    stepThreeText?.visibility = View.GONE
                    switchStepOne?.visibility = View.VISIBLE
                    switchStepTwo?.visibility = View.VISIBLE
                    swicthStepThree?.visibility = View.VISIBLE
                }
                INSTALL_SENSOR -> {
                    installId = INSTALL_SENSOR
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.sensor))
                }
                INSTALL_CONNECTOR -> {
                    installId = INSTALL_CONNECTOR
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.relay))
                }
            }
        }
    }
}
