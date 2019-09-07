package com.dadoutek.uled.othersview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.method.ScrollingMovementMethod
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.allenliu.versionchecklib.v2.AllenVersionChecker
import com.allenliu.versionchecklib.v2.builder.UIData
import com.allenliu.versionchecklib.v2.callback.CustomVersionDialogListener
import com.allenliu.versionchecklib.v2.ui.VersionService.builder
import com.app.hubert.guide.core.Controller
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ProcessUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.connector.ScanningConnectorActivity
import com.dadoutek.uled.curtain.CurtainScanningNewActivity
import com.dadoutek.uled.device.DeviceFragment
import com.dadoutek.uled.fragment.MeFragment
import com.dadoutek.uled.group.GroupListFragment
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.intf.CallbackLinkMainActAndFragment
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbConnector
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.HttpModel.UpdateModel
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.network.VersionBean
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.scene.SceneFragment
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.dadoutek.uled.widget.BaseUpDateDialog
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.*
import com.telink.bluetooth.light.*
import com.telink.bluetooth.light.DeviceInfo
import com.telink.util.Event
import com.telink.util.EventListener
import com.xiaomi.market.sdk.XiaomiUpdateAgent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.indefiniteSnackbar
import java.util.*
import java.util.concurrent.TimeUnit

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 2

/**
 * 首页  修改mes那么后  如果旧有用户不登陆 会报错直接崩溃 因为调用后的mesname是空的
 *
 * 首页设备
 */
class MainActivity : TelinkBaseActivity(), EventListener<String>, CallbackLinkMainActAndFragment {

    private val connectFailedDeviceMacList: MutableList<String> = mutableListOf()
    private var bestRSSIDevice: DeviceInfo? = null

    private lateinit var deviceFragment: DeviceFragment
    private lateinit var groupFragment: GroupListFragment
    private lateinit var meFragment: MeFragment
    private lateinit var sceneFragment: SceneFragment

    //防止内存泄漏
    internal var mDisposable = CompositeDisposable()
    private var mConnectDisposal: Disposable? = null
    private var mScanTimeoutDisposal: Disposable? = null

    private var mApplication: TelinkLightApplication? = null
    private var connectMeshAddress: Int = 0
    private val mDelayHandler = Handler()
    private var retryConnectCount = 0

    private var mConnectSuccessSnackBar: Snackbar? = null
    private var mConnectSnackBar: Snackbar? = null
    private var mScanSnackBar: Snackbar? = null
    private var mNotFoundSnackBar: Snackbar? = null

    private var mTelinkLightService: TelinkLightService? = null
    private var guideShowCurrentPage = false
    private var installId = 0

    private lateinit var stepOneText: TextView
    private lateinit var stepTwoText: TextView
    private lateinit var stepThreeText: TextView
    private lateinit var switchStepOne: TextView
    private lateinit var switchStepTwo: TextView
    private lateinit var swicthStepThree: TextView

    private val mRxPermission = RxPermissions(this)

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                    BluetoothAdapter.STATE_ON -> {
                        TelinkLightService.Instance().idleMode(true)
                        retryConnectCount = 0
                        startScan()
                    }
                    BluetoothAdapter.STATE_OFF -> {
                    }
                }
            }
        }
    }
    //记录用户首次点击返回键的时间
    private var firstTime: Long = 0

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        detectUpdate()
        requestCamera()

        this.setContentView(R.layout.activity_main)
        this.mApplication = this.application as TelinkLightApplication
        initBottomNavigation()
        TelinkLightApplication.getApp().initStompClient()
        addEventListeners()
    }

    private fun addEventListeners() {
        // 监听各种事件
//        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
//        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)

//        this.mApplication?.addEventListener(NotificationEvent.ONLINE_STATUS, this)
        this.mApplication?.addEventListener(NotificationEvent.GET_DEVICE_STATE, this)
        this.mApplication?.addEventListener(ServiceEvent.SERVICE_CONNECTED, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    private fun requestCamera() {
        val disposable = mRxPermission.request(Manifest.permission.CAMERA)
                .subscribe(
                        {

                        },
                        {
                            LogUtils.d(it)
                        }
                )

    }

    /**
     * 检查App是否有新版本
     */
    private fun detectUpdate() {
        XiaomiUpdateAgent.setCheckUpdateOnlyWifi(false)
        XiaomiUpdateAgent.update(this)
    }


    var syncCallback: SyncCallback = object : SyncCallback {
        override fun start() {
            showLoadingDialog(getString(R.string.tip_start_sync))
        }

        override fun complete() {
            AlertDialog.Builder(this@MainActivity)
                    .setCancelable(false)
                    .setMessage(getString(R.string.version_disabled))
                    .setPositiveButton(R.string.exit) { dialog, which ->
                        LogOutAndExitApp()
                    }
                    .show()
        }

        override fun error(msg: String) {
            LogUtils.d("upload data failed msg = $msg")
            AlertDialog.Builder(this@MainActivity)
                    .setCancelable(false)
                    .setMessage(getString(R.string.version_disabled))
                    .setPositiveButton(R.string.exit) { _, _ ->
                        LogOutAndExitApp()
                    }

        }
    }

    private fun LogOutAndExitApp() {
        SharedPreferencesHelper.putBoolean(this@MainActivity, Constant.IS_LOGIN, false)
        TelinkLightService.Instance().disconnect()
        TelinkLightService.Instance().idleMode(true)

        //重启app并杀死原进程
        ActivityUtils.finishAllActivities(true)
        ActivityUtils.startActivity(SplashActivity::class.java)
    }

    //防止viewpager嵌套fragment,fragment放置后台时间过长,fragment被系统回收了
    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState)
        //outState.putParcelable("android:support:fragments", null)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            when {
                bnve.currentItem == 0 -> deviceFragment.myPopViewClickPosition(ev.x, ev.y)
                bnve.currentItem == 1 -> groupFragment.myPopViewClickPosition(ev.x, ev.y)
                bnve.currentItem == 2 -> sceneFragment.myPopViewClickPosition(ev.x, ev.y)
            }
            return super.dispatchTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    var installDialog: AlertDialog? = null
    var isGuide: Boolean = false
    var clickRgb: Boolean = false
    private fun showInstallDeviceList(isGuide: Boolean, clickRgb: Boolean) {
        this.clickRgb = clickRgb
        val view = View.inflate(
                this, R.layout.dialog_install_list, null)
        val close_install_list = view.findViewById<ImageView>(R.id.close_install_list)
        val install_device_recyclerView = view.findViewById<RecyclerView>(R.id.install_device_recyclerView)
        close_install_list.setOnClickListener { v -> installDialog?.dismiss() }

        val installList: ArrayList<InstallDeviceModel> = OtherUtils.getInstallDeviceList(this)

        val installDeviceListAdapter = InstallDeviceListAdapter(R.layout.item_install_device, installList)
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        install_device_recyclerView?.layoutManager = layoutManager
        installDeviceListAdapter.bindToRecyclerView(install_device_recyclerView)
        installDeviceListAdapter.onItemClickListener = onItemClickListenerInstallList

        installDialog = AlertDialog.Builder(this)
                .setView(view)
                .create()

        installDialog?.setOnShowListener {}

        if (isGuide) {
            installDialog?.setCancelable(false)
        }

        installDialog?.show()

        Thread {
            Thread.sleep(100)
            GlobalScope.launch(Dispatchers.Main) {
                guide3(install_device_recyclerView)
            }
        }.start()
    }

    private fun showInstallDeviceDetail(describe: String) {
        val view = View.inflate(this, R.layout.dialog_install_detail, null)
        val close_install_list = view.findViewById<ImageView>(R.id.close_install_list)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        stepOneText = view.findViewById(R.id.step_one)
        stepTwoText = view.findViewById(R.id.step_two)
        stepThreeText = view.findViewById(R.id.step_three)
        switchStepOne = view.findViewById(R.id.switch_step_one)
        switchStepTwo = view.findViewById(R.id.switch_step_two)
        swicthStepThree = view.findViewById(R.id.switch_step_three)
        val install_tip_question = view.findViewById<TextView>(R.id.install_tip_question)
        val search_bar = view.findViewById<Button>(R.id.search_bar)
        close_install_list.setOnClickListener(dialogOnclick)
        btnBack.setOnClickListener(dialogOnclick)
        search_bar.setOnClickListener(dialogOnclick)
        install_tip_question.text = describe
        install_tip_question.movementMethod = ScrollingMovementMethod.getInstance()
        installDialog = AlertDialog.Builder(this)
                .setView(view)
                .create()

        installDialog?.setOnShowListener {}

        installDialog?.show()
    }

    private val dialogOnclick = View.OnClickListener {

        when (it.id) {
            R.id.close_install_list -> {
                installDialog?.dismiss()
            }
            R.id.search_bar -> {
                when (installId) {
                    INSTALL_NORMAL_LIGHT -> {
                        intent = Intent(this, DeviceScanningNewActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, false)
                        intent.putExtra(Constant.TYPE_VIEW, Constant.LIGHT_KEY)
                        startActivityForResult(intent, 0)
                    }
                    INSTALL_RGB_LIGHT -> {
                        intent = Intent(this, DeviceScanningNewActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.TYPE_VIEW, Constant.RGB_LIGHT_KEY)
                        startActivityForResult(intent, 0)

                    }
                    INSTALL_CURTAIN -> {

                        intent = Intent(this, CurtainScanningNewActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                        startActivityForResult(intent, 0)
                    }
                    INSTALL_SWITCH -> startActivity(Intent(this, ScanningSwitchActivity::class.java))
                    INSTALL_SENSOR -> startActivity(Intent(this, ScanningSensorActivity::class.java))
                    INSTALL_CONNECTOR -> {
                        intent = Intent(this, ScanningConnectorActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                        startActivityForResult(intent, 0)
                    }
                }
                installDialog?.dismiss()
            }
            R.id.btnBack -> {
                installDialog?.dismiss()
                showInstallDeviceList(isGuide, clickRgb)
            }
        }
    }

    private fun guide3(install_device_recyclerView: RecyclerView): Controller? {
        installDialog?.layoutInflater
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_GROUPLIST_KEY, false)
        if (guideShowCurrentPage) {
            installDialog?.layoutInflater
            var guide3: View? = null
            if (clickRgb) {
                guide3 = install_device_recyclerView.getChildAt(1)
            } else {
//                guide3 = install_device_recyclerView.getChildAt(0)
                guide3 = install_device_recyclerView.layoutManager!!.findViewByPosition(0)
            }

            return GuideUtils.guideBuilder(this, installDialog!!.window.decorView, GuideUtils.GUIDE_START_INSTALL_DEVICE_NOW)
                    .addGuidePage(GuideUtils.addGuidePage(guide3!!, R.layout.view_guide_simple_group2, getString(R.string.group_list_guide2), View.OnClickListener {
                        guide3.performClick()
                        GuideUtils.changeCurrentViewIsEnd(this, GuideUtils.END_GROUPLIST_KEY, true)
                    }, GuideUtils.END_GROUPLIST_KEY, this))
                    .show()
        }
        return null
    }

    val INSTALL_NORMAL_LIGHT = 0
    val INSTALL_RGB_LIGHT = 1
    val INSTALL_SWITCH = 2
    val INSTALL_SENSOR = 3
    val INSTALL_CURTAIN = 4
    val INSTALL_CONNECTOR = 5

    val onItemClickListenerInstallList = BaseQuickAdapter.OnItemClickListener { _, _, position ->
        var intent: Intent? = null
        isGuide = false
        installDialog?.dismiss()
        when (position) {
            INSTALL_NORMAL_LIGHT -> {
                installId = INSTALL_NORMAL_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this))
            }
            INSTALL_RGB_LIGHT -> {
                installId = INSTALL_RGB_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this))
            }
            INSTALL_CURTAIN -> {
                installId = INSTALL_CURTAIN
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this))
            }
            INSTALL_SWITCH -> {
                installId = INSTALL_SWITCH
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this))
                stepOneText.visibility = GONE
                stepTwoText.visibility = GONE
                stepThreeText.visibility = GONE
                switchStepOne.visibility = View.VISIBLE
                switchStepTwo.visibility = View.VISIBLE
                swicthStepThree.visibility = View.VISIBLE
            }
            INSTALL_SENSOR -> {
                installId = INSTALL_SENSOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this))
            }
            INSTALL_CONNECTOR -> {
                installId = INSTALL_CONNECTOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this))
            }
        }
    }

    override fun showDeviceListDialog(isGuide: Boolean, isClickRgb: Boolean) {
        showInstallDeviceList(isGuide, isClickRgb)
    }

    private fun syncDataAndExit() {
        if (!NetWorkUtils.isNetworkAvalible(this)) {
            LogUtils.d(getString(R.string.net_disconnect_tip_message))
        } else {
            SyncDataPutOrGetUtils.syncPutDataStart(this, syncCallback)
        }
    }

    /**
     * 检查App版本是否可用
     */
    private fun checkVersionAvailable() {
        var version = packageName(this)
        UpdateModel.isVersionAvailable(0, version)
                .subscribe(object : NetworkObserver<ResponseVersionAvailable>() {
                    override fun onNext(s: ResponseVersionAvailable) {
                        if (s.isUsable == false) {
                            syncDataAndExit()
                        }
                        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), "isShowDot", s.isUsable)
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        ToastUtils.showLong(R.string.get_server_version_fail)
                    }
                })
    }

    @SuppressLint("CheckResult")
    private fun getVersion() {
        val info = this.packageManager.getPackageInfo(this.packageName, 0)
        LogUtils.e("zcl**********************VersionBean${info.versionName}")
        UpdateModel.getVersion(info.versionName)!!.subscribe({
            object : NetworkObserver<VersionBean>() {
                override fun onNext(t: VersionBean) {
                    CreateDialog(t)
                    LogUtils.e("zcl**********************VersionBean$t")
                }

                override fun onError(e: Throwable) {
                    super.onError(e)
                    ToastUtils.showLong(R.string.get_server_version_fail)
                }
            }
        }, {})

    }

    private fun CreateDialog(t: VersionBean) {
        builder = AllenVersionChecker
                .getInstance()
                .downloadOnly(crateUIData(t))
        //下载完毕直接安装
        builder.isDirectDownload = true
        builder.isShowNotification = false
        builder.isShowDownloadingDialog = false
        builder.isShowDownloadFailDialog = false

        builder.customVersionDialogListener = createCustomDialogOne(t)
        builder.setForceUpdateListener { forceUpdate() }
        builder.executeMission(this)
    }

    private fun forceUpdate() {
        //上传数据，推出登录
    }

    private fun createCustomDialogOne(t: VersionBean): CustomVersionDialogListener {
        return CustomVersionDialogListener { _, _ ->
            var upDateDialog = BaseUpDateDialog(this, R.style.BaseDialog, R.layout.custom_dialog_two_layout)
            upDateDialog.findViewById<TextView>(R.id.tv_msg).text = t.data.description
            upDateDialog.setCanceledOnTouchOutside(true)
            upDateDialog
        }
    }

    /**
     * @return
     * @important 使用请求版本功能，可以在这里设置downloadUrl
     * 这里可以构造UI需要显示的数据
     * UIData 内部是一个Bundle
     */
    private fun crateUIData(v: VersionBean): UIData {
        val uiData = UIData.create()
        uiData.title = getString(R.string.update_version)
        uiData.downloadUrl = v.data.url
        uiData.content = v.data.description
        return uiData
    }

    fun packageName(context: Context): String {
        val manager = context.packageManager
        var name: String? = null
        try {
            val info = manager.getPackageInfo(context.packageName, 0)
            name = info.versionName
        } catch (e: NameNotFoundException) {
            e.printStackTrace()
        }
        return name!!
    }


    /**
     * 初始化底部的navigation按钮
     */
    private fun initBottomNavigation() {
        deviceFragment = DeviceFragment()
        groupFragment = GroupListFragment()
        sceneFragment = SceneFragment()
        meFragment = MeFragment()
        val fragments: List<Fragment> = listOf(deviceFragment, groupFragment, sceneFragment, meFragment)
        val vpAdapter = ViewPagerAdapter(supportFragmentManager, fragments)
        viewPager.adapter = vpAdapter
        viewPager.offscreenPageLimit = 3    //屏幕外的page数量限制调整为3
        //禁止所有动画效果
        bnve.enableAnimation(false)
        bnve.enableShiftingMode(false)
        bnve.enableItemShiftingMode(false)
        bnve.setupWithViewPager(viewPager)
//        bnve.currentItem = 1  //默认显示第二页
//        viewPager.currentItem = 1
/*
        viewPager.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {
                SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, object : SyncCallback {
                    override fun start() {}
                    override fun complete() {}
                    override fun error(msg: String) {}
                })
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
            }

            override fun onPageSelected(p0: Int) {
            }
        })
*/
    }


    //切换到场景Fragment
    fun transScene() {
        bnve.currentItem = 2
    }

    //切换到组控制Fragment
    private fun tranHome() {
        bnve.currentItem = 1
    }

    override fun onPause() {
        super.onPause()
        mScanTimeoutDisposal?.dispose()
        mConnectDisposal?.dispose()
        mScanSnackBar?.dismiss()
        mNotFoundSnackBar?.dismiss()
        mConnectSnackBar?.dismiss()
        progressBar.visibility = GONE

        //移除事件
        this.mApplication?.removeEventListener(this)

        try {
            this.unregisterReceiver(mReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopConnectTimer()
    }

    override fun onResume() {
        super.onResume()
        checkVersionAvailable()
        //检测service是否为空，为空则重启
        if (TelinkLightService.Instance() == null) {
            mApplication?.startLightService(TelinkLightService::class.java)
        }

        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
        registerReceiver(mReceiver, filter)
    }

    var locationServiceDialog: AlertDialog? = null
    fun showOpenLocationServiceDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.open_location_service)
        builder.setNegativeButton(getString(android.R.string.ok)) { _, _ ->
            BleUtils.jumpLocationSetting()
        }
        locationServiceDialog = builder.create()
        locationServiceDialog?.setCancelable(false)
        locationServiceDialog?.show()
    }

    override fun changeToScene() {
        transScene()
    }

    override fun changeToGroup() {
        tranHome()
    }

    fun hideLocationServiceDialog() {
        locationServiceDialog?.hide()
    }


    override fun onPostResume() {
        super.onPostResume()
        autoConnect()
    }

    fun autoConnect() {
        //检查是否支持蓝牙设备
        if (!LeBluetooth.getInstance().isSupport(applicationContext)) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show()
            ActivityUtils.finishAllActivities()
        } else {  //如果蓝牙没开，则弹窗提示用户打开蓝牙
            if (!LeBluetooth.getInstance().isEnabled) {
                root.indefiniteSnackbar(R.string.openBluetooth, android.R.string.ok) {
                    LeBluetooth.getInstance().enable(applicationContext)
                }
            } else {
                //如果位置服务没打开，则提示用户打开位置服务
                if (!BleUtils.isLocationEnable(this)) {
                    showOpenLocationServiceDialog()
                } else {
                    hideLocationServiceDialog()
                    mTelinkLightService = TelinkLightService.Instance()
                    if (mTelinkLightService?.adapter?.mLightCtrl?.currentLight?.isConnected != true) {
                        while (TelinkApplication.getInstance()?.serviceStarted == true) {
                            LogUtils.d("start Auto connect")
                            GlobalScope.launch(Dispatchers.Main) {
                                retryConnectCount = 0
                                connectFailedDeviceMacList.clear()
                                val meshName = DBUtils.lastUser!!.controlMeshName

                                //自动重连参数
                                val connectParams = Parameters.createAutoConnectParameters()
                                connectParams.setMeshName(meshName)
                                connectParams.setPassword(NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16))
                                connectParams.autoEnableNotification(true)
                                //连接，如断开会自动重连
                                Thread {
                                    TelinkLightService.Instance().autoConnect(connectParams)
                                }.start()

                                //刷新Notify参数
                                val refreshNotifyParams = Parameters.createRefreshNotifyParameters()
                                refreshNotifyParams.setRefreshRepeatCount(2)
                                refreshNotifyParams.setRefreshInterval(1000)
                                //开启自动刷新Notify
                                TelinkLightService.Instance().autoRefreshNotify(refreshNotifyParams)

                            }
                            break
                        }

                    } else {
                        mNotFoundSnackBar?.dismiss()
                        progressBar?.visibility = GONE
                        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), Constant.CONNECT_STATE_SUCCESS_KEY, true)
                    }
                }
            }
        }

        val deviceInfo = this.mApplication?.connectDevice
        if (deviceInfo != null) {
            this.connectMeshAddress = (this.mApplication?.connectDevice?.meshAddress
                    ?: 0x00) and 0xFF
        }
    }

    @SuppressLint("CheckResult")
    private fun startScan() {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    if (it) {
                        TelinkLightService.Instance().idleMode(true)
                        bestRSSIDevice = null   //扫描前置空信号最好设备。
                        //扫描参数
                        // val account = DBUtils.lastUser?.account
                        val meshName = DBUtils.lastUser?.controlMeshName

                        LogUtils.e("zcl**********************扫描账号$meshName")
                        val scanFilters = ArrayList<ScanFilter>()
                        val scanFilter = ScanFilter.Builder()
                                .setDeviceName(meshName)
                                .build()
                        scanFilters.add(scanFilter)

                        val params = LeScanParameters.create()
                        if (!AppUtils.isExynosSoc) {
                            params.setScanFilters(scanFilters)
                        }
                        params.setMeshName(meshName)
                        params.setOutOfMeshName(meshName)
                        params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                        params.setScanMode(false)

                        startScanTimeout()//重新订阅超时时间
                        TelinkLightService.Instance().startScan(params)
                        startCheckRSSITimer()
                    } else {
                        //没有授予权限
                        DialogUtils.showNoBlePermissionDialog(this, {
                            retryConnectCount = 0
                            startScan()
                        }, { finish() })
                    }
                }, {})
    }

    private fun stopConnectTimer() {
        mConnectDisposal?.dispose()
    }

    @SuppressLint("CheckResult")
    private fun connect(mac: String) {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN)
                .subscribe {
                    if (it) {
                        //授予了权限
                        val instance = TelinkLightService.Instance()
                        if (instance != null) {
                            progressBar?.visibility = View.VISIBLE
                            mScanTimeoutDisposal?.dispose()
                            instance.connect(mac, CONNECT_TIMEOUT)
                        }
                    } else {
                        //没有授予权限
                        DialogUtils.showNoBlePermissionDialog(this, { connect(mac) }, { finish() })
                    }
                }
    }

    private fun startScanTimeout() {
        mScanTimeoutDisposal?.dispose()
        mScanTimeoutDisposal = Observable.timer(SCAN_TIMEOUT_SECOND.toLong(), TimeUnit.SECONDS)
                .subscribe({
                    TelinkLightService.Instance()?.idleMode(false)      //use this stop scan
                    onLeScanTimeout()
                }, {})
    }

    private fun startCheckRSSITimer() {
        mScanTimeoutDisposal?.dispose()
        val periodCount = SCAN_TIMEOUT_SECOND.toLong() - SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND
        Observable.intervalRange(1, periodCount, SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND, 1,
                TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(object : io.reactivex.Observer<Long?> {
                    override fun onComplete() {
                        retryConnect()
                    }

                    override fun onSubscribe(d: Disposable) {
                        mScanTimeoutDisposal = d
                    }

                    override fun onNext(t: Long) {
                        if (bestRSSIDevice != null) {
                            mScanTimeoutDisposal?.dispose()
                            LogUtils.d("connect device , mac = ${bestRSSIDevice?.macAddress}  rssi = ${bestRSSIDevice?.rssi}")
                            connect(bestRSSIDevice!!.macAddress)
                        }
                    }

                    override fun onError(e: Throwable) {
                        LogUtils.d(e)
                    }
                })
    }

/*
    private fun login() {
        val meshName = DBUtils.lastUser?.controlMeshName
        val pwd = NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16)
        TelinkLightService.Instance()?.login(Strings.stringToBytes(meshName, 16)
                , Strings.stringToBytes(pwd, 16))
    }
*/

    private fun onNError(event: DeviceEvent) {

        TelinkLightService.Instance().idleMode(true)
        TelinkLog.d("DeviceScanningActivity#onNError")

        val builder = AlertDialog.Builder(this)
        builder.setMessage("当前环境:Android7.0!连接重试:" + " 3次失败!")
        builder.setNegativeButton("confirm") { dialog, _ -> dialog.dismiss() }
        builder.setCancelable(false)
        builder.show()
    }

    /**
     * 重试
     * 先扫描到信号比较好的，再连接。
     */
    private fun retryConnect() {
        if (retryConnectCount < MAX_RETRY_CONNECT_TIME) {
            retryConnectCount++
            autoConnect()
        } else {
            LogUtils.d("exceed max retry time, show connection error")
            TelinkLightService.Instance().idleMode(true)
            setSnack()
        }
    }

    private fun setSnack() {
        if (mNotFoundSnackBar?.isShown != true) {
            progressBar.visibility = GONE
        }
    }

    /**
     * 检查是不是灯
     */
    private fun checkIsLight(info: OnlineStatusNotificationParser.DeviceNotificationInfo?): Boolean {
        if (info != null) {
            when (info.reserve) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB -> return true
                else -> return false
            }
        }
        return true
    }

    private fun checkIsRelay(info: OnlineStatusNotificationParser.DeviceNotificationInfo?): Boolean {
        if (info != null) {
            when (info.reserve) {
                DeviceType.SMART_RELAY -> return true
                else -> return false
            }
        }
        return true
    }

    private fun checkIsCurtain(info: OnlineStatusNotificationParser.DeviceNotificationInfo?): Boolean {
        if (info != null) {
            when (info.reserve) {
                DeviceType.SMART_CURTAIN -> return true
                else -> return false
            }
        }
        return true
    }

    override fun onStop() {
        super.onStop()
        if (TelinkLightService.Instance() != null)
            TelinkLightService.Instance().disableAutoRefreshNotify()
        installDialog?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        this.mDelayHandler.removeCallbacksAndMessages(null)
        Lights.getInstance().clear()
        mDisposable.dispose()
        AllenVersionChecker.getInstance().cancelAllMission(this)
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                mScanTimeoutDisposal?.dispose()

                GlobalScope.launch(Dispatchers.Main) {
                    //在主线程执行
                    stopConnectTimer()
                    if (progressBar?.visibility != GONE)
                        progressBar?.visibility = GONE

                    mScanSnackBar?.dismiss()
                    mConnectSnackBar?.dismiss()

//                    delay(300)
                }

                val connectDevice = this.mApplication?.connectDevice
                RecoverMeshDeviceUtil.createDeviceIfNotExistInDb(deviceInfo)//  如果已连接的设备不存在与数据库，则创建。
                if (connectDevice != null) {
                    this.connectMeshAddress = connectDevice.meshAddress
                }
                ToastUtils.showShort(getString(R.string.connect_success))
                RecoverMeshDeviceUtil.findMeshDevice(DBUtils.lastUser!!.controlMeshName)
                        .subscribeOn(Schedulers.io())
                        .subscribe {
                            LogUtils.d("added $it mesh devices")
                            deviceFragment.refreshView()
                        }
            }
            LightAdapter.STATUS_LOGOUT -> {
                LogUtils.d("STATUS_LOGOUT")
                retryConnect()
            }
//            LightAdapter.STATUS_CONNECTED -> {
//                if (!TelinkLightService.Instance().isLogin)
//                    login()
//            }
            LightAdapter.STATUS_ERROR_N -> onNError(event)

        }
    }

    /**
     * 处理[NotificationEvent.ONLINE_STATUS]事件
     */
    @Synchronized
    private fun onOnlineStatusNotify(event: NotificationEvent) {

        val notificationInfoList: List<OnlineStatusNotificationParser.DeviceNotificationInfo>?

        notificationInfoList = event.parse() as List<OnlineStatusNotificationParser.DeviceNotificationInfo>

        if (notificationInfoList.isEmpty()) {
            LogUtils.d("notificationInfoList is empty")
            return
        }

        for (notificationInfo: OnlineStatusNotificationParser.DeviceNotificationInfo in notificationInfoList) {

            val meshAddress = notificationInfo.meshAddress
            val brightness = notificationInfo.brightness
            val productUUID = notificationInfo.reserve
            val connectionStatus = notificationInfo.connectionStatus
            if (checkIsLight(notificationInfo)) {
                val dbLight = DBUtils.getLightByMeshAddr(meshAddress)
                if (dbLight != null) {
                    dbLight.connectionStatus = connectionStatus.value
                    dbLight.updateIcon()
                    runOnUiThread { /* deviceFragment.notifyDataSetChanged() */ }
                } else {
                    if (connectionStatus != ConnectionStatus.OFFLINE) {
                        val dbLightNew = DbLight()
                        LogUtils.d("light_mesh_2:" + (productUUID and 0xff))
                        if ((productUUID and 0xff) == 0xff || (productUUID and 0xff) == 0x04) {
                            dbLightNew?.productUUID = 0x04
                        } else if ((productUUID and 0xff) == 0x06) {
                            dbLightNew?.productUUID = 0x06
                        }
                        dbLightNew.connectionStatus = connectionStatus.value
                        dbLightNew.updateIcon()
                        dbLightNew.belongGroupId = DBUtils.groupNull?.id
                        dbLightNew.brightness = brightness
                        dbLightNew.color = 0
                        dbLightNew.colorTemperature = 0
                        dbLightNew.meshAddr = meshAddress
                        dbLightNew.name = getString(R.string.unnamed)
                        dbLightNew.macAddr = "0"
                        DBUtils.saveLight(dbLightNew, false)
                        LogUtils.d("creat_light" + dbLightNew.meshAddr)
                    }
                }
            }

            if (checkIsRelay(notificationInfo)) {
                val dbLight = DBUtils.getRelyByMeshAddr(meshAddress)
                if (dbLight != null) {
                    dbLight.connectionStatus = connectionStatus.value
                    dbLight.updateIcon()
                    runOnUiThread { /*deviceFragment.notifyDataSetChanged()*/ }
                } else {
                    if (connectionStatus != ConnectionStatus.OFFLINE) {
                        val dbLightNew = DbConnector()
                        LogUtils.d("light_mesh_2:" + (productUUID and 0xff))
                        if ((productUUID and 0xff) == 0x05) {
                            dbLightNew?.productUUID = 0x05
                        }
                        dbLightNew.connectionStatus = connectionStatus.value
                        dbLightNew.updateIcon()
                        dbLightNew.belongGroupId = DBUtils.groupNull?.id
                        dbLightNew.color = 0
                        dbLightNew.meshAddr = meshAddress
                        dbLightNew.name = getString(R.string.unnamed)
                        dbLightNew.macAddr = "0"
                        DBUtils.saveConnector(dbLightNew, false)
                        LogUtils.d("creat_light" + dbLightNew.meshAddr)
                    }
                }
            }

            if (checkIsCurtain(notificationInfo)) {
                val dbLight = DBUtils.getCurtainByMeshAddr(meshAddress)
                if (dbLight != null) {
                    dbLight.connectionStatus = connectionStatus.value
                    dbLight.updateIcon()
                    runOnUiThread { /*deviceFragment.notifyDataSetChanged()*/ }
                } else {
                    if (connectionStatus != ConnectionStatus.OFFLINE) {
                        val dbLightNew = DbCurtain()
                        LogUtils.d("light_mesh_2:" + (productUUID and 0xff))
                        if ((productUUID and 0xff) == 0x10) {
                            dbLightNew?.productUUID = 0x10
                        }
                        dbLightNew.setConnectionStatus(connectionStatus.value)
                        dbLightNew.updateIcon()
                        dbLightNew.belongGroupId = DBUtils.groupNull?.id
                        dbLightNew.meshAddr = meshAddress
                        dbLightNew.name = getString(R.string.unnamed)
                        dbLightNew.macAddr = "0"
                        DBUtils.saveCurtain(dbLightNew, false)
                        LogUtils.d("creat_light" + dbLightNew.meshAddr)
                    }
                }
            }
        }
    }


    private fun onServiceConnected(event: ServiceEvent) {}

    private fun onServiceDisconnected(event: ServiceEvent) {
        LogUtils.d("onServiceDisconnected")
        TelinkLightApplication.getApp().startLightService(TelinkLightService::class.java)
    }


    /**
     * 事件处理方法
     *
     * @param event
     */
    override fun performed(event: Event<String>) {
        when (event.type) {
//            LeScanEvent.LE_SCAN -> onLeScan(event as LeScanEvent)
//            LeScanEvent.LE_SCAN_TIMEOUT -> onLeScanTimeout()
//            NotificationEvent.ONLINE_STATUS -> this.onOnlineStatusNotify(event as NotificationEvent)
            NotificationEvent.GET_ALARM -> {
            }
            DeviceEvent.STATUS_CHANGED -> {
                progressBar.visibility = GONE
                this.onDeviceStatusChanged(event as DeviceEvent)
            }
            ServiceEvent.SERVICE_CONNECTED -> {
                this.onServiceConnected(event as ServiceEvent)
            }
            ServiceEvent.SERVICE_DISCONNECTED -> this.onServiceDisconnected(event as ServiceEvent)
            NotificationEvent.GET_DEVICE_STATE -> onNotificationEvent(event as NotificationEvent)
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
            }
        }
    }

    /**
     * 扫描不到任何设备了
     * （扫描结束）
     */
    private fun onLeScanTimeout() {
        LogUtils.d("onErrorReport: onLeScanTimeout")
        setSnack()
    }

    private fun isSwitch(uuid: Int): Boolean {
        return when (uuid) {
            DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SENSOR, DeviceType.NIGHT_LIGHT -> {
                LogUtils.d("This is switch")
                true
            }
            else -> false
        }
    }

    /**
     * 处理扫描事件
     *
     * @param event
     */
    @Synchronized
    private fun onLeScan(event: LeScanEvent) {
        val mesh = this.mApplication?.mesh
//        val meshAddress = mesh?.generateMeshAddr()
        val deviceInfo: DeviceInfo = event.args

        Thread {
            val dbLight = DBUtils.getLightByMeshAddr(deviceInfo.meshAddress)
            if (dbLight != null && dbLight.macAddr == "0") {
                dbLight.macAddr = deviceInfo.macAddress
                DBUtils.updateLight(dbLight)
            }
        }.start()

        if (!isSwitch(deviceInfo.productUUID)) {
            if (bestRSSIDevice != null) {
                //扫到的灯的信号更好并且没有连接失败过就把要连接的灯替换为当前扫到的这个。
                if (deviceInfo.rssi > bestRSSIDevice?.rssi ?: 0) {
                    LogUtils.d("changeToScene to device with better RSSI  new meshAddr = ${deviceInfo.meshAddress} rssi = ${deviceInfo.rssi}")
                    bestRSSIDevice = deviceInfo
                }
            } else {
                LogUtils.d("RSSI  meshAddr = ${deviceInfo.meshAddress} rssi = ${deviceInfo.rssi}")
                bestRSSIDevice = deviceInfo
            }
        }
    }

    /**
     * 报错log打印
     */
    private fun onErrorReport(info: ErrorReportInfo) {
        if (bestRSSIDevice != null) {
//            connectFailedDeviceMacList.add(bestRSSIDevice!!.macAddress)
        }
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
//                retryConnect()

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
//                retryConnect()
            }
        }
    }

    private fun onNotificationEvent(event: NotificationEvent) {
        if (!foreground) return
        // 解析版本信息
        val data = event.args.params
        if (data[0] == NotificationEvent.DATA_GET_MESH_OTA_PROGRESS) {
            TelinkLog.w("mesh ota progress: " + data[1])
            val progress = data[1].toInt()
            if (progress != 100) {
                startActivity(Intent(this, OTAUpdateActivity::class.java)
                        .putExtra(OTAUpdateActivity.INTENT_KEY_CONTINUE_MESH_OTA, OTAUpdateActivity.CONTINUE_BY_REPORT)
                        .putExtra("progress", progress))
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
            val secondTime = System.currentTimeMillis()
            var isDelete = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_DELETE, false)
            if (isDelete) {
                val intent = Intent("isDelete")
                intent.putExtra("isDelete", "true")
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(intent)
                return true
            } else {
                if (secondTime - firstTime > 2000) {
                    Toast.makeText(this@MainActivity, R.string.click_double_exit, Toast.LENGTH_SHORT).show()
                    firstTime = secondTime
                    return true
                } else {
                    LogUtils.d("App id = ${ProcessUtils.getCurrentProcessName()}")
                    LogUtils.d("activity stack size = ${ActivityUtils.getActivityList()}")

                    setResult(Activity.RESULT_FIRST_USER)
                    finish()
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

}