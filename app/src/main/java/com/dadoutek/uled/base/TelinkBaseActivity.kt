package com.dadoutek.uled.base

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import cn.smssdk.SMSSDK
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.bean.GwStompBean
import com.dadoutek.uled.gateway.bean.WeekBean
import com.dadoutek.uled.group.GroupOTAListActivity
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.group.TypeListAdapter
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DBUtils.lastUser
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbSensor
import com.dadoutek.uled.model.dbModel.DbSwitch
import com.dadoutek.uled.model.httpModel.AccountModel
import com.dadoutek.uled.model.httpModel.UpdateModel
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.mqtt.IGetMessageCallBack
import com.dadoutek.uled.mqtt.MqttService
import com.dadoutek.uled.mqtt.MyServiceConnection
import com.dadoutek.uled.network.*
import com.dadoutek.uled.othersview.InstructionsForUsActivity
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.router.RouterOtaActivity
import com.dadoutek.uled.router.bean.*
import com.dadoutek.uled.stomp.MqttBodyBean
import com.dadoutek.uled.stomp.StompManager
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.google.gson.Gson
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
import com.telink.util.MeshUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.greendao.DbUtils
import org.jetbrains.anko.singleLine
import org.jetbrains.anko.startActivity
import java.util.concurrent.TimeUnit

///TelinkLog 打印

abstract class TelinkBaseActivity : AppCompatActivity(), IGetMessageCallBack {
    private var dispos: Disposable? = null
    private var receiver: BluetoothListenerReceiver? = null
    var disposableTimer: Disposable? = null
    open var currentShowGroupSetPage = true
    var isScanningJM: Boolean = false
    open var lastTime: Long = 0
    private var isShow: Boolean = false
    private var showTime: Long = 0
    private var serviceConnection: MyServiceConnection? = null
    private var viewInstall: View? = null
    private var installTitleTv: TextView? = null
    private var netWorkChangReceiver: NetWorkChangReceiver? = null
    var mConnectDisposable: Disposable? = null
    private var changeRecevicer: ChangeRecevicer? = null
    private var mStompListener: Disposable? = null
    private var authorStompClient: Disposable? = null
    lateinit var pop: PopupWindow
    var popView: View? = null
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
    var disposableConnectTimer: Disposable? = null
    var isScanning = false
    private var upDateTimer: Disposable? = null
    private lateinit var dialog: Dialog
    private var adapterType: TypeListAdapter? = null
    private var list: MutableList<String>? = null
    private var groupType: Long = 0L
    private var dialogGroupName: TextView? = null
    private var dialogGroupType: TextView? = null
    open lateinit var popMain: PopupWindow
    private val SHOW_LOADING_DIALOG_DELAY: Long = 300 //ms
    open var installDialog: AlertDialog? = null
    var isRgbClick = false
    var clickRgb: Boolean = false
    var installId = 0
    var stepOneText: TextView? = null
    var stepTwoText: TextView? = null
    var stepThreeText: TextView? = null
    var switchStepOne: TextView? = null
    var switchStepTwo: TextView? = null
    var swicthStepThree: TextView? = null
    private var installHelpe: TextView? = null
    val INSTALL_NORMAL_LIGHT = 0
    val INSTALL_RGB_LIGHT = 1
    val INSTALL_SWITCH = 2
    val INSTALL_SENSOR = 3
    val INSTALL_CURTAIN = 4
    val INSTALL_CONNECTOR = 5
    val INSTALL_GATEWAY = 6
    val INSTALL_ROUTER = 7
    var isGuide: Boolean = false
    lateinit var hinitOne: TextView
    lateinit var popFinish: PopupWindow
    lateinit var cancelf: Button
    lateinit var confirmf: Button
    var isEdite: Boolean = false
    var type: Int? = null
    var showDialogHardDelete: AlertDialog? = null

    var renameCancel: TextView? = null
    var renameConfirm: TextView? = null
    var renameEt: EditText? = null
    var popReNameView: View? = null
    lateinit var renameDialog: Dialog
    var disposableRouteTimer: Disposable? = null
    var mHandlerBase = android.os.Handler()

    @SuppressLint("ShowToast", "CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mApplication = this.application as TelinkLightApplication
        isScanningJM = false
        enableConnectionStatusListener()    //尽早注册监听
        //注册网络状态监听广播
        netWorkChangReceiver = NetWorkChangReceiver()
        var filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(netWorkChangReceiver, filter)
        initOnLayoutListener()//加载view监听
        makeDialogAndPop()
        makeInstallView()
        makeRenamePopuwindow()
        makeStopScanPop()
        makeDialog()
        initStompReceiver()
        initChangeRecevicer()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            dispos = RxPermissions(this).request(Manifest.permission.READ_PHONE_STATE).subscribe({}, {})
        }
//        if (TelinkLightApplication.getApp().mStompManager?.mStompClient?.isConnected != true)
//            TelinkLightApplication.getApp().initStompClient()
        receiver = BluetoothListenerReceiver()
        registerReceiver(receiver, makeFilter())
        serviceConnection?.mqttService?.init()
        bindService()
    }

    open fun makeFilter(): IntentFilter? {
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        return filter
    }

    open fun isZeroOrHundred(ws: Int): Int {
        var ws1 = ws
        ws1 = when {
            ws1 <= 1 -> 1
            ws1 >= 100 -> 100
            else -> ws1
        }
        return ws1
    }

    open fun getWeek(str: String): Int {
        var week = 0b00000000
        when (str) {
            getString(R.string.only_one) -> {
                week = 0b00000000
                return week
            }
            getString(R.string.every_day) -> {
                week = 0b01111111
                return week
            }
            else -> {
                val split = str.split(",").toMutableList()
                for (s in split) {
                    when (s) {//bit位 0-6 周日-周六 7代表当天 0代表仅一次
                        getString(R.string.sunday) -> week = week or 0b00000001
                        getString(R.string.monday) -> week = week or 0b00000010
                        getString(R.string.tuesday) -> week = week or 0b00000100
                        getString(R.string.wednesday) -> week = week or 0b00001000
                        getString(R.string.thursday) -> week = week or 0b00010000
                        getString(R.string.friday) -> week = week or 0b00100000
                        getString(R.string.saturday) -> week = week or 0b01000000
                    }
                }
                return week
            }
        }
    }


    fun getOtaVersion(version: String?): String {
        version?.let {
            val split = version.split("-")
            if (split.size >= 2) {
                val versionNum = numberCharat(split[1])
                LogUtils.v("zcl比较版本号-------$version------${TelinkLightApplication.mapBin[split[0]] ?: 0}-----${versionNum}")
                val keys = TelinkLightApplication.mapBin.keys
                val contains = keys.contains(split[0])
                return when {
                    contains -> {
                        val i = TelinkLightApplication.mapBin[split[0]].toString()
                        val sb = StringBuilder()
                        for (j in i.indices) {
                            sb.append(i[j])

                            if (j != i.length - 1)
                                sb.append(".")
                        }
                        split[0] + "-" + sb.toString()
                    }
                    else -> getString(R.string.no_get_version)
                }
            }
        }
        return getString(R.string.no_get_version)
    }

    fun isSuportOta(version: String?): Boolean {
        version?.let {
            val split = version.split("-")
            if (split.size >= 2) {
                val versionNum = numberCharat(split[1])
                LogUtils.v("zcl比较版本号-------$version------${TelinkLightApplication.mapBin[split[0]] ?: 0}-----${versionNum}")
                val keys = TelinkLightApplication.mapBin.keys
                return keys.contains(split[0])
            }
        }
        return false
    }

    fun isMostNew(version: String?): Boolean {
        version?.let {
            val split = version?.split("-")
            if (split.size >= 2) {
                val versionNum = numberCharat(split[1])
                LogUtils.v("zcl比较版本号-------$version------${TelinkLightApplication.mapBin[split[0]] ?: 0}-----${versionNum}")
                if (!TextUtils.isEmpty(versionNum))
                    return versionNum.toInt() >= TelinkLightApplication.mapBin[split[0]] ?: 0
            }
        }
        return false
    }

    open fun numberCharat(string: String): String {
        val sBuffer = StringBuffer()
        val replace = string.replace(".", "", true)
        val str = replace.replace("[a-zA-Z]".toRegex(), "")
        str.forEach { i ->
            if (!(48 > i.toInt() || i.toInt() > 57)) {
                sBuffer.append(i)
            }
        }
        return sBuffer.toString()
    }

    private fun initChangeRecevicer() {
        changeRecevicer = ChangeRecevicer()
        val filter = IntentFilter()
        filter.addAction("STATUS_CHANGED")
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 2
//        registerReceiver(changeRecevicer, filter)
    }

    private fun startTimerUpdate() {
        upDateTimer?.dispose()
        upDateTimer = Observable.interval(0, 5, TimeUnit.SECONDS).subscribe {
            if (netWorkCheck(this) && !Constant.IS_ROUTE_MODE)
                CoroutineScope(Dispatchers.IO).launch {
                    SyncDataPutOrGetUtils.syncPutDataStart(this@TelinkBaseActivity, object : SyncCallback {
                        override fun start() {}
                        override fun complete() {}
                        override fun error(msg: String?) {}
                    })
                }
        }
    }

    // 网络连接判断
    open fun netWorkCheck(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info: NetworkInfo? = cm.activeNetworkInfo
        return info?.isConnected ?: false
    }


    fun stopTimerUpdate() {
        upDateTimer?.dispose()
        upDateTimer = null
    }

    /**
     * 自动重连
     */
    fun autoConnectAll() {
        if (Constant.IS_ROUTE_MODE) return
        val deviceTypes = mutableListOf(DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB)
        val size = DBUtils.getAllCurtains().size + DBUtils.allLight.size + DBUtils.allRely.size
        if (size > 0) {
            if (TelinkLightService.Instance()?.isLogin == false && TelinkLightApplication.getApp().connectDevice == null && TelinkLightApplication.isLoginAccount) {
                TelinkLightService.Instance()?.disconnect()
                ToastUtils.showLong(getString(R.string.connecting_tip))
                Thread.sleep(800)
                mConnectDisposable?.dispose()
                mConnectDisposable = connect(deviceTypes = deviceTypes, retryTimes = 3, fastestMode = true)
                        ?.subscribe({
                            LogUtils.d("connection success")
                        }, {
                            LogUtils.d("connect failed")
                        })

            }
        }
    }

    private fun makeDialogAndPop() {
        singleLogin = AlertDialog.Builder(this)
                .setTitle(R.string.other_device_login)
                .setMessage(getString(R.string.single_login_warm))
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
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

    private fun makeDialog() {
        dialog = Dialog(this@TelinkBaseActivity)
        list = mutableListOf(getString(R.string.normal_light), getString(R.string.rgb_light), getString(R.string.curtain), getString(R.string.relay))
        adapterType = TypeListAdapter(R.layout.item_group, list!!)

        val popView = View.inflate(this@TelinkBaseActivity, R.layout.dialog_add_group, null)
        popMain = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        popMain.isFocusable = true // 设置PopupWindow可获得焦点
        popMain.isTouchable = true // 设置PopupWindow可触摸补充：
        popMain.isOutsideTouchable = false

        val recyclerView = popView.findViewById<RecyclerView>(R.id.pop_recycle)
        recyclerView.layoutManager = LinearLayoutManager(this@TelinkBaseActivity, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapterType

        adapterType?.bindToRecyclerView(recyclerView)

        val dialogGroupTypeArrow = popView.findViewById<ImageView>(R.id.dialog_group_type_arrow)
        val dialogGroupCancel = popView.findViewById<TextView>(R.id.dialog_group_cancel)
        val dialogGroupOk = popView.findViewById<TextView>(R.id.dialog_group_ok)
        dialogGroupType = popView.findViewById<TextView>(R.id.dialog_group_type)
        dialogGroupName = popView.findViewById<TextView>(R.id.dialog_group_name)

        dialogGroupTypeArrow.setOnClickListener {
            if (recyclerView.visibility == View.GONE)
                recyclerView.visibility = View.VISIBLE
            else
                recyclerView.visibility = View.GONE

        }
        dialogGroupType?.setOnClickListener {
            if (recyclerView.visibility == View.GONE)
                recyclerView.visibility = View.VISIBLE
            else
                recyclerView.visibility = View.GONE

        }
        dialogGroupCancel.setOnClickListener { PopUtil.dismiss(popMain) }
        dialogGroupOk.setOnClickListener {
            addNewTypeGroup()
        }

        adapterType?.setOnItemClickListener { _, _, position ->
            dialogGroupType?.text = list!![position]
            recyclerView.visibility = View.GONE
            when (position) {
                0 -> groupType = Constant.DEVICE_TYPE_LIGHT_NORMAL
                1 -> groupType = Constant.DEVICE_TYPE_LIGHT_RGB
                2 -> groupType = Constant.DEVICE_TYPE_CURTAIN
                3 -> groupType = Constant.DEVICE_TYPE_CONNECTOR
            }
        }
        popMain.setOnDismissListener {
            recyclerView.visibility = View.GONE
            dialogGroupType?.text = getString(R.string.not_type)
            dialogGroupName?.text = ""
            groupType = 0
        }
    }

    private fun addNewTypeGroup() {
        // 获取输入框的内容
        if (StringUtils.compileExChar(dialogGroupName?.text.toString().trim { it <= ' ' })) {
            ToastUtils.showLong(getString(R.string.rename_tip_check))
        } else {
            if (groupType == 0L) {
                ToastUtils.showLong(getString(R.string.select_type))
            } else {
                //往DB里添加组数据
                DBUtils.addNewGroupWithType(dialogGroupName?.text.toString().trim { it <= ' ' }, groupType)
                refreshGroupData()
                PopUtil.dismiss(popMain)
            }
        }
    }

    open fun refreshGroupData() {

    }

    //增加全局监听蓝牙开启状态
    private fun showOpenBluetoothDialog(context: Context) {
        val dialogTip = AlertDialog.Builder(context)
        dialogTip.setMessage(R.string.openBluetooth)
        dialogTip.setPositiveButton(android.R.string.ok) { _, _ ->
            LeBluetooth.getInstance().enable(applicationContext)
        }
        dialogTip.setCancelable(true)
        dialogTip.create().show()
    }

    /**
     * 改变Toolbar上的图片和状态
     * @param isConnected       是否是连接状态
     */
    open fun changeDisplayImgOnToolbar(isConnected: Boolean) {
        LogUtils.v("zcl--获取状态-------${Constant.IS_ROUTE_MODE}--------${SharedPreferencesHelper.getBoolean(this, Constant.ROUTE_MODE, false)}-")
        if (Constant.IS_ROUTE_MODE)
            toolbar?.findViewById<ImageView>(R.id.image_bluetooth)?.setImageResource(R.drawable.icon_cloud)
        else {
            if (isConnected) {
                toolbar?.findViewById<ImageView>(R.id.image_bluetooth)?.setImageResource(R.drawable.icon_bluetooth)
                toolbar?.findViewById<ImageView>(R.id.image_bluetooth)?.isEnabled = false
            } else {
                toolbar?.findViewById<ImageView>(R.id.image_bluetooth)?.setImageResource(R.drawable.bluetooth_no)
                toolbar?.findViewById<ImageView>(R.id.image_bluetooth)?.isEnabled = true
                toolbar?.findViewById<ImageView>(R.id.image_bluetooth)?.setOnClickListener {
                    val dialog = BluetoothConnectionFailedDialog(this, R.style.Dialog)
                    dialog.show()
                }
            }
        }

    }

    //打开基类的连接状态变化监听
    private fun enableConnectionStatusListener() { //todo 传感器上传下拉  外部不退出
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, statusChangedListener)
        //LogUtils.d("enableConnectionStatusListener, current listeners = ${mApplication?.mEventBus?.mEventListeners}")
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
                //if (!isScanning)
                // RecoverMeshDeviceUtil.addDevicesToDb(deviceInfo)//  如果已连接的设备不存在数据库，则创建。 主要针对扫描的界面和会连接的界面
            }
            LightAdapter.STATUS_LOGOUT -> {
                LogUtils.v("zcl---baseactivity收到登出广播")
                GlobalScope.launch(Dispatchers.Main) {
                    changeDisplayImgOnToolbar(false)
                    afterLoginOut()
                }
            }

            LightAdapter.STATUS_CONNECTING -> {
                if (!isScanning)
                    ToastUtils.showLong(R.string.connecting_tip)
            }
        }
    }


    open fun afterLoginOut() {

    }

    open fun afterLogin() {

    }

    override fun onResume() {
        super.onResume()
        isShow = true
        if (!isScanningJM) {
            unbindSe()
            bindService()
        }

        LogUtils.v("zcl-----------前台后台-resum------${isForeground(this)}")
        checkVersionAvailable()

        if (!LeBluetooth.getInstance().enable(applicationContext) && !Constant.IS_ROUTE_MODE)
            TmtUtils.midToastLong(this, getString(R.string.open_blutooth_tip))
        val lastUser = lastUser
        lastUser?.let {
            if (it.id.toString() == it.last_authorizer_user_id)//没有上传数据或者当前区域不是自己的区域
                if (!Constant.IS_ROUTE_MODE)
                    startTimerUpdate()
        }

        if (Constant.IS_ROUTE_MODE)
            toolbar?.findViewById<ImageView>(R.id.image_bluetooth)?.setImageResource(R.drawable.icon_cloud)

        if (LeBluetooth.getInstance().isEnabled) {
            if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightService.Instance().isLogin) {
                changeDisplayImgOnToolbar(true)
            } else {
                changeDisplayImgOnToolbar(false)
            }
        } else {
            changeDisplayImgOnToolbar(false)
        }
        //autoConnectAll()
    }

    private fun checkVersionAvailable() {
        var version = packageName(this)
        if (netWorkCheck(this))
            UpdateModel.run {
                isVersionAvailable(0, version)
                        .subscribe({
                            if (!it.isUsable) {
                                loginOutMethod()
                            }
                            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), "isShowDot", it.isUsable)
                        }, {
                            //ToastUtils.showLong(R.string.get_server_version_fail)
                        })
            }
    }

    open fun packageName(context: Context): String {
        val manager = context.packageManager
        var name: String? = null
        try {
            val info = manager.getPackageInfo(context.packageName, 0)
            name = info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return name!!
    }

    override fun onDestroy() {
        super.onDestroy()
        disableConnectionStatusListener()
        unregisterReceiver(receiver)
        unbindSe()
        dispos?.dispose()
        mHandlerBase.removeCallbacksAndMessages(null)
        mConnectDisposable?.dispose()
        disposableTimer?.dispose()
        loadDialog?.dismiss()
        unregisterReceiver(stompRecevice)
        unregisterReceiver(netWorkChangReceiver)
        SMSSDK.unregisterAllEventHandler()
        stopTimerUpdate()

    }

    fun bindService() {
        serviceConnection = MyServiceConnection()
        serviceConnection?.setIGetMessageCallBack(this)
        val intent = Intent(this, MqttService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    open fun unbindSe() { //解绑服务
        try {
            serviceConnection?.let {
                unbindService(it)
            }
        } catch (e: java.lang.Exception) {
            LogUtils.v("zcl-----------${e.message}-------${e.printStackTrace()}")
        }
    }

    open fun initOnLayoutListener() {
        var view = window.decorView
        var viewTreeObserver = view.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener {
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

        if (loadDialog == null)
            loadDialog = Dialog(this, R.style.FullHeightDialog)

        if (loadDialog!!.isShowing)
            return

        //loadDialog没显示才把它显示出来
        if (!this.isFinishing && !loadDialog!!.isShowing) {
            loadDialog!!.setCancelable(false)
            loadDialog!!.setCanceledOnTouchOutside(false)
            loadDialog!!.setContentView(layout)
            if (!this.isDestroyed) {
                runOnUiThread {
                    showTime = System.currentTimeMillis()
                    loadDialog!!.show()
                }
            }
        }
    }

    fun hideLoadingDialog() {
        if (!this@TelinkBaseActivity.isFinishing && loadDialog != null && loadDialog!!.isShowing) {
            //if (System.currentTimeMillis() - showTime > 2000)
            runOnUiThread { loadDialog?.dismiss() }
            /* else
                 Observable.timer(2, TimeUnit.SECONDS).subscribe {
                     runOnUiThread { loadDialog?.dismiss() }
                 }*/
        }
    }

    fun compileExChar(str: String): Boolean {
        return StringUtils.compileExChar(str)
    }


    override fun onPause() {
        super.onPause()
        stopTimerUpdate()
        if (!isScanningJM) {
            unbindSe()
        }
        isShow = false
        showDialogHardDelete?.dismiss()

        disposableTimer?.dispose()
        mConnectDisposable?.dispose()
    }

    override fun onStop() {
        super.onStop()
        val foreground = isForeground(this)
        if (!foreground) {
            unbindSe()
            bindService()
        }
    }

    /*判断应用是否在前台*/
    @SuppressLint("ServiceCast")
    open fun isForeground(context: Context): Boolean {
        val am: ActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val tasks: List<ActivityManager.RunningTaskInfo> = am.getRunningTasks(1)
        if (tasks.isNotEmpty()) {
            val topActivity: ComponentName = tasks[0].topActivity
            if (topActivity.packageName == context.packageName) {
                return true
            }
        }
        return false
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
            SyncDataPutOrGetUtils.syncPutDataStart(activity, syncCallbackUp)
        }
    }

    open var syncCallbackGet: SyncCallback = object : SyncCallback {
        override fun start() {}
        override fun complete() {}
        @SuppressLint("CheckResult")
        override fun error(msg: String) {}
    }

    /**
     * 上传回调
     */
    open var syncCallbackUp: SyncCallback = object : SyncCallback {
        override fun start() {
            showLoadingDialog(getString(R.string.tip_start_sync))
        }

        override fun complete() {
            hideLoadingDialog()
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
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), Constant.IS_LOGIN, false)
        AppUtils.relaunchApp()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n", "StringFormatInvalid")
    private fun makeCodeDialog(type: Int, phone: Any, rid: Any, regionName: Any, lastRegionId: Int? = lastUser?.last_region_id?.toInt()) {
        //移交码为0授权码为1
        var title: String? = null
        var recever: String? = null

        when (type) {
            -1 -> {
                title = getString(R.string.transfer_region_success)
                recever = getString(R.string.recevicer)
            }
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
//                    if (type == 0)
//                        restartApplication()
                    PopUtil.dismiss(pop)
                    when (type) {
                        -1, 0, 2 -> { //移交的区域已被接收 账号已被接收 解除了区域%1$s的授权
                            if (lastRegionId == rid || rid == 1) {//如果正在使用或者是区域一则退出
                                //DBUtils.deleteAllData()//删除数据
                                restartApplication()
                                ToastUtils.showLong(getString(R.string.cancel_authorization))
                            }
                        }
                    }
                }

                initOnLayoutListener()
                LogUtils.v("zcl---------判断tel---${!this@TelinkBaseActivity.isFinishing}----- && --${!pop!!.isShowing} ---&&-- ${true}")
                try {
                    if (!this@TelinkBaseActivity.isFinishing && !pop!!.isShowing && isShow)
                        pop!!.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
                } catch (e: Exception) {
                    LogUtils.v("zcl弹框出现问题${e.localizedMessage}")
                }
                notifyWSData(type, rid)
            }
        }
    }


    open fun notifyWSData(type: Int, rid: Any) {

    }

    private fun initStompReceiver() {
        stompRecevice = StompReceiver()
        val filter = IntentFilter()
        filter.addAction(Constant.LOGIN_OUT)
        //filter.addAction(Constant.GW_COMMEND_CODE)
        //filter.addAction(Constant.CANCEL_CODE)
        //filter.addAction(Constant.PARSE_CODE)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
        registerReceiver(stompRecevice, filter)
    }

    inner class StompReceiver : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context?, intent: Intent?) {
            val msg = intent?.getStringExtra(Constant.LOGIN_OUT) ?: ""
            val cmdBean: CmdBodyBean = Gson().fromJson(msg, CmdBodyBean::class.java)
            //var jsonObject = JSONObject(msg)
            LogUtils.v("zcl------------------收到信息长连接---cmdbean$cmdBean")
            when (cmdBean.cmd) {
                Cmd.singleLogin, Cmd.parseQR, Cmd.unbindRegion, Cmd.gwStatus, Cmd.gwCreateCallback, Cmd.gwControlCallback -> {
                    val codeBean = Gson().fromJson(msg, MqttBodyBean::class.java)
                    when (cmdBean.cmd) {
                        Cmd.singleLogin -> singleDialog(codeBean)//单点登录
                        Cmd.parseQR -> makeCodeDialog(codeBean.type, codeBean.ref_user_phone, codeBean.region_name, codeBean.rid)//推送二维码扫描解析结果
                        Cmd.unbindRegion -> unbindDialog(codeBean)//解除授权信息
                        Cmd.gwStatus -> TelinkLightApplication.getApp().offLine = codeBean.state == 0//1上线了，0下线了
                        Cmd.gwCreateCallback -> if (codeBean.status == 0) receviedGwCmd2000(codeBean.ser_id)//下发标签结果
                        Cmd.gwControlCallback -> receviedGwCmd2500M(codeBean)//推送下发控制指令结果
                    }
                }
                Cmd.tzRouteInAccount -> {
                    val routerGroup = Gson().fromJson(msg, RouteInAccountBean::class.java)
                    routerAccessIn(routerGroup)
                }
                Cmd.tzRouteConfigWifi -> routerConfigWIFI(cmdBean)
                Cmd.tzRouteResetFactoryBySelf, Cmd.tzRouteResetFactoryBySelfphy -> {
                    SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
                    tzRouteResetFactoryBySelf(cmdBean)
                }
                Cmd.routeStartScann -> tzStartRouterScan(cmdBean)
                Cmd.routeScanDeviceInfo -> tzRouteDeviceNum(cmdBean)
                Cmd.routeStopScan -> tzRouteStopScan(cmdBean)

                Cmd.routeGroupingDevice -> {
                    val routerGroup = Gson().fromJson(msg, RouteGroupingOrDelBean::class.java)
                    tzRouterGroupResult(routerGroup)
                }
                Cmd.routeAddScenes -> {
                    val routerScene = Gson().fromJson(msg, RouteSceneBean::class.java)
                    tzRouterAddScene(routerScene)
                }

                Cmd.routeUpdateScenes -> tzRouteUpdateScene(cmdBean)

                Cmd.routeUpdateDeviceVersion -> {//版本号回调
                    val routerVersion = Gson().fromJson(msg, RouteGetVerBean::class.java)
                    tzRouterUpdateVersionRecevice(routerVersion)
                }

                Cmd.tzRouteGetVersioning -> tzRouteGetVersioningNum(cmdBean)
                Cmd.tzRouteGetVersionSend -> tzRouteSendVersioning(cmdBean)

                Cmd.tzRouteConfigDoubleSw -> {//配置双组开关
                    tzRouterConfigDoubleSwRecevice(cmdBean)
                }

                Cmd.tzRouteConfigSceneSw -> {//配置场景开关
                    tzRouterConfigSceneSwRecevice(cmdBean)
                }

                Cmd.tzRouteConfigNormalSw -> {//配置普通开关
                    tzRouterConfigNormalSwRecevice(cmdBean)
                }
                Cmd.tzRouteConfigEightSw -> {//配置普通开关
                    tzRouterConfigEightSwRecevice(cmdBean)
                }
                Cmd.tzRouteConfigEightSesonr -> {//配置普通开关
                    tzRouterConfigSensorRecevice(cmdBean)
                }

                Cmd.routeOTAing -> {
                    val routerOTAingNumBean = Gson().fromJson(msg, RouterOTAingNumBean::class.java)
                    tzRouterOTAingNumRecevice(routerOTAingNumBean)
                }
                Cmd.routeOTAFinish -> {
                    val routerOTAFinishBean = Gson().fromJson(msg, RouterOTAFinishBean::class.java)
                    tzRouterOTAStopRecevice(routerOTAFinishBean)
                }
                Cmd.tzRouteAddGradient, Cmd.tzRouteDelGradient, Cmd.tzRouteUpdateGradient -> tzRouterAddOrDelOrUpdateGradientRecevice(cmdBean)
                Cmd.tzRouteConnectOrDisConnectSwSe -> tzRouterConnectOrDisconnectSwSeRecevice(cmdBean)
                Cmd.routeDeleteGroup -> {
                    val routerGroup = Gson().fromJson(msg, RouteGroupingOrDelBean::class.java)
                    tzRouterDelGroupResult(routerGroup)
                }
                /**
                 * 控制指令下的通知
                 */
                Cmd.tzRouteOpenOrClose -> tzRouterOpenOrClose(cmdBean)
                Cmd.tzRouteContorlCurtain -> tzRouteContorlCurtaine(cmdBean)
                Cmd.tzRouteConfigRgb -> tzRouterConfigRGB(cmdBean)
                Cmd.tzRouteConfigBri -> tzRouterConfigBriOrTemp(cmdBean, 0)
                Cmd.tzRouteConfigTem -> tzRouterConfigBriOrTemp(cmdBean, 1)
                Cmd.tzRouteSysGradientApply -> tzRouterSysGradientApply(cmdBean)
                Cmd.tzRouteGradientApply -> tzRouterGradientApply(cmdBean)
                Cmd.tzRouteGradientStop -> tzRouterGradientStop(cmdBean)

                Cmd.tzRouteSlowUPSlowDownSw -> tzRouterSSSW(cmdBean, false)
                Cmd.tzRouteSlowUPSlowDownSpeed -> tzRouterSSSpeed(cmdBean)
                Cmd.tzRouteResetFactory -> tzRouterResetFactory(cmdBean)
                Cmd.tzRouteUserReset -> tzRouteUserReset(cmdBean)
                Cmd.tzRouteSafeLock -> tzRouterSafeLock(cmdBean)
                Cmd.tzRouteAddTimerScene -> tzRouterAddTimerScene(cmdBean)
                Cmd.tzRouteUpdateTimerScene -> tzRouterUpdateTimerScene(cmdBean)
                Cmd.tzRouteDelTimerScene -> tzRouterDelTimerScene(cmdBean)
                Cmd.tzRouteTimerSceneStatus -> tzRouterChangeTimerSceneStatus(cmdBean)
                Cmd.tzRouteConfigWhite -> tzRouterConfigWhite(cmdBean)
                Cmd.tzRouteConfigBri -> tzRouterConfigBriOrTemp(cmdBean, 0)
                Cmd.tzRouteConfigTem -> tzRouterConfigBriOrTemp(cmdBean, 1)
                Cmd.tzRouteOpenOrCloseSensor -> tzRouteOpenOrCloseSensor(cmdBean)
            }
/*   when (intent?.action) {
                Constant.GW_COMMEND_CODE -> {
                    val gwStompBean = intent.getSerializableExtra(Constant.GW_COMMEND_CODE) as GwStompBean
                    LogUtils.v("zcl-----------长连接接收网关数据-------$gwStompBean")
                    when (gwStompBean.cmd) {
                        700 -> TelinkLightApplication.getApp().offLine = false
                        701 -> TelinkLightApplication.getApp().offLine = true
                        2000 -> if (gwStompBean.status == 0) receviedGwCmd2000(gwStompBean.ser_id)
                        2500 -> receviedGwCmd2500(gwStompBean)
                    }

                }
                Constant.LOGIN_OUT -> {
                    LogUtils.e("zcl_baseMe___________收到登出消息")
                    loginOutMethod()
                }
                Constant.CANCEL_CODE -> {`
                    val extra = intent.getSerializableExtra(Constant.CANCEL_CODE)
                    var cancelBean: CancelAuthorMsg? = null
                    if (extra != null)
                        cancelBean = extra as CancelAuthorMsg

                    val user = DBUtils.lastUser
                    val lastRegionId = user?.last_region_id
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

                    cancelBean?.let { makeCodeDialog(2, it.authorizer_user_phone, cancelBean.rid, cancelBean.region_name, lastRegionId?.toInt()) }//2代表解除授权信息type
                    LogUtils.e("zcl_baseMe___________取消授权${cancelBean == null}")
                }
                Constant.PARSE_CODE -> {
                    val codeBean: QrCodeTopicMsg = intent.getSerializableExtra(Constant.PARSE_CODE) as QrCodeTopicMsg
//                    LogUtils.e("zcl_baseMe___________收到消息***解析二维码***")
                    makeCodeDialog(codeBean.type, codeBean.ref_user_phone, codeBean.rid.toString(), codeBean.region_name)
                }
            }*/
        }
    }

    open fun tzRouteOpenOrCloseSensor(cmdBean: CmdBodyBean) {}
    open fun tzRouteSendVersioning(cmdBean: CmdBodyBean) {}
    open fun tzRouteGetVersioningNum(cmdBean: CmdBodyBean) {}
    open fun tzRouterChangeTimerSceneStatus(cmdBean: CmdBodyBean) {}
    open fun tzRouterDelTimerScene(cmdBean: CmdBodyBean) {}
    open fun tzRouterUpdateTimerScene(cmdBean: CmdBodyBean) {}
    open fun tzRouterAddTimerScene(cmdBean: CmdBodyBean) {}
    open fun tzRouteContorlCurtaine(cmdBean: CmdBodyBean) {}
    open fun tzRouterConfigSensorRecevice(cmdBean: CmdBodyBean) {}
    open fun tzRouterConfigEightSwRecevice(cmdBean: CmdBodyBean) {}
    open fun tzRouterConfigSceneSwRecevice(cmdBean: CmdBodyBean) {}
    open fun tzRouterConfigNormalSwRecevice(cmdBean: CmdBodyBean) {}
    open fun tzRouterConfigDoubleSwRecevice(cmdBean: CmdBodyBean) {}
    open fun tzRouterSysGradientApply(cmdBean: CmdBodyBean) {}
    open fun tzRouterGradientStop(cmdBean: CmdBodyBean) {}
    open fun tzRouterGradientApply(cmdBean: CmdBodyBean) {}
    open fun tzRouterConfigRGB(cmdBean: CmdBodyBean) {}
    open fun tzRouterConfigWhite(cmdBean: CmdBodyBean) {}
    open fun tzRouterDelGroupResult(routerGroup: RouteGroupingOrDelBean?) {}
    open fun tzRouteResetFactoryBySelf(cmdBean: CmdBodyBean) {}
    open fun tzRouteUserReset(cmdBean: CmdBodyBean) {}
    open fun tzRouterSafeLock(cmdBean: CmdBodyBean) {}
    open fun tzRouterApplyScenes(cmdBean: CmdBodyBean) {}
    open fun tzRouterResetFactory(cmdBean: CmdBodyBean) {}
    open fun tzRouterSSSpeed(cmdBean: CmdBodyBean) {}
    open fun tzRouterSSSW(cmdBean: CmdBodyBean, b: Boolean) {}
    open fun tzRouterConfigBriOrTemp(cmdBean: CmdBodyBean, isBri: Int) {}
    open fun tzRouterOpenOrClose(cmdBean: CmdBodyBean) {}
    open fun tzRouteStopScan(cmdBean: CmdBodyBean) {}
    open fun tzRouterConnectOrDisconnectSwSeRecevice(cmdBean: CmdBodyBean) {}
    open fun tzRouterAddOrDelOrUpdateGradientRecevice(cmdBean: CmdBodyBean) {}
    open fun tzRouterOTAStopRecevice(routerOTAFinishBean: RouterOTAFinishBean?) {}
    open fun tzRouterOTAingNumRecevice(routerOTAingNumBean: RouterOTAingNumBean?) {}
    open fun tzRouterUpdateVersionRecevice(routerVersion: RouteGetVerBean?) {}
    open fun tzRouterAddScene(routerScene: RouteSceneBean?) {}
    open fun tzRouterGroupResult(routerGroup: RouteGroupingOrDelBean?) {}
    open fun tzRouteUpdateScene(cmdBodyBean: CmdBodyBean) {}
    open fun tzStartRouterScan(cmdBodyBean: CmdBodyBean) {}
    open fun routerConfigWIFI(cmdBody: CmdBodyBean) {}
    open fun routerAccessIn(cmdBody: RouteInAccountBean) {}
    open fun tzRouteDeviceNum(scanResultBean: CmdBodyBean) {}

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private fun unbindDialog(codeBean: MqttBodyBean) {
        val user = lastUser
        user?.let {
            if (it.last_authorizer_user_id == codeBean.authorizer_user_id.toString() && it.last_region_id == codeBean.rid.toString()
                    && it.id.toString() == it.last_authorizer_user_id) {//是自己的区域
                user.last_region_id = 1.toString()
                user.last_authorizer_user_id = user.id.toString()
                DBUtils.deleteAllData()
                AccountModel.initDatBase(it)
                //更新last—region-id
                DBUtils.saveUser(user)
                SyncDataPutOrGetUtils.syncGetDataStart(user, syncCallbackGet)
            }
        }
        makeCodeDialog(2, codeBean.authorizer_user_phone, codeBean.rid, codeBean.region_name, user?.last_region_id?.toInt())//2代表解除授权信息type
        LogUtils.e("zcl_baseMe___________取消授权${codeBean == null}")
    }

    private fun singleDialog(codeBean: MqttBodyBean) {
        val boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_LOGIN, false)
        LogUtils.v("zcl-------确保登录时成功的单点登录---------key${codeBean.loginStateKey != lastUser?.login_state_key}--$boolean")
        if (codeBean.loginStateKey != lastUser?.login_state_key && boolean) //确保登录时成功的
            loginOutMethod()
    }

    open fun receviedGwCmd2500M(codeBean: MqttBodyBean) {

    }

    open fun receviedGwCmd2500(gwStompBean: GwStompBean) {

    }

    open fun receviedGwCmd2000(serId: String) {

    }

    open fun loginOutMethod() = if (!NetWorkUtils.isNetworkAvalible(this)) {
        AlertDialog.Builder(this)
                .setTitle(R.string.network_tip_title)
                .setMessage(R.string.net_disconnect_tip_message)
                .setPositiveButton(android.R.string.ok
                ) { _, _ ->
                    // 跳转到设置界面
                    this.startActivityForResult(Intent(Settings.ACTION_WIRELESS_SETTINGS), 0)
                }.create().show()
    } else {
        SyncDataPutOrGetUtils.syncPutDataStart(this, object : SyncCallback {
            override fun start() {
                showLoadingDialog(getString(R.string.tip_start_sync))
            }

            override fun complete() {
                hideLoadingDialog()
                val b = this@TelinkBaseActivity.isFinishing
                val showing = singleLogin?.isShowing
                SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), Constant.IS_LOGIN, false)
                TelinkLightService.Instance()?.idleMode(true)
                var isShowSingleForeground = if (!isForeground(this@TelinkBaseActivity))//不是前台
                    !b && showing != null && !showing
                else
                    !b && showing != null && !showing && isShow //是前台
                LogUtils.v("zcl-----------前台后台执行-------$isShowSingleForeground")
                if (isShowSingleForeground) {
                    singleLogin?.dismiss()
                    singleLogin?.show()
                }

            }

            override fun error(msg: String) {
                hideLoadingDialog()
                if (msg != "null")
                    ToastUtils.showLong(msg)
            }
        })
    }


    /**
     * 报错log打印
     */
    fun onErrorReportNormal(info: ErrorReportInfo) {

        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        LogUtils.d("TelinkBluetoothSDK蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        LogUtils.d("TelinkBluetoothSDK无法收到广播包以及响应包")
//                        showToast("无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        LogUtils.d("TelinkBluetoothSDK未扫到目标设备")
//                        showToast("未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        LogUtils.d("TelinkBluetoothSDK未读到att表")
//                        showToast("未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        LogUtils.d("TelinkBluetoothSDK未建立物理连接")
//                        showToast("未建立物理连接")
                    }
                }
            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        LogUtils.d("TelinkBluetoothSDK value check失败： 密码错误")
//                        showToast("value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        LogUtils.d("TelinkBluetoothSDK read login data 没有收到response")
//                        showToast("read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        LogUtils.d("TelinkBluetoothSDK write login data 没有收到response")
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

    fun connect(meshAddress: Int = 0, fastestMode: Boolean = false, macAddress: String? = null, meshName: String? = lastUser?.controlMeshName,
                meshPwd: String? = NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16), retryTimes: Long = 2,
                deviceTypes: List<Int>? = null, connectTimeOutTime: Long = 13): Observable<DeviceInfo>? {
        LogUtils.v("zcl-----连接中判断${mConnectDisposable == null && TelinkLightService.Instance()?.isLogin == false}--" +
                "----${!Constant.IS_ROUTE_MODE}----${TelinkLightApplication.getApp().connectDevice == null}---")
        return if (mConnectDisposable == null && TelinkLightService.Instance()?.isLogin == false && !Constant.IS_ROUTE_MODE) {
            return Commander.connect(meshAddress, fastestMode, macAddress, meshName, meshPwd, retryTimes, deviceTypes, connectTimeOutTime)
                    ?.doOnSubscribe {
                        mConnectDisposable = it
                    }?.doFinally {
                        mConnectDisposable = null
                    }?.doOnError {
                        // TelinkLightService.Instance()?.idleMode(false)
                        LogUtils.v("zcl-----------连接失败失败-------doOnError")
                        // ToastUtils.showShort(getString(R.string.connect_fail))
                    }/*?.doOnNext {
                        it.boundMac = when (it.productUUID) {
                            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB -> DBUtils.getLightByID(it.id.toLong())?.boundMac
                            DeviceType.NORMAL_SWITCH, DeviceType.EIGHT_SWITCH, DeviceType.SMART_CURTAIN_SWITCH, DeviceType.SCENE_SWITCH,
                            DeviceType.DOUBLE_SWITCH, DeviceType.NORMAL_SWITCH2 -> DBUtils.getSwitchByID(it.id.toLong())?.boundMac
                            DeviceType.SENSOR,DeviceType.NIGHT_LIGHT->DBUtils.getSensorByID(it.id.toLong())?.boundMac
                            DeviceType.SMART_CURTAIN->DBUtils.getCurtainByID(it.id.toLong())?.boundMac
                            DeviceType.SMART_RELAY->DBUtils.getConnectorByID(it.id.toLong())?.boundMac
                            else -> DBUtils.getLightByID(it.meshAddress.toLong())?.boundMac
                        }
                    }*/
        } else {
            LogUtils.v("zcl-----------连接失败失败继续连接-------")
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
        // if (LeBluetooth.getInstance().isSupport(applicationContext))
        //  LeBluetooth.getInstance().enable(applicationContext)    //如果没打开蓝牙，就提示用户打开

        //如果位置服务没打开，则提示用户打开位置服务，bleScan必须
        if (!BleUtils.isLocationEnable(this)) {
            showOpenLocationServiceDialog()
        } else {
            hideLocationServiceDialog()
            TelinkLightService.Instance()?.idleMode(true)
            Thread.sleep(200)
            RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if (TelinkLightApplication.getApp().connectDevice == null/*!TelinkLightService.Instance().isLogin*/) {
                            showLoadingDialog(getString(R.string.please_wait))

                            val meshName = lastUser!!.controlMeshName

                            GlobalScope.launch {
                                //自动重连参数
                                val connectParams = Parameters.createAutoConnectParameters()
                                connectParams.setMeshName(meshName)
                                connectParams.setPassword(NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16))
                                connectParams.autoEnableNotification(true)
                                connectParams.setConnectMac(macAddr)
                                LogUtils.v("autoconnect  meshName = $meshName   meshPwd = ${NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16)}   macAddress = ${macAddr}")

                                //连接，如断开会自动重连
                                TelinkLightService.Instance()?.autoConnect(connectParams)
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
                    TelinkLightApplication.isConnecting = false
                    if (!isScanning)
                        ToastUtils.showLong(getString(R.string.connect_success))
                    changeDisplayImgOnToolbar(true)

                }
                LightAdapter.STATUS_LOGOUT -> {
                    TelinkLightApplication.isConnecting = false
                    changeDisplayImgOnToolbar(false)
                    autoConnectAll()
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

    class NetWorkChangReceiver : BroadcastReceiver() {
        private var isHaveNetwork: Boolean = true

        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                var connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                var networkInfo = connectivityManager.activeNetworkInfo
                if (networkInfo != null && networkInfo.isAvailable) {
                    if (!isHaveNetwork) {

                        val connected = TelinkLightApplication.getApp().mStompManager?.mStompClient?.isConnected
                        if (connected != true)
                            TelinkLightApplication.getApp().initStompClient()

                        LogUtils.v("zcl-----------telinbase收到监听有网状态-------$connected")
                        isHaveNetwork = true
                    }
                } else {
                    LogUtils.v("zcl-----------telinbase收到监听无网状态-------")
                    isHaveNetwork = false
                }
            } catch (e: Exception) {
                //ignore
            }
        }
    }

    fun showInstallDeviceList(isGuide: Boolean, clickRgb: Boolean) {
        this.clickRgb = clickRgb
        val view = View.inflate(this, R.layout.dialog_install_list, null)
        val closeInstallList = view.findViewById<ImageView>(R.id.close_install_list)
        val installDeviceRecyclerview = view.findViewById<RecyclerView>(R.id.install_device_recyclerView)
        closeInstallList.setOnClickListener { v -> installDialog?.dismiss() }

        val installList: ArrayList<InstallDeviceModel> = OtherUtils.getInstallDeviceList(this)

        val installDeviceListAdapter = InstallDeviceListAdapter(R.layout.item_install_device, installList)
        val layoutManager = LinearLayoutManager(this)
        installDeviceRecyclerview?.layoutManager = layoutManager
        installDeviceRecyclerview?.adapter = installDeviceListAdapter
        installDeviceListAdapter.bindToRecyclerView(installDeviceRecyclerview)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
        //添加分割线
        installDeviceRecyclerview?.addItemDecoration(decoration)
        installDeviceListAdapter.onItemClickListener = onItemClickListenerInstallList

        installDialog = AlertDialog.Builder(this).setView(view).create()
        installDialog?.setOnShowListener {}
        if (isGuide) installDialog?.setCancelable(false)
        installDialog?.show()
    }

    private val onItemClickListenerInstallList = BaseQuickAdapter.OnItemClickListener { _, _, position ->
        isGuide = false
        installDialog?.dismiss()
        when (position) {
            INSTALL_GATEWAY -> {
                installId = INSTALL_GATEWAY
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.Gate_way))
            }
            INSTALL_NORMAL_LIGHT -> {
                installId = INSTALL_NORMAL_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.normal_light))
            }
            INSTALL_RGB_LIGHT -> {
                installId = INSTALL_RGB_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.rgb_light))
            }
            INSTALL_CURTAIN -> {
                installId = INSTALL_CURTAIN
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.curtain))
            }
            INSTALL_SWITCH -> {
                installId = INSTALL_SWITCH
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.switch_title))
                stepOneText?.visibility = View.GONE
                stepTwoText?.visibility = View.GONE
                stepThreeText?.visibility = View.GONE
                switchStepOne?.visibility = View.VISIBLE
                switchStepTwo?.visibility = View.VISIBLE
                swicthStepThree?.visibility = View.VISIBLE
            }
            INSTALL_SENSOR -> {
                installId = INSTALL_SENSOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.sensor))
            }
            INSTALL_CONNECTOR -> {
                installId = INSTALL_CONNECTOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.relay))
            }
        }
    }

    fun showInstallDeviceDetail(describe: String, position: Int, string: String) {
        installDialog?.dismiss()
        installTitleTv?.text = string
        if (installDialog == null)
            installDialog = AlertDialog.Builder(this).setView(viewInstall).create()
        installDialog?.setOnShowListener {}
        installDialog?.show()
    }

    private fun makeInstallView() {
        viewInstall = View.inflate(this, R.layout.dialog_install_detail, null)
        val closeInstallList = viewInstall?.findViewById<ImageView>(R.id.close_install_list)
        val btnBack = viewInstall?.findViewById<ImageView>(R.id.btnBack)

        installTitleTv = viewInstall?.findViewById(R.id.install_title_tv)
        stepOneText = viewInstall?.findViewById(R.id.step_one)
        stepTwoText = viewInstall?.findViewById(R.id.step_two)
        stepThreeText = viewInstall?.findViewById(R.id.step_three)
        switchStepOne = viewInstall?.findViewById(R.id.switch_step_one)
        switchStepTwo = viewInstall?.findViewById(R.id.switch_step_two)
        swicthStepThree = viewInstall?.findViewById(R.id.switch_step_three)
        installHelpe = viewInstall?.findViewById(R.id.install_see_helpe)

        installHelpe?.setOnClickListener(dialogOnclick)
        val searchBar = viewInstall?.findViewById<Button>(R.id.search_bar)
        closeInstallList?.setOnClickListener(dialogOnclick)
        btnBack?.setOnClickListener(dialogOnclick)
        searchBar?.setOnClickListener(dialogOnclick)
    }

    private val dialogOnclick = View.OnClickListener {
        var medressData = 0
        var allData = DBUtils.allLight
        var sizeData = DBUtils.allLight.size
        if (sizeData != 0) {
            var lightData = allData[sizeData - 1]
            medressData = lightData.meshAddr
        }

        when (it.id) {
            R.id.close_install_list -> installDialog?.dismiss()
            R.id.install_see_helpe -> seeHelpe("#add-and-configure")

            R.id.search_bar -> {
                TelinkLightService.Instance()?.idleMode(true)
                Thread.sleep(500)
                if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                    when (installId) {
                        INSTALL_NORMAL_LIGHT -> {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
                            startActivityForResult(intent, 0)
                            installDialog?.show()
                            finish()
                        }
                        INSTALL_RGB_LIGHT -> {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_RGB)
                            startActivityForResult(intent, 0)
                            installDialog?.show()
                            finish()
                        }
                        INSTALL_CURTAIN -> {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_CURTAIN)
                            startActivityForResult(intent, 0)
                            installDialog?.show()
                            finish()

                        }
                        INSTALL_SWITCH -> {
                            if (!Constant.IS_ROUTE_MODE) {
                                startActivity(Intent(this, ScanningSwitchActivity::class.java))
                            } else {
                                intent = Intent(this, DeviceScanningNewActivity::class.java)
                                intent.putExtra(Constant.DEVICE_TYPE, 99)       //connector也叫relay
                                startActivityForResult(intent, 0)
                            }

                            installDialog?.show()
                            finish()
                        }
                        INSTALL_SENSOR -> {
                            if (!Constant.IS_ROUTE_MODE) {
                                startActivity(Intent(this, ScanningSensorActivity::class.java))
                            } else {
                                intent = Intent(this, DeviceScanningNewActivity::class.java)
                                intent.putExtra(Constant.DEVICE_TYPE, 98)       //connector也叫relay
                                startActivityForResult(intent, 0)
                            }
                            installDialog?.show()
                            finish()
                        }
                        INSTALL_CONNECTOR -> {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_RELAY)
                            startActivityForResult(intent, 0)
                            installDialog?.show()
                            finish()
                        }
                        INSTALL_GATEWAY -> {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.GATE_WAY)
                            startActivityForResult(intent, 0)
                            installDialog?.show()
                            finish()
                        }
                    }
                } else ToastUtils.showLong(getString(R.string.much_lamp_tip))


            }
            R.id.btnBack -> {
                installDialog?.dismiss()
                showInstallDeviceList(isGuide, clickRgb)
            }
        }
    }

    fun seeHelpe(webIndex: String) {
        var intent = Intent(this, InstructionsForUsActivity::class.java)
        intent.putExtra(Constant.WB_TYPE, webIndex)

        startActivity(intent)
    }

    private fun makeStopScanPop() {
        var popView: View = LayoutInflater.from(this).inflate(R.layout.pop_warm, null)

        hinitOne = popView.findViewById(R.id.pop_warm_tv)
        cancelf = popView.findViewById(R.id.tip_cancel)
        confirmf = popView.findViewById(R.id.tip_confirm)
        hinitOne.text = getString(R.string.exit_tips_in_scanning)

        popFinish = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        popFinish.isOutsideTouchable = false
        popFinish.isFocusable = true // 设置PopupWindow可获得焦点
        popFinish.isTouchable = true // 设置PopupWindow可触摸补充：
    }

    private fun makeRenamePopuwindow() {
        popReNameView = View.inflate(this, R.layout.pop_rename, null)
        renameEt = popReNameView?.findViewById(R.id.pop_rename_edt)
        renameCancel = popReNameView?.findViewById(R.id.pop_rename_cancel)
        renameConfirm = popReNameView?.findViewById(R.id.pop_rename_confirm)
        StringUtils.initEditTextFilter(renameEt)
        renameEt?.singleLine = true

        renameDialog = Dialog(this)
        renameDialog?.setContentView(popReNameView)
        renameDialog?.setCanceledOnTouchOutside(false)
        renameCancel?.setOnClickListener { renameDialog?.dismiss() }
        //确定回调 单独写
    }

    @SuppressLint("CheckResult")
    open fun routeDeleteGroup(serId: String, dbGroup: DbGroup) {
        RouterModel.routerDelGp(RouterDelGpBody(serId, dbGroup.meshAddr))?.subscribe({
            /**
            90007,"该组不存在，本地删除即可 "  90015,"空组直接本地删除，后台数据库也会同步删除(无需app调用删除接口)"
            90008,该组里的全部设备都未绑定路由，无法删除" 90005,"以下路由全部没有上线，无法开始分组" 90009,"默认组无法删除"
             */
            when (it.errorCode) {
                0, 90015, 90007 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    if (it.errorCode == 0) {
                        disposableRouteTimer?.dispose()
                        disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                                .subscribe {
                                    hideLoadingDialog()
                                    ToastUtils.showShort(getString(R.string.delete_gp_fail))
                                }
                    } else {
                        DBUtils.deleteGroupOnly(dbGroup)
                        deleteGpSuccess()
                    }
                }
                90008 -> {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                }
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90009 -> ToastUtils.showShort(getString(R.string.all_gp_cont_del))
                else -> ToastUtils.showShort(it.message)
            }
            LogUtils.v("zcl-----------收到路由删组-------$it")
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    open fun deleteGpSuccess() {

    }

    @SuppressLint("CheckResult")
    open fun routeConfigBriGpOrLight(meshAddr: Int, deviceType: Int, brightness: Int, isEnableBright: Int, serId: String) {
        var  bri = when {
            brightness<1 -> 1
            brightness>100 -> 100
            else -> brightness
        }
        LogUtils.v("zcl-----------发送路由调光参数-------$bri")
        RouterModel.routeConfigBrightness(meshAddr, deviceType, bri, isEnableBright, serId)?.subscribe({
            //    "errorCode": 90018"该设备不存在，请重新刷新数据"
            //    "errorCode": 90008,"该设备没有绑定路由，无法操作"
            //    "errorCode": 90007,"该组不存在，请重新刷新数据
            //    "errorCode": 90005"message": "该设备绑定的路由没在线"
            configBriOrColorTempResult(it, 0)
        }) {
            ToastUtils.showShort(it.message)
        }
    }

    @SuppressLint("CheckResult")
    open fun routeConfigTempGpOrLight(meshAddr: Int, deviceType: Int, brightness: Int, serId: String) {
        var  bri = when {
            brightness<1 -> 1
            brightness>100 -> 100
            else -> brightness
        }
        LogUtils.v("zcl----------- zcl-----------发送路由调色参数-------$bri-------")
        RouterModel.routeConfigColorTemp(meshAddr, deviceType, bri, serId)?.subscribe({
            //    "errorCode": 90018"该设备不存在，请重新刷新数据"
            //    "errorCode": 90008,"该设备没有绑定路由，无法操作"
            //    "errorCode": 90007,"该组不存在，请重新刷新数据
            //    "errorCode": 90005"message": "该设备绑定的路由没在线"
            configBriOrColorTempResult(it, 1)

        }) {
            ToastUtils.showShort(it.message)
        }
    }

    open fun configBriOrColorTempResult(it: Response<RouterTimeoutBean>, isBri: Int) {
        LogUtils.v("zcl-----------收到配置亮度-------$isBri")
        when (it.errorCode) {
            0 -> {
                lastTime = System.currentTimeMillis()
                //showLoadingDialog(getString(R.string.please_wait))
                disposableRouteTimer?.dispose()
                disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                        .subscribe {
                            hideLoadingDialog()
                            when (isBri) {
                                0 -> ToastUtils.showShort(getString(R.string.config_bri_fail))
                                1 -> ToastUtils.showShort(getString(R.string.config_color_temp_fail))
                                2 -> ToastUtils.showShort(getString(R.string.config_white_fail))
                                else -> ToastUtils.showShort(getString(R.string.config_color_temp_fail))
                            }
                        }
            }
            90018 -> {
                DBUtils.deleteLocalData()
                //ToastUtils.showShort(getString(R.string.device_not_exit))
                SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
                finish()
            }
            90008 -> {
                hideLoadingDialog()
                ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
            }
            90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
            90005 -> ToastUtils.showShort(getString(R.string.router_offline))
            else -> ToastUtils.showShort(it.message)
        }
    }

    @SuppressLint("CheckResult")
    open fun routerChangeGpDevice(bodyBean: GroupBodyBean) {
        RouterModel.routeBatchGpNew(bodyBean)?.subscribe({ itR ->
            LogUtils.v("zcl-----------收到路由开始分组http成功-------$itR")
            when (itR.errorCode) {
                NetworkStatusCode.OK -> {//等待会回调
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(itR.t.timeout + 3L, TimeUnit.SECONDS)
                            .subscribe {
                                ToastUtils.showShort(getString(R.string.group_timeout))
                            }
                }
                NetworkStatusCode.CURRENT_GP_NOT_EXITE -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                NetworkStatusCode.PRODUCTUUID_NOT_MATCH_DEVICE_TYPE -> ToastUtils.showShort(getString(R.string.device_type_not_match))
                NetworkStatusCode.ROUTER_ALL_OFFLINE -> ToastUtils.showShort(getString(R.string.router_offline))
                NetworkStatusCode.DEVICE_NOT_BINDROUTER -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_version))
            }

        }, {
            LogUtils.v("zcl-----------收到路由fenzu失败-------$it")
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    open fun routerDeviceResetFactory(mac: String, meshAddr: Int, productUUID: Int, ser_id: String) {
        //    "errorCode": 20030,已生成移交码，此时不支持任何恢复操作，请删除移交码再重试"  窗帘 = 0x10
        RouterModel.routeResetFactory(MacResetBody(macAddr = mac, meshAddr = meshAddr, meshType = productUUID, ser_id = ser_id))?.subscribe({
            when (it.errorCode) {
                0 -> {
                    LogUtils.v("zcl-----------收到路由的恢复出厂结果-------$it")
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.reset_factory_fail))
                            }
                }
                90030 -> ToastUtils.showShort(getString(R.string.transfer_accounts_code_exit_cont_perform))
                900018 -> {
                    ToastUtils.showShort(getString(R.string.device_not_exit))
                    finish()
                }
                90008 -> {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                }
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    open fun routeOpenOrCloseBase(meshAddr: Int, productUUID: Int, status: Int, serId: String) {//如果发送后失败则还原 0关1开
        LogUtils.v("zcl-----------收到路由开关灯指令---meshAddr-----$meshAddr----deviceType-------$productUUID-----是开--${status == 1}")
        val subscribe = RouterModel.routeOpenOrClose(meshAddr, productUUID, status, serId)?.subscribe({
            LogUtils.v("zcl-----------收到路由成功-------$it")
            //    "errorCode": 90018,该设备不存在，请重新刷新数据"   "errorCode": 90008,该设备没有绑定路由，无法操作"
            //   "errorCode": 90007该组不存在，请重新刷新数据"   errorCode": 90005,"message": "该设备绑定的路由没在线"
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                if (productUUID == DeviceType.LIGHT_NORMAL)
                                    ToastUtils.showShort(getString(R.string.open_light_faile))
                            }
                }
                90018 -> {
                    DBUtils.deleteLocalData()
                    //ToastUtils.showShort(getString(R.string.device_not_exit))
                    SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
                    finish()
                }
                90008 -> {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                }
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            LogUtils.v("zcl-----------收到路由失败-------$it")
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    open fun routerOpenOrCloseSensor(id: Int, status: Int, ser_id: String) {
        RouterModel.routeSwitchSensor(id, status, ser_id)?.subscribe({
            LogUtils.v("zcl-----------路由请求传感器1开或0关----status${status}---$it")
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                /* if (status == 1)
                                     ToastUtils.showShort(getString(R.string.open_faile))
                                 else
                                     ToastUtils.showShort(getString(R.string.close_faile))*/
                            }
                }
                90030 -> ToastUtils.showShort(getString(R.string.scene_cont_exit_to_refresh))
                90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                90018 -> ToastUtils.showShort(getString(R.string.device_not_exit))
                90011 -> ToastUtils.showShort(getString(R.string.scene_cont_exit_to_refresh))
                90008 -> {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                }
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
            }
        }, {
            LogUtils.v("zcl-----------收到路由失败-------$it")
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")//op	否	int	0连接，1断开。默认0
    open fun routerConnectSw(it: DbSwitch, op: Int, ser_id: String) {  //直连开关或传感器meshType 开关 = 99 或 0x20 或 0x22 或 0x21 或 0x28 或 0x27 或 0x25 传感器 = 98 或 0x23 或 0x24
        LogUtils.v("zcl-----------收到路由请求连接开关id-------${it.id}")
        RouterModel.routerConnectSwOrSe(it?.id ?: 0, 99, op, ser_id)?.subscribe({
            LogUtils.v("zcl-----------收到路由请求连接成功是否是连接--${op == 0}-----$it")
            when (it.errorCode) {
                0 -> {
                    if (op == 0) {
                        disposableRouteTimer?.dispose()
                        disposableRouteTimer = Observable.timer(it.t.timeout.toLong() + 2, TimeUnit.SECONDS)
                                .subscribe {
                                    hideLoadingDialog()
                                    ToastUtils.showShort(getString(R.string.connect_fail))
                                }
                    }
                }
                90018 -> {
                    DBUtils.deleteLocalData()
                    //ToastUtils.showShort(getString(R.string.device_not_exit))
                    SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
                    hideLoadingDialog()
                    finish()
                }
                90008 -> {
                    ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                    hideLoadingDialog()
                }
                90005 -> {
                    ToastUtils.showShort(getString(R.string.router_offline))
                    hideLoadingDialog()
                }
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    //否	int	0连接，1断开。默认0
    open fun routerConnectSensor(it: DbSensor, op: Int, ser_id: String) {  //直连开关或传感器meshType 开关 = 99 或 0x20 或 0x22 或 0x21 或 0x28 或 0x27 或 0x25 传感器 = 98 或 0x23 或 0x24
        if (it.id == 0L)
            return
        RouterModel.routerConnectSwOrSe(it.id, it.productUUID, op, ser_id)
                ?.subscribe({ response ->
                    LogUtils.v("zcl-----------收到路由请求连接开关或者传感器是否是连接--${op == 0}-------$response")
                    when (response.errorCode) {
                        0 -> {
                            if (op == 0) {
                                disposableRouteTimer?.dispose()
                                disposableRouteTimer = Observable.timer((response.t.timeout).toLong() + 1, TimeUnit.SECONDS)
                                        .subscribe {
                                            hideLoadingDialog()
                                            ToastUtils.showShort(getString(R.string.connect_fail))
                                        }
                            }
                        }
                        90018 -> {
                            DBUtils.deleteLocalData()
                            hideLoadingDialog()
                            // ToastUtils.showShort(getString(R.string.device_not_exit))
                            SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
                            finish()
                        }
                        90008 -> {
                            hideLoadingDialog()
                            ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                        }
                        90005 -> {
                            hideLoadingDialog()
                            ToastUtils.showShort(getString(R.string.router_offline))
                        }
                    }

                }, { it1 ->
                    hideLoadingDialog()
                    ToastUtils.showShort(it1.message)
                })
    }

    @SuppressLint("CheckResult")
    open fun routerGetVersion(mesAddress: MutableList<Int>, deviceType: Int, serId: String) {
        //普通灯 = 4 彩灯 = 6 蓝牙连接器 = 5 窗帘 = 16 传感器 = 98 或 0x23 或 0x24n
        // 开关 = 99 或 0x20 或 0x22 或 0x21 或 0x28 或 0x27 或 0x25
        val subscribe = RouterModel.getDevicesVersion(mesAddress, deviceType, serId)?.subscribe({
            LogUtils.v("zcl-----------路由请求版本--mesAddress$mesAddress-----$it")
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong() + 1, TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.get_version_fail))
                            }
                }
                90997 -> ToastUtils.showShort(getString(R.string.get_versioning))
                90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                90018 -> {
                    DBUtils.deleteLocalData()
                    // ToastUtils.showShort(getString(R.string.device_not_exit))
                    SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
                    finish()
                }
                90008 -> {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                }
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                90999 -> goScanning()//扫描中，不能再次进行扫描。请尝试获取路由模式下状态以恢复上次扫描
                90998 -> {
                    ToastUtils.showShort(getString(R.string.otaing_to_ota_activity))
                    startActivity(Intent(this@TelinkBaseActivity, RouterOtaActivity::class.java))
                    finish()
                }//OTA中不能扫描，请稍后。请尝试获取路由模式下状态以恢复上次OTA
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    private fun goScanning() {
        ToastUtils.showShort(getString(R.string.sanning_to_scan_activity))
        Observable.timer(2000, TimeUnit.MILLISECONDS).subscribe {
            startActivity(Intent(this@TelinkBaseActivity, DeviceScanningNewActivity::class.java))
            finish()
        }
    }

    @SuppressLint("CheckResult")
    open fun routerConfigSensor(id: Long, configuration: ConfigurationBean, ser_id: String) {
        RouterModel.configSensor(id, configuration, ser_id)?.subscribe({
            LogUtils.v("zcl-----------收到路由请求配置-------$it")
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.config_fail))
                            }
                }
                90022 -> {
                    ToastUtils.showShort(getString(R.string.device_not_exit))
                    finish()
                }
                90008 -> {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                }
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    open fun routerUpdateLightName(id: Long, name: String) {
        RouterModel.routeUpdateLightName(id, name)?.subscribe({
            renameSucess()
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    open fun routerUpdateSensorName(id: Long, name: String) {
        RouterModel.routeUpdateSensorName(id, name)?.subscribe({
            renameSucess()
            updateAllSensor()
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    open fun updateAllSensor() {
        NetworkFactory.getApi()
                .getSensorList(lastUser?.token)
                .compose(NetworkTransformer())
                .subscribe({
                    DBUtils.deleteAllSensor()
                    for (item in it)
                        DBUtils.saveSensor(item, true)
                }, {})
    }

    open fun renameSucess() {

    }

    override fun setMessage(cmd: Int, extCmd: Int, message: String) {

    }

    open fun getWeekStr(week: Int?): String {
        var tmpWeek = week ?: 0
        val sb = StringBuilder()
        var str = when (tmpWeek) {
            0b01111111, 0b10000000 -> sb.append(getString(R.string.every_day)).toString()
            0b00000000 -> sb.append(getString(R.string.only_one)).toString()
            else -> {
                var list = mutableListOf(
                        WeekBean(getString(R.string.monday), 1, (tmpWeek and Constant.MONDAY) != 0),
                        WeekBean(getString(R.string.tuesday), 2, (tmpWeek and Constant.TUESDAY) != 0),
                        WeekBean(getString(R.string.wednesday), 3, (tmpWeek and Constant.WEDNESDAY) != 0),
                        WeekBean(getString(R.string.thursday), 4, (tmpWeek and Constant.THURSDAY) != 0),
                        WeekBean(getString(R.string.friday), 5, (tmpWeek and Constant.FRIDAY) != 0),
                        WeekBean(getString(R.string.saturday), 6, (tmpWeek and Constant.SATURDAY) != 0),
                        WeekBean(getString(R.string.sunday), 7, (tmpWeek and Constant.SUNDAY) != 0))
                for (i in 0 until list.size) {
                    if (list[i].selected)
                        sb.append(list[i].week).append(",")
                }
                sb.toString().substring(0, sb.length - 1)
            }
        }
        return str
    }

    inner class BluetoothListenerReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    LogUtils.v("zcl-----------监听蓝牙-------${intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)}")
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                        BluetoothAdapter.STATE_TURNING_ON -> {
                            LogUtils.v("zcl-----------监听蓝牙正在打开-------")
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            LogUtils.v("zcl-----------监听蓝牙正在关闭-------")
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            LogUtils.v("zcl-----------监听蓝牙关闭-------")
                        }
                        BluetoothAdapter.STATE_ON -> {
                            if (!DBUtils.isFastDoubleClick(1000)) {
                                autoConnectAll()
                                LogUtils.v("zcl-----------监听蓝牙开启执行自动连接-------")
                            }
                        }
                    }
                }
            }
        }
    }
}
