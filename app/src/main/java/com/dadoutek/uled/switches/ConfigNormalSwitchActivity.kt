package com.dadoutek.uled.switches

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.BuildConfig
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
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


private const val CONNECT_TIMEOUT = 5

class ConfigNormalSwitchActivity : AppCompatActivity(), EventListener<String> {

    private lateinit var mDeviceInfo: DeviceInfo
    private lateinit var mApplication: TelinkLightApplication
    private lateinit var mAdapter: SelectSwitchGroupRvAdapter
    private lateinit var mGroupArrayList: ArrayList<DbGroup>

    private var mDisconnectSnackBar: Snackbar? = null

    private var mConnectedSnackBar: Snackbar? = null

    private var mConfigFailSnackbar: Snackbar? = null

    private var isGlassSwitch=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switch_group)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.select_group)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mApplication = application as TelinkLightApplication

        initView()
        initListener()
        getVersion()
    }

    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            dstAdress = mDeviceInfo.meshAddress
            Commander.getDeviceVersion(dstAdress,
                    successCallback = {
                        versionLayout.visibility = View.VISIBLE
                        tvLightVersion.text = it
                        if(it!!.startsWith("STS")){
                            isGlassSwitch=true
                        }
                    },
                    failedCallback = {
                        versionLayout.visibility = View.GONE
                    })
        } else {
            dstAdress = 0
        }
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(this)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    if (TelinkLightService.Instance().isLogin) {
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


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                showCancelDialog()

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private var mIsDisconnecting: Boolean = false

    private fun disconnect() {
        if(mIsConfiguring){
            this.mApplication.removeEventListener(this)
            GlobalScope.launch(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                showConfigSuccessDialog()
            }
        }else{
            TelinkLightService.Instance().idleMode(true)
            TelinkLightService.Instance().disconnect()
        }
    }

    override fun onBackPressed() {
        showCancelDialog();
    }

    private var mIsConfiguring: Boolean = false

    private fun initListener() {
        this.mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        mApplication.addEventListener(ErrorReportEvent.ERROR_REPORT, this)

        fab.setOnClickListener { view ->
            if(TelinkLightApplication.getInstance().connectDevice==null){
                if(mConnectingSnackBar?.isShown != true){
                    mConfigFailSnackbar?.dismiss()
                    showDisconnectSnackBar()
                }
            }else{
                if (mAdapter.selectedPos != -1) {
                    progressBar.visibility = View.VISIBLE
                    Thread(Runnable {
                        setGroupForSwitch()
                        Thread.sleep(800)
                        Commander.updateMeshName(successCallback = {
                            mIsConfiguring = true
                            disconnect()
                        },
                                failedCallback = {
                                    mConfigFailSnackbar=snackbar(configGroupRoot, getString(R.string.group_failed))
                                    GlobalScope.launch(Dispatchers.Main) {
                                        progressBar.visibility = View.GONE
                                        mIsConfiguring = false
                                    }
                                })
                    }).start()
                } else {
                    snackbar(view, getString(R.string.please_select_group))
                }
            }
        }

        mAdapter.setOnItemChildClickListener { adapter, view, position ->
            when (view.id) {
                R.id.checkBox -> {
                    if (mAdapter.selectedPos != position) {
                        //取消上个Item的勾选状态
                        mGroupArrayList[mAdapter.selectedPos].checked = false
                        mAdapter.notifyItemChanged(mAdapter.selectedPos)

                        //设置新的item的勾选状态
                        mAdapter.selectedPos = position
                        mGroupArrayList[mAdapter.selectedPos].checked = true
                        mAdapter.notifyItemChanged(mAdapter.selectedPos)
                    } else {
                        mGroupArrayList[mAdapter.selectedPos].checked = true
                        mAdapter.notifyItemChanged(mAdapter.selectedPos)
                    }
                }
            }
        }

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

    private fun onErrorReport(info: ErrorReportInfo) {
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        com.dadoutek.uled.util.LogUtils.d("蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        com.dadoutek.uled.util.LogUtils.d("无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        com.dadoutek.uled.util.LogUtils.d("未扫到目标设备")
                    }
                }
                showDisconnectSnackBar()

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        com.dadoutek.uled.util.LogUtils.d("未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        com.dadoutek.uled.util.LogUtils.d("未建立物理连接")

                    }
                }
                showDisconnectSnackBar()

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        com.dadoutek.uled.util.LogUtils.d("value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        com.dadoutek.uled.util.LogUtils.d("read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        com.dadoutek.uled.util.LogUtils.d("write login data 没有收到response")
                    }
                }
                LogUtils.d("onError login")
                showDisconnectSnackBar()

            }
        }
    }

    private fun showDisconnectSnackBar() {
        TelinkLightService.Instance().idleMode(true)
        mDisconnectSnackBar = indefiniteSnackbar(configGroupRoot, getString(R
                .string.device_disconnected), getString(R.string.reconnect)) {
            reconnect()
        }
    }

    private fun onDeviceStatusChanged(deviceEvent: DeviceEvent) {
        val deviceInfo = deviceEvent.args
//        mDeviceMeshName = deviceInfo.meshName
        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                mConnectingSnackBar?.dismiss()
                mConnectedSnackBar = snackbar(configGroupRoot, R.string.connect_success)
                progressBar.visibility=View.GONE
            }


            LightAdapter.STATUS_LOGOUT -> {
//                onLoginFailed()
                if (mIsDisconnecting) {
                    this.mApplication.removeEventListener(this)

                    GlobalScope.launch(Dispatchers.Main) {
                        delay(200)
                        progressBar.visibility = View.GONE
                        finish()
                    }
                } else if (mIsConfiguring) {
                    this.mApplication.removeEventListener(this)
                    GlobalScope.launch(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        showConfigSuccessDialog()
                    }
                } else {
                    showDisconnectSnackBar()

                    LogUtils.d("Disconnected")
                }
            }
        }

    }

    private fun showConfigSuccessDialog() {
        try{
            if(isGlassSwitch){
                TelinkLightService.Instance().idleMode(true)
                TelinkLightService.Instance().disconnect()
                ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
            }else{
                AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.install_success)
                        .setMessage(R.string.tip_config_switch_success)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            TelinkLightService.Instance().idleMode(true)
                            TelinkLightService.Instance().disconnect()
                            ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
                        }
                        .show()
            }
        }catch (e:Exception){
            e.printStackTrace()
        }finally {
            //确认配置成功后,添加开关到服务器
            var newMeshAdress: Int
//            var dbSwitch:DbSwitch=DbSwitch()
            newMeshAdress=groupAdress
//            DBUtils.saveSwitch(dbSwitch,false)
            var dbSwitch: DbSwitch?=DbSwitch()
            DBUtils.saveSwitch(dbSwitch,false)
            dbSwitch!!.belongGroupId=mGroupArrayList.get(mAdapter.selectedPos).id
            dbSwitch.macAddr=mDeviceInfo.macAddress
            dbSwitch.meshAddr=Constant.SWITCH_PIR_ADDRESS
            dbSwitch.productUUID=mDeviceInfo.productUUID
            dbSwitch.index=dbSwitch.id.toInt()
            dbSwitch.name=StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID)

//            dbSwitch= DBUtils.getSwitchByMeshAddr(newMeshAdress)!!
            DBUtils.saveSwitch(dbSwitch,false)
            dbSwitch= DBUtils.getSwitchByMacAddr(mDeviceInfo.macAddress)
            DBUtils.recordingChange(dbSwitch!!.id,
                    DaoSessionInstance.getInstance().dbSwitchDao.tablename,
                    Constant.DB_ADD)

        }
    }

    private var mConnectingSnackBar: Snackbar? = null

    private fun reconnect() {

        //自动重连参数
        val connectParams = Parameters.createAutoConnectParameters()
        connectParams.setMeshName(mDeviceInfo.meshName)

        val mesh = TelinkLightApplication.getApp().mesh
        val pwd: String
        if (mDeviceInfo.meshName == Constant.PIR_SWITCH_MESH_NAME) {
            pwd = mesh.factoryPassword.toString()
        } else {
            pwd = NetworkFactory.md5(NetworkFactory.md5(mDeviceInfo.meshName) + mDeviceInfo.meshName)
                    .substring(0, 16)
        }
        connectParams.setPassword(pwd)
        connectParams.autoEnableNotification(true)
        connectParams.setTimeoutSeconds(CONNECT_TIMEOUT)
        connectParams.setConnectMac(mDeviceInfo.macAddress)

        mDisconnectSnackBar?.dismiss()
        mConnectingSnackBar = indefiniteSnackbar(configGroupRoot, getString(R
                .string.connecting))

        TelinkLightService.Instance().autoConnect(connectParams)

    }

    private fun setGroupForSwitch() {
        val mesh = this.mApplication.mesh
        val params = Parameters.createUpdateParameters()
        if (BuildConfig.DEBUG) {
            params.setOldMeshName(Constant.PIR_SWITCH_MESH_NAME)
        } else {
            params.setOldMeshName(mesh.factoryName)
        }
        params.setOldPassword(mesh.factoryPassword)
        params.setNewMeshName(mesh.name)
        val account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                Constant.DB_NAME_KEY, "dadou")
        if (SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                        Constant.USER_TYPE, Constant.USER_TYPE_OLD) == Constant.USER_TYPE_NEW) {
            params.setNewPassword(NetworkFactory.md5(
                    NetworkFactory.md5(mesh?.password) + account))
        } else {
            params.setNewPassword(mesh?.password)
        }

        params.setUpdateDeviceList(mDeviceInfo)
        val groupAddress = mGroupArrayList.get(mAdapter.selectedPos).meshAddr
        val paramBytes = byteArrayOf(0x01, (groupAddress and 0xFF).toByte(), //0x01 代表添加组
                (groupAddress shr 8 and 0xFF).toByte())
        TelinkLightService.Instance().sendCommandNoResponse(Opcode.SET_GROUP, mDeviceInfo.meshAddress,
                paramBytes)
//        Commander.addGroup(mDeviceInfo.meshAddress,groupAddress,)
    }

    private fun initView() {
        mDeviceInfo = intent.getParcelableExtra<DeviceInfo>("deviceInfo")
        val mesh = mApplication.mesh
//        val dataManager = DataManager(this, mesh.name, mesh.password)
        mGroupArrayList = ArrayList<DbGroup>()
        val groupList = DBUtils.groupList
//        mGroupArrayList.add(dataManager.createAllLightControllerGroup()) //添加全控

        for (group in groupList) {
//            if (group.containsLightList.size > 0 || group.meshAddress == 0xFFFF)
            group.checked = false
            mGroupArrayList.add(group)
        }
        if (groupList.size > 0) {
            groupList[0].checked = true
        }

        mAdapter = SelectSwitchGroupRvAdapter(R.layout.item_select_switch_group_rv, mGroupArrayList)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        mAdapter.bindToRecyclerView(recyclerView)

    }

    val groupAdress: Int
        get() {
            val list = DBUtils.groupList
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
