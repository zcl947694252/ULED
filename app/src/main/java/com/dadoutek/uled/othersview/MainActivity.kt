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
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.app.hubert.guide.core.Controller
import com.app.hubert.guide.util.LogUtil
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.connector.ScanningConnectorActivity
import com.dadoutek.uled.curtain.CurtainScanningNewActivity
import com.dadoutek.uled.device.NewDevieFragment
import com.dadoutek.uled.group.GroupListFragment
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.intf.CallbackLinkMainActAndFragment
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.DeviceListFragment
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.HttpModel.UpdateModel
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.scene.SceneFragment
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.*
import com.telink.bluetooth.light.*
import com.telink.bluetooth.light.DeviceInfo
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.Strings
import com.xiaomi.market.sdk.XiaomiUpdateAgent
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.dialog_install_detail.*
import kotlinx.android.synthetic.main.fragment_me.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar
import org.json.JSONException
import org.json.JSONObject
import org.w3c.dom.Text
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1

class MainActivity : TelinkBaseActivity(), EventListener<String>,CallbackLinkMainActAndFragment{
    private val connectFailedDeviceMacList: MutableList<String> = mutableListOf()
    private var bestRSSIDevice: DeviceInfo? = null

    private lateinit var deviceFragment: DeviceListFragment
    private lateinit var newDeviceFragment: NewDevieFragment
    private lateinit var groupFragment: GroupListFragment
    private lateinit var meFragment: MeFragment
    private lateinit var sceneFragment: SceneFragment

    //防止内存泄漏
    internal var mDisposable = CompositeDisposable()
    private var mApplication: TelinkLightApplication? = null
    private var connectMeshAddress: Int = 0
    private val mDelayHandler = Handler()
    private var retryConnectCount = 0

    private var mConnectSuccessSnackBar: Snackbar? = null
    private var mConnectSnackBar: Snackbar? = null
    private var mScanSnackBar: Snackbar? = null
    private var mNotFoundSnackBar: Snackbar? = null
    private var mConnectDisposal: Disposable? = null
    private var mScanTimeoutDisposal: Disposable? = null
    private var mTelinkLightService: TelinkLightService? = null
    private var isCreate=false
    private var guideShowCurrentPage = false
    private var installId = 0

    private lateinit var stepOneText:TextView
    private lateinit var stepTwoText:TextView
    private lateinit var stepThreeText:TextView
    private lateinit var switchStepOne:TextView
    private lateinit var switchStepTwo:TextView
    private lateinit var swicthStepThree:TextView
    internal var isClickExlogin = false
    private  var isState= false


    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)

                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        TelinkLightService.Instance().idleMode(true)
                        retryConnectCount = 0
                        startScan()
                        LogUtil.d("STATE_ON")
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        LogUtil.d("STATE_OFF")
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
//        var isLoginVersion= SharedPreferencesHelper.getBoolean(TelinkLightApplication.getInstance(),Constant.USER_LOGIN,false)
//        if(isLoginVersion){
//
//        }else{
//            exitLogin()
//        }
        var version=packageName(this)
        var list=DBUtils.getAllUser()
        com.xiaomi.market.sdk.Log.d("dataSize1", list.size.toString())
        detectUpdate()


        this.setContentView(R.layout.activity_main)
        this.mApplication = this.application as TelinkLightApplication
        initBottomNavigation()

        isCreate=true
    }

    private fun exitLogin() {
        isClickExlogin = true
        if (DBUtils.allLight.size == 0 && !DBUtils.dataChangeAllHaveAboutLight && DBUtils.allCurtain.size==0 && !DBUtils.dataChangeAllHaveAboutCurtain) {
            if (isClickExlogin) {
                SharedPreferencesHelper.putBoolean(this, Constant.IS_LOGIN, false)
                TelinkLightService.Instance().disconnect()
                TelinkLightService.Instance().idleMode(true)

                restartApplication()
            }
            hideLoadingDialog()
        } else {
            checkNetworkAndSync(this)
        }

    }

    // 如果没有网络，则弹出网络设置对话框
    fun checkNetworkAndSync(activity: Activity?) {
        if (!NetWorkUtils.isNetworkAvalible(activity!!)) {
            android.app.AlertDialog.Builder(activity)
                    .setTitle(R.string.network_tip_title)
                    .setMessage(R.string.net_disconnect_tip_message)
                    .setPositiveButton(android.R.string.ok
                    ) { dialog, whichButton ->
                        // 跳转到设置界面
                        activity.startActivityForResult(Intent(
                                Settings.ACTION_WIRELESS_SETTINGS),
                                0)
                    }.create().show()
        } else {
            SyncDataPutOrGetUtils.syncPutDataStart(activity, syncCallback)
        }
    }

    internal var syncCallback: SyncCallback = object : SyncCallback {

        override fun start() {
            showLoadingDialog(getString(R.string.tip_start_sync))
        }

        override fun complete() {
            if (isClickExlogin) {
                SharedPreferencesHelper.putBoolean(this@MainActivity, Constant.IS_LOGIN, false)
                TelinkLightService.Instance().disconnect()
                TelinkLightService.Instance().idleMode(true)

                restartApplication()
            } else {
                ToastUtils.showLong(getString(R.string.upload_data_success))
            }
            hideLoadingDialog()
        }

        override fun error(msg: String) {
            if (isClickExlogin) {
                android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.sync_error_exlogin)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                            SharedPreferencesHelper.putBoolean(this@MainActivity, Constant.IS_LOGIN, false)
                            TelinkLightService.Instance().idleMode(true)
                            dialog.dismiss()
                            restartApplication()
                        }
                        .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which ->
                            dialog.dismiss()
                            isClickExlogin = false
                            hideLoadingDialog()
                        }.show()
            } else {
                isClickExlogin = false
                hideLoadingDialog()
            }

            //            Log.d("SyncLog", "error: " + msg);
            //            ToastUtils.showLong(getString(R.string.sync_error_contant));
        }
    }

    //重启app并杀死原进程
    private fun restartApplication() {
        ActivityUtils.finishAllActivities(true)
        ActivityUtils.startActivity(SplashActivity::class.java)
    }

    //防止viewpager嵌套fragment,fragment放置后台时间过长,fragment被系统回收了
    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {

        super.onSaveInstanceState(outState)
        outState.putParcelable("android:support:fragments", null)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if(ev?.getAction()==MotionEvent.ACTION_DOWN){
            if(bnve.currentItem==0){
                newDeviceFragment.myPopViewClickPosition(ev.x,ev.y)
            }else if(bnve.currentItem==1){
                groupFragment.myPopViewClickPosition(ev.x,ev.y)
            }else if(bnve.currentItem==2){
                sceneFragment.myPopViewClickPosition(ev.x,ev.y)
            }
            return super.dispatchTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    var installDialog: android.app.AlertDialog?=null
    var isGuide:Boolean = false
    var clickRgb:Boolean = false
    private fun showInstallDeviceList(isGuide: Boolean, clickRgb: Boolean) {
        this.clickRgb=clickRgb
        val view = View.inflate(this,R.layout.dialog_install_list,null)
        val close_install_list=view.findViewById<ImageView>(R.id.close_install_list)
        val install_device_recyclerView=view.findViewById<RecyclerView>(R.id.install_device_recyclerView)
        close_install_list.setOnClickListener { v ->  installDialog?.dismiss()}

        val installList:ArrayList<InstallDeviceModel> = OtherUtils.getInstallDeviceList(this)

        val installDeviceListAdapter = InstallDeviceListAdapter(R.layout.item_install_device, installList)
        val layoutManager =  LinearLayoutManager(this)
        install_device_recyclerView?.layoutManager = layoutManager
        install_device_recyclerView?.adapter = installDeviceListAdapter
        installDeviceListAdapter.bindToRecyclerView(install_device_recyclerView)
//                val decoration = DividerItemDecoration(this,
//                DividerItemDecoration
//                        .VERTICAL)
//        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color
//                .divider)))
//        //添加分割线
//        install_device_recyclerView?.addItemDecoration(decoration)

        installDeviceListAdapter.onItemClickListener = onItemClickListenerInstallList

        installDialog = android.app.AlertDialog.Builder(this)
                .setView(view)
                .create()

        installDialog?.setOnShowListener {

        }

        if(isGuide){
            installDialog?.setCancelable(false)
        }

        installDialog?.show()

        Thread{
            Thread.sleep(100)
            GlobalScope.launch(Dispatchers.Main){
                guide3(install_device_recyclerView)
            }
        }.start()
    }

    private fun showInstallDeviceDetail(describe : String){
        val view = View.inflate(this,R.layout.dialog_install_detail,null)
        val close_install_list=view.findViewById<ImageView>(R.id.close_install_list)
        val btnBack=view.findViewById<ImageView>(R.id.btnBack)
        stepOneText = view.findViewById<TextView>(R.id.step_one)
        stepTwoText = view.findViewById<TextView>(R.id.step_two)
        stepThreeText = view.findViewById<TextView>(R.id.step_three)
        switchStepOne = view.findViewById<TextView>(R.id.switch_step_one)
        switchStepTwo = view.findViewById<TextView>(R.id.switch_step_two)
        swicthStepThree = view.findViewById<TextView>(R.id.switch_step_three)
        val install_tip_question=view.findViewById<TextView>(R.id.install_tip_question)
        val search_bar=view.findViewById<Button>(R.id.search_bar)
        close_install_list.setOnClickListener(dialogOnclick)
        btnBack.setOnClickListener(dialogOnclick)
        search_bar.setOnClickListener(dialogOnclick)
        install_tip_question.text = describe
        install_tip_question.movementMethod = ScrollingMovementMethod.getInstance()
        installDialog = android.app.AlertDialog.Builder(this)
                .setView(view)
                .create()

        installDialog?.setOnShowListener {

        }

        if(isGuide){
//            installDialog?.setCancelable(false)
        }

        installDialog?.show()
    }

    private val dialogOnclick =View.OnClickListener{
        var medressData=0
        var allData=DBUtils.allLight
        var sizeData=DBUtils.allLight.size
        if(sizeData!=0){
            var lightData= allData[sizeData-1]
            medressData=lightData.meshAddr
        }

        when(it.id){
            R.id.close_install_list->{ installDialog?.dismiss()}
            R.id.search_bar->{
                when (installId) {
                    INSTALL_NORMAL_LIGHT -> {
                        if (medressData < 254) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, false)
                            intent.putExtra(Constant.TYPE_VIEW,Constant.LIGHT_KEY)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_RGB_LIGHT -> {
                        if (medressData < 254) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                            intent.putExtra(Constant.TYPE_VIEW,Constant.RGB_LIGHT_KEY)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_CURTAIN -> {
                        if (medressData < 254) {
                            intent = Intent(this, CurtainScanningNewActivity::class.java)
                            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                            intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_SWITCH -> startActivity(Intent(this, ScanningSwitchActivity::class.java))
                    INSTALL_SENSOR -> startActivity(Intent(this, ScanningSensorActivity::class.java))
                    INSTALL_CONNECTOR -> {
                        if(medressData<254){
                            intent = Intent(this, ScanningConnectorActivity::class.java)
                            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                            intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                            startActivityForResult(intent, 0)
                        }else{
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                }
            }
            R.id.btnBack->{
                installDialog?.dismiss()
                showInstallDeviceList(isGuide,clickRgb)
            }
        }
    }

    private fun guide3(install_device_recyclerView: RecyclerView): Controller? {
        val listView =installDialog?.getListView()
        installDialog?.getLayoutInflater()
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_GROUPLIST_KEY, false)
        if (guideShowCurrentPage) {
            installDialog?.layoutInflater
            var guide3: View? = null
            if(clickRgb){
                guide3 = install_device_recyclerView.getChildAt(1)
            }else{
                guide3 = install_device_recyclerView.getChildAt(0)
            }

            return GuideUtils.guideBuilder(this, installDialog!!.window.decorView,GuideUtils.GUIDE_START_INSTALL_DEVICE_NOW)
                    .addGuidePage(GuideUtils.addGuidePage(guide3!!, R.layout.view_guide_simple_group2, getString(R.string.group_list_guide2), View.OnClickListener {
                        guide3.performClick()
                        GuideUtils.changeCurrentViewIsEnd(this, GuideUtils.END_GROUPLIST_KEY, true)
                    }, GuideUtils.END_GROUPLIST_KEY, this))
                    .show()
        }
        return null
    }

    val INSTALL_NORMAL_LIGHT=0
    val INSTALL_RGB_LIGHT=1
    val INSTALL_SWITCH=2
    val INSTALL_SENSOR=3
    val INSTALL_CURTAIN=4
    val INSTALL_CONNECTOR=5

    val onItemClickListenerInstallList = BaseQuickAdapter.OnItemClickListener {
        adapter, view, position ->
        var intent: Intent? = null
        //点击任何一个选项跳转页面都隐藏引导
//        val controller=guide2()
//            controller?.remove()
        isGuide = false
        installDialog?.dismiss()
//        hidePopupMenu()
        when (position) {
            INSTALL_NORMAL_LIGHT -> {
                installId=INSTALL_NORMAL_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId,this))
            }
            INSTALL_RGB_LIGHT -> {
                installId=INSTALL_RGB_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId,this))
            }
            INSTALL_CURTAIN -> {
                installId=INSTALL_CURTAIN
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId,this))
            }
            INSTALL_SWITCH -> {
                installId=INSTALL_SWITCH
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId,this))
                stepOneText.visibility = View.GONE
                stepTwoText.visibility = View.GONE
                stepThreeText.visibility = View.GONE
                switchStepOne.visibility = View.VISIBLE
                switchStepTwo.visibility = View.VISIBLE
                swicthStepThree.visibility = View.VISIBLE
            }
            INSTALL_SENSOR -> {
                installId=INSTALL_SENSOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId,this))
            }
            INSTALL_CONNECTOR->{
                installId=INSTALL_CONNECTOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId,this))
            }
        }
    }

    override fun showDeviceListDialog(isGuide: Boolean,isClickRgb:Boolean) {
        showInstallDeviceList(isGuide,isClickRgb)
    }

    /**
     * 检查App是否有新版本
     */
    private fun detectUpdate() {
        var version=packageName(this)
        UpdateModel.checkVersion(0,version)!!.subscribe(object : NetworkObserver<Any>() {
            override fun onNext(s: Any) {
                val jsonObject = JSONObject(s as Map<*, *>)
                try {
                    val data = jsonObject.getString("isUsable")
                    var judge="false"
                    if(data==judge){
                        XiaomiUpdateAgent.setCheckUpdateOnlyWifi(true)
                        XiaomiUpdateAgent.update(this@MainActivity)
                 }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            override fun onError(e: Throwable) {
                super.onError(e)
                ToastUtils.showLong(R.string.get_server_version_fail)
            }
        })

    }

    fun packageName(context: Context): String {
        val manager = context.getPackageManager()
        var name: String? = null
        try {
            val info = manager.getPackageInfo(context.getPackageName(), 0)
            name = info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return name!!
    }


    private fun initBottomNavigation() {
        deviceFragment = DeviceListFragment()
        groupFragment = GroupListFragment()
        sceneFragment = SceneFragment()
        newDeviceFragment = NewDevieFragment()
        meFragment = MeFragment()
        val fragments: List<Fragment> = listOf(newDeviceFragment, groupFragment, sceneFragment, meFragment)
        val vpAdapter = ViewPagerAdapter(supportFragmentManager, fragments)
        viewPager.adapter = vpAdapter
        //禁止所有动画效果
        bnve.enableAnimation(false)
        bnve.enableShiftingMode(false)
        bnve.enableItemShiftingMode(false)
        bnve.setupWithViewPager(viewPager)
    }

//    private fun guide1() {
//        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_MAIN_KEY, false)
//        if (guideShowCurrentPage) {
//            val guide1 = bnve.getBottomNavigationItemView(1)
//
//            GuideUtils.guideBuilder(this, GuideUtils.MAIN_STEP0_GUIDE_TO_SCENE)
//                    .addGuidePage(GuideUtils.addGuidePage(guide1, R.layout.view_guide_simple_main_1, getString(R.string.change_to_scene), View.OnClickListener {
//                        transScene()
//                        GuideUtils.changeCurrentViewIsEnd(this,GuideUtils.END_MAIN_KEY,true)
//                    }, GuideUtils.END_MAIN_KEY,this))
//                    .show()
//        }
//    }

    public fun transDevice(){
        bnve.currentItem=0
    }

    public fun transScene(){
        bnve.currentItem=2
    }

    private fun tranHome(){
        Constant.isCreat=true
        bnve.currentItem=1
    }

    override fun onPause() {
        super.onPause()
        mScanTimeoutDisposal?.dispose()
        mConnectDisposal?.dispose()
        mScanSnackBar?.dismiss()
        mNotFoundSnackBar?.dismiss()
        mConnectSnackBar?.dismiss()
        progressBar.visibility = View.GONE

        //移除事件
        this.mApplication?.removeEventListener(this)

        try {
            this.unregisterReceiver(mReceiver)
        }catch (e:Exception){
            e.printStackTrace()
        }
        stopConnectTimer()
    }

    override fun onResume() {
        super.onResume()
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
        builder.setNegativeButton( getString(android.R.string.ok)) { dialog, which ->
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


    fun addEventListeners() {
        // 监听各种事件
        addScanListeners()
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication?.addEventListener(NotificationEvent.ONLINE_STATUS, this)
//        this.mApplication?.addEventListener(NotificationEvent.GET_ALARM, this)
        this.mApplication?.addEventListener(NotificationEvent.GET_DEVICE_STATE, this)
        this.mApplication?.addEventListener(ServiceEvent.SERVICE_CONNECTED, this)
//        this.mApplication?.addEventListener(MeshEvent.OFFLINE, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    override fun onPostResume() {
        super.onPostResume()
        addEventListeners()
        autoConnect()
    }

    public fun autoConnect() {
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
                            GlobalScope.launch(Dispatchers.Main) {
                                retryConnectCount = 0
                                connectFailedDeviceMacList.clear()
                                startScan()
                            }
                            break
                    }

                    } else {
                        mNotFoundSnackBar?.dismiss()
                        progressBar?.visibility = View.GONE
                        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.CONNECT_STATE_SUCCESS_KEY, true);
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
                .subscribe {
                    if (it) {
                        TelinkLightService.Instance().idleMode(true)
                        bestRSSIDevice = null   //扫描前置空信号最好设备。
                        //扫描参数
                        val account = DBUtils.lastUser?.account

                        val scanFilters = ArrayList<ScanFilter>()
                        val scanFilter = ScanFilter.Builder()
                                .setDeviceName(account)
                                .build()
                        scanFilters.add(scanFilter)

                        val params = LeScanParameters.create()
                        if(!AppUtils.isExynosSoc()){
                            params.setScanFilters(scanFilters)
                        }
                        params.setMeshName(account)
                        params.setOutOfMeshName(account)
                        params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                        params.setScanMode(false)

                        addScanListeners()
                        TelinkLightService.Instance().startScan(params)
                        startCheckRSSITimer()

                        if (mConnectSnackBar?.isShown != true && mScanSnackBar?.isShown != true)
                            mScanSnackBar = indefiniteSnackbar(root, getString(R.string.scanning_devices))
                    } else {
                        //没有授予权限
                        DialogUtils.showNoBlePermissionDialog(this, {
                            retryConnectCount = 0
                            startScan()
                        }, { finish() })
                    }
                }
    }

    private fun startConnectTimer() {
        mConnectDisposal?.dispose()
        mConnectDisposal = Observable.timer(CONNECT_TIMEOUT.toLong(), TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    retryConnect()
                }
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
                        if (TelinkLightService.Instance() != null) {
                            progressBar?.visibility = View.VISIBLE
                            TelinkLightService.Instance().connect(mac, CONNECT_TIMEOUT)
                            startConnectTimer()

                            if (mConnectSnackBar?.isShown != true)
                                mConnectSnackBar = indefiniteSnackbar(root, getString(R.string.connecting))

                        }
                    } else {
                        //没有授予权限
                        DialogUtils.showNoBlePermissionDialog(this, { connect(mac) }, { finish() })
                    }
                }
    }

    private fun startCheckRSSITimer() {
        mScanTimeoutDisposal?.dispose()
        val periodCount = SCAN_TIMEOUT_SECOND.toLong() - SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND
        Observable.intervalRange(1, periodCount, SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND, 1,
                TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Long?> {
                    override fun onComplete() {
                        LogUtils.d("onLeScanTimeout()")
                        onLeScanTimeout()
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
                        Log.d("SawTest", "error = $e")

                    }
                })
    }

    private fun login() {
        val account = DBUtils.lastUser?.account
        val pwd = NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16)
        TelinkLightService.Instance().login(Strings.stringToBytes(account, 16)
                , Strings.stringToBytes(pwd, 16))
    }

    private fun onNError(event: DeviceEvent) {

//        ToastUtils.showLong(getString(R.string.connect_fail))
        SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

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
            if (TelinkLightService.Instance().adapter.mLightCtrl.currentLight?.isConnected != true)
                startScan()
            else
                login()
        } else {
            TelinkLightService.Instance().idleMode(true)
            if (mNotFoundSnackBar?.isShown != true) {
                mNotFoundSnackBar = indefiniteSnackbar(root,
                        R.string.not_found_light, R.string.retry) {
                    retryConnectCount = 0
                    connectFailedDeviceMacList.clear()
                    startScan()
                }
            }

        }
    }


    /**
     * 检查是不是灯
     */
    private fun checkIsLight(info: OnlineStatusNotificationParser.DeviceNotificationInfo?): Boolean {
        if (info != null) {
            when (info.reserve) {
                DeviceType.LIGHT_NORMAL,DeviceType.LIGHT_NORMAL_OLD,DeviceType.LIGHT_RGB -> return true
                else -> return false
            }
        }
        return true
    }

    override fun onStop() {
        super.onStop()
        if (TelinkLightService.Instance() != null)
            TelinkLightService.Instance().disableAutoRefreshNotify()
        isCreate=false
        installDialog?.dismiss()
    }


    override fun onDestroy() {
        super.onDestroy()
        this.mDelayHandler.removeCallbacksAndMessages(null)
        Lights.getInstance().clear()
        mDisposable.dispose()
        isCreate=true
    }

    private fun addScanListeners() {
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)
    }

       private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                TelinkLightService.Instance().enableNotification()
                TelinkLightService.Instance().updateNotification()
                GlobalScope.launch(Dispatchers.Main) {
                    stopConnectTimer()
                    if (progressBar?.visibility != View.GONE)
                        progressBar?.visibility = View.GONE

                    mScanSnackBar?.dismiss()
                    mConnectSnackBar?.dismiss()
                    delay(300)

                    if (mConnectSuccessSnackBar?.isShown != true)
                        mConnectSuccessSnackBar = snackbar(root, R.string.connect_success)
                    mConnectSnackBar?.dismiss()
                }

                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, true)
                val connectDevice = this.mApplication?.connectDevice
                if (connectDevice != null) {
                    this.connectMeshAddress = connectDevice.meshAddress
                }
            }
            LightAdapter.STATUS_LOGOUT -> {
                retryConnect()
            }
            LightAdapter.STATUS_CONNECTED -> {
                if (!TelinkLightService.Instance().isLogin)
                    login()
            }
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
//            Log.d("Saw", "meshAddress = " + meshAddress + "  reserve = " + notificationInfo.reserve +
//                    " status = " + notificationInfo.status + " connectionStatus = " + notificationInfo.connectionStatus.value)
            if (checkIsLight(notificationInfo)) {
                val dbLight = DBUtils.getLightByMeshAddr(meshAddress)
                if (dbLight != null) {
                    dbLight.connectionStatus = connectionStatus.value
                    dbLight.updateIcon()
//                    if ((productUUID and 0xff) == 0xff && dbLight.productUUID == 0xff) {
//                        dbLight.productUUID = 0x04
//                            com.blankj.utilcode.util.LogUtils.d("light_mesh_1:"+data[3]+"-"+(data[3].toInt() and 0xff))
//                        com.blankj.utilcode.util.LogUtils.d("light_mesh_1:" + (productUUID and 0xff))
//                    }
//                    Thread {
//                        DBUtils.updateLightLocal(dbLight)
//                    }.start()
                    runOnUiThread { deviceFragment.notifyDataSetChanged() }
                } else {
                    if (connectionStatus != ConnectionStatus.OFFLINE) {
                        val dbLightNew = DbLight()
                        com.blankj.utilcode.util.LogUtils.d("light_mesh_2:" + (productUUID and 0xff))
                        if ((productUUID and 0xff) == 0xff || (productUUID and 0xff) == 0x04) {
                            dbLightNew?.productUUID = 0x04
                        } else if ((productUUID and 0xff) == 0x06) {
                            dbLightNew?.productUUID = 0x06
                        }
                        dbLightNew.setConnectionStatus(connectionStatus.value)
                        dbLightNew.updateIcon()
                        dbLightNew.belongGroupId = DBUtils.groupNull?.id
                        dbLightNew.brightness = brightness
                        dbLightNew.color = 0
                        dbLightNew.colorTemperature = 0
                        dbLightNew.meshAddr = meshAddress
                        dbLightNew.name = getString(R.string.unnamed)
                        dbLightNew.macAddr = "0"
//                        Thread {
                            DBUtils.saveLight(dbLightNew, false)
//                            errorCheck(meshAddress)
//                        }.start()
                        com.dadoutek.uled.util.LogUtils.d("creat_light" + dbLightNew.meshAddr)
                    }
                }
            }

        }
    }

    private fun errorCheck(meshAddress: Int) {
        val list=DBUtils.getLightListByMeshAddr(meshAddress)
        if(list?.size?:0>1){
            for(i in list!!.indices){
                if(list[i].macAddr=="0"){
                    DBUtils.deleteLight(list[i])
                }
            }
        }
    }

    private fun onServiceConnected(event: ServiceEvent) {
//        LogUtils.d("onServiceConnected")
    }

    private fun onServiceDisconnected(event: ServiceEvent) {
        LogUtils.d("onServiceDisconnected")
        TelinkLightApplication.getInstance().startLightService(TelinkLightService::class.java)
    }


    /**
     * 事件处理方法
     *
     * @param event
     */
    override fun performed(event: Event<String>) {
        when (event.type) {
            LeScanEvent.LE_SCAN -> onLeScan(event as LeScanEvent)
//            LeScanEvent.LE_SCAN_TIMEOUT -> onLeScanTimeout()
//            LeScanEvent.LE_SCAN_COMPLETED -> onLeScanTimeout()
            NotificationEvent.ONLINE_STATUS -> this.onOnlineStatusNotify(event as NotificationEvent)
            NotificationEvent.GET_ALARM -> {
            }
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
//            MeshEvent.OFFLINE -> this.onMeshOffline(event as MeshEvent)
            ServiceEvent.SERVICE_CONNECTED -> this.onServiceConnected(event as ServiceEvent)
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
//        if (mConnectSnackBar) {
        if (mNotFoundSnackBar?.isShown != true) {
            mNotFoundSnackBar = indefiniteSnackbar(root,
                    R.string.not_found_light, R.string.retry) {
                retryConnectCount = 0
                connectFailedDeviceMacList.clear()
                startScan()
            }
        }
//        } else {
//            retryConnect()
//        }

    }

    private fun isSwitch(uuid: Int): Boolean {
        return when (uuid) {
            DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SENSOR, DeviceType.NIGHT_LIGHT -> {
                LogUtils.d("This is switch")
                true
            }
            else -> {
                false

            }
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
        val meshAddress = mesh?.generateMeshAddr()
        val deviceInfo: DeviceInfo = event.args

        Thread {
            val dbLight = DBUtils.getLightByMeshAddr(deviceInfo.meshAddress)
            if (dbLight != null && dbLight.macAddr == "0") {
                dbLight.macAddr = deviceInfo.macAddress
                DBUtils.updateLight(dbLight)
            }
        }.start()

        if (!isSwitch(deviceInfo.productUUID) && !connectFailedDeviceMacList.contains(deviceInfo.macAddress)) {
//            connect(deviceInfo.macAddress)
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

    private fun checkIsLightForScan(productUUID: Int): Boolean {
        return !(productUUID == DeviceType.NORMAL_SWITCH ||
                productUUID == DeviceType.NORMAL_SWITCH2 ||
                productUUID == DeviceType.SCENE_SWITCH ||
                productUUID == DeviceType.SENSOR)
    }

    private fun onErrorReport(info: ErrorReportInfo) {
//        LogUtils.d("onErrorReport current device mac = ${bestRSSIDevice?.macAddress}")
        if (bestRSSIDevice != null) {
            connectFailedDeviceMacList.add(bestRSSIDevice!!.macAddress)
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
                retryConnect()

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
                retryConnect()

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
            if (secondTime - firstTime > 2000) {
                Toast.makeText(this@MainActivity, R.string.click_double_exit, Toast.LENGTH_SHORT).show()
                firstTime = secondTime
                return true
            } else {
//                System.exit(0)
                ActivityUtils.finishAllActivities(true)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

}
