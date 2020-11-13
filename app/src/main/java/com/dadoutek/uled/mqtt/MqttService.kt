package com.dadoutek.uled.mqtt

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Binder
import android.os.IBinder
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.EncryptUtils
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.model.Cmd
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.tellink.TelinkLightApplication
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MqttService : Service() {
    val TAG = MqttService::class.java.simpleName
    private var client: MqttAndroidClient? = null

    private var conOpt: MqttConnectOptions? = null

    private val imei = com.dadoutek.uled.util.DeviceUtils.getIMEI(TelinkLightApplication.getApp().mContext)
    private var clientId: String = NetworkFactory.md5((DBUtils.lastUser?.id ?: 0).toString() + imei).substring(8, 24) //客户端标识MD5加密一定是32位
    private var topics = "app/emit/${DBUtils.lastUser?.id}"
    private val host = "${Constant.HOST2}:${Constant.PORT}"
    private val passWord = EncryptUtils.encryptMD5("123456".toByteArray())
    private val userName = "APP_${DBUtils.lastUser?.id ?: 0}"
    private var IGetMessageCallBack: IGetMessageCallBack? = null


    // MQTT是否连接成功
    private val iMqttActionListener = object : IMqttActionListener {
        override fun onSuccess(arg0: IMqttToken) {
            LogUtils.i(TAG, "连接成功 userName = $clientId")
            try {// 订阅myTopic话题
                client?.subscribe(topics, 1)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }

        override fun onFailure(arg0: IMqttToken?, arg1: Throwable) {
            LogUtils.d("connect mqtt server failed: $arg1")
            // 连接失败，重连
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //0  var flag = flags
        // flag = START_STICKY
        return super.onStartCommand(intent, flags, startId)
    }

    // MQTT监听并且接受消息
    private val mqttCallback = object : MqttCallback {
        override fun messageArrived(topic: String, message: MqttMessage) {
            val data = message.payload
             LogUtils.v("zcl_mqtt--****mqtt连接回调------------onPublish---${topic}---${message ?.toString()}");
            val intent = Intent()
            intent.action = Constant.LOGIN_OUT
            intent.putExtra(Constant.LOGIN_OUT, message.toString())
            TelinkLightApplication.getApp().mContext.sendBroadcast(intent)
        }

        override fun deliveryComplete(arg0: IMqttDeliveryToken) {}


        override fun connectionLost(arg0: Throwable) {
            // 失去连接，重连
        }
    }

    /** 判断网络是否连接  */
    private val isConnectIsNormal: Boolean
        get() {
            val connectivityManager = this.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val info = connectivityManager.activeNetworkInfo
            return if (info != null && info.isAvailable) {
                val name = info.typeName
                LogUtils.i(TAG, "MQTT当前网络名称：$name")
                true
            } else {
                LogUtils.i(TAG, "MQTT 没有可用网络")
                false
            }
        }


    override fun onCreate() {
        super.onCreate()
        init()
    }

    open fun init() {
        // 服务器地址（协议+地址+端口号）
        val uri = host
        client = MqttAndroidClient(this, uri, clientId)
        // 设置MQTT监听并且接受消息
        client?.setCallback(mqttCallback)
        // client?.setManualAcks(false)//为true表示收到ack需要自己手动通知messageArrived

        conOpt = MqttConnectOptions()
        // 清除缓存
        conOpt!!.isCleanSession = true
        // 设置超时时间，单位：秒
        conOpt!!.connectionTimeout = 10
        // 心跳包发送间隔，单位：秒
        conOpt!!.keepAliveInterval = 20
        // 用户名
        conOpt!!.userName = userName
        // 密码
        conOpt!!.password = ConvertUtils.bytes2Chars(passWord)    //将字符串转换为字符串数组

        var doConnect = true
        val message = "{\"terminal_uid\":\"$clientId\"}"
        val topic = topics
        val qos = 0
        val retained = false
        if (message != "" || topic != "") {
            // MQTT本身就是为信号不稳定的网络设计的，所以难免一些客户端会无故的和Broker断开连接。
            //当客户端连接到Broker时，可以指定LWT，Broker会定期检测客户端是否有异常。
            //当客户端异常掉线时，Broker就往连接时指定的topic里推送当时指定的LWT消息。
            try {
                conOpt!!.setWill(topic, message.toByteArray(), qos, retained)
            } catch (e: Exception) {
                LogUtils.i(TAG, "Exception Occured", e);
                doConnect = false
                iMqttActionListener.onFailure(null, e)
            }
        }
        if (doConnect) {
            doClientConnection()
        }
    }


    override fun onDestroy() {
        if (client != null) {
            //client!!.disconnect()
            client?.unregisterResources()
        }

        stopSelf()
        super.onDestroy()
    }

    /** 连接MQTT服务器  */
    private fun doClientConnection() {
        if (client?.isConnected == false && isConnectIsNormal) {
            try {
                client?.connect(conOpt, null, iMqttActionListener)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }


    override fun onBind(intent: Intent): IBinder? {
        return CustomBinder()
    }

    fun setIGetMessageCallBack(IGetMessageCallBack: IGetMessageCallBack) {
        this.IGetMessageCallBack = IGetMessageCallBack
    }

    inner class CustomBinder : Binder() {
        val service: MqttService
            get() = this@MqttService
    }


    fun sendAckOfReceivingProgramsFromServer(programTaskId: Int, serId: String, extSerId: String) {
        val cmdByteArray = byteArrayOf()
        client?.publish("主题", cmdByteArray, 1, true)
    }


    fun <T> sendAck(cmd: Cmd) {
        val cmdByteArray = byteArrayOf()
        client?.publish("upStreamTopic", cmdByteArray, 1, true)
    }

}