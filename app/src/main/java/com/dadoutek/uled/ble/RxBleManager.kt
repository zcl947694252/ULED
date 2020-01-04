package com.dadoutek.uledtest.ble

import android.annotation.SuppressLint
import android.content.Context
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.HttpModel.RegionModel
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.jakewharton.rx.ReplayingShare
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.Timeout
import com.polidea.rxandroidble2.exceptions.BleException
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

object RxBleManager {
    private var regionList: MutableList<String>? = null
    public var isNeedRetry: Boolean = true
    private val serviceUUID: String = "19200d0c-0b0a-0908-0706-050403020100"
    private val charUUID: String = "19210d0c-0b0a-0908-0706-050403020100"
    private val manuDataSize = 18
    private val versionPrefix: Byte = 0xDD.toByte() //从0xDD开始就是版本号了
    private val dashChar: Byte = 0x2D        //'-'的ascii码
    private val versionCodeLength = 5       //-之后的版本号长度，2.1.8 为5个byte
    private val supportVersion = "15"           //支持新恢复出厂设置的版本，XX-15
    private val supportVersionWithDot = "3.5.0"     //支持新恢复出厂设置的版本，XX-3.5.1  正式版记得改回来
    private  var rxBleClient: RxBleClient? = null
    private val mHmScannedDevice = hashMapOf<String, RxBleDevice>()
    private var mIsScanning = false
    private val mHmConnectObservable = hashMapOf<RxBleDevice, Observable<RxBleConnection>>()
    private val mHmConnectDisposable = hashMapOf<RxBleDevice, Disposable>()


    fun init(context: Context) {
        rxBleClient = RxBleClient.create(context)
    }

    fun initData() {
        //regionList = SharedPreferencesUtils.getRegionNameList()
        regionList = getRegionList()  //不能使用包i装的response
    }

    @SuppressLint("CheckResult")
    private fun getRegionList(): MutableList<String> {
        var list = mutableListOf<String>()
        RegionModel.getRegionName()?.subscribe({
            if (it.errorCode==0){
                  for (i in it.data) {
                      LogUtils.v("zcl获取区域contromes名$i")
                      list.add(i)
                  }
            LogUtils.v("zcl获取区域contromes名列表$list-------------$it")
                  SharedPreferencesUtils.saveRegionNameList(list)
            }else{
                ToastUtils.showLong(it.message)
            }
        }, {
            LogUtils.v("zclzcl获取区域contromes--------$it")
        })

        return list
    }

    /**
     * getVersion
     */
     fun getVersion(result: ScanResult): String {
        val rssi: Int = result.rssi
        val mac: String = result.bleDevice?.macAddress ?: ""
        val manuData = result.scanRecord?.manufacturerSpecificData
        var version: String = ""
        if (manuData?.valueAt(0)?.size ?: 0 > manuDataSize) {
            val bytes: ByteArray = manuData?.valueAt(0)!!
            if (bytes[11] == versionPrefix) {
                var i = 12
                version = ""
                while (bytes[i] != dashChar) {
                    val b = bytes[i]
                    version += b.toChar()
                    i++
                }
                for (j in i..(i + versionCodeLength)) {
                    val b = bytes[j]
                    version += b.toChar()
                }
            }

        }
        return version
    }

    /**
     * 是否支持新的物理恢复出厂设置方法
     */
     fun isSupportHybridFactoryReset(version: String): Boolean {
        var isSupport = false
        if (version.isNotEmpty()) {
            isSupport = if (version.contains('.')) {
                version.split("-").last() >= supportVersionWithDot
            } else {
                version.split("-").last() >= supportVersion
            }
        }
        return isSupport
    }


    fun scan(deviceName: String? = null): Observable<ScanResult>? {
        mHmScannedDevice.clear()
        val scanFilter: ScanFilter = ScanFilter.Builder()
                .setManufacturerData(0x0211, byteArrayOf())
                .build()

        val scanSettings: ScanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
        return rxBleClient?.scanBleDevices(scanSettings, scanFilter)
                ?.filter {
                    val version = getVersion(it)
                    val b = isSupportHybridFactoryReset(version)
                    //返回true  说明是自己区域下的设备或者为恢复出厂的设备
                    var isMyDevice = isMyDevice(it.bleDevice.name)
                    LogUtils.v("zcl物理搜索设备名$b==============${it.bleDevice.name}-----------------${it.bleDevice.macAddress}")

                    version!=""&&!isMyDevice&&false
                }
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.doOnSubscribe {
                    mIsScanning = true
                }
                ?.doOnDispose {
                    //   LogUtils.d("doOnDispose mIsScanning = $mIsScanning")
                    mIsScanning = false
                }?.doOnError {
                    mIsScanning = false
                }?.retry()
    }

    private fun isMyDevice(name: String?): Boolean {
        var b = false
        if (regionList == null) {
            b = false
        } else
            for (rgName in regionList!!)
                if (name == rgName||name== Constant.DEFAULT_MESH_FACTORY_NAME)
                    b = true
        return b
    }


    fun isScanning(): Boolean {
        return mIsScanning
    }

    fun disconnectAllDevice() {
        for (disposable in mHmConnectDisposable.values) {
            disposable.dispose()
        }
    }

    fun connect(bleDevice: RxBleDevice, isFirst: Boolean = false): Observable<RxBleConnection> {
        val retObservable: Observable<RxBleConnection>
        if (mHmConnectObservable.contains(bleDevice)) {
            retObservable = mHmConnectObservable[bleDevice]!!
        } else {
            val connectionObservable = bleDevice.establishConnection(false, Timeout(6000, TimeUnit.MILLISECONDS))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(ReplayingShare.instance())
                    .doOnDispose {
                        mHmConnectObservable.remove(bleDevice)
                    }
                    .doOnSubscribe {
                        mHmConnectDisposable[bleDevice] = it
                    }.doOnError {
                        disconnectAllDevice()
                    }
                    .doOnNext {
                        isNeedRetry = false
                    }
                    .retry(2) {
                        isNeedRetry
                    }

            mHmConnectObservable[bleDevice] = connectionObservable
            retObservable = connectionObservable
        }
        return retObservable
    }


    fun initRxjavaErrorHandle() {
        RxJavaPlugins.setErrorHandler { throwable ->
            if (throwable is UndeliverableException && throwable.cause is BleException) {
                return@setErrorHandler // ignore BleExceptions as they were surely delivered at least once
            }
            // add other custom handlers if needed
            throw RuntimeException("Unexpected Throwable in RxJavaPlugins error handler", throwable)
        }
    }

    fun writeData(device: RxBleDevice, data: ByteArray): Observable<ByteArray> {
        val connectObservable: Observable<RxBleConnection> = if (mHmConnectObservable.contains(device)) {
            mHmConnectObservable[device]!!
        } else {
            connect(device, true)
        }
        return connectObservable
                .flatMapSingle {
                    val writeCharacteristic = it.writeCharacteristic(UUID.fromString(charUUID), data)
                    writeCharacteristic
                }
    }

}
