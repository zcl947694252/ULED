package com.dadoutek.uled.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_save_lock.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2020/5/12 18:06
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class SafeLockActivity : TelinkBaseActivity(), View.OnClickListener {
    private var mConnectDisposal: Disposable? = null
    private var allGroup: DbGroup? = null
    private var isFristUserClickCheckConnect: Boolean = true
    private var status: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_lock)
        initView()
        initData()
        initListener()
    }

    private fun initData() {
        toolbarTv.text = getString(R.string.safe_lock)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun initView() {
        allGroup = DBUtils.getGroupByMeshAddr(0xFFFF)
    }

    private fun initListener() {
        safe_open.setOnClickListener(this)
        safe_lock.setOnClickListener(this)
        safe_close.setOnClickListener(this)
        safe_unlock.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        isFristUserClickCheckConnect = true
        val dstAddr = this.allGroup!!.meshAddr
        if (TelinkLightApplication.getApp().connectDevice == null && !Constant.IS_ROUTE_MODE)
            checkConnect()
        else {
            when (v?.id) {
                R.id.safe_open -> {
                    ToastUtils.showShort(getString(R.string.open_light))
                    if (Constant.IS_ROUTE_MODE)
                        routeOpenOrCloseBase(DBUtils.allGroups[0].meshAddr, 97, 1, "safeLockOpen")
                    else
                        Commander.openOrCloseLights(dstAddr, true)
                    //safe_open.setBackgroundResource(R.drawable.rect_blue_60)
                    //safe_open_arrow.setImageResource(R.mipmap.icon_safe_arrow_blue)
                    //safe_close.setBackgroundResourc
                    // rawable.rect_gray_60)
                    //safe_close_arrow.setImageResource(R.mipmap.icon_arrow_safe)
                }

                R.id.safe_lock -> {
                    ToastUtils.showShort(getString(R.string.lock))
                    //safe_close.setBackgroundResource(R.drawable.rect_gray_60)
                    // safe_open.setBackgroundResource(R.drawable.rect_blue_60)
                    //1打开2关闭 12位
                    status = 1
                    if (Constant.IS_ROUTE_MODE)
                        routerLockOrUnlock(status, "lock")
                    else
                        TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_EXTEND_OPCODE, 0xffff, byteArrayOf(Opcode.CONFIG_EXTEND_SAFE_LOCK, 1))
                }

                R.id.safe_close -> {
                    ToastUtils.showShort(getString(R.string.close_light))
                    // safe_close.setBackgroundResource(R.drawable.rect_blue_60)
                    //safe_close_arrow.setImageResource(R.mipmap.icon_safe_arrow_blue)
                    //safe_open.setBackgroundResource(R.drawable.rect_gray_60)
                    //safe_open_arrow.setImageResource(R.mipmap.icon_arrow_safe)
                    if (Constant.IS_ROUTE_MODE) {
                        routeOpenOrCloseBase(DBUtils.allGroups[0].meshAddr, 97, 0, "safeLockClose")
                    } else {
                        Commander.openOrCloseLights(dstAddr, false)
                    }
                }

                R.id.safe_unlock -> {
                    ToastUtils.showShort(getString(R.string.unlock))
                    //safe_open.setBackgroundResource(R.drawable.rect_gray_60)
                    //safe_close.setBackgroundResource(R.drawable.rect_blue_60)
                    //1打开2关闭 12位
                    status = 2
                    if (Constant.IS_ROUTE_MODE)
                        routerLockOrUnlock(status, "unlock")
                    else
                        TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_EXTEND_OPCODE, 0xffff, byteArrayOf(Opcode.CONFIG_EXTEND_SAFE_LOCK, 2))
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun routerLockOrUnlock(status: Int, serid: String) {
        RouterModel.routeOpenOrCloseSafeLock(status, serid)?.subscribe({
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                             .subscribeOn(Schedulers.io())
                                             .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                hideLoadingDialog()
                                if (status == 1)
                                    ToastUtils.showShort(getString(R.string.lock_fail))
                                else
                                    ToastUtils.showShort(getString(R.string.unlock_fail))

                            }
                }
                90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                90018 -> {
                    DBUtils.deleteLocalData()
                    //ToastUtils.showShort(getString(R.string.device_not_exit))
                    SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, syncCallbackGet)
                    finish()
                }
                90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                else-> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(getString(R.string.lock_fail))
        })
    }

    override fun tzRouterOpenOrClose(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl------收到路由开关灯通知------------$cmdBean")
        hideLoadingDialog()
        disposableRouteTimer?.dispose()
        when (cmdBean.ser_id) {
            "safeLockOpen" -> when (cmdBean.status) {
                0 -> ToastUtils.showShort(getString(R.string.open_light))
                else -> ToastUtils.showShort(getString(R.string.open_light_faile))
            }
            "safeLockClose" -> when (cmdBean.status) {
                0 -> ToastUtils.showShort(getString(R.string.close_light))
                else -> ToastUtils.showShort(getString(R.string.close_faile))
            }
        }
    }

    override fun tzRouterSafeLock(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl------收到路由开关锁通知------------$cmdBean")
        hideLoadingDialog()
        disposableRouteTimer?.dispose()
        when (cmdBean.ser_id) {
            "lock" -> when (cmdBean.status) {
                0 -> ToastUtils.showShort(getString(R.string.lock))
                else -> ToastUtils.showShort(getString(R.string.lock_fail))
            }
            "unlock" -> when (cmdBean.status) {
                0 -> ToastUtils.showShort(getString(R.string.unlock))
                else -> ToastUtils.showShort(getString(R.string.unlock_fail))
            }
        }
    }

    private fun checkConnect() {
        if (Constant.IS_ROUTE_MODE) return
        try {
            if (TelinkLightApplication.getApp().connectDevice == null) {
                ToastUtils.showShort(getString(R.string.connecting_tip))
                val deviceTypes = mutableListOf(DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD,
                        DeviceType.LIGHT_RGB, DeviceType.SMART_RELAY, DeviceType.SMART_CURTAIN)
                mConnectDisposal = connect(deviceTypes = deviceTypes, fastestMode = true, retryTimes = 10)
                        ?.subscribe(
                                {
                                    ToastUtils.showShort(getString(R.string.connect_success))
                                },
                                {
                                    ToastUtils.showShort(getString(R.string.connect_fail))
                                }
                        )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}