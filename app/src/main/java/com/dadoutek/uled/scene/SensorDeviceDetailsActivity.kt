package com.dadoutek.uled.scene

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter.OnItemChildClickListener
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseToolbarActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.gateway.bean.GwStompBean
import com.dadoutek.uled.gateway.util.Base64Utils
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.Constant.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbSensor
import com.dadoutek.uled.model.HttpModel.GwModel
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.pir.ConfigSensorAct
import com.dadoutek.uled.pir.HumanBodySensorActivity
import com.dadoutek.uled.pir.PirConfigActivity
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.stomp.MqttBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_lights_of_group.*
import kotlinx.android.synthetic.main.activity_sensor_device_details.*
import kotlinx.android.synthetic.main.template_loading_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivity
import java.util.concurrent.TimeUnit


private const val MAX_RETRY_CONNECT_TIME = 5

/**
 * 描述	      ${人体感应器列表}$
 */
class SensorDeviceDetailsActivity : TelinkBaseToolbarActivity(), EventListener<String> {
    private var popVersion: TextView? = null
    private var isLogin: Boolean = false
    private var factory: TextView? = null
    private var disposableTimer: Disposable? = null
    private var disposable: Disposable? = null
    private val CONNECT_SENSOR_TIMEOUT: Long = 20000        //ms

    private var connectSensorTimeoutDisposable: Disposable? = null
    private lateinit var deviceInfo: DeviceInfo
    private var delete: TextView? = null
    private var group: TextView? = null
    private var ota: TextView? = null
    private var rename: TextView? = null
    private var views: View? = null
    private var isClick: Int = 5//
    private var settingType: Int = 0 //0是正常连接 1是点击修改 2是点击删除
    private val NORMAL_SENSOR: Int = 0 //0是正常连接 1是点击修改 2是点击删除
    private val RECOVER_SENSOR: Int = 1 //0是正常连接 1是点击修改 2是点击删除
    private val RESET_SENSOR: Int = 2 //0是正常连接 1是点击修改 2是点击删除
    private val OPEN_CLOSE: Int = 5 //
    private val OTA_SENSOR: Int = 3//3是oat
    private val SENSOR_FINISH: Int = 4//4代表操作完成

    private var sensorDatas: MutableList<DbSensor> = mutableListOf()
    private var adapter: SensorDeviceDetailsAdapter? = null
    private var retryConnectCount = 0
    private val connectFailedDeviceMacList: MutableList<String> = mutableListOf()
    private var currentDevice: DbSensor? = null
    private var positionCurrent: Int = 0
    private var mConnectDevice: DeviceInfo? = null
    private var acitivityIsAlive = true
    private var mApplication: TelinkLightApplication? = null
    private var install_device: TextView? = null
    private var create_group: TextView? = null
    private var create_scene: TextView? = null
    private var popupWindow: PopupWindow? = null
    private val SCENE_MAX_COUNT = 100
    private var compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = this.intent.getIntExtra(DEVICE_TYPE, 0)
        this.mApplication = this.application as TelinkLightApplication
        addScanListeners()
        LogUtils.v("zcl直连灯地址${TelinkLightApplication.getApp().connectDevice?.meshAddress}")
    }

    override fun editeDeviceAdapter() {
        adapter!!.changeState(isEdite)
        adapter!!.notifyDataSetChanged()
    }

    override fun setToolbar(): Toolbar {
        return toolbar
    }

    override fun gpAllVisible(): Boolean {
        return false
    }

    override fun setDeviceDataSize(num: Int): Int {
        return sensorDatas.size
    }

    override fun setLayoutId(): Int {
        return R.layout.activity_sensor_device_details
    }

    override fun setPositiveBtn() {
        currentDevice?.let {
            DBUtils.deleteSensor(it)
            sensorDatas.remove(it)
        }
        adapter?.notifyDataSetChanged()
        isEmptyDevice()
    }

    private fun isEmptyDevice() {
        if (sensorDatas.size > 0) {
            recycleView.visibility = View.VISIBLE
            no_device_relativeLayout.visibility = View.GONE
        } else {
            recycleView.visibility = View.GONE
            no_device_relativeLayout.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        if (compositeDisposable.isDisposed)
            compositeDisposable = CompositeDisposable()

        initData()
        initView()
        popupWindow?.dismiss()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.dispose()
        isClick = 5
        LogUtils.d("connectSensorTimeoutDisposable?.dispose()")
        connectSensorTimeoutDisposable?.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        acitivityIsAlive = false
        if (popupWindow != null && popupWindow!!.isShowing)
            popupWindow!!.dismiss()
        compositeDisposable.dispose()
        isClick = 5

        this.mApplication?.removeEventListener(this)
    }


    private fun initView() {
        recycleView!!.layoutManager = GridLayoutManager(this, 2)
        adapter = SensorDeviceDetailsAdapter(R.layout.template_device_type_item, sensorDatas)
        adapter!!.bindToRecyclerView(recycleView)
        adapter!!.onItemChildClickListener = onItemChildClickListener

        install_device = findViewById(R.id.install_device)
        create_group = findViewById(R.id.create_group)
        create_scene = findViewById(R.id.create_scene)
        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)

        views = LayoutInflater.from(this).inflate(R.layout.popwindown_switch, null)
        group = views?.findViewById<TextView>(R.id.switch_group)
        ota = views?.findViewById<TextView>(R.id.ota)
        factory = views?.findViewById<TextView>(R.id.deleteBtn)
        delete = views?.findViewById<TextView>(R.id.deleteBtnNoFactory)
        rename = views?.findViewById<TextView>(R.id.rename)
        popVersion = views?.findViewById<TextView>(R.id.pop_version)
        popVersion?.text = getString(R.string.firmware_version) + currentDevice?.version
        popVersion?.visibility = View.VISIBLE

        rename?.visibility = View.VISIBLE
        delete?.visibility = View.VISIBLE
        ota?.visibility = View.VISIBLE
        group?.visibility = View.VISIBLE
        group?.text = getString(R.string.relocation)
        progressBar_sensor?.setOnClickListener {}

        add_device_btn.setOnClickListener {
            val lastUser = DBUtils.lastUser
            lastUser?.let {
                if (it.id.toString() != it.last_authorizer_user_id)
                    ToastUtils.showLong(getString(R.string.author_region_warm))
                else {
                    startActivity(Intent(this, ScanningSensorActivity::class.java))
                    doFinish()
                }
            }
        }//添加设备
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { doFinish() }
        toolbarTv.text = getString(R.string.sensor)
    }

    private val onClick = View.OnClickListener {
        when (it.id) {
            R.id.install_device -> {
                showInstallDeviceList()
            }
            R.id.create_group -> {
                dialog_pir?.visibility = View.GONE
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    //addNewGroup()
                    popMain.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
                }
            }
            R.id.create_scene -> {
                dialog_pir?.visibility = View.GONE
                val nowSize = DBUtils.sceneList.size
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    if (nowSize >= SCENE_MAX_COUNT) {
                        ToastUtils.showLong(R.string.scene_16_tip)
                    } else {
                        val intent = Intent(this, NewSceneSetAct::class.java)
                        intent.putExtra(Constant.IS_CHANGE_SCENE, false)
                        startActivity(intent)
                        connectSensorTimeoutDisposable?.dispose()
                        disposable?.dispose()
                    }
                }
            }
        }
    }

    private fun renameSesor() {
        if (!TextUtils.isEmpty(currentDevice?.name))
            renameEt?.setText(currentDevice?.name)
        renameEt?.setSelection(renameEt?.text.toString().length)

        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }

        renameConfirm?.setOnClickListener {    // 获取输入框的内容
            if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                currentDevice?.name = renameEt?.text.toString()
                DBUtils.saveSensor(currentDevice!!, true)
                sensorDatas[positionCurrent].name = renameEt?.text.toString()
                adapter?.notifyDataSetChanged()
                renameDialog.dismiss()
            }
        }
    }

    private fun showInstallDeviceList() {
        dialog_pir.visibility = View.GONE
        showInstallDeviceList(isGuide, isRgbClick)
    }

    private fun initData() {
        sensorDatas.clear()
        sensorDatas.addAll(DBUtils.getAllSensor())
//        LogUtils.e("zcl人体本地数据----------$sensorData")
        setScanningMode(true)
        isEmptyDevice()
    }


    var onItemChildClickListener = OnItemChildClickListener { _, view, position ->
        currentDevice = sensorDatas[position]
        positionCurrent = position
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showLong(getString(R.string.author_region_warm))
            else {
                when (view.id) {
                    R.id.template_device_setting -> {
                        showLoadingDialog(getString(R.string.please_wait))
                        connectAndConfig()
                    }
                    R.id.template_device_icon -> {
                        isClick = OPEN_CLOSE
                        if (TelinkApplication.getInstance().connectDevice == null && DBUtils.getAllGateWay().size > 0) {
                            sendToGw()
                        } else {
                            setOPenOrClose(position)
                        }
                    }
                    R.id.template_device_card_delete -> {
                        val string = getString(R.string.sure_delete_device, currentDevice?.name)
                        builder?.setMessage(string)
                        builder?.create()?.show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun connectAndConfig() {
        TelinkLightService.Instance()?.idleMode(true)
        Thread.sleep(200)
        val deviceTypes = mutableListOf(currentDevice?.productUUID ?: DeviceType.NIGHT_LIGHT)
        connect(macAddress = currentDevice?.macAddr, deviceTypes = deviceTypes)?.subscribe({
            relocationSensor()
        }, {
            hideLoadingDialog()
            ToastUtils.showShort(getString(R.string.connect_fail))
        })
    }

    private fun sendToGw() {
        GwModel.getGwList()?.subscribe(object : NetworkObserver<List<DbGateway>?>() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onNext(t: List<DbGateway>) {
                TelinkLightApplication.getApp().offLine = true
                hideLoadingDialog()
                t.forEach { db ->
                    //网关在线状态，1表示在线，0表示离线
                    if (db.state == 1)
                        TelinkLightApplication.getApp().offLine = false
                }

                if (!TelinkLightApplication.getApp().offLine) {
                    disposableTimer?.dispose()
                    disposableTimer = Observable.timer(7000, TimeUnit.MILLISECONDS).subscribe {
                        hideLoadingDialog()
                        runOnUiThread { ToastUtils.showShort(getString(R.string.gate_way_offline)) }
                    }
                    showLoadingDialog(getString(R.string.please_wait))
                    val low = currentDevice?.meshAddr ?: 0 and 0xff
                    val hight = (currentDevice?.meshAddr ?: 0 shr 8) and 0xff
                    val gattBody = GwGattBody()
                    var gattPar: ByteArray
                    if (currentDevice?.openTag == 1) {
                        gattPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, low.toByte(), hight.toByte(), Opcode.LIGHT_ON_OFF, 0x11, 0x02,
                                0x02, 0, 0, 0, 0, 0, 0, 0, 0, 0)//0关闭
                        gattBody.ser_id = Constant.SER_ID_SENSOR_OFF
                    } else {
                        gattPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, low.toByte(), hight.toByte(), Opcode.LIGHT_ON_OFF, 0x11, 0x02,
                                0x02, 1, 0, 0, 0, 0, 0, 0, 0, 0)//打开
                        gattBody.ser_id = SER_ID_SENSOR_ON
                    }

                    val s = Base64Utils.encodeToStrings(gattPar)
                    gattBody.data = s
                    gattBody.cmd = Constant.CMD_MQTT_CONTROL
                    gattBody.meshAddr = currentDevice?.meshAddr ?: 0
                    sendToServer(gattBody)
                } else {
                    ToastUtils.showShort(getString(R.string.gw_not_online))
                }
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                hideLoadingDialog()
                ToastUtils.showShort(getString(R.string.gw_not_online))
            }
        })
    }

    private fun sendToServer(gattBody: GwGattBody) {
        GwModel.sendDeviceToGatt(gattBody)?.subscribe(object : NetworkObserver<String?>() {
            override fun onNext(t: String) {
                disposableTimer?.dispose()
                if (currentDevice?.openTag == 1) {
                    sendCloseIcon(positionCurrent)
                    byteArrayOf(2, 0, 0, 0, 0, 0, 0, 0)//0关闭
                } else {
                    sendOpenIcon(positionCurrent)
                    byteArrayOf(2, 1, 0, 0, 0, 0, 0, 0)//1打开
                }
                hideLoadingDialog()

                disposableTimer?.dispose()
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                disposableTimer?.dispose()
                ToastUtils.showShort(e.message)
                hideLoadingDialog()
                LogUtils.v("zcl-----------远程控制-------${e.message}")
            }
        })
    }

    @SuppressLint("CheckResult")
    private fun setOPenOrClose(position: Int) {
        if (currentDevice?.version != null && (currentDevice?.version ?: "0").contains("NPR")) {//2.0 11位固定为2  12位0 关闭，1 打开
            val byteArrayOf = if (currentDevice?.openTag == 1) {
                sendCloseIcon(position)
                byteArrayOf(2, 0, 0, 0, 0, 0, 0, 0)//0关闭
            } else {
                sendOpenIcon(position)
                byteArrayOf(2, 1, 0, 0, 0, 0, 0, 0)//1打开
            }
            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT, currentDevice?.meshAddr ?: 0, byteArrayOf)
            DBUtils.saveSensor(sensorDatas[position], true)
            adapter?.notifyDataSetChanged()
        } else {
            ToastUtils.showShort(getString(R.string.dissupport))
        }
    }

    private fun sendOpenIcon(position: Int) {
        sensorDatas[position].openTag = 1
        sensorDatas[position].updateIcon()
        ToastUtils.showShort(getString(R.string.open))
    }

    private fun sendCloseIcon(position: Int) {
        sensorDatas[position].openTag = 0
        sensorDatas[position].updateIcon()
        ToastUtils.showShort(getString(R.string.close))
    }

    @SuppressLint("CheckResult")
    private fun autoConnectSensor(b: Boolean) {
        retryConnectCount++
        GlobalScope.launch(Dispatchers.Main) {
            if (b)
                showLoadingDialog(getString(R.string.please_wait))
        }
        LogUtils.e("zcl开始连接")
        //自动重连参数
        val connectParams = Parameters.createAutoConnectParameters()
        connectParams?.setMeshName(DBUtils.lastUser?.controlMeshName)
        connectParams?.setConnectMac(currentDevice?.macAddr)
        connectParams?.setPassword(NetworkFactory.md5(NetworkFactory.md5(DBUtils.lastUser?.controlMeshName) + DBUtils.lastUser?.controlMeshName).substring(0, 16))
        connectParams?.autoEnableNotification(true)
        connectParams?.setTimeoutSeconds(CONNECT_SENSOR_TIMEOUT.toInt())
        progressBar_sensor.visibility = View.VISIBLE
        //连接，如断开会自动重连
        GlobalScope.launch {
            delay(1000)
            TelinkLightService.Instance()?.autoConnect(connectParams)
        }

        //确保上一个订阅被取消了
        connectSensorTimeoutDisposable?.dispose()
        connectSensorTimeoutDisposable = Observable.timer(CONNECT_SENSOR_TIMEOUT, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe({
            LeBluetooth.getInstance().stopScan()
            TelinkLightService.Instance()?.idleMode(true)
            hideLoadingDialog()
            progressBar_sensor.visibility = View.GONE
            ToastUtils.showLong(getString(R.string.connect_fail))
        }, {
            LogUtils.e(it)
        })
        compositeDisposable.add(connectSensorTimeoutDisposable!!)
    }


    /**
     * 恢复出厂设置
     */
    private fun resetSensor() {
        isClick = SENSOR_FINISH
        //mesadddr发0就是代表只发送给直连灯也就是当前连接灯 也可以使用当前灯的mesAdd 如果使用mesadd 有几个pir就恢复几个
        val disposableReset = Commander.resetDevice(currentDevice?.meshAddr ?: 0, true)
                .subscribe(
                        {
                            hideLoadingDialog()
                            ToastUtils.showShort(getString(R.string.reset_factory_success))
                            currentDevice?.let {
                                DBUtils.deleteSensor(it)
                                sensorDatas.remove(it)
                            }
                            adapter?.notifyDataSetChanged()
                            isEmptyDevice()
                            if (isLogin)
                                TelinkLightService.Instance()?.idleMode(true)
                        }, {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.reset_factory_fail))
                }
                )
        connectSensorTimeoutDisposable?.dispose()
    }

    override fun performed(event: Event<String>) {
        when (event.type) {
            LeScanEvent.LE_SCAN_TIMEOUT -> {
                LogUtils.e("zcl", "zcl******LE_SCAN_TIMEOUT")
                progressBar_sensor.visibility = View.GONE
                hideLoadingDialog()
            }

            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReportNormal(info)
            }

            DeviceEvent.STATUS_CHANGED -> {
                var status = (event as DeviceEvent).args.status
                when (status) {
                    LightAdapter.STATUS_LOGIN -> {//3
                        isLogin = true
                        progressBar_sensor.visibility = View.GONE
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                        when (isClick) {//重新配置
                            RECOVER_SENSOR -> {
                                disposable?.dispose()
                                disposable = Observable.timer(2, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                                        .subscribe {
                                            relocationSensor()
                                        }
                                disposableConnectTimer?.dispose()
                            }
                            OTA_SENSOR -> {//人体感应器ota
                                getVersion(true)
                            }
                            RESET_SENSOR -> resetSensor() //恢复出厂设置
                        }
                    }
                    /**
                     *settingType //0是正常连接 1是点击修改 2是点击删除3是oat   NORMAL_SENSOR = 0
                     *RECOVER_SENSOR = 1  RESET_SENSOR= 2   OTA_SENSOR = 3
                     * STATUS_CONNECTING = 0;STATUS_CONNECTED = 1;STATUS_LOGINING = 2;STATUS_LOGIN = 3;STATUS_LOGOUT = 4;
                     */
                    LightAdapter.STATUS_LOGOUT -> {//4
                        isLogin = false
                        if (isClick == OPEN_CLOSE)
                            return
                        if (isClick != RESET_SENSOR && isClick != SENSOR_FINISH)//恢复
                            retryConnect()
                        progressBar_sensor.visibility = View.GONE

                        when (settingType) {
                            RESET_SENSOR -> {//恢复出厂设置成功后判断灯能扫描
                                Toast.makeText(this@SensorDeviceDetailsActivity, R.string.reset_factory_success, Toast.LENGTH_LONG).show()
                                connectSensorTimeoutDisposable?.dispose()
                                if (currentDevice != null)
                                    DBUtils.deleteSensor(currentDevice!!)
                                hideLoadingDialog()
                                notifyData()//重新设置传感器数量
                                DeviceType
                                settingType = NORMAL_SENSOR
                                if (mConnectDevice != null) {
                                    LogUtils.d(this.javaClass.simpleName, "mConnectDevice.meshAddress = " + mConnectDevice?.meshAddress)
                                    LogUtils.d(this.javaClass.simpleName, "light.getMeshAddr() = " + currentDevice?.meshAddr)

                                    if (currentDevice?.meshAddr == mConnectDevice?.meshAddress) {
                                        GlobalScope.launch {
                                            //踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
                                            delay(1000)
                                            if (this@SensorDeviceDetailsActivity == null || this@SensorDeviceDetailsActivity.isDestroyed ||
                                                    this@SensorDeviceDetailsActivity.isFinishing || !acitivityIsAlive) {
                                            } else
                                                autoConnectSensor(false)
                                        }
                                    }
                                }
                            }
                        }
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                    }
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun getVersion(isOTA: Boolean) {
        if (TelinkApplication.getInstance().connectDevice != null) {
            progressBar_sensor.visibility = View.GONE
            connectSensorTimeoutDisposable?.dispose()
            if (currentDevice != null && currentDevice?.meshAddr != null) {
                LogUtils.v("zcl------------------连接地址${currentDevice!!.meshAddr}")
                Commander.getDeviceVersion(currentDevice!!.meshAddr)
                        .subscribe(
                                { s ->
                                    hideLoadingDialog()
                                    if ("" != s) {
                                        currentDevice!!.version = s
                                        DBUtils.saveSensor(currentDevice!!, false)
                                        skipeDevice(isOTA, s)
                                        isClick = SENSOR_FINISH
                                    } else {
                                        skipeDevice(isOTA, currentDevice?.version ?: "")
                                        hideLoadingDialog()
                                    }
                                },
                                {
                                    hideLoadingDialog()
                                    skipeDevice(isOTA, currentDevice?.version ?: "")
                                }
                        )
            } else {
                ToastUtils.showLong(getString(R.string.get_version_fail))
            }
        }
    }

    private fun SensorDeviceDetailsActivity.skipeDevice(isOTA: Boolean, s: String) {
        if (isOTA) {
            currentDevice?.version = s
            var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), IS_DEVELOPER_MODE, false)
            if (isBoolean) {
                transformView()
            } else {
                if (OtaPrepareUtils.instance().checkSupportOta(s)!!) {
                    OtaPrepareUtils.instance().gotoUpdateView(this@SensorDeviceDetailsActivity, s, otaPrepareListner)
                } else {
                    ToastUtils.showLong(getString(R.string.version_disabled))
                    hideLoadingDialog()
                }
            }
        } else {
            LogUtils.v("zcl传感器判断------------${currentDevice?.version}------$s")
            when (deviceInfo.productUUID) {
                DeviceType.SENSOR -> {//老版本人体感应器
                    currentDevice?.version = s
                    if (""==s)
                        startActivity<PirConfigActivity>("deviceInfo" to deviceInfo, "version" to s)
                    else
                        startActivity<ConfigSensorAct>("deviceInfo" to deviceInfo, "version" to s)
                    doFinish()
                }
                DeviceType.NIGHT_LIGHT -> {//2.0
                    if (""==s||(s.contains("N")||s.contains("PR")||s.contains("NPR"))){
                        if (s.contains("NPR")||""==s)
                            startActivity<PirConfigActivity>("deviceInfo" to deviceInfo, "version" to s)
                        else
                            startActivity<HumanBodySensorActivity>("deviceInfo" to deviceInfo, "update" to "0", "version" to s)
                        doFinish()
                    }else{
                        connectAndConfig()
                    }
                }
            }

        }
    }

    private var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {

        override fun downLoadFileStart() {
            showLoadingDialog(getString(R.string.get_update_file))
        }

        override fun startGetVersion() {
            showLoadingDialog(getString(R.string.verification_version))
        }

        override fun getVersionSuccess(s: String) {
            hideLoadingDialog()
        }

        override fun getVersionFail() {
            ToastUtils.showLong(R.string.verification_version_fail)
            hideLoadingDialog()
        }


        override fun downLoadFileSuccess() {
            hideLoadingDialog()
            transformView()
        }

        override fun downLoadFileFail(message: String) {
            hideLoadingDialog()
            ToastUtils.showLong(R.string.download_pack_fail)
        }
    }

    private fun transformView() {
        connectSensorTimeoutDisposable?.dispose()
        disposable?.dispose()
        val intent = Intent(this@SensorDeviceDetailsActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.OTA_MAC, currentDevice?.macAddr)
        intent.putExtra(Constant.OTA_MES_Add, currentDevice?.meshAddr)
        intent.putExtra(Constant.OTA_VERSION, currentDevice?.version)
        intent.putExtra(Constant.OTA_TYPE, DeviceType.SENSOR)
        startActivity(intent)
        finish()
    }


    /**
     * 重新配置 小心进入后断开
     */
    @SuppressLint("CheckResult")
    private fun relocationSensor() {
        deviceInfo = DeviceInfo()
        currentDevice?.let {
            deviceInfo.meshAddress = it.meshAddr
            deviceInfo.macAddress = it.macAddr
            deviceInfo.productUUID = it.productUUID
            deviceInfo.id = it.id.toString()
            deviceInfo.isConfirm = 1
        }
        settingType = NORMAL_SENSOR
        getVersion(false)
    }


    private fun addScanListeners() {
        this.mApplication?.removeEventListener(this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)//扫描jt
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)//超时jt
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this)//结束jt
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)//设备状态JT
        this.mApplication?.addEventListener(DeviceEvent.CURRENT_CONNECT_CHANGED, this)//设备状态JT
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)//设备状态JT
    }

    /**
     * 重试
     * 先扫描到信号比较好的，再连接。
     */
    private fun retryConnect() {
        if (retryConnectCount < MAX_RETRY_CONNECT_TIME) {
            retryConnectCount++
            autoConnectSensor(true)
        } else {
            TelinkLightService.Instance()?.idleMode(true)

            if (scanPb != null && !scanPb.isShown) {
                retryConnectCount = 0
                connectFailedDeviceMacList.clear()
            }
        }
    }

    fun notifyData() {
        val mOldData: MutableList<DbSensor>? = sensorDatas
        val mNewData: MutableList<DbSensor>? = getNewData()
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return mOldData?.get(oldItemPosition)?.id?.equals(mNewData?.get
                (newItemPosition)?.id) ?: false
            }

            override fun getOldListSize(): Int {
                return mOldData?.size ?: 0
            }

            override fun getNewListSize(): Int {
                return mNewData?.size ?: 0
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val beanOld = mOldData?.get(oldItemPosition)
                val beanNew = mNewData?.get(newItemPosition)
                return if (!beanOld?.name.equals(beanNew?.name)) {
                    return false//如果有内容不同，就返回false
                } else true

            }
        }, true)
        adapter?.let { diffResult.dispatchUpdatesTo(it) }
        sensorDatas = mNewData!!
        adapter!!.setNewData(sensorDatas)
        if (sensorDatas.size <= 0) {
            no_device_relativeLayout.visibility = View.VISIBLE
            recycleView.visibility = View.GONE
        } else {
            no_device_relativeLayout.visibility = View.GONE
            recycleView.visibility = View.VISIBLE
        }
    }

    private fun getNewData(): MutableList<DbSensor> {
        sensorDatas = DBUtils.getAllSensor()
        toolbarTv.text = (currentDevice?.name ?: "")
        return sensorDatas
    }

    private fun doFinish() {
        connectSensorTimeoutDisposable?.dispose()
        disposable?.dispose()
        finish()
    }

    override fun receviedGwCmd2500(gwStompBean: GwStompBean) {
        disposableTimer?.dispose()
        when (gwStompBean.ser_id.toInt()) {
            SER_ID_SENSOR_ON -> sendOpenIcon(positionCurrent)
            SER_ID_SENSOR_OFF -> sendCloseIcon(positionCurrent)
        }
        adapter?.notifyDataSetChanged()
    }

    override fun receviedGwCmd2500M(gwStompBean: MqttBodyBean) {
        disposableTimer?.dispose()
        when (gwStompBean.ser_id.toInt()) {
            SER_ID_SENSOR_ON -> sendOpenIcon(positionCurrent)
            SER_ID_SENSOR_OFF -> sendCloseIcon(positionCurrent)
        }
        adapter?.notifyDataSetChanged()
    }

}
