//package com.dadoutek.uled.service
//
//import android.app.AlertDialog
//import android.app.Service
//import android.content.DialogInterface
//import android.content.Intent
//import android.os.IBinder
//import com.blankj.utilcode.util.ToastUtils
//import com.dadoutek.uled.R
//import com.dadoutek.uled.TelinkLightApplication
//import com.dadoutek.uled.TelinkLightService
//import com.dadoutek.uled.activity.OTAUpdateActivity
//import com.dadoutek.uled.model.Constant
//import com.dadoutek.uled.model.Light
//import com.dadoutek.uled.model.Lights
//import com.dadoutek.uled.model.SharedPreferencesHelper
//import com.telink.bluetooth.TelinkLog
//import com.telink.bluetooth.event.*
//import com.telink.bluetooth.light.ConnectionStatus
//import com.telink.bluetooth.light.LightAdapter
//import com.telink.bluetooth.light.OnlineStatusNotificationParser
//import com.telink.util.Event
//import com.telink.util.EventListener
//
///**
// * Created by hejiajun on 2018/4/28.
// */
//class BleManagerService : Service(), EventListener<String> {
//    private var mApplication: TelinkLightApplication? = null
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        return super.onStartCommand(intent, flags, startId)
//        // 监听各种事件
//        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        this.mApplication?.addEventListener(NotificationEvent.ONLINE_STATUS, this)
//        this.mApplication?.addEventListener(NotificationEvent.GET_ALARM, this)
//        this.mApplication?.addEventListener(NotificationEvent.GET_DEVICE_STATE, this)
//        this.mApplication?.addEventListener(ServiceEvent.SERVICE_CONNECTED, this)
//        this.mApplication?.addEventListener(MeshEvent.OFFLINE, this)
//        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        initListner()
//    }
//
//    private fun initListner() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//
//    }
//
//    override fun onBind(intent: Intent?): IBinder {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun performed(event: Event<String>?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        when (event?.getType()) {
//            NotificationEvent.ONLINE_STATUS -> this.onOnlineStatusNotify(event as NotificationEvent)
//
//            NotificationEvent.GET_ALARM -> {
//            }
//            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
//            MeshEvent.OFFLINE -> this.onMeshOffline(event as MeshEvent)
//            ServiceEvent.SERVICE_CONNECTED -> this.onServiceConnected(event as ServiceEvent)
//            ServiceEvent.SERVICE_DISCONNECTED -> this.onServiceDisconnected(event as ServiceEvent)
//            NotificationEvent.GET_DEVICE_STATE -> onNotificationEvent(event as NotificationEvent)
//
//            ErrorReportEvent.ERROR_REPORT -> {
//                val info = (event as ErrorReportEvent).args
//                TelinkLog.d("MainActivity#performed#ERROR_REPORT: " + " stateCode-" + info.stateCode
//                        + " errorCode-" + info.errorCode
//                        + " deviceId-" + info.deviceId)
//            }
//        }
//    }
//
//    /**
//     * 处理[NotificationEvent.ONLINE_STATUS]事件
//     */
//    @Synchronized private fun onOnlineStatusNotify(event: NotificationEvent) {
//        if (!checkIsLight(event))
//            return
//        TelinkLog.i("MainActivity#onOnlineStatusNotify#Thread ID : " + Thread.currentThread().id)
//        val notificationInfoList: List<OnlineStatusNotificationParser.DeviceNotificationInfo>?
//
//        notificationInfoList = event.parse() as List<OnlineStatusNotificationParser.DeviceNotificationInfo>
//
//        if (notificationInfoList == null || notificationInfoList.size <= 0)
//            return
//
//
//        /*if (this.deviceFragment != null) {
//            this.deviceFragment.onNotify(notificationInfoList);
//        }*/
//
//        for (notificationInfo in notificationInfoList) {
//
//            val meshAddress = notificationInfo.meshAddress
//            val brightness = notificationInfo.brightness
//
//            var light: Light? = this.deviceFragment.getDevice(meshAddress)
//
//            if (light == null) {
//                light = Light()
//                this.deviceFragment.addDevice(light)
//            }
//
//            light.meshAddress = meshAddress
//            light.brightness = brightness
//            light.status = notificationInfo.connectionStatus
//
//            if (light.meshAddress == this.connectMeshAddress) {
//                light.textColor = this.resources.getColor(
//                        R.color.colorPrimary)
//            } else {
//                light.textColor = this.resources.getColor(
//                        R.color.black)
//            }
//
//            light.updateIcon()
//        }
//
//        mHandler.obtainMessage(UPDATE_LIST).sendToTarget()
//    }
//
//    private fun onDeviceStatusChanged(event: DeviceEvent) {
//
//        val deviceInfo = event.args
//
//        when (deviceInfo.status) {
//            LightAdapter.STATUS_LOGIN -> {
//                ToastUtils.showLong(getString(R.string.connect_success))
//                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, true)
//                val connectDevice = this.mApplication.getConnectDevice()
//                if (connectDevice != null) {
//                    this.connectMeshAddress = connectDevice.meshAddress
//                    //                this.showToast("login success");
//                    if (TelinkLightService.Instance().mode == LightAdapter.MODE_AUTO_CONNECT_MESH) {
//                        mHandler.postDelayed(Runnable { TelinkLightService.Instance().sendCommandNoResponse(0xE4.toByte(), 0xFFFF, byteArrayOf()) }, (3 * 1000).toLong())
//                    }
//
//                    if (TelinkLightApplication.getApp().mesh.isOtaProcessing && foreground) {
//                        startActivity(Intent(this, OTAUpdateActivity::class.java)
//                                .putExtra(OTAUpdateActivity.INTENT_KEY_CONTINUE_MESH_OTA, OTAUpdateActivity.CONTINUE_BY_PREVIOUS))
//                    }
//                }
//            }
//            LightAdapter.STATUS_CONNECTING -> {
//            }
//            LightAdapter.STATUS_LOGOUT ->
//                //                this.showToast("disconnect");
//                onLogout()
//
//            LightAdapter.STATUS_ERROR_N -> onNError(event)
//            else -> {
//            }
//        }//                this.showToast("login");
//    }
//
//    private fun onMeshOffline(event: MeshEvent) {
//        val lights = Lights.getInstance().get()
//        val it = lights.iterator()
//        while (it.hasNext()) {
//            val light = it.next()
//            light.status = ConnectionStatus.OFFLINE
//            light.updateIcon()
//            it.remove()
//        }
//        //        for (Light light : lights) {
//        ////            light.status = ConnectionStatus.OFFLINE;
//        ////            light.updateIcon();
//        //              lights.remove(light);
//        //        }
//        this.deviceFragment.notifyDataSetChanged()
//
//        if (TelinkLightApplication.getApp().mesh.isOtaProcessing) {
//            TelinkLightService.Instance().idleMode(true)
//            if (mTimeoutBuilder == null) {
//                mTimeoutBuilder = AlertDialog.Builder(this)
//                mTimeoutBuilder.setTitle("AutoConnect Fail")
//                mTimeoutBuilder.setMessage("Connect device:" + TelinkLightApplication.getApp().mesh.otaDevice.mac + " Fail, Quit? \nYES: quit MeshOTA process, NO: retry")
//                mTimeoutBuilder.setNeutralButton(R.string.quite, DialogInterface.OnClickListener { dialog, which ->
//                    val mesh = TelinkLightApplication.getApp().mesh
//                    mesh.otaDevice = null
//                    mesh.saveOrUpdate(this@MainActivity)
//                    autoConnect()
//                    dialog.dismiss()
//                })
//                mTimeoutBuilder.setNegativeButton(R.string.retry, DialogInterface.OnClickListener { dialog, which ->
//                    autoConnect()
//                    dialog.dismiss()
//                })
//                mTimeoutBuilder.setCancelable(false)
//            }
//            mTimeoutBuilder.show()
//        }
//    }
//
//    private fun onServiceConnected(event: ServiceEvent) {
//        this.autoConnect()
//    }
//
//    private fun onServiceDisconnected(event: ServiceEvent) {
//
//    }
//
//    private fun onNotificationEvent(event: NotificationEvent) {
//        if (!foreground) return
//        // 解析版本信息
//        val data = event.args.params
//        if (data[0] == NotificationEvent.DATA_GET_MESH_OTA_PROGRESS) {
//            TelinkLog.w("mesh ota progress: " + data[1])
//            val progress = data[1].toInt()
//            if (progress != 100) {
//                startActivity(Intent(this, OTAUpdateActivity::class.java)
//                        .putExtra(OTAUpdateActivity.INTENT_KEY_CONTINUE_MESH_OTA, OTAUpdateActivity.CONTINUE_BY_REPORT)
//                        .putExtra("progress", progress))
//            }
//        }
//    }
//
//
//
//}