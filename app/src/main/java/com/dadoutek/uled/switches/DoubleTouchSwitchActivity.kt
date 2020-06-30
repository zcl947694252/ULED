package com.dadoutek.uled.switches

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.util.GsonUtil
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.MeshAddressGenerator
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.light.DeviceInfo
import kotlinx.android.synthetic.main.activity_switch_double_touch.*
import kotlinx.android.synthetic.main.bottom_version_ly.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * 创建者     ZCL
 * 创建时间   2020/4/26 14:54
 * 描述  41-6c
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */

class DoubleTouchSwitchActivity : BaseSwitchActivity(), View.OnClickListener {
    private var isReConfig: Boolean = false
    private var switchDate: DbSwitch? = null
    private lateinit var localVersion: String
    private var isRetryConfig: String? = null
    private lateinit var mDeviceInfo: DeviceInfo
    private var leftGroup: DbGroup? = null
    private var rightGroup: DbGroup? = null
    private val requestCodeNum: Int = 1000
    private var isLeft: Boolean = false
    override fun setVersion() {
        if (TextUtils.isEmpty(localVersion))
            localVersion = getString(R.string.get_version_fail)
        fiVersion?.title =localVersion
    }

    override fun setConnectMeshAddr(): Int {
        return  mDeviceInfo?.meshAddress?:0
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
        return R.layout.activity_switch_double_touch
    }

    override fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        isRetryConfig = intent.getStringExtra("group")
        localVersion = intent.getStringExtra("version")
        eight_switch_versionLayout.setBackgroundColor(getColor(R.color.transparent))
//        bottom_version_number.text = localVersion

        isReConfig = isRetryConfig != null && isRetryConfig == "true"
        fiRename?.isVisible = isReConfig

        if (isReConfig) {
            switchDate = this.intent.extras!!.get("switch") as DbSwitch
            switchDate?.let {
                val stringToList = GsonUtil.stringToList<Double>(it.controlGroupAddrs)
                if (stringToList == null || stringToList.size < 2) {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeNum && resultCode == Activity.RESULT_OK) {
            var group = data?.getSerializableExtra(Constant.EIGHT_SWITCH_TYPE) as DbGroup
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
        intent.putExtra(Constant.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
        startActivityForResult(intent, requestCodeNum)
    }

    private fun upSwitchData() {
        //val newMeshAddr = Constant.SWITCH_PIR_ADDRESS
        val newMeshAddr = MeshAddressGenerator().meshAddress.get()
        Commander.updateMeshName(newMeshAddr = newMeshAddr,
                successCallback = {
                    hideLoadingDialog()
                    mDeviceInfo.meshAddress = newMeshAddr
                    updateSwitch()
                    disconnect()
                    if (switchDate == null)
                        switchDate = DBUtils.getSwitchByMeshAddr(mDeviceInfo.meshAddress)
                    GlobalScope.launch(Dispatchers.Main) {
                        if (!isReConfig)
                        showRenameDialog(switchDate)
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
                dbSwitch!!.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID, this) + dbSwitch.meshAddr
                dbSwitch.macAddr = mDeviceInfo.macAddress
                dbSwitch.meshAddr = /*Constant.SWITCH_PIR_ADDRESS*/mDeviceInfo.meshAddress
                dbSwitch.productUUID = mDeviceInfo.productUUID
                dbSwitch.index = dbSwitch.id.toInt()
                dbSwitch.controlGroupAddrs = GsonUtils.toJson(mutableListOf(leftGroup?.meshAddr ?: 1000000, rightGroup?.meshAddr
                        ?: 1000000))

                DBUtils.saveSwitch(dbSwitch, false)
                DBUtils.recordingChange(dbSwitch.id,
                        DaoSessionInstance.getInstance().dbSwitchDao.tablename,
                        Constant.DB_ADD)
                switchDate = dbSwitch
            }
        } else {
            switchDate!!.controlGroupAddrs = GsonUtils.toJson(mutableListOf(leftGroup?.meshAddr, rightGroup?.meshAddr))
            switchDate!!.meshAddr = MeshAddressGenerator().meshAddress.get()
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
                    showLoadingDialog(getString(R.string.please_wait))
                    GlobalScope.launch {
                        setGroupForSwitch()
                        delay(800)
                        upSwitchData()
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
                SharedPreferencesHelper.putBoolean(this, Constant.IS_FIRST_CONFIG_DOUBLE_SWITCH, false)
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


    override  fun initListener() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
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

    private fun makePop() {
        renameConfirm?.setOnClickListener {
            // 获取输入框的内容
            if (StringUtils.compileExChar(renameEditText?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                switchDate?.name = renameEditText?.text.toString().trim { it <= ' ' }
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
        renameDialog?.setOnDismissListener {
            switchDate?.name = renameEditText?.text.toString().trim { it <= ' ' }
            if (switchDate != null)
                DBUtils.updateSwicth(switchDate!!)
            showConfigSuccessDialog()
        }
    }

    override  fun initView() {
        switch_double_touch_mb.visibility = View.VISIBLE
        switch_double_touch_set.visibility = View.GONE
        switch_double_touch_i_know.paint.color = getColor(R.color.white)
        switch_double_touch_i_know.paint.flags = Paint.UNDERLINE_TEXT_FLAG //下划线
        toolbarTv.text = getString(R.string.double_switch)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.inflateMenu(R.menu.menu_rgb_light_setting)
        makePop()
    }

    override fun onDestroy() {
        super.onDestroy()
        TelinkLightService.Instance()?.idleMode(true)
    }
}