package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object SwitchMdodel {


    fun add(token: String, switch: DbSwitch, id: Long, changeId: Long?): Observable<String>? {
        var dbChild=DbSwitchChild()
        dbChild.productUUID=switch.productUUID
        dbChild.meshAddr=switch.meshAddr
        dbChild.macAddr=switch.macAddr
        if(switch.controlSceneId!=null){
            dbChild.list=switch.controlSceneId
        }else{
            dbChild.list=" "
        }
        dbChild.name=switch.name
        dbChild.id=switch.id
        dbChild.index=switch.index
        dbChild.controlGroupAddr=switch.controlGroupAddr
        dbChild.belongGroupId=switch.belongGroupId
        return NetworkFactory.getApi()
                .addSwitch(token,dbChild,changeId!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, dbSwitch: DbSwitch, lid: Int, id: Long): Observable<String> {
        var dbChild=DbSwitchChild()
        dbChild.productUUID=dbSwitch.productUUID
        dbChild.meshAddr=dbSwitch.meshAddr
        dbChild.macAddr=dbSwitch.macAddr
        if(dbSwitch.controlSceneId!=null){
            dbChild.list=dbSwitch.controlSceneId
        }else{
            dbChild.list=" "
        }
        dbChild.name=dbSwitch.name
        dbChild.id=dbSwitch.id
        dbChild.index=dbSwitch.index
        dbChild.controlGroupAddr=dbSwitch.controlGroupAddr
        dbChild.belongGroupId=dbSwitch.belongGroupId
        return NetworkFactory.getApi()
                .updateSwitch(token,lid,dbChild)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun delete(token: String,  id: Long,  lid: Int): Observable<String>? {
        return NetworkFactory.getApi()
                .deleteSwitch(token,lid)
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
                    for(item in it){
                        DBUtils.saveSwitch(item,true)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}