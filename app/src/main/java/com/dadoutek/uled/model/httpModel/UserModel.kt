package com.dadoutek.uled.model.httpModel

import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.network.ModeStatusBean
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object UserModel {
    fun updateModeStatus(): Observable<Response<Any>> {
        var mode = if (Constant.IS_ROUTE_MODE) 1 else 0
        LogUtils.v("zcl-----------获取状态更新模式-------${Constant.IS_ROUTE_MODE}---$mode----${Constant.IS_OPEN_AUXFUN}")
        return NetworkFactory.getApi()
                .updateAllModeStatus(Constant.IS_OPEN_AUXFUN, mode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun getModeStatus(): Observable<ModeStatusBean>? {
        return NetworkFactory.getApi()
                .allModeStatus
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())

                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun deleteAllData(token: String): Observable<String>? {
        return NetworkFactory.getApi()
                .clearUserData(token)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun clearUserData(regionId: Int): Observable<String>? {
        return NetworkFactory.getApi()
                .clearUserRegionData(regionId)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }
}