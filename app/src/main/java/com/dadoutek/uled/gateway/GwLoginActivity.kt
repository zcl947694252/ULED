package com.dadoutek.uled.gateway

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Network
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.Utils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander.getDeviceVersion
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.NetWorkUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.LightAdapter
import com.telink.util.Arrays
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_gw_login.*
import kotlinx.android.synthetic.main.bottom_version_ly.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2020/3/26 14:10
 * 描述 设置wifi不需要发送标签头 并且只能通过本地设置
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GwLoginActivity : TelinkBaseActivity(), EventListener<String> {
    private var disposableTimer: Disposable? = null
    private var dbGw: DbGateway? = null
    private lateinit var mApp: TelinkLightApplication

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gw_login)
        initView()
        initData()
        initListener()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initListener() {
        gw_login_skip.setOnClickListener {
            if (TelinkLightApplication.getApp().connectDevice != null) {
                skipEvent()
            } else {
                ToastUtils.showShort(getString(R.string.device_disconnected))
                startActivity(Intent(this@GwLoginActivity, GwDeviceDetailActivity::class.java))
                finish()
            }
        }
        gw_login.setOnClickListener {
            val account = gw_login_account.text.toString()
            val pwd = gw_login_pwd.text.toString()
            if (TextUtils.isEmpty(account) || TextUtils.isEmpty(pwd)) {
                ToastUtils.showShort(getString(R.string.wifi_cannot_be_empty))
            } else {
                // sendWIFIParmars("Dadou", "Dadoutek2018")
                showLoadingDialog(getString(R.string.config_setting_gw_wifi))
                sendWIFIParmars(account, pwd)
            }
        }
    }

    private fun skipEvent() {
        val intent = Intent(this@GwLoginActivity, GwEventListActivity::class.java)
        intent.putExtra("data", dbGw)
        startActivity(intent)
        finish()
    }

    private fun sendDeviceMacParmars() {
        var params = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        TelinkLightService.Instance()?.sendCommandResponse(Opcode.CONFIG_GW_GET_MAC, dbGw?.meshAddr
                ?: 0, params, "0")
    }

    override fun performed(event: Event<String>?) {
        if (event is DeviceEvent) {
            this.onDeviceEvent(event)
        }
    }

    private fun onDeviceEvent(event: DeviceEvent) {
        when (event.type) {
            DeviceEvent.STATUS_CHANGED -> {
                when (event.args.status) {
                    LightAdapter.STATUS_LOGIN -> {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                        Log.e("zcl", "zcl***STATUS_LOGIN***")
                    }
                    LightAdapter.STATUS_LOGOUT -> {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                        Log.e("zcl", "zcl***STATUS_LOGOUT***----------")
                        ToastUtils.showShort(getString(R.string.connecting_tip))
                        connect(macAddress = dbGw?.macAddr, retryTimes = 10)
                                ?.subscribe({}, { LogUtils.d("connect failed") })
                    }

                    //获取设备mac
                    LightAdapter.STATUS_SET_GW_COMPLETED -> {//Dadou   Dadoutek2018
                        //mac信息获取成功
                        val deviceInfo = event.args
                        LogUtils.v("zcl-----------获取网关相关返回信息-------$deviceInfo")

                        when (deviceInfo.gwVoipState) {
                            Constant.GW_WIFI_VOIP -> {
                                    GlobalScope.launch(Dispatchers.Main) {
                                    disposableTimer?.dispose()
                                    hideLoadingDialog()
                                    if (deviceInfo.gwWifiState == 0) {
                                        ToastUtils.showShort(getString(R.string.config_success))
                                        val boolean = SharedPreferencesHelper.getBoolean(this@GwLoginActivity, Constant.IS_GW_CONFIG_WIFI, false)
                                        if (boolean) {
                                            startActivity(Intent(this@GwLoginActivity, GwDeviceDetailActivity::class.java))
                                            finish()
                                        } else
                                            skipEvent()
                                    } else
                                        ToastUtils.showLong(getString(R.string.config_WIFI_FAILE))
                                }
                            }

                            Constant.GW_TIME_ZONE_VOIP -> {
                                GlobalScope.launch(Dispatchers.Main) {
                                    disposableTimer?.dispose()
                                    hideLoadingDialog()
                                }
                            }
                        }
                    }

                    //获取设备mac
                    LightAdapter.STATUS_GET_DEVICE_MAC_COMPLETED -> {
                        //mac信息获取成功
                        val deviceInfo = event.args
                        LogUtils.v("zcl-----------蓝牙数据获取设备的macaddress-------$deviceInfo--------------${deviceInfo.sixByteMacAddress}")
                        dbGw?.sixByteMacAddr = deviceInfo.sixByteMacAddress
                    }
                    LightAdapter.STATUS_GET_DEVICE_MAC_FAILURE -> {
                        LogUtils.v("zcl-----------蓝牙数据-get DeviceMAC fail------")
                    }
                }
            }
        }
    }

    private fun sendWIFIParmars(account: String, pwd: String) {
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(15000, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    runOnUiThread { ToastUtils.showLong(getString(R.string.config_WIFI_FAILE)) }
                }
        showLoadingDialog(getString(R.string.please_wait))
        val byteAccount = account.toByteArray()
        val bytePwd = pwd.toByteArray()
        val listAccount = getParmarsList(byteAccount)
        val listPwd = getParmarsList(bytePwd)
        val accountByteSize = byteAccount.size.toByte()
        val pwdByteSize = bytePwd.size.toByte()

        LogUtils.v("zcl----蓝牙数据账号list-------${Arrays.bytesToHexString(byteAccount, ",")}----------${listAccount.size}-----$accountByteSize")
        LogUtils.v("zcl----蓝牙数据密码list-------${Arrays.bytesToHexString(bytePwd, ",")}----------${listPwd.size}----$pwdByteSize")

        sendParmars(listAccount, accountByteSize, false)
        sendParmars(listPwd, pwdByteSize, true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendTimeZoneParmars() {
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(1500, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    runOnUiThread {
                        hideLoadingDialog()
                        ToastUtils.showLong(getString(R.string.get_time_zone_fail))
                    }
                }
        showLoadingDialog(getString(R.string.please_wait))
        val default = TimeZone.getDefault()
        val name = default.getDisplayName(true, TimeZone.SHORT)
        val split = if (name.contains("+")) //0正时区 1负时区
            name.split("+")
        else
            name.split("-")

        val time = split[1].split(":")// +/- 08:46
        val tzHour = if (name.contains("+"))
            time[0].toInt() or (0b00000000)
        else
            time[0].toInt() or (0b10000000)

        val tzMinutes = time[1].toInt()

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour: Int = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val week = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val yearH = (year shr 8) and (0xff)
        val yearL = year and (0xff)

        var labHeadPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0, 0,
                Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_TASK, 0x11, 0x02, tzHour.toByte(), tzMinutes.toByte(), yearH.toByte(),
                yearL.toByte(), month.toByte(), day.toByte(), hour.toByte(), minute.toByte(), second.toByte(), week.toByte())


        var params = byteArrayOf(tzHour.toByte(), tzMinutes.toByte(), yearH.toByte(),
                yearL.toByte(), month.toByte(), day.toByte(), hour.toByte(), minute.toByte(), second.toByte(), week.toByte())
        TelinkLightService.Instance()?.sendCommandResponse(Opcode.CONFIG_GW_SET_TIME_ZONE, dbGw?.meshAddr ?: 0, params, "1")
    }

    private fun sendParmars(listParmars: MutableList<ByteArray>, byteSize: Byte, isPwd: Boolean) {
        var num = 500L
        GlobalScope.launch(Dispatchers.Main) {
            for (i in 0 until listParmars.size) {
                delay(num * i)
                //11-18 11位labelId
                val offset = i * 8
                val bytesArray = listParmars[i]
                var params = byteArrayOf(byteSize, offset.toByte(), bytesArray[0],
                        bytesArray[1], bytesArray[2], bytesArray[3], bytesArray[4], bytesArray[5], bytesArray[6], bytesArray[7])

                if (isPwd) {
                    LogUtils.v("zcl----------蓝牙数据密码参数-------${Arrays.bytesToHexString(params, ",")}")
                    TelinkLightService.Instance().sendCommandResponse(Opcode.CONFIG_GW_WIFI_PASSWORD, dbGw?.meshAddr
                            ?: 0, params, "1")
                } else {
                    LogUtils.v("zcl----------蓝牙数据账号参数-------${Arrays.bytesToHexString(params, ",")}")
                    TelinkLightService.Instance().sendCommandResponse(Opcode.CONFIG_GW_WIFI_SDID, dbGw?.meshAddr
                            ?: 0, params, "1")
                }
            }
        }
    }

    private fun getParmarsList(bytes: ByteArray): MutableList<ByteArray> {
        val lastNum = bytes.size % 8
        val list = mutableListOf<ByteArray>()
        for (index in bytes.indices step 8) {
            var b: ByteArray
            if (index + 8 <= bytes.size) {
                b = ByteArray(8)
                System.arraycopy(bytes, index, b, 0, 8)
                list.add(b)
            } else {
                b = ByteArray(8)
                System.arraycopy(bytes, index, b, 0, lastNum)
                list.add(b)
            }
        }
        return list
    }

    private fun initData() {
        dbGw = intent.getParcelableExtra("data")

        val boolean = SharedPreferencesHelper.getBoolean(this, Constant.IS_GW_CONFIG_WIFI, false)
        if (boolean)
            gw_login_skip.visibility = View.GONE
        else
            gw_login_skip.visibility = View.VISIBLE

        if (TelinkLightApplication.getApp().isConnectGwBle) {
            val disposable = getDeviceVersion(dbGw!!.meshAddr).subscribe(
                    { s: String ->
                        dbGw!!.version = s
                        bottom_version_number.text = dbGw?.version
                        DBUtils.saveGateWay(dbGw!!, true)
                    }, {
                ToastUtils.showLong(getString(R.string.get_version_fail))
            })
        }
        setWIFI()

        val disposable = RxPermissions(this).request(Manifest.permission.CHANGE_WIFI_STATE)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .subscribe({
                    if (it) setWIFI()
                }, { LogUtils.d(it) })

        bottom_version_number.text = dbGw?.version

        // sendDeviceMacParmars()
    }

    private fun setWIFI() {
        GlobalScope.launch {
            if (NetworkUtils.isWifiAvailable() && NetworkUtils.isWifiConnected()) {
                var wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager?.let { itw ->
                    var currentWifiName = itw.connectionInfo.ssid
                    //删除首尾的 \"
                    currentWifiName = currentWifiName.substring(1, currentWifiName.length - 1)
                    runOnUiThread {
                        gw_login_account.setText(currentWifiName)
                        gw_login_account.setSelection(currentWifiName.length)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initView() {
        disableConnectionStatusListener()
        toolbar.title = getString(R.string.config_WIFI)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            startActivity(Intent(this@GwLoginActivity, GwDeviceDetailActivity::class.java))
            finish()
        }
        this.mApp = this.application as TelinkLightApplication
        this.mApp.removeEventListeners()
        this.mApp.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        if (TelinkLightApplication.getApp().isConnectGwBle)
            sendTimeZoneParmars()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableTimer?.dispose()
        this.mApp.removeEventListener(DeviceEvent.STATUS_CHANGED, this)
    }

    override fun receviedGwCmd2000(serId: String) {

    }


}