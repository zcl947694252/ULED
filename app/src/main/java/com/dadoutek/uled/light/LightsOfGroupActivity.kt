package com.dadoutek.uled.light

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.SearchView
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import butterknife.ButterKnife
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.SwitchButtonOnCheckedChangeListener
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.DataManager
import com.dadoutek.uled.util.DialogUtils
import com.dadoutek.uled.util.LogUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.event.NotificationEvent
import com.telink.bluetooth.light.ConnectionStatus
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.OnlineStatusNotificationParser
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_lights_of_group.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.backgroundColor
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by hejiajun on 2018/4/24.
 */

class LightsOfGroupActivity : TelinkBaseActivity(), EventListener<String>, SearchView.OnQueryTextListener {
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


    private var onCheckedChangeListener: SwitchButtonOnCheckedChangeListener = SwitchButtonOnCheckedChangeListener { v, position ->
        currentLight = lightList[position]
        positionCurrent = position
        val opcode = Opcode.LIGHT_ON_OFF
        if (v.id == R.id.img_light) {
            canBeRefresh = true
            if (currentLight!!.connectionStatus == ConnectionStatus.OFF.value) {
                TelinkLightService.Instance().sendCommandNoResponse(opcode, currentLight!!.meshAddr,
                        byteArrayOf(0x01, 0x00, 0x00))
                currentLight!!.connectionStatus = ConnectionStatus.ON.value
            } else {
                TelinkLightService.Instance().sendCommandNoResponse(opcode, currentLight!!.meshAddr,
                        byteArrayOf(0x00, 0x00, 0x00))
                currentLight!!.connectionStatus = ConnectionStatus.OFF.value
            }

            currentLight!!.updateIcon()
            DBUtils.updateLight(currentLight!!)
            runOnUiThread {
                adapter?.notifyDataSetChanged()
            }
        } else if (v.id == R.id.tv_setting) {
            if (scanPb.visibility != View.VISIBLE) {
                val intent = Intent(this@LightsOfGroupActivity, DeviceSettingActivity::class.java)
                intent.putExtra(Constant.LIGHT_ARESS_KEY, currentLight)
                intent.putExtra(Constant.GROUP_ARESS_KEY, group.meshAddr)
                intent.putExtra(Constant.LIGHT_REFRESH_KEY, Constant.LIGHT_REFRESH_KEY_OK)
                startActivityForResult(intent, REQ_LIGHT_SETTING)
            } else {
                ToastUtils.showShort(R.string.reconnecting)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 监听各种事件
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        this.mApplication?.addEventListener(NotificationEvent.ONLINE_STATUS, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
        LogUtils.d("____onStart")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lights_of_group)
        ButterKnife.bind(this)
        initToolbar()
        initParameter()
    }

    private fun initToolbar() {
        toolbar.setTitle(R.string.group_setting_header)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchView!!.clearFocus()
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if(newText!=null&&!newText.isEmpty()){
            filter(newText,true)
            adapter!!.notifyDataSetChanged()
        }else{
            filter(newText,false)
            adapter!!.notifyDataSetChanged()
        }

        return false
    }


    private fun filter(groupName:String?,isSearch:Boolean){
        val list = DBUtils.groupList
//        val nameList : ArrayList<String> = ArrayList()
        if(lightList!=null&&lightList.size>0){
            lightList.clear()
        }

//        for(i in list.indices){
//            nameList.add(list[i].name)
//        }

        if(isSearch){
            for(i in list.indices){
                if(groupName==list[i].name || (list[i].name).startsWith(groupName!!)){
                    lightList.addAll(DBUtils.getLightByGroupID(list[i].id))
                }
            }

        }else{
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
        this.group = this.intent.extras!!.get("group") as DbGroup
        this.mApplication = this.application as TelinkLightApplication
        mDataManager = DataManager(this, mApplication!!.mesh.name, mApplication!!.mesh.password)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        initData()
        initView()
        listenerConnect()
    }

    private fun listenerConnect() {
        LogUtils.d("____onResume")

        Thread{
            Thread.sleep(500)
            if(TelinkLightApplication.getInstance().connectDevice==null){
                launch(UI){
                    autoConnect()
                }
            }
        }.start()
//        if (TelinkLightService.Instance() != null && TelinkLightService.Instance().adapter.mLightCtrl.currentLight != null) {
//            Log.d("Saw", "isConnected = ${TelinkLightService.Instance().adapter.mLightCtrl.currentLight.isConnected}");
//
//            if (!TelinkLightService.Instance().adapter.mLightCtrl.currentLight.isConnected) {
//                autoConnect()
//            }
//        }else if(TelinkLightService.Instance().adapter.mLightCtrl.currentLight == null){
//            autoConnect()
//        }
    }

    override fun onStop() {
        super.onStop()
        this.mApplication!!.removeEventListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        //        this.mApplication.removeEventListener(this);
        canBeRefresh = false
    }

    private fun initData() {
        lightList = ArrayList()
        if (group.meshAddr == 0xffff) {
            //            lightList = DBUtils.getAllLight();
//            lightList=DBUtils.getAllLight()
            filter("",false)
        } else {
            lightList = DBUtils.getLightByGroupID(group.id)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (group.meshAddr == 0xffff) {
            getMenuInflater().inflate(R.menu.menu_search, menu)
            searchView = (menu!!.findItem(R.id.action_search)).actionView as SearchView
            searchView!!.setOnQueryTextListener(this)
            searchView!!.imeOptions=EditorInfo.IME_ACTION_SEARCH
            searchView!!.setQueryHint(getString(R.string.input_groupAdress))
            searchView!!.setSubmitButtonEnabled(true)
            searchView!!.backgroundColor=resources.getColor(R.color.blue)
            searchView!!.alpha=0.3f
        }
        return true
    }

    private fun initView() {
        if (group.meshAddr == 0xffff) {
            toolbar.title = getString(R.string.allLight)+" ("+lightList.size+")"
//            if(searchView==null){
//                toolbar.inflateMenu(R.menu.menu_search)
//                searchView = MenuItemCompat.getActionView(toolbar.menu.findItem(R.id.action_search)) as SearchView
//                searchView!!.setOnQueryTextListener(this)
//            }
        } else {
            toolbar.title = (group.name ?: "")+" ("+lightList.size+")"
        }
        recycler_view_lights.layoutManager = GridLayoutManager(this, 3)
        adapter = LightsOfGroupRecyclerViewAdapter(this, lightList, onCheckedChangeListener)
        recycler_view_lights.adapter = adapter
        for (i in lightList.indices) {
            lightList[i].updateIcon()
        }
    }


    override fun performed(event: Event<String>) {
        when (event.type) {
            NotificationEvent.ONLINE_STATUS -> this.onOnlineStatusNotify(event as NotificationEvent)
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)

            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                TelinkLog.d("MainActivity#performed#ERROR_REPORT: " + " stateCode-" + info.stateCode
                        + " errorCode-" + info.errorCode
                        + " deviceId-" + info.deviceId)
            }
        }//                this.onMeshOffline((MeshEvent) event);
        //                this.onServiceConnected((ServiceEvent) event);
        //                this.onServiceDisconnected((ServiceEvent) event);
        //                onNotificationEvent((NotificationEvent) event);
    }

    /**
     * 自动重连
     */
    private fun autoConnect() {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN).subscribe(Consumer {
            if (it) {
                //授予了权限
                if (TelinkLightService.Instance() != null) {

                    if (TelinkLightService.Instance().mode != LightAdapter.MODE_AUTO_CONNECT_MESH) {

                        ToastUtils.showLong(getString(R.string.connect_state))
                        SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)
                        scanPb.visibility = View.VISIBLE

                        if (this.mApplication!!.isEmptyMesh) {
                            return@Consumer
                        }
                        this.mApplication?.refreshLights()

                        val mesh = this.mApplication?.mesh

                        if (TextUtils.isEmpty(mesh?.name) || TextUtils.isEmpty(mesh?.password)) {
                            TelinkLightService.Instance().idleMode(true)
                            return@Consumer
                        }

                        val account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                                Constant.DB_NAME_KEY, "dadou")
                        //自动重连参数
                        val connectParams = Parameters.createAutoConnectParameters()
                        connectParams.setMeshName(mesh?.name)
                        if (SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(), Constant.USER_TYPE, Constant.USER_TYPE_OLD) == Constant.USER_TYPE_NEW) {
                            connectParams.setPassword(NetworkFactory.md5(
                                    NetworkFactory.md5(mesh?.password) + account))
                        } else {
                            connectParams.setPassword(mesh?.password)
                        }
                        connectParams.autoEnableNotification(true)

                        // 之前是否有在做MeshOTA操作，是则继续
                        if (mesh?.isOtaProcessing == true) {
                            connectParams.setConnectMac(mesh.otaDevice?.mac)
                        }
                        //自动重连
                        Thread {
                            TelinkLightService.Instance().autoConnect(connectParams)
                        }.start()


                    }

                    //刷新Notify参数
                    val refreshNotifyParams = Parameters.createRefreshNotifyParameters()
                    refreshNotifyParams.setRefreshRepeatCount(2)
                    refreshNotifyParams.setRefreshInterval(2000)
                    //开启自动刷新Notify
                    TelinkLightService.Instance().autoRefreshNotify(refreshNotifyParams)
                }
            } else {
                //没有授予权限
                DialogUtils.showNoBlePermissionDialog(this, { autoConnect() }, { finish() })
            }
        })

    }


    private fun onDeviceStatusChanged(event: DeviceEvent) {
        val deviceInfo = event.args
        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                scanPb.visibility = View.GONE
                adapter?.notifyDataSetChanged()
                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, true)
            }
            LightAdapter.STATUS_CONNECTING -> {
                Log.d("connectting", "444")
                scanPb.visibility = View.VISIBLE
            }
            LightAdapter.STATUS_LOGOUT -> {
                onLogout()
            }
            LightAdapter.STATUS_ERROR_N -> {
                onNError()
            }
        }
    }

    private fun onNError() {

        ToastUtils.showLong(getString(R.string.connect_fail))
        SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

        TelinkLightService.Instance().idleMode(true)
        TelinkLog.d("DeviceScanningActivity#onNError")

        val builder = AlertDialog.Builder(this)
        builder.setMessage("当前环境:Android7.0!连接重试:" + " 3次失败!")
        builder.setNegativeButton("confirm") { dialog, _ -> dialog.dismiss() }
        builder.setCancelable(false)
        builder.show()
    }

    private fun onLogout() {
//        runOnUiThread {
//            if (scanPb.visibility == View.VISIBLE) {
//                indefiniteSnackbar(root, R.string.connect_failed_if_there_are_lights, R.string.retry) {
//                    autoConnect()
//                }
//            }
//        }
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
                    DBUtils.updateLight(dbLight)
                    runOnUiThread {
                        adapter?.notifyDataSetChanged()
                    }

                }
            }
        }


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQ_LIGHT_SETTING -> {
                    initData()
                    adapter?.notifyDataSetChanged()
                    val isConnect = data?.getBooleanExtra("data", false) ?: false
                    if (isConnect) {
                        scanPb.visibility = View.VISIBLE
                    }
                }
            }

        }
    }
}

