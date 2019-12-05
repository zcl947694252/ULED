package com.dadoutek.uled.light

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.le.ScanFilter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
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
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.BatchGroupFourDeviceActivity
import com.dadoutek.uled.group.GroupsRecyclerViewAdapter
import com.dadoutek.uled.intf.OnRecyclerviewItemClickListener
import com.dadoutek.uled.intf.OnRecyclerviewItemLongClickListener
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.model.ScannedDeviceItem
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.Constant.VENDOR_ID
import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.othersview.SplashActivity
import com.dadoutek.uled.switches.ConfigCurtainSwitchActivity
import com.dadoutek.uled.switches.ConfigNormalSwitchActivity
import com.dadoutek.uled.switches.ConfigSceneSwitchActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.tellink.TelinkMeshErrorDealActivity
import com.dadoutek.uled.util.*
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
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.startActivity
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 创建者     zcl
 * 创建时间   2019/8/28 18:37
 * 描述	      ${搜索 冷暖灯 全彩灯  窗帘 Connector蓝牙接收器设备}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 */
class DeviceScanningNewActivity : TelinkMeshErrorDealActivity(), EventListener<String>, Toolbar.OnMenuItemClickListener {
    private val MAX_RETRY_COUNT = 6   //update mesh failed的重试次数设置为4次
    private val MAX_RSSI = 90
    private val SCAN_TIMEOUT_SECOND = 25
    private val TIME_OUT_CONNECT = 20
    private val SCAN_DELAY: Long = 1000       // 每次Scan之前的Delay , 1000ms比较稳妥。
    private val HUAWEI_DELAY: Long = 2000       // 华为专用Delay

    private var mAutoConnectDisposable: Disposable? = null
    private var rxBleDispose: Disposable? = null
    private var disposable: Disposable? = null
    private var connectInfo: DeviceInfo? = null
    private var testId: DbGroup? = null
    private lateinit var mMeshAddressGenerator: MeshAddressGenerator
    private var mApplication: TelinkLightApplication? = null
    private var mRxPermission: RxPermissions? = null
    //防止内存泄漏
    internal var mDisposable = CompositeDisposable()
    //分组所含灯的缓存
    private var nowDeviceList: MutableList<ScannedDeviceItem> = mutableListOf()
    private var inflater: LayoutInflater? = null
    private var grouping: Boolean = false
    internal var isFirtst = true
    //标记登录状态
    private lateinit var groupsRecyclerViewAdapter: GroupsRecyclerViewAdapter
    private var groups: MutableList<DbGroup> = ArrayList()
    private var mTimer: Disposable? = null
    private var mUpdateMeshRetryCount = 0
    private var mConnectRetryCount = 0
    //当前所选组index
    private var currentGroupIndex = -1
    private var updateList: MutableList<ScannedDeviceItem> = ArrayList()
    private val indexList = ArrayList<Int>()
    //对一个灯重复分组时记录上一次分组
    private val mGroupingDisposable: Disposable? = null
    //灯的mesh地址
    private var mConnectTimer: Disposable? = null
    private val mBlinkDisposables = SparseArray<Disposable>()
    private var isSelectAll = false
    private var initHasGroup = false
    private var guideShowCurrentPage = false
    private var isGuide = false
    private var layoutmanager: LinearLayoutManager? = null
    private var allLightId: Long = 0
    private var updateMeshStatus: UPDATE_MESH_STATUS? = null
    private var mAddDeviceType: Int = 0
    private var mAddedDevices: MutableList<ScannedDeviceItem> = mutableListOf()
    private var mAddedDevicesInfos = arrayListOf<DeviceInfo>()
    private val mAddedDevicesAdapter: DeviceListAdapter = DeviceListAdapter(R.layout.device_item, mAddedDevices)

    /**
     * 有无被选中的用来分组的灯
     * @return true: 选中了       false:没选中
     */
    private val isSelectLight: Boolean
        get() = mAddedDevices.any { it.isSelected }       //只要已添加的设备里有一个选中的，就返回False

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableConnectionStatusListener()
        mRxPermission = RxPermissions(this)
        //设置屏幕常亮
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_device_scanning)
        TelinkLightService.Instance()?.idleMode(true)
        initData()
        initView()
        initClick()
        startScan()
    }

    private val currentGroup: DbGroup?
        get() {
            if (currentGroupIndex == -1) {
                if (groups.size > 1) {
                    Toast.makeText(this, R.string.please_select_group, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.tip_add_gp, Toast.LENGTH_SHORT).show()
                }
                return null
            }
            return groups[currentGroupIndex]
        }

    /**
     * 是否所有灯都分了组
     *
     * @return false还有没有分组的灯 true所有灯都已经分组
     */
    private val isAllLightsGrouped: Boolean
        get() {
            for (device in mAddedDevices) {
                if (device.belongGroupId == allLightId) {
                    return false
                }
            }
            return true
        }


    private val onRecyclerviewItemClickListener = OnRecyclerviewItemClickListener { v, position ->
        currentGroupIndex = position
        for (i in groups.indices.reversed()) {
            if (i != position && groups[i].checked) {
                updateData(i, false)
            } else if (i == position && !groups[i].checked) {
                updateData(i, true)
            } else if (i == position && groups[i].checked) {
                updateData(i, true)
            }
        }

        groupsRecyclerViewAdapter.notifyDataSetChanged()
        SharedPreferencesHelper.putInt(TelinkLightApplication.getApp(),
                Constant.DEFAULT_GROUP_ID, currentGroupIndex)
    }

    private var startConnect = false

    private val onClick = View.OnClickListener {
        stopScanTimer()
        closeAnimation()

        list_devices.visibility = View.VISIBLE
        scanning_num.visibility = View.GONE
        btn_stop_scan.visibility = View.GONE

        if (mAddedDevices.size > 0) {//表示目前已经搜到了至少有一个设备
            scanSuccess()
        } else {
            scanFail()
        }
    }

    internal var syncCallback: SyncCallback = object : SyncCallback {

        override fun start() {}

        override fun complete() {}

        override fun error(msg: String) {
            ToastUtils.showLong(R.string.upload_data_failed)
            Log.d("Error", msg)
        }
    }

    private var bestRssiDevice: DeviceInfo? = null

    enum class UPDATE_MESH_STATUS {
        SUCCESS,
        FAILED,
        UPDATING_MESH
    }

    private fun isSelectAll() {
        if (isSelectAll) {
            for (j in nowDeviceList.indices) {
                this.updateList.add(nowDeviceList[j])
                nowDeviceList[j].isSelected = true

                btn_add_groups?.setText(R.string.set_group)

                if (hasGroup()) {
                    startBlink(nowDeviceList[j])
                } else {
                    ToastUtils.showLong(R.string.tip_add_group)
                }
            }

            this.mAddedDevicesAdapter.notifyDataSetChanged()
        } else {
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

    private fun hasGroup(): Boolean {
        return groups.size != -1
    }

    /**
     * 让灯开始闪烁
     */
    private fun startBlink(scannedDeviceItem: ScannedDeviceItem) {
        val group: DbGroup
        if (groups.size > 0) {
            val groupId = scannedDeviceItem.belongGroupId
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
        showToast(getString(R.string.scan_end))
        closeAnimation()
        doFinish()
    }

    private fun startTimer() {
        stopScanTimer()
        if (mTimer != null && !mTimer!!.isDisposed)
            mTimer!!.dispose()
        LogUtils.d("startTimer")
         mTimer = Observable.timer((SCAN_TIMEOUT_SECOND).toLong(), TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe {
                    LogUtils.d("onLeScanTimeout")
                    onLeScanTimeout()
                }
    }


    private fun retryScan() {
        if (mUpdateMeshRetryCount < MAX_RETRY_COUNT) {
            mUpdateMeshRetryCount++
            Log.d("ScanningTest", "update mesh failed , retry count = $mUpdateMeshRetryCount")
            stopScanTimer()
            this.startScan()
        } else {
            Log.d("ScanningTest", "update mesh failed , do not retry")
        }
        updateMeshStatus = UPDATE_MESH_STATUS.FAILED
    }


    private fun stopScanTimer() {
        if (mTimer != null && !mTimer!!.isDisposed) {
            mTimer!!.dispose()
        }
    }


    private fun createConnectTimeout(): Disposable {
        return Observable.timer(15, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe {
                    Toast.makeText(mApplication, getString(R.string.connect_fail), Toast.LENGTH_SHORT).show()
                    hideLoadingDialog()
                    mConnectTimer = null
                }
    }

    //处理扫描成功后
    @SuppressLint("CheckResult")
    private fun scanSuccess() {
        TelinkLightService.Instance()?.idleMode(true)
        closeAnimation()
        //更新Title
        toolbar!!.title = getString(R.string.title_scanned_lights_num, mAddedDevices.size)
        //存储当前添加的灯。
        //2018-4-19-hejiajun 添加灯调整位置，防止此时点击灯造成下标越界
        if (nowDeviceList.size > 0) {
            nowDeviceList.clear()
        }
        val elements = mAddedDevicesAdapter.data
        nowDeviceList.addAll(elements)

        showLoadingDialog(resources.getString(R.string.connecting_tip))

        disposableTimer?.dispose()
        disposableTimer = Observable.timer(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    autoConnect()
                }


        list_devices?.visibility = View.VISIBLE
        btn_add_groups?.visibility = View.VISIBLE
        btn_add_groups?.setText(R.string.start_group_bt)
        scanning_num?.visibility = View.GONE
        btn_stop_scan?.visibility = View.GONE

        //进入分组界面之前的监听
        btn_add_groups?.setOnClickListener(onClick)

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
        disableEventListenerInGrouping()
        if (updateList.size > 0) {
            checkNetworkAndSync()
        }
        updateList.clear()

        disposeAllSubscribe()

        this.mApplication?.removeEventListener(this)
        showLoadingDialog(getString(R.string.please_wait))
        TelinkLightService.Instance()?.idleMode(true)
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(2000, TimeUnit.MILLISECONDS)
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
        this.mApplication?.removeEventListener(this)
    }

    /**
     * 开始分组
     */
    private fun startGrouping() {
        closeAnimation()
        //初始化分组页面
        changeGroupView()
        //完成分组跳转
        changOtherView()
        //确定当前分组
        sureGroupingEvent()

        toolbar!!.setNavigationOnClickListener {
            AlertDialog.Builder(this@DeviceScanningNewActivity)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        for (meshAddr in mAddedDevices) {
                            stopBlink(meshAddr)
                        }
                        doFinish()
                    }
                    .setNegativeButton(R.string.btn_cancel) { _, _ -> }
                    .setMessage(R.string.exit_tips_in_group)
                    .show()
        }
    }

    private fun sureGroupingEvent() {
        btn_add_groups?.setText(R.string.sure_group)
        //进入分组界面之后的监听
        btn_add_groups?.setOnClickListener { v ->
            if (isAllLightsGrouped && !isSelectLight) {
                for (device in mAddedDevices) {
                    stopBlink(device)
                }
                doFinish()

            } else {
                sureGroups()
            }
        }
    }

    private fun changOtherView() {
        grouping_completed?.setOnClickListener { v ->
            //判定是否还有灯没有分组，如果没有允许跳转到下一个页面
            if (isAllLightsGrouped) {//所有灯都有分组可以跳转
                showToast(getString(R.string.grouping_completed))
                //页面跳转前进行分组数据保存
                TelinkLightService.Instance()?.idleMode(true)
                //目前测试调到主页
                doFinish()
            } else {
                showToast(getString(R.string.have_lamp_no_group_tip))
            }
        }
    }

    private fun sureGroups() {
        if (isSelectLight&&TelinkLightApplication.getApp().connectDevice!=null) {
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

                showLoadingDialog(resources.getString(R.string.grouping_wait_tip,
                        selectLights.size.toString() + ""))
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
                ToastUtils.showLong("断开连接")
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

        for (i in selectLights.indices) {
            //让选中的灯停下来别再发闪的命令了。
            stopBlink(selectLights[i])
        }

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
                val dbItem = DbLight()
                dbItem.name = getString(R.string.device_name)+dbItem.meshAddr
                dbItem.meshAddr = item.deviceInfo.meshAddress
                dbItem.textColor = this.resources.getColor(R.color.black)
                dbItem.belongGroupId = item.belongGroupId
                dbItem.macAddr = item.deviceInfo.macAddress
                dbItem.meshUUID = item.deviceInfo.meshUUID
                dbItem.productUUID = item.deviceInfo.productUUID
                dbItem.isSelected = item.isSelected
                dbItem.rssi = item.rssi
                DBUtils.saveLight(dbItem, false)
            }


            DeviceType.SENSOR -> {
                val dbItem = DbSensor()
                dbItem.name = getString(R.string.device_name)+dbItem.meshAddr
                dbItem.meshAddr = item.deviceInfo.meshAddress
                dbItem.belongGroupId = item.belongGroupId
                dbItem.macAddr = item.deviceInfo.macAddress
                dbItem.productUUID = item.deviceInfo.productUUID
                dbItem.rssi = item.rssi
                DBUtils.saveSensor(dbItem, false)
            }
            DeviceType.SMART_RELAY -> {
                val dbItem = DbConnector()
                dbItem.name = getString(R.string.device_name)+dbItem.meshAddr
                dbItem.meshAddr = item.deviceInfo.meshAddress
                dbItem.textColor = this.resources.getColor(R.color.black)
                dbItem.belongGroupId = item.belongGroupId
                dbItem.macAddr = item.deviceInfo.macAddress
                dbItem.meshUUID = item.deviceInfo.meshUUID
                dbItem.productUUID = item.deviceInfo.productUUID
                dbItem.isSelected = item.isSelected
                dbItem.rssi = item.rssi
                DBUtils.saveConnector(dbItem, false)
            }
            DeviceType.SMART_CURTAIN -> {
                LogUtils.e("zcl保存分组curtain----${DBUtils.getCurtainByGroupID(item.belongGroupId).size}")
                val dbItem = DbCurtain()
                dbItem.name = getString(R.string.device_name)+dbItem.meshAddr
                dbItem.meshAddr = item.deviceInfo.meshAddress
                dbItem.textColor = this.resources.getColor(R.color.black)
                dbItem.belongGroupId = item.belongGroupId
                dbItem.macAddr = item.deviceInfo.macAddress
                dbItem.productUUID = item.deviceInfo.productUUID
                dbItem.isSelected = item.isSelected
                dbItem.rssi = item.rssi
                DBUtils.saveCurtain(dbItem, false)
                LogUtils.e("zcl保存分组curtain----${DBUtils.getCurtainByGroupID(item.belongGroupId).size}----------${DBUtils.getAllCurtains().size}")
            }
        }
    }

    override fun onBackPressed() {
        if (grouping) {
            for (meshAddr in mAddedDevices) {
                stopBlink(meshAddr)
            }
            doFinish()
        } else {
            AlertDialog.Builder(this)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        doFinish()
                    }
                    .setNegativeButton(R.string.btn_cancel) { _, _ -> }
                    .setMessage(R.string.exit_tips_in_scanning)
                    .show()
        }
    }

    //分组页面调整
    private fun changeGroupView() {
        grouping = true
        toolbar!!.inflateMenu(R.menu.menu_grouping_select_all)
        toolbar!!.setOnMenuItemClickListener(this)
        mAddedDevicesAdapter.notifyDataSetChanged()
        btn_add_groups?.visibility = View.VISIBLE
        groups_bottom?.visibility = View.VISIBLE

        layoutmanager = LinearLayoutManager(this)
        layoutmanager!!.orientation = LinearLayoutManager.HORIZONTAL
        recycler_view_groups.layoutManager = layoutmanager
        groupsRecyclerViewAdapter = GroupsRecyclerViewAdapter(groups, onRecyclerviewItemClickListener,
                OnRecyclerviewItemLongClickListener { _, position -> showGroupForUpdateNameDialog(position) })
        recycler_view_groups?.adapter = groupsRecyclerViewAdapter

        if (groups.size > 0) {
            recycler_view_groups?.scrollToPosition(groups.size - 1)
            add_group_relativeLayout?.visibility = View.GONE
            add_group?.visibility = View.VISIBLE
        } else {
            add_group_relativeLayout?.visibility = View.VISIBLE
            add_group?.visibility = View.GONE
        }

        disableEventListenerInGrouping()
        initOnLayoutListener()
    }

    private fun showGroupForUpdateNameDialog(position: Int) {
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(groups[position].name)
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

    private fun disableEventListenerInGrouping() {
        this.mApplication!!.removeEventListener(this)
    }

    private fun addNewGroup() {
        val textGp = EditText(this)
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
                ToastUtils.showShort(getString(R.string.rename_tip_check))
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
                guideStep2()
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

    private fun updateData(position: Int, checkStateChange: Boolean) {
        groups!![position].checked = checkStateChange
    }

    /**
     * 自动重连
     * 此处用作设备登录
     */
    private fun autoConnect() {

        mAutoConnectDisposable = connect()
                ?.subscribe(
                        {
                            onLogin()
                        },
                        {
                            hideLoadingDialog()
                            LogUtils.d(it)
                        }
                )
    }

    private fun closeAnimation() {
        lottieAnimationView?.cancelAnimation()
        lottieAnimationView?.visibility = View.GONE
    }

    fun connectDevice(mac: String) {
        TelinkLightService.Instance()?.connect(mac, TIME_OUT_CONNECT)
    }


    override fun initOnLayoutListener() {
        val view = window.decorView
        val viewTreeObserver = view.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    //第二部选择组
    private fun guideStep2() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_INSTALL_LIGHT_KEY, false)
        if (guideShowCurrentPage) {
            val guide2 = recycler_view_groups
            GuideUtils.guideBuilder(this, GuideUtils.STEP4_GUIDE_SELECT_GROUP)
                    .addGuidePage(guide2?.let {
                        GuideUtils.addGuidePage(it, R.layout.view_guide_scan1, getString(R.string.scan_light_guide_2),
                                View.OnClickListener { v -> guideStep3() }, GuideUtils.END_INSTALL_LIGHT_KEY, this)
                    })
                    .show()
        }
    }

    //第三部选择灯
    private fun guideStep3() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_INSTALL_LIGHT_KEY, false)
        if (guideShowCurrentPage) {
            val guide3 = list_devices!!.getChildAt(0)
            GuideUtils.guideBuilder(this, GuideUtils.STEP5_GUIDE_SELECT_SOME_LIGHT)
                    .addGuidePage(GuideUtils.addGuidePage(guide3, R.layout.view_guide_scan2, getString(R.string.scan_light_guide_3), View.OnClickListener { v ->
                        guide3.performClick()
                        guideStep4()
                    }, GuideUtils.END_INSTALL_LIGHT_KEY, this))
                    .show()
        }
    }

    //第四部确定分组
    private fun guideStep4() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_INSTALL_LIGHT_KEY, false)
        if (guideShowCurrentPage) {
            val guide4 = btn_add_groups
            GuideUtils.guideBuilder(this, GuideUtils.STEP6_GUIDE_SURE_GROUP)
                    .addGuidePage(guide4?.let {
                        GuideUtils.addGuidePage(it, R.layout.view_guide_scan3, getString(R.string.scan_light_guide_4), View.OnClickListener { v ->
                            guide4.performClick()
                            GuideUtils.changeCurrentViewIsEnd(this, GuideUtils.END_INSTALL_LIGHT_KEY, true)
                        }, GuideUtils.END_INSTALL_LIGHT_KEY, this)
                    })
                    .show()
        }
    }

    private fun initClick() {
        add_group_layout.setOnClickListener {
            //全彩灯以及普通等扫描完毕添加组
            isGuide = false
            addNewGroup()
        }

        mAddedDevicesAdapter.setOnItemClickListener { _, _, position ->
            val item = this.mAddedDevicesAdapter.getItem(position)
            item?.isSelected = !(item?.isSelected ?: false)
            if (item?.isSelected == true) {
                this.updateList.add(item)
                nowDeviceList[position].isSelected = true

                btn_add_groups?.setText(R.string.set_group)

                if (hasGroup()) {
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
        //监听事件
        this.mApplication?.removeEventListener(this)
        this.mApplication!!.addEventListener(LeScanEvent.LE_SCAN, this)
        this.mApplication!!.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
        this.mApplication!!.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)
        this.mApplication!!.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication!!.addEventListener(ErrorReportEvent.ERROR_REPORT, this)

        LogUtils.d("addEventListener(DeviceEvent.STATUS_CHANGED, this)")

        this.inflater = this.layoutInflater

        this.grouping_completed?.setBackgroundColor(resources.getColor(R.color.gray))

        list_devices.adapter = mAddedDevicesAdapter
        list_devices.layoutManager = GridLayoutManager(this, 3)

        this.updateList.clear()

        image_bluetooth?.visibility = View.GONE
        btn_add_groups?.visibility = View.GONE
        grouping_completed?.visibility = View.GONE
        toolbar?.findViewById<TextView>(R.id.tv_function1)?.visibility = View.GONE

        scanning_num.text = getString(R.string.title_scanned_device_num, 0)
        btn_stop_scan?.setText(R.string.stop_scan)
        btn_stop_scan?.setOnClickListener(onClick)

        add_group_relativeLayout?.setOnClickListener { v -> addNewGroup() }
    }

    @SuppressLint("ResourceType")
    private fun initToolbar() {
        toolbar?.setTitle(R.string.scanning)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationContentDescription(R.drawable.navigation_back_white)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initData() {
        mMeshAddressGenerator = MeshAddressGenerator()
        val intent = intent
        mAddDeviceType = intent.getIntExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)

        LogUtils.v("zcl------扫描设备类型$mAddDeviceType------------扫描个数${mAddedDevices.size}----${DBUtils.getAllCurtains()}")

        if (DBUtils.getGroupByMesh(0xffff) != null) {
            allLightId = DBUtils.getGroupByMesh(0xffff)?.id?.toLong() ?: 0
        }

        this.mApplication = this.application as TelinkLightApplication

        if (mAddDeviceType == DeviceType.NORMAL_SWITCH) {
            groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.NORMAL_SWITCH))
            groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.NORMAL_SWITCH2))
            groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.SCENE_SWITCH))
            groups.addAll(DBUtils.getGroupsByDeviceType(DeviceType.SMART_CURTAIN_SWITCH))
        } else
            groups.addAll(DBUtils.getGroupsByDeviceType(mAddDeviceType))

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
        disableConnectionStatusListener()//停止监听 否则扫描到新设备会自动创建新的对象
        //检测service是否为空，为空则重启
        if (TelinkLightService.Instance() == null)
            mApplication!!.startLightService(TelinkLightService::class.java)
    }


    // 如果没有网络，则弹出网络设置对话框
    private fun checkNetworkAndSync() {
        if (NetWorkUtils.isNetworkAvalible(this))
            SyncDataPutOrGetUtils.syncPutDataStart(this, syncCallback)
    }


    override fun onLocationEnable() {}


    internal inner class DeviceListAdapter(layoutId: Int, data: MutableList<ScannedDeviceItem>) : BaseQuickAdapter<ScannedDeviceItem, BaseViewHolder>(layoutId, data) {
        override fun convert(helper: BaseViewHolder?, item: ScannedDeviceItem?) {
            val icon = helper?.getView<ImageView>(R.id.img_icon)
            val groupName = helper?.getView<TextView>(R.id.tv_group_name)
            val deviceName = helper?.getView<TextView>(R.id.tv_device_name)
            val checkBox = helper?.getView<CheckBox>(R.id.selected)

            groupName?.visibility = View.GONE
            checkBox?.isChecked = item?.isSelected ?: false
            deviceName?.text = item?.name
            icon?.setImageResource(R.drawable.icon_device_open)


            if (item?.hasGroup == true) {
                icon?.visibility = View.VISIBLE
                checkBox?.visibility = View.VISIBLE
            } else {
                deviceName?.visibility = View.VISIBLE
                icon?.visibility = View.VISIBLE
                if (grouping) {
                    checkBox?.visibility = View.VISIBLE
                } else {
                    checkBox?.visibility = View.GONE
                }
            }
        }
    }


    /**
     * 泰凌微蓝牙库的状态回调
     * 事件处理方法
     *
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
        ToastUtils.showShort(R.string.restart_bluetooth)
    }


    /**
     * 扫描不到任何设备了
     * （扫描结束）
     */
    private fun onLeScanTimeout() {
        TelinkLightService.Instance()?.idleMode(false)
        list_devices.visibility = View.VISIBLE
        if (mAddedDevices.size > 0)//表示目前已经搜到了至少有一个设备
            scanSuccess()
        else
            scanFail()
    }

    /**
     * 开始扫描
     */
    @SuppressLint("CheckResult")
    private fun startScan() {
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


    @SuppressLint("CheckResult")
    private fun oldStartScan() {
        TelinkLightService.Instance()?.idleMode(true)
        LogUtils.d("#### start scan idleMode true ####")
        startTimer()
        startAnimation()
        handleIfSupportBle()

        //断连后延时一段时间再开始扫描
        disposableTimer?.dispose()
        val delay = if (RomUtils.isHuawei()) HUAWEI_DELAY else SCAN_DELAY
        disposableTimer = Observable.timer(delay, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val mesh = mApplication!!.mesh
                    //扫描参数
                    val params = LeScanParameters.create()
                    if (!AppUtils.isExynosSoc) {
                        if (mAddDeviceType == DeviceType.NORMAL_SWITCH)
                            params.setScanFilters(getSwitchFilters())
                        else
                            params.setScanFilters(getFilters())
                    }
                    params.setMeshName(mesh.factoryName)
                    params.setOutOfMeshName(Constant.OUT_OF_MESH_NAME)
                    params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                    params.setScanMode(true)
                    TelinkLightService.Instance()?.startScan(params)
                    bestRssiDevice = null
                }
    }

    private fun connectBestRssiDevice() {
        val mesh = this.mApplication!!.mesh
        disposable?.dispose()
        disposable = Observable.timer(200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (bestRssiDevice != null) {
                        rxBleDispose?.dispose()
                        updateMesh(bestRssiDevice!!, mMeshAddressGenerator.meshAddress, mesh)
                    }
                }
    }

    private fun getFilters(): ArrayList<ScanFilter> {
        val scanFilters = ArrayList<ScanFilter>()
        val manuData: ByteArray?
        manuData = byteArrayOf(0, 0, 0, 0, 0, 0, mAddDeviceType.toByte())//转换16进制

        val manuDataMask = byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())

        val scanFilter = ScanFilter.Builder()
                .setManufacturerData(VENDOR_ID, manuData, manuDataMask)
                .build()
        scanFilters.add(scanFilter)
        return scanFilters
    }

    private fun getSwitchFilters(): MutableList<ScanFilter> {
        val scanFilters = ArrayList<ScanFilter>()

        scanFilters.add(ScanFilter.Builder()
                .setManufacturerData(VENDOR_ID,
                        byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.NORMAL_SWITCH.toByte()),
                        byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte()))
                .build())

        scanFilters.add(ScanFilter.Builder()
                .setManufacturerData(VENDOR_ID,
                        byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.SCENE_SWITCH.toByte()),
                        byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte()))
                .build())
        scanFilters.add(ScanFilter.Builder()
                .setManufacturerData(VENDOR_ID,
                        byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.NORMAL_SWITCH2.toByte()),
                        byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte()))
                .build())
        scanFilters.add(ScanFilter.Builder()
                .setManufacturerData(VENDOR_ID,
                        byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.SMART_CURTAIN_SWITCH.toByte()),
                        byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte()))
                .build())
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
        if (mAddDeviceType == DeviceType.NORMAL_SWITCH) {
            if (deviceInfo.productUUID == DeviceType.NORMAL_SWITCH || deviceInfo.productUUID == DeviceType.NORMAL_SWITCH2
                    || deviceInfo.productUUID == DeviceType.SCENE_SWITCH || deviceInfo.productUUID == DeviceType.SMART_CURTAIN_SWITCH)

                if (deviceInfo.rssi < MAX_RSSI) {
                    LeBluetooth.getInstance().stopScan()
                    if (deviceInfo.productUUID == DeviceType.SCENE_SWITCH && DBUtils.sceneAll.isEmpty()) {
                        indefiniteSnackbar(topView, R.string.tip_switch, android.R.string.ok) {
                            ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
                            TelinkLightService.Instance()?.idleMode(true)
                        }
                        return
                    }
                    bestRssiDevice = deviceInfo
                    TelinkLightService.Instance()?.connect(deviceInfo.macAddress, 10)
                }
        } else {
            if (bestRssiDevice == null)
                connectBestRssiDevice()
            if (deviceInfo.productUUID == mAddDeviceType && deviceInfo.rssi < MAX_RSSI)
                if (bestRssiDevice == null || deviceInfo.rssi < bestRssiDevice!!.rssi)
                    bestRssiDevice = deviceInfo
        }
    }



    private fun updateMesh(deviceInfo: DeviceInfo, meshAddress: Int, mesh: Mesh) {
        bestRssiDevice = null
        //更新参数
        updateMeshStatus = UPDATE_MESH_STATUS.UPDATING_MESH
        deviceInfo.meshAddress = meshAddress
        val params = Parameters.createUpdateParameters()
        val user = DBUtils.lastUser
        //LogUtils.e("zcl登录", "zcl******mes:${mesh.factoryName}======${mesh.factoryPassword}-------${user?.controlMeshName}" +
        //"============${NetworkFactory.md5(NetworkFactory.md5(user?.controlMeshName) + user?.controlMeshName).substring(0, 16)}")
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


    private fun onDeviceStatusChanged(event: DeviceEvent) {
        val deviceInfo = event.args
        when (deviceInfo.status) {
            LightAdapter.STATUS_UPDATE_MESH_COMPLETED -> {
                if (bestRssiDevice == null) {
                    bestRssiDevice = deviceInfo
                } else {
                    if (bestRssiDevice!!.rssi < deviceInfo.rssi)
                        bestRssiDevice = deviceInfo
                }

                GlobalScope.launch { this@DeviceScanningNewActivity.mApplication!!.mesh.saveOrUpdate(this@DeviceScanningNewActivity) }

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
                toolbar?.title = getString(R.string.title_scanned_device_num, mAddedDevices.size)
                scanning_num.text = getString(R.string.title_scanned_device_num, mAddedDevices.size)

                updateMeshStatus = UPDATE_MESH_STATUS.SUCCESS
                mUpdateMeshRetryCount = 0
                mConnectRetryCount = 0
                this.startScan()    //继续开始扫描设备

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
        if (mAddDeviceType == DeviceType.NORMAL_SWITCH) {
            if (bestRssiDevice?.productUUID == DeviceType.NORMAL_SWITCH ||
                    bestRssiDevice?.productUUID == DeviceType.NORMAL_SWITCH2) {
                startActivity<ConfigNormalSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false")
            } else if (bestRssiDevice?.productUUID == DeviceType.SCENE_SWITCH) {
                startActivity<ConfigSceneSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false")
            } else if (bestRssiDevice?.productUUID == DeviceType.SMART_CURTAIN_SWITCH) {
                startActivity<ConfigCurtainSwitchActivity>("deviceInfo" to bestRssiDevice!!, "group" to "false")
            }

        } else if (mAddDeviceType == DeviceType.SMART_CURTAIN) {
            startGrouping()
        } else {
            val intent = Intent(this, BatchGroupFourDeviceActivity::class.java)

            for (item in mAddedDevices) {
                mAddedDevicesInfos.add(item.deviceInfo)
            }
            intent.putParcelableArrayListExtra(Constant.DEVICE_NUM,mAddedDevicesInfos)

            intent.putExtra(Constant.DEVICE_TYPE,mAddDeviceType)
            startActivity(intent)
            finish()
            LogUtils.v("zcl------扫描设备类型$mAddDeviceType------------扫描个数${mAddedDevices.size}----${DBUtils.getAllCurtains()}")
        }

        fun onDestroy() {
            super.onDestroy()
            //移除事件
            this.mApplication?.removeEventListener(this)
        }
    }
}
