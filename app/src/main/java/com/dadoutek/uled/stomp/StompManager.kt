package com.dadoutek.uled.stomp

import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.base.CancelAuthorMsg
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.stomp.model.QrCodeTopicMsg
import com.google.gson.Gson
import io.reactivex.Flowable
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import java.util.*


class StompManager private constructor() {
    //长连接请求服务器域名地址
    val WS_BASE_URL = "ws://dev.dadoutek.com/smartlight_java/websocket-endpoint"
    //长连接测试请求服务器域名地址
    val WS_BASE_URL_DEBUG = "ws://dev.dadoutek.com/smartlight_test/websocket-endpoint"
    //虚拟主机号。测试服:/smartlight/test 正式服:/smartlight
    val WS_DEBUG_HOST = "/smartlight/test"
    //虚拟主机号
    val WS_HOST = "/smartlight"
    //二维码频道
    val WS_TOPIC_CODE = "/user/topic/code.parse"
    //取消收授权频道
    val WS_AUTHOR_CODE = "/user/topic/authorization.cancel"
    //单点登录频道
    val WS_TOPIC_LOGIN = "/user/topic/user.login.state"

    var mStompClient: StompClient? = null

    companion object {
        private var instance: StompManager? = null
            get() {
                if (field == null) {
                    field = StompManager()
                }
                return field
            }

        @Synchronized
        fun get(): StompManager {
            return instance!!
        }
    }

    fun initStompClient() {
        val headers = ArrayList<StompHeader>()
        headers.add(StompHeader("user-id", DBUtils.lastUser?.id.toString()))
        headers.add(StompHeader("host", WS_HOST))

        //如果已经初始化过了就不初始化了
        if (mStompClient == null) {
            mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_BASE_URL)

            mStompClient?.connect(headers)
            mStompClient?.withClientHeartbeat(5000)
                    ?.withServerHeartbeat(5000)   //设置心跳
        } else if (mStompClient?.isConnected == false) { //如果断连了就再连一次
            mStompClient?.connect(headers)
            mStompClient?.withClientHeartbeat(5000)
                    ?.withServerHeartbeat(5000)   //设置心跳
        }


    }

    /**
     *  @return
     *   onNext内的type属性内有三种状态
     *  LifecycleEvent.Type.OPENED，LifecycleEvent.Type.ERROR，LifecycleEvent.Type.CLOSED
     */
    fun lifeCycle(): Flowable<LifecycleEvent>? {
        return mStompClient!!.lifecycle()

    }

    private fun getHeaders(): List<StompHeader> {
        val headersLogin = ArrayList<StompHeader>()
        headersLogin.add(StompHeader("id", DBUtils.lastUser?.id.toString()))
        headersLogin.add(StompHeader("destination", WS_HOST))
        return headersLogin
    }

    /**
     * 解析二维码的Flowable
     * @return onNext中的数据为key
     */
    fun parseQRCodeTopic(): Flowable<QrCodeTopicMsg> {
        val headersLogin = getHeaders()
        return mStompClient!!.topic(WS_TOPIC_CODE, headersLogin)
                .map { topicMessage ->
                    val payloadCode = topicMessage.payload
                    LogUtils.e("zcl_解析二维码$payloadCode")
                    val msg = Gson().fromJson(payloadCode, QrCodeTopicMsg::class.java)
                    msg
                }
    }


    /**
     * 单点登录的Flowable
     * @return onNext中的数据为key
     */
    fun singleLoginTopic(): Flowable<String> {
        val headersLogin = getHeaders()
        return mStompClient!!.topic(WS_TOPIC_LOGIN, headersLogin)
                .map { topicMessage ->
                    topicMessage.payload
                }

    } /**
     * 解除授权的Flowable
     * @return onNext中的数据为key
     */
    fun cancelAuthorization(): Flowable<CancelAuthorMsg> {
        val headersLogin = getHeaders()
        return mStompClient!!.topic(WS_AUTHOR_CODE, headersLogin)
                .map { topicMessage ->
                    val payload = topicMessage.payload
                    val msg = Gson().fromJson(payload, CancelAuthorMsg::class.java)
                    msg
                }
    }



}
