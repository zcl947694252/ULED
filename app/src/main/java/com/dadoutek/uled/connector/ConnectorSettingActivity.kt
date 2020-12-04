package com.dadoutek.uled.connector

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.RouteGetVerBean
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbConnector
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.GroupBodyBean
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.router.RouterOtaActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.router.bean.RouteGroupingOrDelBean
import com.dadoutek.uled.switches.ChooseGroupOrSceneActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.DataManager
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.light.ConnectionStatus
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_connector_setting.*
import kotlinx.android.synthetic.main.connector_device_setting.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivity
import java.util.*

class ConnectorSettingActivity : TelinkBaseActivity(), TextView.OnEditorActionListener {
    private var isOpen: Boolean = false
    private var isConfigGroup: Boolean = false
    var fiDelete: MenuItem? = null
    var fiFactoryReset: MenuItem? = null
    var fiOta: MenuItem? = null
    var fiChangeGp: MenuItem? = null
    var fiRename: MenuItem? = null
    internal var fiVersion: MenuItem? = null

    private val requestCodeNum: Int = 1000
    private var localVersion: String? = null
    private var currentDbConnector: DbConnector? = null
    private val mDisposable = CompositeDisposable()
    private var mApp: TelinkLightApplication? = null
    private var manager: DataManager? = null
    private var mConnectDevice: DeviceInfo? = null
    private var mConnectTimer: Disposable? = null
    private var isRenameState = false
    private var currentGroup: DbGroup? = null
    private var currentShowPageGroup = true

    private fun renameGroup() {
        if (!TextUtils.isEmpty(currentGroup?.name))
            renameEt?.setText(currentGroup?.name)
        renameEt?.setSelection(renameEt?.text.toString().length)

        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }

        renameConfirm?.setOnClickListener {    // 获取输入框的内容
            if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                val trim = renameEt?.text.toString().trim { it <= ' ' }

                var canSave = true
                val groups = DBUtils.allGroups
                for (i in groups.indices) {
                    if (groups[i].name == trim) {
                        ToastUtils.showLong(TelinkLightApplication.getApp().getString(R.string.repeat_name))
                        canSave = false
                        break
                    }
                }
                if (canSave) {
                    currentGroup?.name = renameEt?.text.toString().trim { it <= ' ' }
                    DBUtils.updateGroup(currentGroup!!)
                    toolbarTv.text = currentGroup?.name
                    renameDialog.dismiss()
                }
            }
        }
    }

    fun remove() {
        AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(getString(R.string.sure_delete_device2))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (Constant.IS_ROUTE_MODE)
                        routerDeviceResetFactory(currentDbConnector!!.macAddr, currentDbConnector!!.meshAddr, currentDbConnector!!.productUUID, "deleteConnector")
                    else
                        if (TelinkLightService.Instance()?.adapter?.mLightCtrl?.currentLight != null && TelinkLightService.Instance()?.adapter?.mLightCtrl?.currentLight?.isConnected == true) {
                            showLoadingDialog(getString(R.string.please_wait))
                            val disposable = Commander.resetDevice(currentDbConnector!!.meshAddr)
                                    .subscribe({
                                        // deleteData()
                                    }, {
                                        GlobalScope.launch(Dispatchers.Main) {
                                            /*    showDialogHardDelete?.dismiss()
                                              showDialogHardDelete = android.app.AlertDialog.Builder(this).setMessage(R.string.delete_device_hard_tip)
                                                      .setPositiveButton(android.R.string.ok) { _, _ ->
                                                          showLoadingDialog(getString(R.string.please_wait))
                                                          deleteData()
                                                      }
                                                      .setNegativeButton(R.string.btn_cancel, null)
                                                      .show()*/
                                        }
                                    })
                            deleteData()
                        } else {
                            ToastUtils.showLong(getString(R.string.bluetooth_open_connet))
                            this.finish()
                        }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    override fun tzRouterResetFactory(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由deleteConnector通知-------$cmdBean")
        if (cmdBean.ser_id == "deleteConnector") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            if (cmdBean.status == 0) {
                deleteData()
            } else {
                ToastUtils.showShort(getString(R.string.reset_factory_fail))
            }
        }
    }

    private fun deleteData() {
        hideLoadingDialog()
        DBUtils.deleteConnector(currentDbConnector!!)

        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(currentDbConnector!!.meshAddr)) {
            TelinkLightApplication.getApp().mesh.saveOrUpdate(this)
        }
        if (mConnectDevice != null) {
            Log.d(this.javaClass.simpleName, "mConnectDevice.meshAddress = " + mConnectDevice?.meshAddress)
            Log.d(this.javaClass.simpleName, "light.getMeshAddr() = " + currentDbConnector?.meshAddr)
            if (currentDbConnector?.meshAddr == mConnectDevice?.meshAddress) {
                this.setResult(Activity.RESULT_OK, Intent().putExtra("data", true))
            }
        }

        SyncDataPutOrGetUtils.syncPutDataStart(this, object : SyncCallback {
            override fun start() {}
            override fun complete() {}
            override fun error(msg: String?) {}
        })
        this.finish()
    }

    private fun updateGroup() {//更新分组 断开提示
        val intent = Intent(this@ConnectorSettingActivity, ChooseGroupOrSceneActivity::class.java)
        val bundle = Bundle()
        bundle.putInt(Constant.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
        bundle.putInt(Constant.DEVICE_TYPE, Constant.DEVICE_TYPE_CONNECTOR.toInt())
        intent.putExtras(bundle)
        startActivityForResult(intent, requestCodeNum)
        this?.setResult(Constant.RESULT_OK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodeNum) {
            var group = data?.getSerializableExtra(Constant.EIGHT_SWITCH_TYPE) as DbGroup
            updateGroupResult(currentDbConnector!!, group)
        }
    }

    private fun updateGroupResult(light: DbConnector, group: DbGroup) {
        if (Constant.IS_ROUTE_MODE)
            routerChangeGpDevice(GroupBodyBean(mutableListOf(light.meshAddr), light.productUUID, "connectorGp", group.meshAddr))
        else {
            Commander.addGroup(light.meshAddr, group.meshAddr, {
                group.deviceType = light.productUUID.toLong()
                light.hasGroup = true
                light.belongGroupId = group.id
                light.name = light.name
                DBUtils.updateConnector(light)
                ToastUtils.showShort(getString(R.string.grouping_success_tip))
                if (group != null)
                    DBUtils.updateGroup(group!!)//更新组类型
            }, {
                ToastUtils.showShort(getString(R.string.grouping_fail))
            })
        }
    }

    @SuppressLint("StringFormatMatches")
    override fun tzRouterGroupResult(bean: RouteGroupingOrDelBean?) {
        if (bean?.ser_id == "connectorGp") {
            LogUtils.v("zcl-----------收到路由普通灯分组通知-------$bean")
            disposableRouteTimer?.dispose()
            if (bean?.finish) {
                hideLoadingDialog()
                when (bean?.status) {
                    -1 -> ToastUtils.showShort(getString(R.string.group_failed))
                    0, 1 -> {
                        if (bean?.status == 0) ToastUtils.showShort(getString(R.string.grouping_success_tip)) else ToastUtils.showShort(getString(R.string.group_some_fail))
                        SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, object : SyncCallback {
                            override fun start() {}
                            override fun complete() {}
                            override fun error(msg: String?) {}
                        })
                    }
                }
            }
        }
    }

    /**
     * 删除组，并且把组里的灯的组也都删除。
     */
    private fun deleteGroup(lights: MutableList<DbConnector>, group: DbGroup, retryCount: Int = 0,
                            successCallback: () -> Unit, failedCallback: () -> Unit) {
        Thread {
            if (lights.count() != 0) {
                val maxRetryCount = 3
                if (retryCount <= maxRetryCount) {
                    val light = lights[0]
                    val lightMeshAddr = light.meshAddr
                    Commander.deleteGroup(lightMeshAddr,
                            successCallback = {
                                light.belongGroupId = DBUtils.groupNull!!.id
                                DBUtils.updateConnector(light)
                                lights.remove(light)
                                //修改分组成功后删除场景信息。
                                deleteAllSceneByLightAddr(light.meshAddr)
                                Thread.sleep(100)

                                if (lights.count() == 0) {
                                    //所有灯都删除了分组
                                    DBUtils.deleteGroupOnly(group)
                                    this.runOnUiThread {
                                        successCallback.invoke()
                                    }
                                } else {
                                    //还有灯要删除分组
                                    deleteGroup(lights, group,
                                            successCallback = successCallback,
                                            failedCallback = failedCallback)
                                }
                            },
                            failedCallback = {
                                deleteGroup(lights, group, retryCount = retryCount + 1,
                                        successCallback = successCallback,
                                        failedCallback = failedCallback)
                            })
                } else {    //超过了重试次数
                    this?.runOnUiThread {
                        failedCallback.invoke()
                    }
                }
            } else {
                DBUtils.deleteGroupOnly(group)
                this.runOnUiThread {
                    successCallback.invoke()
                }
            }
        }.start()

    }

    private fun deleteAllSceneByLightAddr(lightMeshAddr: Int) {
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val params: ByteArray = byteArrayOf(0x00, 0xff.toByte())
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, lightMeshAddr, params)
    }

    private fun saveName() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editTitle?.windowToken, 0)
        editTitle?.isFocusableInTouchMode = false
        editTitle?.isFocusable = false
        if (!currentShowPageGroup) {
            checkAndSaveName()
            isRenameState = false
        } else {
            checkAndSaveNameGp()
        }
    }

    private fun checkAndSaveName() {
        val name = editTitle?.text.toString().trim()
        if (compileExChar(name)) {
            Toast.makeText(this, R.string.rename_tip_check, Toast.LENGTH_SHORT).show()
            relayName.visibility = View.VISIBLE
            relayName.text = currentDbConnector?.name
        } else {
            relayName.visibility = View.VISIBLE
            relayName.text = name
            currentDbConnector?.name = name
            DBUtils.updateConnector(currentDbConnector!!)
        }
    }

    private fun checkAndSaveNameGp() {
        val name = editTitle?.text.toString().trim()
        if (compileExChar(name)) {
            Toast.makeText(this, R.string.rename_tip_check, Toast.LENGTH_SHORT).show()

            relayName.visibility = View.VISIBLE
            relayName.text = currentGroup?.name
        } else {
            var canSave = true
            val groups = DBUtils.allGroups
            for (i in groups.indices) {
                if (groups[i].name == name) {
                    ToastUtils.showLong(TelinkLightApplication.getApp().getString(R.string.repeat_name))
                    canSave = false
                    break
                }
            }

            if (canSave) {
                relayName.visibility = View.VISIBLE
                relayName.text = name
                currentGroup?.name = name
                DBUtils.updateGroup(currentGroup!!)
            }
        }
    }

    private fun checkPermission() {
        mDisposable.add(
                RxPermissions(this)!!.request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe { granted ->
                    if (granted!!) {
                        var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_DEVELOPER_MODE, false)
                        if (isBoolean)
                            transformView()
                        else
                            OtaPrepareUtils.instance().gotoUpdateView(this@ConnectorSettingActivity, localVersion, otaPrepareListner)
                    } else {
                        ToastUtils.showLong(R.string.update_permission_tip)
                    }
                })
    }

    private var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {

        override fun downLoadFileStart() {
            showLoadingDialog(getString(R.string.get_update_file))
        }

        override fun startGetVersion() {
            showLoadingDialog(getString(R.string.verification_version))
        }

        override fun getVersionSuccess(s: String) {
            hideLoadingDialog()
        }

        override fun getVersionFail() {
            ToastUtils.showLong(R.string.verification_version_fail)
            hideLoadingDialog()
        }


        override fun downLoadFileSuccess() {
            hideLoadingDialog()
            transformView()
        }

        override fun downLoadFileFail(message: String) {
            hideLoadingDialog()
            ToastUtils.showLong(R.string.download_pack_fail)
        }
    }

    @SuppressLint("CheckResult")
    private fun transformView() {
        /*val intent = Intent(this@ConnectorSettingActivity, OTAConnectorActivity::class.java)
        intent.putExtra(Constant.UPDATE_LIGHT, currentDbConnector)
        startActivity(intent)*/
        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == currentDbConnector?.meshAddr) {
            skipOta()
        } else {
            showLoadingDialog(getString(R.string.please_wait))
            TelinkLightService.Instance()?.idleMode(true)
            connect(macAddress = currentDbConnector?.macAddr, meshAddress = currentDbConnector?.meshAddr?:0, connectTimeOutTime = 10, retryTimes = 2)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({
                        hideLoadingDialog()
                        skipOta()
                        disposableTimer?.dispose()
                    }, {
                        disposableTimer?.dispose()
                        hideLoadingDialog()
                        runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                    })
        }

    }

    private fun skipOta() {
        val intent = Intent(this@ConnectorSettingActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.OTA_MAC, currentDbConnector?.macAddr)
        intent.putExtra(Constant.OTA_MES_Add, currentDbConnector?.meshAddr)
        intent.putExtra(Constant.OTA_VERSION, currentDbConnector?.version)
        intent.putExtra(Constant.OTA_TYPE, DeviceType.SMART_RELAY)
        startActivity(intent)
        finish()
    }

    private fun renameDevice() {
        if (!TextUtils.isEmpty(currentDbConnector?.name))
            renameEt?.setText(currentDbConnector?.name)
        renameEt?.setSelection(renameEt?.text.toString().length)

        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }

        renameConfirm?.setOnClickListener {    // 获取输入框的内容
            if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                val trim = renameEt?.text.toString().trim { it <= ' ' }
                if (Constant.IS_ROUTE_MODE)
                    RouterModel.routeUpdateRelayName(currentDbConnector!!.id, trim)?.subscribe({
                        afterRenameSucess(trim)
                    }, {
                        ToastUtils.showShort(getString(R.string.rename_faile))
                    })
                else {
                    afterRenameSucess(trim)
                }
            }
        }
    }

    private fun afterRenameSucess(trim: String) {
        currentDbConnector?.name = trim
        DBUtils.updateConnector(currentDbConnector!!)
        toolbarTv?.text = currentDbConnector?.name
        renameDialog.dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connector_setting)
        initView()
        initType()
    }

    private fun initView() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val moreIcon = ContextCompat.getDrawable(toolbar.context, R.drawable.abc_ic_menu_overflow_material)
        if (moreIcon != null) {
            moreIcon.setColorFilter(ContextCompat.getColor(toolbar.context, R.color.black), PorterDuff.Mode.SRC_ATOP)
            toolbar.overflowIcon = moreIcon
        }
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        DBUtils.lastUser?.let {
            if (it.id.toString() == it.last_authorizer_user_id) {
                if (isConfigGroup) {
                    /* menuInflater.inflate(R.menu.menu_rgb_group_setting, menu)
                     toolbar.menu?.findItem(R.id.toolbar_batch_gp)?.isVisible = false
                     toolbar.menu?.findItem(R.id.toolbar_delete_device)?.isVisible = false*/
                } else {
                    menuInflater.inflate(R.menu.menu_rgb_light_setting, menu)
                    fiRename = menu?.findItem(R.id.toolbar_f_rename)
                    fiChangeGp = menu?.findItem(R.id.toolbar_fv_change_group)
                    fiFactoryReset = menu?.findItem(R.id.toolbar_fv_rest)
                    fiOta = menu?.findItem(R.id.toolbar_f_ota)
                    fiDelete = menu?.findItem(R.id.toolbar_f_delete)
                    fiVersion = menu?.findItem(R.id.toolbar_f_version)
                    fiChangeGp?.isVisible = true
                    if (isConfigGroup) {//删除分组 重命名分组
                        fiRename?.title = getString(R.string.update_group)
                        fiOta?.isVisible = false
                        fiDelete?.title = getString(R.string.delete_group)
                        if (TextUtils.isEmpty(localVersion))
                            localVersion = getString(R.string.number_no)
                        fiVersion?.title = localVersion
                    }
                }
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    val menuItemClickListener = Toolbar.OnMenuItemClickListener { item ->
        if (TelinkLightApplication.getApp().connectDevice != null || Constant.IS_ROUTE_MODE) {
            when (item?.itemId) {
                R.id.toolbar_f_rename -> if (isConfigGroup) renameGroup() else renameDevice()
                R.id.toolbar_fv_change_group -> updateGroup()
                R.id.toolbar_f_ota -> {
                    when {
                        !TextUtils.isEmpty(localVersion) -> when {
                            !isSuportOta(currentDbConnector?.version) -> ToastUtils.showShort(getString(R.string.dissupport_ota))
                            isMostNew(currentDbConnector?.version) -> ToastUtils.showShort(getString(R.string.the_last_version))
                            else -> {
                                when {
                                    Constant.IS_ROUTE_MODE -> {
                                        startActivity<RouterOtaActivity>("deviceMeshAddress" to currentDbConnector!!.meshAddr,
                                                "deviceType" to currentDbConnector!!.productUUID, "deviceMac" to currentDbConnector!!.macAddr,
                                                "version" to currentDbConnector!!.version)
                                        finish()
                                    }
                                    else -> checkPermission()
                                }
                            }
                        }
                        else -> Toast.makeText(this, R.string.number_no, Toast.LENGTH_LONG).show()
                    }
                }
                R.id.toolbar_f_delete -> remove()
                R.id.toolbar_on_line -> renameGroup()
            }
        } else {
            showLoadingDialog(getString(R.string.connecting_tip))
            val subscribe = connect(currentDbConnector!!.meshAddr, true)?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())?.subscribe({
                        ToastUtils.showShort(getString(R.string.connect_success))
                        hideLoadingDialog()
                    }, {
                        ToastUtils.showShort(getString(R.string.connect_fail))
                        hideLoadingDialog()
                    })
        }
        true
    }


    override fun onStop() {
        super.onStop()
        mConnectTimer?.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        mConnectTimer?.dispose()
        mDisposable.dispose()
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        doWhichOperation(actionId)
        return true
    }


    private fun doWhichOperation(actionId: Int) {
        when (actionId) {
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_NONE -> {
                saveName()
                if (currentShowPageGroup) {
                    tvRename.visibility = View.GONE
                }
            }
        }
    }

    private fun initType() {
        var type = intent.getStringExtra(Constant.TYPE_VIEW)
        isConfigGroup = type == Constant.TYPE_GROUP
        if (isConfigGroup) {
            currentShowPageGroup = true
            this.currentGroup = this.intent.extras!!.get("group") as DbGroup
            if (currentGroup != null)
                if (currentGroup!!.meshAddr == 0xffff)
                    toolbarTv!!.text = getString(R.string.allLight)
                else
                    toolbarTv!!.text = currentGroup!!.name

            img_function1.setImageResource(R.drawable.icon_editor)
            img_function1.visibility = View.VISIBLE
            img_function1.setOnClickListener {
                renameGroup()
            }
        } else {
            img_function1.visibility = View.GONE
            currentShowPageGroup = false
            initViewLight()
            getVersion()
        }
        connector_switch.isChecked = currentDbConnector?.connectionStatus == 1
        connector_switch?.setOnCheckedChangeListener { _, isChecked ->
            if (TelinkLightApplication.getApp().connectDevice == null && !Constant.IS_ROUTE_MODE)
                autoConnect()
            else {
                if (isChecked)
                    openOrClose(true)
                else
                    openOrClose(false)
            }
        }
    }

    private fun openOrClose(b: Boolean) {
        isOpen = b
        var status = if (b) 1 else 0
        if (isConfigGroup) {
            if (Constant.IS_ROUTE_MODE) {
                routeOpenOrCloseBase(currentGroup!!.meshAddr, 97, status, "switchGp")
            } else {
                Commander.openOrCloseLights(currentGroup?.meshAddr ?: 0, b)
                afterSwitchGp(b)
            }
        } else {
            if (Constant.IS_ROUTE_MODE) {
                routeOpenOrCloseBase(currentDbConnector!!.meshAddr, currentDbConnector!!.productUUID, status, "switchConnector")
            } else {
                Commander.openOrCloseLights(currentDbConnector?.meshAddr ?: 0, isOpen = b)
                afterSwConnector(b)
            }
        }
    }

    override fun tzRouterOpenOrClose(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由switchGp通知-------$cmdBean")
        if (cmdBean.ser_id == "switchGp" || cmdBean.ser_id == "switchConnector") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            if (cmdBean.status == 0) {
                when (cmdBean.ser_id) {
                    "switchGp" -> {
                        afterSwitchGp(isOpen)
                    }
                    "switchConnector" -> {
                        afterSwConnector(isOpen)
                    }
                }
            } else {
                val string = if (isOpen) getString(R.string.open_light_faile) else getString(R.string.close_faile)
                ToastUtils.showShort(string)
            }
        }
    }


    private fun afterSwConnector(b: Boolean) {
        if (b)
            currentDbConnector?.connectionStatus = ConnectionStatus.ON.value
        else
            currentDbConnector?.connectionStatus = ConnectionStatus.OFF.value
        currentDbConnector!!.updateIcon()
        DBUtils.updateConnector(currentDbConnector!!)
    }

    private fun afterSwitchGp(b: Boolean) {
        if (b)
            currentGroup?.connectionStatus = ConnectionStatus.ON.value
        else
            currentGroup?.connectionStatus = ConnectionStatus.OFF.value
        DBUtils.saveGroup(currentGroup!!, false)
    }

    fun autoConnect() {
        val subscribe = connect()?.subscribe({ ToastUtils.showShort(getString(R.string.connecting_tip)) },
                { ToastUtils.showShort(getString(R.string.device_disconnected)) }
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(editTitle?.windowToken, 0)
                editTitle?.isFocusableInTouchMode = false
                editTitle?.isFocusable = false
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initViewLight() {
        this.mApp = this.application as TelinkLightApplication?
        manager = DataManager(mApp, mApp!!.mesh.name, mApp!!.mesh.password)
        val get = this.intent.extras!!.get(Constant.LIGHT_ARESS_KEY)
        if (get != null)
            currentDbConnector = get as DbConnector
        if (currentDbConnector == null) {
            ToastUtils.showShort(getString(R.string.invalid_data))
            finish()
        }
        toolbarTv.text = currentDbConnector?.name ?: ""
        mConnectDevice = TelinkLightApplication.getApp().connectDevice

    }

    private fun getVersion() {
        when {
            Constant.IS_ROUTE_MODE -> routerGetVersion(mutableListOf(currentDbConnector!!.meshAddr), DeviceType.SMART_RELAY, "getRalyVersion")
            TelinkApplication.getInstance().connectDevice != null -> {
                val disposable = Commander.getDeviceVersion(currentDbConnector!!.meshAddr)
                        .subscribe(
                                { s: String ->
                                    localVersion = s
                                    if (TextUtils.isEmpty(s))
                                        localVersion = getString(R.string.number_no)
                                    currentDbConnector!!.version = localVersion
                                    fiVersion?.title = localVersion
                                    if (TextUtils.isEmpty(localVersion))
                                        localVersion = getString(R.string.number_no)
                                    runOnUiThread { fiVersion?.title = localVersion }
                                    DBUtils.saveConnector(currentDbConnector!!, false)
                                },
                                {
                                    if (TextUtils.isEmpty(localVersion))
                                        localVersion = getString(R.string.number_no)
                                    runOnUiThread { fiVersion?.title = localVersion }
                                }
                        )
            }
        }
    }

    override fun tzRouterUpdateVersionRecevice(routerVersion: RouteGetVerBean?) {
        LogUtils.v("zcl-----------收到路由getRalyVersion通知-------$routerVersion")
        if (routerVersion?.ser_id == "getRalyVersion") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            if (routerVersion?.status == 0) {
                currentDbConnector?.version = routerVersion?.succeedNow[0].version
                DBUtils.saveConnector(currentDbConnector!!, false)
                localVersion = routerVersion?.succeedNow[0].version
                runOnUiThread { fiVersion?.title = localVersion }
            } else {
                ToastUtils.showShort(getString(R.string.get_version_fail))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isConfigGroup) {
            val relay = DBUtils.getConnectorByID(currentDbConnector!!.id)
            localVersion = relay?.version
            runOnUiThread { fiVersion?.title = localVersion }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(Constant.RESULT_OK)
            finish()
            false
        } else {
            super.onKeyDown(keyCode, event)
        }
    }
}
