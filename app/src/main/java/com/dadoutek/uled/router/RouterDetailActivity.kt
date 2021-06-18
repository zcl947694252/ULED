package com.dadoutek.uled.router

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import android.text.TextUtils
import android.widget.EditText
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseToolbarActivity
import com.dadoutek.uled.gateway.GwLoginActivity
import com.dadoutek.uled.gateway.bean.DbRouter
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.router.bean.MacResetBody
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_router_detail.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.startActivity
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initData()
        initListener()
    }

    override fun setLayoutId(): Int {
        return R.layout.activity_router_detail
    }

    private fun initListener() {

    }

    override fun batchRenameVisible(): Boolean {
        return true
    }

    override fun renameDevice() {
        if (!TextUtils.isEmpty(router?.name))
            renameEt?.setText(router?.name)
        renameEt?.setSelection(renameEt?.text.toString().length)
        StringUtils.initEditTextFilter(renameEt)
        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }

        renameConfirm?.setOnClickListener {    // 获取输入框的内容
            if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                val trim = renameEt?.text.toString().trim { it <= ' ' }
                routerRenameDevice(trim)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun routerRenameDevice(toString: String) {
        RouterModel.routeUpdateRouterName(router?.id ?: 0, toString)?.subscribe({
            ToastUtils.showShort(getString(R.string.rename_success))
            router?.name = toString
            DBUtils.saveRouter(router!!,true)
            toolbarTv.text = router?.name
            renameDialog?.dismiss()
        }, {
            renameDialog?.dismiss()
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("SetTextI18n")
    private fun initData() {
        val routerId = intent.getLongExtra("routerId", 1000000)
        router = DBUtils.getRouterByID(routerId)
        if (router == null) {
            ToastUtils.showShort(getString(R.string.get_router_empty))
            finish()
        }
        bindRouter?.title = router?.version
        deleteDeviceAll?.title = getString(R.string.delete_device)
        router_detail_mac.text = "MAC:"+router?.macAddr
        toolbarTv.text = router?.name
    }

    private fun initView() {
        type = Constant.INSTALL_ROUTER
    }

    override fun batchGpVisible(): Boolean {
        batchGpAll?.title = getString(R.string.config_WIFI)
        return true
    }

    override fun goOta() {
        startActivity<RouterOtaActivity>("deviceMeshAddress" to 100000, "deviceType" to DeviceType.LIGHT_NORMAL,
                "deviceMac" to router!!.macAddr,"version" to router!!.version )
        finish()
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

    override fun editeDeviceAdapter() {}

    override fun setToolbar(): Toolbar {
        return toolbar
    }

    override fun setDeviceDataSize(num: Int): Int {
        return 1
    }

    @SuppressLint("CheckResult")
    override fun editeDevice() {
        RouterModel.routeResetFactoryBySelf(MacResetBody(router!!.macAddr, 0, 0, "routerFactory"))?.subscribe({
            LogUtils.v("zcl-----------请求路由恢复出厂-------$it")
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                             .subscribeOn(Schedulers.io())
                                             .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.delete_device_fail))
                            }
                }
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                else -> ToastUtils.showShort(it.message)
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