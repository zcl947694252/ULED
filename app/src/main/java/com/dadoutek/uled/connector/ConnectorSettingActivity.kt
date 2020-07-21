package com.dadoutek.uled.connector

import android.Manifest
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
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbConnector
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.ota.OTAConnectorActivity
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
import java.util.*

class ConnectorSettingActivity : TelinkBaseActivity(), TextView.OnEditorActionListener {
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
    private var mRxPermission: RxPermissions? = null
    private var mApp: TelinkLightApplication? = null
    private var manager: DataManager? = null
    private var mConnectDevice: DeviceInfo? = null
    private var mConnectTimer: Disposable? = null
    private var isRenameState = false
    private var group: DbGroup? = null
    private var currentShowPageGroup = true

    private fun renameGroup() {
        if (!TextUtils.isEmpty(group?.name))
            textGp?.setText(group?.name)
        textGp?.setSelection(textGp?.text.toString().length)

        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }

        renameConfirm?.setOnClickListener {    // 获取输入框的内容
            if (StringUtils.compileExChar(textGp?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                var name = textGp?.text.toString().trim { it <= ' ' }
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
                    group?.name = textGp?.text.toString().trim { it <= ' ' }
                    DBUtils.updateGroup(group!!)
                    toolbarTv.text = group?.name
                    renameDialog.dismiss()
                }
            }
        }
    }

    fun remove() {
        AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(getString(R.string.sure_delete_device2))
                .setPositiveButton(android.R.string.ok) { _, _ ->
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

/*    private fun updateGroup() {
        val intent = Intent(this, ConnectorGroupingActivity::class.java)
        if (light == null) {
            ToastUtils.showLong(getString(R.string.please_connect_normal_light))
            TelinkLightService.Instance()?.idleMode(true)
            TelinkLightService.Instance()?.disconnect()
            return
        }
        intent.putExtra(Constant.TYPE_VIEW, Constant.LIGHT_KEY)
        intent.putExtra("light", light)
        intent.putExtra("gpAddress", gpAddress)
        intent.putExtra("uuid", light!!.productUUID)
        Log.d("窗帘升级点击的设备Light", light!!.productUUID.toString() + "," + light!!.meshAddr)
        startActivity(intent)
        this.finish()
    }*/

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
            finish()
        }
    }

    private fun updateGroupResult(light: DbConnector, group: DbGroup) {
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
            relayName.text = group?.name
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
                group?.name = name
                DBUtils.updateGroup(group!!)
            }
        }
    }

    private fun checkPermission() {
        mDisposable.add(
                mRxPermission!!.request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe { granted ->
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

    private fun transformView() {
        val intent = Intent(this@ConnectorSettingActivity, OTAConnectorActivity::class.java)
        intent.putExtra(Constant.UPDATE_LIGHT, currentDbConnector)
        startActivity(intent)
        finish()
    }

    private fun renameDevice() {
        if (!TextUtils.isEmpty(currentDbConnector?.name))
            textGp?.setText(currentDbConnector?.name)
        textGp?.setSelection(textGp?.text.toString().length)

        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }

        renameConfirm?.setOnClickListener {    // 获取输入框的内容
            if (StringUtils.compileExChar(textGp?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                currentDbConnector?.name = textGp?.text.toString().trim { it <= ' ' }
                DBUtils.updateConnector(currentDbConnector!!)
                toolbarTv?.text = currentDbConnector?.name
                renameDialog.dismiss()
            }
        }
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
        if (TelinkLightApplication.getApp().connectDevice != null) {
            when (item?.itemId) {
                R.id.toolbar_f_rename -> if (isConfigGroup) renameGroup() else renameDevice()
                R.id.toolbar_fv_change_group -> updateGroup()
                R.id.toolbar_f_ota -> {
                    if (!TextUtils.isEmpty(localVersion))
                        checkPermission()
                    else
                        Toast.makeText(this, R.string.number_no, Toast.LENGTH_LONG).show()
                }
                R.id.toolbar_f_delete -> remove()
                R.id.toolbar_on_line -> renameGroup()
            }
        } else {
            showLoadingDialog(getString(R.string.connecting))
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
        connector_switch?.setOnCheckedChangeListener { _, isChecked ->
            if (TelinkLightApplication.getApp().connectDevice == null)
                autoConnect()
            else {
                if (!isChecked)
                    openOrClose(true)
                else
                    openOrClose(false)
                currentDbConnector!!.updateIcon()
                DBUtils.updateConnector(currentDbConnector!!)
            }
        }

        var type = intent.getStringExtra(Constant.TYPE_VIEW)
        isConfigGroup = type == Constant.TYPE_GROUP
        if (isConfigGroup) {
            currentShowPageGroup = true
            this.group = this.intent.extras!!.get("group") as DbGroup
            if (group != null)
                if (group!!.meshAddr == 0xffff)
                    toolbarTv!!.text = getString(R.string.allLight)
                else
                    toolbarTv!!.text = group!!.name

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

    }

    private fun openOrClose(b: Boolean) {
        if (currentDbConnector?.productUUID == DeviceType.SMART_CURTAIN) {
            Commander.openOrCloseCurtain(currentDbConnector?.meshAddr ?: 0, isOpen = b, isPause = false)
        } else {
            Commander.openOrCloseLights(currentDbConnector?.meshAddr ?: 0, b)
        }
        if (b)
            currentDbConnector?.connectionStatus = ConnectionStatus.ON.value
        else
            currentDbConnector?.connectionStatus = ConnectionStatus.OFF.value
    }

    fun autoConnect() {
        val subscribe = connect()?.subscribe({ ToastUtils.showShort(getString(R.string.connecting)) },
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
        mRxPermission = RxPermissions(this)
        mConnectDevice = TelinkLightApplication.getApp().connectDevice

    }

    private fun getVersion() {
        if (TelinkApplication.getInstance().connectDevice != null) {
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
                                fiVersion?.title = localVersion
                                DBUtils.saveConnector(currentDbConnector!!, false)
                            },
                            {
                                if (TextUtils.isEmpty(localVersion))
                                    localVersion = getString(R.string.number_no)
                                fiVersion?.title = localVersion
                            }
                    )
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
