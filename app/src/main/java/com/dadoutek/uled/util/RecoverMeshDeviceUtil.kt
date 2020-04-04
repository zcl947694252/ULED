package com.dadoutek.uled.util

import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbConnector
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import com.polidea.rxandroidble2.scan.ScanSettings.SCAN_MODE_LOW_LATENCY
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


/**
 * 把不在数据库内，但是当前环境下存在的设备恢复进数据库
 *
 */
object RecoverMeshDeviceUtil {
    val rxBleClient: RxBleClient = RxBleClient.create(TelinkLightApplication.getApp())

    private val createdDeviceList: MutableList<DeviceInfo> = mutableListOf()  //需要重新配置的设备的mac地址

    val SCAN_TIMEOUT_SECONDS: Long = 20

    data class ResultOfAddDevice(val deviceInfo: DeviceInfo, val hasConflict: Boolean)

    private val scannedDeviceHm = hashMapOf<String, DeviceInfo>()


    private fun getAllDeviceAddressList(): List<Int> {
        val lights = DBUtils.allLight.map { it.meshAddr }
        val curtain = DBUtils.allCurtain.map { it.meshAddr }
        val relay = DBUtils.allRely.map { it.meshAddr }
        val addressList = mutableListOf<Int>()
        addressList.addAll(lights)
        addressList.addAll(curtain)
        addressList.addAll(relay)
        addressList.sortBy { it }
        return addressList

    }

    fun findMeshDevice(deviceName: String?): Observable<Int> {
        createdDeviceList.clear()

        val scanFilter = ScanFilter.Builder().setDeviceName(deviceName).build()
        val scanSettings = ScanSettings.Builder()
               // .setScanMode(SCAN_MODE_LOW_LATENCY)
                .build()


        LogUtils.d("findMeshDevice name = $deviceName")
        return rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .observeOn(Schedulers.io())
                .map {
                    val data = parseData(it)
                    data
                }          //解析数据
                .filter {
                    addDevicesToDb(it)   //当保存数据库成功时，才发射onNext
                }
                .map { deviceInfo ->
                    createdDeviceList.add(deviceInfo)
                    deviceInfo.meshAddress

                }
                .timeout(SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS) {
                    LogUtils.d("findMeshDevice name complete. size = ${createdDeviceList.size}")
                    it.onComplete()                     //如果过了指定时间，还搜不到缺少的设备，就完成
                }
                .observeOn(AndroidSchedulers.mainThread())
    }


    /**
     * 通过mesh网络添加Device，实际就是通过scan恢复设备，但返回的数据略有不同
     */
    fun addDeviceByMesh(deviceName: String?): Observable<ResultOfAddDevice> {
        scannedDeviceHm.clear()

        val scanFilter = ScanFilter.Builder().setDeviceName(deviceName).build()
        val scanSettings = ScanSettings.Builder()
                .setScanMode(SCAN_MODE_LOW_LATENCY)
                .build()

        return rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .observeOn(Schedulers.io())
                .map {

                    parseData(it)    //解析数据
                }
                .map { deviceInfo ->
                    val hasConflict = hasMeshAddrConflict(deviceInfo)
                    ResultOfAddDevice(deviceInfo, hasConflict)
                }
                .filter {
                    var isFilter: Boolean
                    isFilter = if (it.hasConflict) {   //有冲突不过滤掉
                        true
                    } else {  //无冲突就保存数据
                        addDevicesToDb(it.deviceInfo)       //addDevicesToDb返回False代表已有此数据，把isFilter设成false，过滤掉
                    }
                    //如果之前已经发送过的结果，就不发送了。
                    if (scannedDeviceHm.containsKey(it.deviceInfo.macAddress)) {
                        isFilter = false
//                        LogUtils.d("sent result , filtered it. ")
                    } else {
                        scannedDeviceHm[it.deviceInfo.macAddress] = it.deviceInfo
                    }

                    //false是过滤，true是不过滤
                    isFilter
                }
                .timeout(SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS) {
                    //                    LogUtils.d("restoreDeviceByMesh name complete. size = ${createdDeviceList.size}")
                    it.onComplete()                     //如果过了指定时间，还搜不到缺少的设备，就完成
                }
                .observeOn(AndroidSchedulers.mainThread())
    }


    /**
     * 是否有地址冲突
     */
    private fun hasMeshAddrConflict(deviceInfo: DeviceInfo): Boolean {
        var isConflict: Boolean = false
        val isExist = DBUtils.isDeviceExist(deviceInfo.meshAddress)
        if (isExist) {
            when (deviceInfo.productUUID) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB -> {
                    val dbLight = DBUtils.getLightByMeshAddr(deviceInfo.meshAddress)
                    isConflict = dbLight?.macAddr != deviceInfo.macAddress
                }
                DeviceType.SMART_RELAY -> {
                    val relay = DBUtils.getRelyByMeshAddr(deviceInfo.meshAddress)
                    isConflict = relay?.macAddr != deviceInfo.macAddress
                }

                DeviceType.SMART_CURTAIN -> {
                    val curtain = DBUtils.getCurtainByMeshAddr(deviceInfo.meshAddress)
                    isConflict = curtain?.macAddr != deviceInfo.macAddress
                }

                DeviceType.NIGHT_LIGHT -> {
                    val sensor = DBUtils.getSensorByMeshAddr(deviceInfo.meshAddress)
                    isConflict = sensor?.macAddr != deviceInfo.macAddress
                }

            }
        }
        return isConflict
    }


    /**
     * 如果该deviceInfo不存在于数据库，则创建
     * @return true 保存成功    false，无需保存
     */
    fun addDevicesToDb(deviceInfo: DeviceInfo): Boolean {
        val productUUID = deviceInfo.productUUID
        val isExist = DBUtils.isDeviceExist(deviceInfo.meshAddress)
        if (!isExist) {
            when (productUUID) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB -> {
                    val dbLightNew = DbLight()
                    dbLightNew.productUUID = productUUID
                    dbLightNew.connectionStatus = 0
                    dbLightNew.updateIcon()
                    dbLightNew.belongGroupId = DBUtils.groupNull?.id
                    dbLightNew.color = 0
                    dbLightNew.colorTemperature = 0
                    dbLightNew.meshAddr = deviceInfo.meshAddress
                    dbLightNew.name = TelinkLightApplication.getApp().getString(R.string.device_name)+dbLightNew.meshAddr
                    dbLightNew.macAddr = deviceInfo.macAddress
                    DBUtils.saveLight(dbLightNew, false)
                    LogUtils.d(String.format("create meshAddress=  %x", dbLightNew.meshAddr))
                }
                DeviceType.SMART_RELAY -> {
                    val relay = DbConnector()
                    relay.productUUID = productUUID
                    relay.connectionStatus = 0
                    relay.updateIcon()
                    relay.belongGroupId = DBUtils.groupNull?.id
                    relay.color = 0
                    relay.meshAddr = deviceInfo.meshAddress
                    relay.name = TelinkLightApplication.getApp().getString(R.string.device_name)+relay.meshAddr
                    relay.macAddr = deviceInfo.macAddress
                    DBUtils.saveConnector(relay, false)
                    LogUtils.d("create = $relay  " + relay.meshAddr)
                }

                DeviceType.SMART_CURTAIN -> {
                    val curtain = DbCurtain()
                    curtain.productUUID = productUUID
                    curtain.connectionStatus = 0
                    curtain.updateIcon()
                    curtain.belongGroupId = DBUtils.groupNull?.id
                    curtain.meshAddr = deviceInfo.meshAddress
                    curtain.name = TelinkLightApplication.getApp().getString(R.string.device_name)+curtain.meshAddr
                    curtain.macAddr = deviceInfo.macAddress
                    DBUtils.saveCurtain(curtain, false)
                    LogUtils.d("create = $curtain  " + curtain.meshAddr)
                }
            /*    DeviceType.NIGHT_LIGHT -> {
                    val sensor = DbSensor()
                    sensor.productUUID = productUUID
                    sensor.belongGroupId = DBUtils.groupNull?.id
                    sensor.meshAddr = deviceInfo.meshAddress
                    sensor.name = TelinkLightApplication.getApp().getString(R.string.unnamed)
                    sensor.macAddr = deviceInfo.macAddress
                    DBUtils.saveSensor(sensor, false)
                }*/
            }

        }

        return !isExist

    }

    /**
     * 解析数据
     * @return 返回DbLight对象
     */
    fun parseData(scanResult: ScanResult): DeviceInfo? {
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
            type = (scanRecord[position].toInt() and 0xFF)
            position++

            if (type == 0x09) {
                packetContentLength = packetSize - 1
                meshName = ByteArray(16)
                //scanRecord要复制的源数据 postion要复制的源数据的开始位置 meshanme 代表要被复制进去的数组
                //destPos代表被复制数据开始的位置 packetContentLength代表要复制的长度
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

                    val deviceInfo = DeviceInfo()
                    deviceInfo.rssi =scanResult.rssi
                    deviceInfo.meshAddress = meshAddress
                    deviceInfo.macAddress = scanResult.bleDevice.macAddress
                    deviceInfo.meshUUID = meshUUID
                    deviceInfo.productUUID = productUUID
                    return deviceInfo
                }
            }
            packetPosition += packetSize + 1
        }

        return null   //返回空
    }

/*
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
*/
}