package com.dadoutek.uled.switches

import android.annotation.SuppressLint
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
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.BuildConfig
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DBUtils.recordingChange
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbSwitch
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
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
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_switch_group.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.snackbar
import java.util.concurrent.TimeUnit


private const val CONNECT_TIMEOUT = 5

class ConfigCurtainSwitchActivity : BaseSwitchActivity(), EventListener<String> {
    private var currentGroup: DbGroup? = null
    private val requestCodeNum: Int = 1000
    private var newMeshAddr: Int = 0
    private var version: String? = null
    private lateinit var mDeviceInfo: DeviceInfo
    private lateinit var mGroupArrayList: ArrayList<DbGroup>
    private var mDisconnectSnackBar: Snackbar? = null
    private var mConnectedSnackBar: Snackbar? = null
    private var mConfigFailSnackbar: Snackbar? = null
    private var isGlassSwitch = false
    private var groupName: String? = null
    override fun setToolBar(): android.support.v7.widget.Toolbar {
        return toolbar
    }

    override fun setReConfig(): Boolean {
        return isReConfig
    }

    override fun setVersion() {
        if (TextUtils.isEmpty(version))
            version = getString(R.string.get_version_fail)
        else
            mDeviceInfo?.firmwareRevision = version
        fiVersion?.title = version
    }

    override fun setConnectMeshAddr(): Int {
        return mDeviceInfo?.meshAddress
    }


    override fun deleteDevice() {
        deleteSwitch(mDeviceInfo.macAddress)
    }

    override fun goOta() {
        if (isReConfig)
            mDeviceInfo.boundMac = switchDate?.boundMac
        deviceOta(mDeviceInfo)
    }

    override fun reName() {
        showRenameDialog(switchDate, false)
    }

    override fun setLayoutId(): Int {
        return R.layout.activity_switch_group
    }

    override fun initView() {
        toolbarTv?.text = getString(R.string.curtain_switch)
    }
  private fun makePop() {
      renameConfirm?.setOnClickListener {
          // 获取输入框的内容
          if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
              ToastUtils.showLong(getString(R.string.rename_tip_check))
          } else {
              val trim = renameEt?.text.toString().trim { it <= ' ' }
              if (!Constant.IS_ROUTE_MODE)
                  renameSw(trim)
              else
                  routerRenameSw(switchDate!!, trim)

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

    private fun renameSw(trim: String) {
        switchDate?.name = trim
        if (switchDate != null) {
            toolbarTv.text = switchDate?.name
            DBUtils.updateSwicth(switchDate!!)
        } else
            ToastUtils.showLong(getString(R.string.rename_faile))
    }

    override fun routerRenameSwSuccess(trim: String) {
        renameSw(trim)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                configReturn()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private var mIsDisconnecting: Boolean = false

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
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        select_group.setOnClickListener {
            val intent = Intent(this@ConfigCurtainSwitchActivity, ChooseGroupOrSceneActivity::class.java)
            val bundle = Bundle()
            bundle.putInt(Constant.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
            bundle.putInt(Constant.DEVICE_TYPE, Constant.DEVICE_TYPE_CURTAIN.toInt())
            intent.putExtras(bundle)
            startActivityForResult(intent, requestCodeNum)
            setResult(Constant.RESULT_OK)
        }

        fab.setOnClickListener {
            if (TelinkLightApplication.getApp().connectDevice == null && !Constant.IS_ROUTE_MODE) {
                if (mConnectingSnackBar?.isShown != true) {
                    mConfigFailSnackbar?.dismiss()
                    showDisconnectSnackBar()
                }
            } else {
                if (currentGroup == null) {
                    ToastUtils.showShort(getString(R.string.please_select_group))
                    return@setOnClickListener
                }
                if (!Constant.IS_ROUTE_MODE)
                    configSw()
                else
                    routerConfigSw()

            }
        }
    }

    @SuppressLint("CheckResult")
    private fun routerConfigSw() {
        RouterModel.configNormalSw(switchDate!!.id, currentGroup!!.meshAddr, "configNormalSw")
                ?.subscribe({
                    //    "errorCode": 90021, "该开关不存在，请重新刷新数据"   "errorCode": 90008,"该开关没有绑定路由，无法配置"
                    //    "errorCode": 90007,"该组不存在，刷新组列表"   "errorCode": 90005,"以下路由没有上线，无法配置"
                    when (it.errorCode) {
                        0 -> {
                            disposableRouteTimer?.dispose()
                            disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                                    .subscribe {
                                        sw_progressBar.visibility = View.GONE
                                        ToastUtils.showShort(getString(R.string.config_fail))
                                    }
                        }
                        90021 -> {
                            ToastUtils.showShort(getString(R.string.device_not_exit))
                            finish()
                        }
                        900018 -> ToastUtils.showShort(getString(R.string.device_not_exit))
                        90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                        90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                        90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                        else-> ToastUtils.showShort(it.message)
                    }
                }, { ToastUtils.showShort(it.message) })
    }

    override fun tzRouterConfigNormalSwRecevice(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由配置普通开关通知-------$cmdBean")
        if (cmdBean.ser_id == "configNormalSw") {
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

    private fun configSw() {
        sw_progressBar.visibility = View.VISIBLE
        setGroupForSwitch()
        Thread.sleep(300)
        newMeshAddr = if (isReConfig) mDeviceInfo.meshAddress else MeshAddressGenerator().meshAddress.get()
        LogUtils.v("zcl-----------更新开关新mesh-------${newMeshAddr}")
        Commander.updateMeshName(newMeshAddr = newMeshAddr, successCallback = {
            mDeviceInfo.meshAddress = newMeshAddr
            mIsConfiguring = true
            sw_progressBar.visibility = View.GONE
            updateSwitch()
            ToastUtils.showShort(getString(R.string.config_success))
            if (switchDate == null)
                switchDate = DBUtils.getSwitchByMeshAddr(mDeviceInfo.meshAddress)
            if (!isReConfig)
                showRenameDialog(switchDate, false)
            else
                finish()
        },
                failedCallback = {
                    mConfigFailSnackbar = snackbar(configGroupRoot, getString(R.string.group_failed))
                    GlobalScope.launch(Dispatchers.Main) {
                        sw_progressBar.visibility = View.GONE
                        mIsConfiguring = false
                    }
                })
    }

    override fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        version = intent.getStringExtra("version")
        sw_normal_curtain.visibility = View.VISIBLE
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
        if (mGroupArrayList.size > 0)
            mGroupArrayList[0].checked = true
        makePop()
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
        if (info.stateCode == ErrorReportEvent.STATE_CONNECT || info.stateCode == ErrorReportEvent.STATE_SCAN || info.stateCode == ErrorReportEvent.STATE_LOGIN) {
            showDisconnectSnackBar()
        }
    }

    private fun showDisconnectSnackBar() {
        TelinkLightService.Instance()?.idleMode(true)
        reconnect()
    }

    private fun onDeviceStatusChanged(deviceEvent: DeviceEvent) {
        val deviceInfo = deviceEvent.args
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
                        showRenameDialog(switchDate, false)
                    }
                    else -> showDisconnectSnackBar()
                }
            }
        }
    }


    private fun updateSwitch() {
        if (groupName == "false") {
            var dbSwitch = DBUtils.getSwitchByMacAddr(mDeviceInfo.macAddress)
            if (dbSwitch != null) {
                dbSwitch.belongGroupId = currentGroup?.id
                dbSwitch.controlGroupAddr = currentGroup!!.meshAddr
                dbSwitch.meshAddr = /*Constant.SWITCH_PIR_ADDRESS*/mDeviceInfo.meshAddress
                dbSwitch!!.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID, this) + dbSwitch.meshAddr
                dbSwitch.version = mDeviceInfo.firmwareRevision
                DBUtils.updateSwicth(dbSwitch)
                switchDate = dbSwitch
            } else {
                var dbSwitch = DbSwitch()
                DBUtils.saveSwitch(dbSwitch, false)
                dbSwitch.belongGroupId = currentGroup?.id
                dbSwitch.macAddr = mDeviceInfo.macAddress
                dbSwitch.meshAddr = /*Constant.SWITCH_PIR_ADDRESS*/mDeviceInfo.meshAddress
                dbSwitch!!.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID, this) + dbSwitch.meshAddr
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
            switchDate!!.meshAddr = mDeviceInfo?.meshAddress

            DBUtils.updateSwicth(switchDate!!)
        }
    }

    private var mConnectingSnackBar: Snackbar? = null

    private fun reconnect() {
        if (Constant.IS_ROUTE_MODE) return
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
        // mConnectingSnackBar = indefiniteSnackbar(configGroupRoot, getString(R.string.connecting_tip))
        ToastUtils.showShort(getString(R.string.connecting_tip))
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
