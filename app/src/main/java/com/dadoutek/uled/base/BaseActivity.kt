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
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbConnector
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.SplashActivity
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.PopUtil
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main_content.*
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
import java.util.concurrent.TimeUnit

/**
 * 创建者     zcl
 * 创建时间   2019/8/21 20:06
 * 描述	      ${19928748860   %1$s  %2$s  %1$d Android s代表string  d代表int  1-9  }$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${//GlobalScope.launch { delay(1000) } //todo 使用协程替代thread看是否能解决溢出问题 delay想到与thread  所有内容要放入协程}$
 */

abstract class BaseActivity : AppCompatActivity() {
    private var mStompListener: Disposable? = null
    private var isRuning: Boolean = false
    private var authorStompClient: Disposable? = null
    private var compositeDisposable: CompositeDisposable? = null
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
        initOnLayoutListener()
        initStompStatusListener()
    }

    private fun initStompStatusListener() {
        mStompListener = mStompClient?.lifecycle()?.subscribe { t: LifecycleEvent? ->
            when (t) {
                LifecycleEvent.Type.OPENED -> {
                    LogUtils.d( "zcl_Stomp connection opened")

                }
                LifecycleEvent.Type.ERROR -> {
                    LogUtils.d("zcl_Stomp lifecycleEvent.getException()")

                }
                LifecycleEvent.Type.CLOSED -> {
                    LogUtils.d( "zcl_Stomp connection closed")
                }


            }
        }
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
            headers.add(StompHeader("host", Constant.WS_HOST))

            mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, Constant.WS_BASE_URL)
            mStompClient!!.connect(headers)
            mStompClient!!.withClientHeartbeat(25000).withServerHeartbeat(25000)

            var headersLogin = ArrayList<StompHeader>()
            headersLogin.add(StompHeader("id", DBUtils.lastUser!!.id.toString()))
            headersLogin.add(StompHeader("destination", Constant.WS_HOST))

            codeStompClient = mStompClient!!.topic(Constant.WS_TOPIC_CODE, headersLogin).subscribe ({ topicMessage ->
                Log.e(TAGS, "收到解析二维码信息:$topicMessage")
                val payloadCode = topicMessage.payload
                val codeBean = JSONObject(payloadCode)
                val phone = codeBean.get("ref_user_phone")
                val type = codeBean.get("type") as Int
                val account = codeBean.get("account")
                Log.e(TAGS, "zcl***解析二维码***获取消息$payloadCode------------$type----------------$phone-----------$account")
                makeCodeDialog(type, phone, account, "")
            },{ToastUtils.showShort(it.localizedMessage)})

            authorStompClient = mStompClient!!.topic(Constant.WS_AUTHOR_CODE, headersLogin).subscribe ({ topicMessage ->
                val payloadCode = topicMessage.payload
                val codeBean = JSONObject(payloadCode)
                val phone = codeBean.get("authorizer_user_phone")
                val regionName = codeBean.get("region_name")
                val authorizerUserId = codeBean.get("authorizer_user_id")
                val rid = codeBean.get("rid")
                Log.e(TAGS, "zcl***收到解除授权信息***获取消息$payloadCode------------$phone----------------$regionName-----------")
                val user = DBUtils.lastUser

                user?.let {
                    Log.e("zcl_BaseActivity", "zcl****判断**${user.last_authorizer_user_id == authorizerUserId}------${user.last_region_id == rid}------authorizerUserId--$authorizerUserId-----$rid")
                    if (user.last_authorizer_user_id == authorizerUserId.toString() && user.last_region_id == rid.toString()) {
                        user.last_region_id = 1.toString()
                        user.last_authorizer_user_id = user.id.toString()

                        DBUtils.deleteAllData()
                        //创建数据库
                        AccountModel.initDatBase(it)
                        //更新last—region-id
                        DBUtils.saveUser(user)
                        //下拉数据
                        Log.e("zclbaseActivity", "zcl******" + DBUtils.lastUser)
                        SyncDataPutOrGetUtils.syncGetDataStart(user, syncCallbackGet)
                    }
                }
                makeCodeDialog(2, phone, "", regionName)//2代表解除授权信息type
            },{ToastUtils.showShort(it.localizedMessage)})

            loginStompClient = mStompClient!!.topic(Constant.WS_TOPIC_LOGIN, headersLogin).subscribe ({ topicMessage ->
                payload = topicMessage.payload
                Log.e(TAGS, "收到信息:$topicMessage")
                var key = SharedPreferencesHelper.getString(this@BaseActivity, Constant.LOGIN_STATE_KEY, "no_have_key")
                Log.e(TAGS, "zcl***login***获取消息$payload----------------------------" + { payload == key })
                if (payload == key)
                    return@subscribe
                checkNetworkAndSync(this@BaseActivity)
            },{ToastUtils.showShort(it.localizedMessage)})

            normalSubscribe = mStompClient!!.lifecycle().subscribe( { lifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> Log.e(TAGS, "zcl******Stomp connection opened")
                    LifecycleEvent.Type.ERROR -> Log.e(TAGS, "zcl******Error" + lifecycleEvent.exception)
                    LifecycleEvent.Type.CLOSED -> Log.e(TAGS, "zcl******Stomp connection closed")
                    else -> Log.e(TAGS, "zcl******Stomp connection no get")
                }
            },{ToastUtils.showShort(it.localizedMessage)})
            return "Executed"
        }

        override fun onPostExecute(result: String) {}
    }

    open fun notifyWSData() {

    }



    @SuppressLint("SetTextI18n", "StringFormatInvalid", "StringFormatMatches")
    private fun makeCodeDialog(type: Int, phone: Any, account: Any, regionName: Any) {
        //移交码为0授权码为1
        var title: String? = null
        var recever: String? = null

        when (type) {
            0 -> {
                title = getString(R.string.author_account_receviced)
                recever = getString(R.string.recevicer)
            }
            1 -> {
                title = getString(R.string.author_region_receviced)
                recever = getString(R.string.recevicer)
            }
            2 -> {
                title = getString(R.string.author_region_unAuthorized, regionName)
                recever = getString(R.string.licensor)
            }
        }
        //todo  订阅区分逻辑 授权者被授权按着


        runOnUiThread {
            popView?.let {
                it.findViewById<TextView>(R.id.code_warm_title).text = title

                it.findViewById<TextView>(R.id.code_warm_context).text = recever + phone

                it.findViewById<TextView>(R.id.code_warm_i_see).setOnClickListener {
                    PopUtil.dismiss(pop)
                    if (type == 0) {
                        restartApplication()
                    }
                }
                notifyWSData()
                if (!this@BaseActivity.isFinishing && !pop!!.isShowing&&!Constant.isTelBase)
                    pop!!.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
            }
        }


        when (type) {
            0 -> { //修改密码
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
    }

    open fun notifyWSTransferData() {

    }

    override fun onResume() {
        super.onResume()
        Constant.isTelBase = false
        if (SharedPreferencesHelper.getBoolean(this@BaseActivity, Constant.IS_LOGIN, false)) {
            longOperation = LongOperation()
            longOperation!!.execute()
        }
    }

    fun initOnLayoutListener() {
        var view = window.decorView
        var viewTreeObserver = view.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener {
            isRuning = true
            view.viewTreeObserver.removeOnGlobalLayoutListener({})
        }
    }

    override fun onPause() {
        super.onPause()
        releseStomp()
        isRuning = false
    }

    private fun releseStomp() {
        longOperation?.cancel(true)
        normalSubscribe?.dispose()
        loginStompClient?.dispose()
        codeStompClient?.dispose()
        mStompListener?.dispose()
    }


    internal var syncCallbackGet: SyncCallback = object : SyncCallback {
        override fun start() {}
        override fun complete() {}
        override fun error(msg: String) {
        }
    }

    /**
     * 检查网络上传数据
     * 如果没有网络，则弹出网络设置对话框
     */
    fun checkNetworkAndSync(activity: Activity?) {
        codeStompClient?.dispose()
        loginStompClient?.isDisposed
        normalSubscribe?.dispose()
        authorStompClient?.dispose()
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


    val allLights: List<DbLight>
        get() {
            val groupList = DBUtils.groupList
            val lightList = ArrayList<DbLight>()

            for (i in groupList.indices) {
                lightList.addAll(DBUtils.getLightByGroupID(groupList[i].id!!))
            }
            return lightList
        }

    val allCutain: List<DbCurtain>
        get() {
            val groupList = DBUtils.groupList
            val lightList = ArrayList<DbCurtain>()

            for (i in groupList.indices) {
                lightList.addAll(DBUtils.getCurtainByGroupID(groupList[i].id!!))
            }
            return lightList
        }


    val allRely: List<DbConnector>
        get() {
            val groupList = DBUtils.groupList
            val lightList = ArrayList<DbConnector>()

            for (i in groupList.indices) {
                lightList.addAll(DBUtils.getConnectorByGroupID(groupList[i].id!!))
            }
            return lightList
        }

    open fun resetAllLight() {
        showLoadingDialog(getString(R.string.reset_all_now))
        SharedPreferencesHelper.putBoolean(this, Constant.DELETEING, true)
        val lightList = allLights
        val curtainList = allCutain
        val relyList = allRely

        var meshAdre = ArrayList<Int>()
        if (lightList.isNotEmpty()) {
            for (k in lightList.indices) {
                meshAdre.add(lightList[k].meshAddr)
            }
        }

        if (curtainList.isNotEmpty()) {
            for (k in curtainList.indices) {
                meshAdre.add(curtainList[k].meshAddr)
            }
        }

        if (relyList.isNotEmpty()) {
            for (k in relyList.indices) {
                meshAdre.add(relyList[k].meshAddr)
            }
        }

        if (meshAdre.size > 0) {
            Commander.resetLights(meshAdre, {
                SharedPreferencesHelper.putBoolean(this, Constant.DELETEING, false)
                syncData()
                this.bnve?.currentItem = 0
            }, {
                SharedPreferencesHelper.putBoolean(this, Constant.DELETEING, false)
            })
        }
        if (meshAdre.isEmpty()) {
            hideLoadingDialog()
        }
    }

    private fun syncData() {
        SyncDataPutOrGetUtils.syncPutDataStart(this, object : SyncCallback {
            override fun complete() {
                hideLoadingDialog()
                val disposable = Observable.timer(500, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { }

                if (compositeDisposable!!.isDisposed) {
                    compositeDisposable = CompositeDisposable()
                }
                compositeDisposable!!.add(disposable)
            }

            override fun error(msg: String) {
                hideLoadingDialog()
                ToastUtils.showShort(R.string.backup_failed)
            }

            override fun start() {}
        })
    }
}