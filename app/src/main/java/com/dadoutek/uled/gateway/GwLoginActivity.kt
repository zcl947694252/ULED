package com.dadoutek.uled.gateway

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.gateway.bean.GwStompBean
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2020/3/26 14:10
 * 描述
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
                when {
                    //获取设备mac
                    event.args.status == LightAdapter.STATUS_SET_GW_COMPLETED -> {//Dadou   Dadoutek2018
                        //mac信息获取成功
                        val deviceInfo = event.args
                        LogUtils.v("zcl-----------获取网关相关返回信息-------$deviceInfo")

                        when (deviceInfo.gwVoipState) {
                            Constant.GW_WIFI_VOIP ->{
                                disposableTimer?.dispose()
                                hideLoadingDialog()
                                if (deviceInfo.gwWifiState == 0)
                                    skipEvent()
                                else
                                    ToastUtils.showLong(getString(R.string.config_WIFI_FAILE))
                            }

                            Constant.GW_TIME_ZONE_VOIP ->  {
                                disposableTimer?.dispose()
                                hideLoadingDialog()
                            }
                        }
                    }
                    //获取设备mac
                    event.args.status == LightAdapter.STATUS_GET_DEVICE_MAC_COMPLETED -> {
                        //mac信息获取成功
                        val deviceInfo = event.args
                        LogUtils.v("zcl-----------蓝牙数据获取设备的macaddress-------$deviceInfo--------------${deviceInfo.sixByteMacAddress}")
                        dbGw?.sixByteMacAddr = deviceInfo.sixByteMacAddress
                    }
                    event.args.status == LightAdapter.STATUS_GET_DEVICE_MAC_FAILURE -> {
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
                    ToastUtils.showLong(getString(R.string.config_WIFI_FAILE))
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

    private fun sendTimeZoneParmars() {
        disposableTimer?.dispose()
         disposableTimer = Observable.timer(1500, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
                 .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideLoadingDialog()
                    ToastUtils.showLong(getString(R.string.get_time_zone_fail))
                    finish()
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
        var params = byteArrayOf(tzHour.toByte(), tzMinutes.toByte(), yearH.toByte(),
                yearL.toByte(), month.toByte(), day.toByte(), hour.toByte(), minute.toByte(), second.toByte(), week.toByte())
        TelinkLightService.Instance()?.sendCommandResponse(Opcode.CONFIG_GW_SET_TIME_ZONE, dbGw?.meshAddr ?: 0, params,"1")
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
        for (index in 0 until bytes.size step 8) {
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
        dbGw = intent.getParcelableExtra<DbGateway>("data")
       // sendDeviceMacParmars()
    }

    private fun initView() {
        this.mApp = this.application as TelinkLightApplication
        this.mApp.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        if (TelinkLightApplication.getApp().isConnectGwBle)
            sendTimeZoneParmars()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableTimer?.dispose()
    }

    override fun receviedGwCmd2000(serId: String) {

    }

    override fun receviedGwCmd2500(gwStompBean: GwStompBean) {

    }
}