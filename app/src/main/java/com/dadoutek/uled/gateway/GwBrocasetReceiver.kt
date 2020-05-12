package com.dadoutek.uled.gateway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.ImageView
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.LightService
import kotlinx.android.synthetic.main.toolbar.*

class GwBrocasetReceiver : BroadcastReceiver() {
    private var gwStateChangeListerner: GwStateChangeListerner? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        val deviceInfo: DeviceInfo = intent!!.getParcelableExtra(LightService.EXTRA_DEVICE)
        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                gwStateChangeListerner?.loginSuccess()
            }
            LightAdapter.STATUS_LOGOUT -> {
                gwStateChangeListerner?.loginFail()
            }
            LightAdapter.STATUS_SET_GW_COMPLETED -> {//Dadou   Dadoutek2018
                gwStateChangeListerner?.setGwComplete(deviceInfo)
            }
            //获取设备mac
            LightAdapter.STATUS_GET_DEVICE_MAC_COMPLETED -> {
                gwStateChangeListerner?.getMacComplete(deviceInfo)
            }
            LightAdapter.STATUS_GET_DEVICE_MAC_FAILURE -> {
                gwStateChangeListerner?.getMacFaile(deviceInfo)
            }
        }


    }

    interface GwStateChangeListerner {
        fun loginSuccess()
        fun loginFail()
        fun setGwComplete(deviceInfo: DeviceInfo){}
        fun getMacComplete(deviceInfo: DeviceInfo){}
        fun getMacFaile(deviceInfo: DeviceInfo){}
    }

    fun setOnGwStateChangeListerner(gwStateChangeListerner: GwStateChangeListerner) {
        this.gwStateChangeListerner = gwStateChangeListerner
    }
}
