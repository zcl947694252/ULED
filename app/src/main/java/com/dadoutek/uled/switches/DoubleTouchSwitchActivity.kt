package com.dadoutek.uled.switches

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.util.GsonUtil
import com.dadoutek.uled.model.Constants
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbSwitch
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.MeshAddressGenerator
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_switch_double_touch.*
import kotlinx.android.synthetic.main.bottom_version_ly.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2020/4/26 14:54
 * 描述  41-6c
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */

class DoubleTouchSwitchActivity : BaseSwitchActivity(), View.OnClickListener {
    private var disposableTimer: Disposable? = null
    private lateinit var localVersion: String
    private var isRetryConfig: String? = null
    private lateinit var mDeviceInfo: DeviceInfo
    private var leftGroup: DbGroup? = null
    private var rightGroup: DbGroup? = null
    private val requestCodeNum: Int = 1000
    private var isLeft: Boolean = false

    override fun initView() {
        switch_double_touch_mb.visibility = View.GONE
        switch_double_touch_set.visibility = View.VISIBLE
        switch_double_touch_i_know.paint.color = getColor(R.color.white)
        switch_double_touch_i_know.paint.flags = Paint.UNDERLINE_TEXT_FLAG //下划线
        toolbarTv.text = getString(R.string.double_switch)
        makePop()
    }

    override fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        isRetryConfig = intent.getStringExtra("group")
        localVersion = intent.getStringExtra("version")
        eight_switch_versionLayout.setBackgroundColor(getColor(R.color.transparent))
//        bottom_version_number.text = localVersion
        toolbarTv.text = getString(R.string.touch_sw)

        isReConfig = isRetryConfig != null && isRetryConfig == "true"
        fiRename?.isVisible = isReConfig

        if (isReConfig) {
            switchDate = this.intent.extras!!.get("switch") as DbSwitch
            toolbarTv.text = switchDate?.name
            switchDate?.let {
                val stringToList = GsonUtil.stringToList<Double>(it.controlGroupAddrs)
                if (stringToList == null /*|| stringToList.size < 2*/) {
                    ToastUtils.showShort(getString(R.string.invalid_data))
                    finish()
                } else {
                    if (stringToList.size >= 2) {
                        val meshL = stringToList[0]
                        if (meshL != null && meshL.toInt() != 1000000) {
                            leftGroup = DBUtils.getGroupByMeshAddr(meshL.toInt())
                            switch_double_touch_left_tv.text = leftGroup?.name
                        }
                        val meshR = stringToList[1]
                        if (meshR != null && meshR.toInt() != 1000000) {
                            rightGroup = DBUtils.getGroupByMeshAddr(meshR.toInt())
                            switch_double_touch_right_tv.text = rightGroup?.name
                        }
                    }
                }
            }
        }
    }

    override fun setToolBar(): Toolbar {
        return toolbar
    }

    override fun setReConfig(): Boolean {
        return isReConfig
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
        showRenameDialog(switchDate, false)
    }

    override fun setLayoutId(): Int {
        return R.layout.activity_switch_double_touch
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeNum && resultCode == Activity.RESULT_OK) {
            var group = data?.getSerializableExtra(Constants.EIGHT_SWITCH_TYPE) as DbGroup
            if (isLeft) {
                switch_double_touch_left_tv.text = group.name
                leftGroup = group
            } else {
                switch_double_touch_right_tv.text = group.name
                rightGroup = group
            }
        }
    }

    private fun skipSelectGroup() {
        val intent = Intent(this@DoubleTouchSwitchActivity, ChooseGroupOrSceneActivity::class.java)
        //传入0代表是群组
        val bundle = Bundle()
        bundle.putInt(Constants.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
        bundle.putInt(Constants.DEVICE_TYPE, Constants.DEVICE_TYPE_LIGHT_SW.toInt())
        intent.putExtras(bundle)
        startActivityForResult(intent, requestCodeNum)
    }

    private fun upSwitchData() {
        //val newMeshAddr = Constant.SWITCH_PIR_ADDRESS
        val newMeshAddr = if (isReConfig)
            mDeviceInfo.meshAddress else MeshAddressGenerator().meshAddress.get()
        LogUtils.v("zcl-----------更新开关新mesh-------${newMeshAddr}")
        Commander.updateMeshName(newMeshAddr = newMeshAddr,
                successCallback = {
                    hideLoadingDialog()
                    mDeviceInfo.meshAddress = newMeshAddr
                    updateSwitch()
                    disconnect()
                    if (switchDate == null)
                        switchDate = DBUtils.getSwitchByMeshAddr(mDeviceInfo.meshAddress)
                    GlobalScope.launch(Dispatchers.Main) {
                        ToastUtils.showShort(getString(R.string.config_success))
                        if (!isReConfig)
                            showRenameDialog(switchDate, false)
                        else
                            finish()
                    }
                },
                failedCallback = {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.group_failed))
                })
    }

    private fun updateSwitch() {
        if (isRetryConfig == "false") {
            var dbSwitch = DBUtils.getSwitchByMacAddr(mDeviceInfo.macAddress)
            if (dbSwitch != null) {
                dbSwitch!!.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID, this) + dbSwitch.meshAddr
                dbSwitch.controlGroupAddrs = GsonUtils.toJson(mutableListOf(leftGroup?.meshAddr ?: 1000000, rightGroup?.meshAddr
                        ?: 1000000))
                dbSwitch.meshAddr = /*Constant.SWITCH_PIR_ADDRESS*/mDeviceInfo.meshAddress
                DBUtils.updateSwicth(dbSwitch)
                switchDate = dbSwitch
            } else {
                var dbSwitch = DbSwitch()
                DBUtils.saveSwitch(dbSwitch, false)
                dbSwitch.macAddr = mDeviceInfo.macAddress
                dbSwitch.meshAddr = /*Constant.SWITCH_PIR_ADDRESS*/mDeviceInfo.meshAddress
                dbSwitch!!.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID, this) + dbSwitch.meshAddr
                dbSwitch.productUUID = mDeviceInfo.productUUID
                dbSwitch.index = dbSwitch.id.toInt()
                dbSwitch.controlGroupAddrs = GsonUtils.toJson(mutableListOf(leftGroup?.meshAddr ?: 1000000, rightGroup?.meshAddr
                        ?: 1000000))

                DBUtils.saveSwitch(dbSwitch, false)
                DBUtils.recordingChange(dbSwitch.id,
                        DaoSessionInstance.getInstance().dbSwitchDao.tablename,
                        Constants.DB_ADD)
                switchDate = dbSwitch
            }
        } else {
            switchDate!!.controlGroupAddrs = GsonUtils.toJson(mutableListOf(leftGroup?.meshAddr, rightGroup?.meshAddr))
            switchDate!!.meshAddr = mDeviceInfo?.meshAddress
            DBUtils.updateSwicth(switchDate!!)
        }
    }

    private fun disconnect() {
        TelinkLightService.Instance()?.idleMode(true)
        TelinkLightService.Instance()?.disconnect()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.switch_double_touch_use_button -> {
                if (leftGroup == null && rightGroup == null) {
                    ToastUtils.showShort(getString(R.string.config_night_light_select_group))
                } else {
                    if (Constants.IS_ROUTE_MODE) {
                        RouterModel.configDoubleSw(RouterListBody(switchDate!!.id, mutableListOf(leftGroup?.meshAddr ?: 0, rightGroup?.meshAddr
                                ?: 0), "configDoubleSw"))
                                ?.subscribe({
                                    //    "errorCode": 90021, "该开关不存在，请重新刷新数据"  "errorCode": 90008,"该开关没有绑定路由，无法配置"
                                    //    "errorCode": 90007,"该组不存在，刷新组列表"   "errorCode": 90005,"以下路由没有上线，无法配置"
                                    LogUtils.v("zcl-----------收到路由配置请求成功-------$it")
                                    when (it.errorCode) {
                                        0 -> {
                                            showLoadingDialog(getString(R.string.please_wait))
                                            disposableTimer?.dispose()
                                            disposableTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
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
                                        90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                                        90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                                        90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                                        else-> ToastUtils.showShort(it.message)
                                    }
                                }, { ToastUtils.showShort(it.message) })
                    } else{
                        showLoadingDialog(getString(R.string.please_wait))
                        GlobalScope.launch {
                            setGroupForSwitch()
                            delay(800)
                            upSwitchData()
                        }
                    }
                }
            }
            R.id.switch_double_touch_left -> {
                isLeft = true
                skipSelectGroup()
            }
            R.id.switch_double_touch_right -> {
                isLeft = false
                skipSelectGroup()
            }
            R.id.switch_double_touch_i_know -> {
                switch_double_touch_mb.visibility = View.GONE
                switch_double_touch_set.visibility = View.VISIBLE
                SharedPreferencesHelper.putBoolean(this, Constants.IS_FIRST_CONFIG_DOUBLE_SWITCH, false)
            }
        }
    }

    private fun setGroupForSwitch() {
        val leftH: Byte
        val leftL: Byte
        val rightH: Byte
        val rightL: Byte
        if (leftGroup != null) {
            leftH = leftGroup!!.meshAddr.shr(8).toByte()
            leftL = leftGroup!!.meshAddr.and(0xff).toByte()
        } else {
            leftH = 0
            leftL = 0
        }

        if (rightGroup != null) {
            rightH = rightGroup!!.meshAddr.shr(8).toByte()
            rightL = rightGroup!!.meshAddr.and(0xff).toByte()
        } else {
            rightH = 0
            rightL = 0
        }
        val bytes = byteArrayOf(leftL, leftH, rightL, rightH, 0, 0, 0, 0)
        TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_DOUBLE_SWITCH, mDeviceInfo?.meshAddress ?: 0, bytes)
    }


    override fun initListener() {
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        switch_double_touch_use_button.setOnClickListener(this)
        switch_double_touch_left.setOnClickListener(this)
        switch_double_touch_right.setOnClickListener(this)
        switch_double_touch_i_know.setOnClickListener(this)
    }

    private fun showConfigSuccessDialog() {
        if (localVersion.contains("BT") || localVersion.contains("BTL") || localVersion.contains("BTS") || localVersion.contains("STS")) {
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

    @SuppressLint("CheckResult")
    private fun makePop() {
        renameConfirm?.setOnClickListener {
            // 获取输入框的内容
            if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                val trim = renameEt?.text.toString().trim { it <= ' ' }
                if (!Constants.IS_ROUTE_MODE) {
                    renameSw(trim)
                } else {
                    routerRenameSw(switchDate!!, trim)
                }
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

    override fun routerRenameSwSuccess(trim: String) {
        renameSw(trim = trim)
    }

    private fun renameSw(trim: String) {
        switchDate?.name = trim
        DBUtils.updateSwicth(switchDate!!)
        toolbarTv.text = switchDate?.name
    }

    @SuppressLint("CheckResult")
    override fun tzRouterConnectOrDisconnectSwSeRecevice(cmdBean: CmdBodyBean) {
        if (cmdBean.ser_id=="retryConnectSw")
            if (cmdBean.finish) {
                hideLoadingDialog()
                if (cmdBean.status == 0) {
                    ToastUtils.showShort(getString(R.string.connect_success))
                    image_bluetooth.setImageResource(R.drawable.icon_cloud)
                } else {
                    image_bluetooth.setImageResource(R.drawable.bluetooth_no)
                    ToastUtils.showShort(getString(R.string.connect_fail))
                }
            }
    }

    override fun tzRouterConfigDoubleSwRecevice(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由配置双组开关通知-------$cmdBean")
        if (cmdBean.ser_id == "configDoubleSw") {
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

    override fun onDestroy() {
        super.onDestroy()
        disposableRouteTimer?.dispose()
        TelinkLightService.Instance()?.idleMode(true)
    }
}