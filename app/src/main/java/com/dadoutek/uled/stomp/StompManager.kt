package com.dadoutek.uled.stomp

import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.base.CancelAuthorMsg
import com.dadoutek.uled.model.Constant
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
/*    //二维码频道
    val WS_TOPIC_CODE = "/topic/code.parse." + DBUtils.lastUser?.id
        废弃 因为使用这个是再登录成功之前赋值 是上一个账号
    //取消收授权频道
    var WS_AUTHOR_CODE = "/topic/authorization.cancel."+ DBUtils.lastUser?.id*/

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
        headers.add(StompHeader("host", Constant.WS_HOST))

        //如果已经初始化过了就不初始化了
        if (mStompClient == null) {
            mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, Constant.WS_STOMP_URL)

            mStompClient?.connect(headers)
            mStompClient?.withClientHeartbeat(5000)
                    ?.withServerHeartbeat(5000)   //设置心跳
        } else if (mStompClient?.isConnected == false) { //如果断连了就再连一次
            mStompClient?.connect(headers)
            mStompClient?.withClientHeartbeat(5000)?.withServerHeartbeat(5000)   //设置心跳
        }
    }

    /**
     *  @return
     *   onNext内的type属性内有三种状态
     *  LifecycleEvent.Type.OPENED，LifecycleEvent.Type.ERROR，LifecycleEvent.Type.CLOSED
     */
    fun lifeCycle(): Flowable<LifecycleEvent>? {
        return mStompClient?.lifecycle()
    }

    private fun getGwHeaders(): List<StompHeader> {
        val headersLogin = ArrayList<StompHeader>()
        /**
         * ack	String	是	固定: auto
         * id	String	是	固定: common-cmd
         */
        headersLogin.add(StompHeader("ack", "auto"))
        headersLogin.add(StompHeader("id", "common-cmd"))
        return headersLogin
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

        return headersLogin
    }

    /**
     * 解析二维码的Flowable
     * @return onNext中的数据为key
     */
    fun parseQRCodeTopic(): Flowable<QrCodeTopicMsg> {
        val headersLogin = getQRHeaders()

        val WS_TOPIC_CODE = "/topic/code.parse." + DBUtils.lastUser?.id
        // LogUtils.v("zcld订阅频道$WS_TOPIC_CODE")
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
     * 通用推送订阅，通过不同的cmd值进行逻辑判断
     * ack	String	是	固定: auto
     * id	String	是	固定: common-cmd
     */
    fun gwCommend(): Flowable<String> {
        val gwHeaders = getGwHeaders()
       // /topic/common.cmd. + 用户id
        var gwCommendUrl = "/topic/common.cmd." + DBUtils.lastUser?.id
        return mStompClient!!.topic(gwCommendUrl, gwHeaders)
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
//        LogUtils.e("解除授权的订阅id  ----${DBUtils.lastUser}")
        var WSAUTHOR_CODE_NOW = "/topic/authorization.cancel." + DBUtils.lastUser?.id

        return mStompClient!!.topic(WSAUTHOR_CODE_NOW, headersLogin)
                .map { topicMessage ->
                    val payload = topicMessage.payload
                    val msg = Gson().fromJson(payload, CancelAuthorMsg::class.java)
                    LogUtils.v("zcl解除授权得到信息$msg")
                    msg
                }
    }
}
