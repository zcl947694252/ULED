package com.dadoutek.uled.light

import android.hardware.Sensor
import android.hardware.usb.UsbDevice.getDeviceName
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import butterknife.ButterKnife
import com.dadoutek.uled.R
import com.dadoutek.uled.R.string.grouping
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.telink.util.Event
import com.telink.util.EventListener
import kotlinx.android.synthetic.main.activity_config_light_light.*
import java.util.ArrayList

/**
 * Created by hejiajun on 2018/4/24.
 */

class DeviceDetailAct : TelinkBaseActivity(), EventListener<String>{

    private var type:Int?=null

    private var nowLightList: MutableList<DbLight>? = null

    private var lightsData: ArrayList<DbLight>? = null

//    private var curtainData:MutableList<DbCurtain>?=null

    private var sensorData:ArrayList<DbSensor>?=null

    private var switchData:ArrayList<DbSwitch>?=null

    private var curtainList:ArrayList<DbCurtain>?=null

    private var listView:ListView?=null

    private var inflater:LayoutInflater?=null

    private var allLightId: Long = 0

    private var adapter: DeviceListAdapter? = null

    override fun performed(event: Event<String>?) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_detail)
        type=this.intent.getIntExtra(Constant.DEVICE_TYPE,0)
        inflater=this.layoutInflater
        initDate()
        initView()
    }

    private fun initView() {
        listView=findViewById<ListView>(R.id.list)
        this.adapter = DeviceListAdapter()

        this.listView!!.adapter = this.adapter
        adapter!!.setLights(this!!.lightsData!!)
        adapter!!.notifyDataSetChanged()
    }

    private fun initDate() {
        when (type) {
            Constant.INSTALL_NORMAL_LIGHT -> {
                lightsData = DBUtils.getAllNormalLight()
//                nowLightList!!.addAll(lightsData!!)
            }
            Constant.INSTALL_RGB_LIGHT -> {
                lightsData=DBUtils.getAllRGBLight()
//                nowLightList!!.addAll(lightsData!!)
            }
            Constant.INSTALL_SWITCH -> {
                switchData=DBUtils.getAllSwitch()
//                nowLightList!!.addAll(lightsData!!)
            }
            Constant.INSTALL_SENSOR -> {
                sensorData=DBUtils.getAllSensor()
//                nowLightList!!.addAll(lightsData!!)
            }
//            Constant.INSTALL_CURTAIN -> {
//                curtainData=DBUtils.getAllCurtain()
////                curtainList!!.addAll(curtainData!!)
//            }
        }
    }


    internal inner class DeviceListAdapter : BaseAdapter() {

        private var lights: MutableList<DbLight>? = null

        private var switchs:MutableList<DbSwitch>?=null

        private var sensors:MutableList<DbSensor>?=null


        override fun getCount(): Int {
            return if (this.lights == null) 0 else this.lights!!.size
        }

        override fun getItem(position: Int): DbLight {
            return this.lights!![position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            var convertView = convertView

            val holder: DeviceItemHolder = DeviceItemHolder()

            convertView = inflater!!.inflate(R.layout.device_detail_adapter, null)
            val icon = convertView
                    .findViewById<View>(R.id.img_icon) as ImageView
            val txtName = convertView
            .findViewById<View>(R.id.name) as TextView

            holder.icon = icon
            holder.txtName = txtName

            convertView.tag = holder


            val light = this.getItem(position)

            if (light.belongGroupId != 1L) {
                holder.txtName!!.text = DBUtils.getGroupByID(light.belongGroupId!!)!!.name
            } else {
                holder.txtName!!.setText(R.string.not_grouped)
            }

            holder.icon!!.setImageResource(R.drawable.icon_light_on)

            if (light.hasGroup) {
                holder.txtName!!.setText(getDeviceName(light))
                holder.icon!!.visibility = View.VISIBLE
            } else {
                holder.txtName!!.visibility = View.VISIBLE
                holder.icon!!.visibility = View.VISIBLE
            }

            return convertView
        }

        fun add(light: DbLight) {

            if (this.lights == null)
                this.lights = ArrayList()
            DBUtils.saveLight(light, false)
            this.lights!!.add(light)
        }

        operator fun get(meshAddress: Int): DbLight? {

            if (this.lights == null)
                return null

            for (light in this.lights!!) {
                if (light.meshAddr == meshAddress) {
                    return light
                }
            }

            return null
        }

        fun getLights(): List<DbLight>? {
            return lights
        }

        fun setLights(lights: MutableList<DbLight>) {
            this.lights = lights
        }
    }

    private class DeviceItemHolder {
        var icon: ImageView? = null
        var txtName: TextView? = null
    }

    private fun getDeviceName(light: DbLight): String {
        return if (light.belongGroupId != allLightId) {
            DBUtils.getGroupNameByID(light.belongGroupId)
        } else {
            getString(R.string.not_grouped)
        }
    }

}

