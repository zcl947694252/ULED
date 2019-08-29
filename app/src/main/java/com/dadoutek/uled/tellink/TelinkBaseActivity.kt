package com.dadoutek.uled.tellink

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.SplashActivity
import com.dadoutek.uled.util.*
import com.telink.bluetooth.LeBluetooth
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.toolbar.*
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

open class TelinkBaseActivity : AppCompatActivity() {
    private var isRuning: Boolean = false
    private var authorStompClient: Disposable? = null
    private var pop: PopupWindow? = null
    private var popView: View? = null
    private var codeWarmDialog: AlertDialog? = null
    private var singleLogin: AlertDialog? = null
    private var payload: String? = null
    var longOperation: LongOperation? = null
    var normalSubscribe: Disposable? = null
    var loginStompClient: Disposable? = null
    var codeStompClient: Disposable? = null
    val TAG = "zcl"
    private var mStompClient: StompClient? = null
    protected var toast: Toast? = null
    protected var foreground = false
    private var loadDialog: Dialog? = null
    private var mReceive: BluetoothStateBroadcastReceive? = null
    private var mApplication: TelinkLightApplication? = null
    private var mScanDisposal: Disposable? = null

    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initOnLayoutListener()//加载view监听
        this.toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        foreground = true
        this.mApplication = this.application as TelinkLightApplication
        registerBluetoothReceiver()

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
    }

    //增加全局监听蓝牙开启状态
    private fun showOpenBluetoothDialog(context: Context) {
        val dialogTip = AlertDialog.Builder(context)
        dialogTip.setMessage(R.string.openBluetooth)
        dialogTip.setPositiveButton(android.R.string.ok) { dialog, which ->
            LeBluetooth.getInstance().enable(applicationContext)
        }
        dialogTip.setCancelable(false)
        dialogTip.create().show()
    }


    private fun unregisterBluetoothReceiver() {
        if (mReceive != null) {
            unregisterReceiver(mReceive)
            mReceive = null
        }
    }

    private fun registerBluetoothReceiver() {
        if (mReceive == null) {
            mReceive = BluetoothStateBroadcastReceive()
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF")
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON")
        registerReceiver(mReceive, intentFilter)
    }


    override fun onResume() {
        super.onResume()
        Constant.isTelBase = true
        foreground = true
        val blueadapter = BluetoothAdapter.getDefaultAdapter()
        if (blueadapter?.isEnabled == false) {
            showOpenBluetoothDialog(ActivityUtils.getTopActivity())
            if (toolbar != null) {
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener {
                    var dialog = BluetoothConnectionFailedDialog(this, R.style.Dialog)
                    if (this.isFinishing)
                        dialog.show()
                }
            }
        } else {
            if (toolbar != null) {
                if (TelinkLightApplication.getInstance().connectDevice == null) {
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener {
                        var dialog = BluetoothConnectionFailedDialog(this, R.style.Dialog)
                        dialog.show()
                    }
                } else {
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = false
                }
            }
        }
        if (SharedPreferencesHelper.getBoolean(this@TelinkBaseActivity, Constant.IS_LOGIN, false)) {
            longOperation = LongOperation()
            longOperation!!.execute()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        isRuning = false
        this.toast!!.cancel()
        this.toast = null
        unregisterBluetoothReceiver()
        mScanDisposal?.dispose()
        singleLogin?.dismiss()
    }

    open fun initOnLayoutListener() {
        var view = window.decorView
        var viewTreeObserver = view.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener {
            isRuning = true
            view.viewTreeObserver.removeOnGlobalLayoutListener({})
        }
    }

    fun showToast(s: CharSequence) {
        ToastUtils.showLong(s)
    }


    fun showLoadingDialog(content: String) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.dialogview, null)

        val layout = v.findViewById<View>(R.id.dialog_view) as LinearLayout
        val tvContent = v.findViewById<View>(R.id.tvContent) as TextView
        tvContent.text = content

        if (loadDialog == null) {
            loadDialog = Dialog(this,
                    R.style.FullHeightDialog)
        }
        //loadDialog没显示才把它显示出来
        if (!loadDialog!!.isShowing) {
            loadDialog!!.setCancelable(false)
            loadDialog!!.setCanceledOnTouchOutside(false)
            loadDialog!!.setContentView(layout)
            if (!this.isDestroyed) {
                GlobalScope.launch(Dispatchers.Main) {
                    loadDialog!!.show()
                }
            }
        }
    }

    fun hideLoadingDialog() {
        GlobalScope.launch(Dispatchers.Main) {
            if (loadDialog != null && this.isActive) {
                loadDialog!!.dismiss()
            }
        }
    }

    fun compileExChar(str: String): Boolean {
        return StringUtils.compileExChar(str)
    }


    inner class BluetoothStateBroadcastReceive : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (toolbar != null) {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = false
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (toolbar != null) {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener {
                            var dialog = BluetoothConnectionFailedDialog(context, R.style.Dialog)
                            dialog.show()
                        }
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                        BluetoothAdapter.STATE_OFF -> {
                            if (toolbar != null) {
//                                retryConnect()
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener {
                                    var dialog = BluetoothConnectionFailedDialog(context, R.style.Dialog)
                                    dialog.show()
                                }
                            }
                        }
                        BluetoothAdapter.STATE_ON -> {
                            if (toolbar != null) {
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = false
                            }
                        }
                    }
                }
            }
        }
    }


    inner class LongOperation : AsyncTask<String, Void, String>() {
        @RequiresApi(Build.VERSION_CODES.O)
        @SuppressLint("CheckResult")
        override fun doInBackground(vararg params: String): String {
            //虚拟主机号。测试服:/smartlight/test 正式服:/smartlight
            var headers = ArrayList<StompHeader>()
            headers.add(StompHeader("user-id", DBUtils.lastUser?.id.toString()))
            headers.add(StompHeader("host", Constant.WS_DEBUG_HOST))

            mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, Constant.WS_BASE_URL)
            mStompClient!!.connect(headers)

            mStompClient!!.withClientHeartbeat(25000).withServerHeartbeat(25000)

            var headersLogin = ArrayList<StompHeader>()

            headersLogin.add(StompHeader("id", DBUtils.lastUser?.id.toString()))
            headersLogin.add(StompHeader("destination", Constant.WS_DEBUG_HOST))

            codeStompClient = mStompClient!!.topic(Constant.WS_TOPIC_CODE, headersLogin).subscribe(
                    { topicMessage ->
                        //Log.e(TAG, "收到解析二维码信息:$topicMessage")
                        val payloadCode = topicMessage.payload
                        val codeBean = JSONObject(payloadCode)
                        val phone = codeBean.get("ref_user_phone")
                        val type = codeBean.get("type") as Int
                        val account = codeBean.get("account")
                        // Log.e(TAG, "zcl***解析二维码***获取消息$payloadCode------------$type----------------$phone-----------$account")
                        makeCodeDialog(type, phone, account, "")
                    },
                    {
                        ToastUtils.showShort(it.localizedMessage)
                    }
            )

            authorStompClient = mStompClient!!.topic(Constant.WS_AUTHOR_CODE, headersLogin).subscribe({ topicMessage ->
                Log.e(TAG, "收到解除授权信息:$topicMessage")
                val payloadCode = topicMessage.payload
                val codeBean = JSONObject(payloadCode)
                val phone = codeBean.get("authorizer_user_phone")
                val regionName = codeBean.get("region_name")
                val authorizerUserId = codeBean.get("authorizer_user_id")
                val rid = codeBean.get("rid")
                Log.e(TAG, "zcl***解析二维码***获取消息$payloadCode------------$phone----------------$regionName-----------")
                val user = DBUtils.lastUser
                user?.let {
                    if (user.last_authorizer_user_id == authorizerUserId && user.last_region_id == rid) {
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
            },{
                ToastUtils.showShort(it.localizedMessage)
            })


            loginStompClient = mStompClient!!.topic(Constant.WS_TOPIC_LOGIN, headersLogin).subscribe ({ topicMessage ->
                payload = topicMessage.payload
                //Log.e(TAG, "收到信息:$topicMessage")
                var key = SharedPreferencesHelper.getString(this@TelinkBaseActivity, Constant.LOGIN_STATE_KEY, "no_have_key")
                // Log.e(TAG, "zcl***login***获取消息$payload----------------------------" + { payload == key })
                if (payload == key)
                    return@subscribe
                checkNetworkAndSync(this@TelinkBaseActivity)
            },{
                ToastUtils.showShort(it.localizedMessage)
            })

            normalSubscribe = mStompClient!!.lifecycle().subscribe ({ lifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> Log.e(TAG, "zcl******Stomp connection opened")
                    LifecycleEvent.Type.ERROR -> Log.e(TAG, "zcl******Error" + lifecycleEvent.exception)
                    LifecycleEvent.Type.CLOSED -> Log.e(TAG, "zcl******Stomp connection closed")
                }
            },{
                ToastUtils.showShort(it.localizedMessage)
            })
            return "Executed"
        }

        override fun onPostExecute(result: String) {}
    }

    override fun onPause() {
        super.onPause()
        codeStompClient?.dispose()
        loginStompClient?.isDisposed
        normalSubscribe?.dispose()
        longOperation?.cancel(true)
        isRuning = false
        foreground = false
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
                        activity.startActivityForResult(Intent(Settings.ACTION_WIRELESS_SETTINGS), 0)
                    }.create().show()
        } else {
            SyncDataPutOrGetUtils.syncPutDataStart(activity, syncCallback)
        }
    }


    internal var syncCallbackGet: SyncCallback = object : SyncCallback {

        override fun start() {}
        override fun complete() {}

        @SuppressLint("CheckResult")
        override fun error(msg: String) {
        }
    }

    /**
     * 上传回调
     */
    private var syncCallback: SyncCallback = object : SyncCallback {
        override fun start() {
            showLoadingDialog(getString(R.string.tip_start_sync))
        }

        override fun complete() {
            hideLoadingDialog()
            SharedPreferencesHelper.putBoolean(this@TelinkBaseActivity, Constant.IS_LOGIN, false)
            TelinkLightService.Instance()?.disconnect()
            TelinkLightService.Instance()?.idleMode(true)
            val b = this@TelinkBaseActivity.isFinishing
            Log.e("zcl", "zcl***isFinishing***$b")
            if (!b)
                singleLogin!!.show()
        }

        override fun error(msg: String) {
            ToastUtils.showLong(msg)
            hideLoadingDialog()
        }
    }


    //重启app并杀死原进程
    open fun restartApplication() {
        ActivityUtils.finishAllActivities(true)
        ActivityUtils.startActivity(SplashActivity::class.java)
        Log.e("zcl", "zcl******重启app并杀死原进程")
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n", "StringFormatInvalid")
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

                initOnLayoutListener()
                if (!this@TelinkBaseActivity.isFinishing && !pop!!.isShowing && Constant.isTelBase)
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
                                SharedPreferencesHelper.putBoolean(this@TelinkBaseActivity, Constant.IS_LOGIN, false)
                            }
                }
            }
        }
    }

    open fun notifyWSData() {

    }
}


