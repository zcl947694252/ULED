package com.dadoutek.uled.othersview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import com.dadoutek.uled.R
import com.dadoutek.uled.device.model.DeviceItem
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.othersview.adapter.DeviceTypeAdapter
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*

class SelectDeviceTypeActivity : AppCompatActivity() {

    private val deviceTypeList = mutableListOf<com.dadoutek.uled.device.model.DeviceItem>()
    private val deviceAdapter =  DeviceTypeAdapter(R.layout.select_device_type_item, deviceTypeList)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_device_type)
        initView()
        initData()
        initListener()
    }

    private fun initData() {
        deviceTypeList.clear()
        deviceTypeList.add(DeviceItem(getString(R.string.normal_light), 0, DeviceType.LIGHT_NORMAL))
        deviceTypeList.add(DeviceItem(getString(R.string.rgb_light), 0, DeviceType.LIGHT_RGB))
        deviceTypeList.add(DeviceItem(getString(R.string.switch_name), 0, DeviceType.NORMAL_SWITCH))
        deviceTypeList.add(DeviceItem(getString(R.string.sensoR), 0, DeviceType.SENSOR))
        deviceTypeList.add(DeviceItem(getString(R.string.curtain), 0, DeviceType.SMART_CURTAIN))
        deviceTypeList.add(DeviceItem(getString(R.string.relay), 0, DeviceType.SMART_RELAY))
        deviceTypeList.add(DeviceItem(getString(R.string.Gate_way), 0, DeviceType.GATE_WAY))

        deviceAdapter.notifyDataSetChanged()
    }

    private fun initView() {
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbarTv.text = getString(R.string.add_device)
        template_recycleView.layoutManager = GridLayoutManager(this@SelectDeviceTypeActivity,3)
        template_recycleView.adapter = deviceAdapter
    }



    private fun initListener() {
    }

}
