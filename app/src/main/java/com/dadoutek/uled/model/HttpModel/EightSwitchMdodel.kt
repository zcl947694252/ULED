package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.switches.bean.EightSwitchItemBean
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


/**
 * 创建者     ZCL
 * 创建时间   2020/1/16 15:18
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */

object EightSwitchMdodel {

    fun add(/*switch: DbEightSwitch, id: Long, changeId: Long?*/): Observable<Response<EightSwitchItemBean>>? {
       return NetworkFactory.getApi().addSwitch8k(1,DeviceType.EIGHT_SWITCH,1,"假的","xxx:xxx",DeviceType.NORMAL_SWITCH,1,"01234567")
                .subscribeOn(Schedulers.io())
                               .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(): Observable<Response<Int>>? {
        val list = mutableListOf(1)
        val body = BatchRemove8kBody()
        body.idList = list

        return NetworkFactory.getApi().removeSwitch8kList(body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun delete(swid:Long): Observable<Response<Int>>? {
        return NetworkFactory.getApi().removeSwitch8k(swid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun get(): Observable<Response<MutableList<EightSwitchItemBean>>>? {
        return NetworkFactory.getApi().switch8kList(true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}