package com.dadoutek.uled.device

import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.tellink.TelinkLightApplication

class DeviceTypeRecycleViewAdapter(layoutResId: Int, data: List<String>?) : BaseItemDraggableAdapter<String, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, name: String?) {
                helper.setText(R.id.txt_name, name)
              if(name==TelinkLightApplication.getInstance().getString(R.string.normal_light)){
                  var lightList=DBUtils.getAllNormalLight()
                  helper.setImageResource(R.id.device_image,R.drawable.icon_light_device)
                  helper.setText(R.id.device_amount,TelinkLightApplication.getInstance().getString(R.string.number)+"："+lightList.size.toString())
                  helper.setText(R.id.txt_name, name)
              }else if(name==TelinkLightApplication.getInstance().getString(R.string.rgb_light)){
                  var lightList=DBUtils.getAllRGBLight()
                  helper.setImageResource(R.id.device_image,R.drawable.icon_rgb_light_device)
                  helper.setText(R.id.device_amount,TelinkLightApplication.getInstance().getString(R.string.number)+"："+lightList.size.toString())
                  helper.setText(R.id.txt_name, name)
              }else if(name==TelinkLightApplication.getInstance().getString(R.string.switch_title)){
                  var lightList=DBUtils.getAllSwitch()
                  helper.setImageResource(R.id.device_image,R.drawable.icon_switch_device)
                  helper.setText(R.id.device_amount,TelinkLightApplication.getInstance().getString(R.string.number)+"："+lightList.size.toString())
                  helper.setText(R.id.txt_name, name)
              }else if(name==TelinkLightApplication.getInstance().getString(R.string.sensor)){
                  var lightList=DBUtils.getAllSensor()
                  helper.setImageResource(R.id.device_image,R.drawable.icon_sensor_device)
                  helper.setText(R.id.device_amount,TelinkLightApplication.getInstance().getString(R.string.number)+"："+lightList.size.toString())
                  helper.setText(R.id.txt_name, name)
              }else if(name==TelinkLightApplication.getInstance().getString(R.string.curtain)){
                  var lightList=DBUtils.getAllCurtains()
                  helper.setImageResource(R.id.device_image,R.drawable.icon_curtain_device)
                  helper.setText(R.id.device_amount,TelinkLightApplication.getInstance().getString(R.string.number)+"："+lightList.size.toString())
                  helper.setText(R.id.txt_name, name)
              }else if(name==TelinkLightApplication.getInstance().getString(R.string.connector)){
                  var lightList=DBUtils.getAllConnctor()
                  helper.setImageResource(R.id.device_image,R.drawable.icon_acceptor_device)
                  helper.setText(R.id.device_amount,TelinkLightApplication.getInstance().getString(R.string.number)+"："+lightList.size.toString())
                  helper.setText(R.id.txt_name, name)
              }
        }
}
