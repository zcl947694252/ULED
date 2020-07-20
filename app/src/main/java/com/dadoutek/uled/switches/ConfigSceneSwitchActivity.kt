package com.dadoutek.uled.switches

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
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.MainActivity
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
import kotlinx.android.synthetic.main.activity_scene_switch_group.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar
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
    private var switchDate: DbSwitch? = null
    private var mDisconnectSnackBar: Snackbar? = null
    private var mConnectedSnackBar: Snackbar? = null
    private var mConnectingSnackBar: Snackbar? = null
    val map = mutableMapOf<Int, DbScene>()
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
            if (version.contains("BTS")) {
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
        map.clear()

        groupName = intent.getStringExtra("group")
        isReConfig = groupName != null && groupName == "true"
        fiRename?.isVisible = isReConfig

        if (isReConfig){
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

    private fun confirmSceneSw() {
        if (TelinkLightApplication.getApp().connectDevice == null) {
            if (mConnectingSnackBar?.isShown != true) {
                mConfigFailSnackbar?.dismiss()
                showDisconnectSnackBar()
            }
        } else {
            pb_ly.visibility = View.VISIBLE
            GlobalScope.launch {
                setSceneForSwitch()
                newMeshAddr = MeshAddressGenerator().meshAddress.get()
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
                        showRenameDialog(switchDate)

                }, failedCallback = {
                    mConfigFailSnackbar = snackbar(config_scene_switch, getString(R.string.pace_fail))
                    GlobalScope.launch(Dispatchers.Main) {
                        pb_ly.visibility = View.GONE
                        mIsConfiguring = false
                    }
                })
            }
        }
    }


    override fun setVersion() {
        if (TextUtils.isEmpty(version))
            version = getString(R.string.get_version_fail)
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
        showRenameDialog(switchDate)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.scene_one -> {
                configTag = 0
            }
            R.id.scene_two -> {
                configTag = 1
            }
            R.id.scene_three -> {
                configTag = 2
            }
            R.id.scene_four -> {
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
                dbSwitch.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID, this) + dbSwitch!!.meshAddr
                dbSwitch.controlSceneId = getControlScene()
                dbSwitch.macAddr = mDeviceInfo.macAddress
                dbSwitch.meshAddr = /*Constant.SWITCH_PIR_ADDRESS*/ mDeviceInfo.meshAddress
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
        mDisconnectSnackBar = indefiniteSnackbar(config_scene_switch, getString(R
                .string.device_disconnected), getString(R.string.reconnect)) {
            reconnect()
        }
    }

    private fun reconnect() {
        //自动重连参数
        val connectParams = Parameters.createAutoConnectParameters()
        connectParams.setMeshName(mDeviceInfo.meshName)

        val mesh = TelinkLightApplication.getApp().mesh
        val pwd: String
        pwd = if (mDeviceInfo.meshName == Constant.PIR_SWITCH_MESH_NAME) {
            mesh.factoryPassword.toString()
        } else {
            NetworkFactory.md5(NetworkFactory.md5(mDeviceInfo.meshName) + mDeviceInfo.meshName)
                    .substring(0, 16)
        }
        connectParams.setPassword(pwd)
        connectParams.autoEnableNotification(true)
        connectParams.setTimeoutSeconds(CONNECT_TIMEOUT)
        connectParams.setConnectMac(mDeviceInfo.macAddress)

        mDisconnectSnackBar?.dismiss()
        mConnectingSnackBar = indefiniteSnackbar(config_scene_switch, getString(R
                .string.connecting))

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
        val map: Map<Int, DbScene> =/* mAdapter.sceneMap*/map
        for (key in map.keys) {
            when (key) {
                0 -> keyNum = 0x05          //左上按键
                1 -> keyNum = 0x03          //右上按键
                2 -> keyNum = 0x06          //左下按键
                3 -> keyNum = 0x04          //右下按键
            }
            val paramBytes = byteArrayOf(keyNum.toByte(), 7, 0x00, map.getValue(key).id.toByte(), 0x00)

            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH, mDeviceInfo.meshAddress, paramBytes)
            Thread.sleep(200)
        }
    }

    private fun makePop() {
        renameConfirm?.setOnClickListener {
            // 获取输入框的内容
            if (StringUtils.compileExChar(textGp?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                switchDate?.name = textGp?.text.toString().trim { it <= ' ' }
                if (switchDate != null)
                    DBUtils.updateSwicth(switchDate!!)
                else
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
            switchDate?.name = textGp?.text.toString().trim { it <= ' ' }
            if (switchDate != null)
                DBUtils.updateSwicth(switchDate!!)
            showConfigSuccessDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodes) {
            val scene = data?.getParcelableExtra<DbScene>("data")
            scene.let {
                map[configTag] = scene!!
                LogUtils.v("zcl---返回结果$configTag-----$scene-----$map")
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