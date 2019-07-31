package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.model.ResponseVersionAvailable
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.network.VersionBean
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

object UpdateModel {

    fun isVersionAvailable(device: Int, version: String): Observable<ResponseVersionAvailable> {
        return NetworkFactory.getApi()
                .isAvailavle(device, version)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun getVersion(version: String): Observable<VersionBean>? {
        // language==zh 英文 language==en
        var languageType: Int = if (Locale.getDefault().language.contains("zh")) 0 else 1
        return NetworkFactory.getApi()
                .getVersion(version, 0, languageType)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())

    }

    fun isRegister(phone: String): Observable<Any>? {
        return NetworkFactory.getApi()
                .isRegister(phone)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}