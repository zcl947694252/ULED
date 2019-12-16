package com.dadoutek.uled.switches

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_scanning_switch.*
import kotlinx.android.synthetic.main.template_lottie_animation.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.startActivity

private const val CONNECT_TIMEOUT = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val MAX_RETRY_CONNECT_TIME = 1

/**
 * 描述	      ${搜索连接开关}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${为类添加标识}$
 */
class ScanningSwitchActivity : TelinkBaseActivity() {

    private var isSeachedDevice: Boolean = false
    private var connectDisposable: Disposable? = null
    private lateinit var mApplication: TelinkLightApplication
    private var mRxPermission: RxPermissions? = null
    private var bestRSSIDevice: DeviceInfo? = null
    private var mScanTimeoutDisposal: Disposable? = null
    private var mConnectDisposal: Disposable? = null
    private var retryConnectCount = 0
    private var isSupportInstallOldDevice = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning_switch)
        this.mApplication = this.application as TelinkLightApplication
        mRxPermission = RxPermissions(this)
        initView()
        initListener()
        startScan()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                doFinish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private var mIsInited: Boolean = false

    private fun initView() {
        mIsInited = false
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.switch_title)
        retryConnectCount = 0
        isSupportInstallOldDevice = false
    }

    private fun initListener() {
        btn_stop_scan.setOnClickListener {
            if (!isSeachedDevice)
                scanFail()
            else
                ToastUtils.showShort(getString(R.string.connecting_tip))
        }

    }

    //扫描失败处理方法
    private fun scanFail() {
        showToast(getString(R.string.scan_end))
        stopConnectTimer()
        doFinish()
    }

    private fun startAnimation() {
        lottieAnimationView?.playAnimation()
        lottieAnimationView?.visibility = View.VISIBLE
    }


    private fun closeAnimation() {
        lottieAnimationView?.cancelAnimation()
        lottieAnimationView?.visibility = View.GONE
    }


    private fun startScan() {
        TelinkLightService.Instance()?.idleMode(true)
        if (connectDisposable?.isDisposed == false) {
            LogUtils.d("already started")
        } else {
            startAnimation()
            val deviceTypes = mutableListOf(DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2,
                    DeviceType.SCENE_SWITCH, DeviceType.SMART_CURTAIN_SWITCH)
            mConnectDisposal = connect(meshName = Constant.DEFAULT_MESH_FACTORY_NAME, meshPwd = Constant.DEFAULT_MESH_FACTORY_PASSWORD,
                    retryTimes = 3, deviceTypes = deviceTypes, fastestMode = true)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({
                                bestRSSIDevice = it
                                LogUtils.d("onLogin")
                                onLogin()
                            }, {
                                scanFail()
                                LogUtils.d(it)
                            })
        }
    }

    override fun onResume() {
        super.onResume()
        disableConnectionStatusListener()//停止base内部的设备变化监听 不让其自动创建对象否则会重复
    }

    override fun onPause() {
        super.onPause()
        mScanTimeoutDisposal?.dispose()
        connectDisposable?.dispose()
        mConnectDisposal?.dispose()
        stopConnectTimer()
    }

    private fun onLogin() {
        connectDisposable?.dispose()
        mScanTimeoutDisposal?.dispose()
        hideLoadingDialog()
        if (bestRSSIDevice != null) {
            val disposable = Commander.getDeviceVersion(bestRSSIDevice!!.meshAddress)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { version ->
                                if (version != null && version != "") {
                                    if (bestRSSIDevice?.productUUID == DeviceType.NORMAL_SWITCH || bestRSSIDevice?.productUUID == DeviceType.NORMAL_SWITCH2) {
                                        startActivity<ConfigNormalSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "false", "version" to version)
                                    } else if (bestRSSIDevice?.productUUID == DeviceType.SCENE_SWITCH) {
                                        startActivity<ConfigSceneSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "false", "version" to version)
                                    } else if (bestRSSIDevice?.productUUID == DeviceType.SMART_CURTAIN_SWITCH) {
                                        startActivity<ConfigCurtainSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "false", "version" to version)
                                    }
                                    finish()
                                } else {
                                    ToastUtils.showShort(getString(R.string.get_version_fail))
                                    finish()
                                }
                                closeAnimation()
                            }
                            ,
                            {
                                showToast(getString(R.string.get_version_fail))
                                closeAnimation()
                                startActivity<ConfigNormalSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "false", "version" to "")
                                //finish()
                            })

        }
    }


    private fun stopConnectTimer() {
        mConnectDisposal?.dispose()
    }


    private fun doFinish() {
        TelinkLightService.Instance()?.idleMode(true)
        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
        hideLoadingDialog()
    }


    override fun onBackPressed() {
        doFinish()
    }
}
