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
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.scene.SceneEditListAdapter
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.BleUtils
import com.dadoutek.uled.util.StringUtils
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
import kotlinx.android.synthetic.main.toolbar.*
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

    private var disposable: Disposable? = null
    private var currentGroup: DbGroup? = null
    private var btnAddGroups: TextView? = null
    private var emptyGroupView: View? = null
    private var mTelinkLightService: TelinkLightService? = null
    private lateinit var groupAdapter: SceneEditListAdapter
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
    val selectLights = mutableListOf<DbLight>()
    val selectCurtain = mutableListOf<DbCurtain>()
    val selectConnector = mutableListOf<DbConnector>()
    private val mBlinkDisposables = SparseArray<Disposable>()

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
        toolbar.setNavigationOnClickListener { finish() }

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
        groupsByDeviceType = DBUtils.getGroupsByDeviceType(deviceType)
        if (groupsByDeviceType == null) {
            groupsByDeviceType = mutableListOf()
        } else if (groupsByDeviceType!!.size > 0) {
            for (index in groupsByDeviceType!!.indices)
                groupsByDeviceType!![index].isChecked = index == 0
        }
        batch_four_group_title.text = getString(R.string.grouped_num, groupsByDeviceType?.size)

        groupAdapter = SceneEditListAdapter(R.layout.scene_group_edit_item, groupsByDeviceType!!)
        groupAdapter.emptyView = emptyGroupView
        groupAdapter.bindToRecyclerView(batch_four_group_recycle)
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
                groupingLight(deviceListLight, noGroup, listGroup)
                deviceData = if (!isHaveGrouped&&noGroup?.size?:0>0) {
                    noGroup
                } else {
                    batch_four_grouped.isChecked = true
                    listGroup
                }
                if (deviceData!!.size > 8)
                    batch_four_device_recycle.layoutParams.height = ScreenUtils.dp2px(this, 220)
                LogUtils.e("zcl---批量分组4----灯设备信息$deviceData")

                setLightAdatpter()
            }
            DeviceType.SMART_CURTAIN -> {
                deviceListCurtains = DBUtils.getAllCurtains()
            }
            DeviceType.SMART_RELAY -> {
                deviceListRelay = DBUtils.getAllRelay()
            }
        }
    }

    private fun setLightAdatpter() {
        lightAdapter = BatchFourLightAdapter(R.layout.batch_device_item, deviceData!!)
        lightAdapter?.bindToRecyclerView(batch_four_device_recycle)
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
    private fun groupingLight(deviceListLight: ArrayList<DbLight>, noGroup: MutableList<DbLight>?, listGroup: MutableList<DbLight>?) {
        for (i in deviceListLight.indices) {//获取组名 将分组与未分组的拆分
            if (StringUtils.getLightGroupName(deviceListLight[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
                deviceListLight[i].groupName = ""
                noGroup!!.add(deviceListLight[i])
            } else {
                deviceListLight[i].groupName = StringUtils.getLightGroupName(deviceListLight[i])
                listGroup!!.add(deviceListLight[i])
            }
        }

        batch_four_no_group.text = getString(R.string.no_group_device_num, noGroup?.size)
        batch_four_grouped.text = getString(R.string.grouped_num, listGroup?.size)
    }

    private fun initListener() {
        //先取消，这样可以确保不会重复添加监听
        this.mApplication?.removeEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)

        batch_four_group_rg.setOnCheckedChangeListener { _, checkedId -> changeHaveOrNoGroupDevice(checkedId) }

        batch_four_device_all.setOnClickListener { changeDeviceAll() }

        batch_four_group_add_group.setOnClickListener { addNewGroup() }

        btnAddGroups?.setOnClickListener { addNewGroup() }

        groupAdapter.setOnItemClickListener { _, _, position ->
            //如果点中的不是上次选中的组则恢复未选中状态
            changeGroupSelectView(position)
        }

        lightAdapter?.setOnItemChildClickListener { _, _, position ->
            deviceData?.get(position)!!.selected = !deviceData?.get(position)!!.selected
            if (deviceData?.get(position)!!.isSelected)
                startBlink(deviceData?.get(position)!!.belongGroupId,deviceData?.get(position)!!.meshAddr)
            else
                stopBlink(deviceData?.get(position)!!.meshAddr)
            lightAdapter?.notifyDataSetChanged()
        }

        grouping_completed.setOnClickListener {
            sureGroups()
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
                                selectLights.size.toString() + ""))
                        setGroupForLights(currentGroup, selectLights)
                    }
                    DeviceType.SMART_CURTAIN -> {
                        showLoadingDialog(resources.getString(R.string.grouping_wait_tip,
                                selectCurtain.size.toString() + ""))
                    }
                    DeviceType.SMART_RELAY -> {
                        showLoadingDialog(resources.getString(R.string.grouping_wait_tip,
                                selectConnector.size.toString() + ""))
                    }
                }
                //将灯列表的灯循环设置分组

                setGroupForLights(currentGroup, selectLights)
                setGroupForLights(currentGroup, selectLights)
            } else {
                Toast.makeText(mApplication, R.string.select_group_tip, Toast.LENGTH_SHORT).show()
            }
        } else {
            showToast(getString(R.string.selected_lamp_tip))
        }
    }

    private fun setGroupForLights(group: DbGroup?, selectLights: MutableList<DbLight>) {
        if (selectLights.size == deviceData?.size) {
            toolbar!!.menu.findItem(R.id.menu_select_all).title = getString(R.string.cancel)
        } else {
            toolbar!!.menu.findItem(R.id.menu_select_all).title = getString(R.string.select_all)
        }

        for (i in selectLights.indices) {
            //让选中的灯停下来别再发闪的命令了。
            stopBlink(selectLights[i].meshAddr)
        }
       // setGroupOneByOne(group, selectLights, 0)
    }

/*    //等待解决无法加入窗帘如组
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

    }*/
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
                return selectCurtain.size > 0
            }
            DeviceType.SMART_RELAY -> {
                deviceDataRelay ?: return false
                return selectConnector.size > 0
            }
        }
        return false
    }

    private fun isAllSelectCurtain(deviceDataCurtain: MutableList<DbCurtain>) {
        for (device in deviceDataCurtain)
            if (device.selected)
                selectCurtain.add(device)
    }

    private fun isAllSelectConnect(deviceData: MutableList<DbConnector>): Boolean {
        for (device in deviceData) {
            if (device.selected)
                selectConnector.add(device)
            if (!device.selected)
                return false
        }
        return true
    }

    private fun isAllSelect(deviceData: MutableList<DbLight>) {
        for (device in deviceData) {
            if (device.selected)
                selectLights.add(device)
        }
    }

    private fun changeHaveOrNoGroupDevice(checkedId: Int) {
        isHaveGrouped = checkedId == R.id.batch_four_grouped
        if (!isHaveGrouped) {
            batch_four_no_group_line.visibility = View.VISIBLE
            batch_four_grouped_line.visibility = View.INVISIBLE
        } else {
            batch_four_no_group_line.visibility = View.INVISIBLE
            batch_four_grouped_line.visibility = View.VISIBLE
        }
        setChangeHaveOrNoGroupDevice()
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
        if (lastCheckedGroupPostion != position && groupsByDeviceType?.get(lastCheckedGroupPostion) != null)
            groupsByDeviceType?.get(lastCheckedGroupPostion)!!.checked = false

        lastCheckedGroupPostion = position
        if (groupsByDeviceType?.get(position) != null)
            groupsByDeviceType?.get(position)!!.checked = !groupsByDeviceType?.get(position)!!.checked
        currentGroup = if (groupsByDeviceType?.get(position)!!.isChecked)
            groupsByDeviceType?.get(position)
        else
            null
        groupAdapter.notifyDataSetChanged()
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
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                deviceData ?: return
                for (i in deviceData!!.indices) {
                    deviceData!![i].selected = isSelectAll
                    if (isSelectAll)
                        startBlink(deviceData!![i].belongGroupId, deviceData!![i].meshAddr)
                }
                lightAdapter?.notifyDataSetChanged()
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
        if (TelinkLightApplication.getApp().connectDevice == null){
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

    private fun doFinish() {
        //页面跳转前进行分组数据保存
        if (deviceListLight != null && deviceListLight.size > 0) {
            checkNetworkAndSync(this)
        }
        TelinkLightService.Instance()?.idleMode(true)
        finish()
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
                LogUtils.v("zcl---baseactivity收到登入广播")
                ToastUtils.showLong(getString(R.string.connect_success))
                changeDisplayImgOnToolbar(true)

                val connectDevice = this.mApplication?.connectDevice
                LogUtils.d("directly connection device meshAddr = ${connectDevice?.meshAddress}")
            }
            LightAdapter.STATUS_LOGOUT -> {
                LogUtils.v("zcl---baseactivity收到登出广播")
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
    }

}