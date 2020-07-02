package com.dadoutek.uled.switches

/**
 * 创建者     ZCL
 * 创建时间   2020/6/23 16:05
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

private var last_start_time = 0
private var debounce_time = 1000

abstract class BaseSwitchActivity : TelinkBaseActivity() {
    private var deviceType: Int = DeviceType.NORMAL_SWITCH
    var renameDialog: Dialog? = null

    var popReNameView: View? = null
    var renameCancel: TextView? = null
    var renameConfirm: TextView? = null
    var renameEditText: EditText? = null

    private var mConnectDeviceDisposable: Disposable? = null
    private lateinit var otaDeviceInfo: DeviceInfo
    var fiDelete: MenuItem? = null
    var fiFactoryReset: MenuItem? = null
    var fiOta: MenuItem? = null
    var fiChangeGp: MenuItem? = null
    var fiRename: MenuItem? = null
    internal var fiVersion: MenuItem? = null
    var mApp: TelinkLightApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(setLayoutId())
        mApp = application as TelinkLightApplication
        makePopuwindow()
        initToolbar()
        initView()
        initData()
        setVersion()
        initListener()
    }

    private fun initToolbar() {
        var toolbar = setToolBar()

        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        val moreIcon = ContextCompat.getDrawable(toolbar.context, R.drawable.abc_ic_menu_overflow_material)
        if (moreIcon != null) {
            moreIcon.setColorFilter(ContextCompat.getColor(toolbar.context, R.color.black), PorterDuff.Mode.SRC_ATOP)
            toolbar.overflowIcon = moreIcon
        }
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    abstract fun setToolBar():Toolbar

    private fun makePopuwindow() {
        popReNameView = View.inflate(this, R.layout.pop_rename, null)
        renameEditText = popReNameView?.findViewById(R.id.pop_rename_edt)
        renameCancel = popReNameView?.findViewById(R.id.pop_rename_cancel)
        renameConfirm = popReNameView?.findViewById(R.id.pop_rename_confirm)

        renameDialog = Dialog(this)
        renameDialog!!.setContentView(popReNameView)
        renameDialog!!.setCanceledOnTouchOutside(false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        DBUtils.lastUser?.let {
            if (it.id.toString() == it.last_authorizer_user_id) {
                menuInflater.inflate(R.menu.menu_rgb_light_setting, menu)
                fiRename = menu?.findItem(R.id.toolbar_f_rename)
                fiChangeGp = menu?.findItem(R.id.toolbar_fv_change_group)
                fiOta = menu?.findItem(R.id.toolbar_f_ota)
                fiFactoryReset = menu?.findItem(R.id.toolbar_fv_rest)
                fiDelete = menu?.findItem(R.id.toolbar_f_delete)
                fiVersion = menu?.findItem(R.id.toolbar_f_version)
                setVersion()
                val reConfig = setReConfig()
                fiRename?.isVisible = reConfig
                fiOta?.isVisible = reConfig
                fiDelete?.isVisible = reConfig
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    abstract fun setReConfig(): Boolean

    abstract fun setVersion()


    val menuItemClickListener = Toolbar.OnMenuItemClickListener { item ->
        if (TelinkLightApplication.getApp().connectDevice != null) {
            when (item?.itemId) {
                R.id.toolbar_f_rename -> reName()
                R.id.toolbar_fv_change_group -> changeGroup()
                R.id.toolbar_fv_rest -> userReset()
                R.id.toolbar_f_ota -> goOta()
                R.id.toolbar_f_delete -> deleteDevice()
            }
        } else {
            showLoadingDialog(getString(R.string.connecting))
            connect(setConnectMeshAddr(), true)?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({
                        ToastUtils.showShort(getString(R.string.connect_success))
                        hideLoadingDialog()
                    }, {
                        hideLoadingDialog()
                        ToastUtils.showShort(getString(R.string.connect_fail))

                    })
        }
        true
    }

    abstract fun setConnectMeshAddr(): Int

    private fun userReset() {

    }

    fun changeGroup() {

    }

    abstract fun deleteDevice()


    @SuppressLint("SetTextI18n")
    fun showRenameDialog(switchDate: DbSwitch?) {
        hideLoadingDialog()
        StringUtils.initEditTextFilter(renameEditText)

        if (switchDate != null && switchDate?.name != "" && switchDate != null && switchDate?.name != null)
            renameEditText?.setText(switchDate?.name)
        else
            renameEditText?.setText(StringUtils.getSwitchPirDefaultName(switchDate!!.productUUID, this) + "-" + DBUtils.getAllSwitch().size)
        renameEditText?.setSelection(renameEditText?.text.toString().length)

        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }
    }

    fun deleteSwitch(macAddress: String) {
        AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_switch_confirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val sw = DBUtils.getSwitchByMacAddr(macAddress)
                    sw?.let {
                        Commander.resetDevice(sw!!.meshAddr, true)
                                .subscribe(
                                        {
                                            hideLoadingDialog()
                                            ToastUtils.showShort(getString(R.string.delete_switch_success))
                                            DBUtils.deleteSwitch(sw)
                                            TelinkLightService.Instance()?.idleMode(true)

                                            if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(sw.meshAddr))
                                                TelinkLightApplication.getApp().mesh.saveOrUpdate(this)
                                            finish()
                                        }, {
                                    hideLoadingDialog()
                                    ToastUtils.showShort(getString(R.string.delete_device_fail))
                                })
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    fun deviceOta(mDeviceInfo: DeviceInfo, type: Int = DeviceType.NORMAL_SWITCH) {
        deviceType = type
        otaDeviceInfo = mDeviceInfo
        var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_DEVELOPER_MODE, false)
        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.macAddress == mDeviceInfo.macAddress) {
            if (isBoolean)
                transformView(mDeviceInfo, type)
            else
                OtaPrepareUtils.instance().gotoUpdateView(this@BaseSwitchActivity, mDeviceInfo.firmwareRevision, otaPrepareListner)
        } else {
            showLoadingDialog(getString(R.string.please_wait))
            TelinkLightService.Instance()?.idleMode(true)
            mConnectDeviceDisposable = Observable.timer(800, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap { connect(mDeviceInfo?.meshAddress, macAddress = mDeviceInfo?.macAddress) }
                    ?.subscribe({
                        hideLoadingDialog()
                        if (isBoolean) transformView(mDeviceInfo, type)
                        else OtaPrepareUtils.instance().gotoUpdateView(this@BaseSwitchActivity, mDeviceInfo?.firmwareRevision, otaPrepareListner)
                    }, {
                        hideLoadingDialog()
                        ToastUtils.showLong(R.string.connect_fail2)
                    })
        }
    }

    private var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {
        override fun downLoadFileStart() {}
        override fun startGetVersion() {}
        override fun getVersionSuccess(s: String) {}
        override fun getVersionFail() {
            ToastUtils.showLong(R.string.verification_version_fail)
            hideLoadingDialog()
        }

        override fun downLoadFileSuccess() {
            hideLoadingDialog()
            transformView(otaDeviceInfo, deviceType)
        }

        override fun downLoadFileFail(message: String) {
            hideLoadingDialog()
            ToastUtils.showLong(R.string.download_pack_fail)
        }
    }

    private fun transformView(mDeviceInfo: DeviceInfo, type: Int) {
        val intent = Intent(this@BaseSwitchActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.OTA_MAC, mDeviceInfo?.macAddress)
        intent.putExtra(Constant.OTA_MES_Add, mDeviceInfo?.meshAddress)
        intent.putExtra(Constant.OTA_VERSION, mDeviceInfo?.firmwareRevision)
        intent.putExtra(Constant.OTA_TYPE, type)
        val timeMillis = System.currentTimeMillis()
        if (last_start_time == 0 || timeMillis - last_start_time >= debounce_time)
            startActivity(intent)
    }

    abstract fun goOta()

    abstract fun reName()

    abstract fun setLayoutId(): Int
    abstract fun initListener()
    abstract fun initData()
    abstract fun initView()

    override fun onDestroy() {
        super.onDestroy()
        TelinkLightService.Instance()?.idleMode(true)
    }
}