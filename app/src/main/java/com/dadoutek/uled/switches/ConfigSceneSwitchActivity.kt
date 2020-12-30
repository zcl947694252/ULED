package com.dadoutek.uled.switches

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.BuildConfig
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbScene
import com.dadoutek.uled.model.dbModel.DbSwitch
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.MeshAddressGenerator
import com.dadoutek.uled.util.StringUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.ErrorReportInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_scene_switch_group.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

private const val CONNECT_TIMEOUT = 20

class ConfigSceneSwitchActivity : BaseSwitchActivity(), EventListener<String>, View.OnClickListener {
    private val requestCodes: Int = 1000
    private var version: String = ""
    private var newMeshAddr: Int = 0
    private var mSwitchList: ArrayList<String> = ArrayList()
    private var mSceneList: ArrayList<DbScene> = ArrayList()
    private lateinit var mDeviceInfo: DeviceInfo
    private lateinit var mAdapter: SwitchSceneGroupAdapter
    private var mConfigFailSnackbar: Snackbar? = null
    private var groupName: String? = null
    private var mDisconnectSnackBar: Snackbar? = null
    private var mConnectedSnackBar: Snackbar? = null
    private var mConnectingSnackBar: Snackbar? = null
    private val mapConfig = mutableMapOf<Int, DbScene>()
    private var configTag: Int = 0
    private var mIsDisconnecting: Boolean = false
    private var mIsConfiguring: Boolean = false
    override fun setLayoutId(): Int {
        return R.layout.activity_scene_switch_group
    }

    override fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        version = intent.getStringExtra("version")
        if (TextUtils.isEmpty(version))
            version = getString(R.string.get_version_fail)
        else {
            if (version.contains("BTS") || version.contains("SS-2.1.0")) {
                scene_switch_cw.visibility = View.GONE
                scene_switch_touch.visibility = View.VISIBLE
                toolbarTv.text = getString(R.string.touch_sw)
            } else {
                scene_switch_cw.visibility = View.VISIBLE
                scene_switch_touch.visibility = View.GONE
                toolbarTv.text = getString(R.string.light_sw)
            }
        }
        fiVersion?.title = version
        //scene_tvLightVersion?.text = version
        mapConfig.clear()

        groupName = intent.getStringExtra("group")
        isReConfig = groupName != null && groupName == "true"
        fiRename?.isVisible = isReConfig

        if (isReConfig) {
            switchDate = this.intent.extras!!.get("switch") as DbSwitch
            toolbarTv.text = switchDate?.name
        }


        mSwitchList.clear()
        mSwitchList.add(getString(R.string.button1))
        mSwitchList.add(getString(R.string.button2))
        mSwitchList.add(getString(R.string.button3))
        mSwitchList.add(getString(R.string.button4))
    }

    override fun initListener() {
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        scene_one.setOnClickListener(this)
        scene_two.setOnClickListener(this)
        scene_three.setOnClickListener(this)
        scene_four.setOnClickListener(this)
        scene_one_t.setOnClickListener(this)
        scene_two_t.setOnClickListener(this)
        scene_three_t.setOnClickListener(this)
        scene_four_t.setOnClickListener(this)
        scene_use_botton.setOnClickListener { confirmSceneSw() }
        mApp?.removeEventListener(this)
        mApp?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        mApp?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    override fun setToolBar(): Toolbar {
        return toolbar
    }

    override fun setReConfig(): Boolean {
        return isReConfig
    }

    override fun initView() {
        toolbarTv?.text = getString(R.string.scene_set)
        mSceneList.clear()
        mSceneList.addAll(DBUtils.sceneAll)
        makePop()
        if (mSceneList.isEmpty()) {
            scene_use_botton.visibility = View.GONE
            indefiniteSnackbar(config_scene_switch, R.string.tip_switch, android.R.string.ok) {
                ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
                TelinkLightService.Instance()?.idleMode(true)
            }
            return
        }
        mAdapter = SwitchSceneGroupAdapter(R.layout.item_select_switch_scene_rv, mSwitchList, mSceneList, this)
//        recyclerView.layoutManager = GridLayoutManager(this, 2)
//        mAdapter.bindToRecyclerView(recyclerView)
    }

    @SuppressLint("CheckResult")
    private fun confirmSceneSw() {
        if (TelinkLightApplication.getApp().connectDevice == null && !Constant.IS_ROUTE_MODE) {
            if (mConnectingSnackBar?.isShown != true) {
                mConfigFailSnackbar?.dismiss()
                showDisconnectSnackBar()
            }
        } else {
            if (mapConfig.size < 4) {
                ToastUtils.showShort(getString(R.string.please_config_all))
                return
            }
            if (Constant.IS_ROUTE_MODE) {
                val sceneMeshAddrs = ArrayList<Int>(4)
                mapConfig.toSortedMap()
                mapConfig.forEach { sceneMeshAddrs.add(it.value.id.toInt()) }
                routerConfigScene(sceneMeshAddrs)
            } else {
                showLoadingDialog(getString(R.string.please_wait))
                GlobalScope.launch {
                    //setSceneForSwitch()
                    val mesh = mApp?.mesh
                    val params = Parameters.createUpdateParameters()
                    when {
                        BuildConfig.DEBUG -> params.setOldMeshName(Constant.PIR_SWITCH_MESH_NAME)
                        else -> params.setOldMeshName(mesh?.factoryName)
                    }
                    params.setOldPassword(mesh?.factoryPassword)
                    params.setNewMeshName(mesh?.name)

                    when (Constant.USER_TYPE_NEW) {
                        SharedPreferencesHelper.getString(TelinkLightApplication.getApp(), Constant.USER_TYPE, Constant.USER_TYPE_OLD) ->
                            params.setNewPassword(NetworkFactory.md5(NetworkFactory.md5(mesh?.name) + mesh?.name))
                        else -> params.setNewPassword(mesh?.password)
                    }

                    params.setUpdateDeviceList(mDeviceInfo)
                    var keyNum = 0
                    for (key in mapConfig.keys) {
                        when (key) {
                            0 -> keyNum = 0x05          //左上按键
                            1 -> keyNum = 0x03          //右上按键
                            2 -> keyNum = 0x06          //左下按键
                            3 -> keyNum = 0x04          //右下按键
                        }
                        delay(key * 200L)
                        val paramBytes = byteArrayOf(keyNum.toByte(), 7, 0x00, mapConfig.getValue(key).id.toByte(), 0x00)
                        LogUtils.v("zcl-----------配置场景参数-----$keyNum---${mapConfig.getValue(key).id}")
                        TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH, mDeviceInfo.meshAddress, paramBytes)
                    }
                    delay(1500)
                    updateMesh()
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun routerConfigScene(sceneMeshAddrs: MutableList<Int>) {
        RouterModel.configSceneSw(switchDate!!.id, sceneMeshAddrs, "configSceneSw")
                ?.subscribe({
                    // "errorCode": 90021, "该开关不存在，请重新刷新数据"   "errorCode": 90008,"该开关没有绑定路由，无法配置"
                    // "errorCode": 90007,"该组不存在，刷新组列表"    "errorCode": 90005,"以下路由没有上线，无法配置"   "errorCode":90011 , "有场景不存在，刷新场景列表"
                    LogUtils.v("zcl-----------收到路由配置场景请求-------$it")
                    when (it.errorCode) {
                        0 -> {
                            pb_ly.visibility = View.VISIBLE
                            disposableRouteTimer?.dispose()
                            disposableRouteTimer = Observable.timer(1500, TimeUnit.MILLISECONDS)
                                    .subscribe {
                                        hideLoadingDialog()
                                        ToastUtils.showShort(getString(R.string.config_fail))
                                    }
                        }
                        90021 -> {
                            ToastUtils.showShort(getString(R.string.device_not_exit))
                            finish()
                        }
                        900018 -> ToastUtils.showShort(getString(R.string.device_not_exit))
                        90011 -> ToastUtils.showShort(getString(R.string.scene_cont_exit_to_refresh))
                          90008 -> {hideLoadingDialog()
                ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))}
                        90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                        90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                        else -> ToastUtils.showShort(it.message)
                    }
                }, { ToastUtils.showShort(it.message) })
    }

    override fun tzRouterConfigSceneSwRecevice(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由配置场景开关通知-------$cmdBean")
        if (cmdBean.ser_id == "configSceneSw") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            if (cmdBean.status == 0) {
                GlobalScope.launch(Dispatchers.Main) {
                    ToastUtils.showShort(getString(R.string.config_success))
                    if (!isReConfig)
                        showRenameDialog(switchDate, false)
                    else
                        finish()
                }
            } else {
                ToastUtils.showShort(getString(R.string.config_fail))
            }
        }
    }

    override fun routerRenameSwSuccess(trim: String) {
        toolbarTv.text = trim
        switchDate?.name =trim
        DBUtils.updateSwicth(switchDate!!)
    }

    private fun updateMesh() {
        newMeshAddr = if (isReConfig) mDeviceInfo?.meshAddress else MeshAddressGenerator().meshAddress.get()
        LogUtils.v("zcl-----------更新开关新mesh-------${newMeshAddr}")
        Commander.updateMeshName(newMeshAddr = newMeshAddr, successCallback = {
            mDeviceInfo.meshAddress = newMeshAddr
            mIsConfiguring = true
            updateSwitch()
            disconnect()
            ToastUtils.showShort(getString(R.string.config_success))
            pb_ly.visibility = View.GONE
            if (switchDate == null)
                switchDate = DBUtils.getSwitchByMeshAddr(mDeviceInfo.meshAddress)
            if (!isReConfig)
                showRenameDialog(switchDate, false)
            else
                finish()
        }, failedCallback = {
            mConfigFailSnackbar = snackbar(config_scene_switch, getString(R.string.config_fail))
            GlobalScope.launch(Dispatchers.Main) {
                pb_ly.visibility = View.GONE
                mIsConfiguring = false
            }
        })
    }


    override fun setVersion() {
        if (TextUtils.isEmpty(version))
            version = getString(R.string.get_version_fail)
        else
            mDeviceInfo?.firmwareRevision = version
        fiVersion?.title = version
    }

    override fun setConnectMeshAddr(): Int {
        return mDeviceInfo?.meshAddress ?: 0
    }

    override fun deleteDevice() {
        deleteSwitch(mDeviceInfo.macAddress)
    }

    override fun goOta() {
        deviceOta(mDeviceInfo)
    }

    override fun reName() {
        showRenameDialog(switchDate, false)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.scene_one, R.id.scene_one_t -> {
                configTag = 0
            }
            R.id.scene_two, R.id.scene_two_t -> {
                configTag = 1
            }
            R.id.scene_three, R.id.scene_three_t -> {
                configTag = 2
            }
            R.id.scene_four, R.id.scene_four_t -> {
                configTag = 3
            }
        }
        startActivityForResult(Intent(this, SelectSceneListActivity::class.java), requestCodes)
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                // showCancelDialog()
                configReturn()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        // showCancelDialog()
        configReturn()
    }

    private fun disconnect() {
        if (mIsConfiguring) {
            mApp?.removeEventListener(this)
            GlobalScope.launch(Dispatchers.Main) {
                // pb_ly.visibility = View.GONE
            }
        } else {
            TelinkLightService.Instance()?.idleMode(true)
            TelinkLightService.Instance()?.disconnect()
        }
    }

    private fun showConfigSuccessDialog() {
        if (version.contains("BT") || version.contains("BTL") || version.contains("BTS") || version.contains("STS")) {
            TelinkLightService.Instance()?.idleMode(true)
            ToastUtils.showLong(getString(R.string.config_success))
            ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
        } else {
            try {
                AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.install_success)
                        .setMessage(R.string.tip_config_switch_success)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            TelinkLightService.Instance()?.idleMode(true)
                            ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
                        }.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    private fun updateSwitch() {
        if (groupName == "false") {
            var dbSwitch = DBUtils.getSwitchByMacAddr(mDeviceInfo.macAddress)
            if (dbSwitch != null) {
                dbSwitch.controlSceneId = getControlScene()
                dbSwitch.macAddr = mDeviceInfo.macAddress
                dbSwitch.meshAddr = /*Constant.SWITCH_PIR_ADDRESS*/ mDeviceInfo.meshAddress
                dbSwitch.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID, this) + dbSwitch!!.meshAddr
                dbSwitch.productUUID = mDeviceInfo.productUUID
                dbSwitch.version = mDeviceInfo.firmwareRevision
                DBUtils.updateSwicth(dbSwitch)
                switchDate = dbSwitch
            } else {
                var dbSwitch = DbSwitch()
                DBUtils.saveSwitch(dbSwitch, false)
                dbSwitch!!.controlSceneId = getControlScene()
                dbSwitch!!.macAddr = mDeviceInfo.macAddress
                dbSwitch!!.meshAddr = /*Constant.SWITCH_PIR_ADDRESS*/mDeviceInfo.meshAddress
                dbSwitch!!.productUUID = mDeviceInfo.productUUID
                dbSwitch.version = mDeviceInfo.firmwareRevision
                dbSwitch!!.index = dbSwitch.id.toInt()
                DBUtils.saveSwitch(dbSwitch, false)
                val gotSwitchByMac = DBUtils.getSwitchByMacAddr(mDeviceInfo.macAddress)
                DBUtils.recordingChange(gotSwitchByMac?.id,
                        DaoSessionInstance.getInstance().dbSwitchDao.tablename,
                        Constant.DB_ADD)
                switchDate = dbSwitch
            }
        } else {
            switchDate?.meshAddr = mDeviceInfo?.meshAddress
            switchDate!!.controlSceneId = getControlScene()
            switchDate!!.macAddr = mDeviceInfo.macAddress
            //switchDate!!.meshAddr = Constant.SWITCH_PIR_ADDRESS
            switchDate!!.productUUID = mDeviceInfo.productUUID

            DBUtils.updateSwicth(switchDate!!)
        }
    }

    private fun getControlScene(): String? {
        var controlSceneIdList = ""
        val map: Map<Int, DbScene> = mAdapter.sceneMap
        for (scene in map.values) {
            if (controlSceneIdList.isEmpty()) {
                controlSceneIdList = scene.id.toString()
            } else {
                controlSceneIdList += "," + scene.id.toString()
            }
        }
        return controlSceneIdList
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(this)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (TelinkLightApplication.getApp().connectDevice != null) {
                        pb_ly.visibility = View.GONE
                        mIsDisconnecting = true
                        disconnect()
                    } else {
                        finish()
                    }
                }
                .setTitle(R.string.do_you_really_want_to_cancel)
                .show()
    }

    override fun performed(event: Event<String>?) {
        when (event?.type) {
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        TelinkApplication.getInstance().removeEventListener(this)
        TelinkLightService.Instance()?.idleMode(true)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            configReturn()
        }
        return super.onKeyDown(keyCode, event)
    }


    private fun onErrorReport(info: ErrorReportInfo) {
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        //("蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        //("无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        //("未扫到目标设备")
                    }
                }
                showDisconnectSnackBar()

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        //("未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        //("未建立物理连接")
                    }
                }
                showDisconnectSnackBar()

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        //("value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        //("read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        //("write login data 没有收到response")
                    }
                }
                //("onError login")
                showDisconnectSnackBar()

            }
        }
    }

    private fun showDisconnectSnackBar() {//java.lang.ClassCastException: android.widget.LinearLayout cannot be cast to android.support.design.widget.CoordinatorLayout
        TelinkLightService.Instance()?.idleMode(true)
        reconnect()
        // mDisconnectSnackBar = indefiniteSnackbar(config_scene_switch, getString(R.string.device_disconnected), getString(R.string.reconnect)) {}
    }

    private fun reconnect() {
        if (Constant.IS_ROUTE_MODE) return
        //自动重连参数
        val connectParams = Parameters.createAutoConnectParameters()
        connectParams.setMeshName(mDeviceInfo.meshName)

        val mesh = TelinkLightApplication.getApp().mesh
        val pwd: String
        pwd = if (mDeviceInfo.meshName == Constant.PIR_SWITCH_MESH_NAME)
            mesh.factoryPassword.toString()
        else
            NetworkFactory.md5(NetworkFactory.md5(mDeviceInfo.meshName) + mDeviceInfo.meshName).substring(0, 16)

        connectParams.setPassword(pwd)
        connectParams.autoEnableNotification(true)
        connectParams.setTimeoutSeconds(CONNECT_TIMEOUT)
        connectParams.setConnectMac(mDeviceInfo.macAddress)

        mDisconnectSnackBar?.dismiss()
        //mConnectingSnackBar = indefiniteSnackbar(config_scene_switch, getString(R.string.connecting_tip))
        ToastUtils.showShort(getString(R.string.connecting_tip))

        TelinkLightService.Instance()?.autoConnect(connectParams)
    }

    private fun onDeviceStatusChanged(deviceEvent: DeviceEvent) {
        val deviceInfo = deviceEvent.args
        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                mConnectingSnackBar?.dismiss()
                mConnectedSnackBar = snackbar(config_scene_switch, R.string.connect_success)
            }

            LightAdapter.STATUS_LOGOUT -> {
                when {
                    mIsDisconnecting -> {
                        mApp?.removeEventListener(this)
                        GlobalScope.launch(Dispatchers.Main) {
                            delay(200)
                            pb_ly.visibility = View.GONE
                            finish()
                        }
                    }
                    mIsConfiguring -> {
                        mApp?.removeEventListener(this)
                        GlobalScope.launch(Dispatchers.Main) {
                            pb_ly.visibility = View.GONE
                            showConfigSuccessDialog()
                        }
                    }
                    else -> showDisconnectSnackBar()
                }
            }
        }
    }

    private fun setSceneForSwitch() {//设置开关场景
        val mesh = mApp?.mesh
        val params = Parameters.createUpdateParameters()
        if (BuildConfig.DEBUG) {
            params.setOldMeshName(Constant.PIR_SWITCH_MESH_NAME)
        } else {
            params.setOldMeshName(mesh?.factoryName)
        }
        params.setOldPassword(mesh?.factoryPassword)
        params.setNewMeshName(mesh?.name)

        if (SharedPreferencesHelper.getString(TelinkLightApplication.getApp(), Constant.USER_TYPE, Constant.USER_TYPE_OLD) == Constant.USER_TYPE_NEW) {
            params.setNewPassword(NetworkFactory.md5(NetworkFactory.md5(mesh?.name) + mesh?.name))
        } else {
            params.setNewPassword(mesh?.password)
        }

        params.setUpdateDeviceList(mDeviceInfo)
        var keyNum = 0
        for (key in mapConfig.keys) {
            when (key) {
                0 -> keyNum = 0x05          //左上按键
                1 -> keyNum = 0x03          //右上按键
                2 -> keyNum = 0x06          //左下按键
                3 -> keyNum = 0x04          //右下按键
            }
            val paramBytes = byteArrayOf(keyNum.toByte(), 7, 0x00, mapConfig.getValue(key).id.toByte(), 0x00)
            LogUtils.v("zcl-----------配置场景参数-------$paramBytes")
            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH, mDeviceInfo.meshAddress, paramBytes)
        }
    }

    private fun makePop() {
        renameConfirm?.setOnClickListener {
            // 获取输入框的内容
            if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                val trim = renameEt?.text.toString().trim { it <= ' ' }

                switchDate?.name = trim
                if (switchDate != null) {
                    if (Constant.IS_ROUTE_MODE)
                        routerRenameSw(switchDate!!, trim)
                    else {
                        toolbarTv.text = switchDate?.name
                        DBUtils.updateSwicth(switchDate!!)
                    }
                } else
                    ToastUtils.showLong(getString(R.string.rename_faile))

                if (this != null && !this.isFinishing)
                    renameDialog?.dismiss()
            }
        }


        renameCancel?.setOnClickListener {
            if (this != null && !this.isFinishing)
                renameDialog?.dismiss()
        }

        renameDialog?.setOnDismissListener {
            if (!isReConfig)
                finish()
        }
    }

    override fun renameSucess() {
        toolbarTv.text = switchDate?.name
        DBUtils.updateSwicth(switchDate!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodes) {
            val scene = data?.getParcelableExtra<DbScene>("data")
            scene.let {
                mapConfig[configTag] = scene!!
                LogUtils.v("zcl---返回结果$configTag-----$scene-----$mapConfig")
                when (configTag) {
                    0 -> {
                        scene_one.text = scene.name
                        scene_one_t.text = scene.name
                    }
                    1 -> {
                        scene_two.text = scene.name
                        scene_two_t.text = scene.name
                    }
                    2 -> {
                        scene_three.text = scene.name
                        scene_three_t.text = scene.name
                    }
                    3 -> {
                        scene_four.text = scene.name
                        scene_four_t.text = scene.name
                    }
                }
            }
        }
    }
}