package com.dadoutek.uled.light

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.le.ScanFilter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.util.SparseArray
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.RomUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.base.CmdBodyBean
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.GwLoginActivity
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.group.BatchGroupFourDeviceActivity
import com.dadoutek.uled.group.GroupsRecyclerViewAdapter
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.model.ScannedDeviceItem
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.Constant.VENDOR_ID
import com.dadoutek.uled.model.dbModel.*
import com.dadoutek.uled.model.dbModel.DBUtils.lastRegion
import com.dadoutek.uled.model.dbModel.DBUtils.lastUser
import com.dadoutek.uled.model.httpModel.GwModel
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkStatusCode
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.othersview.SplashActivity
import com.dadoutek.uled.switches.*
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.tellink.TelinkMeshErrorDealActivity
import com.dadoutek.uled.util.*
import com.polidea.rxandroidble2.scan.ScanSettings
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.event.MeshEvent
import com.telink.bluetooth.light.*
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_device_scanning.*
import kotlinx.android.synthetic.main.template_lottie_animation.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.singleLine
import org.jetbrains.anko.startActivity
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 创建者     zcl
 * 创建时间   2019/8/28 18:37
 * 描述	      ${搜索 冷暖灯 全彩灯  窗帘 Connector蓝牙接收器设备}$
 *路由逻辑: 1. http请求扫描 等待mqtt回调(无回调 失败) - 接收mqtt上传的设备每次超时每个6秒收到刷新-超时--走跳转逻辑(请求获取未确认扫描结果得到数据包装)
 * --如果得到数据的情况下是超时也就是mqtt没有接到但是没有结束 重新计算超时 如果借宿了 确认清除数据 跳转
 * 更新者     $Author$
 *
 * 更新时间   $Date$
 */
class DeviceScanningNewActivity : TelinkMeshErrorDealActivity(), EventListener<String>, Toolbar.OnMenuItemClickListener {
    private var scanRouterTimeoutTime: Long = 0
    private var routerScanCount: Int = 0
    private val TAG = "zcl-DeviceScanningNewActivity"
    private var disposableFind: Disposable? = null
    private var disposableTimer: Disposable? = null
    private var meshList: MutableList<Int> = mutableListOf()
    private lateinit var dbGw: DbGateway
    private var mConnectDisposal: Disposable? = null
    private val MAX_RETRY_COUNT = 4   //update mesh failed的重试次数设置为6次
    private val MAX_RSSI = 90
    private var scanTimeoutTime = 25L//25
    private val SCAN_DELAY: Long = 1000       // 每次Scan之前的Delay , 1000ms比较稳妥。
    private val HUAWEI_DELAY: Long = 2000       // 华为专用Delay
    val user = lastUser
    private var mAutoConnectDisposable: Disposable? = null
    private var disposable: Disposable? = null
    private var testId: DbGroup? = null
    private var mApplication: TelinkLightApplication? = null
    private var mRxPermission: RxPermissions? = null

    //防止内存泄漏
    internal var mDisposable = CompositeDisposable()

    //分组所含灯的缓存
    private var nowDeviceList: MutableList<ScannedDeviceItem> = mutableListOf()
    private var inflater: LayoutInflater? = null
    private var grouping: Boolean = false
    private var isFirtst = true
    private var lastMyRegion = lastRegion

    //标记登录状态
    private lateinit var groupsRecyclerViewAdapter: GroupsRecyclerViewAdapter
    private var groups: MutableList<DbGroup> = ArrayList()
    private var mTimer: Disposable? = null
    private var mUpdateMeshRetryCount = 0
    private var mConnectRetryCount = 0

    //当前所选组index
    private var currentGroupIndex = -1
    private var updateList: MutableList<ScannedDeviceItem> = ArrayList()

    //对一个灯重复分组时记录上一次分组
    private val mGroupingDisposable: Disposable? = null

    //灯的mesh地址
    private var mConnectTimer: Disposable? = null
    private val mBlinkDisposables = SparseArray<Disposable>()
    private var isSelectAll = false
    private var initHasGroup = false
    private var allLightId: Long = 0
    private var updateMeshStatus: UPDATE_MESH_STATUS? = null
    private var mAddDeviceType: Int = 0
    private var mAddedDevices: MutableList<ScannedDeviceItem> = mutableListOf()
    private var mAddedDevicesInfos = arrayListOf<DeviceInfo>()
    private val mAddedDevicesAdapter: DeviceListAdapter = DeviceListAdapter(R.layout.template_batch_small_item, mAddedDevices)
    //有无被选中的用来分组的灯
    private val isSelectLight: Boolean get() = mAddedDevices.any { it.isSelected }       //只要已添加的设备里有一个选中的，就返回False

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableConnectionStatusListener()
        mRxPermission = RxPermissions(this)
        //设置屏幕常亮
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_device_scanning)
        meshList.clear()
        TelinkLightService.Instance()?.idleMode(true)
        initData()
        initView()
        initListener()
        startScan()
    }


    private val currentGroup: DbGroup? get() {
            if (currentGroupIndex == -1) {
                if (groups.size > 1)
                    Toast.makeText(this, R.string.please_select_group, Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(this, R.string.tip_add_gp, Toast.LENGTH_SHORT).show()
                return null
            }
            return groups[currentGroupIndex]
        }

    /**
     * 是否所有灯都分了组
     * @return false还有没有分组的灯 true所有灯都已经分组
     */
    private val isAllLightsGrouped: Boolean get() {
            for (device in mAddedDevices)
                if (device.belongGroupId == allLightId) {
                    return false
                }
            return true
        }


    internal var syncCallback: SyncCallback = object : SyncCallback {
        override fun start() {}
        override fun complete() {}
        override fun error(msg: String) {
            ToastUtils.showLong(R.string.upload_data_failed)
        }
    }

    private var bestRssiDevice: DeviceInfo? = null

    enum class UPDATE_MESH_STATUS { SUCCESS, FAILED, UPDATING_MESH }

    private fun isSelectAll() {
        when {
            isSelectAll -> {
                for (j in nowDeviceList.indices) {
                    this.updateList.add(nowDeviceList[j])
                    nowDeviceList[j].isSelected = true

                    btn_add_groups?.setText(R.string.sure_group)

                    if (groups.size != -1) {
                        startBlink(nowDeviceList[j])
                    } else {
                        ToastUtils.showLong(R.string.tip_add_group)
                    }
                }

                this.mAddedDevicesAdapter.notifyDataSetChanged()
            }
            else -> {
                for (j in nowDeviceList.indices) {
                    this.updateList.remove(nowDeviceList[j])
                    nowDeviceList[j].isSelected = false
                    stopBlink(nowDeviceList[j])
                    if (!isSelectLight && isAllLightsGrouped) {
                        btn_add_groups?.setText(R.string.complete)
                    }
                }
                this.mAddedDevicesAdapter.notifyDataSetChanged()
            }
        }
    }

    /**
     * 让灯开始闪烁
     */
    private fun startBlink(scannedDeviceItem: ScannedDeviceItem) {
        val group: DbGroup
        if (groups.size > 0) {
            var groupId = scannedDeviceItem!!.belongGroupId
            val groupOfTheLight = DBUtils.getGroupByID(groupId)
            group = when (groupOfTheLight) {
                null -> groups[0]
                else -> groupOfTheLight
            }
            val groupAddress = group.meshAddr
            Log.d("Saw", "startBlink groupAddresss = $groupAddress")
            val dstAddress = scannedDeviceItem.deviceInfo.meshAddress
            val opcode = Opcode.SET_GROUP
            val params = byteArrayOf(0x01, (groupAddress and 0xFF).toByte(), (groupAddress shr 8 and 0xFF).toByte())
            params[0] = 0x01

            if (mBlinkDisposables.get(dstAddress) != null) {
                mBlinkDisposables.get(dstAddress).dispose()
            }

            //每隔1s发一次，就是为了让灯一直闪.
            mBlinkDisposables.put(dstAddress, Observable.interval(0, 1000, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { TelinkLightService.Instance()?.sendCommandNoResponse(opcode, dstAddress, params) })
        }
    }

    private fun stopBlink(light: ScannedDeviceItem) {
        val disposable = mBlinkDisposables.get(light.deviceInfo.meshAddress)
        disposable?.dispose()
    }

    //扫描失败处理方法
    private fun scanFail() {
        closeAnimation()
        btn_stop_scan.visibility = View.GONE
        scanning_no_device.visibility = View.VISIBLE
        //toolbarTv.text = getString(R.string.scan_end)
        scanning_no_factory_btn_ly.visibility = View.VISIBLE
    }

    private fun startTimer() {
        stopScanTimer()
        LogUtils.d("startTimer")
        mTimer = Observable.timer(scanTimeoutTime, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe {
                    LogUtils.d("onLeScanTimeout")
                    onLeScanTimeout()
                }
    }

    private fun retryScan() {
        // toolbarTv.text = getString(R.string.scanning)
        if (mUpdateMeshRetryCount < MAX_RETRY_COUNT) {
            mUpdateMeshRetryCount++
            Log.d("ScanningTest", "update mesh failed , retry count = $mUpdateMeshRetryCount")
            stopScanTimer()
            meshList.clear()
            this.startScan()
        } else {
            Log.d("ScanningTest", "update mesh failed , do not retry")
        }
        updateMeshStatus = UPDATE_MESH_STATUS.FAILED
    }

    private fun stopScanTimer() {
        if (mTimer != null && !mTimer!!.isDisposed) {
            mTimer!!.dispose()
            isScanning = false
        }
    }


    //处理扫描成功后
    @SuppressLint("CheckResult")
    private fun scanSuccess() {
        TelinkLightService.Instance()?.idleMode(true)
        //更新Title
        // toolbar!!.title = getString(R.string.title_scanned_lights_num, mAddedDevices.size)
        // toolbarTv.text = getString(R.string.title_scanned_lights_num, mAddedDevices.size)
        //存储当前添加的灯。
        //2018-4-19-hejiajun 添加灯调整位置，防止此时点击灯造成下标越界
        if (nowDeviceList.size > 0) {
            nowDeviceList.clear()
        }
        val elements = mAddedDevicesAdapter.data
        nowDeviceList.addAll(elements)

        showLoadingDialog(resources.getString(R.string.please_wait))

        disposableConnectTimer?.dispose()
        disposableConnectTimer = Observable.timer(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    autoConnect(mutableListOf(mAddDeviceType))
                }

        // list_devices?.visibility = View.VISIBLE
        // btn_add_groups?.visibility = View.VISIBLE
        //if (mAddDeviceType != DeviceType.GATE_WAY)
        // btn_add_groups?.setText(R.string.start_group_bt)
        //scanning_num?.visibility = View.GONE
        // btn_stop_scan?.visibility = View.GONE

        //进入分组界面之前的监听
        // btn_add_groups?.setOnClickListener(onClick)
    }


    private fun disposeAllSubscribe() {
        for (i in 0 until mBlinkDisposables.size()) {
            val disposable = mBlinkDisposables.get(i)
            disposable?.dispose()
        }
        mGroupingDisposable?.dispose()
        mDisposable.dispose()
        mTimer?.dispose()
        mConnectTimer?.dispose()
    }

    @SuppressLint("CheckResult")
    private fun doFinish() {
        mApplication?.removeEventListener(this)
        if (updateList.size > 0) {
            checkNetworkAndSync()
        }
        updateList.clear()

        disposeAllSubscribe()

        mApplication?.removeEventListener(this)
        showLoadingDialog(getString(R.string.please_wait))
        TelinkLightService.Instance()?.idleMode(true)
        disposableConnectTimer?.dispose()
        disposableConnectTimer = Observable.timer(2000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideLoadingDialog()
                    if (ActivityUtils.isActivityExistsInStack(MainActivity::class.java))
                        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
                    else {
//                        ActivityUtils.startActivity(MainActivity::class.java)
                        LogUtils.d("MainActivity doesn't exist in stack")
                        finish()
                    }
                }
    }

    override fun onPause() {
        super.onPause()
        if (mConnectTimer != null)
            mConnectTimer!!.dispose()
        mConnectDisposal?.dispose()
        mApplication?.removeEventListener(this)
    }


    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    private fun sureGroups() {
        if (isSelectLight && TelinkLightApplication.getApp().connectDevice != null) {
            //进行分组操作
            //获取当前选择的分组
            val group = currentGroup
            if (group != null) {
                if (group.meshAddr == 0xffff) {
                    ToastUtils.showLong(R.string.tip_add_gp)
                    return
                }
                //获取当前勾选灯的列表
                val selectLights = mAddedDevices.filter { it.isSelected }

                showLoadingDialog(getString(R.string.grouping_wait_tip, selectLights.size.toString()))
                //将灯列表的灯循环设置分组
                testId = group
                setGroups(group, selectLights)
            }
        } else {
            showToast(getString(R.string.selected_lamp_tip))
        }
    }

    //等待解决无法加入窗帘如组
    private fun setGroupOneByOne(dbGroup: DbGroup, selectLights: List<ScannedDeviceItem>, index: Int) {
        val dbLight = selectLights[index]
        val lightMeshAddr = dbLight.deviceInfo.meshAddress

        Commander.addGroup(lightMeshAddr, dbGroup.meshAddr, {
            dbLight.belongGroupId = dbGroup.id
            updateGroupResult(dbLight, dbGroup)
            if (index + 1 > selectLights.size - 1)
                completeGroup(selectLights)
            else
                setGroupOneByOne(dbGroup, selectLights, index + 1)
            null
        }) {
            dbLight.belongGroupId = allLightId
            ToastUtils.showLong(R.string.group_fail_tip)
            updateGroupResult(dbLight, dbGroup)
            if (TelinkLightApplication.getApp().connectDevice == null) {
                ToastUtils.showLong(getString(R.string.device_disconnected))
                stopScanTimer()
                onLeScanTimeout()
            } else {
                if (index + 1 > selectLights.size - 1)
                    completeGroup(selectLights)
                else
                    setGroupOneByOne(dbGroup, selectLights, index + 1)
            }
            null
        }

    }

    private fun completeGroup(selectLights: List<ScannedDeviceItem>) {
        //取消分组成功的勾选的灯
        for (i in selectLights.indices) {
            val light = selectLights[i]
            light.isSelected = false
        }
        mAddedDevicesAdapter.notifyDataSetChanged()
        hideLoadingDialog()
        if (isAllLightsGrouped) {
            btn_add_groups?.setText(R.string.complete)
        }
    }

    private fun setGroups(group: DbGroup?, selectLights: List<ScannedDeviceItem>) {
        if (group == null) {
            Toast.makeText(mApplication, R.string.select_group_tip, Toast.LENGTH_SHORT).show()
            return
        }

        if (isSelectAll) {
            toolbar!!.menu.findItem(R.id.menu_select_all).title = getString(R.string.select_all)
            isSelectAll = false
        }

        for (i in selectLights.indices)
        //让选中的灯停下来别再发闪的命令了。
            stopBlink(selectLights[i])

        setGroupOneByOne(group, selectLights, 0)
    }

    private fun updateGroupResult(item: ScannedDeviceItem, group: DbGroup) {
        for (i in nowDeviceList.indices) {
            if (item.deviceInfo.meshAddress == nowDeviceList[i].deviceInfo.meshAddress) {
                if (item.belongGroupId != allLightId) {
                    nowDeviceList[i].hasGroup = true
                    nowDeviceList[i].belongGroupId = group.id
                    nowDeviceList[i].name = DBUtils.getGroupByID(group.id)?.name
                            ?: getString(R.string.not_grouped)
                    updateDevice(item)
                } else {
                    nowDeviceList[i].hasGroup = false
                }
            }
        }
    }

    private fun updateDevice(item: ScannedDeviceItem) {
        when (item.deviceInfo.productUUID) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                val old = DBUtils.getLightByMeshAddr(item.deviceInfo.meshAddress)
                if (old == null || old.macAddr != item.deviceInfo.macAddress) {
                    val dbItem = DbLight()
                    dbItem.meshAddr = item.deviceInfo.meshAddress
                    if (DeviceType.LIGHT_NORMAL == item.deviceInfo.productUUID)
                        dbItem.name = getString(R.string.normal_light) + dbItem.meshAddr
                    else
                        dbItem.name = getString(R.string.rgb_light) + dbItem.meshAddr
                    dbItem.textColor = this.resources.getColor(R.color.black)
                    dbItem.belongGroupId = item.belongGroupId
                    dbItem.macAddr = item.deviceInfo.macAddress
                    dbItem.meshUUID = item.deviceInfo.meshUUID
                    dbItem.productUUID = item.deviceInfo.productUUID
                    dbItem.isSelected = item.isSelected
                    dbItem.version = item.firmwareRevision
                    dbItem.rssi = item.rssi
                    DBUtils.saveLight(dbItem, false)
                }
            }

            DeviceType.SENSOR -> {
                val dbItem = DbSensor()
                dbItem.name = getString(R.string.sensor) + dbItem.meshAddr
                dbItem.meshAddr = item.deviceInfo.meshAddress
                dbItem.belongGroupId = item.belongGroupId
                dbItem.macAddr = item.deviceInfo.macAddress
                dbItem.productUUID = item.deviceInfo.productUUID
                dbItem.version = item.firmwareRevision
                dbItem.rssi = item.rssi
                DBUtils.saveSensor(dbItem, false)
            }
            DeviceType.SMART_RELAY -> {
                val dbItem = DbConnector()
                dbItem.name = getString(R.string.relay) + dbItem.meshAddr
                dbItem.meshAddr = item.deviceInfo.meshAddress
                dbItem.textColor = this.resources.getColor(R.color.black)
                dbItem.belongGroupId = item.belongGroupId
                dbItem.macAddr = item.deviceInfo.macAddress
                dbItem.version = item.firmwareRevision
                dbItem.meshUUID = item.deviceInfo.meshUUID
                dbItem.productUUID = item.deviceInfo.productUUID
                dbItem.isSelected = item.isSelected
                dbItem.rssi = item.rssi
                DBUtils.saveConnector(dbItem, false)
            }
            DeviceType.SMART_CURTAIN -> {
                LogUtils.e("zcl保存分组curtain----${DBUtils.getCurtainByGroupID(item.belongGroupId).size}")
                val dbItem = DbCurtain()
                dbItem.name = getString(R.string.curtain) + dbItem.meshAddr
                dbItem.meshAddr = item.deviceInfo.meshAddress
                dbItem.version = item.firmwareRevision
                dbItem.textColor = this.resources.getColor(R.color.black)
                dbItem.belongGroupId = item.belongGroupId
                dbItem.macAddr = item.deviceInfo.macAddress
                dbItem.productUUID = item.deviceInfo.productUUID
                dbItem.isSelected = item.isSelected
                dbItem.rssi = item.rssi
                DBUtils.saveCurtain(dbItem, false)
                LogUtils.e("zcl保存分组curtain----${DBUtils.getCurtainByGroupID(item.belongGroupId).size}----------${DBUtils.getAllCurtains().size}")
            }
            DeviceType.GATE_WAY -> {
                dbGw = DbGateway(getGwId())
                dbGw.macAddr = item.deviceInfo.macAddress
                dbGw.meshAddr = item.deviceInfo.meshAddress
                dbGw.productUUID = item.deviceInfo.productUUID
                dbGw.version = item.firmwareRevision
                dbGw.sixByteMacAddr = item.deviceInfo.sixByteMacAddress
                dbGw.name = getString(R.string.Gate_way) + dbGw.meshAddr
                dbGw.tags = ""
                dbGw.timePeriodTags = ""
                dbGw.uid = (lastUser?.id ?: 0).toInt()
                DBUtils.saveGateWay(dbGw, false)
                addGw(dbGw)
            }
        }
    }

    private fun addGw(dbGw: DbGateway) {
        GwModel.add(dbGw)?.subscribe({
            LogUtils.v("zcl-----网关失添成功返回-------------$it")
        }, {
            LogUtils.v("zcl-------网关失添加败-----------" + it.message)
        })
    }

    override fun onBackPressed() {
        if (grouping) {
            for (meshAddr in mAddedDevices)
                stopBlink(meshAddr)
            doFinish()
        } else {
            if (isScanning) {
                AlertDialog.Builder(this)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            doFinish()
                        }
                        .setNegativeButton(R.string.btn_cancel) { _, _ -> }
                        .setMessage(R.string.exit_tips_in_scanning)
                        .show()
            } else {
                doFinish()
            }
        }
    }


    private fun showGroupForUpdateNameDialog(position: Int) {
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(groups[position].name)
        textGp.singleLine = true
        //        //设置光标默认在最后
        textGp.setSelection(textGp.text.toString().length)
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.update_group))
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)
                .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showLong(getString(R.string.rename_tip_check))
                    } else {
                        groups!![position].name = textGp.text.toString().trim { it <= ' ' }
                        DBUtils.updateGroup(groups!![position])
                        groupsRecyclerViewAdapter.notifyItemChanged(position)
                        mAddedDevicesAdapter!!.notifyDataSetChanged()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_select_all -> {
                if (isSelectAll) {
                    isSelectAll = false
                    item.setTitle(R.string.select_all)
                } else {
                    isSelectAll = true
                    item.setTitle(R.string.cancel)
                }
                isSelectAll()
            }
        }
        return false
    }


    private fun addNewGroup() {
        val textGp = EditText(this)
        textGp.singleLine = true
        textGp.setText(DBUtils.getDefaultNewGroupName())
        StringUtils.initEditTextFilter(textGp)
        val builder = AlertDialog.Builder(this@DeviceScanningNewActivity)
        builder.setTitle(R.string.create_new_group)
        builder.setIcon(android.R.drawable.ic_dialog_info)
        builder.setView(textGp)
        builder.setCancelable(false)
        builder.setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
            // 获取输入框的内容
            if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                //往DB里添加组数据
                val newGroup: DbGroup? = DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, mAddDeviceType.toLong())
                newGroup?.let {
                    groups?.add(newGroup)
                }
                refreshView()
                dialog.dismiss()
                val imm = this@DeviceScanningNewActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }
        if (!isGuide) {
            builder.setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }
        }
        textGp.isFocusable = true
        textGp.isFocusableInTouchMode = true
        textGp.requestFocus()
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val inputManager = textGp.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.showSoftInput(textGp, 0)
            }
        }, 200)
        builder.show()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }


    private fun refreshView() {
        currentGroupIndex = groups!!.size - 1
        for (i in groups!!.indices.reversed()) {
            groups!![i].checked = i == groups!!.size - 1
        }
        add_group_relativeLayout?.visibility = View.GONE
        add_group?.visibility = View.VISIBLE
        recycler_view_groups?.smoothScrollToPosition(groups!!.size - 1)
        groupsRecyclerViewAdapter.notifyDataSetChanged()
        SharedPreferencesHelper.putInt(TelinkLightApplication.getApp(),
                Constant.DEFAULT_GROUP_ID, currentGroupIndex)
    }


    /**
     * 自动重连
     * 此处用作设备登录
     */
    private fun autoConnect(elements: MutableList<Int>) {
        isScanning = false
        if (mAddDeviceType == DeviceType.GATE_WAY)
            mAutoConnectDisposable = connect(macAddress = bestRssiDevice?.macAddress, fastestMode = true, retryTimes = 2)
                    ?.subscribe(
                            {
                                TelinkLightApplication.getApp().isConnectGwBle = true
                                onLogin()
                            }, {
                        onLogin()
                        /*hideLoadingDialog()
                        ToastUtils.showLong(getString(R.string.connect_fail))
                        LogUtils.d(it)*/
                    })
        else
            mAutoConnectDisposable = connect(deviceTypes = elements, fastestMode = true, retryTimes = 2)
                    ?.subscribe(
                            {
                                onLogin()
                            }, {
                        onLogin()
                        /*    hideLoadingDialog()
                            ToastUtils.showLong(getString(R.string.connect_fail))
                            LogUtils.d(it)*/
                    }
                    )
    }

    private fun closeAnimation() {
        isScanning = false
        lottieAnimationView?.cancelAnimation()
        lottieAnimationView?.visibility = View.GONE
    }

    private fun initListener() {
        cancelf.setOnClickListener { popFinish?.dismiss() }
        confirmf.setOnClickListener {
            popFinish?.dismiss()
            stopScanTimer()
            closeAnimation()
            finish()
        }

        scanning_no_factory_btn.setOnClickListener {
            seeHelpe("#QA1")
        }

        btn_start_scan.setOnClickListener {
            initVisiable()
            //toolbarTv.text = getString(R.string.scanning)
            meshList.clear()
            startScan()
        }
        add_group_layout.setOnClickListener {
            //全彩灯以及普通等扫描完毕添加组
            isGuide = false
            addNewGroup()
        }
        btn_stop_scan?.setOnClickListener {//停止扫描
            closeAnimation()
            list_devices.visibility = View.GONE
            scanning_num.visibility = View.GONE
            btn_stop_scan.visibility = View.GONE
            if (Constant.IS_ROUTE_MODE) {
                RouterModel.routeStopScan(TAG, Constant.SCAN_SERID)
                        ?.subscribe({ itr ->
                            when (itr.errorCode) {
                                0 -> ToastUtils.showShort(getString(R.string.stop_scan_fail))
                                NetworkStatusCode.ROUTER_STOP -> skipeType()//路由停止
                            }
                        }, {
                            ToastUtils.showShort(it.message)
                        })
            } else {
                stopScanTimer()
                if (mAddedDevices.size > 0) {//表示目前已经搜到了至少有一个设备
                    scanSuccess()
                } else {
                    closeAnimation()
                    btn_stop_scan.visibility = View.GONE
                    ToastUtils.showLong(getString(R.string.scan_end))
                    finish()
                }
            }
        }

        add_group_relativeLayout?.setOnClickListener { _ -> addNewGroup() }

        mAddedDevicesAdapter.setOnItemClickListener { _, _, position ->
            val item = this.mAddedDevicesAdapter.getItem(position)
            item?.isSelected = !(item?.isSelected ?: false)
            if (item?.isSelected == true) {
                this.updateList.add(item)
                nowDeviceList[position].isSelected = true

                btn_add_groups?.setText(R.string.sure_group)

                if (groups.size != -1) {
                    startBlink(item)
                } else {
                    ToastUtils.showLong(R.string.tip_add_group)
                }
            } else {
                nowDeviceList[position].isSelected = false
                if (item != null) {
                    stopBlink(item)
                    this.updateList.remove(item)
                }
                if (!isSelectLight && isAllLightsGrouped) {
                    btn_add_groups?.setText(R.string.complete)
                }
            }
            mAddedDevicesAdapter.notifyItemChanged(position)
        }
    }

    private fun initView() {
        initToolbar()
        addListerner()

        this.inflater = this.layoutInflater
        this.grouping_completed?.setBackgroundColor(resources.getColor(R.color.gray))

        list_devices.adapter = mAddedDevicesAdapter
        list_devices.layoutManager = GridLayoutManager(this, 2)

        this.updateList.clear()
        initVisiable()
    }

    @SuppressLint("StringFormatInvalid")
    private fun initVisiable() {
        image_bluetooth?.visibility = View.GONE
        btn_add_groups?.visibility = View.GONE
        grouping_completed?.visibility = View.GONE
        toolbar?.findViewById<TextView>(R.id.tv_function1)?.visibility = View.GONE
        scanning_no_device.visibility = View.GONE
        if (mAddDeviceType != DeviceType.GATE_WAY)
            scanning_num.text = getString(R.string.title_scanned_device_num, mAddedDevices.size)
        else
            scanning_num.text = getString(R.string.scanning)
        btn_stop_scan?.setText(R.string.stop_scan)
        btn_stop_scan?.visibility = View.VISIBLE
    }

    private fun addListerner() {
        //监听事件
        mApplication?.removeEventListener(this)
        mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)
        mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
        mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)
        mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)

        LogUtils.d("addEventListener(DeviceEvent.STATUS_CHANGED, this)")
    }

    @SuppressLint("ResourceType")
    private fun initToolbar() {
        toolbar?.setNavigationIcon(R.drawable.icon_return)
        toolbar?.setNavigationOnClickListener {
            if (isScanning) {
                cancelf.isClickable = true
                confirmf.isClickable = true
                popFinish.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
            } else {
                finish()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initData() {
        val intent = intent
        // mAddDeviceType = intent.getIntExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
        val serializable = intent.getIntExtra(Constant.DEVICE_TYPE, 0)
        mAddDeviceType = if (serializable == 0) DeviceType.LIGHT_NORMAL else serializable

        LogUtils.v("zcl------扫描设备类型$mAddDeviceType------------扫描个数${mAddedDevices.size}----${DBUtils.getAllCurtains()}")

        allLightId = DBUtils.getGroupByMeshAddr(0xffff).id?.toLong() ?: 0
        mApplication = this.application as TelinkLightApplication

        if (mAddDeviceType == DeviceType.NORMAL_SWITCH) {
            groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.NORMAL_SWITCH))
            groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.NORMAL_SWITCH2))
            groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.SCENE_SWITCH))
            groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.SMART_CURTAIN_SWITCH))
        } else
            groups.addAll(DBUtils.getGroupsByDeviceType(mAddDeviceType))

        var title = when (mAddDeviceType) {
            DeviceType.LIGHT_NORMAL -> getString(R.string.normal_light)
            DeviceType.LIGHT_RGB -> getString(R.string.rgb_light)
            DeviceType.SENSOR -> getString(R.string.sensor)
            DeviceType.NORMAL_SWITCH -> getString(R.string.switch_title)
            DeviceType.SMART_RELAY -> getString(R.string.relay)
            DeviceType.SMART_CURTAIN -> getString(R.string.curtain)
            DeviceType.GATE_WAY -> getString(R.string.Gate_way)
            else -> getString(R.string.normal_light)
        }
        toolbarTv?.text = title

        if (groups.size > 0) {
            for (i in groups.indices) {
                if (i == groups.size - 1) { //选中最后一个组
                    groups[i].checked = true
                    currentGroupIndex = i
                    SharedPreferencesHelper.putInt(TelinkLightApplication.getApp(),
                            Constant.DEFAULT_GROUP_ID, currentGroupIndex)
                } else {
                    groups[i].checked = false
                }
            }
            initHasGroup = true
        } else {
            initHasGroup = false
            currentGroupIndex = -1
        }
    }

    override fun onResume() {
        super.onResume()
        stopTimerUpdate()
        disableConnectionStatusListener()//停止监听 否则扫描到新设备会自动创建新的对象
        //检测service是否为空，为空则重启
        if (TelinkLightService.Instance() == null)
            mApplication?.startLightService(TelinkLightService::class.java)
    }

    // 如果没有网络，则弹出网络设置对话框
    private fun checkNetworkAndSync() {
        if (NetWorkUtils.isNetworkAvalible(this))
            SyncDataPutOrGetUtils.syncPutDataStart(this, syncCallback)
    }

    override fun onLocationEnable() {}
    internal inner class DeviceListAdapter(layoutId: Int, data: MutableList<ScannedDeviceItem>) : BaseQuickAdapter<ScannedDeviceItem, BaseViewHolder>(layoutId, data) {
        override fun convert(helper: BaseViewHolder?, item: ScannedDeviceItem?) {
            val icon = helper?.getView<ImageView>(R.id.template_device_batch_icon)
            val groupName = helper?.getView<TextView>(R.id.template_device_batch_title_blow)
            val deviceName = helper?.getView<TextView>(R.id.template_device_batch_title)
            val imageView = helper?.getView<ImageView>(R.id.template_device_batch_selected)

            groupName?.visibility = View.GONE
            if (item?.isSelected == true)
                imageView?.setImageResource(R.drawable.icon_checkbox_selected)
            else
                imageView?.setImageResource(R.drawable.icon_checkbox_unselected)

            deviceName?.text = item?.name
            when (mAddDeviceType) {
                DeviceType.GATE_WAY -> icon?.setImageResource(R.drawable.icon_gw_small)
                DeviceType.LIGHT_RGB -> icon?.setImageResource(R.drawable.icon_rgb_n)
                DeviceType.SMART_RELAY -> icon?.setImageResource(R.drawable.icon_acceptor_s)
                DeviceType.SMART_CURTAIN -> icon?.setImageResource(R.drawable.icon_curtain_s)
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> icon?.setImageResource(R.drawable.icon_light_n)
            }

            if (item?.hasGroup == true) {
                icon?.visibility = View.VISIBLE
                imageView?.visibility = View.VISIBLE
            } else {
                deviceName?.visibility = View.VISIBLE
                icon?.visibility = View.VISIBLE
                if (grouping) {
                    imageView?.visibility = View.VISIBLE
                } else {
                    imageView?.visibility = View.GONE
                }
            }
        }
    }

    /**
     * 泰凌微蓝牙库的状态回调
     * 事件处理方法
     * @param event
     */
    override fun performed(event: Event<String>) {
        when (event.type) {
            LeScanEvent.LE_SCAN -> this.onLeScan(event as LeScanEvent)

            LeScanEvent.LE_SCAN_TIMEOUT -> {
            }
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            MeshEvent.ERROR -> this.onMeshEvent()
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
            }
        }
    }

    private fun onErrorReport(info: ErrorReportInfo) {
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        LogUtils.e("蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        LogUtils.e("无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        LogUtils.e("未扫到目标设备")
                    }
                }
            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        LogUtils.e("未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        LogUtils.e("未建立物理连接")
                    }
                }
            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        LogUtils.e("value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        LogUtils.e("read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        LogUtils.e("write login data 没有收到response")
                    }
                }
            }
        }
    }

    private fun onMeshEvent() {
        ToastUtils.showLong(R.string.restart_bluetooth)
    }

    /**
     * 扫描不到任何设备了
     * （扫描结束）
     */
    private fun onLeScanTimeout() {
        isScanning = false
        if (Constant.IS_ROUTE_MODE) {
            skipeType()
        } else {
            TelinkLightService.Instance()?.idleMode(false)
            if (mAddedDevices.size > 0)//表示目前已经搜到了至少有一个设备
                scanSuccess()
            else
                scanFail()
        }
    }

    override fun startRouterScan(cmdBodyBean: CmdBodyBean) {//收到路由是否开始扫描的回调
        if (cmdBodyBean.ser_id == TAG) {
            disposableTimer?.dispose()
            if (cmdBodyBean.status == Constant.ALL_SUCCESS) {
                routeTimerOut()
                Constant.SCAN_SERID = cmdBodyBean.scanSerId
            } else {
                ToastUtils.showShort(cmdBodyBean.msg)
                closeAnimation()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun receivedRouteDeviceNum(cmdBodyBean: CmdBodyBean) {//收到扫描的设备数
        if (cmdBodyBean.ser_id == TAG) {
            if (cmdBodyBean.status == Constant.ALL_SUCCESS) {
                routerScanCount = cmdBodyBean.count
                if (mAddDeviceType == DeviceType.LIGHT_NORMAL || mAddDeviceType == DeviceType.LIGHT_RGB || mAddDeviceType == DeviceType.SMART_RELAY ||
                        mAddDeviceType == DeviceType.SMART_CURTAIN)
                    scanning_num.text = getString(R.string.title_scanned_device_num, routerScanCount)
                else
                    scanning_num.text = getString(R.string.scanning)
            }
            routeTimerOut()
        }
    }

    private fun routeTimerOut() {
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(scanRouterTimeoutTime, TimeUnit.MILLISECONDS).subscribe {
            skipeType()
        }
    }

    /**
     * 开始扫描
     */
    @SuppressLint("CheckResult")
    private fun startScan() {
        if (Constant.IS_ROUTE_MODE) {//发送命令
            RouterModel.routeStartScan(mAddDeviceType, TAG)?.subscribe({
                scanRouterTimeoutTime = it.timeout.toLong()
                routeTimerOut()
                startAnimation()
            }, {
                ToastUtils.showLong(it.message)
                scanFail()
            })
        } else {
            isScanning = true
            //添加进disposable，防止内存溢出.
            mRxPermission?.request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN)?.subscribe { granted ->
                if (granted!!) {
                    oldStartScan()
                } else {
                    DialogUtils.showNoBlePermissionDialog(this, {
                        startScan()
                        null
                    }, {
                        doFinish()
                        null
                    })
                }
            }?.let {
                mDisposable.add(it)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun oldStartScan() {
        isScanning = true
        TelinkLightService.Instance()?.idleMode(true)
        LogUtils.d("#### start scan idleMode true ####")
        startTimer()
        startAnimation()
        handleIfSupportBle()

        //断连后延时一段时间再开始扫描
        disposableConnectTimer?.dispose()
        val delay = if (RomUtils.isHuawei()) HUAWEI_DELAY else SCAN_DELAY
        disposableConnectTimer = Observable.timer(delay, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val mesh = mApplication?.mesh
                    //扫描参数
                    val params = LeScanParameters.create()
                    if (!AppUtils.isExynosSoc)
                        if (mAddDeviceType == DeviceType.NORMAL_SWITCH)
                            params.setScanFilters(getSwitchFilters())
                        else
                            params.setScanFilters(getFilters())

                    params.setMeshName(mesh?.factoryName)
                    params.setOutOfMeshName(Constant.OUT_OF_MESH_NAME)
                    params.setTimeoutSeconds(scanTimeoutTime.toInt())
                    params.setScanMode(true)
                    TelinkLightService.Instance()?.startScan(params)
                    bestRssiDevice = null
                }
    }

    private fun connectBestRssiDevice() {
        disposable?.dispose()
        val meshAddress = MeshAddressGenerator().meshAddress.get()
        LogUtils.v("zcl-----------网关新的meshAddress-------$meshAddress")
        disposable = Observable.timer(200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (bestRssiDevice != null) {
                        val mesh = mApplication!!.mesh
                        updateMesh(bestRssiDevice!!, meshAddress, mesh)
                    }
                }
    }

    private fun getGwId(): Long {
        val list = DBUtils.getAllGateWay()
        val idList = ArrayList<Int>()
        for (i in list.indices)
            idList.add(list[i].id!!.toInt())

        var id = 0
        if (list.size == 0) {
            id = 1
        } else {
            for (i in 1..100000) {
                if (idList.contains(i)) {
                    Log.d("gwID", "getGwId: " + "aaaaa")
                    continue
                } else {
                    id = i
                    Log.d("sceneID", "getGwId: bbbbb$id")
                    break
                }
            }
        }
        return java.lang.Long.valueOf(id.toLong())
    }

    private fun getFilters(): ArrayList<ScanFilter> {
        val scanFilters = ArrayList<ScanFilter>()
        val manuData = byteArrayOf(0, 0, 0, 0, 0, 0, mAddDeviceType.toByte())//转换16进制
        val manuDataMask = byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())

        val scanFilter = ScanFilter.Builder().setManufacturerData(VENDOR_ID, manuData, manuDataMask).build()
        scanFilters.add(scanFilter)
        return scanFilters
    }

    private fun getSwitchFilters(): MutableList<ScanFilter> {
        val scanFilters = ArrayList<ScanFilter>()

        scanFilters.add(ScanFilter.Builder().setManufacturerData(VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.NORMAL_SWITCH.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        scanFilters.add(ScanFilter.Builder().setManufacturerData(VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.SCENE_SWITCH.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        scanFilters.add(ScanFilter.Builder().setManufacturerData(VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.NORMAL_SWITCH2.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        scanFilters.add(ScanFilter.Builder().setManufacturerData(VENDOR_ID,
                byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.SMART_CURTAIN_SWITCH.toByte()),
                byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())).build())
        return scanFilters
    }

    private fun startAnimation() {
        lottieAnimationView?.playAnimation()
        lottieAnimationView?.visibility = View.VISIBLE
    }

    private fun handleIfSupportBle() {
        //检查是否支持蓝牙设备
        if (!LeBluetooth.getInstance().isSupport(applicationContext)) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show()
            doFinish()
            return
        }

        if (!LeBluetooth.getInstance().isEnabled) {
            LeBluetooth.getInstance().enable(applicationContext)
        }
    }

    /**
     * 处理扫描事件
     *  productUUID 区分设备类型
     * @param event
     */
    @SuppressLint("CheckResult")
    @Synchronized
    private fun onLeScan(event: LeScanEvent) {
        val deviceInfo = event.args
        LogUtils.v("获得扫描数据$deviceInfo")
        if (deviceInfo.productUUID == mAddDeviceType && deviceInfo.rssi < MAX_RSSI)
            if (bestRssiDevice == null || deviceInfo.rssi < bestRssiDevice!!.rssi)
                bestRssiDevice = deviceInfo
        connectBestRssiDevice()
    }

    private fun updateMesh(deviceInfo: DeviceInfo, meshAddress: Int, mesh: Mesh) {
        bestRssiDevice = null
        //更新参数
        updateMeshStatus = UPDATE_MESH_STATUS.UPDATING_MESH

        deviceInfo.meshAddress = meshAddress
        meshList.add(meshAddress)
        val params = Parameters.createUpdateParameters()
        params.setOldMeshName(mesh.factoryName)
        params.setOldPassword(mesh.factoryPassword)
        params.setNewMeshName(user?.controlMeshName)
        params.setNewPassword(NetworkFactory.md5(NetworkFactory.md5(user?.controlMeshName) + user?.controlMeshName).substring(0, 16))
        params.setUpdateDeviceList(deviceInfo)
        TelinkLightService.Instance()?.updateMesh(params)

        stopScanTimer()

        LogUtils.d("updateMesh: " + deviceInfo.meshAddress + "" +
                "--" + deviceInfo.macAddress + "--productUUID:" + deviceInfo.productUUID)
    }

    @SuppressLint("SetTextI18n")
    private fun onDeviceStatusChanged(event: DeviceEvent) {
        val deviceInfo = event.args
        when (deviceInfo.status) {
            LightAdapter.STATUS_UPDATE_MESH_COMPLETED -> {
                if (bestRssiDevice == null)
                    bestRssiDevice = deviceInfo
                else
                    if (bestRssiDevice!!.rssi < deviceInfo.rssi)
                        bestRssiDevice = deviceInfo


                GlobalScope.launch { mApplication?.mesh?.saveOrUpdate(this@DeviceScanningNewActivity) }

                LogUtils.d("update mesh success meshAddress = ${deviceInfo.meshAddress}")
                val scannedDeviceItem = ScannedDeviceItem(deviceInfo, getString(R.string.not_grouped))
                //刚开始扫的设备mac是null所以不能mac去重
                mAddedDevices.add(scannedDeviceItem)
                updateDevice(scannedDeviceItem)

                mAddedDevicesAdapter.notifyDataSetChanged()

                //扫描出灯就设置为非首次进入
                if (isFirtst) {
                    isFirtst = false
                    SharedPreferencesHelper.putBoolean(this@DeviceScanningNewActivity, SplashActivity.IS_FIRST_LAUNCH, false)
                }
                // toolbarTv?.text = getString(R.string.title_scanned_device_num, mAddedDevices.size)
                if (mAddDeviceType != DeviceType.GATE_WAY)
                    scanning_num.text = getString(R.string.title_scanned_device_num, mAddedDevices.size)

                updateMeshStatus = UPDATE_MESH_STATUS.SUCCESS
                mUpdateMeshRetryCount = 0
                mConnectRetryCount = 0
                if (mAddDeviceType != DeviceType.GATE_WAY)
                    this.startScan()    //继续开始扫描设备
                else {
                    btn_add_groups?.setText(R.string.config_gate_way)
                    scanSuccess()
                }
            }
            LightAdapter.STATUS_UPDATE_MESH_FAILURE -> {
                retryScan()
            }

            LightAdapter.STATUS_LOGIN -> {

            }
        }
    }

    private fun onLogin() {
        //进入分组
        hideLoadingDialog()
        if (meshList.size > mAddedDevices.size) {//如果生成的大于当前保存的找回设备
            meshList.removeAll(mAddedDevices.map { it.meshAddress })
            startToRecoverDevices()
        } else {
            skipeType()
        }
    }

    private fun skipeType() {
        //开关传感器不能批量也就不能使用找回
        when (mAddDeviceType) {
            DeviceType.NORMAL_SWITCH -> when (bestRssiDevice?.productUUID) {//传感器开关单独扫描界面
                DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2 -> startActivity<ConfigNormalSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false", "deviceType" to bestRssiDevice?.productUUID)
                DeviceType.SCENE_SWITCH -> skipSwitch()
                DeviceType.SMART_CURTAIN_SWITCH -> startActivity<ConfigCurtainSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false")
            }
            DeviceType.GATE_WAY -> {
                if (Constant.IS_ROUTE_MODE) {
                    skipeGw()
                } else {
                    if (TelinkLightApplication.getApp().isConnectGwBle) {//直连时候获取版本号
                        val disposable = Commander.getDeviceVersion(dbGw.meshAddr).subscribe({ s: String ->
                            dbGw.version = s
                            DBUtils.saveGateWay(dbGw, false)
                            skipeGw()
                        }, {
                            skipeGw()
                        })
                    }
                }
            }
            else -> skipeBatchActivity()
        }
    }

    private fun skipSwitch() {
        var version = bestRssiDevice?.firmwareRevision ?: ""
        when (bestRssiDevice?.productUUID) {
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2 -> {
                startActivity<ConfigNormalSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false", "version" to version)
            }
            DeviceType.DOUBLE_SWITCH -> {
                startActivity<DoubleTouchSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false", "version" to version)
            }
            DeviceType.SCENE_SWITCH -> {
                if (version.contains(DeviceType.EIGHT_SWITCH_VERSION))
                    startActivity<ConfigEightSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false", "version" to version)
                else
                    startActivity<ConfigSceneSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false", "version" to version)
            }

            DeviceType.EIGHT_SWITCH -> {
                startActivity<ConfigEightSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false", "version" to version)
            }
            DeviceType.SMART_CURTAIN_SWITCH -> {
                startActivity<ConfigCurtainSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false", "version" to version)
            }
        }
    }

    private fun startToRecoverDevices() {
        LogUtils.v("zcl------找回controlMeshName:${lastUser?.controlMeshName}")
        disposableTimer = Observable.timer(10, TimeUnit.SECONDS).subscribe {
            hideLoadingDialog()
            disposableFind?.dispose()
            skipeType()
        }
        val scanFilter = com.polidea.rxandroidble2.scan.ScanFilter.Builder().setDeviceName(lastUser?.controlMeshName).build()
        val scanSettings = ScanSettings.Builder().build()
        disposableFind = RecoverMeshDeviceUtil.rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .observeOn(Schedulers.io())
                .map { //解析数据
                    val data = RecoverMeshDeviceUtil.parseData(it)
                    data
                }
                .filter {
                    addDevicesToDb(it)   //当保存数据库成功时，才发射onNext
                }
                .map { deviceInfo ->
                    mAddedDevices.add(ScannedDeviceItem(deviceInfo, deviceInfo.deviceName))
                    RecoverMeshDeviceUtil.count
                }
                .timeout(RecoverMeshDeviceUtil.SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS) {
                    it.onComplete()                     //如果过了指定时间，还搜不到缺少的设备，就完成
                }
                .observeOn(AndroidSchedulers.mainThread()).subscribe({
                    skipeAndDisposable()
                }, {
                    skipeAndDisposable()
                })
    }

    private fun addDevicesToDb(deviceInfo: DeviceInfo): Boolean {
        var isExist: Boolean = true//默认存在与服务器不添加
        if (meshList.contains(deviceInfo.meshAddress)) {
            val productUUID = deviceInfo.productUUID
            isExist = DBUtils.isDeviceExist(deviceInfo.meshAddress)
            if (!isExist) {
                meshList.remove(deviceInfo.meshAddress)
                when (productUUID) {
                    DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB -> {
                        val dbLightNew = DbLight()
                        dbLightNew.productUUID = productUUID
                        dbLightNew.connectionStatus = 0
                        dbLightNew.updateIcon()
                        dbLightNew.belongGroupId = DBUtils.groupNull?.id
                        dbLightNew.color = 0
                        dbLightNew.colorTemperature = 0
                        dbLightNew.meshAddr = deviceInfo.meshAddress

                        val string = if (productUUID == DeviceType.LIGHT_RGB) TelinkLightApplication.getApp().getString(R.string.normal_light)
                        else
                            TelinkLightApplication.getApp().getString(R.string.rgb_light)

                        dbLightNew.name = string + dbLightNew.meshAddr
                        dbLightNew.macAddr = deviceInfo.macAddress
                        DBUtils.saveLight(dbLightNew, false)
                        LogUtils.d(String.format("create meshAddress=  %x", dbLightNew.meshAddr))
                        RecoverMeshDeviceUtil.count++
                        LogUtils.v("zcl找回------找回----------${RecoverMeshDeviceUtil.count}----普通灯$dbLightNew---")
                    }
                    DeviceType.SMART_RELAY -> {
                        val relay = DbConnector()
                        relay.productUUID = productUUID
                        relay.connectionStatus = 0
                        relay.updateIcon()
                        relay.belongGroupId = DBUtils.groupNull?.id
                        relay.color = 0
                        relay.meshAddr = deviceInfo.meshAddress
                        relay.name = TelinkLightApplication.getApp().getString(R.string.relay) + relay.meshAddr
                        relay.macAddr = deviceInfo.macAddress
                        DBUtils.saveConnector(relay, false)
                        LogUtils.d("create = $relay  " + relay.meshAddr)
                        RecoverMeshDeviceUtil.count++
                        LogUtils.v("zcl找回-------------${RecoverMeshDeviceUtil.count}-------relay----")
                    }

                    DeviceType.SMART_CURTAIN -> {
                        val curtain = DbCurtain()
                        curtain.productUUID = productUUID
                        curtain.connectionStatus = 0
                        curtain.updateIcon()
                        curtain.belongGroupId = DBUtils.groupNull?.id
                        curtain.meshAddr = deviceInfo.meshAddress
                        curtain.name = TelinkLightApplication.getApp().getString(R.string.curtain) + curtain.meshAddr
                        curtain.macAddr = deviceInfo.macAddress
                        DBUtils.saveCurtain(curtain, false)
                        LogUtils.d("create = $curtain  " + curtain.meshAddr)
                        RecoverMeshDeviceUtil.count++
                        LogUtils.v("zcl找回------------${RecoverMeshDeviceUtil.count}--------curtain---")
                    }
                    /*       DeviceType.NIGHT_LIGHT, DeviceType.SENSOR -> {
                               val sensor = DbSensor()
                               sensor.productUUID = productUUID
                               sensor.belongGroupId = DBUtils.groupNull?.id
                               sensor.meshAddr = deviceInfo.meshAddress
                               sensor.name = TelinkLightApplication.getApp().getString(R.string.device_name) + sensor.meshAddr
                               sensor.macAddr = deviceInfo.macAddress
                               DBUtils.saveSensor(sensor, false)
                               RecoverMeshDeviceUtil.count++
                               LogUtils.v("zcl找回--------------${RecoverMeshDeviceUtil.count}------sensor---")
                           }
                           DeviceType.DOUBLE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.SMART_CURTAIN_SWITCH, DeviceType.SCENE_SWITCH
                               , DeviceType.NORMAL_SWITCH2 -> {
                               val switch = DbSwitch()
                               switch.productUUID = productUUID
                               switch.belongGroupId = DBUtils.groupNull?.id
                               switch.meshAddr = deviceInfo.meshAddress
                               switch.name = TelinkLightApplication.getApp().getString(R.string.device_name) + switch.meshAddr
                               switch.macAddr = deviceInfo.macAddress
                               DBUtils.saveSwitch(switch, false)
                               RecoverMeshDeviceUtil.count++
                               LogUtils.v("zcl找回------------${RecoverMeshDeviceUtil.count}-------普通-switch-")
                           }
                           DeviceType.EIGHT_SWITCH -> {
                               val switch = DbEightSwitch()
                               switch.productUUID = productUUID
                               switch.meshAddr = deviceInfo.meshAddress
                               switch.name = TelinkLightApplication.getApp().getString(R.string.device_name) + switch.meshAddr
                               switch.macAddr = deviceInfo.macAddress
                               DBUtils.saveEightSwitch(switch, false)
                               RecoverMeshDeviceUtil.count++
                               LogUtils.v("zcl找回-----------${RecoverMeshDeviceUtil.count}--------8k-switch--")
                           }*/
                }
            }
        }
        return !isExist
    }

    private fun skipeAndDisposable() {
        disposableTimer?.dispose()
        hideLoadingDialog()
        skipeType()
    }

    @SuppressLint("CheckResult")
    private fun skipeBatchActivity() {
        val intent = Intent(this, BatchGroupFourDeviceActivity::class.java)
        if (Constant.IS_ROUTE_MODE) {//获取扫描数据
            RouterModel.routeScanningResult()?.subscribe({ it ->
                val data = it.data
                if (data != null && data.status == 0) {
                    mAddedDevicesInfos.clear()
                    when {
                         data.data.isNotEmpty() -> {
                            showLoadingDialog(resources.getString(R.string.please_wait))
                            data.data.forEach { x ->
                                val deviceInfo = DeviceInfo()
                                deviceInfo.id = x.id
                                deviceInfo.index = x.index
                                deviceInfo.sixByteMacAddress = x.macAddr
                                deviceInfo.macAddress = x.macAddr
                                deviceInfo.meshAddress = x.meshAddr
                                deviceInfo.meshUUID = x.meshUUID
                                deviceInfo.deviceName = x.name
                                deviceInfo.productUUID = x.productUUID
                                deviceInfo.uid = x.uid
                                deviceInfo.firmwareRevision = x.version
                                deviceInfo.colorTemperature = x.colorTemperature
                                deviceInfo.color = x.color
                                deviceInfo.brightness = x.brightness
                                deviceInfo.boundMac = x.boundMac
                                deviceInfo.belongRegionId = x.belongRegionId
                                deviceInfo.belongGroupId = x.belongGroupId.toLong()
                                mAddedDevicesInfos.add(deviceInfo)
                            }
                            tellServerClear()
                        }
                        else -> scanFail()
                    }
                } else {//如果还在扫描则重新计算超时时间
                    routeTimerOut()
                }
            }, {
                LogUtils.v("zcl-----------$it-------")
            })
        } else {
            DBUtils.saveRegion(lastMyRegion, true)
            var meshAddress = 0
            for (item in mAddedDevices) {
                val deviceInfo = item.deviceInfo
                when {
                    meshAddress == 0 -> meshAddress = deviceInfo.meshAddress
                    meshAddress < item.deviceInfo.meshAddress -> meshAddress = deviceInfo.meshAddress
                }
                mAddedDevicesInfos.add(item.deviceInfo)
            }
            lastMyRegion.lastGenMeshAddr = meshAddress
            skipeActivity(intent)
        }

        LogUtils.v("zcl------扫描设备类型$mAddDeviceType------------扫描个数${mAddedDevices.size}----${DBUtils.getAllCurtains()}")
    }

    @SuppressLint("CheckResult")
    private fun tellServerClear() {
        val subscribe = RouterModel.routeStopScanClear()
                ?.subscribe({
                    skipeActivity(intent)
                }) {
                    ToastUtils.showShort(it.message)
                }
    }

    private fun skipeActivity(intent: Intent) {
        intent.putParcelableArrayListExtra(Constant.DEVICE_NUM, mAddedDevicesInfos)
        intent.putExtra(Constant.DEVICE_TYPE, mAddDeviceType)
        startActivity(intent)
        hideLoadingDialog()
        finish()
    }

    private fun skipeGw() {
        TelinkLightApplication.getApp().isConnectGwBle = true
        val intent = Intent(this@DeviceScanningNewActivity, GwLoginActivity::class.java)
        intent.putExtra("data", dbGw)
        SharedPreferencesHelper.putBoolean(this, Constant.IS_GW_CONFIG_WIFI, false)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableFind?.dispose()
        disposableTimer?.dispose()
        mConnectDisposal?.dispose()
        disposeAllSubscribe()
        mApplication?.removeEventListener(this)
    }
}
