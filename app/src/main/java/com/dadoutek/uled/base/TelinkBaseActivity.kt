package com.dadoutek.uled.base

import android.Manifest
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
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import cn.smssdk.SMSSDK
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.stomp.StompManager
import com.dadoutek.uled.stomp.model.QrCodeTopicMsg
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.ErrorReportInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class TelinkBaseActivity : AppCompatActivity() {
    private var mConnectDisposable: Disposable? = null
    private var changeRecevicer: ChangeRecevicer? = null
    private var mStompListener: Disposable? = null
    protected var isRuning: Boolean = false
    private var authorStompClient: Disposable? = null
    private var pop: PopupWindow? = null
    private var popView: View? = null
    private var codeWarmDialog: AlertDialog? = null
    private var singleLogin: AlertDialog? = null
    private var payload: String? = null
    var stompLifecycleDisposable: Disposable? = null
    var singleLoginTopicDisposable: Disposable? = null
    var codeStompClient: Disposable? = null
    private lateinit var stompRecevice: StompReceiver
    private var locationServiceDialog: android.support.v7.app.AlertDialog? = null
    private lateinit var mStompManager: StompManager
    private var loadDialog: Dialog? = null
    private var mApplication: TelinkLightApplication? = null
    private var mScanDisposal: Disposable? = null
    public var disposableConnectTimer: Disposable? = null
    var isScanning = false


    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mApplication = this.application as TelinkLightApplication
        enableConnectionStatusListener()    //尽早注册监听
        initOnLayoutListener()//加载view监听
        makeDialogAndPop()
        initStompReceiver()
        initChangeRecevicer()
    }

    private fun initChangeRecevicer() {
        changeRecevicer = ChangeRecevicer()
        val filter = IntentFilter()
        filter.addAction("STATUS_CHANGED")
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 2
//        registerReceiver(changeRecevicer, filter)
    }

    private fun makeDialogAndPop() {
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

    /**
     * 改变Toolbar上的图片和状态
     * @param isConnected       是否是连接状态
     */
    fun changeDisplayImgOnToolbar(isConnected: Boolean) {
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
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, statusChangedListener)
        LogUtils.d("enableConnectionStatusListener, current listeners = ${mApplication?.mEventBus?.mEventListeners}")
    }

    //关闭基类的连接状态变化监听
    fun disableConnectionStatusListener() {
        this.mApplication?.removeEventListener(DeviceEvent.STATUS_CHANGED, statusChangedListener)
    }

    private val statusChangedListener = EventListener<String?> { event ->
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
                LogUtils.v("zcl---baseactivity收到登入广播")
                GlobalScope.launch(Dispatchers.Main) {
                    if (!isScanning)
                        ToastUtils.showLong(getString(R.string.connect_success))
                    changeDisplayImgOnToolbar(true)
                }
                afterLogin()
                val connectDevice = this.mApplication?.connectDevice
                LogUtils.d("directly connection device meshAddr = ${connectDevice?.meshAddress}")
                if (!isScanning)
                    RecoverMeshDeviceUtil.addDevicesToDb(deviceInfo)//  如果已连接的设备不存在数据库，则创建。 主要针对扫描的界面和会连接的界面
            }
            LightAdapter.STATUS_LOGOUT -> {
//              LogUtils.v("zcl---baseactivity收到登出广播")
                GlobalScope.launch(Dispatchers.Main) {
                    changeDisplayImgOnToolbar(false)
                    afterLoginOut()
                }
            }

            LightAdapter.STATUS_CONNECTING -> {
                if (!isScanning)
                    ToastUtils.showLong(R.string.connecting_please_wait)
            }
        }
    }


    open fun afterLoginOut() {

    }

    open fun afterLogin() {

    }

    override fun onResume() {
        super.onResume()
        Constant.isTelBase = true
        val lightService: TelinkLightService? = TelinkLightService.Instance()
        if (LeBluetooth.getInstance().isSupport(applicationContext))
            LeBluetooth.getInstance().enable(applicationContext)

        if (LeBluetooth.getInstance().isEnabled) {
            if (TelinkLightApplication.getApp().connectDevice != null/*lightService?.isLogin == true*/) {
                changeDisplayImgOnToolbar(true)
            } else {
                changeDisplayImgOnToolbar(false)
            }
        } else {
            changeDisplayImgOnToolbar(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disableConnectionStatusListener()
        mConnectDisposable?.dispose()
        isRuning = false
        loadDialog?.dismiss()
        unregisterReceiver(stompRecevice)
        SMSSDK.unregisterAllEventHandler()
    }

    open fun initOnLayoutListener() {
        var view = window.decorView
        var viewTreeObserver = view.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener {
            isRuning = true
            view.viewTreeObserver.removeOnGlobalLayoutListener {}
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
            loadDialog = Dialog(this, R.style.FullHeightDialog)
        }
        //loadDialog没显示才把它显示出来
        if (!loadDialog!!.isShowing && !this@TelinkBaseActivity.isFinishing) {
            loadDialog!!.setCancelable(false)
            loadDialog!!.setCanceledOnTouchOutside(false)
            loadDialog!!.setContentView(layout)
            if (this.isRuning && !this.isDestroyed) {
                loadDialog!!.show()
            }
        }
    }

    fun hideLoadingDialog() {
        if (loadDialog != null && !this@TelinkBaseActivity.isFinishing) {
            loadDialog!!.dismiss()
        }
    }

    fun compileExChar(str: String): Boolean {
        return StringUtils.compileExChar(str)
    }


    override fun onPause() {
        super.onPause()
        mConnectDisposable?.dispose()
        isRuning = false
    }


    /**
     * 检查网络上传数据
     * 如果没有网络，则弹出网络设置对话框
     */
    protected fun checkNetworkAndSync(activity: Activity?) {
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
            val b = this@TelinkBaseActivity.isFinishing
            val showing = singleLogin?.isShowing
            if (!b && showing != null && !showing!!) {
                singleLogin!!.show()
            }
        }

        override fun error(msg: String) {
            hideLoadingDialog()
            if (msg != null && msg != "null")
                ToastUtils.showLong(msg)
        }
    }

    //重启app并杀死原进程
    open fun restartApplication() {
        TelinkApplication.getInstance().removeEventListeners()
        SharedPreferencesHelper.putBoolean(this, Constant.IS_LOGIN, false)
        TelinkLightApplication.getApp().releseStomp()
        AppUtils.relaunchApp()
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
                    if (type == 0)
                        restartApplication()
                }
                notifyWSData()

                initOnLayoutListener()

                LogUtils.v("zcl-------------是否显示${!this@TelinkBaseActivity.isFinishing}--------------${!pop!!.isShowing}")
                if (!this@TelinkBaseActivity.isFinishing && !pop!!.isShowing && window.decorView != null)//isTelBase为false所以不显示 明天看为什么
                    pop!!.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
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
                    val extra = intent.getSerializableExtra(Constant.CANCEL_CODE)
                    var cancelBean: CancelAuthorMsg? = null
                    if (extra != null)
                        cancelBean = extra as CancelAuthorMsg

                    val user = DBUtils.lastUser
                    user?.let {
                        if (user.last_authorizer_user_id == cancelBean?.authorizer_user_id.toString()
                                && user.last_region_id == cancelBean?.rid.toString()) {
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
                    cancelBean?.let { makeCodeDialog(2, it.authorizer_user_phone, "", cancelBean.region_name) }//2代表解除授权信息type
                    LogUtils.e("zcl_baseMe___________收到取消消息")
                }
                Constant.PARSE_CODE -> {
                    val codeBean: QrCodeTopicMsg = intent.getSerializableExtra(Constant.PARSE_CODE) as QrCodeTopicMsg
                    LogUtils.e("zcl_baseMe___________收到消息***解析二维码***")
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
//                        showToast(getString(R.string.close_bluetooth))
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        LogUtils.d("无法收到广播包以及响应包")
//                        showToast("无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        LogUtils.d("未扫到目标设备")
//                        showToast("未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        LogUtils.d("未读到att表")
//                        showToast("未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        LogUtils.d("未建立物理连接")
//                        showToast("未建立物理连接")
                    }
                }
            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        LogUtils.d("value check失败： 密码错误")
//                        showToast("value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        LogUtils.d("read login data 没有收到response")
//                        showToast("read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        LogUtils.d("write login data 没有收到response")
//                        showToast("write login data 没有收到response")
                    }
                }
            }
        }
    }


    fun showOpenLocationServiceDialog() {
        val builder = android.support.v7.app.AlertDialog.Builder(this)
        builder.setTitle(R.string.open_location_service)
        builder.setNegativeButton(getString(android.R.string.ok)) { _, _ ->
            BleUtils.jumpLocationSetting()
        }
        locationServiceDialog = builder.create()
        locationServiceDialog?.setCancelable(false)
        locationServiceDialog?.show()
    }

    fun hideLocationServiceDialog() {
        locationServiceDialog?.hide()
    }


    fun connect(meshAddress: Int = 0, fastestMode: Boolean = false, macAddress: String? = null, meshName: String? = DBUtils.lastUser?.controlMeshName,
                meshPwd: String? = NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16),
                retryTimes: Long = 1, deviceTypes: List<Int>? = null): Observable<DeviceInfo>? {
        mConnectDisposable == null //代表这是第一次执行
        // !TelinkLightService.Instance().isLogin 代表只有没连接的时候，才会往下跑，走连接的流程。
        return if (mConnectDisposable == null && TelinkLightService.Instance()?.isLogin == false) {
            return Commander.connect(meshAddress, fastestMode, macAddress, meshName, meshPwd, retryTimes, deviceTypes)
                    ?.doOnSubscribe {
                        mConnectDisposable = it
                    }
                    ?.doFinally {
                        mConnectDisposable = null
                    }
                    ?.doOnError {
                        TelinkLightService.Instance().idleMode(false)
                    }

        } else {
            LogUtils.d("autoConnect Commander = ${mConnectDisposable?.isDisposed}, isLogin = ${TelinkLightService.Instance()?.isLogin}")
            Observable.create {
                it.onNext(TelinkLightApplication.getApp().connectDevice)
            }
        }

    }

    /**
     * 自动重连
     */
    @SuppressLint("CheckResult")
    @Deprecated("use connect()")
    fun connectOld(macAddr: String?) {
        //如果支持蓝牙就打开蓝牙
        if (LeBluetooth.getInstance().isSupport(applicationContext))
            LeBluetooth.getInstance().enable(applicationContext)    //如果没打开蓝牙，就提示用户打开

        //如果位置服务没打开，则提示用户打开位置服务，bleScan必须
        if (!BleUtils.isLocationEnable(this)) {
            showOpenLocationServiceDialog()
        } else {
            hideLocationServiceDialog()
            TelinkLightService.Instance().idleMode(true)
            Thread.sleep(200)
            RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if (TelinkLightApplication.getApp().connectDevice == null/*!TelinkLightService.Instance().isLogin*/) {
                            showLoadingDialog(getString(R.string.please_wait))

                            val meshName = DBUtils.lastUser!!.controlMeshName

                            GlobalScope.launch {
                                //自动重连参数
                                val connectParams = Parameters.createAutoConnectParameters()
                                connectParams.setMeshName(meshName)
                                connectParams.setPassword(NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16))
                                connectParams.autoEnableNotification(true)
                                connectParams.setConnectMac(macAddr)
                                LogUtils.v("autoconnect  meshName = $meshName   meshPwd = ${NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16)}   macAddress = ${macAddr}")

                                //连接，如断开会自动重连
                                TelinkLightService.Instance().autoConnect(connectParams)
                            }
                        }
                    }, { LogUtils.d(it) })
        }
    }


    inner class ChangeRecevicer : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val deviceInfo = intent?.getParcelableExtra("STATUS_CHANGED") as DeviceInfo
            LogUtils.e("zcl获取通知$deviceInfo")
            when (deviceInfo.status) {
                LightAdapter.STATUS_LOGIN -> {
                    if (!isScanning)
                        ToastUtils.showLong(getString(R.string.connect_success))
                    changeDisplayImgOnToolbar(true)

                }
                LightAdapter.STATUS_LOGOUT -> {
                    changeDisplayImgOnToolbar(false)
                }

            }
        }
    }

    fun setScanningMode(isScanning: Boolean) {
        this.isScanning = isScanning
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (loadDialog != null && loadDialog!!.isShowing)
            hideLoadingDialog()
        else
            finish()
    }

}

