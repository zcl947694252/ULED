package com.dadoutek.uled.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.*
import com.dadoutek.uled.model.httpModel.*
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.google.gson.Gson
import com.mob.tools.utils.DeviceHelper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody


class SyncDataPutOrGetUtils {

    companion object {

        /********************************同步数据之上传数据 */
        @Synchronized
        fun syncPutDataStart(context: Context, syncCallback: SyncCallback) {
            Thread {
                val dbDataChangeList = DBUtils.dataChangeAll
                val dbUser = DBUtils.lastUser
                if (dbDataChangeList.isEmpty()) {
                    GlobalScope.launch(Dispatchers.Main) {
                        syncCallback.complete()
                    }
                    return@Thread
                }

                val observableList = ArrayList<Observable<String>>()

                GlobalScope.launch(Dispatchers.Main) {
                    syncCallback.start()
                }
//                LogUtils.v("zcl得删除添加表$dbDataChangeList")

                for (data in dbDataChangeList) {
                    data.changeId ?: break
                    //群组模式 = 0，场景模式 =1 ，自定义模式= 2，非八键开关 = 3
                    data?.let {
                        var observable: Observable<String>? = this.sendDataToServer(data.tableName,
                                data.changeId, data.changeType, dbUser!!.token, data.id!!, data.type, data.keys ?: "")
                        observable?.let { observableList.add(it) }
                    }
                }

                val observables = arrayOfNulls<Observable<String>>(observableList.size)
                observableList.toArray(observables)

                if (observables.isNotEmpty()) {
                    observables.forEach {
                        it!!.subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread()).subscribe({
                                    LogUtils.v("zcl合并----------正确-$it-------")
                                }, {
                                    LogUtils.v("zcl合并-----------错误-------$it")
                                })
                    }
                    Observable.mergeArrayDelayError(*observables)
                            .subscribe(object : NetworkObserver<String?>() {
                                override fun onComplete() {
                                    GlobalScope.launch(Dispatchers.Main) {
                                        syncCallback.complete()
                                    }
                                }

                                override fun onSubscribe(d: Disposable) {}
                                override fun onNext(t: String) {}
                                override fun onError(e: Throwable) {
                                    LogUtils.d(e)
                                    GlobalScope.launch(Dispatchers.Main) {
                                        if (e.message != "")
                                            syncCallback.error(context.getString(R.string.upload_data_failed))
                                    }
                                }
                            })
                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        syncCallback.complete()
                    }
                }
            }.start()
        }

        private fun sendDataToServer(tableName: String, changeId: Long, type: String,
                                     token: String, id: Long, switchType: Int, keys: String): Observable<String>? {
            if (changeId != null) {
//                LogUtils.v("zcl", "zcl**tableName****$tableName")
                when (tableName) {
                    "DB_GROUP" -> {
                        when (type) {
                            Constant.DB_ADD -> {// 添加token lastReginID
                                val group = DBUtils.getGroupByID(changeId)
                                return group?.let { GroupMdodel.add(/*token,*/ it, /*group.belongRegionId, */id, changeId) }!!
                            }
                            Constant.DB_DELETE -> return GroupMdodel.delete(token, changeId.toInt(), id)
                            Constant.DB_UPDATE -> {
                                val group = DBUtils.getGroupByID(changeId)
                                return group?.let {
                                    return GroupMdodel.add(/*token,*/ group, /*group.belongRegionId, */id, changeId)!!
                                }
                            }
                        }
                    }
                    "DB_GATEWAY" -> {
                        when (type) {
                            /*   Constant.DB_ADD -> {
                                   val gw = DBUtils.getGatewayByID(changeId)
                                   return gw?.let { GwModel.add(it) }
                               }*/
                            Constant.DB_DELETE -> {
                                val list = arrayListOf(changeId.toInt())
                                val gattBody = GwGattBody()
                                gattBody.idList = list
                                return GwModel.deleteGwList(gattBody)
                            }
                            /* Constant.DB_UPDATE -> {
                                 val gw = DBUtils.getGatewayByID(changeId)
                                 gw?.let {
                                     return GwModel.add(gw)
                                 }
                             }*/
                        }
                    }
                    "DB_LIGHT" -> {
                        when (type) {
                            Constant.DB_ADD -> {
                                val light = DBUtils.getLightByID(changeId)
                                LogUtils.v("zcl-----------调节开关添加灯-------$light")
                                return light?.let { LightModel.add(token, it, id, changeId) }
                            }
                            Constant.DB_DELETE -> {
                                return LightModel.delete(token, id, changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                val light = DBUtils.getLightByID(changeId)
                                LogUtils.v("zcl-----------调节开关更新灯-------$light")

                                light?.let {
                                    return LightModel.update(token, light, id, changeId.toInt())
                                }
                            }
                        }
                    }
                    "DB_CONNECTOR" -> {
                        when (type) {
                            Constant.DB_ADD -> {
                                val light = DBUtils.getConnectorByID(changeId)
                                return light?.let { ConnectorModel.add(token, it, id, changeId) }
                            }
                            Constant.DB_DELETE -> {
                                return ConnectorModel.delete(token, id, changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                val light = DBUtils.getConnectorByID(changeId)
                                light?.let {
                                    return ConnectorModel.update(token, light, id, changeId.toInt())
                                }
                            }
                        }
                    }
                    "DB_SWITCH", "DB_EIGHT_SWITCH" -> {
                        when (type) {
                            Constant.DB_ADD -> {
                                val switch = DBUtils.getSwitchByID(changeId)
                                return switch?.let { SwitchMdodel.add(token, it, id, changeId) }
                            }
                            Constant.DB_DELETE -> {
                                return SwitchMdodel.delete(token, id, changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                val switch = DBUtils.getSwitchByID(changeId)
                                switch?.let {
                                    return SwitchMdodel.update(token, switch, changeId.toInt(), id)
                                }
                            }
                        }
                    }

                    /*     "DB_EIGHT_SWITCH" -> {
                             when (type) {
                                 Constant.DB_ADD -> {
                                     val switch = DBUtils.getEightSwitchByID(changeId)
                                     return switch?.let { EightSwitchMdodel.add8k(it, changeId) }
                                 }
                                 Constant.DB_DELETE -> {
                                     return EightSwitchMdodel.delete(id, changeId)
                                 }
                                 Constant.DB_UPDATE -> {
                                     val switch = DBUtils.getEightSwitchByID(changeId)
                                     switch?.let {
                                         return EightSwitchMdodel.update8k(switch, changeId)
                                     }
                                 }
                             }
                         }*/

                    "DB_SENSOR" -> {
                        when (type) {
                            Constant.DB_ADD -> {
                                val sensor = DBUtils.getSensorByID(changeId)
                                LogUtils.v("zcl-----------上传数据传感器-------$sensor")
                                return sensor?.let { SensorMdodel.add(token, it, id, changeId) }
                            }
                            Constant.DB_DELETE -> {
                                return SensorMdodel.delete(token, id, changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                val sensor = DBUtils.getSensorByID(changeId)
                                LogUtils.v("zcl-----------上传数据更新传感器-------$sensor")
                                sensor?.let { return SensorMdodel.update(token, sensor, changeId.toInt(), id) }
                            }
                        }
                    }
                    "DB_CURTAIN" -> {
                        when (type) {
                            Constant.DB_ADD -> {
                                val curtain = DBUtils.getCurtainByID(changeId)
                                return curtain?.let { CurtainMdodel.add(token, it, id, changeId) }
                            }
                            Constant.DB_DELETE -> {
                                return CurtainMdodel.delete(token, id, changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                val curtain = DBUtils.getCurtainByID(changeId)
                                if (curtain != null) {
                                    return CurtainMdodel.update(token, curtain, changeId.toInt(), id)
                                }
                            }
                        }
                    }
                    "DB_REGION" -> {
                        when (type) {
                            Constant.DB_ADD -> {
                                val region = DBUtils.getRegionByID(changeId)
                                return RegionModel.add(token, region, id, changeId)
                            }
                            // Constant.DB_DELETE -> return RegionModel.delete(token, changeId.toInt(), id)
                            /*   Constant.DB_UPDATE -> {
                                   val region = DBUtils.getRegionByID(changeId)
                                   return RegionModel.update(changeId.toInt(), region, id)
                               }*/
                        }
                    }
                    "DB_SCENE" -> {
                        val scene = DBUtils.getSceneByID(changeId)

                        lateinit var postInfoStr: String
                        var bodyScene: RequestBody? = null
                        if (scene != null && type != Constant.DB_DELETE) {
                            val body = DbSceneBody()
                            val gson = Gson()
                            body.name = scene.name
                            body.belongRegionId = scene.belongRegionId
                            body.imgName = scene.imgName
                            body.actions = DBUtils.getActionsBySceneId(changeId)
                            postInfoStr = gson.toJson(body)

                            bodyScene = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), postInfoStr)
                        }

                        when (type) {
                            Constant.DB_ADD -> {
                                val scene = DBUtils.getSceneByID(changeId)
                                //("scene_add--id==" + changeId)
                                if (bodyScene != null) {
                                    return SceneModel.add(token, bodyScene, id, changeId)
                                }
                            }
                            Constant.DB_DELETE -> {
                                //("scene_delete--id==" + changeId)
                                return SceneModel.delete(token, changeId.toInt(), id)
                            }
                            Constant.DB_UPDATE -> {
                                //("scene_update--id==" + changeId)
                                if (bodyScene != null) {
                                    return SceneModel.update(token, changeId.toInt(), bodyScene, id)
                                }
                            }

                        }
                    }

                    "DB_DIY_GRADIENT" -> {
                        val gradient = DBUtils.getGradientByID(changeId)
                        lateinit var postInfoStr: String
                        var bodyGradient: RequestBody? = null
                        if (gradient != null && type != Constant.DB_DELETE) {
                            val body: DbGradientBody = DbGradientBody()
                            val gson: Gson = Gson()
                            body.name = gradient.name
                            body.type = gradient.type
                            body.speed = gradient.speed
                            body.belongRegionId = gradient.belongRegionId
                            body.colorNodes = DBUtils.getColorNodeListByDynamicModeId(changeId)
                            postInfoStr = gson.toJson(body)

                            bodyGradient = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), postInfoStr)
                        }

                        when (type) {
                            Constant.DB_ADD -> {
                                val node = DBUtils.getColorNodeListByDynamicModeId(changeId)
                                if (bodyGradient != null) {
                                    return GradientModel.add(token, bodyGradient, id, changeId)
                                }
                            }
                            Constant.DB_DELETE -> {
                                val body = DbDeleteGradientBody()
                                body.idList = ArrayList()
                                body.idList.add(changeId.toInt())
                                return GradientModel.delete(token, body, id)
                            }
                            Constant.DB_UPDATE -> {
                                if (bodyGradient != null) {
                                    return GradientModel.update(token, changeId.toInt(), bodyGradient, id)
                                }
                            }

                        }
                    }

                    "DB_USER" -> {
                        val user = DBUtils.getUserByID(changeId)
                        if (user == null) {
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
            }
            return null
        }

        /********************************同步数据之下拉数据 */
        @Synchronized
        fun syncGetDataStart(dbUser: DbUser, syncCallBack: SyncCallback) {
            val token = dbUser.token
            DBUtils.saveUser(dbUser)
            // DBUtils.deleteLocalData()
            startGet(token, dbUser.account, syncCallBack)
        }

        private var acc: String? = null

        @SuppressLint("CheckResult")
        private fun startGet(token: String, accountNow: String, syncCallBack: SyncCallback) {
            NetworkFactory.getApi()
                    .getOldRegionList(token)
                    .compose(NetworkTransformer())
                    .flatMap {
                        Log.d("itSize", it.size.toString())
                        acc = accountNow
                        for (item in it) {
                            DBUtils.saveRegion(item, true)
                        }
                        if (it.size != 0) {
                            setupMesh()
                            SharedPreferencesHelper.putString(TelinkLightApplication.getApp(),
                                    Constant.USER_TYPE, Constant.USER_TYPE_NEW)
                        } else {
                            setupMeshCreat(accountNow)
                        }

                        NetworkFactory.getApi()
                                .getRegionInfo(DBUtils.lastUser?.last_authorizer_user_id, DBUtils.lastUser?.last_region_id)
                                .compose(NetworkTransformer())
                    }
                    .flatMap {
                        //保存最后的区域信息到application
                        val application = DeviceHelper.getApplication() as TelinkLightApplication
                        val mesh = application.mesh
                        mesh.name = it.controlMesh
                        mesh.password = it.controlMeshPwd
                        mesh.factoryName = it.installMesh
                        mesh.factoryPassword = it.installMeshPwd

                        DBUtils.lastUser?.controlMeshName = it.controlMesh
                        DBUtils.lastUser?.controlMeshPwd = it.controlMeshPwd


                        SharedPreferencesUtils.saveCurrentUseRegionID(it.id)
                        application.setupMesh(mesh)
                        LogUtils.v("zcl", "zcl下拉数据更新******mesh信息" + DBUtils.lastUser + "------------------" + mesh)
                        DBUtils.saveUser(DBUtils.lastUser!!)
                        LogUtils.v("zcl", "zcl下拉数据更新******mesh信息" + DBUtils.getAllUser().size + "------------------" + DBUtils.getAllUser())
                        NetworkFactory.getApi()
                                .getLightList(token)
                                .compose(NetworkTransformer())
                    }
                    .flatMap {
                        for (item in it) {
                            DBUtils.saveLight(item, true)//一定不能设置成true否则会造成数据过大oom
                            if (item.productUUID == 6)
                                LogUtils.v("zcl------调节开关拉取新数据----${item.color shr 24}------$item--")
                        }
                        NetworkFactory.getApi()
                                .gwList
                                .compose(NetworkTransformer())
                    }.flatMap {
                        for (item in it)
                            DBUtils.saveGateWay(item, true)

                        NetworkFactory.getApi().routerList.compose(NetworkTransformer())
                    }
                    .flatMap {
                        for (item in it)
                            DBUtils.saveRouter(item, true)
                        //DBUtils.saveGateWay(item, true)
                        NetworkFactory.getApi()
                                .getSwitchList(token)
                                .compose(NetworkTransformer())
                    }.flatMap {
                        for (item in it) {
                            DBUtils.saveSwitch(item, true)
                        }
                        NetworkFactory.getApi()
                                .getSensorList(token)
                                .compose(NetworkTransformer())
                    }
                    .flatMap {
                        LogUtils.v("zcl-----------服务器下拉传感器-------$it")
                        for (item in it) {
                            DBUtils.saveSensor(item, true)
                        }
                        NetworkFactory.getApi()
                                .getRelyList(token)
                                .compose(NetworkTransformer())
                    }
                    .flatMap {
                        for (item in it) {
                            DBUtils.saveConnector(item, true)
                        }
                        NetworkFactory.getApi()
                                .getCurtainList(token)
                                .compose(NetworkTransformer())
                    }
                    .flatMap {
                        for (item in it) {
                            DBUtils.saveCurtain(item, true)
                        }
                        NetworkFactory.getApi()
                                .getGradientList(token)
                                .compose(NetworkTransformer())
                    }
                    .flatMap {
                        for (item in it) {
                            DBUtils.saveGradient(item, true)
                            for (i in item.colorNodes.indices) {
                                //("是不是空的"+item.id)
                                if (i < 8) {
                                    val k = i + 1
                                    DBUtils.saveColorNodes(item.colorNodes[i], k.toLong(), item.id)
                                }
                            }
                        }
                        NetworkFactory.getApi()
                                .getGroupList(token)
                                .compose(NetworkTransformer())
                    }.flatMap {
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
                            DBUtils.deleteSceneActionsList(DBUtils.getActionsBySceneId(item?.id!!))
                            for (i in item.actions.indices) {
                                //("是不是空的2"+item.id)
                                DBUtils.saveSceneActions(item.actions[i], item.id)
                            }
                        }
                    }
                    .observeOn(AndroidSchedulers.mainThread())!!.subscribe(
                            {
                                //登录后同步数据完成再上传一次数据
                                if (!Constant.IS_ROUTE_MODE)
                                    syncPutDataStart(TelinkLightApplication.getApp(), syncCallbackSY)
                                SharedPreferencesUtils.saveCurrentUserList(accountNow)
                                GlobalScope.launch(Dispatchers.Main) {
                                    syncCallBack.complete()
                                }
                            }, {
                        GlobalScope.launch(Dispatchers.Main) {
                            it ?: return@launch
                            syncCallBack.error(it.message ?: "")
                            ToastUtils.showLong(it.message ?: "")
                        }
                    }
                    )
        }


        private var syncCallbackSY: SyncCallback = object : SyncCallback {

            override fun start() {

            }

            override fun complete() {
                //("putSuccess:" + "上传成功")
            }

            override fun error(msg: String) {
                //("GetDataError:" + msg)
            }

        }

        private fun setupMesh() {
            val regionList = DBUtils.regionAll
            SharedPreferencesUtils.getLastUser()
            //数据库有区域数据直接加载
            if (regionList.isNotEmpty()) {
//            val usedRegionID=SharedPreferencesUtils.getCurrentUseRegionId()
                val dbRegion = DBUtils.lastRegion
                val application = DeviceHelper.getApplication() as TelinkLightApplication
                val mesh = application.mesh
                mesh.name = dbRegion?.controlMesh
                //mesh.password = dbRegion.controlMeshPwd
                mesh.password = dbRegion?.belongAccount
                mesh.factoryName = dbRegion?.installMesh
                mesh.factoryPassword = dbRegion?.installMeshPwd
                application.setupMesh(mesh)
                SharedPreferencesUtils.saveCurrentUseRegionID(dbRegion?.id!!)
                return
            } else {
                setupMeshCreat(this!!.acc!!)
            }
        }

        private fun setupMeshCreat(account: String) {
            val account = SharedPreferencesHelper.getString(TelinkLightApplication.getApp()
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
            application.setupMesh(mesh)
        }
    }
}
