package com.dadoutek.uled.switches

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.le.ScanFilter
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.*
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.*
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.connector.ScanningConnectorActivity
import com.dadoutek.uled.curtain.CurtainScanningNewActivity
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.intf.CallbackLinkMainActAndFragment
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.model.InstallDeviceModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.dadoutek.uled.windowcurtains.WindowCurtainsActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LeScanParameters
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.Strings
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_lights_of_group.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.activity_switch_device_details.*
import kotlinx.android.synthetic.main.fragment_new_device.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.indefiniteSnackbar
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1

class SwitchDeviceDetailsActivity : TelinkBaseActivity(), EventListener<String>,View.OnClickListener {

    override fun performed(event: Event<String>?) {

    }

    private lateinit var switchData:MutableList<DbSwitch>

    private var mScanTimeoutDisposal: Disposable? = null
    private var mCheckRssiDisposal: Disposable? = null

    private var retryConnectCount = 0

    private var mTelinkLightService: TelinkLightService? = null

    private val SCENE_MAX_COUNT = 16

    private val connectFailedDeviceMacList: MutableList<String> = mutableListOf()

    private var adapter:SwitchDeviceDetailsAdapter?=null

    private var currentLight: DbSwitch? = null

    private var positionCurrent: Int = 0

    private var mConnectDevice: DeviceInfo? = null

    private var acitivityIsAlive = true

    private var mScanDisposal: Disposable? = null

    private var bestRSSIDevice: DeviceInfo? = null

    private var mApplication: TelinkLightApplication? = null

    private var mConnectDisposal: Disposable? = null

    private var install_device: TextView? = null
    private var create_group: TextView? = null
    private var create_scene: TextView? = null

    private lateinit var stepOneText:TextView
    private lateinit var stepTwoText:TextView
    private lateinit var stepThreeText:TextView
    private lateinit var switchStepOne:TextView
    private lateinit var switchStepTwo:TextView
    private lateinit var swicthStepThree:TextView

    private var isRgbClick = false
//    private var isGuide = false

    private var installId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switch_device_details)
//        initData()
//        initView()
    }

    override fun onResume() {
        super.onResume()
        initData()
        initView()
    }

    private fun initData() {
        switchData=DBUtils.getAllSwitch()
        if(switchData.size>0){
            no_device_relativeLayout.visibility=View.GONE
            recycleView.visibility=View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
        }else{
            no_device_relativeLayout.visibility=View.VISIBLE
            recycleView.visibility=View.GONE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                if (dialog?.visibility == View.GONE) {
                    showPopupMenu()
                } else {
                    hidePopupMenu()
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.add_device_btn->addDevice()
        }
    }

    private fun addDevice() {
        startActivity(Intent(this, ScanningSwitchActivity::class.java))
    }

    private fun initView() {
        val layoutmanager = LinearLayoutManager(this)
        mConnectDevice = TelinkLightApplication.getInstance().connectDevice
        recycleView!!.layoutManager = GridLayoutManager(this,3)
        recycleView!!.itemAnimator = DefaultItemAnimator()
        adapter = SwitchDeviceDetailsAdapter(R.layout.device_detail_adapter, switchData)
        adapter!!.bindToRecyclerView(recycleView)

        adapter!!.onItemChildClickListener = onItemChildClickListener
//        for (i in switchData?.indices!!) {
////            switchData!![i].updateIcon()
//        }
//        toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
//        toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
//            if (dialog?.visibility == View.GONE) {
//                showPopupMenu()
//            } else {
//                hidePopupMenu()
//            }
//        }

        install_device = findViewById(R.id.install_device)
        create_group = findViewById(R.id.create_group)
        create_scene = findViewById(R.id.create_scene)
        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)

        add_device_btn.setOnClickListener(this)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.title=getString(R.string.switch_name) + " (" + switchData!!.size + ")"
    }

    private fun showPopupMenu() {
        dialog?.visibility = View.VISIBLE
    }

    private fun hidePopupMenu() {
//        if (!isGuide || GuideUtils.getCurrentViewIsEnd(activity!!, GuideUtils.END_GROUPLIST_KEY, false)) {
//            dialog_pop?.visibility = View.GONE
//        }
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentLight = switchData?.get(position)
        positionCurrent = position
        if (view.id == R.id.tv_setting) {
            AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_switch_confirm)
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        DBUtils.deleteSwitch(currentLight!!)
                         notifyData()
                        Toast.makeText(this@SwitchDeviceDetailsActivity,R.string.delete_switch_success,Toast.LENGTH_LONG).show()
                        if (TelinkLightService.Instance().adapter.mLightCtrl.currentLight.isConnected) {
                            val opcode = Opcode.KICK_OUT
                            TelinkLightService.Instance().sendCommandNoResponse(opcode, currentLight!!.getMeshAddr(), null)
                            if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(currentLight!!.getMeshAddr())) {
                                TelinkLightApplication.getApp().mesh.saveOrUpdate(this)
                            }
                            if (mConnectDevice != null) {
                                Log.d(this.javaClass.getSimpleName(), "mConnectDevice.meshAddress = " + mConnectDevice?.meshAddress)
                                Log.d(this.javaClass.getSimpleName(), "light.getMeshAddr() = " + currentLight?.getMeshAddr())
                                if (currentLight?.meshAddr == mConnectDevice?.meshAddress) {
                                    Log.d("NBA","=====")
//                                    scanPb.visibility = View.VISIBLE
                                    Thread {
                                        //踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
                                        Thread.sleep(2500)
                                        if (this@SwitchDeviceDetailsActivity == null ||
                                                this@SwitchDeviceDetailsActivity .isDestroyed ||
                                                this@SwitchDeviceDetailsActivity .isFinishing || !acitivityIsAlive) {
                                        } else {
                                            autoConnect()
                                        }
                                    }.start()
                                }
                            }


                        } else {
                            ToastUtils.showLong("当前处于未连接状态，重连中。。。")
                        }
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
        }
    }

    fun autoConnect() {
        //检查是否支持蓝牙设备
        if (!LeBluetooth.getInstance().isSupport(applicationContext)) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show()
            ActivityUtils.finishAllActivities()
        } else {  //如果蓝牙没开，则弹窗提示用户打开蓝牙
            if (!LeBluetooth.getInstance().isEnabled) {
                GlobalScope.launch(Dispatchers.Main) {
                    root.indefiniteSnackbar(R.string.openBluetooth, android.R.string.ok) {
                        LeBluetooth.getInstance().enable(applicationContext)
                    }
                }
            } else {
                //如果位置服务没打开，则提示用户打开位置服务
                if (!BleUtils.isLocationEnable(this)) {
                    GlobalScope.launch(Dispatchers.Main) {
                        showOpenLocationServiceDialog()
                    }
                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        hideLocationServiceDialog()
                    }
                    mTelinkLightService = TelinkLightService.Instance()
                    if (TelinkLightApplication.getInstance().connectDevice == null) {
                        while (TelinkApplication.getInstance()?.serviceStarted == true) {
                            GlobalScope.launch(Dispatchers.Main) {
                                retryConnectCount = 0
                                connectFailedDeviceMacList.clear()
                                startScan()
                            }
                            break
                        }

                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            scanPb?.visibility = View.GONE
                            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.CONNECT_STATE_SUCCESS_KEY, true);
                        }
                    }

                }
            }

        }
    }

    @SuppressLint("CheckResult")
    private fun startScan() {
        //当App在前台时，才进行扫描。
        if (AppUtils.isAppForeground())
            if (acitivityIsAlive || !(mScanDisposal?.isDisposed ?: false)) {
                LogUtils.d("startScanLight_LightOfGroup")
                mScanDisposal = RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN)
                        .subscribeOn(Schedulers.io())
                        .subscribe {
                            if (it) {
                                TelinkLightService.Instance().idleMode(true)
                                bestRSSIDevice = null   //扫描前置空信号最好设备。
                                //扫描参数
                                val account = DBUtils.lastUser?.account

                                val scanFilters = java.util.ArrayList<ScanFilter>()
                                val scanFilter = ScanFilter.Builder()
                                        .setDeviceName(account)
                                        .build()
                                scanFilters.add(scanFilter)

                                val params = LeScanParameters.create()
                                if (!com.dadoutek.uled.util.AppUtils.isExynosSoc()) {
                                    params.setScanFilters(scanFilters)
                                }
                                params.setMeshName(account)
                                params.setOutOfMeshName(account)
                                params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                                params.setScanMode(false)

                                addScanListeners()
                                TelinkLightService.Instance().startScan(params)
                                startCheckRSSITimer()

                            } else {
                                //没有授予权限
                                DialogUtils.showNoBlePermissionDialog(this, {
                                    retryConnectCount = 0
                                    startScan()
                                }, { finish() })
                            }
                        }
            }
    }

    private fun addScanListeners() {
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)
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


    private fun onLeScanTimeout() {
        LogUtils.d("onErrorReport: onLeScanTimeout")
//        if (mConnectSnackBar) {
//        indefiniteSnackbar(root, R.string.not_found_light, R.string.retry) {
        TelinkLightService.Instance().idleMode(true)
        LeBluetooth.getInstance().stopScan()
        startScan()
//        }
//        } else {
//            retryConnect()
//        }

    }

    @SuppressLint("CheckResult")
    private fun connect(mac: String) {
        try {
            mCheckRssiDisposal?.dispose()
            mCheckRssiDisposal= RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN)
                    .subscribe {
                        if (it) {
                            //授予了权限
                            if (TelinkLightService.Instance() != null) {
                                progressBar?.visibility = View.VISIBLE
                                TelinkLightService.Instance().connect(mac, CONNECT_TIMEOUT)
                                startConnectTimer()
                            }
                        } else {
                            //没有授予权限
                            DialogUtils.showNoBlePermissionDialog(this, { connect(mac) }, { finish() })
                        }
                    }
        }catch (e: Exception){
            e.printStackTrace()
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
            if (!scanPb.isShown) {
                retryConnectCount = 0
                connectFailedDeviceMacList.clear()
                startScan()
            }

        }
    }

    private fun login() {
        val account = DBUtils.lastUser?.account
        val pwd = NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16)
        TelinkLightService.Instance().login(Strings.stringToBytes(account, 16)
                , Strings.stringToBytes(pwd, 16))
    }

    var locationServiceDialog: AlertDialog? = null
    fun showOpenLocationServiceDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.open_location_service)
        builder.setNegativeButton(getString(android.R.string.ok)) { dialog, which ->
            BleUtils.jumpLocationSetting()
        }
        locationServiceDialog = builder.create()
        locationServiceDialog?.setCancelable(false)
        locationServiceDialog?.show()
    }

    fun hideLocationServiceDialog() {
        locationServiceDialog?.hide()
    }

    fun notifyData() {
        val mOldDatas: MutableList<DbSwitch>? = switchData
        val mNewDatas: MutableList<DbSwitch>? = getNewData()
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return mOldDatas?.get(oldItemPosition)?.id?.equals(mNewDatas?.get
                (newItemPosition)?.id) ?: false
            }

            override fun getOldListSize(): Int {
                return mOldDatas?.size ?: 0
            }

            override fun getNewListSize(): Int {
                return mNewDatas?.size ?: 0
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val beanOld = mOldDatas?.get(oldItemPosition)
                val beanNew = mNewDatas?.get(newItemPosition)
                return if (!beanOld?.name.equals(beanNew?.name)) {
                    return false//如果有内容不同，就返回false
                } else true

            }
        }, true)
        adapter?.let { diffResult.dispatchUpdatesTo(it) }
        switchData = mNewDatas!!
        toolbar.title=getString(R.string.switch_name) + " (" + switchData!!.size + ")"
        adapter!!.setNewData(switchData)
    }

    private fun getNewData(): MutableList<DbSwitch>{
//        if (currentLight!!.meshAddr == 0xffff) {
//            //            lightList = DBUtils.getAllLight();
////            lightList=DBUtils.getAllLight()
//            filter("", false)
//        } else {
            switchData = DBUtils.getAllSwitch()

//        }

//        if (currentLight!!.meshAddr == 0xffff) {
//            toolbar.title = getString(R.string.allLight) + " (" + switchData!!.size + ")"
//        } else {
            toolbar.title = (currentLight!!.name ?: "")
//        }
        return switchData
    }

    override fun onDestroy() {
        super.onDestroy()
        acitivityIsAlive=false
    }

    private fun filter(groupName: String?, isSearch: Boolean) {
        val list = DBUtils.groupList
//        val nameList : ArrayList<String> = ArrayList()
        if (switchData != null && switchData!!.size > 0) {
            switchData!!.clear()
        }

//        for(i in list.indices){
//            nameList.add(list[i].name)
//        }
    }

    private val onClick = View.OnClickListener {
        var intent: Intent? = null
        //点击任何一个选项跳转页面都隐藏引导
//        val controller=guide2()
//            controller?.remove()
        hidePopupMenu()
        when (it.id) {
            R.id.install_device -> {
                showInstallDeviceList()
            }
            R.id.create_group -> {
                dialog?.visibility = View.GONE
                if (TelinkLightApplication.getInstance().connectDevice==null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    addNewGroup()
                }
            }
            R.id.create_scene -> {
                dialog?.visibility = View.GONE
                val nowSize = DBUtils.sceneList.size
                if (TelinkLightApplication.getInstance().connectDevice==null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    if (nowSize >= SCENE_MAX_COUNT) {
                        ToastUtils.showLong(R.string.scene_16_tip)
                    } else {
                        val intent = Intent(this, NewSceneSetAct::class.java)
                        intent.putExtra(Constant.IS_CHANGE_SCENE, false)
                        startActivityForResult(intent, CREATE_SCENE_REQUESTCODE)
                    }
                }
            }
        }
    }

    private fun showInstallDeviceList() {
        dialog.visibility=View.GONE
//        callbackLinkMainActAndFragment?.showDeviceListDialog(isGuide,isRgbClick)
        showInstallDeviceList(isGuide,isRgbClick)
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
        val decoration = DividerItemDecoration(this,
                DividerItemDecoration
                        .VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color
                .divider)))
        //添加分割线
        install_device_recyclerView?.addItemDecoration(decoration)

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
//                guide3(install_device_recyclerView)
            }
        }.start()
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

    private fun addNewGroup() {
//        dialog?.visibility = View.GONE
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(DBUtils.getDefaultNewGroupName())
        //设置光标默认在最后
        textGp.setSelection(textGp.getText().toString().length)
        android.app.AlertDialog.Builder(this)
                .setTitle(R.string.create_new_group)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        //往DB里添加组数据
                        DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, DBUtils.groupList, Constant.DEVICE_TYPE_DEFAULT_ALL,this)
//                        callbackLinkMainActAndFragment?.changeToGroup()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }

    val CREATE_SCENE_REQUESTCODE = 2
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        refreshData()
        if(requestCode==CREATE_SCENE_REQUESTCODE){
//            callbackLinkMainActAndFragment?.changeToScene()
        }
    }
}
