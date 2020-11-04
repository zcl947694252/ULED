package com.dadoutek.uled.router

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.RouterOTAFinishBean
import com.dadoutek.uled.base.RouterOTAingNumBean
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.RouterOTAResultBean
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_batch_group_four.*
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
    private var isRouter: Boolean = false
    private var clickFinish: Boolean = false
    private var getStatusDispose: Disposable? = null
    private var deviceMac: String? = null
    private var currentTimeMillis: Long = 0
    private var deviceMeshAddress: Int = 0
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
            LogUtils.v("zcl-----------路由点击-------$isOtaing----------$clickFinish")
            if (isOtaing) {
                clickFinish = false
                devicesStopOTA()
            } else
                devicesToOTA()
        }
    }

    @SuppressLint("CheckResult")
    private fun devicesStopOTA() {
        RouterModel.routerStopOTA(currentTimeMillis, "router_ota")?.subscribe({
            LogUtils.v("zcl-----------收到路由停止升级请求---time$currentTimeMillis----$it")
            when (it.errorCode) {
                0 -> {
                    SharedPreferencesUtils.setLastOtaTime(currentTimeMillis)
                    SharedPreferencesUtils.setLastOtaType(1)
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.ota_stop_fail))
                            }
                }
                90023 -> ToastUtils.showShort(getString(R.string.startTime_not_exit))
                90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                90018 -> ToastUtils.showShort(getString(R.string.device_not_exit))
                90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    private fun initData() {
        deviceMeshAddress = intent.getIntExtra("deviceMeshAddress", 0)
        deviceType = intent.getIntExtra("deviceType", 0)
        deviceMac = intent.getStringExtra("deviceMac")
        val intExtra = intent.getIntExtra("isOTAing", 3)
        isOtaing = intExtra == 1
        isRouter = deviceMeshAddress == 100000
        if (isOtaing)
            if (isRouter)
                routerOtaByself(SharedPreferencesUtils.getLastOtaTime())
            else
                otaDevice(SharedPreferencesUtils.getLastOtaTime())

    }

    @SuppressLint("CheckResult")
    private fun devicesToOTA() {
        startGetStatuss()
        val currentTimeMillis1 = System.currentTimeMillis()
        if (isRouter)
            routerOtaByself(currentTimeMillis1)
        else
            otaDevice(currentTimeMillis1)
    }

    @SuppressLint("CheckResult")
    private fun routerOtaByself(currentTimeMillis1: Long) {
        RouterModel.routeOtaRouter(deviceMac!!, currentTimeMillis1)?.subscribe({
            LogUtils.v("zcl--------收到升级路由本身成功----------$it")
            isOtaing = false
            when (it.errorCode) {
                0 -> {
                    isOtaing = true
                    setTimeAndOpenUI(currentTimeMillis1)
                    SharedPreferencesUtils.setLastOtaTime(currentTimeMillis1)
                    SharedPreferencesUtils.setLastOtaType(3)
                }
                90999 -> goScanning()
                90998 -> {//OTA中，不能再次进行OTA。请尝试获取路由模式下状态以恢复上次OTA
                    isOtaing = true
                    ToastUtils.showShort(getString(R.string.ota_update_title))
                    setTimeAndOpenUI(SharedPreferencesUtils.getLastOtaTime())
                }
                90004 -> ToastUtils.showShort(getString(R.string.router_not_exit))
                90001 -> ToastUtils.showShort(getString(R.string.router_offline))
                90029 -> ToastUtils.showShort(getString(R.string.router_version_error))
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    private fun otaDevice(currentTimeMillis1: Long) {
        RouterModel.toDevicesOTA(mutableListOf(deviceMeshAddress), deviceType, currentTimeMillis1)?.subscribe({
            LogUtils.v("zcl-----------收到路由升级请求---deviceMeshAddress$deviceMeshAddress---time$currentTimeMillis1-------deviceTye${deviceType}----$it")
            when (it.errorCode) {
                0 -> {
                    setTimeAndOpenUI(currentTimeMillis1)
                    isOtaing = true
                    ToastUtils.showShort(getString(R.string.ota_update_title))
                    router_ota_start.text = getString(R.string.stop_ota)
                    router_ota_warm.text = getString(R.string.ota_update_title)
                    otaCount = 0
                    router_ota_wave_progress_bar?.value = 0f
                } //比如扫描时杀掉APP后恢复至扫描页面，OTA时杀掉APP后恢复至OTA等待
                90999 -> {//扫描中不能OTA，请稍后。请尝试获取路由模式下状态以恢复上次扫描
                    isOtaing = false
                    goScanning()
                }
                90998 -> {//OTA中，不能再次进行OTA。请尝试获取路由模式下状态以恢复上次OTA
                    isOtaing = true
                    ToastUtils.showShort(getString(R.string.ota_update_title))
                    setTimeAndOpenUI(SharedPreferencesUtils.getLastOtaTime())
                }
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    private fun goScanning() {
        isOtaing = false
        ToastUtils.showShort(getString(R.string.sanning_to_scan_activity))
        Observable.timer(2000, TimeUnit.MILLISECONDS).subscribe {
            startActivity(Intent(this@RouterOtaActivity, DeviceScanningNewActivity::class.java))
            finish()
        }
    }

    private fun setTimeAndOpenUI(currentTimeMillis1: Long) {
        currentTimeMillis = currentTimeMillis1
        SharedPreferencesUtils.setLastOtaTime(currentTimeMillis)
        disposableRouteTimer = Observable.interval(1, 1, TimeUnit.SECONDS).subscribe { itTime ->
            runOnUiThread {
                if (itTime <= 180)//最高优先级
                    router_ota_wave_progress_bar?.value = itTime.toFloat()
                else
                    afterOtaFail()
            }
        }
        ToastUtils.showShort(getString(R.string.ota_update_title))
        router_ota_warm.text = getString(R.string.ota_update_title)
        if (!isRouter)
            router_ota_start.text = getString(R.string.stop_ota)
        else {
            router_ota_start.text = getString(R.string.otaing)
            router_ota_start.isClickable = false
        }
        otaCount = 0
    }

    private fun startGetStatuss() {
        LogUtils.v("zcl-----------开始获取路由状态")
        getStatusDispose = Observable.interval(30, 30, TimeUnit.SECONDS).subscribe {
            RouterModel.routerOTAResult(1, 5000, currentTimeMillis)?.subscribe({
                val filter = it.filter { item -> deviceMac.toString() == item.macAddr }
                if (filter.isNotEmpty()) {
                LogUtils.v("zcl-----------获取路由状态-------$it--${filter.isNotEmpty()}--------${deviceMac.toString()}--")
                    //ota结果。-1失败 0成功 1升级中 2已停止 3处理中
                    val routerOTAResultBean = filter[0]
                    when (routerOTAResultBean.status) {
                        0 -> afterOtaSuccess()
                        -1, 2 -> afterOtaFailState(routerOTAResultBean)
                    }
                }
            }, {
                ToastUtils.showShort(it.message)
            })
        }
    }

    private fun initView() {
        router_ota_wave_progress_bar.value = 0f
        toolbarTv.text = getString(R.string.ota)
        currentTimeMillis = SharedPreferencesUtils.getLastOtaTime()
        setPop()
        toolbar.setNavigationOnClickListener {
            isFinish()
        }
        toolbar.setNavigationIcon(R.drawable.icon_return)
    }

    private fun setPop() {
        hinitOne.text = getString(R.string.is_exit_ota)
        cancelf.setOnClickListener { popFinish?.dismiss() }
        confirmf.setOnClickListener {
            clickFinish = true
            devicesStopOTA()
            popFinish?.dismiss()
        }
    }

    override fun tzRouterOTAingNumRecevice(routerOTAingNumBean: RouterOTAingNumBean?) {
        //升级中通知
        LogUtils.v("zcl-----------收到路由ota通知-------$routerOTAingNumBean")
        disposableRouteTimer?.dispose()
        hideLoadingDialog()
        val status = routerOTAingNumBean?.status
        val otaResult = routerOTAingNumBean?.otaResult
        if (status == 0 && deviceMac == otaResult?.macAddr && otaResult?.failedCode == -1)
            afterOtaSuccess()
        else
            if (status == -1 || status == 1)//ota结果。-1失败 0成功 1升级中 2已停止 3处理中
                afterOtaFail()
    }

    private fun afterOtaFail() {
        router_ota_start.text = getString(R.string.retry_ota)
        router_ota_warm.text = getString(R.string.router_ota_faile)
        ToastUtils.showShort(getString(R.string.router_ota_faile))
        isOtaing = false
    }

    override fun tzRouterOTAStopRecevice(routerOTAFinishBean: RouterOTAFinishBean?) {
        LogUtils.v("zcl-----------收到路由ota停止通知-------$routerOTAFinishBean")
        if (routerOTAFinishBean?.finish == true) {
            if (routerOTAFinishBean.ser_id == "router_ota") {
                initUi()
                if (routerOTAFinishBean.status == 0 && clickFinish)
                    finish()
            }
        }
    }

    private fun initUi() {
        isOtaing = false
        disposableRouteTimer?.dispose()
        hideLoadingDialog()
        router_ota_start.text = getString(R.string.start_update)
        router_ota_warm.text = getString(R.string.ota_prepare_title)
        router_ota_wave_progress_bar.value = 0f
        Thread.sleep(100)
        router_ota_wave_progress_bar.value = 0f
    }

    private fun afterOtaFailState(resultBean: RouterOTAResultBean) {
        //失败原因。-1没有失败 0设备未绑定路由  1设备未获取版本号 2设备未绑定路由且未获取版本号  3设备绑定的路由没上线
        // 4版本号异常 5已是最新版本，无需升级 6路由升级失败 7路由蓝牙连接失败 8路由下载bin文件失败 99路由回复超时
        isOtaing = false
        when (resultBean.failedCode) {
            -1 -> afterOtaSuccess()
            0 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
            1 -> ToastUtils.showShort(getString(R.string.no_get_version))
            2 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_version))
            3 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
            4 -> ToastUtils.showShort(getString(R.string.version_error))
            5 -> {
                ToastUtils.showShort(getString(R.string.the_last_version))
                twoSecondFinish()
            }
            6 -> ToastUtils.showShort(getString(R.string.router_ota_faile))
            7 -> ToastUtils.showShort(getString(R.string.router_offline))
            8 -> ToastUtils.showShort(getString(R.string.router_load_bin_faile))
            99 -> ToastUtils.showShort(getString(R.string.router_time_out))
        }
        if (resultBean.failedCode != -1) {
            router_ota_start.text = getString(R.string.retry_ota)
            router_ota_warm.text = getString(R.string.router_ota_faile)
            router_ota_wave_progress_bar.value = 0f
            getStatusDispose?.dispose()
            disposableRouteTimer?.dispose()
        }
    }

    private fun afterOtaSuccess() {
        ToastUtils.showShort(getString(R.string.ota_success))
        router_ota_start.text = getString(R.string.ota_success)
        router_ota_warm.text = getString(R.string.ota_success)
        router_ota_start.isClickable = false
        router_ota_wave_progress_bar.value = 180f
        initUi()
        SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, syncCallbackGet)
        twoSecondFinish()
    }

    @SuppressLint("CheckResult")
    private fun twoSecondFinish() {
        getStatusDispose?.dispose()
        disposableRouteTimer?.dispose()
        ToastUtils.showShort(R.string.exit_update)
        Observable.timer(2500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    finish()
                }
    }

    override fun onBackPressed() {
        isFinish()
    }

    private fun isFinish() {
        if (isOtaing && !isRouter)
            popFinish.showAtLocation(window.decorView.rootView, Gravity.CENTER, 0, 0)
        else
            finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        getStatusDispose?.dispose()
        disposableRouteTimer?.dispose()
    }
}