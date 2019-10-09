package com.dadoutek.uled.communicate

import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.AppUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.MeshEvent
import com.telink.bluetooth.event.NotificationEvent
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.Strings
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.experimental.and
import kotlin.Unit as Unit1

object Commander : EventListener<String> {
    private var mTargetGroupAddr: Int = 0   //要把device分到的Group的地址
    private var mGotGroupAddr: Int = 0  //从Device获取到的Group地址
    private var mDstAddr: Int = 0
    private var mGroupSuccess: Boolean = false
    private var mResetSuccess: Boolean = false
    private var mGetVersionSuccess: Boolean = false
    private var mUpdateMeshSuccess: Boolean = false
    private var version: String? = null

    init {
        //监听事件
//        TelinkLightApplication.getApp()?.addEventListener(LeScanEvent.LE_SCAN, this)
//        TelinkLightApplication.getApp()?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
//        TelinkLightApplication.getApp()?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        TelinkLightApplication.getApp()?.addEventListener(MeshEvent.UPDATE_COMPLETED, this)
//        TelinkLightApplication.getApp()?.addEventListener(MeshEvent.ERROR, this)
    }

    fun openOrCloseLights(groupAddr: Int, isOpen: Boolean) {
        val opcode = Opcode.LIGHT_ON_OFF
        mTargetGroupAddr = groupAddr
        val params: ByteArray

        if (isOpen) {
            //0x64代表延时100ms，从而保证多个灯同步开关
            params = byteArrayOf(0x01, 0x64, 0x00)
        } else {
            params = byteArrayOf(0x00, 0x64, 0x00)
        }

        GlobalScope.launch {
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, mTargetGroupAddr, params)
        }
    }

    /**
     * groupAddr 目标地址
     * isOpen  是否开关窗帘
     * isPause 是否暂停开关窗帘
     */
    fun openOrCloseCurtain(groupAddr: Int, isOpen: Boolean, isPause: Boolean) {
        val opcode = Opcode.CURTAIN_ON_OFF
        mTargetGroupAddr = groupAddr
        val params: ByteArray

        if (isPause) {
            params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0B, 0x00, Opcode.CURTAIN_PACK_END)
        } else {
            if (isOpen) {
                //0x64代表延时100ms保证开关同步
                params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0A, 0x00, Opcode.CURTAIN_PACK_END)
            } else {
                params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0C, 0x00, Opcode.CURTAIN_PACK_END)
            }
        }

        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, mTargetGroupAddr, params)
    }


    fun resetLightsOld(lightList: List<Int>, successCallback: () -> Unit1,
                       failedCallback: () -> Unit1) {

        LogUtils.e("zcl---添加表 更新数据$lightList")
        val sleepTime: Long = 200
        val resendCmdTime: Int = 3
        var connectDeviceIndex: Int = 0
        val lastIndex = lightList.size - 1
        val connectDeviceMeshAddr = TelinkLightApplication.getApp().connectDevice?.meshAddress ?: 0x00
        if (lightList.isNotEmpty()) {
            Thread {
                //找到当前连接的灯的mesh地址
                for (k in lightList.indices) {
                    if (lightList[k] == connectDeviceMeshAddr) {
                        connectDeviceIndex = k
                        break
                    }
                }
                //把当前连接的灯放到list的最后一个
                Collections.swap(lightList, lastIndex, connectDeviceIndex)
                for (light in lightList) {
                    for (k in 0..resendCmdTime) {
                        val opcode = Opcode.KICK_OUT
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, light, null)
                        Thread.sleep(sleepTime)
                    }
                    DBUtils.deleteAll()//删除人体感应器和开关
                    Thread.sleep(sleepTime)
                    for (k in lightList.indices) {
                        if (DBUtils.getLightByMeshAddr(lightList[k]) != null) {
                            var ligh = DBUtils.getLightByMeshAddr(lightList[k])
                            if (ligh != null) {
                                DBUtils.deleteLight(ligh)
                            }
                        } else if (DBUtils.getCurtainByMeshAddr(lightList[k]) != null) {
                            var curtain = DBUtils.getCurtainByMeshAddr(lightList[k])
                            if (curtain != null) {
                                DBUtils.deleteCurtain(curtain)
                            }
                        } else if (DBUtils.getRelyByMeshAddr(lightList[k]) != null) {
                            var rely = DBUtils.getRelyByMeshAddr(lightList[k])
                            if (rely != null) {
                                DBUtils.deleteConnector(rely)
                            }
                        }
                    }

                }
                GlobalScope.launch(Dispatchers.Main) {
                    successCallback.invoke()
                }
            }.start()
        } else {
            //就算数据里没设备，设备也有可能存在，这是就发指令让它恢复出厂设置
            GlobalScope.launch {
                TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.KICK_OUT, 0xFFFF, null) //直连灯不响应此条命令0xFFFF的目标地址
                delay(500)
                TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.KICK_OUT, 0x0000, null) //延时后再给直连灯发恢复出厂设置指令
            }
            failedCallback.invoke()
        }

    }

    @Synchronized
    fun resetLights(lightList: List<Int>, successCallback: () -> Unit1,
                    failedCallback: () -> Unit1) {
        val connectDeviceMeshAddr = TelinkLightApplication.getApp().connectDevice?.meshAddress ?: 0x00


        var isSupportFastResetFactory: Boolean = false
        getDeviceVersion(connectDeviceMeshAddr,
                {
                    isSupportFastResetFactory = AppUtils.isSupportFastResetFactory(it)
                    if (isSupportFastResetFactory) {
                        GlobalScope.launch {
                            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.KICK_OUT, 0xFFFF, null) //直连灯不响应此条命令0xFFFF的目标地址
                            delay(500)
                            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.KICK_OUT, 0x0000, null) //延时后再给直连灯发恢复出厂设置指令
                            DBUtils.deleteAllNormalLight()
                            DBUtils.deleteAllRGBLight()
                            DBUtils.deleteAllConnector()
                            DBUtils.deleteAllCurtain()
                        successCallback.invoke()
                        }
                    } else {
                        resetLightsOld(lightList, successCallback, failedCallback)
                    }
                },
                {
                    resetLightsOld(lightList, successCallback, failedCallback)
                })


    }

    @Synchronized
    fun resetCurtain(lightList: List<DbCurtain>, successCallback: () -> Unit1,
                     failedCallback: () -> Unit1) {
        val sleepTime: Long = 200
        val resendCmdTime: Int = 3
        var connectDeviceIndex: Int = 0
        val lastIndex = lightList.size - 1
        val connectDeviceMeshAddr = TelinkLightApplication.getApp()?.connectDevice?.meshAddress
                ?: 0x00

        if (lightList.isNotEmpty()) {
            Thread {
                //找到当前连接的灯的mesh地址
                for (k in lightList.indices) {
                    if (lightList[k].meshAddr == connectDeviceMeshAddr) {
                        connectDeviceIndex = k
                        break
                    }
                }
                //把当前连接的灯放到list的最后一个
                Collections.swap(lightList, lastIndex, connectDeviceIndex)
                for (light in lightList) {
                    for (k in 0..resendCmdTime) {
                        val opcode = Opcode.KICK_OUT
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, light.meshAddr, null)
                        Thread.sleep(sleepTime)
                    }
                    Thread.sleep(sleepTime)
                    DBUtils.deleteCurtain(light)
                }
                GlobalScope.launch(Dispatchers.Main) {
                    successCallback.invoke()
                }
            }.start()
        } else {
            failedCallback.invoke()
        }
    }

    fun addScene(sceneId: Long, meshAddr: Int, color: Int) {
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val list = DBUtils.getActionsBySceneId(sceneId)
        var params: ByteArray
        for (i in list.indices) {
            Thread.sleep(100)
            var temperature = list[i].colorTemperature.toByte()
            if (temperature > 99)
                temperature = 99
            var light = list[i].brightness.toByte()
            if (light > 99)
                light = 99

            var red = color and 0xff0000 shr 16
            var green = color and 0x00ff00 shr 8
            var blue = color and 0x0000ff
            var w = color shr 24

            params = byteArrayOf(0x01, sceneId.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte())
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, meshAddr, params)
        }
    }

    fun deleteGroup(lightMeshAddr: Int, successCallback: () -> Unit1, failedCallback: () -> Unit1) {
        TelinkLightApplication.getApp()?.addEventListener(NotificationEvent.GET_GROUP, this)

        mDstAddr = lightMeshAddr
        mTargetGroupAddr = 0xFFFF
        mGroupSuccess = false
        val opcode = Opcode.SET_GROUP          //0xD7 代表设置 组的指令
//        val params = byteArrayOf(0x01, (groupMeshAddr and 0xFF).toByte(), //0x00 代表删除组
//                (groupMeshAddr shr 8 and 0xFF).toByte())
        val params = byteArrayOf(0x00, 0xFF.toByte(), 0xFF.toByte()) //0x00 代表删除组

        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, lightMeshAddr, params)
        Observable.interval(0, 200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Long?> {
                    var mDisposable: Disposable? = null
                    override fun onComplete() {
                        mDisposable?.dispose()
                        TelinkLightApplication.getApp()?.removeEventListener(Commander)
                    }

                    override fun onSubscribe(d: Disposable) {
                        mDisposable = d
                    }

                    override fun onNext(t: Long) {
                        val timeOut = 30
                        //("mGroupSuccess = $mGroupSuccess")
                        if (t >= timeOut) {   //10次 * 200 = 2000, 也就是超过了2s就超时
                            onComplete()
                            failedCallback.invoke()
                        } else if (mGroupSuccess) {
                            onComplete()
                            successCallback.invoke()
                        }
                    }

                    override fun onError(e: Throwable) {
                    }
                })
    }


    /**
     * 修改设备分组
     */
    fun addGroup(dstAddr: Int, groupAddr: Int, successCallback: () -> Unit1, failedCallback: () -> Unit1) {
        TelinkLightApplication.getApp()?.addEventListener(NotificationEvent.GET_GROUP, this)
        mDstAddr = dstAddr
        mTargetGroupAddr = groupAddr
        mGroupSuccess = false
        val opcode = Opcode.SET_GROUP          //0xD7 代表添加组的指令
        val params = byteArrayOf(0x01, (groupAddr and 0xFF).toByte(), //0x01 代表添加组
                (groupAddr shr 8 and 0xFF).toByte())

        GlobalScope.launch {
            delay(200)
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, dstAddr, params)
        }

        Observable.interval(0, 300, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Long?> {
                    var mDisposable: Disposable? = null
                    override fun onComplete() {
                        mDisposable?.dispose()
                        TelinkLightApplication.getApp()?.removeEventListener(Commander)
                    }

                    override fun onSubscribe(d: Disposable) {
                        mDisposable = d
                    }

                    override fun onNext(t: Long) {
                        if (t >= 10) {   //10次 * 200 = 2000, 也就是超过了2s就超时
                            onComplete()
                            failedCallback.invoke()
                        } else if (mGroupSuccess) {
                            onComplete()
                            successCallback.invoke()
                        }
                    }

                    override fun onError(e: Throwable) {
                        onComplete()
                        failedCallback.invoke()
                        LogUtils.e("addGroup error: ${e.message}")
                    }
                })
    }


    /**
     * 获取指定设备所属的组地址
     * @param deviceAddr    目标设备地址。
     * @return  返回组地址，例如 0x8001
     */
    fun getGroup(deviceAddr: Int): Observable<Int> {
        TelinkLightApplication.getApp()?.addEventListener(NotificationEvent.GET_GROUP, this)
        mGotGroupAddr = 0
        mDstAddr = deviceAddr
        val params = byteArrayOf(0x08, 0x01)
        GlobalScope.launch {
            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.GET_GROUP, deviceAddr, params)
        }
        var isFinish: Boolean = false
        return Observable.create<Int> { emitter ->
            Observable.interval(200, 200, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .takeWhile {
                        val ret = it < 10 * 200
                        if (!ret) {
                            isFinish = true
                            emitter.onError(Throwable("timeout"))
                        }
                        !isFinish
                    }
                    .subscribe {
                        if (mGotGroupAddr != 0) {
                            emitter.onNext(mGotGroupAddr)
                            isFinish = true
                            emitter.onComplete()
                        }
                    }
        }
    }

    fun updateMeshName(newMeshName: String = DBUtils.lastUser!!.account, newMeshAddr: Int =
            Constant.SWITCH_PIR_ADDRESS, successCallback: () -> Unit1, failedCallback: () -> Unit1) {
        mUpdateMeshSuccess = false
        TelinkLightApplication.getApp()?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        val password = Strings.stringToBytes(NetworkFactory.md5(NetworkFactory.md5(newMeshName) + newMeshName), 16)
        TelinkLightService.Instance()?.adapter!!.mode = LightAdapter.MODE_UPDATE_MESH
        TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight.newMeshAddress = newMeshAddr
        TelinkLightService.Instance()?.adapter!!.mLightCtrl.reset(Strings.stringToBytes(newMeshName, 16), password, null)

        Observable.interval(0, 200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Long?> {
                    var mDisposable: Disposable? = null
                    override fun onComplete() {
                        mDisposable?.dispose()
                        TelinkLightApplication.getApp()?.removeEventListener(Commander)
                    }

                    override fun onSubscribe(d: Disposable) {
                        mDisposable = d
                    }

                    override fun onNext(t: Long) {
                        if (t >= 30) {   //10次 * 200 = 2000, 也就是超过了2s就超时
                            onComplete()
                            failedCallback.invoke()
                        } else if (mUpdateMeshSuccess) {
                            onComplete()
                            successCallback.invoke()
                        }
                    }

                    override fun onError(e: Throwable) {
                        LogUtils.e("zcl", "zcl**********updateMeshName*******onError***${e.message}")
                    }
                })
    }


    override fun performed(event: Event<String>?) {
        when (event?.type) {
            NotificationEvent.GET_GROUP -> this.onGetGroupEvent(event as NotificationEvent)
            NotificationEvent.GET_DEVICE_STATE -> this.onGetLightVersion(event as NotificationEvent)
            NotificationEvent.USER_ALL_NOTIFY -> this.onKickoutEvent(event as NotificationEvent)
            MeshEvent.ERROR -> this.onMeshEvent(event as MeshEvent)
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
        }

    }

    private fun onDeviceStatusChanged(deviceEvent: DeviceEvent) {
        val deviceInfo = deviceEvent.args
        when (deviceInfo.status) {
            LightAdapter.STATUS_UPDATE_MESH_COMPLETED -> {
                mUpdateMeshSuccess = true
//                ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
            }
            LightAdapter.STATUS_UPDATE_MESH_FAILURE -> {
            }
        }
    }


    private fun onMeshEvent(event: MeshEvent) {

    }

    private fun onGetGroupEvent(event: NotificationEvent) {
        val info = event.args

        val srcAddress = info.src
        val params = info.params

        if (srcAddress != mDstAddr) {
            //这说明这条回复不是刚刚发出去的指令的回复
            return
        }

        var groupAddress: Int
        val len = params.size

        for (j in 0 until len) {
            mGotGroupAddr = (params[j].toInt() and 0xFFFF)
            if (mTargetGroupAddr != 0xFFFF && mGotGroupAddr != 0xFFFF) {
                mGotGroupAddr = mGotGroupAddr or 0x8000
            }

            if (mTargetGroupAddr == mGotGroupAddr) {
                LogUtils.d("mGroupSuccess = true")
                mGroupSuccess = true
            }
        }
    }

    //关闭渐变
    fun closeGradient(dstAddr: Int, id: Int, speed: Int) {
        var opcode = Opcode.APPLY_RGB_GRADIENT
        //关闭渐变
        val gradientActionType = 0x03
        val params: ByteArray
        params = byteArrayOf(gradientActionType.toByte(), id.toByte(), speed.toByte())
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, dstAddr, params)
    }

    //加载渐变
    fun applyGradient(dstAddr: Int, id: Int, speed: Int, firstAddress: Int) {
        var opcode = Opcode.APPLY_RGB_GRADIENT
        //开始内置渐变
        val gradientActionType = 0x02
        val params: ByteArray
        params = byteArrayOf(gradientActionType.toByte(), id.toByte(), speed.toByte(), firstAddress.toByte())
        GlobalScope.launch {
            for (i in 0..2) {
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, dstAddr, params)
                //Thread.sleep(50)//当阻塞方法收到中断请求的时候就会抛出InterruptedException异常
                delay(50)
            }
        }
    }

    //加载自定义渐变
    fun applyDiyGradient(dstAddr: Int, id: Int, speed: Int, firstAddress: Int) {
        var opcode = Opcode.APPLY_RGB_GRADIENT
        //开始自定义渐变
        val gradientActionType = 0x04
        val params: ByteArray
        params = byteArrayOf(gradientActionType.toByte(), id.toByte(), speed.toByte(), firstAddress.toByte())
        for (i in 0..2) {
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, dstAddr, params)
            Thread.sleep(50)
        }
    }

    //删除渐变
    fun deleteGradient(dstAddr: Int, id: Int, successCallback: (version: String?) -> Unit1,
                       failedCallback: () -> Unit1) {
        var opcode = Opcode.APPLY_RGB_GRADIENT
        //删除渐变
        val gradientActionType = 0x01
        val params: ByteArray
        //删除方式为删除索引
        val deleteType = 0x01
        params = byteArrayOf(gradientActionType.toByte(), id.toByte(), deleteType.toByte())
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, dstAddr, params)
    }

    /**
     * 添加渐变
     * id  渐变索引号
     * node 渐变节点号。（填0: 从此颜色开始渐变，填1: 经过此颜色，填2: 以此颜色结束)
     * mode 渐变模式 (填0: RGB渐变， 填1: 双色温渐变)
     * (C没值就都先传FF)(W没值就都先传FF)
     * 双色温渐变模式：[16]: 色温
     */
    fun addGradient(dstAddr: Int, id: Int, node: Int, mode: Int, brightness: Int,
                    r: Int, g: Int, b: Int, c: Int = 0xff, w: Int = 0xff, successCallback: (version: String?) -> Unit1,
                    failedCallback: () -> Unit1) {
        var opcode = Opcode.APPLY_RGB_GRADIENT
        val gradientActionType = 0x00
        val params: ByteArray
        params = byteArrayOf(gradientActionType.toByte(), id.toByte(), node.toByte(),
                mode.toByte(), brightness.toByte(), r.toByte(), g.toByte(), b.toByte(), c.toByte(), w.toByte())
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, dstAddr, params)
    }

    fun getDeviceVersion(dstAddr: Int, successCallback: (version: String?) -> Unit1,
                         failedCallback: () -> Unit1) {
        TelinkLightApplication.getApp()?.addEventListener(NotificationEvent.GET_DEVICE_STATE, this)

        mDstAddr = dstAddr
        mGetVersionSuccess = false
        var opcode = Opcode.GET_VERSION          //0xFC 代表获取灯版本的指令

        val params: ByteArray
        if (TelinkApplication.getInstance().connectDevice.meshAddress == dstAddr) {
            params = byteArrayOf(0x00, 0x00)
        } else {
            opcode = Opcode.SEND_MESSAGE_BY_MESH
            val meshAddr = TelinkApplication.getInstance().connectDevice.meshAddress
            params = byteArrayOf(0x3c, (meshAddr and 0xFF).toByte(), ((meshAddr shr 8) and 0xFF).toByte())  //第二个byte是地址的低byte，第三个byte是地址的高byte
        }

        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, dstAddr, params)
        Observable.interval(0, 200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Long?> {
                    var mDisposable: Disposable? = null
                    override fun onComplete() {
                        mDisposable?.dispose()
                        TelinkLightApplication.getApp()?.removeEventListener(Commander)
                    }

                    override fun onSubscribe(d: Disposable) {
                        mDisposable = d
                    }

                    override fun onNext(t: Long) {
                        if (t >= 10) {   //30次 * 200 = 6000, 也就是超过了2s就超时
                            onComplete()
                            failedCallback.invoke()
                        } else if (mGetVersionSuccess) {
                            onComplete()
                            successCallback.invoke(version)
                        }
                    }

                    override fun onError(e: Throwable) {
                        onComplete()
                        failedCallback.invoke()
                        //(e.message)
                    }
                })
    }

    private fun onGetLightVersion(event: NotificationEvent) {
        val data = event.args.params
        if (data[0] == (Opcode.GET_VERSION and 0x3F)) {
            version = Strings.bytesToString(Arrays.copyOfRange(data, 1, data.size - 1))
//
//          val version = Strings.bytesToString(data)
            val meshAddress = event.args.src

//            val light = DBUtils.getLightByMeshAddr(meshAddress)
//            light.version = version

            if (version != "") {
                mGetVersionSuccess = true
            }
//            TelinkLog.i("OTAPrepareActivity#GET_DEVICE_STATE#src:$meshAddress get version success: $version")
        } else {
            version = Strings.bytesToString(data)
//            val version = Strings.bytesToString(data)
            val meshAddress = event.args.src

//            val light = DBUtils.getLightByMeshAddr(meshAddress)
//            light.version = version

            if (version != "") {
                mGetVersionSuccess = true
            }
//            TelinkLog.i("OTAPrepareActivity#GET_DEVICE_STATE#src:$meshAddress get version success: $version")
        }
    }

    private fun onKickoutEvent(notificationEvent: NotificationEvent) {
        val data = notificationEvent.args.params
        for (i in data.indices) {
        }
//        if(data[0].toInt()== mDstAddr){
        mResetSuccess = true
//        }
    }

    /**
     * 自动重连
     * 此处用作设备登录
     */
    fun autoConnect(macAddress: String, successCallback: (version: String?) -> Unit1,
                    failedCallback: () -> Unit1) {
        if (TelinkLightService.Instance() != null) {
            if (TelinkLightService.Instance()?.mode != LightAdapter.MODE_AUTO_CONNECT_MESH) {
                TelinkLightService.Instance()?.idleMode(true);

                //自动重连参数
                val connectParams = Parameters.createAutoConnectParameters()
                connectParams.setMeshName(DBUtils.lastUser?.controlMeshName)
                connectParams.setConnectMac(macAddress)
                connectParams.setPassword(NetworkFactory.md5(
                        NetworkFactory.md5(DBUtils.lastUser?.controlMeshName) + DBUtils.lastUser?.controlMeshName).substring(0, 16))
                connectParams.autoEnableNotification(true)

                //连接，如断开会自动重连
                Thread {
                    try {
                        Thread.sleep(300)
                        TelinkLightService.Instance()?.autoConnect(connectParams)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }.start()
            }

            //刷新Notify参数
            val refreshNotifyParams = Parameters.createRefreshNotifyParameters()
            refreshNotifyParams.setRefreshRepeatCount(1)
            refreshNotifyParams.setRefreshInterval(2000)
            //开启自动刷新Notify
            TelinkLightService.Instance()?.autoRefreshNotify(refreshNotifyParams)
        }
    }
}