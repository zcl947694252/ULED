package com.dadoutek.uled.tellink

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.CancelAuthorMsg
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.othersview.SplashActivity
import com.dadoutek.uled.stomp.StompManager
import com.dadoutek.uled.stomp.model.QrCodeTopicMsg
import com.dadoutek.uled.util.*
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.light.ErrorReportInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.util.EventListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

open class TelinkBaseActivity : AppCompatActivity() {
    private var mStompListener: Disposable? = null
    private var isRuning: Boolean = false
    private var authorStompClient: Disposable? = null
    private var pop: PopupWindow? = null
    private var popView: View? = null
    private var codeWarmDialog: AlertDialog? = null
    private var singleLogin: AlertDialog? = null
    private var payload: String? = null
    var stompLifecycleDisposable: Disposable? = null
    var singleLoginTopicDisposable: Disposable? = null
    var codeStompClient: Disposable? = null
    val TAG = "zcl"
    private lateinit var stompRecevice: StompReceiver

    private lateinit var mStompManager: StompManager
    protected var toast: Toast? = null
    protected var foreground = false
    private var loadDialog: Dialog? = null
//    private var mReceive: BluetoothStateBroadcastReceive? = null
    private var mApplication: TelinkLightApplication? = null
    private var mScanDisposal: Disposable? = null

    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initOnLayoutListener()//加载view监听
        this.toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        foreground = true
        this.mApplication = this.application as TelinkLightApplication

        initStompReceiver()
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


/*
    private fun unregisterBluetoothReceiver() {
        if (mReceive != null) {
            unregisterReceiver(mReceive)
            mReceive = null
        }
    }
*/

/*
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
*/


    /**
     * 改变Toolbar上的图片和状态
     * @param isConnected       是否是连接状态
     */
    private fun changeDisplayImgOnToolbar(isConnected: Boolean) {
        if (isConnected) {
            if (toolbar != null) {

                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = false
            }

        } else {
            if (toolbar != null) {
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener {
                    val dialog = BluetoothConnectionFailedDialog(this, R.style.Dialog)
                    dialog.show()
                }
            }

        }
    }

    //打开基类的连接状态变化监听
    fun enableConnectionStatusListener() {
        //先取消，这样可以确保不会重复添加监听
        this.mApplication?.removeEventListener(DeviceEvent.STATUS_CHANGED, StatusChangedListener)
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, StatusChangedListener)
    }

    //关闭基类的连接状态变化监听
    fun disableConnectionStatusListener() {
        this.mApplication?.removeEventListener(DeviceEvent.STATUS_CHANGED, StatusChangedListener)
    }

    private val StatusChangedListener = EventListener<String?> { event ->
        when (event.type) {
            DeviceEvent.STATUS_CHANGED -> {
                onDeviceStatusChanged(event as DeviceEvent)
            }
        }
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {
        val deviceInfo = event.args
        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                ToastUtils.showLong(getString(R.string.connect_success))
                changeDisplayImgOnToolbar(true)

                val connectDevice = this.mApplication?.connectDevice
                LogUtils.d("directly connection device meshAddr = ${connectDevice?.meshAddress}")
                RecoverMeshDeviceUtil.addDevicesToDb(deviceInfo)//  如果已连接的设备不存在数据库，则创建。

            }
            LightAdapter.STATUS_LOGOUT -> {
                changeDisplayImgOnToolbar(false)
            }

            LightAdapter.STATUS_CONNECTING -> {
                ToastUtils.showLong(R.string.connecting_please_wait)
            }
        }


    }

    override fun onResume() {
        super.onResume()
        enableConnectionStatusListener()
        Constant.isTelBase = true
        foreground = true
        val lightService: TelinkLightService? = TelinkLightService.Instance()
        if (LeBluetooth.getInstance().isSupport(applicationContext))
            LeBluetooth.getInstance().enable(applicationContext)

        if (LeBluetooth.getInstance().isEnabled){
            if (lightService?.isLogin == true) {
                changeDisplayImgOnToolbar(true)
            }else{
                changeDisplayImgOnToolbar(false)
            }
        }else{
            changeDisplayImgOnToolbar(false)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        isRuning = false
        this.toast!!.cancel()
        this.toast = null
//        unregisterBluetoothReceiver()
        unregisterReceiver(stompRecevice)

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

/*
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
*/

    override fun onPause() {
        super.onPause()
        isRuning = false
        foreground = false

        disableConnectionStatusListener()
    }


    /**
     * 检查网络上传数据
     * 如果没有网络，则弹出网络设置对话框
     */
    fun checkNetworkAndSync(activity: Activity?) {
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
            if (!b && !singleLogin?.isShowing!!) {
                singleLogin!!.show()
            }
        }

        override fun error(msg: String) {
            ToastUtils.showLong(msg)
            hideLoadingDialog()
        }
    }

    //重启app并杀死原进程
    open fun restartApplication() {
        TelinkLightApplication.getApp().releseStomp()
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
                            .subscribe(object : NetworkObserver<Response<DbUser>?>() {
                                override fun onNext(t: Response<DbUser>) {
                                    Log.e("zcl", "zcl修改密码******${t.message}")
                                    SharedPreferencesUtils.saveLastUser(split[0] + "-" + split[1] + "-" + account)
                                    codeStompClient?.dispose()
                                    singleLoginTopicDisposable?.isDisposed
                                    stompLifecycleDisposable?.dispose()
                                    TelinkLightService.Instance()?.disconnect()
                                    TelinkLightService.Instance()?.idleMode(true)
                                    SharedPreferencesHelper.putBoolean(this@TelinkBaseActivity, Constant.IS_LOGIN, false)
                                }
                            })
                }
            }
        }
    }

    open fun notifyWSData() {

    }

    private fun initStompReceiver() {
        stompRecevice = StompReceiver()
        val filter = IntentFilter()
        filter.addAction(Constant.LOGIN_OUT)
        filter.addAction(Constant.CANCEL_CODE)
        filter.addAction(Constant.PARSE_CODE)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
        registerReceiver(stompRecevice, filter)
    }

    inner class StompReceiver : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constant.LOGIN_OUT -> {
                    LogUtils.e("zcl_baseMe___________收到登出消息")
                    loginOutMethod()
                }
                Constant.CANCEL_CODE -> {
                    val cancelBean = intent.getSerializableExtra(Constant.CANCEL_CODE) as CancelAuthorMsg
                    val user = DBUtils.lastUser
                    user?.let {
                        if (user.last_authorizer_user_id == cancelBean.authorizer_user_id.toString()
                                && user.last_region_id == cancelBean.rid.toString()) {
                            user.last_region_id = 1.toString()
                            user.last_authorizer_user_id = user.id.toString()
                            DBUtils.deleteAllData()
                            AccountModel.initDatBase(it)
                            //更新last—region-id
                            DBUtils.saveUser(user)
                            Log.e("zclbaseActivity", "zcl******" + DBUtils.lastUser)
                            SyncDataPutOrGetUtils.syncGetDataStart(user, syncCallbackGet)
                        }
                    }
                    makeCodeDialog(2, cancelBean.authorizer_user_phone, "", cancelBean.region_name)//2代表解除授权信息type
                    LogUtils.e("zcl_baseMe___________收到取消消息")
                }
                Constant.PARSE_CODE -> {
                    val codeBean: QrCodeTopicMsg = intent.getSerializableExtra(Constant.PARSE_CODE) as QrCodeTopicMsg
                    Log.e(TAG, "zcl_baseMe___________收到取消消息***解析二维码***")
                    makeCodeDialog(codeBean.type, codeBean.ref_user_phone, codeBean.account, "")
                }
            }
        }
    }

    open fun loginOutMethod() {
        checkNetworkAndSync(this@TelinkBaseActivity)
    }


    /**
     * 报错log打印
     */
    fun onErrorReportNormal(info: ErrorReportInfo) {

        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        LogUtils.d("蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        LogUtils.d("无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        LogUtils.d("未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        LogUtils.d("未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        LogUtils.d("未建立物理连接")
                    }
                }
            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        LogUtils.d("value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        LogUtils.d("read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        LogUtils.d("write login data 没有收到response")
                    }
                }
            }
        }
    }
}


