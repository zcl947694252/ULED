package com.dadoutek.uled.util

import android.content.Context
import android.util.Log
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.model.HttpModel.*
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.google.gson.Gson
import com.mob.tools.utils.DeviceHelper
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import okhttp3.MediaType
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit


class SyncDataPutOrGetUtils {
    companion object {

        /********************************同步数据之上传数据 */

//        var isSuccess: Boolean = true

        @Synchronized
        fun syncPutDataStart(context: Context, syncCallback: SyncCallback) {

            Thread {

                val dbDataChangeList = DBUtils.dataChangeAll

                val dbUser = DBUtils.lastUser

                if (dbDataChangeList.size == 0) {
                    launch(UI) {
                        syncCallback.complete()
                    }
                    return@Thread
                }

                val observableList = ArrayList<Observable<String>>()
//                val observableList : MutableList<Observable<String>> = ArrayList<Observable<String>>()

                for (i in dbDataChangeList.indices) {
                    if (i == 0) {
                        launch(UI) {
                            syncCallback.start()
                        }
                    }
                    var observable: Observable<String> = this.sendDataToServer(dbDataChangeList[i].tableName,
                            dbDataChangeList[i].changeId,
                            dbDataChangeList[i].changeType,
                            dbUser!!.token, dbDataChangeList[i].id!!)!!
                    observableList.add(observable)

                    if (i == dbDataChangeList.size - 1) {
//                                observableNew = observableList.get(j).mergeWith(observableList.get(j-1))
//                                observableNew=Observable.merge(observableList)
                        val observables = arrayOfNulls<Observable<String>>(observableList.size)
                        observableList.toArray(observables)

                        Observable.mergeArrayDelayError<String>(*observables)
                                .doFinally {
                                }
                                .subscribe(object : NetworkObserver<String?>() {
                                    override fun onComplete() {
                                        launch(UI) {
                                            syncCallback.complete()
//                                            ToastUtils.showLong(context.getString(R.string.upload_data_success))
                                        }
                                    }
                                    override fun onSubscribe(d: Disposable) {
                                    }

                                    override fun onNext(t: String) {
                                    }

                                    override fun onError(e: Throwable) {
                                        launch(UI) {
                                            syncCallback.error(e.cause.toString())
//                                            ToastUtils.showLong(context.getString(R.string.upload_data_success))
                                        }
                                    }
                                })
                    }
                }

            }.start()
        }

        private fun sendDataToServer(tableName: String, changeId: Long, type: String,
                                     token: String, id: Long): Observable<String>? {
            var result: Observable<String>?
            when (tableName) {
                "DB_GROUP" -> {
                    when (type) {
                        Constant.DB_ADD -> {
                            val group = DBUtils.getGroupByID(changeId)
                            return GroupMdodel.add(token, group, group.belongRegionId, id, changeId)!!

                        }
                        Constant.DB_DELETE -> return GroupMdodel.delete(token, changeId.toInt(), id)
                        Constant.DB_UPDATE -> {
                            val group = DBUtils.getGroupByID(changeId)
                            return GroupMdodel.update(token, changeId.toInt(),
                                    group.name, group.brightness, group.colorTemperature, id)
                        }
                    }
                }
                "DB_LIGHT" -> {

                    when (type) {
                        Constant.DB_ADD -> {
                            val light = DBUtils.getLightByID(changeId)
                            return LightModel.add(token, light, id, changeId)
                        }
                        Constant.DB_DELETE -> {
                            return LightModel.delete(token,
                                    id, changeId.toInt())
                        }
                        Constant.DB_UPDATE -> {
                            val light = DBUtils.getLightByID(changeId)
                            return LightModel.update(token,
                                    light.name, light.brightness,
                                    light.colorTemperature, light.belongGroupId.toInt(),
                                    id, changeId.toInt())
                        }
                    }
                }
                "DB_REGION" -> {
                    when (type) {
                        Constant.DB_ADD -> {
                            val region = DBUtils.getRegionByID(changeId)
                            return RegionModel.add(token, region, id, changeId)
                        }
                        Constant.DB_DELETE -> return RegionModel.delete(token, changeId.toInt(),
                                id)
                        Constant.DB_UPDATE -> {
                            val region = DBUtils.getRegionByID(changeId)
                            return RegionModel.update(token,
                                    changeId.toInt(), region, id)
                        }
                    }
                }
                "DB_SCENE" -> {
                    val scene = DBUtils.getSceneByID(changeId)

                    lateinit var postInfoStr: String
                    var bodyScene: RequestBody? = null
                    if (scene != null && type != Constant.DB_DELETE) {
                        val body: DbSceneBody = DbSceneBody()
                        val gson: Gson = Gson()
                        body.name = scene.name
                        body.belongRegionId = scene.belongRegionId
                        body.actions = DBUtils.getActionsBySceneId(changeId)

                        postInfoStr = gson.toJson(body)

                        bodyScene = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), postInfoStr)
                    }

                    when (type) {
                        Constant.DB_ADD -> {
                            val scene = DBUtils.getSceneByID(changeId)
                            LogUtils.d("scene_add--id=="+changeId)
                            if(bodyScene!=null){
                                return SceneModel.add(token, bodyScene
                                        , id, changeId)
                            }
                        }
                        Constant.DB_DELETE -> {
                            LogUtils.d("scene_delete--id=="+changeId)
                            return SceneModel.delete(token,
                                    changeId.toInt(), id)
                        }
                        Constant.DB_UPDATE -> {
                            LogUtils.d("scene_update--id=="+changeId)
                            if(bodyScene!=null){
                                return SceneModel.update(token, changeId.toInt(), bodyScene, id)
                            }
                        }

                    }
                }

                "DB_USER" -> {
                    val user = DBUtils.getUserByID(changeId)

                    if (user == null && type != Constant.DELETEING) {
                        Log.d("用户数据出错", "")
                    }

                    when (type) {
                        Constant.DB_ADD -> {
                            //注册时已经添加
                            return null
                        }
                        Constant.DB_DELETE -> {
                            //无用户删除操作
                            return null
                        }
                        Constant.DB_UPDATE ->
                            return AccountModel.update(token, user.avatar, user.name, user.email, "oh my god!")

                    }
                }
            }
            return null
        }

        /********************************同步数据之下拉数据 */
        @Synchronized
        fun syncGetDataStart(dbUser: DbUser, syncCallBack: SyncCallback) {
            val token = dbUser.token
            startGet(token, dbUser.account, syncCallBack)
        }

        private fun startGet(token: String, accountNow: String, syncCallBack: SyncCallback) {

            NetworkFactory.getApi()
                    .getRegionList(token)
                    .compose(NetworkTransformer())
                    .flatMap {
                        for (item in it) {
                            DBUtils.saveRegion(item, true)
                        }

                        if (it.size != 0) {
                            setupMesh()
                            SharedPreferencesHelper.putString(TelinkLightApplication.getInstance(),
                                    Constant.USER_TYPE, Constant.USER_TYPE_NEW)
                        } else {
                            setupMeshCreat(accountNow)
                        }
                        NetworkFactory.getApi()
                                .getLightList(token)
                                .compose(NetworkTransformer())
                    }
                    .flatMap {
                        for (item in it) {
                            DBUtils.saveLight(item, true)
                        }
                        NetworkFactory.getApi()
                                .getGroupList(token)
                                .compose(NetworkTransformer())
                    }
                    .flatMap {
                        for (item in it) {
                            DBUtils.saveGroup(item, true)
                        }
                        NetworkFactory.getApi()
                                .getSceneList(token)
                                .compose(NetworkTransformer())
                    }
                    .observeOn(Schedulers.io())
                    .doOnNext {
                        for (item in it) {
                            DBUtils.saveScene(item, true)
                            for (i in item.actions.indices) {
                                val k = i + 1
                                DBUtils.saveSceneActions(item.actions[i], k.toLong(), item.id)
                            }
                        }
                    }
                    .observeOn(AndroidSchedulers.mainThread())!!.subscribe(
                    object : NetworkObserver<List<DbScene>>() {
                        override fun onNext(item: List<DbScene>) {
                            //登录后同步数据完成再上传一次数据
                            syncPutDataStart(TelinkLightApplication.getInstance(), syncCallbackSY)
                            SharedPreferencesUtils.saveCurrentUserList(accountNow)
//                            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.IS_LOGIN, true)
                            launch(UI) {
                                syncCallBack.complete()
                            }
                        }

                        override fun onError(e: Throwable) {
                            super.onError(e)
                            launch(UI) {
                                syncCallBack.error(e.message)
                            }
                        }
                    }
            )
        }

        internal var syncCallbackSY: SyncCallback = object : SyncCallback {

            override fun start() {

            }

            override fun complete() {
                LogUtils.d("putSuccess:" + "上传成功")
            }

            override fun error(msg: String) {
                LogUtils.d("GetDataError:" + msg)
            }

        }

        private fun setupMesh() {
            val regionList = DBUtils.regionAll

            //数据库有区域数据直接加载
            if (regionList.size != 0) {
//            val usedRegionID=SharedPreferencesUtils.getCurrentUseRegion()
                val dbRegion = DBUtils.lastRegion
                val application = DeviceHelper.getApplication() as TelinkLightApplication
                val mesh = application.mesh
                mesh.name = dbRegion.controlMesh
                mesh.password = dbRegion.controlMeshPwd
                mesh.factoryName = dbRegion.installMesh
                mesh.factoryPassword = dbRegion.installMeshPwd
//            mesh.saveOrUpdate(TelinkLightApplication.getInstance())
                application.setupMesh(mesh)
                SharedPreferencesUtils.saveCurrentUseRegion(dbRegion.id!!)
                return
            }
        }

        private fun setupMeshCreat(account: String) {
            val account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance()
                    , Constant.DB_NAME_KEY, "dadou")
            val dbRegio = DbRegion()
            dbRegio.belongAccount = account
            dbRegio.controlMesh = account
            dbRegio.controlMeshPwd = account
            dbRegio.installMesh = Constant.DEFAULT_MESH_FACTORY_NAME
            dbRegio.installMeshPwd = Constant.DEFAULT_MESH_FACTORY_PASSWORD
            DBUtils.saveRegion(dbRegio, false)

            val application = DeviceHelper.getApplication() as TelinkLightApplication
            val mesh = application.mesh
            mesh.name = dbRegio.controlMesh
            mesh.password = dbRegio.controlMeshPwd
            mesh.factoryName = dbRegio.installMesh
            mesh.factoryPassword = dbRegio.installMeshPwd
//        mesh.saveOrUpdate(TelinkLightApplication.getInstance())
            application.setupMesh(mesh)
        }
    }
}
