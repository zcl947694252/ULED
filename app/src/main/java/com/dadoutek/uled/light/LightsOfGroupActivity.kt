package com.dadoutek.uled.light

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import butterknife.ButterKnife
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.BatchGroupActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.rgb.RGBSettingActivity
import com.dadoutek.uled.rgb.RgbBatchGroupActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.DataManager
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.ConnectionStatus
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_lights_of_group.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.backgroundColor
import java.util.*
import kotlin.collections.ArrayList

/**
 * 创建者     zcl
 * 创建时间   2019/8/30 18:46
 * 描述	      ${点击组名跳转搜索设备}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   显示X组里所有灯的页面
 */

class LightsOfGroupActivity : TelinkBaseActivity(), SearchView.OnQueryTextListener, View.OnClickListener {

    private val REQ_LIGHT_SETTING: Int = 0x01

    private lateinit var group: DbGroup
    private var mDataManager: DataManager? = null
    private var mApplication: TelinkLightApplication? = null
    private lateinit var lightList: MutableList<DbLight>
    private var adapter: LightsOfGroupRecyclerViewAdapter? = null
    private var positionCurrent: Int = 0
    private var currentLight: DbLight? = null
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
    private var strLight: String? = null


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
        val view = getWindow().getDecorView()
        val viewTreeObserver = view.getViewTreeObserver()
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                lazyLoad()
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.light_add_device_btn -> {
                if (strLight == "cw_light") {
                    if (DBUtils.getAllNormalLight().size == 0) {
                        intent = Intent(this, DeviceScanningNewActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, false)
                        intent.putExtra(Constant.TYPE_VIEW, Constant.LIGHT_KEY)
                        startActivityForResult(intent, 0)
                    } else {
                        addDevice()
                    }
                } else if (strLight == "rgb_light") {
                    if (DBUtils.getAllRGBLight().size == 0) {
                        intent = Intent(this, DeviceScanningNewActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.TYPE_VIEW, Constant.RGB_LIGHT_KEY)
                        startActivityForResult(intent, 0)
                    } else {
                        addDevice()
                    }
                }
            }
        }
    }

    private fun addDevice() {
        if (strLight == "cw_light") {
            val intent = Intent(this,
                    BatchGroupActivity::class.java)
            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
            intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
            intent.putExtra("lightType", "cw_light")
            intent.putExtra("cw_light_group_name", group.name)
            startActivity(intent)
        } else if (strLight == "rgb_light") {
            val intent = Intent(this,
                    RgbBatchGroupActivity::class.java)
            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
            intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
            intent.putExtra("lightType", "rgb_light")
            intent.putExtra("rgb_light_group_name", group.name)
//            startActivity(intent)
            startActivity(intent)
        }
    }

    fun lazyLoad() {
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
        if (lightList != null && lightList.size > 0) {
            lightList.clear()
        }

//        for(i in list.indices){
//            nameList.add(list[i].name)
//        }

        if (isSearch) {
            for (i in list.indices) {
                if (groupName == list[i].name || (list[i].name).startsWith(groupName!!)) {
                    lightList.addAll(DBUtils.getLightByGroupID(list[i].id))
                }
            }

        } else {
            for (i in list.indices) {
                if (list.get(i).meshAddr == 0xffff) {
                    Collections.swap(list, 0, i)
                }
            }

            for (j in list.indices) {
                lightList.addAll(DBUtils.getLightByGroupID(list[j].id))
            }
        }
    }

    private fun initParameter() {
        val gp = this.intent.extras!!.get("group")
        if (gp!=null)
        this.group = gp as DbGroup
        val light = this.intent.extras!!.get("light")
        if (light!=null)
        this.strLight = light as String
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
        TelinkLightService.Instance()?.disableAutoRefreshNotify()
    }

    override fun onDestroy() {
        super.onDestroy()
        canBeRefresh = false
        acitivityIsAlive = false
        mScanDisposal?.dispose()
        if (TelinkLightApplication.getApp().connectDevice == null) {
            TelinkLightService.Instance()?.idleMode(true)
            LeBluetooth.getInstance().stopScan()
        }
    }

    private fun initData() {
        lightList = ArrayList()
        if (group.meshAddr == 0xffff) {
            //            lightList = DBUtils.getAllLight();
//            lightList=DBUtils.getAllLight()
            filter("", false)
        } else {
            lightList = DBUtils.getLightByGroupID(group.id)
        }

        if (lightList.size > 0) {
            recycler_view_lights.visibility = View.VISIBLE
            no_light.visibility = View.GONE
            if (strLight == "cw_light") {
                var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
                toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
                batchGroup.setText(R.string.batch_group)
                batchGroup.visibility = View.GONE
                batchGroup.setOnClickListener {
                    val intent = Intent(this,
                            BatchGroupActivity::class.java)
                    intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                    intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                    intent.putExtra("lightType", "cw_light_group")
                    intent.putExtra("group", group.id.toInt())
                    startActivity(intent)
                }
            } else if (strLight == "rgb_light") {
                var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
                toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
                batchGroup.setText(R.string.batch_group)
                batchGroup.visibility = View.GONE
                batchGroup.setOnClickListener {
                    val intent = Intent(this,
                            BatchGroupActivity::class.java)
                    intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                    intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                    intent.putExtra("lightType", "rgb_light_group")
                    intent.putExtra("group", group.id.toInt())
                    startActivity(intent)
                }
            }
        } else {
            toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
            recycler_view_lights.visibility = View.GONE
            no_light.visibility = View.VISIBLE
        }
    }

    private fun getNewData(): MutableList<DbLight> {
        if (group.meshAddr == 0xffff) {
            //            lightList = DBUtils.getAllLight();
//            lightList=DBUtils.getAllLight()
            filter("", false)
        } else {
            lightList = DBUtils.getLightByGroupID(group.id)
        }

        if (group.meshAddr == 0xffff) {
            toolbar.title = getString(R.string.allLight) + " (" + lightList.size + ")"
        } else {
            toolbar.title = (group.name ?: "") + " (" + lightList.size + ")"
        }
        return lightList
    }

    fun notifyData() {
        val mOldDatas: MutableList<DbLight>? = lightList
        val mNewDatas: MutableList<DbLight>? = getNewData()
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


    private fun initView() {
        if (group.meshAddr == 0xffff) {
            toolbar.title = getString(R.string.allLight) + " (" + lightList.size + ")"
//            if(searchView==null){
//                toolbar.inflateMenu(R.menu.menu_search)
//                searchView = MenuItemCompat.getActionView(toolbar.menu.findItem(R.id.action_search)) as SearchView
//                searchView!!.setOnQueryTextListener(this)
//            }
        } else {
            toolbar.title = (group.name ?: "") + " (" + lightList.size + ")"
        }
        light_add_device_btn.setOnClickListener(this)
        recyclerView = findViewById(R.id.recycler_view_lights)
        recyclerView!!.layoutManager = GridLayoutManager(this, 3)
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        adapter = LightsOfGroupRecyclerViewAdapter(R.layout.item_lights_of_group, lightList)
        adapter!!.onItemChildClickListener = onItemChildClickListener
        adapter!!.bindToRecyclerView(recyclerView)
        if (strLight == "cw_light") {
            for (i in lightList.indices) {
                lightList[i].updateIcon()
            }
        } else if (strLight == "rgb_light") {
            for (i in lightList.indices) {
                lightList[i].updateRgbIcon()
            }
        }

        if (strLight == "cw_light") {
            if (DBUtils.getAllNormalLight().size == 0) {
                light_add_device_btn.text = getString(R.string.device_scan_scan)
            } else {
                light_add_device_btn.text = getString(R.string.add_device)
            }
        } else if (strLight == "rgb_light") {
            if (DBUtils.getAllRGBLight().size == 0) {
                light_add_device_btn.text = getString(R.string.device_scan_scan)
            } else {
                light_add_device_btn.text = getString(R.string.add_device)
            }
        }

    }


    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentLight = lightList[position]
        positionCurrent = position
        val opcode = Opcode.LIGHT_ON_OFF
        if (view.id == R.id.img_light) {
            canBeRefresh = true
            if (currentLight!!.connectionStatus == ConnectionStatus.OFF.value) {
//                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, currentLight!!.meshAddr,
//                        byteArrayOf(0x01, 0x00, 0x00))
                if (currentLight!!.productUUID == DeviceType.SMART_CURTAIN) {
                    Commander.openOrCloseCurtain(currentLight!!.meshAddr, true, false)
                } else {
                    Commander.openOrCloseLights(currentLight!!.meshAddr, true)
                }

                currentLight!!.connectionStatus = ConnectionStatus.ON.value
            } else {
//                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, currentLight!!.meshAddr,
//                        byteArrayOf(0x00, 0x00, 0x00))
                if (currentLight!!.productUUID == DeviceType.SMART_CURTAIN) {
                    Commander.openOrCloseCurtain(currentLight!!.meshAddr, false, false)
                } else {
                    Commander.openOrCloseLights(currentLight!!.meshAddr, false)
                }
                currentLight!!.connectionStatus = ConnectionStatus.OFF.value
            }

            if (strLight == "cw_light") {
                currentLight!!.updateIcon()
            } else if (strLight == "rgb_light") {
                currentLight!!.updateRgbIcon()
            }
//            currentLight!!.updateIcon()
            DBUtils.updateLight(currentLight!!)
            runOnUiThread {
                adapter?.notifyDataSetChanged()
            }
        } else
            if (view.id == R.id.tv_setting) {
                if (scanPb.visibility != View.VISIBLE) {
                    //判断是否为rgb灯
                    var intent = Intent(this@LightsOfGroupActivity, NormalSettingActivity::class.java)
                    if (currentLight?.productUUID == DeviceType.LIGHT_RGB) {
                        intent = Intent(this@LightsOfGroupActivity, RGBSettingActivity::class.java)
                        intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_LIGHT)
                    }
                    intent.putExtra(Constant.LIGHT_ARESS_KEY, currentLight)
                    intent.putExtra(Constant.GROUP_ARESS_KEY, group.meshAddr)
                    intent.putExtra(Constant.LIGHT_REFRESH_KEY, Constant.LIGHT_REFRESH_KEY_OK)
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
            if (this@LightsOfGroupActivity == null ||
                    this@LightsOfGroupActivity.isDestroyed ||
                    this@LightsOfGroupActivity.isFinishing || !acitivityIsAlive) {
            } else {
                connect()?.subscribe(
                        {
                            onConnected(it)
                        },
                        {
                            LogUtils.d(it)
                        }
                )
            }
        }.start()
    }


/*
    private fun startConnectTimer() {
        mConnectDisposal?.dispose()
        mConnectDisposal = Observable.timer(CONNECT_TIMEOUT.toLong(), TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ retryConnect() }, {})
    }
*/

    private fun stopConnectTimer() {
        mConnectDisposal?.dispose()
    }


    private fun onNError(event: DeviceEvent) {
        TelinkLightService.Instance()?.idleMode(true)
        TelinkLog.d("DeviceScanningActivity#onNError")

        val builder = AlertDialog.Builder(this)
        builder.setMessage("当前环境:Android7.0!连接重试:" + " 3次失败!")
        builder.setNegativeButton("confirm") { dialog, _ -> dialog.dismiss() }
        builder.setCancelable(false)
        builder.show()
    }

    override fun onPause() {
        super.onPause()
        mScanTimeoutDisposal?.dispose()
        mConnectDisposal?.dispose()
        mNotFoundSnackBar?.dismiss()
        //移除事件
        stopConnectTimer()
        mCheckRssiDisposal?.dispose()
    }

    private fun onConnected(deviceInfo: DeviceInfo) {
        GlobalScope.launch(Dispatchers.Main) {
            stopConnectTimer()
            if (progressBar?.visibility != View.GONE)
                progressBar?.visibility = View.GONE
            delay(300)
        }
        this.connectMeshAddress = deviceInfo.meshAddress
        scanPb.visibility = View.GONE
        adapter?.notifyDataSetChanged()
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {

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
            LightAdapter.STATUS_LOGOUT -> {
//                retryConnect()
            }
            LightAdapter.STATUS_CONNECTING -> {
//                Log.d("connectting", "444")
                scanPb.visibility = View.VISIBLE
            }
            LightAdapter.STATUS_CONNECTED -> {
//                TelinkLightService.Instance() ?: return
//                if (!TelinkLightService.Instance()!!.isLogin)
//                    login()
            }
            LightAdapter.STATUS_ERROR_N -> onNError(event)
        }
    }

}

