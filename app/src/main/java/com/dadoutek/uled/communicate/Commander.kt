package com.dadoutek.uled.communicate

import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.TelinkLightApplication
import com.dadoutek.uled.TelinkLightService
import com.dadoutek.uled.intf.NetworkFactory
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.MeshEvent
import com.telink.bluetooth.event.NotificationEvent
import com.telink.bluetooth.light.DeviceInfo
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
import java.util.concurrent.TimeUnit

object Commander : EventListener<String> {
    private var mApplication: TelinkLightApplication? = null
    private var mGroupingAddr: Int = 0
    private var mLightAddr: Int = 0
    private var mGroupSuccess: Boolean = false
    private var mUpdateMeshSuccess: Boolean = false

    init {
        mApplication = TelinkLightApplication.getApp()
        //监听事件
//        mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)
//        mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
//        mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        mApplication?.addEventListener(MeshEvent.UPDATE_COMPLETED, this)
//        mApplication?.addEventListener(MeshEvent.ERROR, this)
    }

    fun addGroup(lightMeshAddr: Int, groupMeshAddr: Int, successCallback: () -> Unit, failedCallback: () -> Unit) {
        mApplication?.addEventListener(NotificationEvent.GET_GROUP, this)

        mLightAddr = lightMeshAddr
        mGroupingAddr = groupMeshAddr
        mGroupSuccess = false
        val opcode = Opcode.SET_GROUP          //0xD7 代表添加组的指令
        val params = byteArrayOf(0x01, (groupMeshAddr and 0xFF).toByte(), //0x01 代表添加组
                (groupMeshAddr shr 8 and 0xFF).toByte())
        TelinkLightService.Instance().sendCommandNoResponse(opcode, lightMeshAddr, params)
        Observable.interval(0, 200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Long?> {
                    var mDisposable: Disposable? = null
                    override fun onComplete() {
                        mDisposable?.dispose()
                        mApplication?.removeEventListener(Commander)
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
                        LogUtils.d(e.message)
                    }
                })
    }


    fun updateMeshName(deviceInfo: DeviceInfo, successCallback: () -> Unit, failedCallback: () -> Unit) {
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        val mesh = mApplication!!.mesh
        val params = Parameters.createUpdateParameters()
        params.setOldMeshName(mesh.factoryName)
        params.setOldPassword(mesh.factoryPassword)
        params.setNewMeshName(mesh.name)
        val account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                Constant.DB_NAME_KEY, "dadou")
        if (SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                        Constant.USER_TYPE, Constant.USER_TYPE_OLD) == Constant.USER_TYPE_NEW) {
            params.setNewPassword(NetworkFactory.md5(
                    NetworkFactory.md5(mesh?.password) + account))
        } else {
            params.setNewPassword(mesh?.password)
        }

        params.setUpdateDeviceList(deviceInfo)

        TelinkLightService.Instance().adapter.mode = LightAdapter.MODE_UPDATE_MESH
        val meshName = Strings.stringToBytes(mesh.name, 16)
        var password = Strings.stringToBytes(mesh.password, 16)
        if (SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                        Constant.USER_TYPE, Constant.USER_TYPE_OLD) == Constant.USER_TYPE_NEW) {
            password = Strings.stringToBytes(NetworkFactory.md5(
                    NetworkFactory.md5(mesh?.password) + account), 16)
        } else {
            password = Strings.stringToBytes(mesh.password, 16)
        }

        TelinkLightService.Instance().adapter.mLightCtrl.reset(meshName, password, null)

        Observable.interval(0, 200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Long?> {
                    var mDisposable: Disposable? = null
                    override fun onComplete() {
                        mDisposable?.dispose()
                        mApplication?.removeEventListener(Commander)
                    }

                    override fun onSubscribe(d: Disposable) {
                        mDisposable = d
                    }

                    override fun onNext(t: Long) {
                        if (t >= 10) {   //10次 * 200 = 2000, 也就是超过了2s就超时
                            onComplete()
                            failedCallback.invoke()
                        } else if (mUpdateMeshSuccess) {
                            onComplete()
                            successCallback.invoke()
                        }
                    }

                    override fun onError(e: Throwable) {
                        LogUtils.d(e.message)
                    }
                })


    }


    override fun performed(event: Event<String>?) {
        when (event?.type) {
            NotificationEvent.GET_GROUP -> this.onGetGroupEvent(event as NotificationEvent)
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
//        ToastUtils.showShort(event.toString())
        LogUtils.d("Error ${event.toString()}")
    }

    private fun onGetGroupEvent(event: NotificationEvent) {
        val info = event.args

        val srcAddress = info.src and 0xFF
        val params = info.params


        if (srcAddress != mLightAddr) {
            return
        }

        var groupAddress: Int
        val len = params.size

        for (j in 0 until len) {

            groupAddress = params[j].toInt()

            if (groupAddress == 0x00 || groupAddress == 0xFF)
                break

            groupAddress = groupAddress or 0x8000

            if (mGroupingAddr == groupAddress) {
                LogUtils.d(String.format("grouping success, groupAddr = %x groupingLight.meshAddr = %x", groupAddress, mLightAddr))
                mGroupSuccess = true
            }
        }
    }


}