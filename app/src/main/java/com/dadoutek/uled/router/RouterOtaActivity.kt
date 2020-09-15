package com.dadoutek.uled.router

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.routerModel.RouterModel
import kotlinx.android.synthetic.main.activity_router_ota.*
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2020/9/11 11:47
 * 描述路由多设备升级
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class RouterOtaActivity : TelinkBaseActivity() {
    private var isGroup: Boolean = false
    private var deviceType: Int = 0
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
    }

    @SuppressLint("CheckResult")
    private fun initData() {
        val serializableExtra = intent.getSerializableExtra("group")
        if (serializableExtra != null)
            dbGroup = serializableExtra as DbGroup
        isGroup = dbGroup != null
        deviceType = if (isGroup)
            intent.getIntExtra("DeviceType", 0)
        else
            (dbGroup?.deviceType ?: 0).toInt()

        mesAddrsList.clear()
        when (deviceType) {
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

        RouterModel.toDevicesOTA(mesAddrsList, deviceType)?.subscribe({
            when (it.errorCode) {
                0 -> {
                    ToastUtils.showShort(getString(R.string.ota_update_title))
                    router_ota_tv.text = getString(R.string.otaing)
                } //比如扫描时杀掉APP后恢复至扫描页面，OTA时杀掉APP后恢复至OTA等待
                90998 -> {//扫描中不能OTA，请稍后。请尝试获取路由模式下状态以恢复上次扫描
                    ToastUtils.showShort(getString(R.string.sanning_to_scan_activity))
                    io.reactivex.Observable.timer(2000, TimeUnit.MILLISECONDS).subscribe {
                        startActivity(Intent(this@RouterOtaActivity, DeviceScanningNewActivity::class.java))
                        finish()
                    }
                }
                90999 -> {//OTA中，不能再次进行OTA。请尝试获取路由模式下状态以恢复上次OTA
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

    }

}