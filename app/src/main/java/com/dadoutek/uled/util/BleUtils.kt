package com.dadoutek.uled.util

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import com.blankj.utilcode.util.ActivityUtils
import android.bluetooth.BluetoothManager



object BleUtils {
    // 检查位置服务是否打开了
    fun isLocationEnable(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return gps || network
    }

    fun enableBluetooth(context: Context) {
        //初始化ble设配器
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val mBluetoothAdapter = manager.adapter
        //判断蓝牙是否开启，如果关闭则请求打开蓝牙
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled) {
            mBluetoothAdapter.enable();
        }
    }

    //跳转到位置服务设置
    fun jumpLocationSetting() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        ActivityUtils.startActivity(intent)
    }
}