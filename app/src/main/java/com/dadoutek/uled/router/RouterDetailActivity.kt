package com.dadoutek.uled.router

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.Log
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.base.TelinkBaseToolbarActivity
import com.dadoutek.uled.gateway.GwLoginActivity
import com.dadoutek.uled.gateway.bean.DbRouter
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.router.bean.MacResetBody
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.Observable
import kotlinx.android.synthetic.main.toolbar.*
import org.greenrobot.greendao.DbUtils
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2020/10/10 10:21
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class RouterDetailActivity : TelinkBaseToolbarActivity() {
    private var router: DbRouter? = null

    override fun batchGpVisible(): Boolean {
        batchGpAll?.title = getString(R.string.config_WIFI)
        return true
    }

    override fun bindRouterVisible(): Boolean {
        bindRouter?.title = router?.version
        return true
    }

    override fun skipBatch() {
        val intent = Intent(this, GwLoginActivity::class.java)
        intent.putExtra("is_router", true)
        intent.putExtra("mac", router?.macAddr?.toLowerCase())
        startActivity(intent)
        finish()
    }

    override fun setDeletePositiveBtn() {
    }

    override fun editeDeviceAdapter() {
    }

    override fun setToolbar(): Toolbar {
        return toolbar
    }

    override fun setDeviceDataSize(num: Int): Int {
        return 1
    }

    override fun setLayoutId(): Int {
        return R.layout.activity_router_detail
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {

    }

    private fun initData() {
        val routerId = intent.getLongExtra("routerId", 1000000)
        router = DBUtils.getRouterByID(routerId)
        if (router==null){
            ToastUtils.showShort(getString(R.string.get_router_empty))
            finish()
        }
        bindRouter?.title = router?.version
        deleteDeviceAll?.title = getString(R.string.delete_device)
    }

    private fun initView() {
        toolbarTv.text = getString(R.string.router)
    }

    @SuppressLint("CheckResult")
    override fun editeDevice() {
        RouterModel.routeResetFactoryBySelf(MacResetBody(router!!.macAddr, 0, 0, "routerFactory"))?.subscribe({
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.delete_device_fail))
                            }
                }
                90004 -> ToastUtils.showShort(getString(R.string.region_not_router))
                else-> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    override fun tzRouteResetFactoryBySelf(cmdBean: CmdBodyBean) {
        if (cmdBean.ser_id == "routerFactory") {
            disposableRouteTimer?.dispose()
            LogUtils.v("zcl-----------收到路由恢复出厂得到通知-------$cmdBean")
            hideLoadingDialog()
            if (cmdBean.status == 0)
                deleteData()
            else
                ToastUtils.showShort(getString(R.string.reset_factory_fail))
        }
    }

    private fun deleteData() {
        LogUtils.v("zcl-----恢复出厂成功")
        hideLoadingDialog()
        ToastUtils.showShort(getString(R.string.successful_resumption))
        DBUtils.deleteRouter(router!!)

        SyncDataPutOrGetUtils.syncPutDataStart(this, object : SyncCallback {
            override fun start() {}

            override fun complete() {}

            override fun error(msg: String?) {}
        })
        finish()
    }
}