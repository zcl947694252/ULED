package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbRegion
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object RegionModel {
    fun add(token: String, controlMesh: String, controlMeshPwd: String, installMesh: String
            , installMeshPwd: String, id: Long, changeId: Long?): Observable<String>? {
        return NetworkFactory.getApi()
                .addRegion(token,controlMesh,controlMeshPwd,installMesh,installMeshPwd,changeId!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, rid: Int ,controlMesh: String, controlMeshPwd: String,installMesh: String
               ,installMeshPwd: String,id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .updateRegion(token,rid,controlMesh,controlMeshPwd,installMesh,installMeshPwd)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun delete(token: String, rid: Int,id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .deleteRegion(token,rid)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun get(token: String): Observable<MutableList<DbRegion>>? {
        return NetworkFactory.getApi()
                .getRegionList(token)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    for(item in it){
                        DBUtils.saveRegion(item,true)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}