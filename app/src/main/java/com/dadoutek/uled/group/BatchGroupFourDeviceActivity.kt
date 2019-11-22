package com.dadoutek.uled.group

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2019/10/16 14:41
 * 描述
 *
 * 更新者     zcl$
 * 更新时间   用于冷暖灯,彩灯,窗帘控制器的批量分组$
 *
 * 24khome  威廉威定制 bosie agender 三家衬衫
 * 更新描述
 */
class BatchGroupFourDeviceActivity : TelinkBaseActivity(), EventListener<String> {
    private var disposableGroupTimer: Disposable? = null
    private var isCompatible: Boolean = true
    private var isChange: Boolean = false
    private var lpShort: LinearLayout.LayoutParams? = null
    private var lplong: LinearLayout.LayoutParams? = null
    private var disposable: Disposable? = null
    private var currentGroup: DbGroup? = null
    private var btnAddGroups: TextView? = null
    private var emptyGroupView: View? = null
    private var mTelinkLightService: TelinkLightService? = null

    private val noGroup: MutableList<DbLight> = mutableListOf()
    private val listGroup: MutableList<DbLight> = mutableListOf()
    private val deviceData: MutableList<DbLight> = mutableListOf()
    private val groupsByDeviceType: MutableList<DbGroup> = mutableListOf()

    private val noGroupCutain: MutableList<DbCurtain> = mutableListOf()
    private val listGroupCutain: MutableList<DbCurtain> = mutableListOf()
    private val deviceDataCurtain: MutableList<DbCurtain> = mutableListOf()


    private val selectLights = mutableListOf<DbLight>()
    private val selectCurtains = mutableListOf<DbCurtain>()
    private val selectRelays = mutableListOf<DbConnector>()

    private val deviceDataRelayAll: ArrayList<DbConnector> = arrayListOf()
    private val deviceDataCurtainAll: ArrayList<DbCurtain> = arrayListOf()
    private val deviceDataLightAll: ArrayList<DbLight> = arrayListOf()

    private var noGroupRelay: MutableList<DbConnector> = mutableListOf()
    private var listGroupRelay: MutableList<DbConnector> = mutableListOf()
    private var deviceDataRelay: MutableList<DbConnector> = mutableListOf()

    private var groupAdapter: BatchGrouopEditListAdapter = BatchGrouopEditListAdapter(R.layout.batch_four_group_edit_item, groupsByDeviceType)
    private var lightAdapter: BatchFourLightAdapter = BatchFourLightAdapter(R.layout.batch_device_item, deviceData)
    private var relayAdapter: BatchFourRelayAdapter = BatchFourRelayAdapter(R.layout.batch_device_item, deviceDataRelay)
    private var curtainAdapter: BatchFourCurtainAdapter = BatchFourCurtainAdapter(R.layout.batch_device_item, deviceDataCurtain)


    private var deviceType: Int = 100
    private var lastCheckedGroupPostion: Int = 1000
    private var allLightId: Long = 0//有设备等于0说明没有分组成功
    private var retryConnectCount = 0
    private var connectMeshAddress: Int = 0
    private var mApplication: TelinkLightApplication? = null

    private var checkedNoGrouped: Boolean = true

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
        toolbar.setTitleTextColor(getColor(R.color.white))
        toolbar.title = getString(R.string.batch_group)
        image_bluetooth.visibility = View.VISIBLE
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            checkNetworkAndSync(this)
            ToastUtils.showShort(getString(R.string.grouping_success_tip))
            setDeviceTypeDataStopBlink(deviceType)
            finish()
        }

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

        lplong = batch_four_no_group.layoutParams as LinearLayout.LayoutParams
        lplong?.weight = 3f

        lpShort = batch_four_no_group_line_ly.layoutParams as LinearLayout.LayoutParams
        lpShort?.weight = 2f

        batch_four_compatible_mode.isChecked = true
        isCompatible = true

        deviceType = intent.getIntExtra(Constant.DEVICE_TYPE, 100)

        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                setDeviceListAndEmpty(deviceData.size)
                lightAdapter.bindToRecyclerView(batch_four_device_recycle)
            }
            DeviceType.SMART_CURTAIN -> {
                setDeviceListAndEmpty(deviceDataCurtain.size)
                curtainAdapter.bindToRecyclerView(batch_four_device_recycle)
            }
            DeviceType.SMART_RELAY -> {
                setDeviceListAndEmpty(deviceDataRelay.size)
                relayAdapter.bindToRecyclerView(batch_four_device_recycle)
            }
        }
        groupAdapter.bindToRecyclerView(batch_four_group_recycle)
        autoConnect()
    }

    private fun initData() {
        setDevicesData(deviceType)
        setGroupData(deviceType)
    }

    /**
     * 设置组数据
     */
    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    private fun setGroupData(deviceType: Int) {
        currentGroup = null
        groupsByDeviceType.clear()
        groupsByDeviceType.addAll(DBUtils.getGroupsByDeviceType(deviceType))
        groupsByDeviceType.addAll(DBUtils.getGroupsByDeviceType(0))

        if (!isAddGroupEmptyView)
            groupAdapter.emptyView = emptyGroupView

        for (i in groupsByDeviceType.indices)
            groupsByDeviceType[i].isCheckedInGroup = false

        isAddGroupEmptyView = true
        groupAdapter.notifyItemRangeChanged(0, groupsByDeviceType.size)

        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                changeGroupingCompleteState(selectLights.size)
            }
            DeviceType.SMART_CURTAIN -> {
                changeGroupingCompleteState(selectCurtains.size)
            }
            DeviceType.SMART_RELAY -> {
                changeGroupingCompleteState(selectRelays.size)
            }
        }
    }

    private fun showGroupForUpdateNameDialog(position: Int) {
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(groupsByDeviceType?.get(position)?.name)
        //        //设置光标默认在最后
        textGp.setSelection(textGp.text.toString().length)
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.update_name_gp))
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)
                .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        groupsByDeviceType?.get(position)?.name = textGp.text.toString().trim { it <= ' ' }
                        val group = groupsByDeviceType?.get(position)
                        if (group != null)
                            DBUtils.updateGroup(group)
                        groupAdapter?.notifyItemRangeChanged(position, 1)
                        setDevicesData(deviceType)
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()
    }

    private fun clearSelectors() {
        selectLights.clear()
        selectCurtains.clear()
        selectRelays.clear()
    }

    /**
     * 设置设备数据
     */
    @SuppressLint("StringFormatInvalid")
    private fun setDevicesData(deviceType: Int) {
        clearSelectors()

        when (deviceType) {
            DeviceType.LIGHT_NORMAL -> {
                noGroup.clear()
                listGroup.clear()
                deviceDataLightAll.addAll(DBUtils.getAllNormalLight())
            }
            DeviceType.LIGHT_RGB -> {
                noGroup.clear()
                listGroup.clear()
                deviceDataLightAll.addAll(DBUtils.getAllRGBLight())
            }
            DeviceType.SMART_CURTAIN -> {
                noGroupCutain.clear()
                listGroupCutain.clear()
                deviceDataCurtainAll.addAll(DBUtils.getAllCurtains())
            }
            DeviceType.SMART_RELAY -> {
                noGroupRelay.clear()
                listGroupRelay.clear()
                deviceDataRelayAll.addAll(DBUtils.getAllRelay())
            }
        }


        when (deviceType) {
            DeviceType.LIGHT_RGB, DeviceType.LIGHT_NORMAL -> {
                groupingNoOrHaveAndchangeTitle(deviceType)
                //checkedNoGrouped = noGroup?.size ?: 0 > 0  //初始化状态是否显示未分组灯 如果有就显示未分组否则一分组
            }
            DeviceType.SMART_CURTAIN -> {
                groupingNoOrHaveAndchangeTitle(DeviceType.SMART_CURTAIN)
                //checkedNoGrouped = noGroupCutain?.size ?: 0 > 0
            }
            DeviceType.SMART_RELAY -> {
                groupingNoOrHaveAndchangeTitle(DeviceType.SMART_RELAY)
                // checkedNoGrouped = noGroupRelay?.size ?: 0 > 0
            }
        }
        changeDeviceData()//赋值指定数据
    }


    private fun renameCurtain(curtain: DbCurtain?) {
        if (curtain != null) {
            var textGp = EditText(this)
            StringUtils.initEditTextFilter(textGp)
            textGp.setText(curtain?.name)
            textGp.setSelection(textGp.text.toString().length)
            android.app.AlertDialog.Builder(this@BatchGroupFourDeviceActivity)
                    .setTitle(R.string.rename)
                    .setView(textGp)
                    .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                        // 获取输入框的内容
                        if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                            ToastUtils.showShort(getString(R.string.rename_tip_check))
                        } else {
                            curtain.name = textGp.text.toString().trim { it <= ' ' }
                            DBUtils.updateCurtain(curtain)
                            changeDeviceData()
                            dialog.dismiss()
                        }
                    }
                    .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()
        }
    }

    private fun renameConnector(connector: DbConnector?) {
        if (connector != null) {
            var textGp = EditText(this)
            StringUtils.initEditTextFilter(textGp)
            textGp.setText(connector?.name)
            textGp.setSelection(textGp.text.toString().length)
            android.app.AlertDialog.Builder(this@BatchGroupFourDeviceActivity)
                    .setTitle(R.string.rename)
                    .setView(textGp)

                    .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                        // 获取输入框的内容
                        if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                            ToastUtils.showShort(getString(R.string.rename_tip_check))
                        } else {
                            connector.name = textGp.text.toString().trim { it <= ' ' }
                            DBUtils.updateConnector(connector)
                            changeDeviceData()
                            dialog.dismiss()
                        }
                    }
                    .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()
        }
    }

    private fun renameLight(light: DbLight?) {
        var textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(light?.name)
        textGp.setSelection(textGp.text.toString().length)
        android.app.AlertDialog.Builder(this@BatchGroupFourDeviceActivity)
                .setTitle(R.string.rename)
                .setView(textGp)
                .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        light?.name = textGp.text.toString().trim { it <= ' ' }
                        if (light != null)
                            DBUtils.updateLight(light)
                        changeDeviceData()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()
    }

    private fun setDeviceListAndEmpty(size: Int?) {
        if (size == 0) {
            batch_four_device_recycle.visibility = View.INVISIBLE
            image_no_device.visibility = View.VISIBLE
        } else {
            batch_four_device_recycle.visibility = View.VISIBLE
            image_no_device.visibility = View.INVISIBLE
        }
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
                                if (TelinkLightApplication.getApp().connectDevice == null) {
                                    ToastUtils.showShort(getString(R.string.connecting_tip))
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
            this.connectMeshAddress = (this.mApplication?.connectDevice?.meshAddress ?: 0x00) and 0xFF
        }
    }

    /**
     * 将已分组与未分组设备分离并设置recucleview
     */
    @SuppressLint("StringFormatMatches")
    private fun groupingNoOrHaveAndchangeTitle(deviceType: Int) {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                if (deviceType == DeviceType.LIGHT_NORMAL) {
                    deviceDataLightAll.clear()
                    deviceDataLightAll.addAll(DBUtils.getAllNormalLight())

                } else if (deviceType == DeviceType.LIGHT_RGB) {
                    deviceDataLightAll.clear()
                    deviceDataLightAll.addAll(DBUtils.getAllRGBLight())
                }

                for (i in deviceDataLightAll.indices) {//获取组名 将分组与未分组的拆分
                    deviceDataLightAll[i].selected = false
                    if (StringUtils.getLightGroupName(deviceDataLightAll[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
                        deviceDataLightAll[i].groupName = ""
                        noGroup.add(deviceDataLightAll[i])
                    } else {
                        deviceDataLightAll[i].groupName = StringUtils.getLightGroupName(deviceDataLightAll[i])
                        listGroup.add(deviceDataLightAll[i])
                    }
                }
            }
            DeviceType.SMART_CURTAIN -> {
                deviceDataCurtainAll.clear()
                deviceDataCurtainAll.addAll(DBUtils.getAllCurtains())

                for (i in deviceDataCurtainAll.indices) {//获取组名 将分组与未分组的拆分
                    deviceDataCurtainAll[i].selected = false
                    if (StringUtils.getCurtainName(deviceDataCurtainAll[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
                        deviceDataCurtainAll[i].groupName = ""
                        noGroupCutain.add(deviceDataCurtainAll[i])
                    } else {
                        deviceDataCurtainAll[i].groupName = StringUtils.getCurtainName(deviceDataCurtainAll[i])
                        listGroupCutain.add(deviceDataCurtainAll[i])
                    }
                }
            }
            DeviceType.SMART_RELAY -> {
                deviceDataRelayAll.clear()
                deviceDataRelayAll.addAll(DBUtils.getAllRelay())

                for (i in deviceDataRelayAll.indices) {//获取组名 将分组与未分组的拆分
                    deviceDataRelayAll[i].selected = false
                    if (StringUtils.getConnectorName(deviceDataRelayAll[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
                        deviceDataRelayAll[i].groupName = ""
                        noGroupRelay.add(deviceDataRelayAll[i])
                    } else {
                        deviceDataRelayAll[i].groupName = StringUtils.getConnectorName(deviceDataRelayAll[i])
                        listGroupRelay.add(deviceDataRelayAll[i])
                    }
                }
            }
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun changeTitleChecked() {
        if (checkedNoGrouped) {//显示没有分组的情况下
            batch_four_no_group.layoutParams = lplong
            batch_four_no_group.textSize = 18f
            batch_four_no_group_line_ly.layoutParams = lplong
            batch_four_grouped.layoutParams = lpShort
            batch_four_grouped.textSize = 15f
            batch_four_grouped_line_ly.layoutParams = lpShort

            batch_four_no_group_line.visibility = View.VISIBLE
            batch_four_grouped_line.visibility = View.INVISIBLE
        } else {//显示分组的情况下
            batch_four_no_group.layoutParams = lpShort
            batch_four_no_group.textSize = 15f
            batch_four_no_group_line_ly.layoutParams = lpShort
            batch_four_grouped.layoutParams = lplong
            batch_four_grouped.textSize = 18f
            batch_four_grouped_line_ly.layoutParams = lplong

            batch_four_no_group_line.visibility = View.INVISIBLE
            batch_four_grouped_line.visibility = View.VISIBLE
        }
    }

    private fun initListener() {
        //先取消，这样可以确保不会重复添加监听
//        this.mApplication?.removeEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        batch_four_compatible_mode.setOnCheckedChangeListener { _, isChecked ->
            isCompatible = isChecked
            if (!isCompatible) {
                ToastUtils.showShort(getString(R.string.compatibility_mode))
            }

            when (deviceType) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                    for (db in selectLights) {//获取组名 将分组与未分组的拆分
                        startBlink(db.belongGroupId, db.meshAddr)
                    }
                }

                DeviceType.SMART_RELAY -> {
                    for (db in selectRelays) {//获取组名 将分组与未分组的拆分
                        startBlink(db.belongGroupId, db.meshAddr)
                    }
                }
            }
            LogUtils.v("zcl是否选中$isChecked")
        }

        batch_four_group_rg.setOnCheckedChangeListener { _, checkedId ->
            checkedNoGrouped = checkedId == batch_four_no_group.id
            changeDeviceData()
        }

        batch_four_device_all.setOnClickListener { changeDeviceAll() }

        batch_four_group_add_group.setOnClickListener { addNewGroup() }

        grouping_completed.setOnClickListener {
            if (TelinkLightApplication.getApp().connectDevice != null)
                sureGroups()
            else
                autoConnect()
        }

        groupAdapter.setOnItemClickListener { _, _, position ->
            //如果点中的不是上次选中的组则恢复未选中状态
            changeGroupSelectView(position)
        }

        groupAdapter?.setOnItemLongClickListener { _, _, position ->
            showGroupForUpdateNameDialog(position)
            false
        }

        btnAddGroups?.setOnClickListener { addNewGroup() }

        //如果adapter内部使用了addLongclicklistener 那么外部必须使用longchildren否则addLongClick无效
        lightAdapter.setOnItemClickListener { _, _, position ->
            deviceData[position].selected = !deviceData[position].selected
            if (deviceData[position].isSelected) {
                startBlink(deviceData[position].belongGroupId, deviceData[position].meshAddr)
                selectLights.add(deviceData[position])
            } else {
                stopBlink(deviceData[position].meshAddr, deviceData[position].belongGroupId)
                selectLights.remove(deviceData[position])
            }
            changeGroupingCompleteState(selectLights.size)
            lightAdapter.notifyItemRangeChanged(0, deviceData.size)
            LogUtils.v("zcl更新后状态${deviceData[position].selected}")
        }
        lightAdapter.setOnItemLongClickListener { _, _, position ->
            renameLight(deviceData[position])
            false
        }


        relayAdapter.setOnItemClickListener { _, _, position ->
            deviceDataRelay[position].selected = !deviceDataRelay[position].selected
            if (deviceDataRelay[position].isSelected) {
                startBlink(deviceDataRelay[position].belongGroupId, deviceDataRelay[position].meshAddr)
                selectRelays.add(deviceDataRelay[position])
            } else {
                stopBlink(deviceDataRelay[position].meshAddr, deviceDataRelay[position].belongGroupId)
                selectRelays.remove(deviceDataRelay[position])
            }
            changeGroupingCompleteState(selectRelays.size)
            relayAdapter.notifyItemRangeChanged(0, deviceDataRelay.size)
        }
        relayAdapter.setOnItemLongClickListener { _, _, position ->
            renameConnector(deviceDataRelay[position])
            false
        }

        curtainAdapter.setOnItemClickListener { _, _, position ->
            deviceDataCurtain[position].selected = !deviceDataCurtain[position].selected
            if (deviceDataCurtain[position].isSelected) {
                startBlink(deviceDataCurtain[position].belongGroupId, deviceDataCurtain[position].meshAddr)
                selectCurtains.add(deviceDataCurtain[position])
            } else {
                stopBlink(deviceDataCurtain[position].meshAddr, deviceDataCurtain[position].belongGroupId)
                selectCurtains.remove(deviceDataCurtain[position])
            }
            changeGroupingCompleteState(selectCurtains.size)
            curtainAdapter.notifyItemRangeChanged(0, deviceDataCurtain.size)
        }
        curtainAdapter.setOnItemLongClickListener { _, _, position ->
            renameCurtain(deviceDataCurtain[position])
            false
        }
    }

    private fun changeGroupingCompleteState(size: Int) {
        if (size > 0 && currentGroup != null) {
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

                    }
                    DeviceType.SMART_CURTAIN -> {
                        showLoadingDialog(resources.getString(R.string.grouping_wait_tip,
                                selectCurtains.size.toString()))
                    }
                    DeviceType.SMART_RELAY -> {
                        showLoadingDialog(resources.getString(R.string.grouping_wait_tip,
                                selectRelays.size.toString()))

                    }
                }
                setGroup(deviceType)
                //将灯列表的灯循环设置分组
                setGroupForLights(deviceType)
            } else {
                Toast.makeText(mApplication, R.string.select_group_tip, Toast.LENGTH_SHORT).show()
            }
        } else {
            showToast(getString(R.string.selected_lamp_tip))
        }
    }

    @SuppressLint("CheckResult")
    private fun setGroup(deviceType: Int) {
        setDeviceTypeDataStopBlink(deviceType)
        disposableGroupTimer?.dispose()
        disposableGroupTimer = Observable.timer(7000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.group_fail_tip))
                }
        setGroupOneByOne(currentGroup!!, deviceType, 0)
    }

    private fun setDeviceTypeDataStopBlink(deviceType: Int) {
        when (deviceType) {
            DeviceType.LIGHT_RGB, DeviceType.LIGHT_NORMAL -> {
                showLoadingDialog(resources.getString(R.string.grouping_wait_tip, selectLights.size.toString()))
                for (i in selectLights.indices) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(selectLights[i].meshAddr, selectLights[i].belongGroupId)
                }
            }
            DeviceType.SMART_CURTAIN -> {
                showLoadingDialog(resources.getString(R.string.grouping_wait_tip, selectCurtains.size.toString()))
                for (curtain in selectCurtains) {//使用选中一个indices 出现java.lang.IndexOutOfBoundsException: Invalid index 0, size is 0
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(curtain.meshAddr, curtain.belongGroupId)
                }
            }
            DeviceType.SMART_RELAY -> {
                showLoadingDialog(resources.getString(R.string.grouping_wait_tip, selectRelays.size.toString()))
                for (i in selectRelays.indices) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(selectRelays[i].meshAddr, selectRelays[i].belongGroupId)
                }
            }
        }
    }

    private fun completeGroup(deviceType: Int) {
        hideLoadingDialog()
        when (deviceType) {
            DeviceType.LIGHT_RGB, DeviceType.LIGHT_NORMAL -> {
                //取消分组成功的勾选的灯
                for (i in selectLights.indices) {
                    val light = selectLights[i]
                    light.selected = false
                }
                lightAdapter.notifyItemRangeChanged(0, deviceData.size)
            }
            DeviceType.SMART_CURTAIN -> {
                //取消分组成功的勾选的灯
                for (i in selectCurtains.indices) {
                    val light = selectCurtains[i]
                    light.selected = false
                }
                curtainAdapter.notifyItemRangeChanged(0, deviceDataCurtain.size)
            }
            DeviceType.SMART_RELAY -> {
                //取消分组成功的勾选的灯
                for (i in selectRelays.indices) {
                    val light = selectRelays[i]
                    light.selected = false
                }
                relayAdapter.notifyItemRangeChanged(0, deviceDataRelay.size)
            }
        }

        if (currentGroup != null)
            DBUtils.updateGroup(currentGroup!!)//更新组类型

        SyncDataPutOrGetUtils.syncPutDataStart(this, object : SyncCallback {
            override fun start() {
                LogUtils.v("zcl更新start")
            }

            override fun complete() {
                LogUtils.v("zcl更新结束")
                hideLoadingDialog()
                clearSelectors()
                refreshData()
                initData()

            }

            override fun error(msg: String?) {
                LogUtils.v("zcl更新错误")
                hideLoadingDialog()
                clearSelectors()
                refreshData()
                initData()
            }
        })


        //grouping_completed.setText(R.string.complete)
    }

    private fun setGroupForLights(deviceType: Int) {
        when (deviceType) {
            DeviceType.LIGHT_RGB, DeviceType.LIGHT_NORMAL -> {
                for (i in selectLights) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(i.meshAddr, i.belongGroupId)
                }
            }
            DeviceType.SMART_CURTAIN -> {
                for (i in selectCurtains) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(i.meshAddr, i.belongGroupId)
                }
            }
            DeviceType.SMART_RELAY -> {
                for (i in selectRelays) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(i.meshAddr, i.belongGroupId)
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
    }

    private fun updateGroupResultRelay(light: DbConnector, group: DbGroup) {
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
        var deviceMeshAddr = 0
        var dbLight: DbLight? = null
        var dbCurtain: DbCurtain? = null
        var dbRelay: DbConnector? = null

        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                dbLight = selectLights[index]
                deviceMeshAddr = dbLight.meshAddr
                currentGroup?.deviceType = dbLight.productUUID.toLong()
            }
            DeviceType.SMART_CURTAIN -> {
                dbCurtain = selectCurtains[index]
                deviceMeshAddr = dbCurtain.meshAddr
                currentGroup?.deviceType = dbCurtain.productUUID.toLong()
            }
            DeviceType.SMART_RELAY -> {
                dbRelay = selectRelays[index]
                deviceMeshAddr = dbRelay.meshAddr
                currentGroup?.deviceType = dbRelay.productUUID.toLong()
            }
        }

        val successCallback: () -> Unit = {
            disposableGroupTimer?.dispose()
            when (deviceType) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                    dbLight?.belongGroupId = dbGroup.id
                    updateGroupResultLight(dbLight!!, dbGroup)
                    if (index + 1 > selectLights.size - 1)
                        completeGroup(deviceType)
                    else
                        setGroupOneByOne(dbGroup, deviceType, index + 1)
                }
                DeviceType.SMART_CURTAIN -> {
                    dbCurtain?.belongGroupId = dbGroup.id
                    updateGroupResultCurtain(dbCurtain!!, dbGroup)
                    if (index + 1 > selectCurtains.size - 1)
                        completeGroup(deviceType)
                    else
                        setGroupOneByOne(dbGroup, deviceType, index + 1)
                }
                DeviceType.SMART_RELAY -> {
                    dbRelay?.belongGroupId = dbGroup.id
                    updateGroupResultRelay(dbRelay!!, dbGroup)
                    if (index + 1 > selectRelays.size - 1)
                        completeGroup(deviceType)
                    else
                        setGroupOneByOne(dbGroup, deviceType, index + 1)
                }
            }
        }
        val failedCallback: () -> Unit = {
            disposableGroupTimer?.dispose()
            ToastUtils.showLong(R.string.group_fail_tip)
            when (deviceType) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                    dbLight?.belongGroupId = allLightId
                    updateGroupResultLight(dbLight!!, dbGroup)
                    if (TelinkLightApplication.getApp().connectDevice == null) {
                        ToastUtils.showLong(getString(R.string.connect_fail))
                    } else {
                        if (index + 1 > selectLights.size - 1)
                            completeGroup(deviceType)
                        else
                            setGroupOneByOne(dbGroup, deviceType, index + 1)
                    }
                }
                DeviceType.SMART_CURTAIN -> {
                    dbCurtain?.belongGroupId = allLightId
                    updateGroupResultCurtain(dbCurtain!!, dbGroup)
                    if (TelinkLightApplication.getApp().connectDevice == null) {
                        ToastUtils.showLong(getString(R.string.connect_fail))
                    } else {
                        if (index + 1 > selectCurtains.size - 1)
                            completeGroup(deviceType)
                        else
                            setGroupOneByOne(dbGroup, deviceType, index + 1)
                    }
                }
                DeviceType.SMART_RELAY -> {
                    dbRelay?.belongGroupId = allLightId
                    updateGroupResultRelay(dbRelay!!, dbGroup)
                    if (TelinkLightApplication.getApp().connectDevice == null) {
                        ToastUtils.showLong(getString(R.string.connect_fail))
                    } else {
                        if (index + 1 > selectRelays.size - 1)
                            completeGroup(deviceType)
                        else
                            setGroupOneByOne(dbGroup, deviceType, index + 1)
                    }
                }
            }

        }
        isChange = true
        Commander.addGroup(deviceMeshAddr, dbGroup.meshAddr, successCallback, failedCallback)
    }

    private fun stopBlink(meshAddr: Int, belongGroupId: Long) {
        val group: DbGroup
        val groupOfTheLight = DBUtils.getGroupByID(belongGroupId)
        group = when (groupOfTheLight) {
            null -> currentGroup ?: DbGroup()
            else -> groupOfTheLight
        }
        val groupAddress = group.meshAddr
        Log.d("zcl", "startBlink groupAddresss = $groupAddress")
        newStartBlinkOpcode(groupAddress, meshAddr, false)

        val disposable = mBlinkDisposables?.get(meshAddr)
        disposable?.dispose()
    }

    private fun isSelectDevice(deviceType: Int): Boolean {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                return selectLights.size > 0
            }
            DeviceType.SMART_CURTAIN -> {
                return selectCurtains.size > 0
            }
            DeviceType.SMART_RELAY -> {
                return selectRelays.size > 0
            }
        }
        return false
    }

    /**
     * 分组与未分组切换逻辑
     */
    private fun changeDeviceData() {
        batch_four_device_all.text = getString(R.string.select_all)
        setAllSelect(false)

        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                val size = deviceData.size
                // Inconsistency detected. Invalid item position 8(offset:40).state:32 android.support.v7.widget.RecyclerView
                deviceData.clear()
                lightAdapter.notifyItemRangeRemoved(0, size)
                if (checkedNoGrouped) {
                    batch_four_no_group.isChecked = true
                    deviceData.addAll(noGroup)
                } else {
                    batch_four_grouped.isChecked = true
                    deviceData.addAll(listGroup)
                }

                setTitleTexts(noGroup.size, listGroup.size)
                setDeviceListAndEmpty(deviceData.size)
                lightAdapter.notifyItemRangeInserted(0, deviceData.size)
            }
            DeviceType.SMART_CURTAIN -> {
                val size = deviceDataCurtain.size
                deviceDataCurtain.clear()
                lightAdapter.notifyItemRangeRemoved(0, size)

                if (checkedNoGrouped) {
                    batch_four_no_group.isChecked = true
                    deviceDataCurtain.addAll(noGroupCutain)
                } else {
                    batch_four_grouped.isChecked = true
                    deviceDataCurtain.addAll(listGroupCutain)
                }

                setTitleTexts(noGroupCutain.size, listGroupCutain.size)
                setDeviceListAndEmpty(deviceDataCurtain.size)
                curtainAdapter.notifyItemRangeInserted(0, deviceDataCurtain.size)
            }
            DeviceType.SMART_RELAY -> {
                val size = deviceDataRelay.size
                deviceDataRelay.clear()
                relayAdapter.notifyItemRangeRemoved(0, size)
                if (checkedNoGrouped) {
                    batch_four_no_group.isChecked = true
                    deviceDataRelay.addAll(noGroupRelay)
                } else {
                    batch_four_grouped.isChecked = true
                    deviceDataRelay.addAll(listGroupRelay)
                }

                setTitleTexts(noGroupRelay.size, listGroupRelay.size)
                setDeviceListAndEmpty(deviceDataRelay.size)
                relayAdapter.notifyItemRangeInserted(0, deviceDataRelay.size)
            }
        }
        changeTitleChecked()//改变title
    }

    @SuppressLint("StringFormatMatches")
    private fun setTitleTexts(noSize: Int?, haveSize: Int?) {
        batch_four_no_group.text = getString(R.string.no_group_device_num, noSize ?: 0)
        batch_four_grouped.text = getString(R.string.grouped_num, haveSize ?: 0)
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
        if (lastCheckedGroupPostion != position && lastCheckedGroupPostion != 1000) {
            groupsByDeviceType[lastCheckedGroupPostion].isCheckedInGroup = false
            groupAdapter.notifyItemRangeChanged(lastCheckedGroupPostion, 1)
        }

        lastCheckedGroupPostion = position
        groupsByDeviceType[position].isCheckedInGroup = !groupsByDeviceType[position].isCheckedInGroup

        currentGroup = if (groupsByDeviceType[position].isCheckedInGroup)
            groupsByDeviceType[position]
        else
            null
        groupAdapter.notifyItemRangeChanged(position, 1)

        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                changeGroupingCompleteState(selectLights.size)
            }
            DeviceType.SMART_CURTAIN -> {
                changeGroupingCompleteState(selectCurtains.size)
            }
            DeviceType.SMART_RELAY -> {
                changeGroupingCompleteState(selectRelays.size)
            }

        }
    }

    private fun addNewGroup() {
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(DBUtils.getDefaultNewGroupName())
        //设置光标默认在最后
        textGp.setSelection(textGp.text.toString().length)
        AlertDialog.Builder(this)
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

    fun refreshData() {
        val groupList = DBUtils.getGroupsByDeviceType(deviceType)
        //以下是检索组里有多少设备的代码
        for (group in groupList) {
            when (group.deviceType) {
                Constant.DEVICE_TYPE_LIGHT_NORMAL -> {
                    group.deviceCount = DBUtils.getLightByGroupID(group.id).size  //查询改组内设备数量  普通灯和冷暖灯是一个方法  查询什么设备类型有grouplist内容决定
                }
                Constant.DEVICE_TYPE_LIGHT_RGB -> {
                    group.deviceCount = DBUtils.getLightByGroupID(group.id).size  //查询改组内设备数量
                }
                Constant.DEVICE_TYPE_CONNECTOR -> {
                    group.deviceCount = DBUtils.getConnectorByGroupID(group.id).size  //查询改组内设备数量
                }
                Constant.DEVICE_TYPE_CURTAIN -> {
                    group.deviceCount = DBUtils.getCurtainByGroupID(group.id).size  //查询改组内设备数量//窗帘和传感器是一个方法
                }
            }
        }
    }

    private fun setAllSelect(isSelectAll: Boolean) {
        clearSelectors()//清理所有选中
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                for (i in deviceData.indices) {
                    deviceData[i].selected = isSelectAll
                    if (isSelectAll)
                        startBlink(deviceData[i].belongGroupId, deviceData[i].meshAddr)
                    else
                        stopBlink(deviceData[i].meshAddr, deviceData[i].belongGroupId)
                    LogUtils.v("zcl---全选${deviceData[i].selected}")
                }

                if (isSelectAll)
                    selectLights.addAll(deviceData)
                else
                    selectLights.removeAll(deviceData)

                lightAdapter.notifyItemRangeChanged(0, deviceData.size)
                changeGroupingCompleteState(selectLights.size)
            }
            DeviceType.SMART_CURTAIN -> {

                for (i in deviceDataCurtain.indices) {
                    deviceDataCurtain[i].selected = isSelectAll
                    if (isSelectAll)
                        startBlink(deviceDataCurtain[i].belongGroupId, deviceDataCurtain[i].meshAddr)
                    else
                        stopBlink(deviceDataCurtain[i].meshAddr, deviceDataCurtain[i].belongGroupId)
                }

                if (isSelectAll)
                    selectCurtains.addAll(deviceDataCurtain)
                else
                    selectCurtains.removeAll(deviceDataCurtain)

                curtainAdapter.notifyItemRangeChanged(0, deviceDataCurtain.size)
                changeGroupingCompleteState(selectCurtains.size)
            }
            DeviceType.SMART_RELAY -> {

                for (i in deviceDataRelay.indices) {
                    deviceDataRelay[i].selected = isSelectAll
                    if (isSelectAll)
                        startBlink(deviceDataRelay[i].belongGroupId, deviceDataRelay[i].meshAddr)
                    else
                        stopBlink(deviceDataRelay[i].meshAddr, deviceDataRelay[i].belongGroupId)
                }

                if (isSelectAll)
                    selectRelays.addAll(deviceDataRelay)
                else
                    selectRelays.removeAll(deviceDataRelay)

                relayAdapter.notifyItemRangeChanged(0, deviceDataRelay.size)

                changeGroupingCompleteState(selectRelays.size)
            }
        }

    }

    /**
     * 让灯开始闪烁
     */
    private fun startBlink(belongGroupId: Long, meshAddr: Int) {
        val group: DbGroup
        val groupOfTheLight = DBUtils.getGroupByID(belongGroupId)
        group = when (groupOfTheLight) {
            null -> currentGroup ?: DbGroup()
            else -> groupOfTheLight
        }
        val groupAddress = group.meshAddr
        Log.d("zcl", "startBlink groupAddresss = $groupAddress")

        if (isCompatible) {
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
        } else {
            newStartBlinkOpcode(groupAddress, meshAddr, true)
        }

    }

    private fun newStartBlinkOpcode(groupAddress: Int, meshAddr: Int, isStart: Boolean) {
        val opcode = Opcode.LIGHT_BLINK_ON_OFF
        val params: ByteArray
        if (isStart) {
            params = byteArrayOf(0x01, (groupAddress and 0xFF).toByte(), (groupAddress shr 8 and 0xFF).toByte())
            params[0] = 0x01
        } else {
            params = byteArrayOf(0x00, (groupAddress and 0xFF).toByte(), (groupAddress shr 8 and 0xFF).toByte())
            params[0] = 0x00
        }
        TelinkLightService.Instance().sendCommandNoResponse(opcode, meshAddr, params)
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
                if (!isScanning)
                    ToastUtils.showLong(getString(R.string.connect_success))
                changeDisplayImgOnToolbar(true)

                val connectDevice = this.mApplication?.connectDevice
                LogUtils.d("directly connection device meshAddr = ${connectDevice?.meshAddress}")
            }
            LightAdapter.STATUS_LOGOUT -> {
                changeDisplayImgOnToolbar(false)
                hideLoadingDialog()
                autoConnect()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
        this.mApplication?.removeEventListener(this)
        isAddGroupEmptyView = false
    }


    override fun onBackPressed() {
        super.onBackPressed()
        setDeviceTypeDataStopBlink(deviceType)
        checkNetworkAndSync(this)
        ToastUtils.showShort(getString(R.string.grouping_success_tip))
        finish()
    }
}