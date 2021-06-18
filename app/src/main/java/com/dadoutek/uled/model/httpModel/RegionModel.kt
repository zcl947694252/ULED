package com.dadoutek.uled.model.httpModel

import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbRegion
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.network.bean.RegionAuthorizeBean
import com.dadoutek.uled.network.bean.TransferRegionBean
import com.dadoutek.uled.region.RegionBcBean
import com.dadoutek.uled.region.bean.ParseCodeBean
import com.dadoutek.uled.region.bean.RegionBean
import com.dadoutek.uled.region.bean.ShareCodeBean
import com.dadoutek.uled.region.bean.TransferBean
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

    fun update( rid: Int, dbRegion: DbRegion, id: Long): Observable<RegionBcBean>? {
        return NetworkFactory.getApi()
                .updateRegion( rid, dbRegion)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(id)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun updateMesh( rid: Int, dbRegion: DbRegion): Observable<RegionBcBean>? {
        return NetworkFactory.getApi()
                .updateRegion( rid, dbRegion)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
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
                    //subscribe调用前使用的方法
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


    fun clearRegion(): Observable<String>? {
        return NetworkFactory.getApi()
                .clearRegion(DBUtils.lastUser?.last_region_id!!.toInt())
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {}
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
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun parseQRCode(code: String, password: String): Observable<ParseCodeBean>? {
        return NetworkFactory.getApi()
                .parseQRCode(code, password)
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
    fun transferCode(): Observable<TransferBean> {
        return NetworkFactory.getApi()
                .makeTransferCode()
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 查看授权码
     */
    fun lookAuthorCodeState(rid: Long): Observable<ShareCodeBean>? {
        //数据转换
        return NetworkFactory.getApi()
                .mlookAuthroizeCode(rid)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 查看移交吗
     */
    fun lookTransferCodeState(): Observable<TransferBean> {
        //数据转换
        return NetworkFactory.getApi()
                .mGetTransferCode()
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 查看生成授权码
     */
    fun lookAuthorizeCode(rid: Long): Observable<ShareCodeBean> {
        return NetworkFactory.getApi()
                .mlookAuthroizeCode(rid)
                .compose(NetworkTransformer())
                .flatMap {
                    var isNewQr = it.code == null || it.code.trim() == "" || it.expire <= 0
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
    fun lookTransferCode(): Observable<TransferBean> {
        //数据转换
        return NetworkFactory.getApi()
                .mGetTransferCode()
                .compose(NetworkTransformer())
                .flatMap {
                    var isNewQr = it.code == null || it.code.trim() == "" || it.expire <= 0
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

    /**
     * 查看生成单个区域移交码
     */
    fun lookAndMakeRegionQR(id: Long): Observable<TransferRegionBean>? {
        return NetworkFactory.getApi()
                .mlookRegionTransferCodeBean(id)
                .compose(NetworkTransformer())
                .flatMap {
                    var isNewQr = it.code == null || it.code.trim() == "" || it.expire <= 0
                    if (isNewQr)
                        NetworkFactory.getApi().mGetTransferRegionQR(id).compose(NetworkTransformer())
                    else {
                        Observable.create { emitter ->
                            emitter.onNext(it)
                        }
                    }
                }
                .doOnNext {}
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

    }

    /**
     * 查看单个区域移交码
     */
    fun lookTransforRegionCode(id: Long): Observable<TransferRegionBean>? {
        return NetworkFactory.getApi()
                .mlookRegionTransferCodeBean(id)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }


    /**
     * 取消区域移交码
     */

    fun removeTransferRegionCode(rid: Long): Observable<Response<TransferRegionBean>>? {
        return NetworkFactory.getApi()
                .removeTransferRegionCode(rid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 取消二维码 三码合一
     */
    fun removeQrCode(code: String): Observable<Response<Any>>? {
        val body = RemoveCodeBody()
        body.code = code
        return NetworkFactory.getApi()
                .allCodeRemove(body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 查看各个区域controlmesname
     */
    fun getRegionName(): Observable<Response<MutableList<String>>> {
        //数据转换
        return NetworkFactory.getApi()
                .regionNameList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}

