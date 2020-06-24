package com.dadoutek.uled.connector

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import butterknife.ButterKnife
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbConnector
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.rgb.RGBSettingActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.BleUtils
import com.dadoutek.uled.util.DataManager
import com.dadoutek.uled.util.DialogUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.*
import com.telink.bluetooth.light.*
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.Strings
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_connector_of_group.*
import kotlinx.android.synthetic.main.activity_lights_of_group.no_light
import kotlinx.android.synthetic.main.activity_lights_of_group.recycler_view_lights
import kotlinx.android.synthetic.main.activity_lights_of_group.scanPb
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.design.indefiniteSnackbar
import java.util.*
import java.util.concurrent.TimeUnit

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1

class ConnectorOfGroupActivity : TelinkBaseActivity(), EventListener<String>, SearchView.OnQueryTextListener, View.OnClickListener {
    private val REQ_LIGHT_SETTING: Int = 0x01

    private lateinit var group: DbGroup
    private var mDataManager: DataManager? = null
    private var mApplication: TelinkLightApplication? = null
    private lateinit var lightList: MutableList<DbConnector>
    private var adapter: ConnectorOfGroupRecyclerViewAdapter? = null
    private var positionCurrent: Int = 0
    private var currentLight: DbConnector? = null
    private var searchView: SearchView? = null
    private var canBeRefresh = true
    private var bestRSSIDevice: DeviceInfo? = null
    private var connectMeshAddress: Int = 0
    private var retryConnectCount = 0
    private val connectFailedDeviceMacList: MutableList<String> = mutableListOf()
    private var mConnectDisposal: Disposable? = null
    private var mScanDisposal: Disposable? = null
    private var mScanTimeoutDisposal: Disposable? = null
    private var mCheckRssiDisposal: Disposable? = null
    private var mNotFoundSnackBar: Snackbar? = null
    private var acitivityIsAlive = true
    private var recyclerView: RecyclerView? = null


    override fun onPostResume() {
        super.onPostResume()
        addListeners()
    }

    private fun addListeners() {
        addScanListeners()
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connector_of_group)
        ButterKnife.bind(this)
        initToolbar()
        initParameter()
        initData()
        initView()
        initOnLayoutListener()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.light_add_device_btns -> {
                DBUtils.lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showLong(getString(R.string.author_region_warm))
                    else {
                        if (DBUtils.getAllRelay().size == 0) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_RELAY)
                            startActivityForResult(intent, 0)
                        } else {
                            addDevice()
                        }
                    }
                }

            }
        }
    }

    private fun addDevice() {
        val intent = Intent(this,
                ConnectorBatchGroupActivity::class.java)
        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
        intent.putExtra("relayType", "group_relay")
        intent.putExtra("relay_group_name", group.name)
        startActivity(intent)
        finish()
    }

    override fun initOnLayoutListener() {
        val view = window.decorView
        val viewTreeObserver = view.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    private fun initToolbar() {
        toolbar.setTitle(R.string.group_setting_header)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchView!!.clearFocus()
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null && !newText.isEmpty()) {
            filter(newText, true)
            adapter!!.notifyDataSetChanged()
        } else {
            filter(newText, false)
            adapter!!.notifyDataSetChanged()
        }

        return false
    }


    private fun filter(groupName: String?, isSearch: Boolean) {
        val list = DBUtils.groupList
        if (lightList != null && lightList.size > 0) {
            lightList.clear()
        }

        if (isSearch) {
            for (i in list.indices) {
                if (groupName == list[i].name || (list[i].name).startsWith(groupName!!)) {
                    lightList.addAll(DBUtils.getConnectorByGroupID(list[i].id))
                }
            }

        } else {
            for (i in list.indices) {
                if (list[i].meshAddr == 0xffff) {
                    Collections.swap(list, 0, i)
                }
            }

            for (j in list.indices) {
                lightList.addAll(DBUtils.getConnectorByGroupID(list[j].id))
            }
        }
    }

    private fun initParameter() {
        val get = this.intent.extras!!.get("group")
        if (null != get)
            this.group = get as DbGroup
        this.mApplication = this.application as TelinkLightApplication
        mDataManager = DataManager(this, mApplication!!.mesh.name, mApplication!!.mesh.password)
    }

    override fun onResume() {
        super.onResume()
        initToolbar()
        initParameter()
        initData()
        initView()
        initOnLayoutListener()
    }

    override fun onStop() {
        super.onStop()
        this.mApplication!!.removeEventListener(this)
        if (TelinkLightService.Instance() != null)
            TelinkLightService.Instance()?.disableAutoRefreshNotify()
    }

    override fun onDestroy() {
        super.onDestroy()
        canBeRefresh = false
        acitivityIsAlive = false
        mScanDisposal?.dispose()
        if (TelinkLightApplication.getApp().connectDevice == null) {
            //TelinkLightService.Instance()?.idleMode(true)
            LeBluetooth.getInstance().stopScan()
        }
    }

    private fun initData() {
        lightList = ArrayList()
        if (group.meshAddr == 0xffff) {
            filter("", false)
        } else {
            lightList = DBUtils.getConnectorByGroupID(group.id)
        }

        if (lightList.size > 0) {
            recycler_view_lights.visibility = View.VISIBLE
            no_light.visibility = View.GONE
            var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
            toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
            batchGroup.setText(R.string.batch_group)
            batchGroup.visibility = View.GONE
            batchGroup.setOnClickListener {
                val intent = Intent(this, ConnectorBatchGroupActivity::class.java)
                intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                intent.putExtra("relayType", "relayGroup")
                intent.putExtra("group", group.id.toInt())
                startActivity(intent)
            }
        } else {
            recycler_view_lights.visibility = View.GONE
            no_light.visibility = View.VISIBLE
            toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
        }
    }

    private fun getNewData(): MutableList<DbConnector> {
        if (group.meshAddr == 0xffff) {
            filter("", false)
        } else {
            lightList = DBUtils.getConnectorByGroupID(group.id)
        }

        if (group.meshAddr == 0xffff) {
            toolbarTv.text = getString(R.string.allLight) + " (" + lightList.size + ")"
        } else {
            toolbarTv.text = (group.name ?: "") + " (" + lightList.size + ")"
        }
        return lightList
    }

    fun notifyData() {
        val mOldDatas: MutableList<DbConnector>? = lightList
        val mNewDatas: MutableList<DbConnector>? = getNewData()
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return mOldDatas?.get(oldItemPosition)?.id?.equals(mNewDatas?.get
                (newItemPosition)?.id) ?: false;
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
        lightList = mNewDatas!!
        adapter?.setNewData(lightList)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (group.meshAddr == 0xffff) {
            menuInflater.inflate(R.menu.menu_search, menu)
            searchView = (menu!!.findItem(R.id.action_search)).actionView as SearchView
            searchView!!.setOnQueryTextListener(this)
            searchView!!.imeOptions = EditorInfo.IME_ACTION_SEARCH
            searchView!!.queryHint = getString(R.string.input_groupAdress)
            searchView!!.isSubmitButtonEnabled = true
            searchView!!.backgroundColor = resources.getColor(R.color.blue)
            searchView!!.alpha = 0.3f
            return super.onCreateOptionsMenu(menu)
        }
        return true
    }


    private fun initView() {
        if (group.meshAddr == 0xffff) {
            toolbarTv.text = getString(R.string.allLight) + " (" + lightList.size + ")"
        } else {
            toolbarTv.text = (group.name ?: "") + " (" + lightList.size + ")"
        }
        light_add_device_btns.setOnClickListener(this)
        recyclerView = findViewById(R.id.recycler_view_lights)
        recyclerView!!.layoutManager = GridLayoutManager(this, 3)
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        adapter = ConnectorOfGroupRecyclerViewAdapter(R.layout.item_lights_of_group, lightList)
        adapter!!.onItemChildClickListener = onItemChildClickListener
        adapter!!.bindToRecyclerView(recyclerView)
        for (i in lightList.indices) {
            lightList[i].updateIcon()
        }

        if (DBUtils.getAllRelay().size == 0) {
            light_add_device_btns.text = getString(R.string.device_scan_scan)
        } else {
            light_add_device_btns.text = getString(R.string.add_device)
        }
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentLight = lightList[position]
        positionCurrent = position
        if (view.id == R.id.img_light) {
            canBeRefresh = true
            if (currentLight!!.connectionStatus == ConnectionStatus.OFF.value) {
                if (currentLight!!.productUUID == DeviceType.SMART_CURTAIN) {
                    Commander.openOrCloseCurtain(currentLight!!.meshAddr, true, false)
                } else {
                    Commander.openOrCloseLights(currentLight!!.meshAddr, true)
                }

                currentLight!!.connectionStatus = ConnectionStatus.ON.value
            } else {
                if (currentLight!!.productUUID == DeviceType.SMART_CURTAIN) {
                    Commander.openOrCloseCurtain(currentLight!!.meshAddr, false, false)
                } else {
                    Commander.openOrCloseLights(currentLight!!.meshAddr, false)
                }
                currentLight!!.connectionStatus = ConnectionStatus.OFF.value
            }

            currentLight!!.updateIcon()
            DBUtils.updateConnector(currentLight!!)
            runOnUiThread {
                adapter?.notifyDataSetChanged()
            }
        } else
            if (view.id == R.id.tv_setting) {

                val lastUser = DBUtils.lastUser
                lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showLong(getString(R.string.author_region_warm))
                    else {
                        if (scanPb.visibility != View.VISIBLE) {
                            //判断是否为rgb灯
                            var intent = Intent(this@ConnectorOfGroupActivity, ConnectorSettingActivity::class.java)
                            if (currentLight?.productUUID == DeviceType.LIGHT_RGB) {
                                intent = Intent(this@ConnectorOfGroupActivity, RGBSettingActivity::class.java)
                                intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_LIGHT)
                            }
                            intent.putExtra(Constant.LIGHT_ARESS_KEY, currentLight)
                            intent.putExtra(Constant.GROUP_ARESS_KEY, group.meshAddr)
                            intent.putExtra(Constant.LIGHT_REFRESH_KEY, Constant.LIGHT_REFRESH_KEY_OK)
                            startActivityForResult(intent, REQ_LIGHT_SETTING)
                        } else {
                            ToastUtils.showLong(R.string.reconnecting)
                        }
                    }
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        notifyData()
        val isConnect = data?.getBooleanExtra("data", false) ?: false
        if (isConnect) {
            scanPb.visibility = View.VISIBLE
        }

        Thread {
            //踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
            Thread.sleep(2500)
            if (this@ConnectorOfGroupActivity == null ||
                    this@ConnectorOfGroupActivity.isDestroyed ||
                    this@ConnectorOfGroupActivity.isFinishing || !acitivityIsAlive) {
            } else {
                autoConnect()
            }
        }.start()
    }

    /**********************************telink part***************************************************/

    override fun performed(event: Event<String>) {
        when (event.type) {
            LeScanEvent.LE_SCAN -> onLeScan(event as LeScanEvent)
//            LeScanEvent.LE_SCAN_TIMEOUT -> onLeScanTimeout()
//            LeScanEvent.LE_SCAN_COMPLETED -> onLeScanTimeout()
//            NotificationEvent.ONLINE_STATUS -> this.onOnlineStatusNotify(event as NotificationEvent)
            NotificationEvent.GET_ALARM -> {
            }
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
//            MeshEvent.OFFLINE -> this.onMeshOffline(event as MeshEvent)
            ServiceEvent.SERVICE_CONNECTED -> this.onServiceConnected(event as ServiceEvent)
            ServiceEvent.SERVICE_DISCONNECTED -> this.onServiceDisconnected(event as ServiceEvent)
//            NotificationEvent.GET_DEVICE_STATE -> onNotificationEvent(event as NotificationEvent)
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
            }
        }
    }

    private fun onLogout() {

    }

    /**
     * 处理[NotificationEvent.ONLINE_STATUS]事件
     */
    private fun onOnlineStatusNotify(event: NotificationEvent) {

        if (canBeRefresh) {
            canBeRefresh = false
        } else {
            return
        }

        TelinkLog.i("MainActivity#onOnlineStatusNotify#Thread ID : " + Thread.currentThread().id)

        val notificationInfoList = event.parse() as List<OnlineStatusNotificationParser.DeviceNotificationInfo>

        if (notificationInfoList.isEmpty())
            return

        for (notificationInfo in notificationInfoList) {

            if (notificationInfo.meshAddress == TelinkApplication.getInstance().connectDevice.meshAddress) {
                currentLight?.textColor = ContextCompat.getColor(
                        this, R.color.primary)
            }

            for (dbLight in lightList) {
                if (notificationInfo.meshAddress == dbLight.meshAddr) {
                    dbLight.connectionStatus = notificationInfo.connectionStatus.value
                    dbLight.updateIcon()
                    DBUtils.updateConnector(dbLight)
                    runOnUiThread {
                        adapter?.notifyDataSetChanged()
                    }

                }
            }
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
                    if (TelinkLightApplication.getApp().connectDevice == null) {
                        while (TelinkApplication.getInstance()?.serviceStarted == true) {
                            GlobalScope.launch(Dispatchers.Main) {
                                retryConnectCount = 0
                                connectFailedDeviceMacList.clear()
                                startScan()
                            }
                            break;
                        }

                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            scanPb?.visibility = View.GONE
                            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), Constant.CONNECT_STATE_SUCCESS_KEY, true);
                        }
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
        //当App在前台时，才进行扫描。
        if (AppUtils.isAppForeground())
            if (acitivityIsAlive || !(mScanDisposal?.isDisposed ?: false)) {
                mScanDisposal = RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN)
                        .subscribeOn(Schedulers.io())
                        .subscribe {
                            if (it) {
                                TelinkLightService.Instance()?.idleMode(true)
                                bestRSSIDevice = null   //扫描前置空信号最好设备。
                                //扫描参数
                                val account = DBUtils.lastUser?.account

                                val scanFilters = ArrayList<ScanFilter>()
                                val scanFilter = ScanFilter.Builder()
                                        .setDeviceName(account)
                                        .build()
                                scanFilters.add(scanFilter)

                                val params = LeScanParameters.create()
                                //if (!com.dadoutek.uled.util.AppUtils.isExynosSoc)
                                params.setScanFilters(scanFilters)

                                params.setMeshName(account)
                                params.setOutOfMeshName(account)
                                params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                                params.setScanMode(false)

                                addScanListeners()
                                TelinkLightService.Instance()?.startScan(params)
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
        try {
            mCheckRssiDisposal?.dispose()
            mCheckRssiDisposal = RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN)
                    .subscribe {
                        if (it) {
                            //授予了权限
                            if (TelinkLightService.Instance() != null) {
                                progressBar?.visibility = View.VISIBLE
                                TelinkLightService.Instance()?.connect(mac, CONNECT_TIMEOUT)
                                startConnectTimer()
                            }
                        } else {
                            //没有授予权限
                            DialogUtils.showNoBlePermissionDialog(this, { connect(mac) }, { finish() })
                        }
                    }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 扫描不到任何设备了
     * （扫描结束）
     */
    private fun onLeScanTimeout() {
        ("onErrorReport: onLeScanTimeout")
//        if (mConnectSnackBar) {
//        indefiniteSnackbar(root, R.string.not_found_light, R.string.retry) {
        TelinkLightService.Instance()?.idleMode(true)
        LeBluetooth.getInstance().stopScan()
        startScan()
//        }
//        } else {
//            retryConnect()
//        }

    }

    private fun startCheckRSSITimer() {
        mScanTimeoutDisposal?.dispose()
        val periodCount = SCAN_TIMEOUT_SECOND.toLong() - SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND
        Observable.intervalRange(1, periodCount, SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND, 1,
                TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Long?> {
                    override fun onComplete() {
                        onLeScanTimeout()
                    }

                    override fun onSubscribe(d: Disposable) {
                        mScanTimeoutDisposal = d
                    }

                    override fun onNext(t: Long) {
                        if (bestRSSIDevice != null) {
                            mScanTimeoutDisposal?.dispose()
                            ("connect device , mac = ${bestRSSIDevice?.macAddress}  rssi = ${bestRSSIDevice?.rssi}")
                            connect(bestRSSIDevice!!.macAddress)
                        }
                    }

                    override fun onError(e: Throwable) {
                        Log.d("SawTest", "error = $e")
                    }
                })
    }

    private fun addScanListeners() {
        this.mApplication?.removeEventListener(this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)
    }

    private fun login() {
        val meshName = DBUtils.lastUser?.controlMeshName
        val pwd = NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16)
        TelinkLightService.Instance()?.login(Strings.stringToBytes(meshName, 16)
                , Strings.stringToBytes(pwd, 16))
    }

    private fun onNError(event: DeviceEvent) {

//        ToastUtils.showLong(getString(R.string.connect_fail))


        TelinkLightService.Instance()?.idleMode(true)
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
            if (TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight?.isConnected != true)
                startScan()
            else
                login()
        } else {
            TelinkLightService.Instance()?.idleMode(true)
            if (!scanPb.isShown) {
                retryConnectCount = 0
                connectFailedDeviceMacList.clear()
                startScan()
            }

        }
    }

    override fun onPause() {
        super.onPause()
        mScanTimeoutDisposal?.dispose()
        mConnectDisposal?.dispose()
        mNotFoundSnackBar?.dismiss()
        //移除事件
        this.mApplication?.removeEventListener(this)
        stopConnectTimer()
        mCheckRssiDisposal?.dispose()
//        try {
//            this.unregisterReceiver(mReceiver)
//        }catch (e:Exception){
//            e.printStackTrace()
//        }
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {

                TelinkLightService.Instance()?.enableNotification()
                TelinkLightService.Instance()?.updateNotification()
                GlobalScope.launch(Dispatchers.Main) {
                    stopConnectTimer()
                    if (progressBar?.visibility != View.GONE)
                        progressBar?.visibility = View.GONE
                    delay(300)
                }

                val connectDevice = this.mApplication?.connectDevice
                if (connectDevice != null) {
                    this.connectMeshAddress = connectDevice.meshAddress
                }

                scanPb.visibility = View.GONE
                adapter?.notifyDataSetChanged()

            }
            LightAdapter.STATUS_CONNECTING -> {
                Log.d("connectting", "444")
                scanPb.visibility = View.VISIBLE
            }
            LightAdapter.STATUS_CONNECTED -> {
                TelinkLightService.Instance() ?: return
                if (TelinkLightApplication.getApp().connectDevice == null)
                    login()
            }
            LightAdapter.STATUS_ERROR_N -> onNError(event)
        }
    }

    private fun onServiceConnected(event: ServiceEvent) {
//        ("onServiceConnected")
    }

    private fun onServiceDisconnected(event: ServiceEvent) {
        TelinkLightApplication.getApp().startLightService(TelinkLightService::class.java)
    }

    /**
     * 处理扫描事件
     *
     * @param event
     */
    @Synchronized
    private fun onLeScan(event: LeScanEvent) {
        val mesh = this.mApplication?.mesh
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
                    bestRSSIDevice = deviceInfo
                }
            } else {
                bestRSSIDevice = deviceInfo
            }

        }

    }

    private fun isSwitch(uuid: Int): Boolean {
        return when (uuid) {
            DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SENSOR, DeviceType.NIGHT_LIGHT -> {
                ("This is switch")
                true
            }
            else -> {
                false

            }
        }
    }

    private fun onErrorReport(info: ErrorReportInfo) {
//        ("onErrorReport current device mac = ${bestRSSIDevice?.macAddress}")
        if (bestRSSIDevice != null) {
            connectFailedDeviceMacList.add(bestRSSIDevice!!.macAddress)
        }
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        // ("蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        // ("无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        //("未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        ("未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        ("未建立物理连接")
                    }
                }
                retryConnect()

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        //("value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        //("read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        // ("write login data 没有收到response")
                    }
                }
                retryConnect()

            }
        }
    }
}
