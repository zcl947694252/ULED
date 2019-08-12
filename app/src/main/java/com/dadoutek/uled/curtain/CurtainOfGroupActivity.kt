package com.dadoutek.uled.curtain

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.util.DiffUtil
import android.support.v7.widget.GridLayoutManager
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
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.BleUtils
import com.dadoutek.uled.util.DataManager
import com.dadoutek.uled.util.DialogUtils
import com.dadoutek.uled.windowcurtains.CurtainBatchGroupActivity
import com.dadoutek.uled.windowcurtains.WindowCurtainsActivity
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
import kotlinx.android.synthetic.main.activity_lights_of_group.*
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
import kotlin.collections.ArrayList

/**
 * Created by hejiajun on 2018/4/24.
 */

private const val MAX_RETRY_CONNECT_TIME = 5
private const val CONNECT_TIMEOUT = 10
private const val SCAN_TIMEOUT_SECOND: Int = 10
private const val SCAN_BEST_RSSI_DEVICE_TIMEOUT_SECOND: Long = 1

class CurtainOfGroupActivity : TelinkBaseActivity(), EventListener<String>, SearchView.OnQueryTextListener ,View.OnClickListener{
    private val REQ_LIGHT_SETTING: Int = 0x01

    private lateinit var group: DbGroup
    private var mDataManager: DataManager? = null
    private var mApplication: TelinkLightApplication? = null
    private lateinit var curtainList: MutableList<DbCurtain>
    private var adapter: CurtainsOfGroupRecyclerViewAdapter? = null
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

    override fun onStart() {
        super.onStart()
        // 监听各种事件
        //"____onStart")
    }

    override fun onPostResume() {
        super.onPostResume()
        addListeners()
    }

    fun addListeners() {
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


    private fun initOnLayoutListener() {
        val view = getWindow().getDecorView()
        val viewTreeObserver = view.getViewTreeObserver()
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                lazyLoad()
            }
        })
    }

    fun lazyLoad() {
//        if (curtainList.size != 0) {
//            val guide2: View? = adapter!!.getViewByPosition(0, R.id.img_light)
//            val guide3 = adapter!!.getViewByPosition(0, R.id.tv_setting)
//
//            val builder = GuideUtils.guideBuilder(this@CurtainOfGroupActivity, Constant.TAG_LightsOfGroupActivity)
//            builder.addGuidePage(GuideUtils.addGuidePage(guide2!!, R.layout
//                    .view_guide_simple_light_1, getString(R.string.light_guide_1), View
//                    .OnClickListener {
//                        ActivityUtils.startActivity(ScanningSensorActivity::class.java)
//                    }))
//            builder.addGuidePage(GuideUtils.addGuidePage(guide3!!, R.layout.view_guide_simple_light_1, getString(R.string.light_guide_2)))
//            val guide = builder.show()
//
//        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.light_add_device_btn -> {
                if(DBUtils.getAllCurtains().size ==0){
                    intent = Intent(this, CurtainScanningNewActivity::class.java)
                    intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                    intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                    startActivityForResult(intent, 0)
                }else{
                    addDevice()
                }
            }
        }
    }

    private fun addDevice() {
//        intent = Intent(this, CurtainsDeviceDetailsActivity::class.java)
//        intent.putExtra(Constant.DEVICE_TYPE,Constant.INSTALL_CURTAIN_OF)
//        intent.putExtra("curtain_name",group.name)
        val intent = Intent(this,
                CurtainBatchGroupActivity::class.java)
        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
        intent.putExtra("curtain","group_curtain")
        intent.putExtra("curtain_group_name",group.name)
        startActivity(intent)
        finish()
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
//        val nameList : ArrayList<String> = ArrayList()
        if (curtainList != null && curtainList.size > 0) {
            curtainList.clear()
        }

//        for(i in list.indices){
//            nameList.add(list[i].name)
//        }

        if (isSearch) {
            for (i in list.indices) {
                if (groupName == list[i].name || (list[i].name).startsWith(groupName!!)) {
                    curtainList.addAll(DBUtils.getCurtainByGroupID(list[i].id))
                }
            }

        } else {
            for (i in list.indices) {
                if (list.get(i).meshAddr == 0xffff) {
                    Collections.swap(list, 0, i)
                }
            }

            for (j in list.indices) {
                curtainList.addAll(DBUtils.getCurtainByGroupID(list[j].id))
            }
        }
    }

    private fun initParameter() {
        this.group = this.intent.extras!!.get("group") as DbGroup
        this.mApplication = this.application as TelinkLightApplication
        mDataManager = DataManager(this, mApplication!!.mesh.name, mApplication!!.mesh.password)
    }

    override fun onResume() {
        super.onResume()
//        initToolbar()
//        initParameter()
//        initData()
//        initView()
//        initOnLayoutListener()
    }

    override fun onStop() {
        super.onStop()
        this.mApplication!!.removeEventListener(this)
            TelinkLightService.Instance()?.disableAutoRefreshNotify()
    }

    override fun onDestroy() {
        super.onDestroy()
        //        this.mApplication.removeEventListener(this);
        canBeRefresh = false
        acitivityIsAlive = false
        mScanDisposal?.dispose()
        if (TelinkLightApplication.getInstance().connectDevice == null) {
            TelinkLightService.Instance()?.idleMode(true)
            LeBluetooth.getInstance().stopScan()
        }
    }

    private fun initData() {
        curtainList = ArrayList()
        if (group.meshAddr == 0xffff) {
            //            curtainList = DBUtils.getAllLight();
//            curtainList=DBUtils.getAllLight()
            filter("", false)
        } else {
            curtainList = DBUtils.getCurtainByGroupID(group.id)
        }

        if (curtainList.size > 0) {
            recycler_view_lights.visibility = View.VISIBLE
            no_light.visibility = View.GONE
            var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
            toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
            batchGroup.setText(R.string.batch_group)
            batchGroup.setOnClickListener(View.OnClickListener {
                val intent = Intent(this,
                        CurtainBatchGroupActivity::class.java)
                intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                intent.putExtra("curtain", "curtain_group")
                intent.putExtra("group",group.id.toInt())
                startActivity(intent)
                finish()
            })
        } else {
            recycler_view_lights.visibility = View.GONE
            no_light.visibility = View.VISIBLE
            toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility=View.GONE
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
            toolbar.title = getString(R.string.allLight) + " (" + curtainList.size + ")"
        } else {
            toolbar.title = (group.name ?: "") + " (" + curtainList.size + ")"
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
        adapter?.let { diffResult.dispatchUpdatesTo(it) }
        curtainList = mNewDatas!!
        adapter?.setNewData(curtainList)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (group.meshAddr == 0xffff) {
            getMenuInflater().inflate(R.menu.menu_search, menu)
            searchView = (menu!!.findItem(R.id.action_search)).actionView as SearchView
            searchView!!.setOnQueryTextListener(this)
            searchView!!.imeOptions = EditorInfo.IME_ACTION_SEARCH
            searchView!!.setQueryHint(getString(R.string.input_groupAdress))
            searchView!!.setSubmitButtonEnabled(true)
            searchView!!.backgroundColor = resources.getColor(R.color.blue)
            searchView!!.alpha = 0.3f
//            val icon = searchView!!.findViewById<ImageView>(android.support.v7.appcompat.R.id.search_button)
            return super.onCreateOptionsMenu(menu)
        }
        return true
    }

    fun getAlphaBitmap(mBitmap: Bitmap, mColor: Int): Bitmap {
        //	    	BitmapDrawable mBitmapDrawable = (BitmapDrawable) mContext.getResources().getDrawable(R.drawable.enemy_infantry_ninja);
        //	    	Bitmap mBitmap = mBitmapDrawable.getBitmap();

        //BitmapDrawable的getIntrinsicWidth（）方法，Bitmap的getWidth（）方法
        //注意这两个方法的区别
        //Bitmap mAlphaBitmap = Bitmap.createBitmap(mBitmapDrawable.getIntrinsicWidth(), mBitmapDrawable.getIntrinsicHeight(), Config.ARGB_8888);
        val mAlphaBitmap = Bitmap.createBitmap(mBitmap.width, mBitmap.height, Bitmap.Config.ARGB_8888)

        val mCanvas = Canvas(mAlphaBitmap)
        val mPaint = Paint()

        mPaint.color = mColor
        //从原位图中提取只包含alpha的位图
        val alphaBitmap = mBitmap.extractAlpha()
        //在画布上（mAlphaBitmap）绘制alpha位图
        mCanvas.drawBitmap(alphaBitmap, 0f, 0f, mPaint)

        return mAlphaBitmap
    }

    private fun initView() {
        if (group.meshAddr == 0xffff) {
            toolbar.title = getString(R.string.allLight) + " (" + curtainList.size + ")"
//            if(searchView==null){
//                toolbar.inflateMenu(R.menu.menu_search)
//                searchView = MenuItemCompat.getActionView(toolbar.menu.findItem(R.id.action_search)) as SearchView
//                searchView!!.setOnQueryTextListener(this)
//            }
        } else {
            toolbar.title = (group.name ?: "") + " (" + curtainList.size + ")"
        }
        light_add_device_btn.setOnClickListener(this)
        recycler_view_lights.layoutManager = GridLayoutManager(this, 3)
        adapter = CurtainsOfGroupRecyclerViewAdapter(R.layout.item_lights_of_group, curtainList)
        adapter!!.onItemChildClickListener = onItemChildClickListener
        adapter!!.bindToRecyclerView(recycler_view_lights)
        for (i in curtainList.indices) {
            curtainList[i].updateIcon()
        }

        if(DBUtils.getAllCurtains().size==0){
            light_add_device_btn.text = getString(R.string.device_scan_scan)
        }else{
            light_add_device_btn.text = getString(R.string.add_device)
        }
    }



    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentCurtain = curtainList[position]
        positionCurrent = position
        val opcode = Opcode.LIGHT_ON_OFF
//        if (view.id == R.id.img_light) {
//            canBeRefresh = true
//            if (currentCurtain!!.connectionStatus == ConnectionStatus.OFF.value) {
////                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, currentCurtain!!.meshAddr,
////                        byteArrayOf(0x01, 0x00, 0x00))
//                if(currentCurtain!!.productUUID==DeviceType.SMART_CURTAIN){
//                    Commander.openOrCloseCurtain(currentCurtain!!.meshAddr,true,false)
//                }else{
//                    Commander.openOrCloseLights(currentCurtain!!.meshAddr,true)
//                }
//
//                currentCurtain!!.connectionStatus = ConnectionStatus.ON.value
//            } else {
////                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, currentCurtain!!.meshAddr,
////                        byteArrayOf(0x00, 0x00, 0x00))
//                if(currentCurtain!!.productUUID==DeviceType.SMART_CURTAIN){
//                    Commander.openOrCloseCurtain(currentCurtain!!.meshAddr,false,false)
//                }else{
//                    Commander.openOrCloseLights(currentCurtain!!.meshAddr,false)
//                }
//                currentCurtain!!.connectionStatus = ConnectionStatus.OFF.value
//            }
//
//            currentCurtain!!.updateIcon()
//            DBUtils.updateCurtain(currentCurtain!!)
//            runOnUiThread {
//                adapter?.notifyDataSetChanged()
//            }
//        } else
    if (view.id == R.id.tv_setting) {
            if (scanPb.visibility != View.VISIBLE) {
                //判断是否为rgb灯
                if(currentCurtain?.productUUID==DeviceType.SMART_CURTAIN){
                    intent=Intent(this@CurtainOfGroupActivity, WindowCurtainsActivity::class.java)
                    intent.putExtra(Constant.TYPE_VIEW,Constant.TYPE_CURTAIN)
                }
                intent.putExtra(Constant.LIGHT_ARESS_KEY, currentCurtain)
                intent.putExtra(Constant.GROUP_ARESS_KEY, group.meshAddr)
                intent.putExtra(Constant.LIGHT_REFRESH_KEY, Constant.LIGHT_REFRESH_KEY_OK)
                intent.putExtra(Constant.CURTAINS_ARESS_KEY, currentCurtain!!.meshAddr)
                intent.putExtra(Constant.CURTAINS_KEY, currentCurtain!!.productUUID)
                startActivityForResult(intent, REQ_LIGHT_SETTING)
            } else {
                ToastUtils.showShort(R.string.reconnecting)
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
            if (this@CurtainOfGroupActivity == null ||
                    this@CurtainOfGroupActivity.isDestroyed ||
                    this@CurtainOfGroupActivity.isFinishing || !acitivityIsAlive) {
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
                currentCurtain?.textColor = ContextCompat.getColor(
                        this, R.color.primary)
            }

            for (dbCutain in curtainList) {
                if (notificationInfo.meshAddress == dbCutain.meshAddr) {
                    dbCutain.connectionStatus = notificationInfo.connectionStatus.value
                    dbCutain.updateIcon()
                    DBUtils.updateCurtain(dbCutain)
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
                    var root=findViewById<ConstraintLayout>(R.id.configPirRoot)
                    root.indefiniteSnackbar( R.string.openBluetooth, android.R.string.ok) {
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
                    if (TelinkLightApplication.getInstance().connectDevice == null) {
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
                            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.CONNECT_STATE_SUCCESS_KEY, true);
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
                //"startScanLight_LightOfGroup")
                mScanDisposal = RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN)
                        .subscribeOn(Schedulers.io())
                        .subscribe {
                            if (it) {
                                TelinkLightService.Instance()?.idleMode(true)
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
            mCheckRssiDisposal=RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN)
                    .subscribe {
                        if (it) {
                            //授予了权限
                                progressBar?.visibility = View.VISIBLE
                                TelinkLightService.Instance()?.connect(mac, CONNECT_TIMEOUT)
                                startConnectTimer()
                        } else {
                            //没有授予权限
                            DialogUtils.showNoBlePermissionDialog(this, { connect(mac) }, { finish() })
                        }
                    }
        }catch (e:Exception){
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
        val account = DBUtils.lastUser?.account
        val pwd = NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16)
        TelinkLightService.Instance()?.login(Strings.stringToBytes(account, 16)
                , Strings.stringToBytes(pwd, 16))
    }

    private fun onNError(event: DeviceEvent) {

//        ToastUtils.showLong(getString(R.string.connect_fail))
        SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

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
                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, true)
            }
            LightAdapter.STATUS_CONNECTING -> {
                Log.d("connectting", "444")
                scanPb.visibility = View.VISIBLE
            }

            LightAdapter.STATUS_LOGOUT -> {
                retryConnect()
            }

            LightAdapter.STATUS_CONNECTED -> {
                TelinkLightService.Instance()?:return
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
        TelinkLightApplication.getInstance().startLightService(TelinkLightService::class.java)
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

