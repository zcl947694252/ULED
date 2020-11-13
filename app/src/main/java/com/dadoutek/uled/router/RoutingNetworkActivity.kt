package com.dadoutek.uled.router

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.inputmethod.InputMethodManager
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.router.bean.RouteInAccountBean
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_routing_network.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.startActivity
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2020/8/15 12:03
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class RoutingNetworkActivity : TelinkBaseActivity() {
    private val TAG = "RoutingNetworkActivity"
    private var mac: String? = null
    private var timeOutTimer: Disposable? = null
    private var result: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routing_network)
        initView()
        initData()
        initListener()
    }

    @SuppressLint("CheckResult")
    private fun initListener() {
        routing_confim.setOnClickListener {
            val timeZone = NetWorkUtils.getTimeZone()
            val split = timeZone.replace("GMT+", "").split(":")
            mac = routing_device_code.text.toString()
            if (TextUtils.isEmpty(mac)) {
                ToastUtils.showShort(getString(R.string.device_code_cannot_be_empty))
            } else {
                RouterModel.routerAccessInNet(mac!!.toLowerCase(), split[0].toInt(), split[1].toInt(), TAG)
                        ?.subscribe({
                            LogUtils.v("zcl-----------路由请求入网-------$it")
                            when (it.errorCode) {
                                0 -> {
                                   hideLoadingDialog()
                                    timeOutTimer?.dispose()
                                    timeOutTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                                             .subscribeOn(Schedulers.io())
                                                             .observeOn(AndroidSchedulers.mainThread()).subscribe {
                                        ToastUtils.showShort(getString(R.string.router_access_in_fail))
                                    }
                                }
                                90010 -> ToastUtils.showShort(getString(R.string.route_code_error))
                                90003 -> ToastUtils.showShort(getString(R.string.route_other_add))
                                90002 -> ToastUtils.showShort(getString(R.string.router_added))
                                90001 -> ToastUtils.showShort(getString(R.string.router_offline))
                            }
                        }, {
                            ToastUtils.showShort(it.message)
                        })
                LogUtils.v("zcl时区----$timeZone")
            }
        }
    }

    private fun initData() {
        result = intent.getStringExtra(Constant.ONE_QR)
        routing_device_code.setText(result)
        routing_device_code.setSelection(result?.length ?: 0)
    }

    private fun initView() {
        toolbarTv.text = getString(R.string.routing_network)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setNavigationIcon(R.drawable.icon_return)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        routing_device_code.requestFocus()
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    override fun routerAccessIn(routerGroup: RouteInAccountBean) {
        LogUtils.v("zcl----------收到路由入网通知-------$routerGroup")
        if (routerGroup.ser_id == TAG) {
            if (routerGroup.status == Constant.ALL_SUCCESS) {
                SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, object : SyncCallback {
                    override fun start() { }

                    override fun complete() {
                        startToDetail(routerGroup)
                    }

                    override fun error(msg: String?) {
                        startToDetail(routerGroup)
                    }
                })
               /* val intent = Intent(this@RoutingNetworkActivity, GwLoginActivity::class.java)
                intent.putExtra("is_router", true)
                intent.putExtra("mac", mac?.toLowerCase())
                startActivity(intent)
                finish()*/
            } else {
                ToastUtils.showShort(getString(R.string.router_access_in_fail))
            }
            hideLoadingDialog()
            timeOutTimer?.dispose()
        }
    }

    private fun startToDetail(routerGroup: RouteInAccountBean) {
        startActivity<RouterDetailActivity>("routerId" to routerGroup?.router.id.toLong())
        ToastUtils.showShort(getString(R.string.router_access_in_success))
        finish()
    }
}


