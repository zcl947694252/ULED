package com.dadoutek.uled.stomp

import android.util.Log
import com.blankj.utilcode.util.PhoneUtils.getIMEI
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.DeviceUtils
import com.dadoutek.uled.util.DeviceUtils.getIMEI
import org.fusesource.hawtbuf.Buffer
import org.fusesource.hawtbuf.UTF8Buffer
import org.fusesource.mqtt.client.*
import org.greenrobot.greendao.DbUtils


/**
 * 创建者     ZCL
 * 创建时间   2020/7/21 17:32
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class MqttManger private constructor() {
    private val HOST: String? = "47.107.227.130"
    private val PORT: Int = 1885
    private var connection: CallbackConnection? = null
    private var mqtt: MQTT? = null
    private val imei = getIMEI(TelinkLightApplication.getApp().mContext)
    private var CLIENTID: String = NetworkFactory.md5((DBUtils.lastUser?.id?:0).toString()+ imei) //设备唯一标识
    var topics = arrayOf(Topic("app/test", QoS.AT_LEAST_ONCE), Topic("app/test", QoS.AT_LEAST_ONCE))

    private val listener = object : Listener {
        override fun onFailure(value: Throwable?) {
            Log.e("zcl_mqtt", "zcl******mqtt连接回调----------onFailure");
            initMqtt()
        }

        override fun onPublish(topic: UTF8Buffer?, body: Buffer?, ack: Runnable?) {
            Log . e ("zcl_mqtt", "zcl******mqtt连接回调------------onPublish---${topic?.toString()}---${body?.toString()}======${body?.get(0).toString()}====${body?.get(1).toString()}");
        }

        override fun onConnected() {
            Log.e("zcl_mqtt", "zcl******mqtt连接回调------------onConnected");
        }

        override fun onDisconnected() {
            Log.e("zcl_mqtt", "zcl******mqtt连接回调----------onDisconnected");
        }
    }
    private val connectBack = object : Callback<Void?> {
        override fun onSuccess(value: Void?) {
            Log.e("zcl_mqtt", "zcl******mqtt连接成功----------value")
            initTopics()
        }

        override fun onFailure(value: Throwable?) {
            Log.e("zcl_mqtt", "zcl******mqtt连接失败----------value")
        }
    }

    private fun initTopics() {
        connection?.subscribe(topics, object : Callback<ByteArray?> {
            override fun onSuccess(value: ByteArray?) {
                Log.v("zcl_mqtt","zclmqtt------------------订阅成功")
                startPublish()
            }

            override fun onFailure(value: Throwable?) {
                Log.v("zcl_mqtt","zclmqtt------------------订阅失败")
                initTopics()
            }
        })
    }

    private fun startPublish() {
        connection?.publish("singleLogin", "你好".toByteArray(), QoS.AT_LEAST_ONCE, false, object : Callback<Void?> {
            override fun onSuccess(value: Void?) {
                Log.v("zcl_mqtt","zclmqtt------------------发送成功")
            }

            override fun onFailure(value: Throwable?) {
               // connection?.disconnect(null)
            }
        })
    }

    private fun initMqtt() {//去订阅app/test 浏览器里访问 http://dev.dadoutek.com/smartlight_test/test/send 会向该频道发送{"hello":"world"}
        mqtt = MQTT()
        mqtt?.setHost(HOST, PORT)
        mqtt?.setClientId(CLIENTID)
        mqtt?.isCleanSession = false//是否记住重连时请清除缓存
        mqtt?.keepAlive = 5//心跳时间

        mqtt?.setUserName("APP_${DBUtils.lastUser?.id?:0}")
        mqtt?.setPassword("123456")

        mqtt?.isWillRetain =true //是否在断联后发送最后消息
        mqtt?.setWillMessage("close")
        mqtt?.willQos = QoS.AT_LEAST_ONCE//至少一次
        mqtt?.version ="3.3.0"

        mqtt?.connectAttemptsMax = 1//默认是-1 无重试上限
        mqtt?.reconnectDelay =10 //首次重连间隔时间毫秒
        mqtt?.reconnectAttemptsMax = 30000 //重连间隔时间默认时间30000毫秒

        //socket设置
        mqtt?.receiveBufferSize = 65536 //设socket缓冲区大小 ,默认65536 64k
        mqtt?.sendBufferSize = 65536//发送缓冲区
        //设置发哦是那个数据包头的流量类型或服务器类型字段 默认8 意味吞吐量最大化传出
        mqtt?.trafficClass=8
        //带宽限制
        mqtt?.maxReadRate = 0 //设置连接的最大接收速率单位bytes/s  0 无限制
        mqtt?.maxWriteRate = 0 //设置发送的最大接收速率单位bytes/s  0 无限制

        connection = mqtt?.callbackConnection()
        connection?.listener(listener)
        connection?.connect(connectBack)
    }
}