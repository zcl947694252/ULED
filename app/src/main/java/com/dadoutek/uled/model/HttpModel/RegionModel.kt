package com.dadoutek.uled.model.HttpModel

import android.content.Context
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbRegion
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.network.bean.RegionAuthorizeBean
import com.dadoutek.uled.region.bean.RegionBean
import com.dadoutek.uled.region.bean.ShareCodeBean
import com.dadoutek.uled.region.bean.TransferData
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object RegionModel {

    fun add(token: String, dbRegion: DbRegion, id: Long, changeId: Long?): Observable<String>? {
        return NetworkFactory.getApi()
                .addRegion(token, dbRegion, changeId!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, rid: Int, dbRegion: DbRegion, id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .updateRegion(token, rid, dbRegion)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun delete(token: String, rid: Int, id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .deleteRegion(token, rid)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    /*区域新接口*/
    fun get(): Observable<MutableList<RegionBean>>? {
        return NetworkFactory.getApi()
                .gotRegionActivityList()
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    for (item in it) {
                        val dbRegion = DbRegion()
                        dbRegion.installMeshPwd = item.installMeshPwd
                        dbRegion.controlMeshPwd = item.controlMeshPwd
                        dbRegion.belongAccount = item.belongAccount
                        dbRegion.controlMesh = item.controlMesh
                        dbRegion.installMesh = item.installMesh
                        dbRegion.name = item.name
                        dbRegion.id = item.id
                        DBUtils.saveRegion(dbRegion, true)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
    }


    fun getAuthorizerList(): Observable<MutableList<RegionAuthorizeBean>>? {
        return NetworkFactory.getApi()
                .gotAuthorizerList()
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }


    fun addRegions(token: String, dbRegion: DbRegion, rid: Long?): Observable<Any>? {
        return NetworkFactory.getApi()
                .addRegionNew(token, dbRegion, rid!!)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun getAuthorizationCode(regionId: Long): Observable<ShareCodeBean>? {
        return NetworkFactory.getApi()
                .regionAuthorizationCode(regionId)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun removeAuthorizationCode(regionId: Long, type: Int): Observable<String>? {
        return NetworkFactory.getApi()
                .removeAuthorizeCode(regionId, type)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun removeTransferCode(): Observable<String>? {
        return NetworkFactory.getApi()
                .removeTransferCode()
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun removeRegion(id: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .removeRegion(id.toInt())
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun parseQRCode(code: String,password: String): Observable<String>? {
        return NetworkFactory.getApi()
                .parseQRCode(code,password)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun cancelAuthorize(ref_id: Int, rid: Int): Observable<String>? {
        return NetworkFactory.getApi()
                .cancelAuthorize(ref_id, rid)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     *授权码失效
     */
    fun dropAuthorizeRegion(authorizer_id: Int, rid: Int): Observable<String>? {
        return NetworkFactory.getApi()
                .dropAuthorize(authorizer_id, rid)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 生成移交吗
     */
    fun transferCode(): Observable<TransferData> {
        return NetworkFactory.getApi()
                .makeTransferCode()
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 查看移交吗
     */
    fun lookTransferCodeState(): Observable<TransferData> {
        //数据转换
        return NetworkFactory.getApi()
                .mlookTransferCode()
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }


    /**
     * 查看生成授权码
     */
    fun lookAuthorizeCode(rid: Long, context: Context): Observable<ShareCodeBean> {
        return NetworkFactory.getApi()
                .mlookAuthroizeCode(rid)
                .compose(NetworkTransformer())
                .flatMap{
                    var isNewQr = it.code == null || it.code.trim() == ""||it.expire<=0
                    SharedPreferencesHelper.putBoolean(context,Constant.IS_NEW_AUTHOR_CODE,isNewQr)
                    if (isNewQr)
                        NetworkFactory.getApi().regionAuthorizationCode(rid).compose(NetworkTransformer())
                    else
                        Observable.create { emitter ->
                            emitter.onNext(it)
                        }
                }
                .subscribeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 查看生成移交吗
     */
    fun lookTransferCode(context: Context): Observable<TransferData> {
        //数据转换
        return NetworkFactory.getApi()
                .mlookTransferCode()
                .compose(NetworkTransformer())
                .flatMap {
                    var isNewQr = it.code == null || it.code.trim() == ""||it.expire<=0
                    SharedPreferencesHelper.putBoolean(context,Constant.IS_NEW_TRANSFER_CODE,isNewQr)
                    if (isNewQr)
                        NetworkFactory.getApi().makeTransferCode().compose(NetworkTransformer())
                    else {
                        Observable.create { emitter ->
                            emitter.onNext(it)
                        }
                    }
                }
                .subscribeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }
}

