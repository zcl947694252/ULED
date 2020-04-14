package com.dadoutek.uled.othersview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.allenliu.versionchecklib.v2.AllenVersionChecker
import com.allenliu.versionchecklib.v2.builder.UIData
import com.allenliu.versionchecklib.v2.callback.CustomVersionDialogListener
import com.app.hubert.guide.core.Controller
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.AppUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.device.DeviceFragment
import com.dadoutek.uled.fragment.MeFragment
import com.dadoutek.uled.group.GroupListFragment
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.intf.CallbackLinkMainActAndFragment
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.Constant.DEFAULT_MESH_FACTORY_NAME
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.HttpModel.RegionModel
import com.dadoutek.uled.model.HttpModel.UpdateModel
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.network.VersionBean
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.region.bean.RegionBean
import com.dadoutek.uled.scene.SceneFragment
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.widget.BaseUpDateDialog
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.event.NotificationEvent
import com.telink.bluetooth.event.ServiceEvent
import com.telink.bluetooth.light.LightAdapter
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.MeshUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private var retryDisposable: Disposable? = null
    private lateinit var receiver: HomeKeyEventBroadCastReceiver
    private val mCompositeDisposable = CompositeDisposable()
    private var disposableCamera: Disposable? = null

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

    private var guideShowCurrentPage = false
    private var installId = 0

    private lateinit var stepOneText: TextView
    private lateinit var stepTwoText: TextView
    private lateinit var stepThreeText: TextView
    private lateinit var switchStepOne: TextView
    private lateinit var switchStepTwo: TextView
    private lateinit var swicthStepThree: TextView
    private lateinit var stepThreeTextSmall: TextView

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                    BluetoothAdapter.STATE_ON -> {
                        retryConnectCount = 0
                        autoConnect()
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
        // detectUpdate()
        TelinkLightApplication.getApp().initStompClient()
        LogUtils.v("zcl首页---oncreate")
        this.setContentView(R.layout.activity_main)
        this.mApplication = this.application as TelinkLightApplication
        if (Constant.isDebug) {//如果是debug模式可以切换 并且显示
            when (SharedPreferencesHelper.getInt(this, Constant.IS_TECK, 0)) {
                0 -> DEFAULT_MESH_FACTORY_NAME = "dadousmart"
                1 -> DEFAULT_MESH_FACTORY_NAME = "dadoutek3"
                2 -> DEFAULT_MESH_FACTORY_NAME = "dadourd"
            }
            Constant.PIR_SWITCH_MESH_NAME = DEFAULT_MESH_FACTORY_NAME
            main_toast.visibility = VISIBLE
        } else {
            main_toast.visibility = GONE
        }
        main_toast.text = DEFAULT_MESH_FACTORY_NAME
        main_toast.setOnClickListener {
            val intent = Intent(this@MainActivity, ExtendActivity::class.java)
            startActivity(intent)
        }
        initBottomNavigation()

        checkVersionAvailable()

        receiver = HomeKeyEventBroadCastReceiver()
        registerReceiver(receiver, IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

        getRegionList()
    }

    @SuppressLint("CheckResult")
    private fun getRegionList() {
        val list = mutableListOf<String>()
        RegionModel.get()?.subscribe(object : NetworkObserver<MutableList<RegionBean>?>() {
            override fun onNext(t: MutableList<RegionBean>) {
                for (i in t) {
                    i.controlMesh?.let { it -> list.add(it) }
                }
                SharedPreferencesUtils.saveRegionNameList(list)

            }
        })
    }

    private fun getGwId(): Long {
        val list = DBUtils.getAllGateWay()
        val idList = ArrayList<Int>()
        for (i in list.indices) {
            idList.add(list[i].id!!.toInt())
        }

        var id = 0
        if (list.size == 0) {
            id = 1
        } else {
            for (i in 1..10000) {
                if (idList.contains(i)) {
                    Log.d("gwID", "getGwId: " + "aaaaa")
                    continue
                } else {
                    id = i
                    Log.d("sceneID", "getGwId: bbbbb$id")
                    break
                }
            }
        }
        return java.lang.Long.valueOf(id.toLong())
    }

    private fun startToRecoverDevices() {
        LogUtils.v("zcl------找回controlMeshName:${DBUtils.lastUser?.controlMeshName}")
        val disposable = RecoverMeshDeviceUtil.findMeshDevice(DBUtils.lastUser?.controlMeshName)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        {
                            //                            LogUtils.d("zcl--重加-----added device $it ")
                            deviceFragment.refreshView()
                        },
                        {
                            LogUtils.d(it)
//                            LogUtils.d("zcl--重加-----added device throwable$it")
                        },
                        {
                            //                            LogUtils.d("zcl--重加-----added device complete")
//                            LogUtils.d("added mesh devices complete")
                            //connect()
                        })
        mCompositeDisposable.add(disposable)
    }

    /**
     * 检查App是否有新版本
     */
    @Deprecated("we don't need call this function manually")
    private fun detectUpdate() {
//        XiaomiUpdateAgent.setCheckUpdateOnlyWifi(false)
//        XiaomiUpdateAgent.update(this)
    }


    var syncCallback: SyncCallback = object : SyncCallback {
        override fun start() {
            showLoadingDialog(getString(R.string.tip_start_sync))
        }

        override fun complete() {
            hideLoadingDialog()
            AlertDialog.Builder(this@MainActivity)
                    .setCancelable(false)
                    .setMessage(getString(R.string.version_disabled))
                    .setPositiveButton(R.string.exit) { dialog, which ->
                        LogOutAndExitApp()
                    }
                    .show()
        }

        override fun error(msg: String) {
            hideLoadingDialog()
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
        this.mApplication?.removeEventListeners()
        TelinkLightService.Instance()?.idleMode(true)
        //重启app并杀死原进程
        ActivityUtils.finishAllActivities(true)
        ActivityUtils.startActivity(SplashActivity::class.java)
    }

    //防止viewpager嵌套fragment,fragment放置后台时间过长,fragment被系统回收了
    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        try {
            if (ev?.action == MotionEvent.ACTION_DOWN) {
                when {
                    bnve.currentItem == 0 -> deviceFragment.myPopViewClickPosition(ev.x, ev.y)
                    bnve.currentItem == 1 -> groupFragment.myPopViewClickPosition(ev.x, ev.y)
                    bnve.currentItem == 2 -> sceneFragment.myPopViewClickPosition(ev.x, ev.y)
                }
            }
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
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

        GlobalScope.launch {
            delay(100)
            GlobalScope.launch(Dispatchers.Main) {
                guide3(install_device_recyclerView)
            }
        }
    }

    private fun showInstallDeviceDetail(describe: String, position: Int) {
        val view = View.inflate(this, R.layout.dialog_install_detail, null)
        val close_install_list = view.findViewById<ImageView>(R.id.close_install_list)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        stepOneText = view.findViewById(R.id.step_one)
        stepTwoText = view.findViewById(R.id.step_two)
        stepThreeText = view.findViewById(R.id.step_three)
        stepThreeTextSmall = view.findViewById(R.id.step_three_small)
        switchStepOne = view.findViewById(R.id.switch_step_one)
        switchStepTwo = view.findViewById(R.id.switch_step_two)
        swicthStepThree = view.findViewById(R.id.switch_step_three)
        val install_tip_question = view.findViewById<TextView>(R.id.install_tip_question)
        val search_bar = view.findViewById<Button>(R.id.search_bar)
        close_install_list.setOnClickListener(dialogOnclick)
        btnBack.setOnClickListener(dialogOnclick)
        search_bar.setOnClickListener(dialogOnclick)

        if (position== Constant.INSTALL_SWITCH)
            stepThreeTextSmall.visibility = View.VISIBLE
        else
            stepThreeTextSmall.visibility = View.GONE

        val title = view.findViewById<TextView>(R.id.textView5)
        if (position == INSTALL_NORMAL_LIGHT) {
            title.visibility = GONE
            install_tip_question.visibility = GONE
        } else {
            title.visibility = VISIBLE
            install_tip_question.visibility = VISIBLE
        }


        install_tip_question.text = describe
        install_tip_question.movementMethod = ScrollingMovementMethod.getInstance()
        installDialog = AlertDialog.Builder(this).setView(view).create()
        installDialog?.setOnShowListener {}
        installDialog?.show()
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
            R.id.close_install_list -> {
                installDialog?.dismiss()
            }
            R.id.search_bar -> {
                when (installId) {
                    INSTALL_NORMAL_LIGHT -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_RGB_LIGHT -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_RGB)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_CURTAIN -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_CURTAIN)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_SWITCH -> {
                        //intent = Intent(this, DeviceScanningNewActivity::class.java)
                        //intent.putExtra(Constant.DEVICE_TYPE, DeviceType.NORMAL_SWITCH)
                        //startActivityForResult(intent, 0)
                        startActivity(Intent(this, ScanningSwitchActivity::class.java))
                    }
                    INSTALL_SENSOR -> {
                        startActivity(Intent(this, ScanningSensorActivity::class.java))
                    }
                    INSTALL_CONNECTOR -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_RELAY)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }

                    INSTALL_GATEWAY -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.GATE_WAY)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
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

    private val INSTALL_NORMAL_LIGHT = 0
    private val INSTALL_RGB_LIGHT = 1
    private val INSTALL_SWITCH = 2
    private val INSTALL_SENSOR = 3
    private val INSTALL_CURTAIN = 4
    private val INSTALL_CONNECTOR = 5
    val INSTALL_GATEWAY = 6

    private val onItemClickListenerInstallList = BaseQuickAdapter.OnItemClickListener { _, _, position ->
        isGuide = false
        installDialog?.dismiss()
        when (position) {
            INSTALL_GATEWAY -> {
                installId = INSTALL_GATEWAY
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_NORMAL_LIGHT -> {
                installId = INSTALL_NORMAL_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_RGB_LIGHT -> {
                installId = INSTALL_RGB_LIGHT

                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_CURTAIN -> {
                installId = INSTALL_CURTAIN
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_SWITCH -> {
                installId = INSTALL_SWITCH
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
                stepOneText.visibility = GONE
                stepTwoText.visibility = GONE
                stepThreeText.visibility = GONE
                switchStepOne.visibility = VISIBLE
                switchStepTwo.visibility = VISIBLE
                swicthStepThree.visibility = VISIBLE
            }
            INSTALL_SENSOR -> {
                installId = INSTALL_SENSOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_CONNECTOR -> {
                installId = INSTALL_CONNECTOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
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
     * 检查App版本是否可用  报用户不存在  版本失败
     *  <-- 200  http://47.107.227.130/smartlight_test/app/isAvailable?platform=0&currentVersion=3.3.1 (26ms)
    2019-10-12 15:29:57.077 7283-11765/com.dadoutek.uled D/OkHttp: Server: nginx/1.14.0 (Ubuntu)
    2019-10-12 15:29:57.077 7283-11765/com.dadoutek.uled D/OkHttp: Date: Sat, 12 Oct 2019 07:29:57 GMT
    2019-10-12 15:29:57.077 7283-11765/com.dadoutek.uled D/OkHttp: Content-Type: application/json;charset=UTF-8
    2019-10-12 15:29:57.077 7283-11765/com.dadoutek.uled D/OkHttp: Content-Length: 92
    2019-10-12 15:29:57.077 7283-11765/com.dadoutek.uled D/OkHttp: Connection: keep-alive
    2019-10-12 15:29:57.077 7283-11765/com.dadoutek.uled D/OkHttp: {"data":null,"errorCode":20001,"serverTime":1570865397299,"message":"20001 用户不存在"}
     */
    private fun checkVersionAvailable() {
        var version = packageName(this)
        UpdateModel.run {
            isVersionAvailable(0, version)
                    .subscribe(object : NetworkObserver<ResponseVersionAvailable>() {
                        override fun onNext(s: ResponseVersionAvailable) {
                            if (!s.isUsable) {
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
    }

    private fun createCustomDialogOne(t: VersionBean): CustomVersionDialogListener {
        return CustomVersionDialogListener { _, _ ->
            var upDateDialog = BaseUpDateDialog(this, R.style.BaseDialog, R.layout.custom_dialog_two_layout)
            upDateDialog.findViewById<TextView>(R.id.tv_msg).text = t.description
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
        uiData.downloadUrl = v.url
        uiData.content = v.description
        return uiData
    }

    private fun packageName(context: Context): String {
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
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
            }

            override fun onPageSelected(p0: Int) {
                val intent = Intent("isDelete")
                intent.putExtra("isDelete", "true")
                LocalBroadcastManager.getInstance(this@MainActivity).sendBroadcast(intent)
            }
        })
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
        disableConnectionStatusListener()
        mCompositeDisposable.clear()
        mScanTimeoutDisposal?.dispose()
        mConnectDisposal?.dispose()
        progressBar.visibility = GONE
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
        if (TelinkLightService.Instance() == null)
            mApplication?.startLightService(TelinkLightService::class.java)
        startToRecoverDevices()
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
        registerReceiver(mReceiver, filter)
    }

    override fun onPostResume() {
        super.onPostResume()
//        LogUtils.v("zcl--重加---------onPostResume")
        deviceFragment.refreshView()
        groupFragment.refreshView()
        sceneFragment.refreshView()
        autoConnect()
    }


    override fun changeToScene() {
        transScene()
    }

    override fun changeToGroup() {
        tranHome()
    }


    private fun onLogin() {
        mScanTimeoutDisposal?.dispose()
        GlobalScope.launch(Dispatchers.Main) {
            stopConnectTimer()
            if (progressBar?.visibility != GONE)
                progressBar?.visibility = GONE
        }

        val connectDevice = this.mApplication?.connectDevice
        if (connectDevice != null) {
            this.connectMeshAddress = connectDevice.meshAddress
        }
    }


    @SuppressLint("CheckResult")
    fun autoConnect() {
        //如果支持蓝牙就打开蓝牙
       // if (LeBluetooth.getInstance().isSupport(applicationContext))
            //LeBluetooth.getInstance().enable(applicationContext)    //如果没打开蓝牙，就提示用户打开

        //如果位置服务没打开，则提示用户打开位置服务，bleScan必须
        if (!BleUtils.isLocationEnable(this)) {
            showOpenLocationServiceDialog()
        } else {
            hideLocationServiceDialog()
            //LogUtils.v("zcl--重加---------isstart"+TelinkApplication.getInstance()?.serviceStarted)
            if (TelinkApplication.getInstance()?.serviceStarted == true) {
                RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            if (TelinkLightApplication.getApp().connectDevice == null) {
                                ToastUtils.showLong(R.string.connecting_please_wait)

                                val deviceTypes = mutableListOf(DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB,
                                        DeviceType.SMART_RELAY, DeviceType.SMART_CURTAIN)
                                mConnectDisposal = connect(deviceTypes = deviceTypes, retryTimes = 10)
                                        ?.subscribe(
                                                {
                                                    RecoverMeshDeviceUtil.addDevicesToDb(it)
                                                    onLogin()
                                                },
                                                {
                                                    LogUtils.d("connect failed, reason = $it")
                                                }
                                        )
                            }
                        }, { LogUtils.d(it) })
            } else {
                this.mApplication?.startLightService(TelinkLightService::class.java)
                autoConnect()
            }
        }

        val deviceInfo = this.mApplication?.connectDevice
        if (deviceInfo != null) {
            this.connectMeshAddress = (this.mApplication?.connectDevice?.meshAddress
                    ?: 0x00) and 0xFF
        }
    }

    private fun stopConnectTimer() {
        mConnectDisposal?.dispose()
    }


    private fun onNError(event: DeviceEvent) {
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
            TelinkLightService.Instance().idleMode(true)
            ToastUtils.showLong(getString(R.string.reconnecting))
            retryDisposable?.dispose()
            retryDisposable = Observable.timer(1000, TimeUnit.MILLISECONDS)
                    .subscribe {
                        autoConnect()
                    }
        } else {
            LogUtils.d("exceed max retry time, show connection error")
            //不调用断开，这样它就会在后台一直重连
//            TelinkLightService.Instance()?.idleMode(true)
        }
    }

    override fun onStop() {
        super.onStop()
        TelinkLightService.Instance()?.disableAutoRefreshNotify()
        installDialog?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()

        TelinkLightApplication.getApp().releseStomp()
        unregisterReceiver(receiver)
        //移除事件
        TelinkLightService.Instance()?.idleMode(true)
        this.mDelayHandler.removeCallbacksAndMessages(null)
        Lights.getInstance().clear()

        mDisposable.dispose()
        disposableCamera?.dispose()
        mCompositeDisposable.dispose()

        AllenVersionChecker.getInstance().cancelAllMission(this)
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args
        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                mScanTimeoutDisposal?.dispose()
                GlobalScope.launch(Dispatchers.Main) {
                    stopConnectTimer()
                    if (progressBar?.visibility != GONE)
                        progressBar?.visibility = GONE
                }

                val connectDevice = this.mApplication?.connectDevice
                if (connectDevice != null) {
                    this.connectMeshAddress = connectDevice.meshAddress
                }

            }
            LightAdapter.STATUS_LOGOUT -> {
                retryConnect()
            }

            LightAdapter.STATUS_ERROR_N -> onNError(event)
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
//            DeviceEvent.STATUS_CHANGED -> {
//                progressBar.visibility = GONE
//                this.onDeviceStatusChanged(event as DeviceEvent)
//            }
            ServiceEvent.SERVICE_CONNECTED -> {
                this.onServiceConnected(event as ServiceEvent)
            }
            ServiceEvent.SERVICE_DISCONNECTED -> this.onServiceDisconnected(event as ServiceEvent)
            NotificationEvent.GET_DEVICE_STATE -> onNotificationEvent(event as NotificationEvent)
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReportNormal(info)
            }
        }
    }


    private fun onNotificationEvent(event: NotificationEvent) {
        if (!AppUtils.isAppForeground()) return
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
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
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
                    TelinkLightApplication.getApp().releseStomp()
                    ActivityUtils.finishAllActivities(true)
                    TelinkApplication.getInstance().removeEventListeners()
                    //TelinkLightApplication.getApp().doDestroy()
                    finish()
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }


    class HomeKeyEventBroadCastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            /*   TelinkLightApplication.getApp().releseStomp()
               ActivityUtils.finishAllActivities(true)
               TelinkApplication.getInstance().removeEventListeners()
               TelinkLightApplication.getApp().doDestroy()*/
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }


}


