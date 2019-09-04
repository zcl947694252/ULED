package com.dadoutek.uled.util

import android.text.TextUtils
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.internal.RxBleLog
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import java.util.*
import java.util.concurrent.TimeUnit


object MeshConflictFixUtil {
    private val rxBleClient: RxBleClient = RxBleClient.create(TelinkLightApplication.getApp())

    private  val scannedDeviceMap: TreeMap<Int, String> = TreeMap()  //扫描到的设备    key: meshAddress      value: macAddress
    private val needResetMeshAddrDeviceMacList: MutableList<String> = mutableListOf()  //需要重新配置的设备的mac地址


    private val SCAN_TIMEOUT_SECONDS: Long = 15

    fun scanConflictMesh(deviceName: String): Observable<MutableList<String>> {
        RxBleClient.setLogLevel(RxBleLog.DEBUG);
        scannedDeviceMap.clear()
        needResetMeshAddrDeviceMacList.clear()

        val scanFilter = ScanFilter.Builder().setDeviceName(deviceName).build()
        val scanSettings = ScanSettings.Builder().build()

        return rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .map { parseData(it) }          //解析数据，如果冲突了，返回mac地址
                .filter {
//                    val isEmpty =  it.trim().isNotEmpty()
//                    LogUtils.d("$it isEmpty = $isEmpty")
                   val ret =  it.isNotEmpty() and !needResetMeshAddrDeviceMacList.contains(it)
                    LogUtils.d("filter = $ret mac = $it")
                    ret
                }     //如果mac为空，说明没冲突，过滤掉
                .map { conflictMacAddr ->       //冲突的mac地址
                    LogUtils.d("conflictMacAddr = $conflictMacAddr")
                    if (!needResetMeshAddrDeviceMacList.contains(conflictMacAddr)) {
                        needResetMeshAddrDeviceMacList.add(conflictMacAddr)
                    }
                    needResetMeshAddrDeviceMacList
                }
                .timeout(SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS){
                    it.onComplete()                     //如果过了指定时间，还搜不到mesh冲突的设备，就直接complete
                }
                .takeLast(1)        //只onNext最后一次macList

    }

    private fun parseData(scanResult: ScanResult): String {
        val scanRecord = scanResult.scanRecord.bytes

        val length = scanRecord.size
        var packetPosition = 0
        var packetContentLength: Int
        var packetSize: Int
        var position: Int
        var type: Int
        var meshName: ByteArray? = null

        var rspData = 0

        while (packetPosition < length) {

            packetSize = scanRecord[packetPosition].toInt()

            if (packetSize == 0)
                break

            position = packetPosition + 1
            type = (scanRecord[position].toInt() and 0xFF).toInt()
            position++

            if (type == 0x09) {

                packetContentLength = packetSize - 1

//                if (packetContentLength > 16 || packetContentLength <= 0)
//                    return null

                meshName = ByteArray(16)
                System.arraycopy(scanRecord, position, meshName, 0, packetContentLength)
            } else if (type == 0xFF) {

                rspData++

                if (rspData == 2) {

                    val vendorId = ((scanRecord[position++].toInt() and 0xFF) shl 8) + (scanRecord[position++].toInt() and 0xFF)
//                    if (vendorId != Manufacture.getDefault().vendorId)
//                        return null

                    val meshUUID = (scanRecord[position++].toInt() and 0xFF) + (scanRecord[position++].toInt() and 0xFF shl 8)
                    position += 4
                    val productUUID = (scanRecord[position++].toInt() and 0xFF) + (scanRecord[position++].toInt() and 0xFF shl 8)
                    val status = scanRecord[position++].toInt() and 0xFF
                    val meshAddress = (scanRecord[position++].toInt() and 0xFF) + (scanRecord[position].toInt() and 0xFF shl 8)

                    //如果Map里已经包含了该meshAddr,就看mac是不是同一个
                    if (scannedDeviceMap.containsKey(meshAddress)) {
                        val mac = scannedDeviceMap[meshAddress]
                        if (mac == scanResult.bleDevice.macAddress) {
                            //如果mac地址和mesh地址都一样，就说明是同一个设备，没冲突

                        } else {
                            //如果mesh一样，mac不一样，就说明mesh地址冲突了，两个不同的设备有着相同的mesh地址
                            //把mac地址加到冲突的mac地址列表
//                            if (!needResetMeshAddrDeviceMacList.contains(scanResult.bleDevice.macAddress)) {
//                                needResetMeshAddrDeviceMacList.add(scanResult.bleDevice.macAddress)
//
//                            }
                            return scanResult.bleDevice.macAddress
                        }
                    } else {
                        //没包含就加进去
                        scannedDeviceMap[meshAddress] = scanResult.bleDevice.macAddress
                    }

//                    val light = LightPeripheral(device, scanRecord, rssi, meshName, meshAddress)
//                    light.putAdvProperty(LightPeripheral.ADV_MESH_NAME, meshName)
//                    light.putAdvProperty(LightPeripheral.ADV_MESH_ADDRESS, meshAddress)
//                    light.putAdvProperty(LightPeripheral.ADV_MESH_UUID, meshUUID)
//                    light.putAdvProperty(LightPeripheral.ADV_PRODUCT_UUID, productUUID)
//                    light.putAdvProperty(LightPeripheral.ADV_STATUS, status)

//                    return light
                }
            }

            packetPosition += packetSize + 1
        }

        return ""   //返回空
    }
}