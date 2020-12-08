package com.dadoutek.uled.router

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.bean.DbRouter
import com.dadoutek.uled.group.*
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.*
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.router.bean.TitleBean
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_batch_group_four.*
import kotlinx.android.synthetic.main.activity_device_bind_router.*
import kotlinx.android.synthetic.main.activity_device_bind_router.batch_see_help
import kotlinx.android.synthetic.main.activity_device_bind_router.grouping_completed
import kotlinx.android.synthetic.main.toolbar.toolbar
import kotlinx.android.synthetic.main.toolbar.toolbarTv
import java.util.concurrent.TimeUnit

/**
 * 创建者     ZCL
 * 创建时间   2020/8/13 15:17
 * 描述
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class BindRouterActivity : TelinkBaseActivity() {
    private var disposableIntervalTime: Disposable? = null
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

    private val lightAdapter: BatchLightAdapter = BatchLightAdapter(R.layout.template_batch_small_item, deviceDataLight, true)
    private val curtainAdapter: BatchCurtainAdapter = BatchCurtainAdapter(R.layout.template_batch_small_item, deviceDataCurtain, true)
    private val relayAdapter: BatchRelayAdapter = BatchRelayAdapter(R.layout.template_batch_small_item, deviceDataRelay, true)
    private val swAdapter: BatchSwAdapter = BatchSwAdapter(R.layout.template_batch_small_item, deviceDataSw)
    private val senserAdapter: BatchSensorAdapter = BatchSensorAdapter(R.layout.template_batch_small_item, deviceDataSensor)
    private val blinkList = mutableListOf<Int>()
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
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setNavigationIcon(R.drawable.icon_return)
    }

    override fun onDestroy() {
        super.onDestroy()
        blinkList.clear()
        disposableIntervalTime?.dispose()
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

        lightAdapter.setOnItemClickListener { adapter, _, position ->
            deviceDataLight[position].selected = !deviceDataLight[position].selected
            if (deviceDataLight[position].selected)
                blinkList.add(deviceDataLight[position].meshAddr)
            changeGroupingCompleteState()
            adapter.notifyDataSetChanged()
        }
        curtainAdapter.setOnItemClickListener { adapter, _, position ->
            deviceDataCurtain[position].selected = !deviceDataCurtain[position].selected
            if (deviceDataCurtain[position].selected)
                blinkList.add(deviceDataCurtain[position].meshAddr)
            changeGroupingCompleteState()
            adapter.notifyDataSetChanged()
        }
        relayAdapter.setOnItemClickListener { adapter, _, position ->
            deviceDataRelay[position].selected = !deviceDataRelay[position].selected
            if (deviceDataRelay[position].selected)
                blinkList.add(deviceDataRelay[position].meshAddr)
            changeGroupingCompleteState()
            adapter.notifyDataSetChanged()
        }
        swAdapter.setOnItemClickListener { adapter, _, position ->
            deviceDataSw[position].selected = !deviceDataSw[position].selected
            if (deviceDataSw[position].selected)
                blinkList.add(deviceDataSw[position].meshAddr)
            changeGroupingCompleteState()
            adapter.notifyDataSetChanged()
        }
        senserAdapter.setOnItemClickListener { adapter, _, position ->
            deviceDataSensor[position].selected = !deviceDataSensor[position].selected
            if (deviceDataSensor[position].selected)
                blinkList.add(deviceDataSensor[position].meshAddr)
            changeGroupingCompleteState()
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

        bind_router_all.setOnClickListener {
            isAll = !isAll
            if (isAll)
                bind_router_all.setImageResource(R.drawable.icon_all_checked)
            else
                bind_router_all.setImageResource(R.drawable.icon_all_check)
            setAllSelect(isAll)
        }
    }


    private fun setAllSelect(all: Boolean) {
        blinkList.clear()
        when ((currentGroup?.deviceType ?: 0).toInt()) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                deviceDataLight.forEach {
                    it.selected = all
                    if (all)
                        blinkList.add(it.meshAddr)
                }

                lightAdapter.notifyDataSetChanged()
            }
            DeviceType.SMART_CURTAIN -> {
                deviceDataCurtain.forEach {
                    it.selected = all
                    if (all)
                        blinkList.add(it.meshAddr)
                }
                curtainAdapter.notifyDataSetChanged()
            }
            DeviceType.SMART_RELAY -> {
                deviceDataRelay.forEach {
                    it.selected = all
                    if (all)
                        blinkList.add(it.meshAddr)
                }
                relayAdapter.notifyDataSetChanged()
            }
            DeviceType.NORMAL_SWITCH -> {
                deviceDataSw.forEach {
                    it.selected = all
                    if (all)
                        blinkList.add(it.meshAddr)
                }
                swAdapter.notifyDataSetChanged()
            }
            DeviceType.SENSOR -> {
                deviceDataSensor.forEach {
                    it.selected = all
                    if (all)
                        blinkList.add(it.meshAddr)
                }
                senserAdapter.notifyDataSetChanged()
            }
        }

        changeGroupingCompleteState()
    }

    private fun changeGroupSelectView(position: Int) {
        if (lastCheckedGroupPostion != position && lastCheckedGroupPostion != 1000) {
            routerList[lastCheckedGroupPostion].isSelect = false
            routerAdapter.notifyItemRangeChanged(lastCheckedGroupPostion, 1)
        }
        lastCheckedGroupPostion = position
        routerList[position].select = !routerList[position].select
        changeGroupingCompleteState()
        routerAdapter.notifyDataSetChanged()
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
            meshAddList.isEmpty() -> ToastUtils.showShort(getString(R.string.please_select_device_at_leaset))
            routerList.none { it.isSelect } -> ToastUtils.showShort(getString(R.string.please_select_router))
            else -> bindRouterHttp(meshAddList)
        }
    }

    @SuppressLint("CheckResult")
    private fun bindRouterHttp(meshAddList: MutableList<Int>) {
        showLoadingDialog(getString(R.string.binding_router))
        /**
         * meshType 普通灯 = 4 彩灯 = 6 蓝牙连接器 = 5  窗帘 = 16 开关 = 99 或 0x20 或 0x22 或 0x21 或 0x28 或 0x27 或 0x25 开关统一使用99
         * 传感器 = 98 或 0x23 或 0x24
         */
        var meshType = when ((currentGroup?.deviceType ?: 0).toInt()) {
            DeviceType.LIGHT_NORMAL -> 4
            DeviceType.LIGHT_RGB -> 6
            DeviceType.SMART_CURTAIN -> 16
            DeviceType.SMART_RELAY -> 5
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.EIGHT_SWITCH, DeviceType.DOUBLE_SWITCH, DeviceType.SCENE_SWITCH,
            DeviceType.SMART_CURTAIN_SWITCH -> 99
            DeviceType.SENSOR -> 98
            else -> 4
        }
        blinkList.clear()
        RouterModel.bindRouter(meshAddList, meshType, routerList.filter { it.isSelect }[0].macAddr)?.subscribe({
            SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, object : SyncCallback {
                override fun start() {}
                override fun complete() {
                    ToastUtils.showShort(getString(R.string.bind_success))
                    changeDataUpdate()
                    hideLoadingDialog()
                    bind_router_all.setImageResource(R.drawable.icon_all_check)
                }

                override fun error(msg: String?) {
                    ToastUtils.showShort(getString(R.string.bind_success))
                    changeDataUpdate()
                    hideLoadingDialog()
                }
            })
            hideLoadingDialog()
        }, {
            ToastUtils.showShort(getString(R.string.bind_fail) + it.message)
            hideLoadingDialog()
        })
    }

    private fun changeGroupingCompleteState() {
        var selectSize = when (currentGroup?.deviceType?.toInt()) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB -> deviceDataLight.filter { it.isSelected }.size
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SMART_CURTAIN_SWITCH,
            DeviceType.EIGHT_SWITCH, DeviceType.SCENE_SWITCH, DeviceType.DOUBLE_SWITCH -> deviceDataSw.filter { it.isSelected }.size
            DeviceType.SENSOR -> deviceDataSensor.filter { it.isSelected }.size
            DeviceType.SMART_CURTAIN -> deviceDataCurtain.filter { it.isSelected }.size
            DeviceType.SMART_RELAY -> deviceDataRelay.filter { it.isSelected }.size
            else -> deviceDataLight.filter { it.isSelected }.size
        }
        if (selectSize > 0 && routerList.any { it.isSelect }) {//选中分组并且有选中设备的情况下
            grouping_completed.setBackgroundResource(R.drawable.rect_blue_5)
            grouping_completed.isClickable = true
        } else {//没有选中设备或者未选中分组的情况下
            grouping_completed.isClickable = false
            grouping_completed.setBackgroundResource(R.drawable.rect_solid_radius5_e)
        }
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
                when {
                    currentGroup?.brightness != 10000 -> deviceDataLight.addAll(DBUtils.getLightByGroupID(currentGroup?.id ?: 0))
                    else -> {
                        if (currentGroup?.deviceType?.toInt() == DeviceType.LIGHT_RGB)
                            deviceDataLight.addAll(DBUtils.getAllRGBLight())
                        else
                            deviceDataLight.addAll(DBUtils.getAllNormalLight())
                    }
                }
                lightAdapter.notifyDataSetChanged()
            }
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SMART_CURTAIN_SWITCH,
            DeviceType.EIGHT_SWITCH, DeviceType.SCENE_SWITCH, DeviceType.DOUBLE_SWITCH -> {
                deviceDataSw.clear()
                deviceDataSw.addAll(DBUtils.getAllSwitch())
                swAdapter.notifyDataSetChanged()
            }
            DeviceType.SENSOR -> {
                deviceDataSensor.clear()
                deviceDataSensor.addAll(DBUtils.getAllSensor())
                senserAdapter.notifyDataSetChanged()
            }
            DeviceType.SMART_CURTAIN -> {
                deviceDataCurtain.clear()
                if (currentGroup?.brightness != 10000)
                    deviceDataCurtain.addAll(DBUtils.getCurtainByGroupID(currentGroup?.id ?: 0))
                else
                    deviceDataCurtain.addAll(DBUtils.getAllCurtains())
                curtainAdapter.notifyDataSetChanged()
            }
            DeviceType.SMART_RELAY -> {
                deviceDataRelay.clear()
                if (currentGroup?.brightness != 10000)
                    deviceDataRelay.addAll(DBUtils.getRelayByGroupID(currentGroup?.id ?: 0))
                else
                    deviceDataRelay.addAll(DBUtils.getAllRelay())
                relayAdapter.notifyDataSetChanged()
            }
        }
        routerList.forEach { it.isSelect = false }
        routerAdapter.notifyDataSetChanged()
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
        bind_router_recycle.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
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

        if (Constant.IS_ROUTE_MODE) {
            disposableIntervalTime?.dispose()
            disposableIntervalTime = Observable.interval(0, 3000, TimeUnit.MILLISECONDS)
                    .subscribe {
                        //   LogUtils.v("zcl-----------收到理由闪烁-------$blinkList---------${blinkList.size}")
                        if (blinkList.size > 0) {
                            RouterModel.routeBatchGpBlink(GroupBlinkBodyBean(blinkList, (currentGroup?.deviceType
                                    ?: 0).toInt()))?.subscribe({
                                // LogUtils.v("zcl----------收到理由闪烁--------成功")
                            }, {
                                //  LogUtils.v("zcl----------收到理由闪烁--------失败")
                            })
                        }
                    }
        }
    }
}