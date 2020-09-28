package com.dadoutek.uled.router

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.RouterOTAFinishBean
import com.dadoutek.uled.base.RouterOTAingNumBean
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.OTAResultBodyBean
import com.dadoutek.uled.util.SharedPreferencesUtils
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_router_ota.*
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2020/9/11 11:47
 * 描述路由多设备升级   http获取ota升级结果是不是最后一次升级的最新结果如果不是怎么获取最新的
 * 如果收到的是进行ota的通知怎么获取当前第几个了来进行显示 需要升级的是多少个
 *   停止升级是退出还是停留当前界面
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class RouterOtaActivity : TelinkBaseActivity() {
    private var disposableTimer: Disposable? = null
    private var isOtaing: Boolean = false
    private var isGroup: Boolean = false
    private var deviceType: Int = 0
    private var otaCount: Int = 0
    private var dbGroup: DbGroup? = null
    private val mesAddrsList = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_router_ota)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        router_ota_wave_progress_bar?.setOnClickListener {
            router_ota_wave_progress_bar?.value = 40F
        }
        router_ota_start.setOnClickListener {
            if (isOtaing)
                devicesStopOTA()
            else
                devicesToOTA()
        }
    }

    @SuppressLint("CheckResult")
    private fun devicesStopOTA() {
        RouterModel.routerStopOTA(System.currentTimeMillis())?.subscribe({
            disposableTimer?.dispose()
            disposableTimer = Observable.timer(1500, TimeUnit.MILLISECONDS)
                    .subscribe {
                        showLoadingDialog(getString(R.string.please_wait))
                    }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    private fun initData() {
        val gpOrTypeOrDevice = intent.getIntExtra("GroupOrTypeOrDevice", 0)
        if (gpOrTypeOrDevice == 0) {
            ToastUtils.showShort(getString(R.string.invalid_data))
            finish()
        }
        /*   deviceType = if (isGroup)
               intent.getIntExtra("DeviceType", 0)
           else
               (dbGroup?.deviceType ?: 0).toInt()*/

        mesAddrsList.clear()
        when (gpOrTypeOrDevice) {
            1 -> {
                val id = intent.getIntExtra("groupOrDeviceId", 0)
                when {
                    id != 0 -> DBUtils.getLightByGroupID(id.toLong()).forEach { mesAddrsList.add(it.meshAddr) }
                    else -> {
                        ToastUtils.showShort(getString(R.string.invalid_data))
                        finish()
                    }
                }
            }
            2 -> when (intent.getIntExtra("DeviceType", 0)) {
                DeviceType.LIGHT_NORMAL -> {
                    DBUtils.getAllNormalLight().forEach { mesAddrsList.add(it.meshAddr) }
                }
                DeviceType.LIGHT_RGB -> {
                    DBUtils.getAllRGBLight().forEach { mesAddrsList.add(it.meshAddr) }
                }
                DeviceType.NORMAL_SWITCH -> {
                    DBUtils.getAllSwitch().forEach { mesAddrsList.add(it.meshAddr) }
                }
                DeviceType.SENSOR -> {
                    DBUtils.getAllSensor().forEach { mesAddrsList.add(it.meshAddr) }
                }
                DeviceType.SMART_CURTAIN -> {
                    DBUtils.getAllCurtains().forEach { mesAddrsList.add(it.meshAddr) }
                }
                DeviceType.SMART_RELAY -> {
                    DBUtils.getAllRelay().forEach { mesAddrsList.add(it.meshAddr) }
                }
                DeviceType.GATE_WAY -> {
                    DBUtils.getAllGateWay().forEach { mesAddrsList.add(it.meshAddr) }
                }
            }
            3 -> {
                val id = intent.getIntExtra("groupOrDeviceId", 0)
                when {
                    id != 0 -> mesAddrsList.add(id)
                    else -> {
                        ToastUtils.showShort(getString(R.string.invalid_data))
                        finish()
                    }
                }
            }
        }
        RouterModel.routerOTAResult(1, 50, 0)?.subscribe({
            if (it != null && it.size > 0)
                SharedPreferencesUtils.setLastOtaTime(it[0].start)
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    private fun devicesToOTA() {
        RouterModel.toDevicesOTA(mesAddrsList, deviceType)?.subscribe({
            when (it.errorCode) {
                0 -> {
                    isOtaing = true
                    ToastUtils.showShort(getString(R.string.ota_update_title))
                    router_ota_tv.text = getString(R.string.otaing)
                    otaCount = 0
                } //比如扫描时杀掉APP后恢复至扫描页面，OTA时杀掉APP后恢复至OTA等待
                90998 -> {//扫描中不能OTA，请稍后。请尝试获取路由模式下状态以恢复上次扫描
                    ToastUtils.showShort(getString(R.string.sanning_to_scan_activity))
                    Observable.timer(2000, TimeUnit.MILLISECONDS).subscribe {
                        startActivity(Intent(this@RouterOtaActivity, DeviceScanningNewActivity::class.java))
                        finish()
                    }
                }
                90999 -> {//OTA中，不能再次进行OTA。请尝试获取路由模式下状态以恢复上次OTA
                    isOtaing = true
                    ToastUtils.showShort(getString(R.string.ota_update_title))
                    router_ota_tv.visibility = View.VISIBLE
                    router_ota_tv.text = getString(R.string.otaing)
                }
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    private fun initView() {
        router_ota_wave_progress_bar.value = 0f
        router_ota_tv.text = getString(R.string.routing_update)
    }

    override fun tzRouterOTAingNumRecevice(routerOTAingNumBean: RouterOTAingNumBean?) {
        //升级中通知
        otaCount++
        router_ota_num.text = otaCount.toString()
    }

    override fun tzRouterOTAFinishRecevice(routerOTAFinishBean: RouterOTAFinishBean?) {
        if (routerOTAFinishBean?.finish == true) {
            hideLoadingDialog()
            if (routerOTAFinishBean?.status == 0) {
                isOtaing = true
                ToastUtils.showShort(getString(R.string.ota_finish))
                disposableTimer?.dispose()
            }
        }
    }
}