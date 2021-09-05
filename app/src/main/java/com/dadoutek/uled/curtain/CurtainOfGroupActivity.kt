package com.dadoutek.uled.curtain

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.appcompat.widget.SearchView
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import butterknife.ButterKnife
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.curtains.WindowCurtainsActivity
import com.dadoutek.uled.group.BatchGroupFourDeviceActivity
import com.dadoutek.uled.group.GroupOTAListActivity

import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbCurtain
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.router.BindRouterActivity
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
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.ErrorReportInfo
import com.telink.bluetooth.light.LeScanParameters
import com.telink.bluetooth.light.LightAdapter
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.Strings
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_lights_of_group.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.startActivity
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * Created by hejiajun on 2018/4/24.
 */

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1

class CurtainOfGroupActivity : TelinkBaseActivity(), EventListener<String>, SearchView.OnQueryTextListener, View.OnClickListener {
    private var deleteDevice: MenuItem? = null
    private var onlineUpdate: MenuItem? = null
    private var batchGp: MenuItem? = null
    private var bindRoute: MenuItem? = null
    private var isDelete: Boolean = false

    //    private lateinit var mMeshAddressGenerator: MeshAddressGenerator
    private val REQ_LIGHT_SETTING: Int = 0x01

    private lateinit var group: DbGroup
    private var mDataManager: DataManager? = null
    private var mApplication: TelinkLightApplication? = null
    private lateinit var curtainList: MutableList<DbCurtain>
    private var adapterDevice: CurtainsOfGroupRecyclerViewAdapter? = null
    private var positionCurrent: Int = 0
    private var currentCurtain: DbCurtain? = null
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

    override fun onResume() {
        super.onResume()
        addListeners()
        initParameter()
        initData()
        initView()
    }

    private fun addListeners() {
        mApplication?.removeEventListener(this)
        addScanListeners()
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        this.mApplication?.addEventListener(NotificationEvent.ONLINE_STATUS, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lights_of_group)
        ButterKnife.bind(this)
        initToolbar()
        initParameter()
        initData()
        initView()
        initOnLayoutListener()
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


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.light_add_device_btn -> {
                DBUtils.lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showLong(getString(R.string.author_region_warm))
                    else {
                        if (DBUtils.getAllCurtains().size == 0) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_CURTAIN)
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
//        intent = Intent(this, CurtainsDeviceDetailsActivity::class.java)
//        intent.putExtra(Constant.DEVICE_TYPE,Constant.INSTALL_CURTAIN_OF)
//        intent.putExtra("curtain_name",group.name)
        /* val intent = Intent(this, CurtainBatchGroupActivity::class.java)
         intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
         intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
         intent.putExtra("curtain","group_curtain")
         intent.putExtra("curtain_group_name",group.name)
         startActivity(intent)
         finish()*/
        val intent = Intent(this, BatchGroupFourDeviceActivity::class.java)
        intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_CURTAIN)
        startActivity(intent)

    }

    private fun initToolbar() {
        toolbarTv.setText(R.string.group_setting_header)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }

        val moreIcon = ContextCompat.getDrawable(toolbar.context, R.drawable.abc_ic_menu_overflow_material)
        if (moreIcon != null) {
            moreIcon.setColorFilter(ContextCompat.getColor(toolbar.context, R.color.black), PorterDuff.Mode.SRC_ATOP)
            toolbar.overflowIcon = moreIcon
        }
        setMenu()
    }


    override fun onQueryTextSubmit(query: String?): Boolean {
        searchView!!.clearFocus()
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null && !newText.isEmpty()) {
            filter(newText, true)
            adapterDevice!!.notifyDataSetChanged()
        } else {
            filter(newText, false)
            adapterDevice!!.notifyDataSetChanged()
        }

        return false
    }


    private fun filter(groupName: String?, isSearch: Boolean) {
        val list = DBUtils.groupList
        if (curtainList != null && curtainList.size > 0)
            curtainList.clear()

        if (isSearch) {
            for (i in list.indices)
                if (groupName == list[i].name || (list[i].name).startsWith(groupName!!))
                    curtainList.addAll(DBUtils.getCurtainByGroupID(list[i].id))
        } else {
            for (i in list.indices)
                if (list[i].meshAddr == 0xffff)
                    Collections.swap(list, 0, i)

            for (j in list.indices)
                curtainList.addAll(DBUtils.getCurtainByGroupID(list[j].id))

        }
    }

    private fun initParameter() {
        this.mApplication = this.application as TelinkLightApplication
        mDataManager = DataManager(this, mApplication!!.mesh.name, mApplication!!.mesh.password)
        val get = this.intent.extras!!.get("group") ?: return
        this.group = get as DbGroup
    }


    override fun onStop() {
        super.onStop()
        this.mApplication!!.removeEventListener(this)
        TelinkLightService.Instance()?.disableAutoRefreshNotify()
    }

    override fun onDestroy() {
        super.onDestroy()
        this.mApplication?.removeEventListener(this)
        canBeRefresh = false
        acitivityIsAlive = false
        mScanDisposal?.dispose()
        if (TelinkLightApplication.getApp().connectDevice == null) {
            TelinkLightService.Instance()?.idleMode(true)
            LeBluetooth.getInstance().stopScan()
        }
    }

    private fun initData() {
        curtainList = ArrayList()
        when (group.meshAddr) {
            0xffff -> filter("", false)
            else -> curtainList = DBUtils.getCurtainByGroupID(group.id)
        }

        toolbar.title = group.name + "(${group.deviceCount})"

        if (curtainList.size > 0) {
            recycler_view_lights.visibility = View.VISIBLE
            no_light_ly.visibility = View.GONE
        } else {
            recycler_view_lights.visibility = View.GONE
            no_light_ly.visibility = View.VISIBLE
        }
    }

    private fun getNewData(): MutableList<DbCurtain> {
        if (group.meshAddr == 0xffff) {
            //            curtainList = DBUtils.getAllLight();
//            curtainList=DBUtils.getAllLight()
            filter("", false)
        } else {
            curtainList = DBUtils.getCurtainByGroupID(group.id)
        }

        if (group.meshAddr == 0xffff) {
            toolbarTv.text = getString(R.string.allLight)
        } else {
            toolbarTv.text = (group.name ?: "") + " (" + curtainList.size + ")"
        }
        return curtainList
    }

    fun notifyData() {
        val mOldDatas: MutableList<DbCurtain>? = curtainList
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
        adapterDevice?.let { diffResult.dispatchUpdatesTo(it) }
        curtainList = mNewDatas!!
        adapterDevice?.setNewData(curtainList)
    }

    private fun initView() {
        if (group.meshAddr == 0xffff) {
            toolbarTv.text = getString(R.string.allLight)
        } else {
            toolbarTv.text = (group.name ?: "") + " (" + curtainList.size + ")"
        }

        light_add_device_btn.setOnClickListener(this)
        recycler_view_lights.layoutManager = GridLayoutManager(this, 2)
        adapterDevice = CurtainsOfGroupRecyclerViewAdapter(R.layout.template_device_type_item, curtainList)
        adapterDevice!!.onItemChildClickListener = onItemChildClickListener
        adapterDevice!!.bindToRecyclerView(recycler_view_lights)
        for (i in curtainList.indices) {
            curtainList[i].updateIcon()
        }

        if (DBUtils.getAllCurtains().size == 0) {
            light_add_device_btn.text = getString(R.string.device_scan_scan)
        } else {
            light_add_device_btn.text = getString(R.string.add_device)
        }
    }

    private fun setMenu() {
        toolbar.inflateMenu(R.menu.menu_rgb_group_setting)
        batchGp = toolbar.menu?.findItem(R.id.toolbar_batch_gp)
        onlineUpdate = toolbar.menu?.findItem(R.id.toolbar_on_line)
        deleteDevice = toolbar.menu?.findItem(R.id.toolbar_delete_device)
        bindRoute = toolbar.menu?.findItem(R.id.toolbar_bind_router)

        batchGp?.title = getString(R.string.batch_group)
        onlineUpdate?.title = getString(R.string.online_upgrade)
        deleteDevice?.title = getString(R.string.edite_device)
        bindRoute?.title = getString(R.string.bind_reouter)

        deleteDevice?.isVisible = true
        batchGp?.isVisible = true
        bindRoute?.isVisible = true
        toolbar.menu?.findItem(R.id.toolbar_add_scene)?.isVisible = false
        toolbar.menu?.findItem(R.id.toolbar_check_data)?.isVisible = false

        toolbar.setOnMenuItemClickListener { itm ->
            DBUtils.lastUser?.let {
                if (it.id.toString() != it.last_authorizer_user_id)
                    ToastUtils.showLong(getString(R.string.author_region_warm))
                else
                    when (itm.itemId) {
                        R.id.toolbar_batch_gp -> skipeBatch()
                        R.id.toolbar_on_line -> goOta()
                        R.id.toolbar_delete_device -> editeDevice()
                        R.id.toolbar_bind_router ->bindRouterDevice()
                    }
            }
            true
        }
    }

    private fun bindRouterDevice() {
        val intent = Intent(this, BindRouterActivity::class.java)
        intent.putExtra("group", group)
        startActivity(intent)
    }

    private fun skipeBatch() {
        when {
            DBUtils.getAllCurtains().size == 0 -> ToastUtils.showShort(getString(R.string.no_device))
            TelinkLightApplication.getApp().connectDevice != null || Constant.IS_ROUTE_MODE ->
                startActivity<BatchGroupFourDeviceActivity>(Constant.DEVICE_TYPE to DeviceType.SMART_CURTAIN, "gp" to group?.meshAddr)
            else -> ToastUtils.showShort(getString(R.string.connect_fail))
        }
    }

    private fun goOta() {
        if (DBUtils.getAllCurtains().size == 0)
            ToastUtils.showShort(getString(R.string.no_device))
        else
            startActivity<GroupOTAListActivity>("group" to group!!, "DeviceType" to DeviceType.SMART_CURTAIN)
    }

    private fun editeDevice() {
        isDelete = !isDelete
        if (isDelete)
            deleteDevice?.title = getString(R.string.cancel_edite)
        else
            deleteDevice?.title = getString(R.string.edite_device)

        adapterDevice?.changeState(isDelete)
        adapterDevice?.notifyDataSetChanged()
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentCurtain = curtainList[position]
        positionCurrent = position
        when (view.id) {
            R.id.template_device_icon, R.id.template_device_setting -> {
                if (scanPb.visibility != View.VISIBLE) {
                    //判断是否为rgb灯
                    if (currentCurtain?.productUUID == DeviceType.SMART_CURTAIN) {
                        intent = Intent(this@CurtainOfGroupActivity, WindowCurtainsActivity::class.java)
                        intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_CURTAIN)
                    }
                    intent.putExtra(Constant.LIGHT_ARESS_KEY, currentCurtain)
                    intent.putExtra(Constant.GROUP_ARESS_KEY, group.meshAddr)
                    intent.putExtra(Constant.LIGHT_REFRESH_KEY, Constant.LIGHT_REFRESH_KEY_OK)
                    intent.putExtra(Constant.CURTAINS_ARESS_KEY, currentCurtain!!.meshAddr)
                    intent.putExtra(Constant.CURTAINS_KEY, currentCurtain!!.productUUID)
                    startActivityForResult(intent, REQ_LIGHT_SETTING)
                } else {
                    ToastUtils.showLong(R.string.connecting_tip)
                }
            }
            R.id.template_device_card_delete -> showDeleteSingleDialog(currentCurtain!!)
        }
    }


    private fun showDeleteSingleDialog(dbLight: DbCurtain) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.sure_delete_device, dbLight.name))
        TelinkLightService.Instance()?.idleMode(true)
        builder.setPositiveButton(getString(android.R.string.ok)) { _, _ ->
            deletePreGroup(dbLight)

            DBUtils.updateGroup(group)

            dbLight.hasGroup = false
            dbLight.belongGroupId = 1
            DBUtils.updateCurtain(dbLight)

            curtainList.remove(dbLight)
            adapterDevice?.notifyDataSetChanged()
            setEmptyAndToolbarTV()
        }
        builder.setNegativeButton(getString(R.string.cancel)) { _, _ -> }
        val dialog = builder.show()
        dialog.show()
    }

    private fun setEmptyAndToolbarTV() {
        toolbarTv.text = group?.name + "(${group?.deviceCount})"
        if (curtainList.size > 0) {
            recycler_view_lights.visibility = View.VISIBLE
            no_light_ly.visibility = View.GONE
        } else {
            recycler_view_lights.visibility = View.GONE
            no_light_ly.visibility = View.VISIBLE
        }
    }


    private fun deletePreGroup(dbLight: DbCurtain) {
        if (DBUtils.getGroupByID(dbLight.belongGroupId!!) != null) {
            val groupAddress = DBUtils.getGroupByID(dbLight.belongGroupId!!)?.meshAddr
            val opcode = Opcode.SET_GROUP
            val params = byteArrayOf(0x00, (groupAddress!! and 0xFF).toByte(), (groupAddress shr 8 and 0xFF).toByte())//0x00表示删除组
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, dbLight.meshAddr, params)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        notifyData()
        val isConnect = data?.getBooleanExtra("data", false) ?: false
        if (isConnect) {
            scanPb.visibility = View.VISIBLE
        }

        GlobalScope.launch {
            //踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
            delay(2500)
            if (this@CurtainOfGroupActivity == null ||
                    this@CurtainOfGroupActivity.isDestroyed ||
                    this@CurtainOfGroupActivity.isFinishing || !acitivityIsAlive) {
            } else {
                autoConnect()
            }
        }
    }

    /**********************************telink part***************************************************/

    override fun performed(event: Event<String>) {
        when (event.type) {
            LeScanEvent.LE_SCAN -> onLeScan(event as LeScanEvent)
            NotificationEvent.GET_ALARM -> {
            }
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            ServiceEvent.SERVICE_CONNECTED -> this.onServiceConnected(event as ServiceEvent)
            ServiceEvent.SERVICE_DISCONNECTED -> this.onServiceDisconnected(event as ServiceEvent)
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
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
                    var root = findViewById<ConstraintLayout>(R.id.configPirRoot)
                    root.indefiniteSnackbar(R.string.openBluetooth, android.R.string.ok) {
                        LeBluetooth.getInstance().enable(applicationContext)
                    }
                }
            } else {
                //如果位置服务没打开，则提示用户打开位置服务
                if (!BleUtils.isLocationEnable(this)) {
                    CoroutineScope(Dispatchers.Main).launch {
                        showOpenLocationServiceDialog()
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
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
        if (deviceInfo != null)
            this.connectMeshAddress = (this.mApplication?.connectDevice?.meshAddress ?: 0x00) and 0xFF
    }

    @SuppressLint("CheckResult")
    private fun startScan() {
        //当App在前台时，才进行扫描。
        if (AppUtils.isAppForeground())
            if (acitivityIsAlive || mScanDisposal?.isDisposed != true) {
                mScanDisposal = RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN)
                        .subscribeOn(Schedulers.io())
                        .subscribe({
                            if (it) {
                                TelinkLightService.Instance()?.idleMode(true)
                                bestRSSIDevice = null   //扫描前置空信号最好设备。
                                //扫描参数
                                val account = DBUtils.lastUser?.account
                                val scanFilters = java.util.ArrayList<ScanFilter>()
                                val scanFilter = ScanFilter.Builder().setDeviceName(account).build()
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
                        }, {})
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
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        when {
                            it -> {//授予了权限
                                progressBar?.visibility = View.VISIBLE
                                TelinkLightService.Instance()?.connect(mac, CONNECT_TIMEOUT)
                                startConnectTimer()
                            }
                            else -> {//没有授予权限
                                DialogUtils.showNoBlePermissionDialog(this, { connect(mac) }, { finish() })
                            }
                        }
                    }, {})
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 扫描不到任何设备了
     * （扫描结束）
     */
    private fun onLeScanTimeout() {
        //"onErrorReport: onLeScanTimeout")
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
                        //"onLeScanTimeout()")
                        onLeScanTimeout()
                    }

                    override fun onSubscribe(d: Disposable) {
                        mScanTimeoutDisposal = d
                    }

                    override fun onNext(t: Long) {
                        if (bestRSSIDevice != null) {
                            mScanTimeoutDisposal?.dispose()
                            //"connect device , mac = ${bestRSSIDevice?.macAddress}  rssi = ${bestRSSIDevice?.rssi}")
                            connect(bestRSSIDevice!!.macAddress)
                        }
                    }

                    override fun onError(e: Throwable) {
                        Log.d("SawTest", "error = $e")
                    }
                })
    }

    private fun addScanListeners() {
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
            if (TelinkLightService.Instance()?.adapter?.mLightCtrl?.currentLight?.isConnected != true)
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
                adapterDevice?.notifyDataSetChanged()

            }
            LightAdapter.STATUS_CONNECTING -> {
                Log.d("connectting", "444")
                scanPb.visibility = View.VISIBLE
            }

            LightAdapter.STATUS_LOGOUT -> {
                retryConnect()
            }

            LightAdapter.STATUS_CONNECTED -> {
                TelinkLightService.Instance() ?: return
                if (!TelinkLightService.Instance()!!.isLogin)
                    login()
            }
            LightAdapter.STATUS_ERROR_N -> onNError(event)
        }
    }

    private fun onServiceConnected(event: ServiceEvent) {
//        //"onServiceConnected")
    }

    private fun onServiceDisconnected(event: ServiceEvent) {
        //"onServiceDisconnected")
        if (TelinkLightService.Instance() == null)
        TelinkLightApplication.getApp().startLightService(TelinkLightService::class.java)
    }

    /**
     * 处理扫描事件
     *
     * @param event
     */
    @Synchronized
    private fun onLeScan(event: LeScanEvent) {
//        val mesh = this.mApplication?.mesh
//        val meshAddress = mMeshAddressGenerator.meshAddress
        val deviceInfo: DeviceInfo = event.args

        Thread {
            val dbCutain = DBUtils.getLightByMeshAddr(deviceInfo.meshAddress)
            if (dbCutain != null && dbCutain.macAddr == "0") {
                dbCutain.macAddr = deviceInfo.macAddress
                DBUtils.updateLight(dbCutain)
            }
        }.start()

        if (!isSwitch(deviceInfo.productUUID) && !connectFailedDeviceMacList.contains(deviceInfo.macAddress)) {
//            connect(deviceInfo.macAddress)
            if (bestRSSIDevice != null) {
                //扫到的灯的信号更好并且没有连接失败过就把要连接的灯替换为当前扫到的这个。
                if (deviceInfo.rssi > bestRSSIDevice?.rssi ?: 0) {
                    //"changeToScene to device with better RSSI  new meshAddr = ${deviceInfo.meshAddress} rssi = ${deviceInfo.rssi}")
                    bestRSSIDevice = deviceInfo
                }
            } else {
                //"RSSI  meshAddr = ${deviceInfo.meshAddress} rssi = ${deviceInfo.rssi}")
                bestRSSIDevice = deviceInfo
            }

        }

    }

    private fun isSwitch(uuid: Int): Boolean {
        return when (uuid) {
            DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SENSOR, DeviceType.NIGHT_LIGHT -> {
                //"This is switch")
                true
            }
            else -> {
                false

            }
        }
    }

    private fun onErrorReport(info: ErrorReportInfo) {
//        //"onErrorReport current device mac = ${bestRSSIDevice?.macAddress}")
        if (bestRSSIDevice != null) {
            connectFailedDeviceMacList.add(bestRSSIDevice!!.macAddress)
        }
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        //"蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        //"无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        //"未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        //"未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        //"未建立物理连接")
                    }
                }
                retryConnect()

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        //"value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        //"read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        //"write login data 没有收到response")
                    }
                }
                retryConnect()

            }
        }
    }
}


