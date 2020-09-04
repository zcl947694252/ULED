package com.dadoutek.uled.switches

import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.ble.RxBleManager.initData
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_scanning_switch.*
import kotlinx.android.synthetic.main.empty_box_view.*
import kotlinx.android.synthetic.main.template_lottie_animation.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.startActivity
import java.util.*

/**
 * 描述	      ${搜索连接开关}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${为类添加标识}$
 */
class ScanningSwitchActivity : TelinkBaseActivity() {
    private var count: Int = 0
    private lateinit var mApplication: TelinkLightApplication
    private var mRxPermission: RxPermissions? = null
    private var bestRSSIDevice: DeviceInfo? = null
    private var mConnectDisposal: Disposable? = null
    private var retryConnectCount = 0
    private var isSupportInstallOldDevice = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning_switch)
        this.mApplication = this.application as TelinkLightApplication
        mRxPermission = RxPermissions(this)
        initView()
        initData()
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
        toolbarTv?.text = getString(R.string.switch_title)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            if (isScanning) {
                cancelf.isClickable = true
                confirmf.isClickable = true
                popFinish.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
            } else {
                finish()
            }
        }
        retryConnectCount = 0
        isSupportInstallOldDevice = false
    }

    private fun initData() {

    }

    private fun initListener() {
        cancelf.setOnClickListener { popFinish?.dismiss() }
        confirmf.setOnClickListener {
            popFinish?.dismiss()
            stopConnectTimer()
            closeAnimation()
            doFinish()
        }
        btn_stop_scan.setOnClickListener {
            if (isScanning){
                scanFail()
                doFinish()
            }
            else
                startScan()
        }
        scanning_num.setOnClickListener {
            if (isScanning)
                seeHelpe("#QA8")
        }
    }

    //扫描失败处理方法
    private fun scanFail() {
        scanning_num.text = getString(R.string.see_help)
        showToast(getString(R.string.scan_end))
        stopConnectTimer()
        closeAnimation()
        //  doFinish()
        btn_stop_scan.text = getString(R.string.scan_retry)
        image_no_group.visibility = View.VISIBLE
    }

    private fun startAnimation() {
        isScanning = true
        lottieAnimationView?.playAnimation()
        lottieAnimationView?.visibility = View.VISIBLE
    }


    private fun closeAnimation() {
        isScanning = false
        lottieAnimationView?.cancelAnimation()
        lottieAnimationView?.visibility = View.GONE
    }


    private fun startScan() {
        btn_stop_scan.text = getString(R.string.stop_scan)
        TelinkLightService.Instance()?.idleMode(true)
        scanning_num.text = getString(R.string.scanning)
        startAnimation()
        image_no_group.visibility = View.GONE
        val deviceTypes = mutableListOf(DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2,
                DeviceType.SCENE_SWITCH, DeviceType.DOUBLE_SWITCH, DeviceType.SMART_CURTAIN_SWITCH, DeviceType.EIGHT_SWITCH)
        mConnectDisposal = connect(meshName = Constant.DEFAULT_MESH_FACTORY_NAME, meshPwd = Constant.DEFAULT_MESH_FACTORY_PASSWORD,
                retryTimes = 3, deviceTypes = deviceTypes, fastestMode = true)
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({
                    bestRSSIDevice = it
                    onLogin()
                }, {
                    scanFail()
                })
    }

    override fun onResume() {
        super.onResume()
        stopTimerUpdate()
        disableConnectionStatusListener()//停止base内部的设备变化监听 不让其自动创建对象否则会重复
    }

    override fun onPause() {
        super.onPause()
        mConnectDisposal?.dispose()
        stopConnectTimer()
    }

    private fun onLogin() {
        count+1
        hideLoadingDialog()
        if (bestRSSIDevice != null) {

            val meshAddress = bestRSSIDevice!!.meshAddress
            val mac = bestRSSIDevice!!.sixByteMacAddress.split(":")
            if (mac != null && mac.size >= 6) {
                val mac1 = Integer.getInteger(mac[3], 16)
                val mac2 = Integer.getInteger(mac[4], 16)
                val mac3 = Integer.getInteger(mac[5], 16)
                val mac4 = Integer.getInteger(mac[6], 16)

                val instance = Calendar.getInstance()
                val second = instance.get(Calendar.SECOND).toByte()
                val minute = instance.get(Calendar.MINUTE).toByte()
                val hour = instance.get(Calendar.HOUR_OF_DAY).toByte()
                val day = instance.get(Calendar.DAY_OF_MONTH).toByte()
                val byteArrayOf = byteArrayOf((meshAddress and 0xFF).toByte(), (meshAddress shr 8 and 0xFF).toByte(), mac1.toByte(),
                        mac2.toByte(), mac3.toByte(), mac4.toByte(),second,minute,hour,day)
                TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.TIME_ZONE, meshAddress, byteArrayOf)
            }

            val disposable = Commander.getDeviceVersion(bestRSSIDevice!!.meshAddress)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { version ->
                                if (version != null && version != "") {
                                    skipSwitch(version)
                                    finish()
                                } else {
                                    val version1 = bestRSSIDevice?.firmwareRevision ?: ""

                                    if (TextUtils.isEmpty(version1))
                                        ToastUtils.showLong(getString(R.string.get_version_fail))
                                    else
                                        skipSwitch(version1)
                                    finish()
                                }
                                closeAnimation()
                            }, {
                        //showToast(getString(R.string.get_version_fail))
                        closeAnimation()
                        val version1 = bestRSSIDevice?.firmwareRevision ?: ""

                        if (TextUtils.isEmpty(version1))
                            ToastUtils.showLong(getString(R.string.get_version_fail))
                        else
                            skipSwitch(version1)
                        finish()
                    })

        }
    }

    private fun skipSwitch(version: String) {
        when (bestRSSIDevice?.productUUID) {
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2 -> {
                startActivity<ConfigNormalSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "false", "version" to version)
            }
            DeviceType.DOUBLE_SWITCH -> {
                startActivity<DoubleTouchSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "false", "version" to version)
            }
            DeviceType.SCENE_SWITCH -> {
                if (version.contains(DeviceType.EIGHT_SWITCH_VERSION))
                    startActivity<ConfigEightSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "false", "version" to version)
                else
                    startActivity<ConfigSceneSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "false", "version" to version)
            }

            DeviceType.EIGHT_SWITCH -> {
                startActivity<ConfigEightSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "false", "version" to version)
            }
            DeviceType.SMART_CURTAIN_SWITCH -> {
                startActivity<ConfigCurtainSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "false", "version" to version)
            }
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
