package com.dadoutek.uled.switches

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.TextView
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.BuildConfig
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
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
import kotlinx.android.synthetic.main.activity_switch_group.*
import kotlinx.android.synthetic.main.content_switch_group.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar

private const val CONNECT_TIMEOUT = 20

class ConfigSceneSwitchActivity : TelinkBaseActivity(), EventListener<String> {
    private var popReNameView: View? = null
    private var renameDialog: Dialog? = null
    private var renameCancel: TextView? = null
    private var popRename: PopupWindow? = null
    private var renameConfirm: TextView? = null
    private var renameEditText: EditText? = null
    private var newMeshAddr: Int = 0
    private var alertDialog: android.app.AlertDialog? = null
    private lateinit var mDeviceInfo: DeviceInfo
    private lateinit var mApplication: TelinkLightApplication
    private lateinit var mAdapter: SwitchSceneGroupAdapter
    private lateinit var mSwitchList: ArrayList<String>
    private lateinit var mSceneList: List<DbScene>
    private var mConfigFailSnackbar: Snackbar? = null
    private var groupName: String? = null
    private var switchDate: DbSwitch? = null
    private var mDisconnectSnackBar: Snackbar? = null
    private var mConnectedSnackBar: Snackbar? = null
    private var mConnectingSnackBar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switch_group)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.scene_set)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mApplication = application as TelinkLightApplication

        initData()
        initView()
        initListener()
    }


    private fun initData() {
        //startActivity<ConfigSceneSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true", "switch" to currentSwitch, "version" to version)
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        tvLightVersionText?.text = intent.getStringExtra("version")

        groupName = intent.getStringExtra("group")
        if (groupName != null && groupName == "true") {
            switchDate = this.intent.extras!!.get("switch") as DbSwitch
        }
        mSwitchList = ArrayList()
        mSwitchList.add(getString(R.string.button1))
        mSwitchList.add(getString(R.string.button2))
        mSwitchList.add(getString(R.string.button3))
        mSwitchList.add(getString(R.string.button4))
        mSceneList = DBUtils.sceneAll
    }

    private fun initListener() {
        this.mApplication.removeEventListener(this)
        this.mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        mApplication.addEventListener(ErrorReportEvent.ERROR_REPORT, this)

        fab.setOnClickListener {
            if (TelinkLightApplication.getApp().connectDevice == null) {
                if (mConnectingSnackBar?.isShown != true) {
                    mConfigFailSnackbar?.dismiss()
                    showDisconnectSnackBar()
                }
            } else {
                progressBar.visibility = View.VISIBLE
                GlobalScope.launch {
                    setSceneForSwitch()
                    newMeshAddr = MeshAddressGenerator().meshAddress
                    Commander.updateMeshName(newMeshAddr = newMeshAddr, successCallback = {
                        mDeviceInfo.meshAddress = newMeshAddr
                        mIsConfiguring = true
                        updateSwitch()
                        disconnect()
                        if (switchDate == null)
                            switchDate = DBUtils.getSwitchByMeshAddr(mDeviceInfo.meshAddress)
                        showRenameDialog()

                    },
                            failedCallback = {
                                mConfigFailSnackbar = snackbar(configGroupRoot, getString(R.string.pace_fail))
                                GlobalScope.launch(Dispatchers.Main) {
                                    progressBar.visibility = View.GONE
                                    mIsConfiguring = false
                                }
                            })
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showRenameDialog() {
        hideLoadingDialog()
        popRename?.dismiss()
        StringUtils.initEditTextFilter(renameEditText)

        if (switchDate != null && switchDate?.name != "")
            renameEditText?.setText(switchDate?.name)
        else if (switchDate != null)
            renameEditText?.setText(StringUtils.getSwitchPirDefaultName(switchDate!!.productUUID) + "-"
                    + DBUtils.getAllSwitch().size)
        renameEditText?.setSelection(renameEditText?.text.toString().length)

        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                showCancelDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        showCancelDialog()
    }

    private var mIsDisconnecting: Boolean = false
    private var mIsConfiguring: Boolean = false

    private fun disconnect() {
        if (mIsConfiguring) {
            this.mApplication.removeEventListener(this)
            GlobalScope.launch(Dispatchers.Main) {
                progressBar.visibility = View.GONE
            }
        } else {
            TelinkLightService.Instance()?.idleMode(true)
            TelinkLightService.Instance()?.disconnect()
        }
    }

    private fun showConfigSuccessDialog() {
        try {
            AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.install_success)
                    .setMessage(R.string.tip_config_switch_success)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                     /*   if ((groupName != null && groupName == "true") || (groupName != null && groupName == "false")) {
                            updateSwitch()
                        } else {
                            saveSwitch()
                        }*/
                        TelinkLightService.Instance()?.idleMode(true)
                        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
                    }.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateSwitch() {
        if (groupName == "false") {
            var dbSwitch = DBUtils.getSwitchByMacAddr(mDeviceInfo.macAddress)
            if (dbSwitch != null) {
                dbSwitch.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID)
                dbSwitch.controlSceneId = getControlScene()
                dbSwitch.macAddr = mDeviceInfo.macAddress
                dbSwitch.meshAddr = /*Constant.SWITCH_PIR_ADDRESS*/ mDeviceInfo.meshAddress
                dbSwitch.productUUID = mDeviceInfo.productUUID
                DBUtils.updateSwicth(dbSwitch)
            } else {
                var dbSwitch = DbSwitch()
                DBUtils.saveSwitch(dbSwitch, false)
                dbSwitch!!.controlSceneId = getControlScene()
                dbSwitch!!.macAddr = mDeviceInfo.macAddress
                dbSwitch!!.meshAddr = /*Constant.SWITCH_PIR_ADDRESS*/mDeviceInfo.meshAddress
                dbSwitch!!.productUUID = mDeviceInfo.productUUID
                dbSwitch!!.index = dbSwitch.id.toInt()
                DBUtils.saveSwitch(dbSwitch, false)
                val gotSwitchByMac = DBUtils.getSwitchByMacAddr(mDeviceInfo.macAddress)
                DBUtils.recordingChange(gotSwitchByMac?.id,
                        DaoSessionInstance.getInstance().dbSwitchDao.tablename,
                        Constant.DB_ADD)
            }
        } else {
            switchDate!!.controlSceneId = getControlScene()
            switchDate!!.macAddr = mDeviceInfo.macAddress
            switchDate!!.meshAddr = Constant.SWITCH_PIR_ADDRESS
            switchDate!!.productUUID = mDeviceInfo.productUUID

            DBUtils.updateSwicth(switchDate!!)
        }
    }

    private fun saveSwitch() {
        //确认配置成功后,添加开关到服务器
        var switch = DBUtils.getSwitchByMacAddr(mDeviceInfo.macAddress)
        if (switch != null) {
            var dbSwitch: DbSwitch? = DbSwitch()
            dbSwitch!!.controlSceneId = getControlScene()
            dbSwitch!!.macAddr = mDeviceInfo.macAddress
            dbSwitch!!.meshAddr = Constant.SWITCH_PIR_ADDRESS
            dbSwitch!!.productUUID = mDeviceInfo.productUUID
            dbSwitch!!.index = switch.id.toInt()
            dbSwitch.id = switch.id
            DBUtils.updateSwicth(dbSwitch)
        } else {
            val dbSwitch = DbSwitch()
            DBUtils.saveSwitch(dbSwitch, false)
            dbSwitch!!.controlSceneId = getControlScene()
            dbSwitch!!.macAddr = mDeviceInfo.macAddress
            dbSwitch!!.meshAddr = Constant.SWITCH_PIR_ADDRESS
            dbSwitch!!.productUUID = mDeviceInfo.productUUID
            dbSwitch!!.index = dbSwitch.id.toInt()
            DBUtils.saveSwitch(dbSwitch, false)
            val gotSwitchByMac = DBUtils.getSwitchByMacAddr(mDeviceInfo.macAddress)
            DBUtils.recordingChange(gotSwitchByMac?.id, DaoSessionInstance.getInstance().dbSwitchDao.tablename, Constant.DB_ADD)
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
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    if (TelinkLightApplication.getApp().connectDevice != null) {
                        progressBar.visibility = View.VISIBLE
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
            finish()
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

    private fun showDisconnectSnackBar() {
        TelinkLightService.Instance()?.idleMode(true)
        mDisconnectSnackBar = indefiniteSnackbar(configGroupRoot, getString(R
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
        mConnectingSnackBar = indefiniteSnackbar(configGroupRoot, getString(R
                .string.connecting))

        TelinkLightService.Instance()?.autoConnect(connectParams)

    }

    private fun onDeviceStatusChanged(deviceEvent: DeviceEvent) {
        val deviceInfo = deviceEvent.args
        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                mConnectingSnackBar?.dismiss()
                mConnectedSnackBar = snackbar(configGroupRoot, R.string.connect_success)
                progressBar.visibility = View.GONE
            }


            LightAdapter.STATUS_LOGOUT -> {
                when {
                    mIsDisconnecting -> {
                        this.mApplication.removeEventListener(this)

                        GlobalScope.launch(Dispatchers.Main) {
                            delay(200)
                            progressBar.visibility = View.GONE
                            finish()
                        }
                    }
                    mIsConfiguring -> {
                        this.mApplication.removeEventListener(this)
                        GlobalScope.launch(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            showConfigSuccessDialog()
                        }
                    }
                    else -> showDisconnectSnackBar()
                }
            }
        }

    }

    private fun setSceneForSwitch() {
        val mesh = this.mApplication.mesh
        val params = Parameters.createUpdateParameters()
        if (BuildConfig.DEBUG) {
            params.setOldMeshName(Constant.PIR_SWITCH_MESH_NAME)
        } else {
            params.setOldMeshName(mesh.factoryName)
        }
        params.setOldPassword(mesh.factoryPassword)
        params.setNewMeshName(mesh.name)

        if (SharedPreferencesHelper.getString(TelinkLightApplication.getApp(), Constant.USER_TYPE, Constant.USER_TYPE_OLD) == Constant.USER_TYPE_NEW) {
            params.setNewPassword(NetworkFactory.md5(NetworkFactory.md5(mesh.name) + mesh.name))
        } else {
            params.setNewPassword(mesh?.password)
        }

        params.setUpdateDeviceList(mDeviceInfo)

        var keyNum = 0
        val map: Map<Int, DbScene> = mAdapter.sceneMap
        for (key in map.keys) {
            when (key) {
                0 -> keyNum = 0x05          //左上按键
                1 -> keyNum = 0x03          //右上按键
                2 -> keyNum = 0x06          //左下按键
                3 -> keyNum = 0x04          //右下按键
            }
            val paramBytes = byteArrayOf(keyNum.toByte(), 7, 0x00, map.getValue(key).id.toByte(), 0x00)

            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH,
                    mDeviceInfo.meshAddress, paramBytes)
            Thread.sleep(200)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun initView() {
        makePop()

        if (mSceneList.isEmpty()) {
            fab.visibility = View.GONE
            indefiniteSnackbar(configGroupRoot, R.string.tip_switch, android.R.string.ok) {
                ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
                TelinkLightService.Instance()?.idleMode(true)
            }
            return
        }
        mAdapter = SwitchSceneGroupAdapter(R.layout.item_select_switch_scene_rv, mSwitchList, mSceneList, this)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        mAdapter.bindToRecyclerView(recyclerView)
    }

    private fun makePop() {
        popReNameView = View.inflate(this, R.layout.pop_rename, null)
        renameEditText = popReNameView?.findViewById<EditText>(R.id.pop_rename_edt)
        renameCancel = popReNameView?.findViewById<TextView>(R.id.pop_rename_cancel)
        renameConfirm = popReNameView?.findViewById<TextView>(R.id.pop_rename_confirm)
        renameConfirm?.setOnClickListener {
            // 获取输入框的内容
            if (StringUtils.compileExChar(renameEditText?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showShort(getString(R.string.rename_tip_check))
            } else {
                switchDate?.name = renameEditText?.text.toString().trim { it <= ' ' }
                if (switchDate != null)
                    DBUtils.updateSwicth(switchDate!!)
                else
                    ToastUtils.showShort(getString(R.string.rename_faile))

                if (this != null && !this.isFinishing)
                    renameDialog?.dismiss()
            }
        }


        renameCancel?.setOnClickListener {
            if (this != null && !this.isFinishing)
                renameDialog?.dismiss()
        }

        renameDialog = Dialog(this)
        renameDialog!!.setContentView(popReNameView)
        renameDialog!!.setCanceledOnTouchOutside(false)

        renameDialog?.setOnDismissListener {
            switchDate?.name = renameEditText?.text.toString().trim { it <= ' ' }
            if (switchDate!=null)
            DBUtils.updateSwicth(switchDate!!)
            showConfigSuccessDialog()
        }
    }


    val groupAdress: Int
        get() {
            val list = DBUtils.swtichList
            val idList = java.util.ArrayList<Int>()
            for (i in list.indices.reversed()) {
                if (list[i].meshAddr == 0xffff) {
                    list.removeAt(i)
                }
            }

            for (i in list.indices) {
                idList.add(list[i].meshAddr)
            }

            var id = 0
            for (i in 0x8001..33023) {
                if (idList.contains(i)) {
                    Log.d("sceneID", "getSceneId: " + "aaaaa")
                    continue
                } else {
                    id = i
                    Log.d("sceneID", "getSceneId: bbbbb$id")
                    break
                }
            }

            if (list.size == 0) {
                id = 0x8001
            }
            return id
        }

}