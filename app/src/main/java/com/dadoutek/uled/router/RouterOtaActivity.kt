package com.dadoutek.uled.router

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.RouterOTAFinishBean
import com.dadoutek.uled.base.RouterOTAingNumBean
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.routerModel.RouterModel
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
    private var getStatusDispose: Disposable? = null
    private var deviceMac: String? = null
    private var currentTimeMillis: Long = 0
    private var deviceId: Int = 0
    private var isOtaing: Boolean = false
    private var deviceType: Int = 0
    private var otaCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_router_ota)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
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
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.otaing))
                            }
                }
                90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                90018 -> ToastUtils.showShort(getString(R.string.device_not_exit))
                90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_not_router))
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    private fun initData() {
        deviceId = intent.getIntExtra("deviceId", 0)
        deviceType = intent.getIntExtra("deviceType", 0)
        deviceMac = intent.getStringExtra("deviceMac")
        /*  val gpOrTypeOrDevice = intent.getIntExtra("GroupOrTypeOrDevice", 0)
          if (gpOrTypeOrDevice == 0) {
              ToastUtils.showShort(getString(R.string.invalid_data))
              finish()
          }

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
          })*/
    }

    @SuppressLint("CheckResult")
    private fun devicesToOTA() {
        startGetStatuss()
        RouterModel.toDevicesOTA(mutableListOf(deviceId), deviceType, currentTimeMillis)?.subscribe({
            when (it.errorCode) {
                0 -> {
                    currentTimeMillis = System.currentTimeMillis()
                    SharedPreferencesUtils.setLastOtaTime(currentTimeMillis)
                    disposableRouteTimer = Observable.interval(1, 1, TimeUnit.SECONDS).subscribe { itTime ->
                        if (itTime <= 120)//最高优先级
                            router_ota_wave_progress_bar?.value = itTime.toFloat()
                        else
                            afterOtaFail()
                    }
                    isOtaing = true
                    ToastUtils.showShort(getString(R.string.ota_update_title))
                    router_ota_tv.text = getString(R.string.otaing)
                    router_ota_start.text = getString(R.string.stop_ota)
                    otaCount = 0
                } //比如扫描时杀掉APP后恢复至扫描页面，OTA时杀掉APP后恢复至OTA等待
                90998 -> {//扫描中不能OTA，请稍后。请尝试获取路由模式下状态以恢复上次扫描
                    isOtaing = false
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

    private fun startGetStatuss() {
        getStatusDispose = Observable.interval(30, 30, TimeUnit.SECONDS).subscribe {
            RouterModel.routerOTAResult(1, 5000, currentTimeMillis)?.subscribe({
                val filter = it.filter { item -> deviceMac.toString() == item.macAddr }
                if (filter.isNotEmpty()) {
                    //失败原因。-1没有失败 0设备未绑定路由  1设备未获取版本号 2设备未绑定路由且未获取版本号  3设备绑定的路由没上线
                    // 4版本号异常 5已是最新版本，无需升级 6路由回复失败 7路由蓝牙连接失败 8路由下载bin文件失败 99路由回复超时
                    //ota结果。-1失败 0成功 1升级中 2已停止 3处理中
                    when (filter[0].status) {
                        0 -> afterOtaSuccess()
                        -1, 2 -> afterOtaFail()
                    }
                }
            }, {
                ToastUtils.showShort(it.message)
            })
        }
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
                isOtaing = false
            getStatusDispose?.dispose()
            when (routerOTAFinishBean?.status) {
                0 -> {
                    afterOtaSuccess()
                }
                else -> {
                    afterOtaFail()
                }
            }
        }
    }

    private fun afterOtaFail() {
        router_ota_start.text = getString(R.string.retry_ota)
        ToastUtils.showShort(getString(R.string.ota_fail))
        router_ota_tv.text = getString(R.string.ota_fail)
    }

    private fun afterOtaSuccess() {
        ToastUtils.showShort(getString(R.string.ota_success))
        router_ota_tv.text = getString(R.string.ota_success)
        router_ota_start.text = getString(R.string.ota_success)
        ToastUtils.showLong(R.string.exit_update)
        Handler().postDelayed(Runnable { finish() }, 2000)
    }
}