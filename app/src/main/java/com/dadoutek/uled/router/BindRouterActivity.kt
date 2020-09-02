package com.dadoutek.uled.router

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.widget.LinearLayout
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.bean.DbRouter
import com.dadoutek.uled.group.*
import com.dadoutek.uled.model.dbModel.*
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.router.bean.TitleBean
import kotlinx.android.synthetic.main.activity_batch_group_four.*
import kotlinx.android.synthetic.main.activity_device_bind_router.*
import kotlinx.android.synthetic.main.activity_device_bind_router.batch_see_help
import kotlinx.android.synthetic.main.activity_device_bind_router.grouping_completed
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.toolbar
import kotlinx.android.synthetic.main.toolbar.toolbarTv

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
    private var lastCheckedGroupPostion: Int = 1000
    private var isAll: Boolean = false
    private var currentGroup: DbGroup? = null
    private var currentPostion: Int = 0
    private var listTitle: MutableList<TitleBean> = mutableListOf()
    private val titleAdapter = TitleAdapter(R.layout.item_title, listTitle)
    private val routerList = RxDeviceList<DbRouter>()
    private val routerAdapter: BatchRouterAdapter = BatchRouterAdapter(R.layout.template_batch_small_item, routerList)

    private val deviceDataLight: MutableList<DbLight> = mutableListOf()
    private val deviceDataCurtain: MutableList<DbCurtain> = mutableListOf()
    private val deviceDataRelay: MutableList<DbConnector> = mutableListOf()
    private val deviceDataSw: MutableList<DbSwitch> = mutableListOf()
    private val deviceDataSensor: MutableList<DbSensor> = mutableListOf()

    private val lightAdapter: BatchLightAdapter = BatchLightAdapter(R.layout.template_batch_small_item, deviceDataLight)
    private val curtainAdapter: BatchCurtainAdapter = BatchCurtainAdapter(R.layout.template_batch_small_item, deviceDataCurtain)
    private val relayAdapter: BatchRelayAdapter = BatchRelayAdapter(R.layout.template_batch_small_item, deviceDataRelay)
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
            changeDataUpdate()
            adapter.notifyDataSetChanged()
        }

        routerAdapter.setOnItemClickListener { _, _, position ->
            routerList.forEach { it.isSelect = false }
            routerList[position].isSelect = true
        }

        lightAdapter.setOnItemClickListener { adapter, _, position ->
            deviceDataLight.forEach {
                it.selected = false
            }
            deviceDataLight[position].selected = !deviceDataLight[position].selected
            adapter.notifyDataSetChanged()
        }
        curtainAdapter.setOnItemClickListener { adapter, _, position ->
            deviceDataCurtain.forEach {
                it.selected = false
            }
            deviceDataCurtain[position].selected = !deviceDataCurtain[position].selected
            adapter.notifyDataSetChanged()
        }
        relayAdapter.setOnItemClickListener { adapter, _, position ->
            deviceDataRelay.forEach {
                it.selected = false
            }
            deviceDataRelay[position].selected = !deviceDataRelay[position].selected
            adapter.notifyDataSetChanged()
        }
        swAdapter.setOnItemClickListener { adapter, _, position ->
            deviceDataSw.forEach {
                it.selected = false
            }
            deviceDataSw[position].selected = !deviceDataSw[position].selected
            adapter.notifyDataSetChanged()
        }
        senserAdapter.setOnItemClickListener { adapter, _, position ->
            deviceDataSensor.forEach {
                it.selected = false
            }
            deviceDataSensor[position].selected = !deviceDataSensor[position].selected
            adapter.notifyDataSetChanged()
        }
        batch_see_help.setOnClickListener { seeHelpe("#prepare") }
        grouping_completed.setOnClickListener {
            when ((currentGroup?.deviceType ?: 0).toInt()) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> lightBindRouter()
                DeviceType.SMART_CURTAIN -> curtainBindRouter()
                DeviceType.SMART_RELAY -> relayBindRouter()
                DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2 -> switchBindRouter()
                DeviceType.SENSOR -> sensorBindRouter()
            }
        }

        routerAdapter.setOnItemClickListener { _, _, position ->
            //如果点中的不是上次选中的组则恢复未选中状态
            changeGroupSelectView(position)
        }

        batch_four_device_all.setOnClickListener { changeDeviceAll() }
    }

    /**
     * 全选与取消功能相关
     */
    private fun changeDeviceAll() {
        isAll = !isAll
        if (isAll)
            batch_four_device_all.setImageResource(R.drawable.icon_all_checked)
        else
            batch_four_device_all.setImageResource(R.drawable.icon_all_check)
        setAllSelect(isAll)
    }

    private fun setAllSelect(all: Boolean) {
        when ((currentGroup?.deviceType?:0).toInt()) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
            }
            DeviceType.SMART_CURTAIN -> {
            }
            DeviceType.SMART_RELAY -> {
                deviceDataRelay.forEach {
                    it.isSelected = all
                }
                swAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun changeGroupSelectView(position: Int) {
        if (lastCheckedGroupPostion != position && lastCheckedGroupPostion != 1000) {
            routerList[lastCheckedGroupPostion].isSelect = false
            routerAdapter.notifyItemRangeChanged(lastCheckedGroupPostion, 1)
        }
        lastCheckedGroupPostion = position
        routerList[position].isSelect = !routerList[position].select
    }

    private fun relayBindRouter() {
        val relay = deviceDataRelay.filter { it.selected }
        val meshAddList = mutableListOf<Int>()
        relay.forEach { meshAddList.add(it.meshAddr) }
        bindFilter(meshAddList)
    }

    private fun curtainBindRouter() {
        val curtain = deviceDataCurtain.filter { it.selected }
        val meshAddList = mutableListOf<Int>()
        curtain.forEach { meshAddList.add(it.meshAddr) }
        bindFilter(meshAddList)
    }

    private fun lightBindRouter() {
        val light = deviceDataLight.filter { it.selected }
        val meshAddList = mutableListOf<Int>()
        light.forEach { meshAddList.add(it.meshAddr) }
        bindFilter(meshAddList)
    }

    private fun sensorBindRouter() {
        val sensor = deviceDataSensor.filter { it.selected }
        val meshAddList = mutableListOf<Int>()
        sensor.forEach { meshAddList.add(it.meshAddr) }
        bindFilter(meshAddList)
    }

    private fun switchBindRouter() {
        val sw = deviceDataSw.filter { it.selected }
        val meshAddList = mutableListOf<Int>()
        sw.forEach { meshAddList.add(it.meshAddr) }
        bindFilter(meshAddList)
    }

    private fun bindFilter(meshAddList: MutableList<Int>) {
        when {
            meshAddList.isEmpty() -> ToastUtils.showShort(getString(R.string.please_select_sensor_at_leaset))
            routerList.none { it.isSelect } -> ToastUtils.showShort(getString(R.string.please_select_router))
            else -> bindRouterHttp(meshAddList)
        }
    }

    @SuppressLint("CheckResult")
    private fun bindRouterHttp(meshAddList: MutableList<Int>) {
        /**
         * meshType 普通灯 = 4
         * 彩灯 = 6 蓝牙连接器 = 5  窗帘 = 16 开关 = 99 或 0x20 或 0x22 或 0x21 或 0x28 或 0x27 或 0x25 开关统一使用99
         * 传感器 = 98 或 0x23 或 0x24
         */
        var meshType = when ((currentGroup?.deviceType ?: 0).toInt()) {
            DeviceType.LIGHT_NORMAL -> 4
            DeviceType.LIGHT_RGB -> 6
            DeviceType.SMART_CURTAIN -> 16
            DeviceType.SMART_RELAY -> 5
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2,DeviceType.EIGHT_SWITCH,DeviceType.DOUBLE_SWITCH,DeviceType.SCENE_SWITCH,
            DeviceType.SMART_CURTAIN_SWITCH-> 99
            DeviceType.SENSOR -> 98
            else -> 4
        }
        RouterModel.bindRouter(meshAddList, meshType, routerList.filter { it.isSelect }[0].macAddr)?.subscribe({
            ToastUtils.showShort(it.message)
            resetRouterData()
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    private fun resetRouterData() {
        routerList.forEach { it.isSelect = false }
        routerAdapter.notifyDataSetChanged()
    }

    private fun changeDataUpdate() {
        /*    when (position) {
                0, 1, 4, 5 -> {}
                2 -> {
                    deviceDataSw.clear()
                    deviceDataSw.addAll(DBUtils.getGroupByID())
                    deviceDataSw.forEach { it.isChecked = false }
                    swAdapter.bindToRecyclerView(bind_device_recycle)
                }
                3 -> {
                    deviceDataSensor.clear()
                    deviceDataSensor.addAll(DBUtils.getAllSensor())
                    deviceDataSensor.forEach { it.isChecked = false }
                    senserAdapter.bindToRecyclerView(bind_device_recycle)
                }
            }*/
        when (currentGroup?.deviceType?.toInt()) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB -> {
                deviceDataLight.clear()
                deviceDataLight.addAll(DBUtils.getLightByGroupID(currentGroup?.id ?: 0))
                lightAdapter.notifyDataSetChanged()
            }
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SMART_CURTAIN_SWITCH,
            DeviceType.EIGHT_SWITCH, DeviceType.SCENE_SWITCH, DeviceType.DOUBLE_SWITCH -> {
            }
            DeviceType.SENSOR -> {
            }
            DeviceType.SMART_CURTAIN -> {
                deviceDataCurtain.clear()
                deviceDataCurtain.addAll(DBUtils.getCurtainByGroupID(currentGroup?.id ?: 0))
                curtainAdapter.notifyDataSetChanged()
            }
            DeviceType.SMART_RELAY -> {
                deviceDataRelay.clear()
                deviceDataRelay.addAll(DBUtils.getRelayByGroupID(currentGroup?.id ?: 0))
                relayAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun initData() {
        intent.extras?.get("group")?.let {
            currentGroup = it as DbGroup
        }

        bind_device_recycle.layoutManager = GridLayoutManager(this, 4)
        bind_device_recycle.itemAnimator = DefaultItemAnimator()
        when (currentGroup?.deviceType?.toInt()) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB -> lightAdapter.bindToRecyclerView(bind_device_recycle)
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SMART_CURTAIN_SWITCH,
            DeviceType.EIGHT_SWITCH, DeviceType.SCENE_SWITCH, DeviceType.DOUBLE_SWITCH -> swAdapter.bindToRecyclerView(bind_device_recycle)
            DeviceType.SENSOR -> senserAdapter.bindToRecyclerView(bind_device_recycle)
            DeviceType.SMART_CURTAIN -> curtainAdapter.bindToRecyclerView(bind_device_recycle)
            DeviceType.SMART_RELAY -> relayAdapter.bindToRecyclerView(bind_device_recycle)
        }

        changeDataUpdate()

        routerList.clear()
        routerList.addAll(DBUtils.getAllRouter())
        routerList.forEach { it.isSelect = false }
        bind_router_recycle.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false)
        routerAdapter.bindToRecyclerView(bind_router_recycle)
    }

    private fun initView() {
        bind_router_type.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        bind_router_type.adapter = titleAdapter
        listTitle.clear()
       listTitle.addAll(mutableListOf(TitleBean(getString(R.string.normal_light), true), TitleBean(getString(R.string.rgb_light),
               false), TitleBean(getString(R.string.switch_title), false), TitleBean(getString(R.string.sensor), false),
               TitleBean(getString(R.string.curtain), false), TitleBean(getString(R.string.relay), false)))
        titleAdapter.bindToRecyclerView(bind_router_type)
    }
}