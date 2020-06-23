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
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtaPrepareUtils
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit
private var last_start_time = 0
private var debounce_time = 1000
abstract class BaseSwitchActivity : TelinkBaseActivity() {
    internal var fiDelete: MenuItem? = null
    internal var fiFactoryReset: MenuItem? = null
    internal var fiOta: MenuItem? = null
    internal var fiChangeGp: MenuItem? = null
    internal var fiRename: MenuItem? = null
    internal var fiVersion: MenuItem? = null
    var mApp: TelinkLightApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(setLayoutId())
        mApp = application as TelinkLightApplication
        initData()
        initView()
        initListener()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        DBUtils.lastUser?.let {
            if (it.id.toString() == it.last_authorizer_user_id) {
                menuInflater.inflate(R.menu.menu_rgb_light_setting, menu)
                fiRename = menu?.findItem(R.id.toolbar_f_rename)
                fiChangeGp = menu?.findItem(R.id.toolbar_v_change_group)
                fiOta = menu?.findItem(R.id.toolbar_f_ota)
                fiFactoryReset = menu?.findItem(R.id.toolbar_v_reset)
                fiOta = menu?.findItem(R.id.toolbar_f_ota)
                fiDelete = menu?.findItem(R.id.toolbar_f_delete)
                fiVersion = menu?.findItem(R.id.toolbar_f_version)
            }
        }
        return super.onCreateOptionsMenu(menu)
    }


    val menuItemClickListener = Toolbar.OnMenuItemClickListener { item ->
        when (item?.itemId) {
            R.id.toolbar_f_rename -> reName()
            R.id.toolbar_v_change_group-> changeGroup()
            R.id.toolbar_v_reset-> deviceFactoryReset()
            R.id.toolbar_f_ota -> goOta()
            R.id.toolbar_f_delete -> deleteDevice()
        }
        true
    }

    private fun deviceFactoryReset() {

    }

    fun changeGroup(){

    }

    abstract fun deleteDevice()

     fun deleteSwitch(macAddress: String) {
        AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_switch_confirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val switchByMacAddr = DBUtils.getSwitchByMacAddr(macAddress)
                    switchByMacAddr?.let {
                        DBUtils.deleteSwitch(it)
                        ToastUtils.showShort(getString(R.string.delete_switch_success))
                        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(it.meshAddr))
                            TelinkLightApplication.getApp().mesh.saveOrUpdate(this)
                        finish()
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    fun deviceOta(mDeviceInfo: DeviceInfo) {
        var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_DEVELOPER_MODE, false)
        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.macAddress ==  mDeviceInfo.macAddress) {
            if (isBoolean)
                transformView(mDeviceInfo)
            else
                OtaPrepareUtils.instance().gotoUpdateView(this@ConfigSceneSwitchActivity, version, otaPrepareListner)
        } else {
            showLoadingDialog(getString(R.string.please_wait))
            TelinkLightService.Instance()?.idleMode(true)
            mConnectDeviceDisposable = Observable.timer(800, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap { connect(mDeviceInfo?.meshAddress, macAddress = mDeviceInfo?.macAddress) }
                    ?.subscribe({
                        hideLoadingDialog()
                        if (isBoolean) transformView(mDeviceInfo)
                        else OtaPrepareUtils.instance().gotoUpdateView(this@ConfigSceneSwitchActivity, mDeviceInfo?.firmwareRevision, otaPrepareListner)
                    }, {
                        hideLoadingDialog()
                        ToastUtils.showLong(R.string.connect_fail2)
                    })
        }
    }

    private fun transformView(mDeviceInfo: DeviceInfo) {
        val intent = Intent(this@BaseSwitchActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.OTA_MAC, mDeviceInfo?.macAddress)
        intent.putExtra(Constant.OTA_MES_Add, mDeviceInfo?.meshAddress)
        intent.putExtra(Constant.OTA_VERSION, mDeviceInfo?.firmwareRevision)
        intent.putExtra(Constant.OTA_TYPE, DeviceType.NORMAL_SWITCH)
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
}