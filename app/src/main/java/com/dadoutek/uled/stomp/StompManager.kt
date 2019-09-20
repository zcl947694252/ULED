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
    val WS_TOPIC_CODE = "/topic/code.parse." + DBUtils.lastUser?.id
    //取消收授权频道
    val WS_AUTHOR_CODE = "/topic/authorization.cancel."+ DBUtils.lastUser?.id

    //单点登录频道
    val WS_TOPIC_LOGIN = "/user/topic/user.login.state"

    private var mStompClient: StompClient? = null

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
        headers.add(StompHeader("host", WS_DEBUG_HOST))

        //如果已经初始化过了就不初始化了
        if (mStompClient == null) {
            mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_BASE_URL_DEBUG)

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

    private fun getLoginHeaders(): List<StompHeader> {
        val headersLogin = ArrayList<StompHeader>()

        /**
         * ack	String	否	固定:auto
        id	String	是	固定:login-state
        exclusive	boolean	是	固定: true
         */
        headersLogin.add(StompHeader("ack", "auto"))
        headersLogin.add(StompHeader("id", "login-state"))
        headersLogin.add(StompHeader("exclusive", "login-true"))
        return headersLogin
    }

    private fun getQRHeaders(): List<StompHeader> {
        val headersLogin = ArrayList<StompHeader>()
        /**
         *ack	String	是	固定: auto
         *  id	String	是	固定: code-parse
         *exclusive	boolean	是	固定: true
         *  x-queue-name	boolean	是	固定: sl-code-parse-用户id
         */
        headersLogin.add(StompHeader("ack", "auto"))
        headersLogin.add(StompHeader("id", "code-parse"))
        headersLogin.add(StompHeader("exclusive", "true"))
//        headersLogin.add(StompHeader("x-queue-name", "sl-code-parse-"+DBUtils.lastUser?.id))
        return headersLogin
    }

    private fun getAuthorHeaders(): List<StompHeader> {
        val headersLogin = ArrayList<StompHeader>()
        /**
        ack	String	是	固定: client-individual
        id	String	是	固定: authorization-cancel
        durable	boolean	是	固定: true
        auto-delete	boolean	是	固定: false
        x-queue-name	boolean	是	固定: sl-authorization-cancel-用户id
        */
        headersLogin.add(StompHeader("ack", "client-individual"))
        headersLogin.add(StompHeader("id", "authorization-cancel"))
        headersLogin.add(StompHeader("durable", "true"))
        headersLogin.add(StompHeader("auto-delete", "false"))
        headersLogin.add(StompHeader("x-queue-name", "sl-authorization-cancel-"+DBUtils.lastUser?.id))
        return headersLogin
    }

    /**
     * 解析二维码的Flowable
     * @return onNext中的数据为key
     */
    fun parseQRCodeTopic(): Flowable<QrCodeTopicMsg> {
        val headersLogin = getQRHeaders()
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
        val headersLogin = getLoginHeaders()
        return mStompClient!!.topic(WS_TOPIC_LOGIN, headersLogin)
                .map { topicMessage ->
                    topicMessage.payload
                }

    }

    /**
     * 解除授权的Flowable
     * @return onNext中的数据为key
     */
    fun cancelAuthorization(): Flowable<CancelAuthorMsg> {
        val headersLogin = getAuthorHeaders()
        return mStompClient!!.topic(WS_AUTHOR_CODE, headersLogin)
                .map { topicMessage ->
                    val payload = topicMessage.payload
                    val msg = Gson().fromJson(payload, CancelAuthorMsg::class.java)
                    msg
                }
    }



}
