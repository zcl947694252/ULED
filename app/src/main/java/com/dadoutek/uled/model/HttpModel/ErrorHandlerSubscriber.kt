package com.cheyw.liaofan.common.rx.subscriber

import android.content.Context
import com.dadoutek.uled.model.HttpModel.RxErrorHandler
import io.reactivex.subscribers.DefaultSubscriber


/**
 * 创建者     ZCL
 * 创建时间   2018/4/4 18:55
 * 描述	      ${ErrorHanlderSubcriber没有上下文所以我们无法创建对象只能外部传入,需要注入}
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${
 */

abstract class ErrorHandlerSubscriber<T>(context: Context) : DefaultSubscriber<T>() {
    var mErrorHanlder: RxErrorHandler = RxErrorHandler(context)

    override fun onComplete() {}

    override fun onError(t: Throwable) {
        t.printStackTrace()
        //进行异常判断
        val exception = mErrorHanlder.create(t)
        if (exception == null) {
            exception!!.printStackTrace()
        }
        //进行异常友好提示
        mErrorHanlder.showErrorMessage(exception)
    }
}

