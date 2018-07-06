package com.dadoutek.uled.network


import com.dadoutek.uled.model.Response
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


class NetworkTransformer<T> : ObservableTransformer<Response<T>, T> {
    override fun apply(upstream: Observable<Response<T>>): ObservableSource<T> {
        return upstream
                .map(ServerResultFunc())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}
