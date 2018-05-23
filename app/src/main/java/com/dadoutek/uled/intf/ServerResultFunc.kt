package com.dadoutek.uled.intf

import com.dadoutek.uled.model.Response

import io.reactivex.functions.Function

class ServerResultFunc<T> : Function<Response<T>, T> {
    @Throws(Exception::class)
    override fun apply(response: Response<T>): T {
        if (response.errorCode != NetworkStatusCode.OK)
            ServerResultException.handleException(response)
        return response.t
    }
}