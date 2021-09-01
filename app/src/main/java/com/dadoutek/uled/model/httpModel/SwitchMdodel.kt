package com.dadoutek.uled.model.httpModel

import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.model.BodyBias
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbSwitch
import com.dadoutek.uled.model.dbModel.DbSwitchChild
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.network.bean.SwitchListBodyBean
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object SwitchMdodel {
    fun add(token: String, switch: DbSwitch, id: Long, changeId: Long?): Observable<String>? {
        var dbChild = DbSwitchChild()
        dbChild.productUUID = switch.productUUID
        dbChild.meshAddr = switch.meshAddr
        dbChild.macAddr = switch.macAddr
        dbChild.controlGroupAddrs = switch.controlGroupAddrs

        if (switch.controlSceneId != null) {
            dbChild.list = switch.controlSceneId
        } else {
            dbChild.list = " "
        }
        dbChild.name = switch.name
        dbChild.setId(switch.id)
        dbChild.index = switch.index
        dbChild.controlGroupAddr = switch.controlGroupAddr
        dbChild.belongGroupId = switch.belongGroupId

        dbChild.firmwareVersion = switch.version
        dbChild.type = switch.type
        dbChild.keys = switch.keys
        LogUtils.v("zcl-----------添加H开关新mesh-------${switch?.meshAddr}")
        return NetworkFactory.getApi()
                .addSwitch(token, dbChild, changeId!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, dbSwitch: DbSwitch, lid: Int, id: Long): Observable<String> {
        var dbChild = DbSwitchChild()
        dbChild.productUUID = dbSwitch.productUUID
        dbChild.meshAddr = dbSwitch.meshAddr
        dbChild.macAddr = dbSwitch.macAddr
        dbChild.controlGroupAddrs = dbSwitch.controlGroupAddrs
        if (dbSwitch.controlSceneId != null) {
            dbChild.list = dbSwitch.controlSceneId
        } else {
            dbChild.list = " "
        }
        dbChild.name = dbSwitch.name
        dbChild.setId(dbSwitch.id)
        dbChild.index = dbSwitch.index
        dbChild.controlGroupAddr = dbSwitch.controlGroupAddr
        dbChild.belongGroupId = dbSwitch.belongGroupId

        dbChild.firmwareVersion = dbSwitch.version
        dbChild.type = dbSwitch.type
        dbChild.keys = dbSwitch.keys
        LogUtils.v("zcl-----------更新H开关新mesh-------${dbSwitch?.meshAddr}")
        return NetworkFactory.getApi()
                .updateSwitch(token, lid, dbChild)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun delete(token: String, id: Long, lid: Int): Observable<String>? {
        return NetworkFactory.getApi()
                .deleteSwitch(token, lid)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun get(token: String): Observable<MutableList<DbSwitch>>? {
        return NetworkFactory.getApi()
                .getSwitchList(token)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    for (item in it) {
                        DBUtils.saveSwitch(item, true)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun batchAddOrUpdateSwitch(list:List<DbSwitch>) : Observable<String>? {
        return NetworkFactory.getApi()
            .updateSwitch(SwitchListBodyBean(list))
            .compose(NetworkTransformer())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {

            }
    }


    fun remove(list: List<Int>) : Observable<String> ? {
        return NetworkFactory.getApi()
            .deleteSwitch(BodyBias(list))
            .compose(NetworkTransformer())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {

            }
    }
}