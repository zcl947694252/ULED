package com.dadoutek.uled.stomp

import android.content.Intent
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.DeviceUtils.getIMEI
import com.google.gson.Gson
import kotlin.synchronized as synchronized


/**
 * 创建者     ZCL
 * 创建时间   2020/7/21 17:32
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
object MqttManger {
  /*  private var isconnect: Boolean = false
    private var connection: CallbackConnection? = null
    private var mqtt: MQTT? = null
    private val imei = getIMEI(TelinkLightApplication.getApp().mContext)
    private var clientId: String = NetworkFactory.md5((DBUtils.lastUser?.id ?: 0).toString() + imei).substring(8, 24) //设备唯一标识 MD5加密一定是32位
    private var topics = arrayOf(Topic("app/emit/${DBUtils.lastUser?.id}", QoS.AT_LEAST_ONCE))

    private val listener = object : Listener {
        override fun onFailure(value: Throwable?) {
            LogUtils.v("zcl_mqtt--******mqtt连接回调----------onFailure");
        }

        override fun onPublish(topic: UTF8Buffer?, body: Buffer?, ack: Runnable?) {
            LogUtils.v("zcl_mqtt--****mqtt连接回调------------onPublish---${topic?.toString()}---${body?.toString()}");
            val bean = Gson().fromJson(body?.ascii().toString(), MqttBodyBean::class.java)
            ack?.run()
            val intent = Intent()
            intent.action = Constant.LOGIN_OUT
            intent.putExtra(Constant.LOGIN_OUT, bean)
            TelinkLightApplication.getApp().mContext.sendBroadcast(intent)
        }


        override fun onConnected() {
            //LogUtils.v("zcl_mqtt--******mqtt连接回调------------onConnected");
        }

        override fun onDisconnected() {
            // LogUtils.v("zcl_mqtt--******mqtt连接回调----------onDisconnected");
        }
    }
    private val connectBack = object : Callback<Void?> {
        override fun onSuccess(value: Void?) {
            LogUtils.v("zcl_mqtt******mqtt连接成功----------$value")
            isconnect = true
            //initTopics()
        }

        override fun onFailure(value: Throwable?) {
            LogUtils.v("zcl_mqtt******mqtt连接失败----------$value")
            //initMqtt()
        }
    }

    private fun initTopics() {
        connection?.subscribe(topics, object : Callback<ByteArray?> {
            override fun onSuccess(value: ByteArray?) {
                LogUtils.v("zcl_mqtt------------------订阅成功")
            }

            override fun onFailure(value: Throwable?) {
                LogUtils.v("zcl_mqtt------------------订阅失败")
                initTopics()
            }
        })
    }

    private fun startPublish() {
        connection?.publish("singleLogin", "你好".toByteArray(), QoS.AT_LEAST_ONCE, false, object : Callback<Void?> {
            override fun onSuccess(value: Void?) {
                LogUtils.v("zcl_mqtt------------------发送成功$value")
            }

            override fun onFailure(value: Throwable?) {
                // connection?.disconnect(null)
                LogUtils.v("zcl_mqtt------------------发送失败$value")
            }
        })
    }

    fun initMqtt() {//去订阅app/test 浏览器里访问 http://dev.dadoutek.com/smartlight_test/test/send 会向该频道发送{"hello":"world"}
        synchronized(this) {
            if (mqtt == null)
                mqtt = MQTT()
            mqtt?.setHost(Constant.HOST, Constant.PORT)
            mqtt?.setClientId(clientId)
            mqtt?.isCleanSession = true//当客户端掉线时 ，服务器端会清除 客户端 session 。 重连后 客户端会有一个新的session。
            mqtt?.keepAlive = 10//心跳时间

            mqtt?.setUserName("APP_${DBUtils.lastUser?.id ?: 0}")
            mqtt?.setPassword("123456")
            // mqtt?.isWillRetain =true //是否在断联后发送最后消息
            // mqtt?.setWillMessage("close")
            mqtt?.willQos = QoS.AT_LEAST_ONCE//至少一次

            mqtt?.connectAttemptsMax = 0//默认是-1 无重试上限
            mqtt?.reconnectDelay = 1000 //首次重连间隔时间毫秒
            mqtt?.reconnectAttemptsMax =0//设置为-1以使用无限尝试。默认为-1
            mqtt?.reconnectDelayMax = 5000 //在重连接尝试之间等待ms的最大时间。缺省值为30,000。


            //socket设置
            mqtt?.receiveBufferSize = 65536 //设socket缓冲区大小 ,默认65536 64k
            mqtt?.sendBufferSize = 65536//发送缓冲区
            //设置发哦是那个数据包头的流量类型或服务器类型字段 默认8 意味吞吐量最大化传出
            mqtt?.trafficClass = 8
            //带宽限制
            mqtt?.maxReadRate = 0 //设置连接的最大接收速率单位bytes/s  0 无限制
            mqtt?.maxWriteRate = 0 //设置发送的最大接收速率单位bytes/s  0 无限制
            if (connection == null)
                connection = mqtt?.callbackConnection()
            connection?.listener(listener)
            if (!isconnect)
                connection?.connect(connectBack)
            initTopics()
        }
    }

    fun doDisconnect() {
        //切换账号后会导致无法定下  可能是由于当时上一个连接的没有断开
        connection?.kill(null)
        connection?.failure()
    }*/
}