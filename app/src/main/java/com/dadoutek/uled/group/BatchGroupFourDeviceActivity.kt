package com.dadoutek.uled.group

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.le.ScanFilter
import android.graphics.Color
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
import com.blankj.utilcode.util.RomUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
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
import com.dadoutek.uled.util.*
import com.dadoutek.uled.widget.RecyclerGridDecoration
import com.polidea.rxandroidble2.scan.ScanSettings
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LeScanParameters
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_batch_group_four.*
import kotlinx.coroutines.Dispatchers
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
 * 更新描述
 */
class BatchGroupFourDeviceActivity : TelinkBaseActivity(), EventListener<String>, BaseQuickAdapter.OnItemLongClickListener, BaseQuickAdapter.OnItemClickListener {

    private var disposableScan: Disposable? = null
    private var disposableTimerResfresh: Disposable? = null
    private var scanningList: ArrayList<DeviceInfo>? = null
    private val deviceData: MutableList<DbLight> = mutableListOf()
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
    private val compositeDisposable = CompositeDisposable()
    private val SCAN_DELAY: Long = 1000       // 每次Scan之前的Delay , 1000ms比较稳妥。
    private val HUAWEI_DELAY: Long = 2000       // 华为专用Delay

    private val noGroup: MutableList<DbLight> = mutableListOf()
    private val listGroup: MutableList<DbLight> = mutableListOf()
    private val groupsByDeviceType: MutableList<DbGroup> = mutableListOf()

    private val noGroupCutain: MutableList<DbCurtain> = mutableListOf()
    private val listGroupCutain: MutableList<DbCurtain> = mutableListOf()
    private val deviceDataCurtain: MutableList<DbCurtain> = mutableListOf()

    private val deviceDataRelayAll = RxDeviceList<DbConnector>()
    private val deviceDataCurtainAll = RxDeviceList<DbCurtain>()
    private val deviceDataLightAll = RxDeviceList<DbLight>()

    private val noGroupRelay: MutableList<DbConnector> = mutableListOf()
    private val listGroupRelay: MutableList<DbConnector> = mutableListOf()
    private val deviceDataRelay: MutableList<DbConnector> = mutableListOf()

    private val groupAdapter: BatchGrouopEditListAdapter = BatchGrouopEditListAdapter(R.layout.batch_four_group_edit_item, groupsByDeviceType)

    private val lightAdapter: BatchFourLightAdapter = BatchFourLightAdapter(R.layout.batch_device_item, noGroup)
    private val lightGroupedAdapter: BatchFourLightAdapter = BatchFourLightAdapter(R.layout.batch_device_item, listGroup)

    private val relayAdapter: BatchFourRelayAdapter = BatchFourRelayAdapter(R.layout.batch_device_item, noGroupRelay)
    private val relayGroupedAdapter: BatchFourRelayAdapter = BatchFourRelayAdapter(R.layout.batch_device_item, listGroupRelay)

    private val curtainAdapter: BatchFourCurtainAdapter = BatchFourCurtainAdapter(R.layout.batch_device_item, noGroupCutain)
    private val curtainGroupedAdapter: BatchFourCurtainAdapter = BatchFourCurtainAdapter(R.layout.batch_device_item, listGroupCutain)

    private val allLightId: Long = 1//belongGroupId如果等于1则标识没有群组

    private var deviceType: Int = 100
    private var lastCheckedGroupPostion: Int = 1000
    private var retryConnectCount = 0
    private var connectMeshAddress: Int = 0
    private var mApplication: TelinkLightApplication? = null

    private var checkedNoGrouped: Boolean = true

    private val mBlinkDisposables = SparseArray<Disposable>()
    private var isAddGroupEmptyView: Boolean = false
    private var isComplete: Boolean = false

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
            ToastUtils.showLong(getString(R.string.grouping_success_tip))
            setDeviceTypeDataStopBlink(deviceType,false)
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

        batch_four_device_recycle_grouped.layoutManager = GridLayoutManager(this, 2)
        batch_four_device_recycle_grouped.addItemDecoration(RecyclerGridDecoration(this, 2))

        batch_four_group_recycle.layoutManager = GridLayoutManager(this, 2)
        batch_four_group_recycle.addItemDecoration(RecyclerGridDecoration(this, 2))

        lplong = batch_four_no_group.layoutParams as LinearLayout.LayoutParams
        lplong?.weight = 3f

        lpShort = batch_four_no_group_line_ly.layoutParams as LinearLayout.LayoutParams
        lpShort?.weight = 2f

        batch_four_compatible_mode.isChecked = true
        isCompatible = true

        deviceType = intent.getIntExtra(Constant.DEVICE_TYPE, 100)
        scanningList = intent.getParcelableArrayListExtra(Constant.DEVICE_NUM)

        //设置进度View下拉的起始点和结束点，scale 是指设置是否需要放大或者缩小动画
        swipe_refresh_ly.setProgressViewOffset(true, -0, 100)
        //设置进度View下拉的结束点，scale 是指设置是否需要放大或者缩小动画
        swipe_refresh_ly.setProgressViewEndTarget(true, 180)
        //设置进度View的组合颜色，在手指上下滑时使用第一个颜色，在刷新中，会一个个颜色进行切换
        swipe_refresh_ly.setColorSchemeColors(Color.BLACK, Color.GREEN, Color.RED, Color.YELLOW, Color.BLUE)
        //设置触发刷新的距离
        swipe_refresh_ly.setDistanceToTriggerSync(200)

        setAdapterAndSubscribleData()
        autoConnect()
    }

    @SuppressLint("StringFormatMatches")
    private fun setAdapterAndSubscribleData() {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                lightAdapter.bindToRecyclerView(batch_four_device_recycle)
                lightGroupedAdapter.bindToRecyclerView(batch_four_device_recycle_grouped)

                val observableNoGroup = deviceDataLightAll.getObservable { it.belongGroupId == allLightId }
                val disposableNoGrouped = observableNoGroup.subscribe({ list ->
                    sortList(list)
                    noGroup.clear()
                    noGroup.addAll(list)
                    if (checkedNoGrouped)
                        setDeviceListAndEmpty(noGroup.size)
                    setTitleTexts(noGroup.size, listGroup.size)
                    batch_four_no_group.text = getString(R.string.no_group_device_num, noGroup.size)
                    lightAdapter.notifyDataSetChanged()
                }, {
                    LogUtils.e("zcl---订阅频道${it.localizedMessage}")
                })
                compositeDisposable.add(disposableNoGrouped)

                val observableGrouped = deviceDataLightAll.getObservable { it.belongGroupId != allLightId }
                val disposableGrouped = observableGrouped.subscribe({ list ->
                    sortList(list)
                    listGroup.clear()
                    listGroup.addAll(list)
                    if (!checkedNoGrouped)
                        setDeviceListAndEmpty(listGroup.size)
                    setTitleTexts(noGroup.size, listGroup.size)
                    lightGroupedAdapter.notifyDataSetChanged()
                }, {
                    LogUtils.e("zcl---订阅频道${it.localizedMessage}")
                })
                compositeDisposable.add(disposableGrouped)
            }
            DeviceType.SMART_CURTAIN -> {
                curtainAdapter.bindToRecyclerView(batch_four_device_recycle)
                curtainGroupedAdapter.bindToRecyclerView(batch_four_device_recycle_grouped)//默认是隐藏的显示未分组的界面

                val observableNoGroup = deviceDataCurtainAll.getObservable { it.belongGroupId == allLightId }
                val disposableNoGrouped = observableNoGroup.subscribe({ list ->
                    sortCurtain(list)
                    noGroupCutain.clear()
                    noGroupCutain.addAll(list)
                    if (checkedNoGrouped)
                        setDeviceListAndEmpty(noGroupCutain.size)
                    setTitleTexts(noGroupCutain.size, listGroupCutain.size)
                    curtainAdapter.notifyDataSetChanged()
                }, {
                    LogUtils.e("zcl---订阅频道${it.localizedMessage}")
                })
                compositeDisposable.add(disposableNoGrouped)

                val observableGrouped = deviceDataCurtainAll.getObservable { it.belongGroupId != allLightId }
                val disposableGrouped = observableGrouped.subscribe({ list ->
                    sortCurtain(list)
                    listGroupCutain.clear()
                    listGroupCutain.addAll(list)
                    if (!checkedNoGrouped)
                        setDeviceListAndEmpty(listGroupCutain.size)
                    setTitleTexts(noGroupCutain.size, listGroupCutain.size)
                    curtainGroupedAdapter.notifyDataSetChanged()
                }, {
                    LogUtils.e("zcl---订阅频道${it.localizedMessage}")
                })
                compositeDisposable.add(disposableGrouped)
            }
            DeviceType.SMART_RELAY -> {
                relayAdapter.bindToRecyclerView(batch_four_device_recycle)
                relayGroupedAdapter.bindToRecyclerView(batch_four_device_recycle_grouped)

                val observableNoGroup = deviceDataRelayAll.getObservable { it.belongGroupId == allLightId }
                val disposableNoGrouped = observableNoGroup.subscribe({ list ->
                    sortRelay(list)
                    noGroupRelay.clear()
                    noGroupRelay.addAll(list)
                    if (checkedNoGrouped)
                        setDeviceListAndEmpty(noGroupRelay.size)
                    setTitleTexts(noGroupRelay.size, listGroupRelay.size)
                    relayAdapter.notifyDataSetChanged()
                }, {
                    LogUtils.e("zcl---订阅频道${it.localizedMessage}")
                })
                compositeDisposable.add(disposableNoGrouped)

                val observableGrouped = deviceDataRelayAll.getObservable { it.belongGroupId != allLightId }
                val disposableGrouped = observableGrouped.subscribe({ list ->
                    sortRelay(list)
                    listGroupRelay.clear()
                    listGroupRelay.addAll(list)
                    if (!checkedNoGrouped)
                        setDeviceListAndEmpty(listGroupRelay.size)
                    setTitleTexts(noGroupRelay.size, listGroupRelay.size)
                    relayGroupedAdapter.notifyDataSetChanged()
                }, {
                    LogUtils.e("zcl---订阅频道${it.localizedMessage}")
                })
                compositeDisposable.add(disposableGrouped)
            }
        }
        groupAdapter.bindToRecyclerView(batch_four_group_recycle)
    }

    private fun sortRelay(list: List<DbConnector>?) {
        Collections.sort(list) { o1, o2 ->
            o2.rssi - o1.rssi
        }
    }

    private fun sortCurtain(list: List<DbCurtain>?) {
        Collections.sort(list) { o1, o2 ->
            o2.rssi - o1.rssi
        }
    }

    private fun sortList(list: List<DbLight>?) {
        Collections.sort(list) { o1, o2 ->
            o2.rssi - o1.rssi
        }
    }

    private fun initData() {
        refreshData()
        setDevicesData()
        setGroupData()
    }

    /**
     * 设置组数据
     */
    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    private fun setGroupData() {
        groupsByDeviceType.clear()
        groupsByDeviceType.addAll(DBUtils.getGroupsByDeviceType(deviceType))
        groupsByDeviceType.addAll(DBUtils.getGroupsByDeviceType(0))

        if (!isAddGroupEmptyView)
            groupAdapter.emptyView = emptyGroupView

        for (gp in groupsByDeviceType)
            gp.isCheckedInGroup = currentGroup != null && currentGroup!!.id == gp.id

        isAddGroupEmptyView = true
        groupAdapter.notifyItemRangeChanged(0, groupsByDeviceType.size)

        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                changeGroupingCompleteState(deviceDataLightAll.filter { it.isSelected }.size, noGroup.size)
            }
            DeviceType.SMART_CURTAIN -> {
                changeGroupingCompleteState(deviceDataCurtainAll.filter { it.isSelected }.size, noGroupCutain.size)
            }
            DeviceType.SMART_RELAY -> {
                changeGroupingCompleteState(deviceDataRelayAll.filter { it.isSelected }.size, noGroupRelay.size)
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
                        ToastUtils.showLong(getString(R.string.rename_tip_check))
                    } else {
                        groupsByDeviceType?.get(position)?.name = textGp.text.toString().trim { it <= ' ' }
                        val group = groupsByDeviceType?.get(position)
                        if (group != null)
                            DBUtils.updateGroup(group)
                        groupAdapter?.notifyItemRangeChanged(position, 1)
                        when (deviceType) {
                            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                                for (device in listGroup) {
                                    if (device.belongGroupId == group.id)
                                        device.groupName = group.name
                                }
                                lightGroupedAdapter.notifyDataSetChanged()
                            }
                            DeviceType.SMART_CURTAIN -> {
                                for (device in listGroupCutain) {
                                    if (device.belongGroupId == group.id)
                                        device.groupName = group.name
                                }
                                curtainGroupedAdapter.notifyDataSetChanged()
                            }
                            DeviceType.SMART_RELAY -> {
                                for (device in listGroupRelay) {
                                    if (device.belongGroupId == group.id)
                                        device.groupName = group.name
                                }
                                relayGroupedAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()
    }

    /**
     * 设置设备数据
     */
    @SuppressLint("StringFormatInvalid")
    private fun setDevicesData() {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL -> {
                deviceDataLightAll.clear()
                if (scanningList == null)
                    deviceDataLightAll.addAll(DBUtils.getAllNormalLight())
                else {
                    for (item in scanningList!!) {
                        val light = DbLight()
                        light.macAddr = item.macAddress
                        light.meshAddr = item.meshAddress
                        light.productUUID = item.productUUID
                        light.meshUUID = item.meshUUID
                        light.belongGroupId = allLightId
                        light.name = getString(R.string.device_name) + light.meshAddr
                        light.rssi = item.rssi
                        deviceDataLightAll.add(light)
                    }
                }
            }
            DeviceType.LIGHT_RGB -> {
                deviceDataLightAll.clear()
                if (scanningList == null)
                    deviceDataLightAll.addAll(DBUtils.getAllRGBLight())
                else for (item in scanningList!!) {
                    val light = DbLight()
                    light.macAddr = item.macAddress
                    light.meshAddr = item.meshAddress
                    light.productUUID = item.productUUID
                    light.meshUUID = item.meshUUID
                    light.belongGroupId = allLightId
                    light.name = getString(R.string.device_name) + light.meshAddr
                    light.rssi = item.rssi
                    deviceDataLightAll.add(light)
                }

            }
            DeviceType.SMART_CURTAIN -> {
                deviceDataCurtainAll.clear()
                if (scanningList == null)
                    deviceDataCurtainAll.addAll(DBUtils.getAllCurtains())
                else for (item in scanningList!!) {
                    val device = DbCurtain()
                    device.macAddr = item.macAddress
                    device.meshAddr = item.meshAddress
                    device.productUUID = item.productUUID
                    device.belongGroupId = allLightId
                    device.name = getString(R.string.device_name) + device.meshAddr
                    device.rssi = item.rssi
                    deviceDataCurtainAll.add(device)
                }
            }
            DeviceType.SMART_RELAY -> {
                deviceDataRelayAll.clear()
                if (scanningList == null)
                    deviceDataRelayAll.addAll(DBUtils.getAllRelay())
                else
                    for (item in scanningList!!) {
                        val device = DbConnector()
                        device.macAddr = item.macAddress
                        device.meshAddr = item.meshAddress
                        device.productUUID = item.productUUID
                        device.belongGroupId = allLightId
                        device.name = getString(R.string.device_name) + device.meshAddr
                        device.rssi = item.rssi
                        deviceDataRelayAll.add(device)
                    }
            }
        }
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
                            ToastUtils.showLong(getString(R.string.rename_tip_check))
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
                            ToastUtils.showLong(getString(R.string.rename_tip_check))
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
                        ToastUtils.showLong(getString(R.string.rename_tip_check))
                    } else {
                        light?.name = textGp.text.toString().trim { it <= ' ' }
                        if (light != null) {
                            DBUtils.updateLight(light)
                            changeSorceData(light.id)
                        }
                        // changeDeviceData()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()
    }

    private fun changeSorceData(id: Long?) {
        if (id != null)
            when (deviceType) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                    val light = DBUtils.getLightByID(id)
                    deviceDataLightAll.remove(light)
                    deviceDataLightAll.add(light!!)
                    /* for (device in deviceDataLightAll) {
                     if (light?.id == device.id){
                         device.name = light?.name
                         target = device
                     }
                 }*/

                }
                DeviceType.SMART_CURTAIN -> {

                    val curtain = DBUtils.getCurtainByID(id)
                    deviceDataCurtainAll.remove(curtain)
                    deviceDataCurtainAll.add(curtain!!)
                    /*   for (device in deviceDataCurtainAll) {
                           if (curtain?.id == device.id)
                               device.name = curtain?.name
                       }*/
                }
                DeviceType.SMART_RELAY -> {
                    val connector = DBUtils.getConnectorByID(id)
                    deviceDataRelayAll.remove(connector)
                    deviceDataRelayAll.add(connector!!)
                    /*  for (device in deviceDataRelayAll) {
                          if (connector?.id == device.id)
                              device.name = connector?.name
                      }*/
                }
            }
    }

    private fun setDeviceListAndEmpty(size: Int?) {
        if (size ?: 0 <= 0) {
            batch_four_device_recycle.visibility = View.GONE
            batch_four_device_recycle_grouped.visibility = View.GONE

            image_no_device.visibility = View.VISIBLE
        } else {
            if (checkedNoGrouped) {
                batch_four_device_recycle.visibility = View.VISIBLE
                batch_four_device_recycle_grouped.visibility = View.GONE
            } else {
                batch_four_device_recycle_grouped.visibility = View.VISIBLE
                batch_four_device_recycle.visibility = View.GONE
            }
            image_no_device.visibility = View.GONE
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
                                    ToastUtils.showLong(getString(R.string.connecting_tip))
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

                                        TelinkLightService.Instance()?.autoConnect(connectParams)
                                    }
                                }
                            }, { LogUtils.d(it) })
                compositeDisposable.add(disposable!!)

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


    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        if (checkedNoGrouped)
            when (deviceType) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                    noGroup[position].selected = !noGroup[position].selected
                    if (noGroup[position].isSelected) {
                        startBlink(noGroup[position].belongGroupId, noGroup[position].meshAddr)
                    } else {
                        stopBlink(noGroup[position].meshAddr, noGroup[position].belongGroupId)
                    }
                    changeGroupingCompleteState(deviceDataLightAll.filter { it.isSelected }.size, noGroup.size)
                    lightAdapter.notifyItemRangeChanged(0, noGroup.size)
                    LogUtils.v("zcl更新后状态${noGroup[position].selected}")
                }

                DeviceType.SMART_CURTAIN -> {
                    noGroupCutain[position].selected = !noGroupCutain[position].selected
                    if (noGroupCutain[position].isSelected) {
                        startBlink(noGroupCutain[position].belongGroupId, noGroupCutain[position].meshAddr)
                    } else {
                        stopBlink(noGroupCutain[position].meshAddr, noGroupCutain[position].belongGroupId)
                    }
                    changeGroupingCompleteState(deviceDataCurtainAll.filter { it.isSelected }.size, noGroupCutain.size)
                    curtainAdapter.notifyItemRangeChanged(0, noGroupCutain.size)
                    LogUtils.v("zcl更新后状态${noGroupCutain[position].selected}")
                }
                DeviceType.SMART_RELAY -> {
                    noGroupRelay[position].selected = !noGroupRelay[position].selected
                    if (noGroupRelay[position].isSelected) {
                        startBlink(noGroupRelay[position].belongGroupId, noGroupRelay[position].meshAddr)
                    } else {
                        stopBlink(noGroupRelay[position].meshAddr, noGroupRelay[position].belongGroupId)
                    }
                    changeGroupingCompleteState(deviceDataRelayAll.filter { it.isSelected }.size, noGroupRelay.size)
                    relayAdapter.notifyItemRangeChanged(0, noGroupRelay.size)
                    LogUtils.v("zcl更新后状态${noGroupRelay[position].selected}")
                }
            }
        else
            when (deviceType) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                    listGroup[position].selected = !listGroup[position].selected
                    if (listGroup[position].isSelected) {
                        startBlink(listGroup[position].belongGroupId, listGroup[position].meshAddr)
                    } else {
                        stopBlink(listGroup[position].meshAddr, listGroup[position].belongGroupId)
                    }
                    changeGroupingCompleteState(deviceDataLightAll.filter { it.isSelected }.size, noGroup.size)
                    lightGroupedAdapter.notifyItemRangeChanged(0, listGroup.size)
                    LogUtils.v("zcl更新后状态${listGroup[position].selected}")
                }

                DeviceType.SMART_CURTAIN -> {
                    listGroupCutain[position].selected = !listGroupCutain[position].selected
                    if (listGroupCutain[position].isSelected) {
                        startBlink(listGroupCutain[position].belongGroupId, listGroupCutain[position].meshAddr)
                    } else {
                        stopBlink(listGroupCutain[position].meshAddr, listGroupCutain[position].belongGroupId)
                    }
                    changeGroupingCompleteState(deviceDataCurtainAll.filter { it.isSelected }.size, noGroupCutain.size)
                    curtainGroupedAdapter.notifyItemRangeChanged(0, listGroupCutain.size)
                    LogUtils.v("zcl更新后状态${listGroupCutain[position].selected}")
                }
                DeviceType.SMART_RELAY -> {
                    listGroupRelay[position].selected = !listGroupRelay[position].selected
                    if (listGroupRelay[position].isSelected) {
                        startBlink(listGroupRelay[position].belongGroupId, listGroupRelay[position].meshAddr)
                    } else {
                        stopBlink(listGroupRelay[position].meshAddr, listGroupRelay[position].belongGroupId)
                    }
                    changeGroupingCompleteState(deviceDataRelayAll.filter { it.isSelected }.size, noGroupRelay.size)
                    relayGroupedAdapter.notifyItemRangeChanged(0, listGroupRelay.size)
                    LogUtils.v("zcl更新后状态${listGroupRelay[position].selected}")
                }
            }
    }

    override fun onItemLongClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int): Boolean {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                if (checkedNoGrouped)
                    renameLight(noGroup[position])
                else
                    renameLight(listGroup[position])
            }
            DeviceType.SMART_CURTAIN -> {
                if (checkedNoGrouped)
                    renameCurtain(noGroupCutain[position])
                else
                    renameCurtain(listGroupCutain[position])
            }
            DeviceType.SMART_RELAY -> {
                if (checkedNoGrouped)
                    renameConnector(noGroupRelay[position])
                else
                    renameConnector(listGroupRelay[position])
            }
        }
        return false
    }

    private fun initListener() {
        //先取消，这样可以确保不会重复添加监听
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)

        swipe_refresh_ly.setOnRefreshListener {
            findMeshDevice(DBUtils.lastUser?.controlMeshName)
            disposableTimerResfresh?.dispose()
            disposableTimerResfresh = Observable.timer(4000, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        swipe_refresh_ly.isRefreshing = false
                        disposableScan?.dispose()
                    }
            compositeDisposable.add(disposableTimerResfresh!!)
        }
        batch_four_compatible_mode.setOnCheckedChangeListener { _, isChecked ->
            isCompatible = isChecked
            if (!isCompatible)
                ToastUtils.showLong(getString(R.string.compatibility_mode))
            when (deviceType) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                    for (db in deviceDataLightAll.filter { it.isSelected })//获取组名 将分组与未分组的拆分
                        startBlink(db.belongGroupId, db.meshAddr)
                }
                DeviceType.SMART_CURTAIN -> {
                    for (db in deviceDataCurtainAll.filter { it.isSelected }) //获取组名 将分组与未分组的拆分
                        startBlink(db.belongGroupId, db.meshAddr)
                }

                DeviceType.SMART_RELAY -> {
                    for (db in deviceDataRelayAll.filter { it.isSelected })//获取组名 将分组与未分组的拆分
                        startBlink(db.belongGroupId, db.meshAddr)
                }
            }
            LogUtils.v("zcl是否选中$isChecked")
        }
        batch_four_group_rg.setOnCheckedChangeListener { _, checkedId ->
            checkedNoGrouped = checkedId == batch_four_no_group.id
            switchNoGroupOrGroupedAndSetEmpty()
            changeTitleChecked()
            batch_four_device_all.text = getString(R.string.select_all)
            setAllSelect(false)
        }
        batch_four_device_all.setOnClickListener { changeDeviceAll() }
        batch_four_group_add_group.setOnClickListener { addNewGroup() }
        grouping_completed.setOnClickListener {
            if (!isComplete){
            if (TelinkLightApplication.getApp().connectDevice != null)
                sureGroups()
            else
                autoConnect()
            }else{
                finish()
            }
        }
        groupAdapter.setOnItemClickListener { _, _, position ->
            //如果点中的不是上次选中的组则恢复未选中状态
            changeGroupSelectView(position)
        }
        groupAdapter.setOnItemLongClickListener { _, _, position ->
            showGroupForUpdateNameDialog(position)
            false
        }
        btnAddGroups?.setOnClickListener { addNewGroup() }

        //如果adapter内部使用了addLongclicklistener 那么外部必须使用longchildren否则addLongClick无效
        lightAdapter.onItemClickListener = this
        relayAdapter.onItemClickListener = this
        curtainAdapter.onItemClickListener = this
        lightGroupedAdapter.onItemClickListener = this
        relayGroupedAdapter.onItemClickListener = this
        curtainGroupedAdapter.onItemClickListener = this

        lightAdapter.onItemLongClickListener = this
        relayAdapter.onItemLongClickListener = this
        curtainAdapter.onItemLongClickListener = this

        lightGroupedAdapter.onItemLongClickListener = this
        relayGroupedAdapter.onItemLongClickListener = this
        curtainGroupedAdapter.onItemLongClickListener = this
    }

    private fun switchNoGroupOrGroupedAndSetEmpty() {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                for (light in deviceDataLightAll)
                    light.selected = false
                if (checkedNoGrouped)
                    setDeviceListAndEmpty(noGroup.size)
                else
                    setDeviceListAndEmpty(listGroup.size)
            }
            DeviceType.SMART_CURTAIN -> {
                for (curtain in deviceDataCurtainAll)
                    curtain.selected = false
                if (checkedNoGrouped)
                    setDeviceListAndEmpty(noGroupCutain.size)
                else
                    setDeviceListAndEmpty(listGroupCutain.size)
            }
            DeviceType.SMART_RELAY -> {
                for (relay in deviceDataRelayAll)
                    relay.selected = false

                if (checkedNoGrouped)
                    setDeviceListAndEmpty(noGroupRelay.size)
                else
                    setDeviceListAndEmpty(listGroupRelay.size)
            }
        }
    }

    private fun changeGroupingCompleteState(selectSize: Int, noGroupSize: Int) {

        if (selectSize > 0 && currentGroup != null) {//选中分组并且有选中设备的情况下
            grouping_completed.setBackgroundResource(R.drawable.btn_rec_blue_bt)
            isComplete = false
            grouping_completed.isClickable = true
            grouping_completed.text = getString(R.string.set_group)
        } else {//没有选中设备或者未选中分组的情况下
            if (noGroupSize > 0 || selectSize > 0 || currentGroup != null) {
                //有未分组的设备或者有选中设备或者选中组但是未达到分组条件的情况下
                isComplete = false
                grouping_completed.text = getString(R.string.set_group)
                grouping_completed.isClickable = false
                grouping_completed.setBackgroundResource(R.drawable.btn_rec_black_bt)
            } else {//既没有选中设备有没有选中分组还没有未分组设备的情况下显示完成
                isComplete = true
                grouping_completed.isClickable = true
                grouping_completed.setBackgroundResource(R.drawable.btn_rec_black_bt)
                grouping_completed.text = getString(R.string.complete)
            }
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
                                deviceDataLightAll.filter { it.isSelected }.size.toString()))

                    }
                    DeviceType.SMART_CURTAIN -> {
                        showLoadingDialog(resources.getString(R.string.grouping_wait_tip,
                                deviceDataCurtainAll.filter { it.isSelected }.size.toString()))
                    }
                    DeviceType.SMART_RELAY -> {
                        showLoadingDialog(resources.getString(R.string.grouping_wait_tip,
                                deviceDataRelayAll.filter { it.isSelected }.size.toString()))

                    }
                }
                //将灯列表的灯循环设置分组
                setGroup(deviceType)
                setDeviceStopBlink(deviceType)
            } else {
                Toast.makeText(mApplication, R.string.select_group_tip, Toast.LENGTH_SHORT).show()
            }
        } else {
            showToast(getString(R.string.selected_lamp_tip))
        }
    }

    @SuppressLint("CheckResult")
    private fun setGroup(deviceType: Int) {
        setDeviceTypeDataStopBlink(deviceType, false)
        disposableGroupTimer?.dispose()
        disposableGroupTimer = Observable.timer(7000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideLoadingDialog()
                    ToastUtils.showLong(getString(R.string.group_fail_tip))
                }
        setGroupOneByOne(currentGroup!!, deviceType, 0)
    }

    private fun setDeviceTypeDataStopBlink(deviceType: Int, b: Boolean = true) {
        when (deviceType) {
            DeviceType.LIGHT_RGB, DeviceType.LIGHT_NORMAL -> {
                if (b)
                showLoadingDialog(resources.getString(R.string.grouping_wait_tip, deviceDataLightAll.filter { it.isSelected }.size.toString()))
                else
                  showLoadingDialog(getString(R.string.please_wait))
                for (light in deviceDataLightAll.filter { it.isSelected }) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(light.meshAddr, light.belongGroupId)
                }
            }
            DeviceType.SMART_CURTAIN -> {
                if (b)
                    showLoadingDialog(resources.getString(R.string.grouping_wait_tip, deviceDataCurtainAll.filter { it.isSelected }.size.toString()))
                else
                    showLoadingDialog(getString(R.string.please_wait))

                for (curtain in deviceDataCurtainAll.filter { it.isSelected }) {//使用选中一个indices 出现java.lang.IndexOutOfBoundsException: Invalid index 0, size is 0
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(curtain.meshAddr, curtain.belongGroupId)
                }
            }
            DeviceType.SMART_RELAY -> {
                if (b)
                    showLoadingDialog(resources.getString(R.string.grouping_wait_tip, deviceDataRelayAll.filter { it.isSelected }.size.toString()))
                else
                    showLoadingDialog(getString(R.string.please_wait))


                for (relay in deviceDataRelayAll.filter { it.isSelected }) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(relay.meshAddr, relay.belongGroupId)
                }
            }
        }
    }

    private fun completeGroup(deviceType: Int) {
        hideLoadingDialog()
        when (deviceType) {
            DeviceType.LIGHT_RGB, DeviceType.LIGHT_NORMAL -> {
                //取消分组成功的勾选的灯
                for (i in deviceDataLightAll.filter { it.isSelected })
                    i.selected = false
            }
            DeviceType.SMART_CURTAIN -> {
                //取消分组成功的勾选的灯
                for (i in deviceDataCurtainAll.filter { it.isSelected })
                    i.selected = false
            }
            DeviceType.SMART_RELAY -> {
                //取消分组成功的勾选的灯
                for (i in deviceDataRelayAll.filter { it.isSelected })
                    i.selected = false
            }
        }

        if (currentGroup != null)
            DBUtils.updateGroup(currentGroup!!)//更新组类型
        refreshData()
        setGroupData()
        changeSourceData()
        SyncDataPutOrGetUtils.syncPutDataStart(this, object : SyncCallback {
            override fun start() {}

            override fun complete() {
                hideLoadingDialog()
            }

            override fun error(msg: String?) {
                hideLoadingDialog()
            }
        })
    }

    private fun changeSourceData() {
        when (deviceType) {
            DeviceType.LIGHT_RGB, DeviceType.LIGHT_NORMAL -> {
                val list = mutableListOf<DbLight>()
                list.addAll(deviceDataLightAll)
                deviceDataLightAll.clear()
                deviceDataLightAll.addAll(list)
            }
            DeviceType.SMART_CURTAIN -> {
                val list = mutableListOf<DbCurtain>()
                list.addAll(deviceDataCurtainAll)
                deviceDataCurtainAll.clear()
                deviceDataCurtainAll.addAll(list)
            }
            DeviceType.SMART_RELAY -> {
                val list = mutableListOf<DbConnector>()
                list.addAll(deviceDataRelayAll)
                deviceDataRelayAll.clear()
                deviceDataRelayAll.addAll(list)
            }
        }
    }

    private fun setDeviceStopBlink(deviceType: Int) {
        when (deviceType) {
            DeviceType.LIGHT_RGB, DeviceType.LIGHT_NORMAL -> {
                for (i in deviceDataLightAll.filter { it.isSelected }) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(i.meshAddr, i.belongGroupId)
                }
            }
            DeviceType.SMART_CURTAIN -> {
                for (i in deviceDataCurtainAll.filter { it.isSelected }) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(i.meshAddr, i.belongGroupId)
                }
            }
            DeviceType.SMART_RELAY -> {
                for (i in deviceDataRelayAll.filter { it.isSelected }) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(i.meshAddr, i.belongGroupId)
                }
            }
        }
    }

    private fun updateGroupResultLight(light: DbLight, group: DbGroup) {
        for (db in deviceDataLightAll.filter { it.isSelected }) {
            if (light.meshAddr == db.meshAddr) {
                if (light.belongGroupId != allLightId) {
                    db.hasGroup = true
                    db.belongGroupId = group.id
                    db.name = light.name
                    DBUtils.updateLight(light)
                } else {
                    db.hasGroup = false
                }
            }
        }
    }

    private fun updateGroupResultCurtain(light: DbCurtain, group: DbGroup) {
        for (i in deviceDataCurtainAll.filter { it.isSelected }.indices) {
            if (light.meshAddr == deviceDataCurtainAll.filter { it.isSelected }[i].meshAddr) {
                if (light.belongGroupId != allLightId) {
                    deviceDataCurtainAll.filter { it.isSelected }[i].hasGroup = true
                    deviceDataCurtainAll.filter { it.isSelected }[i].belongGroupId = group.id
                    deviceDataCurtainAll.filter { it.isSelected }[i].name = light.name
                    DBUtils.updateCurtain(light)
                } else {
                    deviceDataCurtainAll.filter { it.isSelected }[i].hasGroup = false
                }
            }
        }
    }

    private fun updateGroupResultRelay(light: DbConnector, group: DbGroup) {
        for (i in deviceDataRelayAll.filter { it.isSelected }.indices) {
            if (light.meshAddr == deviceDataRelayAll.filter { it.isSelected }[i].meshAddr) {
                if (light.belongGroupId != allLightId) {
                    deviceDataRelayAll.filter { it.isSelected }[i].hasGroup = true
                    deviceDataRelayAll.filter { it.isSelected }[i].belongGroupId = group.id
                    deviceDataRelayAll.filter { it.isSelected }[i].name = light.name
                    DBUtils.updateConnector(light)
                } else {
                    deviceDataRelayAll.filter { it.isSelected }[i].hasGroup = false
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
                dbLight = deviceDataLightAll.filter { it.isSelected }[index]
                deviceMeshAddr = dbLight.meshAddr
                currentGroup?.deviceType = dbLight.productUUID.toLong()
            }
            DeviceType.SMART_CURTAIN -> {
                dbCurtain = deviceDataCurtainAll.filter { it.isSelected }[index]
                deviceMeshAddr = dbCurtain.meshAddr
                currentGroup?.deviceType = dbCurtain.productUUID.toLong()
            }
            DeviceType.SMART_RELAY -> {
                dbRelay = deviceDataRelayAll.filter { it.isSelected }[index]
                deviceMeshAddr = dbRelay.meshAddr
                currentGroup?.deviceType = dbRelay.productUUID.toLong()
            }
        }

        val successCallback: () -> Unit = {
            when (deviceType) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                    deviceDataLightAll.filter { it.isSelected }[index].groupName = currentGroup?.name
                }
                DeviceType.SMART_CURTAIN -> {
                    deviceDataCurtainAll.filter { it.isSelected }[index].groupName = currentGroup?.name
                }
                DeviceType.SMART_RELAY -> {
                    deviceDataRelayAll.filter { it.isSelected }[index].groupName = currentGroup?.name
                }
            }

            disposableGroupTimer?.dispose()
            when (deviceType) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                    dbLight?.belongGroupId = dbGroup.id
                    updateGroupResultLight(dbLight!!, dbGroup)
                    if (index + 1 > deviceDataLightAll.filter { it.isSelected }.size - 1)
                        completeGroup(deviceType)
                    else
                        setGroupOneByOne(dbGroup, deviceType, index + 1)
                }
                DeviceType.SMART_CURTAIN -> {
                    dbCurtain?.belongGroupId = dbGroup.id
                    updateGroupResultCurtain(dbCurtain!!, dbGroup)
                    if (index + 1 > deviceDataCurtainAll.filter { it.isSelected }.size - 1)
                        completeGroup(deviceType)
                    else
                        setGroupOneByOne(dbGroup, deviceType, index + 1)
                }
                DeviceType.SMART_RELAY -> {
                    dbRelay?.belongGroupId = dbGroup.id
                    updateGroupResultRelay(dbRelay!!, dbGroup)
                    if (index + 1 > deviceDataRelayAll.filter { it.isSelected }.size - 1)
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
                    //dbLight?.belongGroupId = allLightId  因为有些本身已经分组所以再次分失败不能
                    // 更改他的belongGroupId
                    //updateGroupResultLight(dbLight!!, dbGroup)
                    if (TelinkLightApplication.getApp().connectDevice == null) {
                        ToastUtils.showLong(getString(R.string.connect_fail))
                    } else {
                        if (index + 1 > deviceDataLightAll.filter { it.isSelected }.size - 1)
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
                        if (index + 1 > deviceDataCurtainAll.filter { it.isSelected }.size - 1)
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
                        if (index + 1 > deviceDataRelayAll.filter { it.isSelected }.size - 1)
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
                return deviceDataLightAll.run { any { it.isSelected } }
            }
            DeviceType.SMART_CURTAIN -> {
                return deviceDataCurtainAll.run { any { it.isSelected } }
            }
            DeviceType.SMART_RELAY -> {
                return deviceDataRelayAll.run { filter { it.isSelected }.isNotEmpty() }
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

    /**
     * 全选与取消功能相关
     */
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
                changeGroupingCompleteState(deviceDataLightAll.filter { it.isSelected }.size, noGroup.size)
            }
            DeviceType.SMART_CURTAIN -> {
                changeGroupingCompleteState(deviceDataCurtainAll.filter { it.isSelected }.size, noGroupCutain.size)
            }
            DeviceType.SMART_RELAY -> {
                changeGroupingCompleteState(deviceDataRelayAll.filter { it.isSelected }.size, noGroupRelay.size)
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
                        ToastUtils.showLong(getString(R.string.rename_tip_check))
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
                        setGroupData()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()
    }

    fun refreshData() {
        batch_four_device_all.text = getString(R.string.select_all)
        setAllSelect(false)//还原状态

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
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                if (checkedNoGrouped) {
                    for (device in noGroup) {
                        for (sourceDevice in deviceDataLightAll) {
                            if (device.id == sourceDevice.id) {
                                sourceDevice.selected = isSelectAll
                                device.selected = isSelectAll
                                if (isSelectAll)
                                    startBlink(device.belongGroupId, device.meshAddr)
                                else
                                    stopBlink(device.meshAddr, device.belongGroupId)
                            }
                        }
                    }
                    changeGroupingCompleteState(noGroup.filter { it.isSelected }.size, noGroup.size)
                    lightAdapter.notifyDataSetChanged()
                } else {
                    for (device in listGroup) {
                        for (sourceDevice in deviceDataLightAll) {
                            if (device.id == sourceDevice.id) {
                                sourceDevice.selected = isSelectAll
                                device.selected = isSelectAll
                                if (isSelectAll)
                                    startBlink(device.belongGroupId, device.meshAddr)
                                else
                                    stopBlink(device.meshAddr, device.belongGroupId)
                            }
                        }
                    }
                    changeGroupingCompleteState(listGroup.filter { it.isSelected }.size, noGroup.size)
                    lightGroupedAdapter.notifyDataSetChanged()
                }
            }
            DeviceType.SMART_CURTAIN -> {
                if (checkedNoGrouped) {
                    for (device in noGroupCutain) {
                        for (sourceDevice in deviceDataCurtainAll) {
                            if (device.id == sourceDevice.id) {
                                sourceDevice.selected = isSelectAll
                                device.selected = isSelectAll
                                if (isSelectAll)
                                    startBlink(device.belongGroupId, device.meshAddr)
                                else
                                    stopBlink(device.meshAddr, device.belongGroupId)
                            }
                        }
                    }
                    changeGroupingCompleteState(noGroupCutain.filter { it.isSelected }.size, noGroupCutain.size)
                    curtainAdapter.notifyDataSetChanged()
                } else {
                    for (device in listGroupCutain) {
                        for (sourceDevice in deviceDataCurtainAll) {
                            if (device.id == sourceDevice.id) {
                                sourceDevice.selected = isSelectAll
                                device.selected = isSelectAll
                                if (isSelectAll)
                                    startBlink(device.belongGroupId, device.meshAddr)
                                else
                                    stopBlink(device.meshAddr, device.belongGroupId)
                            }
                        }
                    }
                    changeGroupingCompleteState(listGroupCutain.filter { it.isSelected }.size, noGroupCutain.size)
                    curtainAdapter.notifyDataSetChanged()
                }
            }
            DeviceType.SMART_RELAY -> {
                if (checkedNoGrouped) {
                    for (device in noGroupRelay) {
                        for (sourceDevice in deviceDataRelayAll) {
                            if (device.id == sourceDevice.id) {
                                sourceDevice.selected = isSelectAll
                                device.selected = isSelectAll
                                if (isSelectAll)
                                    startBlink(device.belongGroupId, device.meshAddr)
                                else
                                    stopBlink(device.meshAddr, device.belongGroupId)
                            }
                        }
                    }
                    changeGroupingCompleteState(noGroupRelay.filter { it.isSelected }.size, noGroupRelay.size)
                    relayGroupedAdapter.notifyDataSetChanged()
                } else {
                    for (device in listGroup) {
                        for (sourceDevice in deviceDataRelayAll) {
                            if (device.id == sourceDevice.id) {
                                sourceDevice.selected = isSelectAll
                                device.selected = isSelectAll
                                if (isSelectAll)
                                    startBlink(device.belongGroupId, device.meshAddr)
                                else
                                    stopBlink(device.meshAddr, device.belongGroupId)
                            }
                        }
                    }
                    changeGroupingCompleteState(listGroupRelay.filter { it.isSelected }.size, noGroupRelay.size)
                    relayGroupedAdapter.notifyDataSetChanged()
                }

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
        compositeDisposable.dispose()
        this.mApplication?.removeEventListener(this)
        isAddGroupEmptyView = false
    }


    override fun onBackPressed() {
        super.onBackPressed()
        setDeviceTypeDataStopBlink(deviceType, false)
        checkNetworkAndSync(this)
        ToastUtils.showLong(getString(R.string.grouping_success_tip))
        finish()
    }


    @SuppressLint("CheckResult")
    fun findMeshDevice(deviceName: String?) {
        val scanFilter = com.polidea.rxandroidble2.scan.ScanFilter.Builder().setDeviceName(deviceName).build()
        val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

        LogUtils.d("findMeshDevice name = $deviceName")
        disposableScan?.dispose()
        disposableScan = RecoverMeshDeviceUtil.rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .observeOn(Schedulers.io())
                .map { RecoverMeshDeviceUtil.parseData(it) }          //解析数据
                .timeout(RecoverMeshDeviceUtil.SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS) {
                    LogUtils.d("findMeshDevice name complete.")
                    it.onComplete()                     //如果过了指定时间，还搜不到缺少的设备，就完成
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it != null)
                        refreshRssi(it)
                }, {})
        compositeDisposable.add(disposableScan!!)

    }

    private fun refreshRssi(deviceInfo: DeviceInfo) {
        LogUtils.v("zcl信号$deviceInfo")
        GlobalScope.launch(Dispatchers.Main) {
            if (deviceInfo.productUUID == deviceType) {
                var deviceChangeL: DbLight? = null
                var deviceChangeC: DbCurtain? = null
                var deviceChangeR: DbConnector? = null
                when (deviceType) {
                    DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                        for (device in deviceDataLightAll) {
                            if (device.meshAddr == deviceInfo.meshAddress) {
                                device.rssi = deviceInfo.rssi
                                deviceChangeL = device
                                LogUtils.v("zcl设备信号$deviceInfo----------------$deviceChangeL")
                            }
                        }
                        if (null != deviceChangeL) {
                            deviceDataLightAll.remove(deviceChangeL)
                            deviceDataLightAll.add(deviceChangeL)
                        }
                    }
                    DeviceType.SMART_CURTAIN -> {
                        for (device in deviceDataCurtainAll) {
                            if (device.macAddr == deviceInfo.macAddress) {
                                device.rssi = deviceInfo.rssi
                                deviceChangeC = device
                                LogUtils.v("zcl设备信号$deviceInfo----------------$deviceChangeC")
                            }
                        }
                        if (null != deviceChangeC) {
                            deviceDataCurtainAll.remove(deviceChangeC)
                            deviceDataCurtainAll.add(deviceChangeC)
                        }
                    }
                    DeviceType.SMART_RELAY -> {
                        for (device in deviceDataRelayAll) {
                            if (device.macAddr == deviceInfo.macAddress) {
                                device.rssi = deviceInfo.rssi
                                deviceChangeR = device
                                LogUtils.v("zcl设备信号$deviceInfo----------------$deviceChangeR")
                            }
                        }
                        if (null != deviceChangeL) {
                            deviceDataRelayAll.remove(deviceChangeR)
                            deviceDataRelayAll.add(deviceChangeR!!)
                        }
                    }
                }

            }
        }
    }

    /**
     * 开始扫描
     */

    @SuppressLint("CheckResult")
    private fun oldStartScan() {
        LogUtils.d("####信号 start scan idleMode true ####")
        //断连后延时一段时间再开始扫描
        disposableConnectTimer?.dispose()
        LeBluetooth.getInstance().stopScan()
        val delay = if (RomUtils.isHuawei()) HUAWEI_DELAY else SCAN_DELAY
        disposableConnectTimer = Observable.timer(delay, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val mesh = mApplication!!.mesh
                    //扫描参数
                    val params = LeScanParameters.create()
                    // if (!AppUtils.isExynosSoc) {
                    params.setScanFilters(getFilters())
                    //}
                    params.setMeshName(DBUtils.lastUser?.controlMeshName)
                    params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                    params.setScanMode(false)
                    TelinkLightService.Instance()?.startScan(params)
                }
    }


    private fun getFilters(): ArrayList<ScanFilter> {
        val scanFilters = ArrayList<ScanFilter>()
        val manuData: ByteArray?
        manuData = byteArrayOf(0, 0, 0, 0, 0, 0, deviceType.toByte())//转换16进制

        val manuDataMask = byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())

        val scanFilter = ScanFilter.Builder()
                .setManufacturerData(Constant.VENDOR_ID, manuData, manuDataMask)
                .build()
        scanFilters.add(scanFilter)
        return scanFilters
    }
}