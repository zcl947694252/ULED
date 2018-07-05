package com.dadoutek.uled.communicate

import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.TelinkLightApplication
import com.dadoutek.uled.TelinkLightService
import com.dadoutek.uled.model.Opcode
import com.telink.TelinkApplication
import com.telink.bluetooth.event.MeshEvent
import com.telink.bluetooth.event.NotificationEvent
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

object Commander : EventListener<String> {
    private var mApplication: TelinkApplication? = null
    private var mGroupingAddr: Int = 0
    private var mLightAddr: Int = 0
    private var mGroupSuccess: Boolean = false

    init {
        mApplication = TelinkLightApplication.getInstance()
        //监听事件
//        mApplication?.addEventListener(LeScanEvent.LE_SCAN, this)
//        mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
//        mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        mApplication?.addEventListener(MeshEvent.UPDATE_COMPLETED, this)
//        mApplication?.addEventListener(MeshEvent.ERROR, this)
    }

    fun deleteGroup(lightMeshAddr: Int, successCallback: () -> Unit, failedCallback: () -> Unit) {
        mApplication?.addEventListener(NotificationEvent.GET_GROUP, this)

        mLightAddr = lightMeshAddr
        mGroupingAddr = 0xFFFF
        mGroupSuccess = false
        val opcode = Opcode.SET_GROUP          //0xD7 代表设置 组的指令
//        val params = byteArrayOf(0x01, (groupMeshAddr and 0xFF).toByte(), //0x00 代表删除组
//                (groupMeshAddr shr 8 and 0xFF).toByte())
        val params = byteArrayOf(0x00, 0xFF.toByte(), //0x00 代表删除组
                0xFF.toByte())
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
                        LogUtils.d("mGroupSuccess = $mGroupSuccess")
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

    override fun performed(event: Event<String>?) {
        when (event?.type) {
            NotificationEvent.GET_GROUP -> this.onGetGroupEvent(event as NotificationEvent)
            MeshEvent.ERROR -> this.onMeshEvent(event as MeshEvent)
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
            if (mGroupingAddr != 0xFFFF) {
                groupAddress = groupAddress or 0x8000
            } else {
                groupAddress = mGroupingAddr
            }

            if (mGroupingAddr == groupAddress) {
                LogUtils.d(String.format("grouping success, groupAddr = %x groupingLight.meshAddr = %x", groupAddress, mLightAddr))
                mGroupSuccess = true
            }
        }
    }


}