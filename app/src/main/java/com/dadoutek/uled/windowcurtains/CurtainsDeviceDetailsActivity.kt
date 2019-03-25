package com.dadoutek.uled.windowcurtains

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.util.DiffUtil
import android.support.v7.widget.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.light.DeviceDetailListAdapter
import com.dadoutek.uled.light.NormalSettingActivity
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.rgb.RGBSettingActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.BleUtils
import com.dadoutek.uled.util.DialogUtils
import com.dadoutek.uled.util.LogUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.light.ConnectionStatus
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
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.indefiniteSnackbar
import java.lang.Exception
import java.util.concurrent.TimeUnit

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1


class CurtainsDeviceDetailsActivity : TelinkBaseActivity() , EventListener<String> {
    override fun performed(event: Event<String>?) {

    }

    private lateinit var curtain:MutableList<DbCurtain>

    private var recyclerView:RecyclerView?=null

    private var adapter:CurtainDeviceDetailsAdapter?=null

    internal var showList: List<ItemTypeGroup>? = null

    private var gpList: List<ItemTypeGroup>? = null

    private var type: Int? = null

    private var inflater: LayoutInflater? = null

    private var currentLight: DbCurtain? = null

    private var positionCurrent: Int = 0

//    private lateinit var lightList: MutableList<DbLight>

    private var canBeRefresh = true

//    private lateinit var group: DbGroup

    private val REQ_LIGHT_SETTING: Int = 0x01

    private var acitivityIsAlive = true

    private var mTelinkLightService: TelinkLightService? = null

    private var retryConnectCount = 0

    private var bestRSSIDevice: DeviceInfo? = null

    private val connectFailedDeviceMacList: MutableList<String> = mutableListOf()

    private var mConnectDisposal: Disposable? = null
    private var mScanDisposal: Disposable? = null
    private var mScanTimeoutDisposal: Disposable? = null
    private var mCheckRssiDisposal: Disposable? = null
    private var mNotFoundSnackBar: Snackbar? = null
    private var mApplication: TelinkLightApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_curtains_device_details)
        initData()
        initView()
    }

    private fun initData() {
        gpList = DBUtils.getgroupListWithType(this!!)

        curtain=DBUtils.getAllCurtain()
        showList = ArrayList()
        showList = gpList

    }

    private fun initView() {
        val layoutmanager = LinearLayoutManager(this)
        recyclerView = findViewById<RecyclerView>(R.id.recycleView)
        recyclerView!!.layoutManager = GridLayoutManager(this,3)
//        val decoration = DividerItemDecoration(this!!,
//                DividerItemDecoration
//                        .VERTICAL)
//        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this!!, R.color
//                .divider)))
//        recyclerView!!.addItemDecoration(decoration)
        //添加Item变化动画
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        adapter = CurtainDeviceDetailsAdapter(R.layout.device_detail_adapter, curtain)
        adapter!!.bindToRecyclerView(recyclerView)
        adapter!!.onItemChildClickListener = onItemChildClickListener
        for (i in curtain?.indices!!) {
            curtain!![i].updateIcon()
        }

        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setTitle(R.string.details)
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentLight = curtain?.get(position)
        positionCurrent = position
        val opcode = Opcode.LIGHT_ON_OFF
        if (view.id == R.id.img_light) {
            canBeRefresh = true
            if (currentLight!!.connectionStatus == ConnectionStatus.OFF.value) {
//                TelinkLightService.Instance().sendCommandNoResponse(opcode, currentLight!!.meshAddr,
//                        byteArrayOf(0x01, 0x00, 0x00))
                if (currentLight!!.productUUID == DeviceType.SMART_CURTAIN) {
                    Commander.openOrCloseCurtain(currentLight!!.meshAddr, true, false)
                } else {
                    Commander.openOrCloseLights(currentLight!!.meshAddr, true)
                }

                currentLight!!.connectionStatus = ConnectionStatus.ON.value
            } else {
//                TelinkLightService.Instance().sendCommandNoResponse(opcode, currentLight!!.meshAddr,
//                        byteArrayOf(0x00, 0x00, 0x00))
                if (currentLight!!.productUUID == DeviceType.SMART_CURTAIN) {
                    Commander.openOrCloseCurtain(currentLight!!.meshAddr, false, false)
                } else {
                    Commander.openOrCloseLights(currentLight!!.meshAddr, false)
                }
                currentLight!!.connectionStatus = ConnectionStatus.OFF.value
            }

            currentLight!!.updateIcon()
            DBUtils.updateCurtain(currentLight!!)
            runOnUiThread {
                adapter?.notifyDataSetChanged()
            }
        } else if (view.id == R.id.tv_setting) {
//            if (scanPb.visibility != View.VISIBLE) {
//                判断是否为rgb灯
            var intent = Intent(this@CurtainsDeviceDetailsActivity, WindowCurtainsActivity::class.java)
            intent.putExtra(Constant.TYPE_VIEW,Constant.TYPE_CURTAIN)
            intent.putExtra(Constant.LIGHT_ARESS_KEY, currentLight)
            intent.putExtra(Constant.GROUP_ARESS_KEY, currentLight!!.meshAddr)
            intent.putExtra(Constant.LIGHT_REFRESH_KEY, Constant.LIGHT_REFRESH_KEY_OK)
            startActivityForResult(intent, REQ_LIGHT_SETTING)
//            }
//        else {
//                ToastUtils.showShort(R.string.reconnecting)
//            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        canBeRefresh = false
        acitivityIsAlive = false
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
            if (this@CurtainsDeviceDetailsActivity == null ||
                    this@CurtainsDeviceDetailsActivity.isDestroyed ||
                    this@CurtainsDeviceDetailsActivity.isFinishing || !acitivityIsAlive) {
            } else {
                autoConnect()
            }
        }.start()
    }
    fun notifyData() {
        val mOldDatas: MutableList<DbCurtain>? = curtain
        val mNewDatas: MutableList<DbCurtain>? = getNewData()
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
        curtain = mNewDatas!!
        adapter!!.setNewData(curtain)
    }

    private fun getNewData(): MutableList<DbCurtain> {
        if (currentLight!!.meshAddr == 0xffff) {
            //            lightList = DBUtils.getAllLight();
//            lightList=DBUtils.getAllLight()
            filter("", false)
        } else {
                curtain = DBUtils.getAllCurtain()

        }

        if (currentLight!!.meshAddr == 0xffff) {
            toolbar.title = getString(R.string.allLight) + " (" + curtain.size + ")"
        } else {
            toolbar.title = (currentLight!!.name ?: "")
        }
        return curtain
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
                                TelinkLightService.Instance().connect(mac,CONNECT_TIMEOUT)
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

    /**
     * 扫描不到任何设备了
     * （扫描结束）
     */
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

    private fun filter(groupName: String?, isSearch: Boolean) {
        val list = DBUtils.groupList
//        val nameList : ArrayList<String> = ArrayList()
        if (curtain != null && curtain!!.size > 0) {
            curtain!!.clear()
        }

//        for(i in list.indices){
//            nameList.add(list[i].name)
//        }
    }
}
