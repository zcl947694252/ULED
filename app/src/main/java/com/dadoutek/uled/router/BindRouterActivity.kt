package com.dadoutek.uled.router

import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.bean.DbRouter
import com.dadoutek.uled.group.*
import com.dadoutek.uled.model.dbModel.*
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.router.bean.TitleBean
import kotlinx.android.synthetic.main.activity_device_bind_router.*
import kotlinx.android.synthetic.main.toolbar.*

/**
 * 创建者     ZCL
 * 创建时间   2020/8/13 15:17
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class BindRouterActivity : TelinkBaseActivity() {
    private var lastCheckedGroupPostion: Int = 0
    private var currentPostion: Int = 0
    private var listTitle: MutableList<TitleBean> = mutableListOf()
    private val titleAdapter = TitleAdapter(R.layout.item_title, listTitle)
    private val routerList = RxDeviceList<DbRouter>()
    private val routerAdapter: BatchRouterAdapter = BatchRouterAdapter(R.layout.template_batch_small_item, routerList)

    private val deviceDataLightGroup = RxDeviceList<DbGroup>()
    private val deviceDataSw: MutableList<DbSwitch> = mutableListOf()
    private val deviceDataSensor: MutableList<DbSensor> = mutableListOf()

    private val groupAdapter: BatchGrouopEditListAdapter = BatchGrouopEditListAdapter(R.layout.template_batch_small_item, deviceDataLightGroup, true)
    private val swAdapter: BatchSwAdapter = BatchSwAdapter(R.layout.template_batch_small_item, deviceDataSw)
    private val senserAdapter: BatchSensorAdapter = BatchSensorAdapter(R.layout.template_batch_small_item, deviceDataSensor)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_bind_router)

        initToolbar()
        initView()
        initData()
        initListener()
    }

    private fun initToolbar() {
        toolbarTv.text = getString(R.string.bind_reouter)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setNavigationIcon(R.drawable.icon_return)
    }

    private fun initListener() {
        titleAdapter.setOnItemClickListener { adapter, _, position ->
            for (i in listTitle.indices) {
                listTitle[i].isChexked = i == position
                if (i == position)
                    currentPostion = position
            }
            changeDataUpdate(position)
            adapter.notifyDataSetChanged()
        }

        routerAdapter.setOnItemClickListener { _, view, position ->
            routerList.forEach { it.isChecked = false }
            routerList[position].isChecked = true
        }

        groupAdapter.setOnItemClickListener { adapter, _, position ->
            deviceDataLightGroup[position].isChecked = !deviceDataLightGroup[position].isChecked
            adapter.notifyDataSetChanged()
        }
        swAdapter.setOnItemClickListener { adapter, _, position ->
            deviceDataSw.forEach {
                it.isChecked = false
            }
            deviceDataSw[position].isChecked = !deviceDataSw[position].isChecked
            adapter.notifyDataSetChanged()
        }
        senserAdapter.setOnItemClickListener { adapter, _, position ->
            deviceDataSensor.forEach {
                it.isChecked = false
            }
            deviceDataSensor[position].isChecked = !deviceDataSensor[position].isChecked

            adapter.notifyDataSetChanged()
        }
        batch_see_help.setOnClickListener { seeHelpe("#prepare") }
        grouping_completed.setOnClickListener {
            when (currentPostion) {
                0, 1, 4, 5 -> groupBindRouter()
                2 -> switchBindRouter()
                3 -> sensorBindRouter()
            }
        }
    }

    private fun sensorBindRouter() {
        val sensor = deviceDataSensor.filter { it.isChecked }
        val router = routerList.filter { it.isChecked }
        val meshAddList = mutableListOf<Int>()
        sensor.forEach { meshAddList.add(it.meshAddr) }
        when {
            sensor.isEmpty() -> ToastUtils.showShort(getString(R.string.please_select_sensor_at_leaset))
            router.isEmpty() -> ToastUtils.showShort(getString(R.string.please_select_router))
            else -> {
                //进行绑定配置 开关统一使用99
                RouterModel.bindRouter(meshAddList, 99, router[0].macAddr)?.subscribe({
                    ToastUtils.showShort(it.message)
                    resetGroupData()
                    resetRouterData()
                }, {
                    ToastUtils.showShort(it.message)
                })
            }
        }
    }

    private fun switchBindRouter() {
        val sw = deviceDataSw.filter { it.isChecked }
        val router = routerList.filter { it.isChecked }
        val meshAddList = mutableListOf<Int>()
        sw.forEach { meshAddList.add(it.meshAddr) }
        when {
            sw.isEmpty() -> ToastUtils.showShort(getString(R.string.please_select_sw_at_leaset))
            router.isEmpty() -> ToastUtils.showShort(getString(R.string.please_select_router))
            else -> {
                //进行绑定配置 开关统一使用99
                RouterModel.bindRouter(meshAddList, 99, router[0].macAddr)?.subscribe({
                    ToastUtils.showShort(it.message)
                    resetGroupData()
                    resetRouterData()
                }, {
                    ToastUtils.showShort(it.message)
                })
            }
        }
    }

    private fun groupBindRouter() {
        val group = deviceDataLightGroup.filter { it.isChecked }
        val router = routerList.filter { it.isChecked }
        val meshAddList = mutableListOf<Int>()
        group.forEach { meshAddList.add(it.meshAddr) }
        when {
            group.isEmpty() -> ToastUtils.showShort(getString(R.string.please_select_group))
            router.isEmpty() -> ToastUtils.showShort(getString(R.string.please_select_router))
            else -> {
                //进行绑定配置
                var deviceType = when (currentPostion) {
                    0 -> DeviceType.LIGHT_NORMAL
                    1 -> DeviceType.LIGHT_RGB
                    4 -> DeviceType.SMART_CURTAIN
                    5 -> DeviceType.SMART_RELAY
                    else -> DeviceType.LIGHT_NORMAL
                }

                RouterModel.bindRouter(meshAddList, deviceType, router[0].macAddr)?.subscribe({
                    ToastUtils.showShort(it.message)
                    resetGroupData()
                    resetRouterData()
                }, {
                    ToastUtils.showShort(it.message)
                })
            }
        }
    }

    private fun resetGroupData() {
        deviceDataLightGroup.forEach { it.isChecked = false }
        groupAdapter.notifyDataSetChanged()
    }

    private fun resetRouterData() {
        routerList.forEach { it.isChecked = false }
        routerAdapter.notifyDataSetChanged()
    }

    private fun changeDataUpdate(position: Int) {
        bind_router_recycle.layoutManager = GridLayoutManager(this, 4)
        bind_router_recycle.itemAnimator = DefaultItemAnimator()
        when (position) {
            0, 1, 4, 5 -> {
                deviceDataLightGroup.clear()
                when (position) {
                    0 -> {
                        deviceDataLightGroup.addAll(DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_NORMAL))
                        deviceDataLightGroup.addAll(DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_NORMAL_OLD))
                        deviceDataLightGroup.forEach { it.isChecked = false }
                    }
                    1 -> {
                        deviceDataLightGroup.addAll(DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_RGB))
                        deviceDataLightGroup.forEach { it.isChecked = false }
                    }
                    4 -> {
                        deviceDataLightGroup.addAll(DBUtils.getGroupsByDeviceType(DeviceType.SMART_CURTAIN))
                        deviceDataLightGroup.forEach { it.isChecked = false }
                    }
                    5 -> {
                        deviceDataLightGroup.addAll(DBUtils.getGroupsByDeviceType(DeviceType.SMART_RELAY))
                        deviceDataLightGroup.forEach { it.isChecked = false }
                    }
                }
                groupAdapter.bindToRecyclerView(bind_router_recycle)
            }
            2 -> {
                deviceDataSw.clear()
                deviceDataSw.addAll(DBUtils.getAllSwitch())
                deviceDataSw.forEach { it.isChecked = false }
                swAdapter.bindToRecyclerView(bind_router_recycle)
            }
            3 -> {
                deviceDataSensor.clear()
                deviceDataSensor.addAll(DBUtils.getAllSensor())
                deviceDataSensor.forEach { it.isChecked = false }
                senserAdapter.bindToRecyclerView(bind_router_recycle)
            }
        }
    }

    private fun initData() {
        listTitle.clear()
        listTitle.addAll(mutableListOf(TitleBean(getString(R.string.normal_light), true), TitleBean(getString(R.string.rgb_light),
                false), TitleBean(getString(R.string.switch_title), false), TitleBean(getString(R.string.sensor), false),
                TitleBean(getString(R.string.curtain), false), TitleBean(getString(R.string.relay), false)))
        titleAdapter.notifyDataSetChanged()
        changeDataUpdate(0)

        routerList.clear()
        routerList.addAll(DBUtils.getAllRouter())
        routerList.forEach { it.isChecked = false }
        routerAdapter.bindToRecyclerView(batch_router_recycle)
    }

    private fun initView() {
        bind_router_type.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        bind_router_type.adapter = titleAdapter
        titleAdapter.bindToRecyclerView(bind_router_type)
    }
}