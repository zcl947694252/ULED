package com.dadoutek.uled.switches

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DBUtils.recordingChange
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbSwitch
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.MeshAddressGenerator
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.ErrorReportInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_switch_group.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


private const val CONNECT_TIMEOUT = 5

class ConfigNormalSwitchActivity : BaseSwitchActivity(), EventListener<String> {
    private var isTouchSw: Boolean = false
    private var disposableTimer: Disposable? = null
    private var deviceConfigType: Int = 0
    private var groupName: String? = null
    private var currentGroup: DbGroup? = null
    private val requestCodeNum: Int = 1000
    private lateinit var mDeviceInfo: DeviceInfo
    private lateinit var mGroupArrayList: ArrayList<DbGroup>
    private var localVersion: String = ""
    private var isGlassSwitch = false
    private var switchDate: DbSwitch? = null
    override fun setLayoutId(): Int {
        return R.layout.activity_switch_group
    }

    override fun setToolBar(): Toolbar {
        return toolbar
    }

    override fun setReConfig(): Boolean {
        return isReConfig
    }

    override fun initView() {
        toolbar?.title = getString(R.string.select_group)
        makePop()
    }

    override fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        groupName = intent.getStringExtra("group")
        localVersion = intent.getStringExtra("version")
        deviceConfigType = intent.getIntExtra("deviceType", 0)

        //st bt 全部为触摸开关
        isTouchSw = localVersion.contains("BT")
                || localVersion.contains("ST") || localVersion.contains("SS-2.0.1")
        //单调光光能开关 sl bl   触摸单调光 btl sts
        if (!isTouchSw) {
            if (localVersion.contains("SL") || localVersion.contains("BL"))
                sw_normal_iv.setImageResource(R.drawable.sw_normal_single)
            else
                sw_normal_iv.setImageResource(R.drawable.sw_normal_add_minus)
            toolbarTv.text = getString(R.string.light_sw)
        } else {
            toolbarTv.text = getString(R.string.touch_sw)
            if (localVersion.contains("BTL") || localVersion.contains("STS"))
                sw_normal_iv.setImageResource(R.drawable.touch_sw_single)
            else
                sw_normal_iv.setImageResource(R.drawable.sw_touch_normal)
            //tLightVersion.text = localVersion
        }


        if (TextUtils.isEmpty(localVersion))
            localVersion = mDeviceInfo.firmwareRevision ?: ""
        if (TextUtils.isEmpty(localVersion))
            localVersion = getString(R.string.get_version_fail)
        fiVersion?.title = localVersion

        //tvLightVersion.text = localVersion
        //if (localVersion.contains("BT") || localVersion.contains("BTL") || localVersion.contains("BTS") || localVersion.contains("STS"))
        isGlassSwitch = isTouchSw

        isReConfig = groupName != null && groupName == "true"
        fiRename?.isVisible = isReConfig

        if (isReConfig) {
            switchDate = this.intent.extras!!.get("switch") as DbSwitch
            toolbarTv.text = switchDate?.name
        } else {
            groupName = "false"
        }

        mGroupArrayList = ArrayList()
        val groupList = DBUtils.groupList

        for (group in groupList)
            if (OtherUtils.isNormalGroup(group) || OtherUtils.isRGBGroup(group) || OtherUtils.isAllRightGroup(group) || OtherUtils.isConnector(group)) {
                group.checked = false
                mGroupArrayList.add(group)
            }

        if (mGroupArrayList.size > 0)
            mGroupArrayList[0].checked = true
    }

    override fun setVersion() {
        if (TextUtils.isEmpty(localVersion))
            localVersion = getString(R.string.get_version_fail)
        else
            mDeviceInfo?.firmwareRevision = localVersion
        fiVersion?.title = localVersion
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
        mApp?.removeEventListener(this)
        mApp?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        mApp?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)

        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        select_group.setOnClickListener {
            val intent = Intent(this@ConfigNormalSwitchActivity, ChooseGroupOrSceneActivity::class.java)
            //传入0代表是群组
            val bundle = Bundle()
            bundle.putInt(Constant.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
            bundle.putInt(Constant.DEVICE_TYPE, Constant.DEVICE_TYPE_LIGHT_SW.toInt())
            intent.putExtras(bundle)
            startActivityForResult(intent, requestCodeNum)
            setResult(Constant.RESULT_OK)
        }
        fab.setOnClickListener {
            if (currentGroup == null) {
                ToastUtils.showShort(getString(R.string.please_select_group))
                return@setOnClickListener
            }

            if (TelinkLightApplication.getApp().connectDevice == null&&!Constant.IS_ROUTE_MODE) {
                if (mConnectingSnackBar?.isShown != true) {
                    showDisconnectSnackBar()
                }
            } else {
                // (mAdapter.selectedPos != -1) {
                sw_progressBar.visibility = View.VISIBLE
                if (Constant.IS_ROUTE_MODE)
                    RouterModel.configNormalSw(switchDate!!.id, currentGroup!!.meshAddr,"configNormalSw")
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
                                }
                            }, { ToastUtils.showShort(it.message) })
                else
                    Thread(Runnable {
                        sendGroupForSwitch()
                        Thread.sleep(800)
                        //val newMeshAddr = Constant.SWITCH_PIR_ADDRESS
                        val newMeshAddr = if (isReConfig)
                            mDeviceInfo?.meshAddress else MeshAddressGenerator().meshAddress.get()
                        LogUtils.v("zcl-----------开关新mesh-------$newMeshAddr")
                        Commander.updateMeshName(newMeshAddr = newMeshAddr,
                                successCallback = {
                                    mDeviceInfo.meshAddress = newMeshAddr
                                    mIsConfiguring = true
                                    updateSwitch()
                                    disconnect()
                                    if (switchDate == null)
                                        switchDate = DBUtils.getSwitchByMeshAddr(mDeviceInfo.meshAddress)
                                    ToastUtils.showShort(getString(R.string.config_success))
                                    if (!isReConfig)
                                        showRenameDialog(switchDate)
                                    else
                                        finish()
                                },
                                failedCallback = {
                                    GlobalScope.launch(Dispatchers.Main) {
                                        sw_progressBar.visibility = View.GONE
                                        mIsConfiguring = false
                                    }
                                })
                    }).start()
                /*  } else {
                      snackbar(view, getString(R.string.please_select_group))
                  }*/
            }
        }

        /*      mAdapter.setOnItemChildClickListener { _, view, position ->
                  when (view.id) {
                      R.id.checkBox -> {
                          if (mAdapter.selectedPos != position) {
                              //取消上个Item的勾选状态
                              currentGroup!!.checked = false
                              mAdapter.notifyItemChanged(mAdapter.selectedPos)

                              //设置新的item的勾选状态
                              mAdapter.selectedPos = position
                              currentGroup!!.checked = true
                              mAdapter.notifyItemChanged(mAdapter.selectedPos)
                          } else {
                              currentGroup!!.checked = true
                              mAdapter.notifyItemChanged(mAdapter.selectedPos)
                          }
                      }
                  }
              }*/
    }

    override fun onDestroy() {
        super.onDestroy()
        mApp?.removeEventListener(this)
        TelinkLightService.Instance()?.idleMode(true)
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
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                    }
                }
                showDisconnectSnackBar()

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                    }
                }
                showDisconnectSnackBar()
            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                    }
                }
                showDisconnectSnackBar()
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun showDisconnectSnackBar() {
        if (!Constant.IS_ROUTE_MODE) {
            TelinkLightService.Instance()?.idleMode(true)
            reconnect()
        } else {
            routerRetrySw(switchDate?.id ?: 0)
        }
    }


    @SuppressLint("CheckResult")
    override fun tzRouterConnectSwSeRecevice(cmdBean: CmdBodyBean) {
        if (cmdBean.ser_id=="retryConnectSw")
        if (cmdBean.finish) {
            if (cmdBean.status == 0) {
                ToastUtils.showShort(getString(R.string.connect_success))
                image_bluetooth.setImageResource(R.drawable.icon_cloud)
            } else {
                image_bluetooth.setImageResource(R.drawable.bluetooth_no)
                ToastUtils.showShort(getString(R.string.connect_fail))
            }
        }
    }

    override fun tzRouterConfigNormalSwRecevice(cmdBean: CmdBodyBean) {
              LogUtils.v("zcl-----------收到路由配置普通开关通知-------$cmdBean")
                      if (cmdBean.ser_id=="configNormalSw"){
                          disposableRouteTimer?.dispose()
                          hideLoadingDialog()
                          if (cmdBean.status==0){
                              GlobalScope.launch(Dispatchers.Main) {
                                  ToastUtils.showShort(getString(R.string.config_success))
                                  if (!isReConfig)
                                      showRenameDialog(switchDate)
                                  else
                                      finish()
                              }
                          }else{
                              ToastUtils.showShort(getString(R.string.config_fail))
                          }
                      }
    }

    private fun onDeviceStatusChanged(deviceEvent: DeviceEvent) {
        val deviceInfo = deviceEvent.args
//        mDeviceMeshName = deviceInfo.meshName
        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                mConnectingSnackBar?.dismiss()
                sw_progressBar.visibility = View.GONE
            }

            LightAdapter.STATUS_LOGOUT -> {
                when {
                    mIsDisconnecting -> {
                        this.mApp?.removeEventListener(this)
                        GlobalScope.launch(Dispatchers.Main) {
                            delay(200)
                            sw_progressBar.visibility = View.GONE
                            finish()
                        }
                    }
                    mIsConfiguring -> {
                        mApp?.removeEventListener(this)
                        GlobalScope.launch(Dispatchers.Main) {
                            sw_progressBar.visibility = View.GONE
                            showConfigSuccessDialog()
                        }
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
                        }
                        .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateSwitch() {
        if (groupName == "false") {
            var dbSwitch = DBUtils.getSwitchByMacAddr(mDeviceInfo.macAddress)
            if (dbSwitch != null) {
                dbSwitch.belongGroupId = currentGroup!!.id
                dbSwitch?.meshAddr = mDeviceInfo?.meshAddress
                dbSwitch.controlGroupAddr = currentGroup!!.meshAddr
                dbSwitch!!.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID, this) + dbSwitch.meshAddr
                dbSwitch.version = mDeviceInfo.firmwareRevision
                DBUtils.updateSwicth(dbSwitch)
                switchDate = dbSwitch
            } else {
                var dbSwitch = DbSwitch()
                DBUtils.saveSwitch(dbSwitch, false)
                dbSwitch.belongGroupId = currentGroup!!.id
                dbSwitch.macAddr = mDeviceInfo.macAddress
                dbSwitch.meshAddr = mDeviceInfo.meshAddress
                dbSwitch.productUUID = mDeviceInfo.productUUID
                dbSwitch.version = mDeviceInfo.firmwareRevision
                dbSwitch.index = dbSwitch.id.toInt()
                dbSwitch.controlGroupAddr = currentGroup!!.meshAddr

                Log.e("zcl", "zcl*****设置新的开关使用插入替换$dbSwitch")
                DBUtils.saveSwitch(dbSwitch, false)

                LogUtils.e("zcl", "zcl*****设置新的开关使用插入替换" + DBUtils.getAllSwitch())
                val gotSwitchByMac = DBUtils.getSwitchByMacAddr(mDeviceInfo.macAddress)
                recordingChange(gotSwitchByMac?.id, DaoSessionInstance.getInstance().dbSwitchDao.tablename, Constant.DB_ADD)
                switchDate = dbSwitch
            }
            switchDate = dbSwitch
        } else {
            switchDate?.meshAddr = mDeviceInfo?.meshAddress
            switchDate!!.belongGroupId = currentGroup!!.id
            switchDate!!.controlGroupAddr = currentGroup!!.meshAddr
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
            NetworkFactory.md5(NetworkFactory.md5(mDeviceInfo.meshName) + mDeviceInfo.meshName).substring(0, 16)
        }
        connectParams.setPassword(pwd)
        connectParams.autoEnableNotification(true)
        connectParams.setTimeoutSeconds(CONNECT_TIMEOUT)
        connectParams.setConnectMac(mDeviceInfo.macAddress)

        //  mConnectingSnackBar = indefiniteSnackbar(configGroupRoot, getString(R.string.connecting))
        ToastUtils.showShort(getString(R.string.connecting_tip))
        TelinkLightService.Instance()?.autoConnect(connectParams)
    }

    private fun sendGroupForSwitch() {
        val groupAddress = currentGroup!!.meshAddr
        val paramBytes = byteArrayOf(0x01, (groupAddress and 0xFF).toByte(), (groupAddress shr 8 and 0xFF).toByte())
        //0x01 代表添加组
        TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.SET_GROUP, mDeviceInfo.meshAddress, paramBytes)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (KeyEvent.KEYCODE_BACK == keyCode)
            finish()
        return super.onKeyDown(keyCode, event)
    }


}
