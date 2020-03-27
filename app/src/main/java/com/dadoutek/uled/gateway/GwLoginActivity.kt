package com.dadoutek.uled.gateway

import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.LightAdapter
import com.telink.util.Event
import com.telink.util.EventListener
import kotlinx.android.synthetic.main.activity_gw_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


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
        gw_login.setOnClickListener {
            val account = gw_login_account.text.toString()
            val pwd = gw_login_pwd.text.toString()
            if (TextUtils.isEmpty(account) || TextUtils.isEmpty(pwd)) {
                ToastUtils.showShort(getString(R.string.wifi_cannot_be_empty))
            } else {
                //sendWIFIParmars(false, account)
                //sendWIFIParmars(false, pwd)
                sendDeviceMacParmars()
                var bytes = byteArrayOf(0xff.toByte(),0xff.toByte())
                val encoder = Base64.getEncoder()
                val s = encoder.encodeToString(bytes)
                LogUtils.v("zcl-----------s$s-------")
            }
        }
    }

    override fun performed(event: Event<String>?) {
        if (event is DeviceEvent) {
            this.onDeviceEvent(event)
        }
    }

    private fun onDeviceEvent(event: DeviceEvent) {
        when(event.type){
            DeviceEvent.STATUS_CHANGED ->{
                when {
                    //连接成功后获取firmware信息
                    event.args.status == LightAdapter.STATUS_CONNECTED -> TelinkLightService.Instance()?.deviceMac
                    //获取设备mac
                    event.args.status == LightAdapter.STATUS_GET_DEVICE_MAC_COMPLETED -> {
                        //mac信息获取成功
                        val deviceInfo = event.args
                        LogUtils.v("zcl-----------蓝牙数据获取设备的macaddress-------${deviceInfo.macAddress}")
                    }
                    event.args.status == LightAdapter.STATUS_GET_DEVICE_MAC_FAILURE -> {
                        LogUtils.v("zcl-----------蓝牙数据-get DeviceMAC fail------")
                    }
                }
            }
        }
    }

    private fun sendDeviceMacParmars() {
        var params = byteArrayOf(0, 0, 0, 0, 0, 0, 0,0)
        TelinkLightService.Instance()?.sendCommandResponse(Opcode.CONFIG_GW_GET_MAC, 11, params,"0")
        //TelinkLightService.Instance()?.deviceMac
    }

    private fun sendWIFIParmars(isAccount: Boolean, account: String) {
        val bytes = account.toByteArray()
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
        LogUtils.v("zcl----------发送命令list-------$list----------${list.size}")
        var num = 500L
        GlobalScope.launch(Dispatchers.Main) {
            for (i in 0 until list.size) {
                delay(num * i)
                //11-18 11位labelId
                val offset = i * 8
                val bytesArray = list[i]
                var params = byteArrayOf(bytes.size.toByte(), offset.toByte(), bytesArray[0],
                        bytesArray[1], bytesArray[2], bytesArray[3], bytesArray[4], bytesArray[5], bytesArray[6], bytesArray[7])

                if (isAccount)
                    TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_GW_WIFI_SDID, 11, params)
                else
                    TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_GW_WIFI_PASSWORD, 11, params)
                if (!isAccount&&i==list.size-1){
                    finish()
                }
            }
        }
    }

    private fun initData() {}

    private fun initView() {
        this.mApp = this.application as TelinkLightApplication
        this.mApp.addEventListener(DeviceEvent.STATUS_CHANGED, this)
    }
}