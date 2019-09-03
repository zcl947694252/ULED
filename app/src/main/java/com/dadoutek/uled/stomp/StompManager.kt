package com.dadoutek.uled.stomp

import android.util.Log
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.stomp.model.QrCodeTopicMsg
import com.google.gson.Gson
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import java.util.*


class StompManager private constructor() {
    private var mQrcodeTopicDisposable: Disposable? = null
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
        headers.add(StompHeader("host", Constant.WS_HOST))

        //如果已经初始化过了就不初始化了
        if (mStompClient == null) {
            mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, Constant.WS_BASE_URL)


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


    fun qrCodeTopic(): Flowable<QrCodeTopicMsg>? {
        val headersLogin = ArrayList<StompHeader>()
        headersLogin.add(StompHeader("id", DBUtils.lastUser?.id.toString()))
        headersLogin.add(StompHeader("destination", Constant.WS_HOST))
        return mStompClient?.topic(Constant.WS_TOPIC_CODE, headersLogin)
                ?.map { topicMessage ->
                    val payloadCode = topicMessage.payload
                    Gson().fromJson(payloadCode, QrCodeTopicMsg::class.java)
                }

    }


    /**
     * 单点登录的Flowable
     * @return onNext中的数据为key
     */
    fun singleLoginTopic(): Flowable<String> {
        val headersLogin = ArrayList<StompHeader>()
        headersLogin.add(StompHeader("id", DBUtils.lastUser?.id.toString()))
        headersLogin.add(StompHeader("destination", Constant.WS_HOST))
        return mStompClient!!.topic(Constant.WS_TOPIC_LOGIN, headersLogin)
                .map { topicMessage ->
                    topicMessage.payload
                }

    }


}
