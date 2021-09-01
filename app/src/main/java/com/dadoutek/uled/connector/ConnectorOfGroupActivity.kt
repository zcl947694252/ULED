package com.dadoutek.uled.connector

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R

import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.BatchGroupFourDeviceActivity
import com.dadoutek.uled.group.GroupOTAListActivity
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbConnector
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.rgb.RGBSettingActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.BleUtils
import com.dadoutek.uled.util.DataManager
import com.dadoutek.uled.util.DialogUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
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
import kotlinx.android.synthetic.main.activity_lights_of_group.no_light_ly
import kotlinx.android.synthetic.main.activity_lights_of_group.recycler_view_lights
import kotlinx.android.synthetic.main.activity_lights_of_group.scanPb
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.startActivity
import java.util.*
import java.util.concurrent.TimeUnit

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1

class ConnectorOfGroupActivity : TelinkBaseActivity(), EventListener<String>, SearchView.OnQueryTextListener, View.OnClickListener {
    private var isOpen: Int = 0
    private var bindRouter: MenuItem? = null
    private val REQ_LIGHT_SETTING: Int = 0x01
    private lateinit var group: DbGroup
    private var mDataManager: DataManager? = null
    private var mApplication: TelinkLightApplication? = null
    private lateinit var lightList: MutableList<DbConnector>
    private var adapterDevice: ConnectorOfGroupRecyclerViewAdapter? = null
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
    private var recyclerView: androidx.recyclerview.widget.RecyclerView? = null
    private var deleteDevice: MenuItem? = null
    private var onlineUpdate: MenuItem? = null
    private var batchGp: MenuItem? = null
    private var isDelete: Boolean = false

    override fun onPostResume() {
        super.onPostResume()
        addListeners()
        initData()
    }

    private fun addListeners() {
        addScanListeners()
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connector_of_group)
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
        /*   val intent = Intent(this, ConnectorBatchGroupActivity::class.java)
           intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
           intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
           intent.putExtra("relayType", "group_relay")
           intent.putExtra("relay_group_name", group.name)
           startActivity(intent)
           finish()*/
        val intent = Intent(this, BatchGroupFourDeviceActivity::class.java)
        intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_RELAY)
        startActivity(intent)
    }


    private fun initToolbar() {
        toolbarTv?.text = getString(R.string.group_setting_header)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        val moreIcon = ContextCompat.getDrawable(toolbar.context, R.drawable.abc_ic_menu_overflow_material)
        if (moreIcon != null) {
            moreIcon.setColorFilter(ContextCompat.getColor(toolbar.context, R.color.black), PorterDuff.Mode.SRC_ATOP)
            toolbar.overflowIcon = moreIcon
        }
        setMenu()
    }

    private fun setMenu() {
        toolbar.inflateMenu(R.menu.menu_rgb_group_setting)
        batchGp = toolbar.menu?.findItem(R.id.toolbar_batch_gp)
        onlineUpdate = toolbar.menu?.findItem(R.id.toolbar_on_line)
        deleteDevice = toolbar.menu?.findItem(R.id.toolbar_delete_device)
        bindRouter = toolbar.menu?.findItem(R.id.toolbar_bind_router)

        batchGp?.title = getString(R.string.batch_group)
        onlineUpdate?.title = getString(R.string.online_upgrade)
        deleteDevice?.title = getString(R.string.edite_device)

        deleteDevice?.isVisible = true
        batchGp?.isVisible = true
        bindRouter?.isVisible = true
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
                        R.id.toolbar_bind_router -> bindDeviceRouter()
                    }
            }
            true
        }
    }

    private fun bindDeviceRouter() {

    }

    private fun skipeBatch() {
        when {
            DBUtils.getAllRelay().size == 0 -> ToastUtils.showShort(getString(R.string.no_device))
            TelinkLightApplication.getApp().connectDevice != null || Constant.IS_ROUTE_MODE ->
                startActivity<BatchGroupFourDeviceActivity>(Constant.DEVICE_TYPE to DeviceType.SMART_RELAY, "gp" to group?.meshAddr)
            else -> ToastUtils.showShort(getString(R.string.connect_fail))
        }
    }

    private fun goOta() {
        if (DBUtils.getAllRelay().size == 0)
            ToastUtils.showShort(getString(R.string.no_device))
        else
            startActivity<GroupOTAListActivity>("group" to group!!, "DeviceType" to DeviceType.SMART_RELAY)
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

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchView!!.clearFocus()
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null && newText.isNotEmpty()) {
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
        if (lightList != null && lightList.size > 0)
            lightList.clear()

        when {
            isSearch -> {
                for (i in list.indices)
                    if (groupName == list[i].name || (list[i].name).startsWith(groupName!!))
                        lightList.addAll(DBUtils.getRelayByGroupID(list[i].id))


            }
            else -> {
                for (i in list.indices)
                    if (list[i].meshAddr == 0xffff)
                        Collections.swap(list, 0, i)

                for (j in list.indices)
                    lightList.addAll(DBUtils.getRelayByGroupID(list[j].id))
            }
        }
    }

    private fun initParameter() {
        this.mApplication = this.application as TelinkLightApplication
        mDataManager = DataManager(this, mApplication!!.mesh.name, mApplication!!.mesh.password)
        val get = this.intent.extras!!.get("group") ?: return
        group = get as DbGroup
    }

    override fun onResume() {
        super.onResume()
        initParameter()
        initData()
        initView()
    }

    override fun onStop() {
        super.onStop()
        mApplication!!.removeEventListener(this)
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
        when (group.meshAddr) {
            0xffff -> filter("", false)
            else -> lightList = DBUtils.getRelayByGroupID(group.id)
        }
        toolbar.title = group?.name + "(${group?.deviceCount})"
        if (lightList.size > 0) {
            recycler_view_lights.visibility = View.VISIBLE
            no_light_ly.visibility = View.GONE
        } else {
            recycler_view_lights.visibility = View.GONE
            no_light_ly.visibility = View.VISIBLE
        }
    }

    private fun getNewData(): MutableList<DbConnector> {
        when (group.meshAddr) {
            0xffff -> filter("", false)
            else -> lightList = DBUtils.getRelayByGroupID(group.id)
        }

        when (group.meshAddr) {
            0xffff -> toolbarTv.text = getString(R.string.allLight) + " (" + lightList.size + ")"
            else -> toolbarTv.text = (group.name ?: "") + " (" + lightList.size + ")"
        }
        return lightList
    }

    fun notifyData() {
        val mOldDatas: MutableList<DbConnector>? = lightList
        val mNewDatas: MutableList<DbConnector>? = getNewData()
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return mOldDatas?.get(oldItemPosition)?.id?.equals(mNewDatas?.get(newItemPosition)?.id) ?: false
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
        lightList = mNewDatas!!
        adapterDevice?.setNewData(lightList)
    }


    private fun initView() {
        when (group.meshAddr) {
            0xffff -> toolbarTv.text = getString(R.string.allLight) + " (" + lightList.size + ")"
            else -> toolbarTv.text = (group.name ?: "") + " (" + lightList.size + ")"
        }
        light_add_device_btns.setOnClickListener(this)
        recyclerView = findViewById(R.id.recycler_view_lights)
        recyclerView!!.layoutManager = GridLayoutManager(this, 2)
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        adapterDevice = ConnectorOfGroupRecyclerViewAdapter(R.layout.template_device_type_item, lightList)
        adapterDevice!!.onItemChildClickListener = onItemChildClickListener
        adapterDevice!!.bindToRecyclerView(recyclerView)
        for (i in lightList.indices)
            lightList[i].updateIcon()

        when (DBUtils.getAllRelay().size) {
            0 -> light_add_device_btns.text = getString(R.string.device_scan_scan)
            else -> light_add_device_btns.text = getString(R.string.add_device)
        }
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentLight = lightList[position]
        positionCurrent = position
        when (view.id) {
            R.id.template_device_icon -> {
                canBeRefresh = true
                when (currentLight!!.connectionStatus) {
                    ConnectionStatus.OFF.value -> {
                        isOpen = 1
                        when {
                            Constant.IS_ROUTE_MODE -> routeOpenOrCloseBase(currentLight!!.meshAddr, currentLight!!.productUUID, 1, "relaySw")
                            else -> {
                                Commander.openOrCloseLights(currentLight!!.meshAddr, true)
                                currentLight!!.connectionStatus = ConnectionStatus.ON.value
                                afterClickIcon()
                            }
                        }
                    }
                    else -> {
                        isOpen = 0
                        when {
                            Constant.IS_ROUTE_MODE -> routeOpenOrCloseBase(currentLight!!.meshAddr, currentLight!!.productUUID, 0, "relaySw")
                            else -> {
                                Commander.openOrCloseLights(currentLight!!.meshAddr, false)
                                currentLight!!.connectionStatus = ConnectionStatus.OFF.value
                                afterClickIcon()
                            }
                        }


                    }
                }


            }
            R.id.template_device_setting -> {
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
                            ToastUtils.showLong(R.string.connecting_tip)
                        }
                    }
                }
            }

            R.id.template_device_card_delete -> showDeleteSingleDialog(currentLight!!)
        }
    }

    override fun tzRouterOpenOrClose(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由relaySw通知-------$cmdBean")
        if (cmdBean.ser_id == "relaySw") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            if (cmdBean.status == 0) {
                currentLight!!.connectionStatus = isOpen
                afterClickIcon()
            } else {
                if (isOpen == 1)
                    ToastUtils.showShort(getString(R.string.open_light_faile))
                else
                    ToastUtils.showShort(getString(R.string.close_faile))
            }
        }
    }

    private fun afterClickIcon() {
        currentLight!!.updateIcon()
        DBUtils.updateConnector(currentLight!!)
        adapterDevice?.notifyDataSetChanged()
    }

    private fun showDeleteSingleDialog(dbLight: DbConnector) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.sure_delete_device, dbLight.name))
//        TelinkLightService.Instance()?.idleMode(true)
        builder.setPositiveButton(getString(android.R.string.ok)) { _, _ ->
            deletePreGroup(dbLight)
            DBUtils.updateGroup(group)
            dbLight.hasGroup = false
            dbLight.belongGroupId = 1
            DBUtils.updateRelayLocal(dbLight)

            lightList.remove(dbLight)
            adapterDevice?.notifyDataSetChanged()
            setEmptyAndToolbarTV()
        }
        builder.setNegativeButton(getString(R.string.cancel)) { _, _ -> }
        val dialog = builder.show()
        dialog.show()
    }

    private fun setEmptyAndToolbarTV() {
        toolbarTv.text = group?.name + "(${group?.deviceCount})"
        if (lightList.size > 0) {
            recycler_view_lights.visibility = View.VISIBLE
            no_light_ly.visibility = View.GONE
        } else {
            recycler_view_lights.visibility = View.GONE
            no_light_ly.visibility = View.VISIBLE
        }
    }


    private fun deletePreGroup(dbLight: DbConnector) {
        if (DBUtils.getGroupByID(dbLight!!.belongGroupId!!) != null) {
            val groupAddress = DBUtils.getGroupByID(dbLight!!.belongGroupId!!)?.meshAddr
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
            when {
                this@ConnectorOfGroupActivity == null || this@ConnectorOfGroupActivity.isDestroyed || this@ConnectorOfGroupActivity.isFinishing || !acitivityIsAlive -> {
                }
                else -> autoConnect()
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
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            if (it) {
                                TelinkLightService.Instance()?.idleMode(true)
                                bestRSSIDevice = null   //扫描前置空信号最好设备。
                                //扫描参数
                                val account = DBUtils.lastUser?.account

                                val scanFilters = ArrayList<ScanFilter>()
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
                    .subscribe ({
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
                    },{})
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 扫描不到任何设备了
     * （扫描结束）
     */
    private fun onLeScanTimeout() {
        TelinkLightService.Instance()?.idleMode(true)
        LeBluetooth.getInstance().stopScan()
        startScan()
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
        TelinkLightService.Instance()?.login(Strings.stringToBytes(meshName, 16), Strings.stringToBytes(pwd, 16))
    }

    private fun onNError(event: DeviceEvent) {
        TelinkLightService.Instance()?.idleMode(true)
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
            LightAdapter.STATUS_CONNECTED -> {
                TelinkLightService.Instance() ?: return
                if (TelinkLightApplication.getApp().connectDevice == null)
                    login()
            }
            LightAdapter.STATUS_ERROR_N -> onNError(event)
        }
    }

    private fun onServiceDisconnected(event: ServiceEvent) {
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
            when {
                bestRSSIDevice != null -> {
                    //扫到的灯的信号更好并且没有连接失败过就把要连接的灯替换为当前扫到的这个。
                    if (deviceInfo.rssi > bestRSSIDevice?.rssi ?: 0)
                        bestRSSIDevice = deviceInfo
                }
                else -> bestRSSIDevice = deviceInfo
            }

        }

    }

    private fun isSwitch(uuid: Int): Boolean {
        return when (uuid) {
            DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SENSOR, DeviceType.NIGHT_LIGHT -> true
            else -> false
        }
    }

    private fun onErrorReport(info: ErrorReportInfo) {
        if (bestRSSIDevice != null)
            connectFailedDeviceMacList.add(bestRSSIDevice!!.macAddress)

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
