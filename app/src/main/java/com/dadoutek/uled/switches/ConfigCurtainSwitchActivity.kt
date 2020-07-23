package com.dadoutek.uled.switches

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.BuildConfig
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DBUtils.recordingChange
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.MeshAddressGenerator
import com.dadoutek.uled.util.OtherUtils
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
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar


private const val CONNECT_TIMEOUT = 5

class ConfigCurtainSwitchActivity : BaseSwitchActivity(), EventListener<String> {
    private var currentGroup: DbGroup? = null
    private val requestCodeNum: Int = 1000
    private var newMeshAddr: Int = 0
    private var version: String? = null
    private lateinit var mDeviceInfo: DeviceInfo
    private lateinit var mAdapter: SelectSwitchGroupRvAdapter
    private lateinit var mGroupArrayList: ArrayList<DbGroup>
    private var mDisconnectSnackBar: Snackbar? = null
    private var mConnectedSnackBar: Snackbar? = null
    private var mConfigFailSnackbar: Snackbar? = null
    private var isGlassSwitch = false
    private var groupName: String? = null
    private var switchDate: DbSwitch? = null
    override fun setToolBar(): android.support.v7.widget.Toolbar {
        return toolbar
    }

    override fun setReConfig(): Boolean {
        return isReConfig
    }

    override fun setVersion() {
        if (TextUtils.isEmpty(version))
            version = getString(R.string.get_version_fail)
        fiVersion?.title = version
    }

    override fun setConnectMeshAddr(): Int {
       return mDeviceInfo?.meshAddress?:0
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

    override fun setLayoutId(): Int {
        return R.layout.activity_switch_group
    }

    override fun initView() {
        makePop()
        toolbarTv?.text = getString(R.string.curtain_switch)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.inflateMenu(R.menu.menu_rgb_light_setting)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        version = intent.getStringExtra("version")

        // tvLightVersion?.text = version
        if (version!!.startsWith("ST") || (version!!.contains("BT") || version!!.contains("BTL") || version!!.contains("BTS"))) {
            isGlassSwitch = true
        }
        groupName = intent.getStringExtra("group")
        isReConfig = groupName != null && groupName == "true"

        if (isReConfig) {
            switchDate = this.intent.extras!!.get("switch") as DbSwitch
            toolbarTv.text = switchDate?.name
        }
        mGroupArrayList = ArrayList()
        val groupList = DBUtils.groupList

        for (group in groupList) {
            if (OtherUtils.isCurtain(group)) {
                group.checked = false
                mGroupArrayList.add(group)
            }
        }
        if (mGroupArrayList.size > 0) {
            mGroupArrayList[0].checked = true
        }

       // mAdapter = SelectSwitchGroupRvAdapter(R.layout.item_select_switch_group_rv, mGroupArrayList)
        //recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        //recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
       // mAdapter.bindToRecyclerView(recyclerView)
    }

    private fun makePop() {
        popReNameView = View.inflate(this, R.layout.pop_rename, null)
        renameEt = popReNameView?.findViewById<EditText>(R.id.pop_rename_edt)
        renameCancel = popReNameView?.findViewById<TextView>(R.id.pop_rename_cancel)
        renameConfirm = popReNameView?.findViewById<TextView>(R.id.pop_rename_confirm)
        renameConfirm?.setOnClickListener {
            // 获取输入框的内容
            if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                switchDate?.name = renameEt?.text.toString().trim { it <= ' ' }
                DBUtils.updateSwicth(switchDate!!)
                if (this != null && !this.isFinishing) {
                    renameDialog?.dismiss()
                }
            }
        }
        renameCancel?.setOnClickListener {
            if (this != null && !this.isFinishing) {
                renameDialog?.dismiss()
            }
        }

        renameDialog = Dialog(this)
        renameDialog!!.setContentView(popReNameView)
        renameDialog!!.setCanceledOnTouchOutside(false)

        renameDialog?.setOnDismissListener {
            switchDate?.name = renameEt?.text.toString().trim { it <= ' ' }
            if (switchDate != null)
                DBUtils.updateSwicth(switchDate!!)
            showConfigSuccessDialog()
        }
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(this)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (TelinkLightApplication.getApp().connectDevice == null) {
                        sw_progressBar.visibility = View.VISIBLE
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
                //showCancelDialog()
                configReturn()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private var mIsDisconnecting: Boolean = false

    private fun disconnect() {
        if (mIsConfiguring) {
            mApp?.removeEventListener(this)
            GlobalScope.launch(Dispatchers.Main) {
                sw_progressBar.visibility = View.GONE
                showConfigSuccessDialog()
            }
        } else {
            TelinkLightService.Instance()?.idleMode(true)
            TelinkLightService.Instance()?.disconnect()
        }
    }


    override fun onBackPressed() {
        //showCancelDialog()
        configReturn()
    }

    private var mIsConfiguring: Boolean = false

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodeNum) {
            currentGroup = data?.getSerializableExtra(Constant.EIGHT_SWITCH_TYPE) as DbGroup
            group_name.text = currentGroup?.name
        }
    }

    override fun initListener() {
        mApp?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        mApp?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)

        select_group.setOnClickListener {
            val intent = Intent(this@ConfigCurtainSwitchActivity, ChooseGroupOrSceneActivity::class.java)
            val bundle = Bundle()
            bundle.putInt(Constant.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
            bundle.putInt(Constant.DEVICE_TYPE, Constant.DEVICE_TYPE_CURTAIN.toInt())
            intent.putExtras(bundle)
            startActivityForResult(intent, requestCodeNum)
            setResult(Constant.RESULT_OK)
        }

        fab.setOnClickListener { view ->
            if (TelinkLightApplication.getApp().connectDevice == null) {
                if (mConnectingSnackBar?.isShown != true) {
                    mConfigFailSnackbar?.dismiss()
                    showDisconnectSnackBar()
                }
            } else {
                if (currentGroup == null) {
                    ToastUtils.showShort(getString(R.string.please_select_group))
                    return@setOnClickListener
                }
                if (mAdapter.selectedPos != -1) {
                    sw_progressBar.visibility = View.VISIBLE
                    setGroupForSwitch()
                    newMeshAddr = MeshAddressGenerator().meshAddress.get()
                    Commander.updateMeshName(newMeshAddr = newMeshAddr, successCallback = {
                        mDeviceInfo.meshAddress = newMeshAddr
                        mIsConfiguring = true
                        updateSwitch()
                        if (switchDate == null)
                            switchDate = DBUtils.getSwitchByMeshAddr(mDeviceInfo.meshAddress)
                        if (!isReConfig)
                            showRenameDialog(switchDate)
                    },
                            failedCallback = {
                                mConfigFailSnackbar = snackbar(configGroupRoot, getString(R.string.group_failed))
                                GlobalScope.launch(Dispatchers.Main) {
                                    sw_progressBar.visibility = View.GONE
                                    mIsConfiguring = false
                                }
                            })
                } else {
                    snackbar(view, getString(R.string.please_select_group))
                }
            }
        }

        /*  mAdapter.setOnItemChildClickListener { _, view, position ->
              when (view.id) {
                  R.id.checkBox -> {
                      if (mAdapter.selectedPos != position) {
                          //取消上个Item的勾选状态
                          currentGroup?.checked = false
                          mAdapter.notifyItemChanged(mAdapter.selectedPos)

                          //设置新的item的勾选状态
                          mAdapter.selectedPos = position
                          currentGroup?.checked = true
                          mAdapter.notifyItemChanged(mAdapter.selectedPos)
                      } else {
                          currentGroup?.checked = true
                          mAdapter.notifyItemChanged(mAdapter.selectedPos)
                      }
                  }
              }
          }*/

    }

    override fun initData() {
        TODO("Not yet implemented")
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
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> { }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> { }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> { }
                }
                showDisconnectSnackBar()
            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> { }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> { }
                }
                showDisconnectSnackBar()

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> { }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {}
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {}
                }
                showDisconnectSnackBar()
            }
        }
    }

    private fun showDisconnectSnackBar() {
        TelinkLightService.Instance()?.idleMode(true)
        mDisconnectSnackBar = indefiniteSnackbar(configGroupRoot, getString(R.string.device_disconnected), getString(R.string.reconnect)) {
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
                sw_progressBar.visibility = View.GONE
            }


            LightAdapter.STATUS_LOGOUT -> {
                when {
                    mIsDisconnecting -> {
                        mApp?.removeEventListener(this)

                        GlobalScope.launch(Dispatchers.Main) {
                            delay(200)
                            sw_progressBar.visibility = View.GONE
                            finish()
                        }
                    }
                    mIsConfiguring -> {
                        showRenameDialog(switchDate)
                    }
                    else -> showDisconnectSnackBar()
                }
            }
        }
    }

    private fun showConfigSuccessDialog() {
        try {
            if (isGlassSwitch) {
                TelinkLightService.Instance()?.idleMode(true)
                TelinkLightService.Instance()?.disconnect()
                ToastUtils.showLong(getString(R.string.config_success))
                ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
            } else {
                AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.install_success)
                        .setMessage(R.string.tip_config_switch_success)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            TelinkLightService.Instance()?.idleMode(true)
                            TelinkLightService.Instance()?.disconnect()
                            ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
                        }.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun updateSwitch() {
        if (groupName == "false") {
            var dbSwitch = DBUtils.getSwitchByMacAddr(mDeviceInfo.macAddress)
            if (dbSwitch != null) {
                dbSwitch!!.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID, this) + dbSwitch.meshAddr
                dbSwitch.belongGroupId = currentGroup?.id
                dbSwitch.controlGroupAddr = currentGroup!!.meshAddr
                dbSwitch.meshAddr = /*Constant.SWITCH_PIR_ADDRESS*/mDeviceInfo.meshAddress
                dbSwitch.version = mDeviceInfo.firmwareRevision
                DBUtils.updateSwicth(dbSwitch)
                switchDate = dbSwitch
            } else {
                var dbSwitch = DbSwitch()
                DBUtils.saveSwitch(dbSwitch, false)
                dbSwitch!!.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID, this) + switchDate!!.meshAddr
                dbSwitch.belongGroupId = currentGroup?.id
                dbSwitch.macAddr = mDeviceInfo.macAddress
                dbSwitch.meshAddr = /*Constant.SWITCH_PIR_ADDRESS*/mDeviceInfo.meshAddress
                dbSwitch.productUUID = mDeviceInfo.productUUID
                dbSwitch.version = mDeviceInfo.firmwareRevision
                dbSwitch.index = dbSwitch.id.toInt()

                DBUtils.saveSwitch(dbSwitch, false)
                recordingChange(dbSwitch.id, DaoSessionInstance.getInstance().dbSwitchDao.tablename, Constant.DB_ADD)
                switchDate = dbSwitch
            }
        } else {
            switchDate!!.belongGroupId = currentGroup?.id
            switchDate!!.controlGroupAddr = currentGroup!!.meshAddr
            //switchDate!!.meshAddr = Constant.SWITCH_PIR_ADDRESS
            switchDate!!.meshAddr = MeshAddressGenerator().meshAddress.get()
            DBUtils.updateSwicth(switchDate!!)
        }
    }

    private var mConnectingSnackBar: Snackbar? = null

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
        mConnectingSnackBar = indefiniteSnackbar(configGroupRoot, getString(R.string.connecting))

        TelinkLightService.Instance()?.autoConnect(connectParams)
    }

    private fun setGroupForSwitch() {
        val mesh = mApp?.mesh
        val params = Parameters.createUpdateParameters()
        if (BuildConfig.DEBUG) {
            params.setOldMeshName(Constant.PIR_SWITCH_MESH_NAME)
        } else {
            params.setOldMeshName(mesh?.factoryName)
        }
        params.setOldPassword(mesh?.factoryPassword)
        params.setNewMeshName(mesh?.name)
        params.setNewPassword(NetworkFactory.md5(NetworkFactory.md5(mesh?.name) + mesh?.name))

        params.setUpdateDeviceList(mDeviceInfo)
        val groupAddress = currentGroup!!.meshAddr
        val paramBytes = byteArrayOf(0x01, (groupAddress and 0xFF).toByte(), //0x01 代表添加组
                (groupAddress shr 8 and 0xFF).toByte())
        TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.SET_GROUP, mDeviceInfo.meshAddress, paramBytes)
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
}
