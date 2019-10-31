package com.dadoutek.uled.group

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.app.hubert.guide.util.ScreenUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.BleUtils
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.dadoutek.uled.widget.RecyclerGridDecoration
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_batch_group_four.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2019/10/16 14:41
 * 描述
 *
 * 更新者     zcl$
 * 更新时间   用于冷暖灯,彩灯,窗帘控制器的批量分组$
 * 更新描述
 */
class BatchGroupFourDevice : TelinkBaseActivity(), EventListener<String> {

    private var relayAdapter: BatchFourRelayAdapter? = null
    private var curtainAdapter: BatchFourCurtainAdapter? = null
    private var disposable: Disposable? = null
    private var currentGroup: DbGroup? = null
    private var btnAddGroups: TextView? = null
    private var emptyGroupView: View? = null
    private var mTelinkLightService: TelinkLightService? = null
    private var groupAdapter: BatchGrouopEditListAdapter? = null
    private var lightAdapter: BatchFourLightAdapter? = null
    private lateinit var deviceListRelay: ArrayList<DbConnector>
    private lateinit var deviceListCurtains: ArrayList<DbCurtain>
    private lateinit var deviceListLight: ArrayList<DbLight>
    private var groupsByDeviceType: MutableList<DbGroup>? = null
    private var deviceType: Int = 100
    private var lastCheckedGroupPostion: Int = 0
    private var allLightId: Long = 0//有设备等于0说明没有分组成功
    private var retryConnectCount = 0
    private var connectMeshAddress: Int = 0
    private var mApplication: TelinkLightApplication? = null

    private var noGroup: MutableList<DbLight>? = null
    private var listGroup: MutableList<DbLight>? = null
    private var deviceData: MutableList<DbLight>? = null

    private var noGroupCutain: MutableList<DbCurtain>? = null
    private var listGroupCutain: MutableList<DbCurtain>? = null
    private var deviceDataCurtain: MutableList<DbCurtain>? = null

    private var noGroupRelay: MutableList<DbConnector>? = null
    private var listGroupRelay: MutableList<DbConnector>? = null
    private var deviceDataRelay: MutableList<DbConnector>? = null

    private var isHaveGrouped: Boolean = false
    //获取当前勾选灯的列表
    private var selectLights = mutableListOf<DbLight>()
    private var selectCurtains = mutableListOf<DbCurtain>()
    private var selectRelays = mutableListOf<DbConnector>()
    private val mBlinkDisposables = SparseArray<Disposable>()
    private var isAddGroupEmptyView: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_group_four)
        this.mApplication = this.application as TelinkLightApplication
        initView()
        initData()
        initListener()
    }

    private fun initView() {
        toolbarTv.text = getString(R.string.batch_group)
        image_bluetooth.visibility = View.VISIBLE
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            checkNetworkAndSync(this)
            finish() }

        if (TelinkApplication.getInstance().connectDevice == null)
            image_bluetooth.setImageResource(R.drawable.bluetooth_no)
        else
            image_bluetooth.setImageResource(R.drawable.icon_bluetooth)

        emptyGroupView = LayoutInflater.from(this).inflate(R.layout.empty_group_view, null)
        btnAddGroups = emptyGroupView!!.findViewById<TextView>(R.id.add_groups_btn)

        batch_four_device_recycle.layoutManager = GridLayoutManager(this, 2)
        batch_four_device_recycle.addItemDecoration(RecyclerGridDecoration(this, 2))


        batch_four_group_recycle.layoutManager = GridLayoutManager(this, 2)
        batch_four_group_recycle.addItemDecoration(RecyclerGridDecoration(this, 2))

        autoConnect()
    }

    private fun initData() {
        deviceType = intent.getIntExtra(Constant.DEVICE_TYPE, 100)
        setDevicesData(deviceType, isHaveGrouped)
        setGroupData(deviceType)
    }

    /**
     * 设置组数据
     */
    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    private fun setGroupData(deviceType: Int) {
        ClearSelectors()
        groupsByDeviceType = DBUtils.getGroupsByDeviceType(deviceType)
        if (groupsByDeviceType == null) {
            groupsByDeviceType = mutableListOf()
        } else if (groupsByDeviceType!!.size > 0) {
            for (index in groupsByDeviceType!!.indices)
                groupsByDeviceType!![index].isChecked = index == 0
        }
        batch_four_group_title.text = getString(R.string.grouped_num, groupsByDeviceType?.size)

        groupAdapter = BatchGrouopEditListAdapter(R.layout.batch_four_group_edit_item, groupsByDeviceType!!)
        if (!isAddGroupEmptyView)
            groupAdapter?.emptyView = emptyGroupView

        isAddGroupEmptyView = true
        groupAdapter?.bindToRecyclerView(batch_four_group_recycle)
        if (groupsByDeviceType!!.size > 0) {
            for (index in groupsByDeviceType!!.indices) {
                val isSelect = index == 0
                groupsByDeviceType!![index].isChecked = isSelect
                if (isSelect) {
                    lastCheckedGroupPostion = 0
                    currentGroup = groupsByDeviceType!![index]
                }
            }
        }

        groupAdapter?.setOnItemClickListener { _, _, position ->
            //如果点中的不是上次选中的组则恢复未选中状态

            changeGroupSelectView(position)
        }

    }

    private fun ClearSelectors() {
        selectLights.clear()
        selectCurtains.clear()
        selectRelays.clear()
    }

    /**
     * 设置设备数据
     */
    @SuppressLint("StringFormatInvalid")
    private fun setDevicesData(deviceType: Int, isHaveGrouped: Boolean) {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL -> {
                noGroup = mutableListOf()
                listGroup = mutableListOf()
                deviceListLight = DBUtils.getAllNormalLight()
            }
            DeviceType.LIGHT_RGB -> {
                noGroup = mutableListOf()
                listGroup = mutableListOf()
                deviceListLight = DBUtils.getAllRGBLight()
            }
            DeviceType.SMART_CURTAIN -> {
                noGroupCutain = mutableListOf()
                listGroupCutain = mutableListOf()
                deviceListCurtains = DBUtils.getAllCurtains()
            }
            DeviceType.SMART_RELAY -> {
                noGroupRelay = mutableListOf()
                listGroupRelay = mutableListOf()
                deviceListRelay = DBUtils.getAllRelay()
            }
        }

        when (deviceType) {
            DeviceType.LIGHT_RGB, DeviceType.LIGHT_NORMAL -> {
                groupingLight(DeviceType.LIGHT_RGB)
                deviceData = if (!isHaveGrouped && noGroup?.size ?: 0 > 0) {
                    isSelectNo(true)
                    noGroup
                } else {
                    isSelectNo(false)
                    listGroup
                }
                setDeviceListHeight(deviceData?.size)

                LogUtils.e("zcl---批量分组4----灯设备信息$deviceData")
                setLightAdatpter()
            }
            DeviceType.SMART_CURTAIN -> {
                deviceListCurtains = DBUtils.getAllCurtains()
                groupingLight(DeviceType.SMART_CURTAIN)
                deviceDataCurtain = if (!isHaveGrouped && noGroupCutain?.size ?: 0 > 0) {
                    isSelectNo(true)
                    noGroupCutain
                } else {
                    isSelectNo(false)
                    listGroupCutain
                }
                setDeviceListHeight(deviceDataCurtain?.size)

                LogUtils.e("zcl---批量分组4----灯设备信息$deviceData")
                setCurtainAdatpter()
            }
            DeviceType.SMART_RELAY -> {
                groupingLight(DeviceType.SMART_RELAY)
                deviceDataRelay = if (!isHaveGrouped && noGroupRelay?.size ?: 0 > 0) {
                    isSelectNo(true)
                    noGroupRelay
                } else {
                    isSelectNo(false)
                    listGroupRelay
                }
                setDeviceListHeight(deviceDataRelay?.size)

                LogUtils.e("zcl---批量分组4----灯设备信息$deviceData")
                setRelayAdatpter()
            }
        }
    }

    private fun setLightAdatpter() {
        lightAdapter = BatchFourLightAdapter(R.layout.batch_device_item, deviceData!!)
        setDeviceListHeight(deviceData?.size)
        lightAdapter?.bindToRecyclerView(batch_four_device_recycle)
    }

    private fun setCurtainAdatpter() {
        curtainAdapter = BatchFourCurtainAdapter(R.layout.batch_device_item, deviceDataCurtain!!)
        setDeviceListHeight(deviceDataCurtain?.size)
        curtainAdapter?.bindToRecyclerView(batch_four_device_recycle)
    }

    private fun setRelayAdatpter() {
        relayAdapter = BatchFourRelayAdapter(R.layout.batch_device_item, deviceDataRelay!!)
        setDeviceListHeight(deviceDataRelay?.size)
        relayAdapter?.bindToRecyclerView(batch_four_device_recycle)
    }

    private fun setDeviceListHeight(size: Int?) {
        if (size == 0) {
            batch_four_device_recycle.visibility = View.INVISIBLE
            image_no_device.visibility = View.VISIBLE
        } else {
            batch_four_device_recycle.visibility = View.VISIBLE
            image_no_device.visibility = View.INVISIBLE
        }
        if (size ?: 0 > 8)
            batch_four_device_recycle.layoutParams.height = ScreenUtils.dp2px(this, 220)
        else
            batch_four_device_recycle.layoutParams.height = ScreenUtils.dp2px(this, 100)
    }


    @SuppressLint("CheckResult")
    fun autoConnect() {
        //如果支持蓝牙就打开蓝牙
        if (LeBluetooth.getInstance().isSupport(applicationContext))
            LeBluetooth.getInstance().enable(applicationContext)    //如果没打开蓝牙，就提示用户打开

        //如果位置服务没打开，则提示用户打开位置服务，bleScan必须
        if (!BleUtils.isLocationEnable(this)) {
            showOpenLocationServiceDialog()
        } else {
            hideLocationServiceDialog()
            mTelinkLightService = TelinkLightService.Instance()
            while (TelinkApplication.getInstance()?.serviceStarted == true) {
                disposable?.dispose()
                if (!this.isDestroyed)
                disposable = RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            if (!TelinkLightService.Instance().isLogin) {
                                ToastUtils.showLong(R.string.connecting_please_wait)

                                retryConnectCount = 0
                                val meshName = DBUtils.lastUser!!.controlMeshName

                                GlobalScope.launch {
                                    //自动重连参数
                                    val connectParams = Parameters.createAutoConnectParameters()
                                    connectParams.setMeshName(meshName)
                                    connectParams.setPassword(NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16))
                                    connectParams.autoEnableNotification(true)
                                    connectParams.setConnectDeviceType(
                                            mutableListOf(DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB,
                                                    DeviceType.SMART_RELAY, DeviceType.SMART_CURTAIN))
                                    //连接，如断开会自动重连
                                    TelinkLightService.Instance().autoConnect(connectParams)

                                }
                            }
                        }, { LogUtils.d(it) })
                break
            }
        }
        val deviceInfo = this.mApplication?.connectDevice
        if (deviceInfo != null) {
            this.connectMeshAddress = (this.mApplication?.connectDevice?.meshAddress
                    ?: 0x00) and 0xFF
        }
    }


    @SuppressLint("StringFormatMatches")
    private fun groupingLight(deviceType: Int) {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                for (i in deviceListLight.indices) {//获取组名 将分组与未分组的拆分
                    if (StringUtils.getLightGroupName(deviceListLight[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
                        deviceListLight[i].groupName = ""
                        noGroup!!.add(deviceListLight[i])
                    } else {
                        deviceListLight[i].groupName = StringUtils.getLightGroupName(deviceListLight[i])
                        listGroup!!.add(deviceListLight[i])
                    }
                }
                setNoOrHavedGroupText(noGroup?.size, listGroup?.size)
            }
            DeviceType.SMART_CURTAIN -> {
                for (i in deviceListCurtains.indices) {//获取组名 将分组与未分组的拆分
                    if (StringUtils.getCurtainName(deviceListCurtains[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
                        deviceListCurtains[i].groupName = ""
                        noGroupCutain!!.add(deviceListCurtains[i])
                    } else {
                        deviceListLight[i].groupName = StringUtils.getLightGroupName(deviceListLight[i])
                        listGroupCutain!!.add(deviceListCurtains[i])
                    }
                }
                setNoOrHavedGroupText(noGroupCutain?.size, listGroupCutain?.size)
            }
            DeviceType.SMART_RELAY -> {
                for (i in deviceListRelay.indices) {//获取组名 将分组与未分组的拆分
                    if (StringUtils.getConnectorName(deviceListRelay[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
                        deviceListRelay[i].groupName = ""
                        noGroupRelay!!.add(deviceListRelay[i])
                    } else {
                        deviceListLight[i].groupName = StringUtils.getLightGroupName(deviceListLight[i])
                        listGroupRelay!!.add(deviceListRelay[i])
                    }
                }
                setNoOrHavedGroupText(noGroupRelay?.size, listGroupRelay?.size)
            }
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun setNoOrHavedGroupText(noGroupSize: Int?, listGroupSize: Int?) {
        batch_four_no_group.text = getString(R.string.no_group_device_num, noGroupSize)
        batch_four_grouped.text = getString(R.string.grouped_num, listGroupSize)
    }

    private fun initListener() {
        //先取消，这样可以确保不会重复添加监听
        this.mApplication?.removeEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)

        batch_four_group_rg.setOnCheckedChangeListener { _, checkedId -> changeHaveOrNoGroupDevice(checkedId) }

        batch_four_device_all.setOnClickListener { changeDeviceAll() }

        batch_four_group_add_group.setOnClickListener { addNewGroup() }

        btnAddGroups?.setOnClickListener { addNewGroup() }

        lightAdapter?.setOnItemChildClickListener { _, _, position ->
            deviceData?.get(position)!!.selected = !deviceData?.get(position)!!.selected
            if (deviceData?.get(position)!!.isSelected) {
                startBlink(deviceData?.get(position)!!.belongGroupId, deviceData?.get(position)!!.meshAddr)
                selectLights.add(deviceData!![position])
            } else {
                stopBlink(deviceData?.get(position)!!.meshAddr)
                selectLights.remove(deviceData!![position])
            }
            changeGroupingCompleteState()
            lightAdapter?.notifyDataSetChanged()
        }

        curtainAdapter?.setOnItemChildClickListener { _, _, position ->
            deviceDataCurtain?.get(position)!!.selected = !deviceDataCurtain?.get(position)!!.selected
            if (deviceDataCurtain?.get(position)!!.isSelected) {
                startBlink(deviceDataCurtain?.get(position)!!.belongGroupId, deviceDataCurtain?.get(position)!!.meshAddr)
                selectCurtains.add(deviceDataCurtain!![position])
            } else {
                stopBlink(deviceDataCurtain?.get(position)!!.meshAddr)
                selectCurtains.remove(deviceDataCurtain!![position])
            }
            changeGroupingCompleteState()
            curtainAdapter?.notifyDataSetChanged()
        }

        relayAdapter?.setOnItemChildClickListener { _, _, position ->
            deviceDataRelay?.get(position)!!.selected = !deviceDataRelay?.get(position)!!.selected
            if (deviceDataRelay?.get(position)!!.isSelected) {
                startBlink(deviceDataRelay?.get(position)!!.belongGroupId, deviceDataRelay?.get(position)!!.meshAddr)
                selectRelays.add(deviceDataRelay!![position])
            } else {
                stopBlink(deviceDataRelay?.get(position)!!.meshAddr)
                selectRelays.remove(deviceDataRelay!![position])
            }
        changeGroupingCompleteState()
            relayAdapter?.notifyDataSetChanged()
        }
        grouping_completed.setOnClickListener {
            sureGroups()
        }
    }

    private fun changeGroupingCompleteState() {
        if (selectLights.size > 0 && currentGroup != null) {
            grouping_completed.setBackgroundResource(R.drawable.btn_rec_blue_bt)
            grouping_completed.isClickable = true
        } else {
            grouping_completed.isClickable = false
            grouping_completed.setBackgroundResource(R.drawable.btn_rec_black_bt)
        }
    }


    private fun sureGroups() {
        val isSelected = isSelectDevice(deviceType)
        if (isSelected) {
            //进行分组操作
            //获取当前选择的分组
            if (currentGroup != null) {
                if (currentGroup!!.meshAddr == 0xffff) {
                    ToastUtils.showLong(R.string.tip_add_gp)
                    return
                }
                when (deviceType) {
                    DeviceType.LIGHT_RGB, DeviceType.LIGHT_NORMAL -> {
                        showLoadingDialog(resources.getString(R.string.grouping_wait_tip,
                                selectLights.size.toString()))
                        setGroup(DeviceType.LIGHT_RGB)
                    }
                    DeviceType.SMART_CURTAIN -> {
                        showLoadingDialog(resources.getString(R.string.grouping_wait_tip,
                                selectCurtains.size.toString()))
                        setGroup(DeviceType.SMART_CURTAIN)
                    }
                    DeviceType.SMART_RELAY -> {
                        showLoadingDialog(resources.getString(R.string.grouping_wait_tip,
                                selectRelays.size.toString()))
                        setGroup(DeviceType.SMART_RELAY)
                    }
                }
                //将灯列表的灯循环设置分组
                setGroupForLights(currentGroup, deviceType)

            } else {
                Toast.makeText(mApplication, R.string.select_group_tip, Toast.LENGTH_SHORT).show()
            }
        } else {
            showToast(getString(R.string.selected_lamp_tip))
        }
    }

    private fun setGroup(deviceType: Int) {
        when (deviceType) {
            DeviceType.LIGHT_RGB, DeviceType.LIGHT_NORMAL -> {
                showLoadingDialog(resources.getString(R.string.grouping_wait_tip, selectLights.size.toString()))
                for (i in selectLights.indices) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(selectLights[i].meshAddr)
                }
            }
            DeviceType.SMART_CURTAIN -> {
                showLoadingDialog(resources.getString(R.string.grouping_wait_tip, selectCurtains.size.toString()))

                for (i in selectCurtains.indices) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(selectCurtains[i].meshAddr)
                }
            }
            DeviceType.SMART_RELAY -> {
                showLoadingDialog(resources.getString(R.string.grouping_wait_tip, selectRelays.size.toString()))
                for (i in selectRelays.indices) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(selectRelays[i].meshAddr)
                }
            }
        }
        setGroupOneByOne(currentGroup!!, deviceType, 0)
    }

    private fun completeGroup(deviceType: Int) {
        when (deviceType) {
            DeviceType.LIGHT_RGB, DeviceType.LIGHT_NORMAL -> {
                //取消分组成功的勾选的灯
                for (i in selectLights.indices) {
                    val light = selectLights[i]
                    light.selected = false
                }
                lightAdapter?.notifyDataSetChanged()
            }
            DeviceType.SMART_CURTAIN -> {
                //取消分组成功的勾选的灯
                for (i in selectCurtains.indices) {
                    val light = selectCurtains[i]
                    light.selected = false
                }
                curtainAdapter?.notifyDataSetChanged()
            }
            DeviceType.SMART_RELAY -> {
                //取消分组成功的勾选的灯
                for (i in selectRelays.indices) {
                    val light = selectRelays[i]
                    light.selected = false
                }
                relayAdapter?.notifyDataSetChanged()
            }
        }

        hideLoadingDialog()
        if (isAllLightsGrouped()) {
            //grouping_completed.setText(R.string.complete)
            ToastUtils.showShort(getString(R.string.grouping_completed))
            SyncDataPutOrGetUtils.syncPutDataStart(this, object : SyncCallback {
                override fun start() {
                }

                override fun complete() {
                }

                override fun error(msg: String?) {
                }
            })
            setGroupData(deviceType)
            ClearSelectors()
        }
    }

    private fun setGroupForLights(group: DbGroup?, deviceType: Int) {

        when (deviceType) {
            DeviceType.LIGHT_RGB, DeviceType.LIGHT_NORMAL -> {
                for (i in selectLights.indices) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(selectLights[i].meshAddr)
                }
            }
            DeviceType.SMART_CURTAIN -> {
                for (i in selectCurtains.indices) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(selectCurtains[i].meshAddr)
                }
            }
            DeviceType.SMART_RELAY -> {
                for (i in selectRelays.indices) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(selectRelays[i].meshAddr)
                }
            }
        }
    }

    private fun updateGroupResultLight(light: DbLight, group: DbGroup) {
        for (i in selectLights.indices) {
            if (light.meshAddr == selectLights[i].meshAddr) {
                if (light.belongGroupId != allLightId) {
                    selectLights[i].hasGroup = true
                    selectLights[i].belongGroupId = group.id
                    selectLights[i].name = light.name
                    DBUtils.updateLight(light)
                } else {
                    selectLights[i].hasGroup = false
                }
            }
        }
    }
    private fun updateGroupResultCurtain(light: DbCurtain, group: DbGroup) {
        for (i in selectCurtains.indices) {
            if (light.meshAddr == selectCurtains[i].meshAddr) {
                if (light.belongGroupId != allLightId) {
                    selectCurtains[i].hasGroup = true
                    selectCurtains[i].belongGroupId = group.id
                    selectCurtains[i].name = light.name
                    DBUtils.updateCurtain(light)
                } else {
                    selectCurtains[i].hasGroup = false
                }
            }
        }
    }   private fun updateGroupResultRelay(light: DbConnector, group: DbGroup) {
        for (i in selectRelays.indices) {
            if (light.meshAddr == selectRelays[i].meshAddr) {
                if (light.belongGroupId != allLightId) {
                    selectRelays[i].hasGroup = true
                    selectRelays[i].belongGroupId = group.id
                    selectRelays[i].name = light.name
                    DBUtils.updateConnector(light)
                } else {
                    selectRelays[i].hasGroup = false
                }
            }
        }
    }

    //等待解决无法加入窗帘如组
    private fun setGroupOneByOne(dbGroup: DbGroup, deviceType: Int, index: Int) {
        var deviceMeshAddr  =0
        var dbLight:DbLight?=null
        var dbCurtain:DbCurtain?=null
        var dbRelay:DbConnector?=null

        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                 dbLight = selectLights[index]
                deviceMeshAddr = dbLight.meshAddr
            }
            DeviceType.SMART_CURTAIN -> {
                 dbCurtain = selectCurtains[index]
                deviceMeshAddr = dbCurtain.meshAddr
            }
            DeviceType.SMART_RELAY -> {
                 dbRelay = selectRelays[index]
                deviceMeshAddr = dbRelay.meshAddr
            }
        }

        val successCallback: () -> Unit = {
            when (deviceType) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                    dbLight?.belongGroupId = dbGroup.id
                    updateGroupResultLight(dbLight!!, dbGroup)
                    if (index + 1 > selectLights.size - 1)
                        completeGroup(DeviceType.LIGHT_RGB)
                    else
                        setGroupOneByOne(dbGroup, DeviceType.LIGHT_RGB, index + 1)
                }
                DeviceType.SMART_CURTAIN -> {
                    dbCurtain?.belongGroupId = dbGroup.id
                    updateGroupResultCurtain(dbCurtain!!, dbGroup)
                    if (index + 1 > selectCurtains.size - 1)
                        completeGroup(DeviceType.SMART_CURTAIN)
                    else
                        setGroupOneByOne(dbGroup, DeviceType.SMART_CURTAIN, index + 1)
                }
                DeviceType.SMART_RELAY -> {
                    dbRelay?.belongGroupId = dbGroup.id
                    updateGroupResultRelay(dbRelay!!, dbGroup)
                    if (index + 1 > selectRelays.size - 1)
                        completeGroup(DeviceType.SMART_RELAY)
                    else
                        setGroupOneByOne(dbGroup, DeviceType.SMART_RELAY, index + 1)
                }
            }
        }
        val failedCallback: () -> Unit = {
                    ToastUtils.showLong(R.string.group_fail_tip)
            when (deviceType) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                    dbLight?.belongGroupId = allLightId
                    updateGroupResultLight(dbLight!!, dbGroup)
                    if (TelinkLightApplication.getApp().connectDevice == null) {
                        ToastUtils.showLong(getString(R.string.connect_fail))
                    } else {
                        if (index + 1 > selectLights.size - 1)
                          completeGroup(DeviceType.LIGHT_RGB)
                        else
                          setGroupOneByOne(dbGroup, DeviceType.LIGHT_RGB, index + 1)
                    }
                }
                DeviceType.SMART_CURTAIN -> {
                    dbCurtain?.belongGroupId = allLightId
                    updateGroupResultCurtain(dbCurtain!!, dbGroup)
                    if (TelinkLightApplication.getApp().connectDevice == null) {
                        ToastUtils.showLong(getString(R.string.connect_fail))
                    } else {
                        if (index + 1 > selectCurtains.size - 1)
                            completeGroup(DeviceType.SMART_CURTAIN)
                        else
                            setGroupOneByOne(dbGroup, DeviceType.SMART_CURTAIN, index + 1)
                    }
                }
                DeviceType.SMART_RELAY -> {
                    dbRelay?.belongGroupId = allLightId
                    updateGroupResultRelay(dbRelay!!, dbGroup)
                    if (TelinkLightApplication.getApp().connectDevice == null) {
                        ToastUtils.showLong(getString(R.string.connect_fail))
                    } else {
                        if (index + 1 > selectRelays.size - 1)
                            completeGroup(DeviceType.SMART_RELAY)
                        else
                            setGroupOneByOne(dbGroup, DeviceType.SMART_RELAY, index + 1)
                    }
                }
            }
        }
        Commander.addGroup(deviceMeshAddr, dbGroup.meshAddr, successCallback, failedCallback)
    }

    private fun stopBlink(meshAddr: Int) {
        val disposable = mBlinkDisposables.get(meshAddr)
        disposable?.dispose()
    }

    private fun isSelectDevice(deviceType: Int): Boolean {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                deviceData ?: return false
                return selectLights.size > 0
            }
            DeviceType.SMART_CURTAIN -> {
                deviceDataCurtain ?: return false
                return selectCurtains.size > 0
            }
            DeviceType.SMART_RELAY -> {
                deviceDataRelay ?: return false
                return selectRelays.size > 0
            }
        }
        return false
    }


    private fun changeHaveOrNoGroupDevice(checkedId: Int) {
        batch_four_device_all.text = getString(R.string.select_all)
        setAllSelect(false)
        isHaveGrouped = checkedId == R.id.batch_four_grouped
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                if (deviceData!!.size > 8)
                    batch_four_device_recycle.layoutParams.height = ScreenUtils.dp2px(this, 220)
            }
            DeviceType.SMART_CURTAIN -> {
                if (deviceDataCurtain!!.size > 8)
                    batch_four_device_recycle.layoutParams.height = ScreenUtils.dp2px(this, 220)
            }
            DeviceType.SMART_RELAY -> {
                if (deviceDataRelay!!.size > 8)
                    batch_four_device_recycle.layoutParams.height = ScreenUtils.dp2px(this, 220)
            }
        }


        if (isHaveGrouped) {//是否选中了已分组
            isSelectNo(false)//是否选中不分组 不显示
        } else {
            isSelectNo(true)//显示
        }
        setChangeHaveOrNoGroupDevice()
    }

    private fun isSelectNo(b: Boolean) {
        batch_four_grouped.isChecked = !b
        if (b) {
            when (deviceType) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                    if (deviceData?.size?:0 > 8)
                        batch_four_device_recycle.layoutParams.height = ScreenUtils.dp2px(this, 220)
                }
                DeviceType.SMART_CURTAIN -> {
                    if (deviceDataCurtain?.size?:0 > 8)
                        batch_four_device_recycle.layoutParams.height = ScreenUtils.dp2px(this, 220)
                }
                DeviceType.SMART_RELAY -> {
                    if (deviceDataRelay?.size?:0 > 8)
                        batch_four_device_recycle.layoutParams.height = ScreenUtils.dp2px(this, 220)
                }
            }
            batch_four_no_group_line.visibility = View.VISIBLE
            batch_four_grouped_line.visibility = View.INVISIBLE
        } else {
            batch_four_no_group_line.visibility = View.INVISIBLE
            batch_four_grouped_line.visibility = View.VISIBLE
            ClearSelectors()
        }
    }

    private fun changeDeviceAll() {
        val isSelectAll = getString(R.string.select_all) == batch_four_device_all.text.toString()
        if (isSelectAll)
            batch_four_device_all.text = getString(R.string.cancel)
        else
            batch_four_device_all.text = getString(R.string.select_all)
        setAllSelect(isSelectAll)
    }

    private fun changeGroupSelectView(position: Int) {
        if (lastCheckedGroupPostion != position && groupsByDeviceType?.get(lastCheckedGroupPostion) != null){
            groupsByDeviceType?.get(lastCheckedGroupPostion)!!.checked = false
        }

        lastCheckedGroupPostion = position
        if (groupsByDeviceType?.get(position) != null)
            groupsByDeviceType?.get(position)!!.checked = !groupsByDeviceType?.get(position)!!.checked
        currentGroup = if (groupsByDeviceType?.get(position)!!.isChecked)
            groupsByDeviceType?.get(position)
        else
            null

        changeGroupingCompleteState()
        groupAdapter?.notifyDataSetChanged()
    }

    private fun addNewGroup() {
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(DBUtils.getDefaultNewGroupName())
        //设置光标默认在最后
        textGp.setSelection(textGp.text.toString().length)
        android.app.AlertDialog.Builder(this)
                .setTitle(R.string.create_new_group)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)
                .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        //往DB里添加组数据
                        var groupType = Constant.DEVICE_TYPE_DEFAULT_ALL
                        when (deviceType) {
                            DeviceType.LIGHT_NORMAL -> groupType = Constant.DEVICE_TYPE_LIGHT_NORMAL
                            DeviceType.LIGHT_RGB -> groupType = Constant.DEVICE_TYPE_LIGHT_RGB
                            DeviceType.SMART_CURTAIN -> groupType = Constant.DEVICE_TYPE_CURTAIN
                            DeviceType.SMART_RELAY -> groupType = Constant.DEVICE_TYPE_CONNECTOR
                        }
                        DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, groupType)
                        setGroupData(deviceType)
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()
    }

    private fun setAllSelect(isSelectAll: Boolean) {
        ClearSelectors()//清理所有选中
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                deviceData ?: return

                selectLights.clear()
                if (isSelectAll)
                    selectLights = deviceData as MutableList<DbLight>

                for (i in deviceData!!.indices) {
                    deviceData!![i].selected = isSelectAll
                    if (isSelectAll)
                        startBlink(deviceData!![i].belongGroupId, deviceData!![i].meshAddr)
                }
                lightAdapter?.notifyDataSetChanged()
            }
            DeviceType.SMART_CURTAIN -> {
                deviceDataCurtain ?: return

                selectCurtains.clear()
                if (isSelectAll)
                    selectCurtains = deviceDataCurtain as MutableList<DbCurtain>

                for (i in deviceData!!.indices) {
                    deviceDataCurtain!![i].selected = isSelectAll
                    if (isSelectAll)
                        startBlink(deviceDataCurtain!![i].belongGroupId, deviceDataCurtain!![i].meshAddr)
                }
                curtainAdapter?.notifyDataSetChanged()
            }
            DeviceType.SMART_RELAY -> {
                deviceDataRelay ?: return

                selectRelays.clear()
                if (isSelectAll)
                    selectRelays = deviceDataRelay as MutableList<DbConnector>

                for (i in deviceDataRelay!!.indices) {
                    deviceDataRelay!![i].selected = isSelectAll
                    if (isSelectAll)
                        startBlink(deviceDataRelay!![i].belongGroupId, deviceDataRelay!![i].meshAddr)
                }
                relayAdapter?.notifyDataSetChanged()
            }
        }
    }

    private fun setChangeHaveOrNoGroupDevice() {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                deviceData = if (isHaveGrouped)
                    listGroup
                else
                    noGroup
                setLightAdatpter()
            }
            DeviceType.SMART_CURTAIN -> {
                deviceDataCurtain = if (isHaveGrouped)
                    listGroupCutain
                else
                    noGroupCutain
                setCurtainAdatpter()
            }
            DeviceType.SMART_RELAY -> {
                deviceDataRelay = if (isHaveGrouped)
                    listGroupRelay
                else
                    noGroupRelay
                setRelayAdatpter()
            }
        }
    }


    /**
     * 让灯开始闪烁
     */
    private fun startBlink(belongGroupId: Long, meshAddr: Int) {
        val group: DbGroup
        if (currentGroup != null) {
            val groupOfTheLight = DBUtils.getGroupByID(belongGroupId)
            group = when (groupOfTheLight) {
                null -> currentGroup!!
                else -> groupOfTheLight
            }
            val groupAddress = group.meshAddr
            Log.d("zcl", "startBlink groupAddresss = $groupAddress")
            val opcode = Opcode.SET_GROUP
            val params = byteArrayOf(0x01, (groupAddress and 0xFF).toByte(), (groupAddress shr 8 and 0xFF).toByte())
            params[0] = 0x01

            if (mBlinkDisposables.get(meshAddr) != null) {
                mBlinkDisposables.get(meshAddr).dispose()
            }
            //每隔1s发一次，就是为了让灯一直闪.
            mBlinkDisposables.put(meshAddr, Observable.interval(0, 1000, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { TelinkLightService.Instance().sendCommandNoResponse(opcode, meshAddr, params) })
        }
    }

    override fun onResume() {
        super.onResume()
        //检测service是否为空，为空则重启
        if (TelinkLightApplication.getApp().connectDevice == null) {
            if (TelinkLightService.Instance() == null)
                mApplication?.startLightService(TelinkLightService::class.java)
            autoConnect()
        }
    }


    /**
     * 是否所有灯都分了组
     *
     * @return false还有没有分组的灯 true所有灯都已经分组
     */
    private fun isAllLightsGrouped(): Boolean {
        for (j in deviceListLight.indices) {
            if (deviceListLight[j].belongGroupId == allLightId) {
                return false
            }
        }
        return true
    }

    override fun performed(event: Event<String>?) {
        when (event?.type) {
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReportNormal(info)
            }
        }
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {
        val deviceInfo = event.args
        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                ToastUtils.showLong(getString(R.string.connect_success))
                changeDisplayImgOnToolbar(true)

                val connectDevice = this.mApplication?.connectDevice
                LogUtils.d("directly connection device meshAddr = ${connectDevice?.meshAddress}")
            }
            LightAdapter.STATUS_LOGOUT -> {
                changeDisplayImgOnToolbar(false)
                autoConnect()
            }
            LightAdapter.STATUS_CONNECTING -> {
                ToastUtils.showLong(R.string.connecting_please_wait)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
        isAddGroupEmptyView = false
    }

}