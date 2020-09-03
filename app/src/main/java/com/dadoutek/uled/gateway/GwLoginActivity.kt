package com.dadoutek.uled.gateway

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.CmdBodyBean
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander.getDeviceVersion
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.NetWorkUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightService
import com.telink.util.Arrays
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
import org.json.JSONObject
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
class GwLoginActivity : TelinkBaseActivity() {
    private val TAG = "GwLoginActivity"
    private var isRouter: Boolean = false
    private var mac: String? = null
    private var receiver: GwBrocasetReceiver? = null
    private var disposableTimer: Disposable? = null
    private var dbGw: DbGateway? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gw_login)

        receiver = GwBrocasetReceiver()
        val filter = IntentFilter()
        filter.addAction(LightService.ACTION_STATUS_CHANGED)
        registerReceiver(receiver, filter)
        receiver?.setOnGwStateChangeListerner(object : GwBrocasetReceiver.GwStateChangeListerner {
            override fun loginSuccess() {
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
            }

            @SuppressLint("CheckResult")
            override fun loginFail() {
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                Log.e("zcl", "zcl***STATUS_LOGOUT***----------")
                ToastUtils.showShort(getString(R.string.connecting_tip))
                connect(macAddress = dbGw?.macAddr, retryTimes = 10)
                        ?.subscribe({}, { LogUtils.d("connect failed") })
            }

            override fun setGwComplete(deviceInfo: DeviceInfo) { //Dadou   Dadoutek2018
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

            override fun getMacComplete(deviceInfo: DeviceInfo) {
                //mac信息获取成功
                LogUtils.v("zcl-----------蓝牙数据获取设备的macaddress-------$deviceInfo--------${deviceInfo.sixByteMacAddress}")
                dbGw?.sixByteMacAddr = deviceInfo.sixByteMacAddress
            }

            override fun getMacFaile(deviceInfo: DeviceInfo) {
                LogUtils.v("zcl--------蓝牙数据-get DeviceMAC fail------")
            }
        })

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
                if (isRouter)
                    sendRouterWifi(account, pwd)
                else
                    sendWIFIParmars(account, pwd)
            }
        }
    }

    /**
     * 配置路由器wifi
     */
    @SuppressLint("CheckResult")
    private fun sendRouterWifi(account: String, pwd: String) {
        val timeZone = NetWorkUtils.getTimeZone()
        val split = timeZone.replace("GMT+", "").split(":")
        RouterModel.routerConfigWifi(mac!!, account, pwd, split[0].toInt(), split[1].toInt(), TAG)
                ?.subscribe({
                    showLoadingDialog(getString(R.string.config_success))
                    finish()
                }, {
                    ToastUtils.showShort(it.message)
                })
    }

    override fun routerConfigWIFI(cmdBody: CmdBodyBean) {
        if (TAG == cmdBody.ser_id) {
            if (cmdBody.status == Constant.ALL_SUCCESS) {
                ToastUtils.showShort(getString(R.string.config_WIFI_success))
                SharedPreferencesHelper.putBoolean(this, Constant.IS_GW_CONFIG_WIFI, false)
                ActivityUtils.finishAllActivities()
                ActivityUtils.startActivity(MainActivity::class.java)
            } else {
                ToastUtils.showShort(getString(R.string.config_WIFI_FAILE))
            }

            hideLoadingDialog()
            disposableTimer?.dispose()
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


    private fun sendWIFIParmars(account: String, pwd: String) {
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(30000, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    runOnUiThread {
                        hideLoadingDialog()
                        ToastUtils.showLong(getString(R.string.config_WIFI_FAILE))
                    }
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
                var params = byteArrayOf(byteSize, offset.toByte(), bytesArray[0], bytesArray[1], bytesArray[2], bytesArray[3],
                        bytesArray[4], bytesArray[5], bytesArray[6], bytesArray[7])

                if (isPwd) {
                    LogUtils.v("zcl----------蓝牙数据密码参数-------${Arrays.bytesToHexString(params, ",")}")
                    TelinkLightService.Instance().sendCommandResponse(Opcode.CONFIG_GW_WIFI_PASSWORD, dbGw?.meshAddr ?: 0, params, "1")
                } else {
                    LogUtils.v("zcl----------蓝牙数据账号参数-------${Arrays.bytesToHexString(params, ",")}")
                    TelinkLightService.Instance().sendCommandResponse(Opcode.CONFIG_GW_WIFI_SDID, dbGw?.meshAddr ?: 0, params, "1")
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

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("CheckResult")
    private fun initData() {
        isRouter = intent.getBooleanExtra("is_router", false)
        when {
            isRouter -> {
                mac = intent.getStringExtra("mac")

                LogUtils.v("zcl路由器-----------$mac-------")
            }
            else -> {
                if (TelinkLightApplication.getApp().isConnectGwBle)
                    sendTimeZoneParmars()

                if (!Constant.IS_ROUTE_MODE)
                    dbGw = intent.getParcelableExtra("data")

                if (SharedPreferencesHelper.getBoolean(this, Constant.IS_GW_CONFIG_WIFI, false))
                    gw_login_skip.visibility = View.GONE
                else
                    gw_login_skip.visibility = View.VISIBLE

                if (TelinkLightApplication.getApp().isConnectGwBle) {
                    dbGw?.let {
                        getDeviceVersion(it.meshAddr).subscribe({ s: String ->
                            it.version = s
                            bottom_version_number.text = it.version
                            DBUtils.saveGateWay(it, false)
                        }, {
                            ToastUtils.showLong(getString(R.string.get_version_fail))
                        })
                    }
                }
                bottom_version_number.text = dbGw?.version


                // sendDeviceMacParmars()
            }
        }
        setWIFI()
        RxPermissions(this).request(Manifest.permission.CHANGE_WIFI_STATE)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .subscribe({
                    if (it) setWIFI()
                }, { LogUtils.d(it) })
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

    private fun initView() {
        disableConnectionStatusListener()
        toolbarTv.text = getString(R.string.config_net)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        disposableTimer?.dispose()
    }
}

