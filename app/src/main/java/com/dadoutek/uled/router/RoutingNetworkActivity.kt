package com.dadoutek.uled.router

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.text.method.ReplacementTransformationMethod
import android.view.inputmethod.InputMethodManager
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.GwLoginActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.HttpModel.RouterModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.util.NetWorkUtils
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_routing_network.*
import kotlinx.android.synthetic.main.toolbar.*
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
    private  var mac: String? = null
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
                RouterModel.routerAccessInNet(mac!!.toLowerCase(), split[0].toInt(), split[1].toInt())
                        ?.subscribe({
                            showLoadingDialog(getString(R.string.please_wait))
                            timeOutTimer?.dispose()
                            timeOutTimer = Observable.timer(it.toLong(), TimeUnit.SECONDS).subscribe {
                            ToastUtils.showShort(getString(R.string.router_access_in_fail))
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

    override fun routerAccessIn() {
        ToastUtils.showShort(getString(R.string.router_access_in_success))
       SharedPreferencesHelper.putBoolean(this, Constant.IS_GW_CONFIG_WIFI, false)
        val intent = Intent(this@RoutingNetworkActivity, GwLoginActivity::class.java)
        intent.putExtra("is_router",true)
        intent.putExtra("mac",mac?.toLowerCase())
        startActivity(intent)
        finish()
        hideLoadingDialog()
        timeOutTimer?.dispose()
    }
}


