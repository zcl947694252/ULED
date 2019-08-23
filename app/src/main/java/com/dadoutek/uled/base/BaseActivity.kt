package com.dadoutek.uled.base

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.blankj.utilcode.util.ActivityUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.SplashActivity
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.PopUtil
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import java.util.*

/**
 * 创建者     zcl
 * 创建时间   2019/8/21 20:06
 * 描述	      ${19928748860 }$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}$
 */

abstract class BaseActivity : AppCompatActivity() {
    private var pop: PopupWindow? = null
    private var popView: View? = null
    private var dialog: Dialog? = null
    private var codeWarmDialog: AlertDialog? = null
    var loadDialog: Dialog? = null
    val TAGS = "zcl_BaseActivity"
    private var singleLogin: AlertDialog? = null
    private var longOperation: LongOperation? = null
    private var normalSubscribe: Disposable? = null
    private var payload: String? = null
    private var loginStompClient: Disposable? = null
    private var codeStompClient: Disposable? = null
    private var mStompClient: StompClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(setLayoutID())

        singleLogin = AlertDialog.Builder(this)
                .setTitle(R.string.other_device_login)
                .setMessage(getString(R.string.single_login_warm))
                .setCancelable(false)
                .setOnDismissListener {
                    restartApplication()
                }.setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                    restartApplication()
                }.create()

        popView = LayoutInflater.from(this).inflate(R.layout.code_warm, null)

        pop = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        pop?.let {
            it.isFocusable = true // 设置PopupWindow可获得焦点
            it.isTouchable = true // 设置PopupWindow可触摸补充：
            it.isOutsideTouchable = false
        }

        initView()
        initData()
        initListener()
    }

    abstract fun setLayoutID(): Int
    abstract fun initView()
    abstract fun initData()
    abstract fun initListener()

    inner class LongOperation : AsyncTask<String, Void, String>() {
        @SuppressLint("CheckResult")
        override fun doInBackground(vararg params: String): String {
            //虚拟主机号。测试服:/smartlight/test 正式服:/smartlight
            var headers = ArrayList<StompHeader>()
            headers.add(StompHeader("user-id", DBUtils.lastUser!!.id.toString()))
            headers.add(StompHeader("host", Constant.WS_DEBUG_HOST))

            mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, Constant.WS_BASE_URL)
            mStompClient!!.connect(headers)
            mStompClient!!.withClientHeartbeat(25000).withServerHeartbeat(25000)

            var headersLogin = ArrayList<StompHeader>()
            headersLogin.add(StompHeader("id", DBUtils.lastUser!!.id.toString()))
            headersLogin.add(StompHeader("destination", Constant.WS_DEBUG_HOST))

            codeStompClient = mStompClient!!.topic(Constant.WS_TOPIC_CODE, headersLogin).subscribe { topicMessage ->
                Log.e(TAGS, "收到解析二维码信息:$topicMessage")
                val payloadCode = topicMessage.payload
                val codeBean = JSONObject(payloadCode)
                val phone = codeBean.get("ref_user_phone")
                val type = codeBean.get("type") as Int
                val account = codeBean.get("account")
                Log.e(TAGS, "zcl***解析二维码***获取消息$payloadCode------------$type----------------$phone-----------$account")
                makeCodeDialog(type, phone, account)
            }


            loginStompClient = mStompClient!!.topic(Constant.WS_TOPIC_LOGIN, headersLogin).subscribe { topicMessage ->
                payload = topicMessage.payload
                Log.e(TAGS, "收到信息:$topicMessage")
                var key = SharedPreferencesHelper.getString(this@BaseActivity, Constant.LOGIN_STATE_KEY, "no_have_key")
                Log.e(TAGS, "zcl***login***获取消息$payload----------------------------" + { payload == key })
                if (payload == key)
                    return@subscribe
                checkNetworkAndSync(this@BaseActivity)
            }

            normalSubscribe = mStompClient!!.lifecycle().subscribe { lifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> Log.e(TAGS, "zcl******Stomp connection opened")
                    LifecycleEvent.Type.ERROR -> Log.e(TAGS, "zcl******Error" + lifecycleEvent.exception)
                    LifecycleEvent.Type.CLOSED -> Log.e(TAGS, "zcl******Stomp connection closed")
                    else -> Log.e(TAGS, "zcl******Stomp connection no get")
                }
            }
            return "Executed"
        }

        override fun onPostExecute(result: String) {}
    }

    open fun notifyWSData() {

    }

    @SuppressLint("SetTextI18n")
    private fun makeCodeDialog(type: Int, phone: Any, account: Any) {
        //移交码为0授权码为1
        var title: String? = null


        when (type) {
            0 -> title = getString(R.string.author_account_receviced)
            1 -> title = getString(R.string.author_region_receviced)
        }
        runOnUiThread {
            popView?.let {
                it.findViewById<TextView>(R.id.code_warm_title).text = title
                it.findViewById<TextView>(R.id.code_warm_context).text = getString(R.string.recevicer) + phone

                it.findViewById<TextView>(R.id.code_warm_i_see).setOnClickListener {
                    PopUtil.dismiss(pop)
                    if (type == 0) {
                        restartApplication()
                    }
                }
                notifyWSData()
                if (!this@BaseActivity.isFinishing && !pop!!.isShowing)
                    pop!!.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
            }
        }


        if (type == 0) {
            //修改密码
            val user = SharedPreferencesUtils.getLastUser()
            DBUtils.deleteAllData()
            user?.let {
                val split = user.split("-")
                if (split.size < 3)
                    return@let
                Log.e("zcl", "zcl***修改密码***" + split[0] + "------" + split[1] + ":===" + split[2] + "====" + account)

                NetworkFactory.getApi()
                        .putPassword(account.toString(), NetworkFactory.md5(split[1]))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            Log.e("zcl", "zcl修改密码******${it.message}")
                            SharedPreferencesUtils.saveLastUser(split[0] + "-" + split[1] + "-" + account)
                            codeStompClient?.dispose()
                            loginStompClient?.isDisposed
                            normalSubscribe?.dispose()
                            longOperation?.cancel(true)
                            TelinkLightService.Instance()?.disconnect()
                            TelinkLightService.Instance()?.idleMode(true)
                            SharedPreferencesHelper.putBoolean(this@BaseActivity, Constant.IS_LOGIN, false)
                        }
            }
        }
    }

    open fun notifyWSTransferData() {

    }

    override fun onResume() {
        super.onResume()
        if (SharedPreferencesHelper.getBoolean(this@BaseActivity, Constant.IS_LOGIN, false)) {
            longOperation = LongOperation()
            longOperation!!.execute()
        }
    }

    override fun onPause() {
        super.onPause()
        codeStompClient?.dispose()
        loginStompClient?.isDisposed
        normalSubscribe?.dispose()
        longOperation?.cancel(true)
    }


    /**
     * 检查网络上传数据
     * 如果没有网络，则弹出网络设置对话框
     */
    fun checkNetworkAndSync(activity: Activity?) {
        codeStompClient?.dispose()
        loginStompClient?.isDisposed
        normalSubscribe?.dispose()
        longOperation?.cancel(true)

        if (!NetWorkUtils.isNetworkAvalible(activity!!)) {
            AlertDialog.Builder(activity)
                    .setTitle(R.string.network_tip_title)
                    .setMessage(R.string.net_disconnect_tip_message)
                    .setPositiveButton(android.R.string.ok
                    ) { _, _ ->
                        // 跳转到设置界面
                        activity.startActivityForResult(Intent(
                                Settings.ACTION_WIRELESS_SETTINGS),
                                0)
                    }.create().show()
        } else {
            SyncDataPutOrGetUtils.syncPutDataStart(activity, syncCallback)
        }
    }

    /**
     * 上传回调
     */
    private var syncCallback: SyncCallback = object : SyncCallback {
        override fun error(msg: String?) {}

        override fun start() {
            showLoadingDialog(this@BaseActivity.getString(R.string.tip_start_sync))
        }

        override fun complete() {
            SharedPreferencesHelper.putBoolean(this@BaseActivity, Constant.IS_LOGIN, false)
            TelinkLightService.Instance()?.disconnect()
            TelinkLightService.Instance()?.idleMode(true)
            if (!this@BaseActivity.isFinishing)
                singleLogin!!.show()
        }
    }

    //重启app并杀死原进程
    private fun restartApplication() {
        ActivityUtils.finishAllActivities(true)
        ActivityUtils.startActivity(SplashActivity::class.java)
        Log.e("zcl", "zcl******重启app并杀死原进程")
    }


    fun showLoadingDialog(content: String) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.dialogview, null)

        val layout = v.findViewById<View>(R.id.dialog_view) as LinearLayout
        val tvContent = v.findViewById<View>(R.id.tvContent) as TextView
        tvContent.text = content

        if (loadDialog == null)
            loadDialog = Dialog(this, R.style.FullHeightDialog)

        //loadDialog没显示才把它显示出来
        if (!loadDialog!!.isShowing) {
            loadDialog!!.setCancelable(false)
            loadDialog!!.setCanceledOnTouchOutside(false)
            loadDialog!!.setContentView(layout)
            if (!this.isDestroyed)
                GlobalScope.launch(Dispatchers.Main) { loadDialog!!.show() }
        }
    }

    fun hideLoadingDialog() {
        GlobalScope.launch(Dispatchers.Main) {
            if (loadDialog != null && this.isActive) {
                loadDialog!!.dismiss()
            }
        }
    }

}