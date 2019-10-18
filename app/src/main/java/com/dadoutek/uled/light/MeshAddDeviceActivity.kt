package com.dadoutek.uled.light

import android.bluetooth.le.ScanFilter
import android.os.Bundle
import android.view.View
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Constant.DEFAULT_MESH_FACTORY_NAME
import com.dadoutek.uled.model.Constant.DEFAULT_MESH_FACTORY_PASSWORD
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.AppUtils
import com.dadoutek.uled.util.MeshAddressGenerator
import com.dadoutek.uled.util.RecoverMeshDeviceUtil
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LeScanParameters
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_device_scanning.btn_stop_scan
import kotlinx.android.synthetic.main.activity_mesh_add_device.*
import kotlinx.android.synthetic.main.template_lottie_animation.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class MeshAddDeviceActivity : TelinkBaseActivity() {
    private val RECONNECT_TIMEOUT: Long = 20
    private val SCAN_TIMEOUT_SECOND = 10
    private val MAX_RSSI = 90

    enum class HandleMode {
        MESH_SCAN,
        FIX_CONFLICT
    }

    private var mHandleMode: HandleMode = HandleMode.MESH_SCAN
    private var mApplication: TelinkLightApplication? = null
    private val mCompositeDisposable = CompositeDisposable()
    private val mAddedDeviceList = mutableListOf<DeviceInfo>()
    private val mConflictDeviceList = mutableListOf<DeviceInfo>()
    private var mReconnectTimer: Disposable? = null

    private var mAddDeviceType: Int = 0
    private var bestRssiDevice: DeviceInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mApplication = this.application as TelinkLightApplication

        setContentView(R.layout.activity_mesh_add_device)

        initData()
        initListener()
        initToolbar()
        autoConnect()
//        startScan()

    }

    private fun initData() {
        mConflictDeviceList.clear()
        mAddedDeviceList.clear()
        mAddDeviceType = intent.getIntExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
    }


    override fun onDestroy() {
        super.onDestroy()
        removeListeners()
        mCompositeDisposable.clear()
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        doFinish()

    }

    private fun doFinish() {
        showLoadingDialog(getString(R.string.please_wait))
        TelinkLightService.Instance().idleMode(true)
        val disposable = Observable.timer(2000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideLoadingDialog()
                    finish()
                }
    }

    private fun initListener() {
        enableBaseConnectionStatusListener = false

        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, StatusChangedListener)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, StatusChangedListener)

        btn_stop_scan.setOnClickListener {
            doFinish()
        }
    }

    private fun removeListeners() {
        this.mApplication?.removeEventListener(StatusChangedListener)
    }

    private fun startAnimation() {
        lottieAnimationView.playAnimation()
        lottieAnimationView?.visibility = View.VISIBLE
    }

    private fun closeAnimation() {
        lottieAnimationView?.cancelAnimation()
        lottieAnimationView?.visibility = View.GONE
    }


    private fun initToolbar() {
        toolbar?.setTitle(R.string.scanning)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            doFinish()
        }
    }

    private fun addConflictList(deviceInfo: DeviceInfo) {
        var isNeedAdd = true
        for (conflictDevice in mConflictDeviceList) {
            if (conflictDevice.macAddress == deviceInfo.macAddress) {
                isNeedAdd = false
            }
        }
        if (isNeedAdd) {
            mConflictDeviceList.add(deviceInfo)
        }
        LogUtils.d("conflict device mac = ${mConflictDeviceList.map { it.macAddress }}")
    }


    private fun startMeshScan(deviceInfo: DeviceInfo) {
        //先改除了直连灯以外的设备的地址，直连灯不会响应此条命令，只转发。

        val disposable = provisionThroughMesh()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
//                .flatMap { provisionThroughMesh(dstAddr = 0x0000) }//再改直连灯的地址。
                .flatMap {
                    updateMeshAddress(deviceInfo)
                    provisionThroughMesh(dstAddr = 0x0000)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .map {
                    //                    TelinkLightService.Instance()?.idleMode(true)

                    val meshName = DBUtils.lastUser!!.controlMeshName
                    val pwd = NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16)

//                    LogUtils.d("autoConnect(meshName, pwd, HandleMode.FIX_CONFLICT)")
//                    autoConnect(meshName, pwd, HandleMode.FIX_CONFLICT)
                    it
                }
                .observeOn(Schedulers.io())
                .flatMap {
                    RecoverMeshDeviceUtil.addDeviceByMesh(it)
                }
                .map {
                    if (it.hasConflict) {
//                                LogUtils.d("conflict device mac = ${it.deviceInfo.macAddress}")
                        addConflictList(it.deviceInfo)

                    } else {
                        mAddedDeviceList.add(it.deviceInfo)
                    }
                    it
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            tv_added_info.text = getString(R.string.title_scanned_device_num, mAddedDeviceList.size)
                        },
                        {
                            LogUtils.d(it)
                        },
                        {
                            LogUtils.d("start to fixConflict complete add device")
//                            fixConflict()
                            reConnect()
                            LogUtils.d("complete add device by mesh")
                        }
                )

        mCompositeDisposable.add(disposable)
    }

    private fun startReconnectTimer() {
        mReconnectTimer = Observable.timer(RECONNECT_TIMEOUT, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    ToastUtils.showLong(R.string.scan_success)
                    doFinish()
                }

    }

    private fun stopReconnectTimer() {
        mReconnectTimer?.dispose()
    }

    private fun reConnect() {
        LogUtils.d("reConnect")
        startReconnectTimer()
        autoConnect()
    }

    /**
     * 自动重连
     * 此处用作设备登录
     */
    private fun autoConnect(targetMeshName: String = DEFAULT_MESH_FACTORY_NAME, targetMeshPwd: String = DEFAULT_MESH_FACTORY_PASSWORD
                            , mode: HandleMode = HandleMode.MESH_SCAN, mac: String = "") {
        mHandleMode = mode
        startAnimation()
        if (TelinkLightService.Instance() != null) {
            GlobalScope.launch(Dispatchers.IO)
            {
                TelinkLightService.Instance().idleMode(true)
                delay(500)
                //自动连接参数
                val connectParams = Parameters.createAutoConnectParameters()
                connectParams.setMeshName(targetMeshName)
                connectParams.setPassword(targetMeshPwd)
                connectParams.autoEnableNotification(true)
                if (mac.isNotEmpty())
                    connectParams.setConnectMac(mac)
                //连接，如断开会自动重连
                LogUtils.d("connect name = $targetMeshName   pwd = $targetMeshPwd   mac = $mac")
                TelinkLightService.Instance().autoConnect(connectParams)
            }

        }
    }

    /**
     * 泰凌微蓝牙库的状态回调
     * 事件处理方法
     *
     * @param event
     */
    private val StatusChangedListener = EventListener<String?> { event ->
        when (event.type) {

            LeScanEvent.LE_SCAN -> onLeScan(event as LeScanEvent)

            DeviceEvent.STATUS_CHANGED -> {
                onDeviceStatusChanged(event as DeviceEvent)
            }
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReportNormal(info)
            }
        }
    }

    private fun onLeScan(event: LeScanEvent) {
        val mesh = this.mApplication!!.mesh
        val deviceInfo = event.args
        if (deviceInfo.productUUID == mAddDeviceType && deviceInfo.rssi < MAX_RSSI) {
            LeBluetooth.getInstance().stopScan()
//            updateMesh(deviceInfo)
            autoConnect(mac = deviceInfo.macAddress)
        }
    }

    private fun getHigh8Byte(bytes: ByteArray): ByteArray {
        if (bytes.size >= 8) {
            return bytes.sliceArray(IntRange(0, 7))
        } else {
            return bytes
        }
    }

    private fun getLow8Byte(bytes: ByteArray): ByteArray {
        if (bytes.size > 8) {
            return bytes.sliceArray(IntRange(8, bytes.size - 1))
        } else {
            return byteArrayOf()
        }
    }


    private fun provisionThroughMesh(dstAddr: Int = 0xFFFF): Observable<String> {
        val opcode = Opcode.MESH_PROVISION
        val address = dstAddr
        val MESH_NAME1: Byte = 0
        val MESH_NAME2: Byte = 1
        val MESH_PWD1: Byte = 2
        val MESH_PWD2: Byte = 3

        return Observable.create<String> { emitter ->
            val user = DBUtils.lastUser
            if (user != null) {
                val pwd = NetworkFactory.md5(NetworkFactory.md5(user.controlMeshName) + user.controlMeshName).substring(0, 16)

                LogUtils.d("mesh name = ${user.controlMeshName}   pwd = ${pwd}")

                val userBytes = user.controlMeshName.toByteArray()
                val hUserBytes = getHigh8Byte(userBytes)
                val lUserBytes = getLow8Byte(userBytes)

                val pwdBytes = pwd.toByteArray()
                val hPwdBytes = getHigh8Byte(pwdBytes)
                val lPwdBytes = getLow8Byte(pwdBytes)


                var executeResult = true
                var params = byteArrayOf(MESH_NAME1) + hUserBytes
                executeResult = executeResult and TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                        params)
                Thread.sleep(100)

                params = byteArrayOf(MESH_NAME2) + lUserBytes
                executeResult = executeResult and TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                        params)
                Thread.sleep(100)

                params = byteArrayOf(MESH_PWD1) + hPwdBytes
                executeResult = executeResult and TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                        params)
                Thread.sleep(100)

                params = byteArrayOf(MESH_PWD2) + lPwdBytes
                executeResult = executeResult and TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                        params)
                Thread.sleep(100)

                LogUtils.d("sent change mesh name cmd")
                emitter.onNext(user.controlMeshName)
                emitter.onComplete()

            } else {
                emitter.onError(Throwable("There are no mesh name or pwd"))
            }

        }


    }

    private fun fixConflict() {
        if (TelinkLightService.Instance()?.isLogin == true) {
            val opcode = Opcode.FIX_MESHADDR_CONFLICT
            val address = 0xFFFF
            for (device in mConflictDeviceList) {
                if (device.macAddress.isNotEmpty()) {
                    LogUtils.d("fix conflict mac = ${device.macAddress}")
                    val macBytes = ConvertUtils.hexString2Bytes(device.macAddress.replace(":", ""))

                    val newMeshAddr = MeshAddressGenerator().meshAddress
                    val newMeshAddrBytes = byteArrayOf((newMeshAddr and 0xFF).toByte(), ((newMeshAddr shr 8) and 0xFF00).toByte())
                    var params = macBytes + newMeshAddrBytes

                    Thread().run {
                        TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                                params)
                    }
                    mConflictDeviceList.remove(device)
//                    Thread.sleep(100)
                } else {
                    LogUtils.d("device.macAddress.isNotEmpty()")
                }
            }
        } else {
            LogUtils.d("not connect yet, couldn't fix conflict")
        }

    }


    private fun getFilters(): ArrayList<ScanFilter> {
        val scanFilters = ArrayList<ScanFilter>()
        val manuData: ByteArray?
        manuData = byteArrayOf(0, 0, 0, 0, 0, 0, mAddDeviceType.toByte())//转换16进制

        val manuDataMask = byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())

        val scanFilter = ScanFilter.Builder()
                .setManufacturerData(Constant.VENDOR_ID, manuData, manuDataMask)
                .build()
        scanFilters.add(scanFilter)
        return scanFilters
    }

    private fun startScan() {
        //添加进disposable，防止内存溢出.
        TelinkLightService.Instance()?.idleMode(true)
        startAnimation()
        LeBluetooth.getInstance().stopScan()

        //断连后延时一段时间再开始扫描
        val disposable = Observable.timer(1000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    val mesh = mApplication!!.mesh
                    //扫描参数
                    val params = LeScanParameters.create()
                    if (!AppUtils.isExynosSoc) {
                        params.setScanFilters(getFilters())
                    }
                    params.setMeshName(mesh.factoryName)
                    params.setOutOfMeshName(Constant.OUT_OF_MESH_NAME)
                    params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                    params.setScanMode(true)

                    TelinkLightService.Instance()?.startScan(params)
                }


    }

    private fun getNewMeshAddr(oldMeshAddr: Int): Int {
        var newMeshAddr = oldMeshAddr + 1
        while (DBUtils.isDeviceExist(newMeshAddr)) {
            newMeshAddr++
        }
        return newMeshAddr
    }

    private fun updateMesh(deviceInfo: DeviceInfo) {
        //更新参数
        deviceInfo.meshAddress = getNewMeshAddr(deviceInfo.meshAddress)
        val params = Parameters.createUpdateParameters()
        val user = DBUtils.lastUser
        params.setOldMeshName(DEFAULT_MESH_FACTORY_NAME)
        params.setOldPassword(DEFAULT_MESH_FACTORY_PASSWORD)
        params.setNewMeshName(user?.controlMeshName)
        params.setNewPassword(NetworkFactory.md5(NetworkFactory.md5(user?.controlMeshName) + user?.controlMeshName).substring(0, 16))
        params.setUpdateDeviceList(deviceInfo)
        TelinkLightService.Instance().updateMesh(params)

        LogUtils.d("updateMesh: " + deviceInfo.meshAddress + "" +
                "--" + deviceInfo.macAddress + "--productUUID:" + deviceInfo.productUUID)
    }


    private fun updateMeshAddress(deviceInfo: DeviceInfo) {
        val oldMeshAddr = deviceInfo.meshAddress
        var newMeshAddr = oldMeshAddr + 1
        while (DBUtils.isDeviceExist(newMeshAddr)) {
            newMeshAddr++
        }
//        deviceInfo.meshAddress = newMeshAddr
//        val params = Parameters.createUpdateParameters()
//        val user = DBUtils.lastUser
//        params.setOldMeshName(DEFAULT_MESH_FACTORY_NAME)
//        params.setOldPassword(DEFAULT_MESH_FACTORY_PASSWORD)
//        params.setNewMeshName(user?.controlMeshName)
//        params.setNewPassword(NetworkFactory.md5(NetworkFactory.md5(user?.controlMeshName) + user?.controlMeshName).substring(0, 16))
//        params.setUpdateDeviceList(deviceInfo)
//
//        LogUtils.d("update to new mesh address ${deviceInfo.meshAddress}")
//        TelinkLightService.Instance().updateMesh(params)


        val opcode = 0xE0.toByte()
        val params = byteArrayOf((newMeshAddr and 0xFF).toByte(), (newMeshAddr shr 8 and 0xFF).toByte())

//        TelinkLog.d("prepare update mesh address -->" + light.getMacAddress() + " src : " + Integer.toHexString(oldAddress) + " new : " + Integer.toHexString(newAddress))


        LogUtils.d("update to new mesh address ${ConvertUtils.bytes2HexString(params)}")
        TelinkLightService.Instance().sendCommandNoResponse(opcode, 0x0000, params)
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {
        val deviceInfo = event.args
        when (deviceInfo.status) {

            LightAdapter.STATUS_UPDATE_MESH_COMPLETED -> {
                LogUtils.d("update mesh success meshAddress = ${deviceInfo.meshAddress}")
                RecoverMeshDeviceUtil.addDevicesToDb(deviceInfo)
            }

            LightAdapter.STATUS_UPDATE_MESH_FAILURE -> {
                LogUtils.d("update mesh failed meshAddress = ${deviceInfo.meshAddress}")
//                updateMesh(deviceInfo)
            }


            LightAdapter.STATUS_LOGIN -> {
                stopReconnectTimer()
                when (mHandleMode) {
                    HandleMode.MESH_SCAN -> {
//                        if (DBUtils.isDeviceExist(deviceInfo.meshAddress)){
//                            updateMeshAddress(deviceInfo)
//
//                        }
                        startMeshScan(deviceInfo)
                    }
                    MeshAddDeviceActivity.HandleMode.FIX_CONFLICT ->
                        fixConflict()
                }
            }
        }
    }
}

