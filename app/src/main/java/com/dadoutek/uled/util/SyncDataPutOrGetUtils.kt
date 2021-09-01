package com.dadoutek.uled.util

import android.annotation.SuppressLint
import android.content.Context
import android.transition.Scene
import android.util.Log
import androidx.annotation.MainThread
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.BodyBias
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Light
import com.dadoutek.uled.model.dbModel.*
import com.dadoutek.uled.model.httpModel.*
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.network.bean.GradientBody
import com.dadoutek.uled.network.bean.SceneBody
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.google.gson.Gson
import com.mob.tools.utils.DeviceHelper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.http.Body
import java.lang.Thread.sleep

/**
 * chown 改 2021.8.27
 */
class SyncDataPutOrGetUtils {

    companion object {

        val dbLights : ArrayList<DbLight> = ArrayList() //
        val delDbLights: ArrayList<Int> = ArrayList() //
        val dbGroups : ArrayList<DbGroup> = ArrayList() //
        val delDbGroups : ArrayList<Int> = ArrayList() //
        val dbScenes : ArrayList<SceneBody> = ArrayList() //
        val delDbScenes : ArrayList<Int> = ArrayList() //
        val connectors : ArrayList<DbConnector> = ArrayList() //
        val delConnectors : ArrayList<Int> = ArrayList() //
        val switchs : ArrayList<DbSwitch> = ArrayList() //
        val delSwitchs : ArrayList<Int> = ArrayList() //
        val sensors : ArrayList<DbSensor> = ArrayList() //
        val delSensors : ArrayList<Int> = ArrayList() //
        val curtains : ArrayList<DbCurtain> = ArrayList() //
        val delCurtains : ArrayList<Int> = ArrayList() //
        val gradients : ArrayList<GradientBody> = ArrayList() //
        val delGradients : ArrayList<Int> = ArrayList() //
        private val dbUser = DBUtils.lastUser //获取最后一个用户信息
        val observableList : ArrayList<Observable<String>> by lazy { ArrayList<Observable<String>>() }  //所有的被观察者列表
        private fun addtoObservable() {
            if (dbGroups.size>0)
                GroupMdodel.batchAddOrUpdateGp2(dbGroups)?.let {
                    observableList.add(it)
                }
            if (dbLights.size>0)
                LightModel.batchAddorUpdateLight(dbLights)?.let {
                    observableList.add(it)
                }
            if (dbScenes.size>0)
                SceneModel.batchAddOrUpdateScene(dbScenes)?.let {
                    observableList.add(it)
                }
            if (connectors.size>0)
                ConnectorModel.batchAddOrUpdateConnector(connectors)?.let {
                    observableList.add(it)
                }

            if (switchs.size>0)
                SwitchMdodel.batchAddOrUpdateSwitch(switchs)?.let {
                    observableList.add(it)
                }
            if (sensors.size>0)
                SensorMdodel.batchAddOrUpdateSensor(sensors)?.let {
                    observableList.add(it)
                }
            if (curtains.size>0)
                CurtainMdodel.batchAddOrUpdateCurtain(curtains)?.let {
                    observableList.add(it)
                }
            if (gradients.size>0)
                GradientModel.batchAddOrUpdateGradient(gradients)?.let {
                    observableList.add(it)
                }
            if (delDbLights.size>0) {
                    LightModel.remove(delDbLights)?.let {
                        observableList.add(it)
                    }
            }
            if (delDbGroups.size>0) {
                GroupMdodel.remove(delDbGroups)?.let {
                    observableList.add(it)
                }
            }
            if (delDbScenes.size>0) {
                SceneModel.remove(delDbScenes)?.let {
                    observableList.add(it)
                }
            }
            if (delConnectors.size>0) {
                ConnectorModel.remove(delConnectors)?.let {
                    observableList.add(it)
                }
            }
            if (delCurtains.size>0) {
                CurtainMdodel.remove(delCurtains)?.let {
                    observableList.add(it)
                }
            }
            if (delSwitchs.size>0) {
                SwitchMdodel.remove(delSwitchs)?.let {
                    observableList.add(it)
                }
            }
            if (delSensors.size>0) {
                SensorMdodel.remove(delSensors)?.let {
                    observableList.add(it)
                }
            }
            if (delGradients.size>0) {
                GradientModel.remove(delGradients)?.let {
                    observableList.add(it)
                }
            }
        }
        /********************************同步数据之上传数据 */
        @SuppressLint("CheckResult")
        @Synchronized
        fun syncPutDataStart(context: Context, syncCallback: SyncCallback) {
            Thread {
                val dbDataChangeList = DBUtils.dataChangeAll //获取所有改变的数据

                //dbDataChangeLightList
                if (dbDataChangeList.isEmpty()) {
                    GlobalScope.launch(Dispatchers.Main) {
                        syncCallback.complete() //如果是空的，直接同步完成
                    }
                    return@Thread
                }

                //创建主线程的协程 开始同步 看来得看具体的实现，baseActivity中是直接显示一个同步的dialog
                GlobalScope.launch(Dispatchers.Main) {
                    syncCallback.start()
                }

                for (data in dbDataChangeList) { // 此方法不可取，在changed东西太多的时候会开太多线程直接挂掉
                    data.changeId ?: break
                    //群组模式 = 0，场景模式 =1 ，自定义模式= 2，非八键开关 = 3
                    sendDataToServer(data.tableName,
                            data.changeId, data.changeType, dbUser!!.token, data.id!!, data.type, data.keys ?: "", data)
                }

                addtoObservable()
                LogUtils.v("chown ----------observableList------------ ${observableList.size}")
                val observables = arrayOfNulls<Observable<String>>(observableList.size)
                observableList.toArray(observables)

                if (observables.isNotEmpty()) {
                    LogUtils.v("chown ------------observables.isNotEmpty   observables.isNotEmpty   observables.isNotEmpty")
                    observables.forEach {
                        it!!.subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread()).subscribe({
                                }, {
                                })
                    }
                    Observable.mergeArrayDelayError(*observables)
                            .subscribe(object : NetworkObserver<String?>() {// 显示是这一行中奔溃
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
//                    LogUtils.v("chown ------------else Complete------==============--=")

                    GlobalScope.launch(Dispatchers.Main) {
                        syncCallback.complete()
                    }
                }
            }.start()
            dbLights.clear()
            dbGroups.clear()
            dbScenes.clear()
            connectors.clear()
            switchs.clear()
            sensors.clear()
            curtains.clear()
            gradients.clear()
            observableList.clear()
            delDbLights.clear()
            delDbGroups.clear()
            delDbScenes.clear()
            delCurtains.clear()
            delGradients.clear()
            delSensors.clear()
            delConnectors.clear()
            delSwitchs.clear()
        }

        private fun sendDataToServer(tableName: String, changeId: Long, type: String,
                                     token: String, id: Long, switchType: Int, keys: String, data: DbDataChange) {
            if (changeId != null) {
//                LogUtils.v("zcl", "zcl**tableName****$tableName")
                when (tableName) {
                    "DB_GROUP" -> {
                        val group = DBUtils.getGroupByID(changeId)
                        when (type) {
                            Constant.DB_ADD -> {// 添加token lastReginID
                                group?.let {
                                    dbGroups.add(group)
//                                    GradientModel.update(token, changeId.toInt(), bodyGradient, id)
                                }
                            }
                            Constant.DB_DELETE -> {
                                LogUtils.v("chown --- delete group $group")
//                                delDbGroups.add(data)
                                delDbGroups.add(changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                group?.let {
                                    dbGroups.add(group)
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
                                GwModel.deleteGwList(gattBody)?.let {
                                    observableList.add(it)
                                }
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
                        val light = DBUtils.getLightByID(changeId)
                        when (type) {
                            Constant.DB_ADD -> {
                                light?.let {
                                    LogUtils.v("chown ---- addlight!--- $light")
                                    dbLights.add(light)
                                }
                            }
                            Constant.DB_DELETE -> {
//                                delDbLights.add(data)
                                LogUtils.v("chown -- dblight ${id.toInt()} and ${changeId.toInt()}")
                                delDbLights.add(changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                light?.let {
                                    dbLights.add(light)
                                }
                            }
                        }
                    }
                    "DB_CONNECTOR" -> {
                        val connector = DBUtils.getConnectorByID(changeId)
                        when (type) {
                            Constant.DB_ADD -> {
                                connector?.let {
                                    connectors.add(connector)
                                }
                            }
                            Constant.DB_DELETE -> {
//                                delConnectors.add(data)
                                delConnectors.add(changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                connector?.let {
                                    connectors.add(connector)
                                }
                            }
                        }
                    }
                    "DB_SWITCH", "DB_EIGHT_SWITCH" -> {
                        val switch = DBUtils.getSwitchByID(changeId)
                        when (type) {
                            Constant.DB_ADD -> {
                                switch?.let {
                                    switchs.add(switch)
                                    }
                            }
                            Constant.DB_DELETE -> {
//                                delSwitchs.add(data)
                                delSwitchs.add(changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                switch?.let {
                                    switchs.add(switch)
                                }
                            }
                        }
                    }

                    "DB_SENSOR" -> {
                        val sensor = DBUtils.getSensorByID(changeId)

                        when (type) {
                            Constant.DB_ADD -> {
                                sensor?.let {
                                    sensors.add(sensor)
                                    }
                            }
                            Constant.DB_DELETE -> {
//                                delSensors.add(data)
                                delSensors.add(changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                sensor?.let {
                                    sensors.add(sensor)
                                }
                            }
                        }
                    }
                    "DB_CURTAIN" -> {
                        val curtain = DBUtils.getCurtainByID(changeId)
                        when (type) {
                            Constant.DB_ADD -> {
                                curtain?.let {
                                    curtains.add(curtain)
                                }
                            }
                            Constant.DB_DELETE -> {
//                                delCurtains.add(data)
                                delCurtains.add(changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                if (curtain != null) {
                                    curtains.add(curtain)
                                }
                            }
                        }
                    }
                    "DB_REGION" -> {
                        when (type) {
                            Constant.DB_ADD -> {
                                val region = DBUtils.getRegionByID(changeId)
                                RegionModel.add(token, region, id, changeId)?.let { observableList.add(it) }
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
                        var sceneBody : SceneBody? = null
                        if (scene != null && type != Constant.DB_DELETE) {
                            sceneBody = SceneBody(scene.id,scene.name,scene.actions,scene.index,scene.imgName)
                        }
                        when (type) {
                            Constant.DB_ADD -> {
                                if (sceneBody!=null)
                                    dbScenes.add(sceneBody)
                            }
                            Constant.DB_DELETE -> {
//                                delDbScenes.add(data)
                                delDbScenes.add(changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                if (sceneBody != null) {
                                    dbScenes.add(sceneBody)
                                }
                            }
                        }
                    }

                    // 动态变换 dcs
                    "DB_DIY_GRADIENT" -> {
                        val gradient = DBUtils.getGradientByID(changeId)
                        var gradientBody: GradientBody? = null
                        if (gradient != null && type != Constant.DB_DELETE) {
                            gradientBody = GradientBody(gradient.id,gradient.name,gradient.type,gradient.speed,gradient.colorNodes,gradient.index)
                        }

                        when (type) {
                            Constant.DB_ADD -> {
                                if (gradientBody != null) {
                                    gradients.add(gradientBody)
                                }
                            }
                            Constant.DB_DELETE -> {
                                val body = DbDeleteGradientBody()
                                body.idList = ArrayList()
                                body.idList.add(changeId.toInt())
                                gradients.forEach {
                                    if (it.id == id)
                                        gradients.remove(it)
                                }
//                                delGradients.add(data)
                                delGradients.add(changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                if (gradientBody != null) {
                                    gradients.add(gradientBody)
                                }
                            }
                        }
                    }

                    "DB_USER" -> {
                        val user = DBUtils.getUserByID(changeId)
                        when (type) {
                            Constant.DB_ADD -> {
                                //注册时已经添加
                            }
                            Constant.DB_DELETE -> {
                                //无用户删除操作
                            }
                            Constant.DB_UPDATE ->{
                                AccountModel.update(token, user.avatar, user.name, user.email, "oh my god!")?.let { observableList.add(it) } // Oh God!
                            }
                        }
                    }
                }
            }
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
                                if (!Constant.IS_ROUTE_MODE){
                                    syncPutDataStart(TelinkLightApplication.getApp(), syncCallbackSY)
                                    LogUtils.v("chown -- 同步数据")
                                }
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
                mesh.name = dbRegion.controlMesh
                //mesh.password = dbRegion.controlMeshPwd
                mesh.password = dbRegion.belongAccount
                mesh.factoryName = dbRegion.installMesh
                mesh.factoryPassword = dbRegion.installMeshPwd
                application.setupMesh(mesh)
                SharedPreferencesUtils.saveCurrentUseRegionID(dbRegion.id!!)
                return
            } else {
                setupMeshCreat(this.acc!!)
            }
        }

        private fun setupMeshCreat(accounts: String) {
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

/*
    private fun sendDataToServer(tableName: String, changeId: Long, type: String,
                                     token: String, id: Long, switchType: Int, keys: String): Observable<String>? {
            if (changeId != null) {
                when (tableName) {
                    "DB_GROUP" -> {
                        val group = DBUtils.getGroupByID(changeId)
                        when (type) {
                            Constant.DB_ADD -> {// 添加token lastReginID
                                return group?.let {
                                    GroupMdodel.add(/*token,*/ it, /*group.belongRegionId, */id, changeId) }!!
                            }
                            Constant.DB_DELETE -> {
                                return GroupMdodel.delete(token, changeId.toInt(), id)
                            }
                            Constant.DB_UPDATE -> {
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
                        val light = DBUtils.getLightByID(changeId)
                        when (type) {
                            Constant.DB_ADD -> {
                                return light?.let {
                                    LightModel.add(token, it, id, changeId)
                                }
                            }
                            Constant.DB_DELETE -> {
                                return LightModel.delete(token, id, changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                light?.let {
                                    return LightModel.update(token, light, id, changeId.toInt())
                                }
                            }
                        }
                    }
                    "DB_CONNECTOR" -> {
                        val connector = DBUtils.getConnectorByID(changeId)
                        when (type) {
                            Constant.DB_ADD -> {
                                return connector?.let {
                                    ConnectorModel.add(token, it, id, changeId) }
                            }
                            Constant.DB_DELETE -> {
                                return ConnectorModel.delete(token, id, changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                connector?.let {
                                    return ConnectorModel.update(token, connector, id, changeId.toInt())
                                }
                            }
                        }
                    }
                    "DB_SWITCH", "DB_EIGHT_SWITCH" -> {
                        val switch = DBUtils.getSwitchByID(changeId)
                        when (type) {
                            Constant.DB_ADD -> {
                                return switch?.let {
                                    SwitchMdodel.add(token, it, id, changeId) }
                            }
                            Constant.DB_DELETE -> {
                                return SwitchMdodel.delete(token, id, changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                switch?.let {
                                    return SwitchMdodel.update(token, switch, changeId.toInt(), id)
                                }
                            }
                        }
                    }

                    "DB_SENSOR" -> {
                        val sensor = DBUtils.getSensorByID(changeId)

                        when (type) {
                            Constant.DB_ADD -> {
                                return sensor?.let {
                                    SensorMdodel.add(token, it, id, changeId) }
                            }
                            Constant.DB_DELETE -> {
                                return SensorMdodel.delete(token, id, changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
                                sensor?.let {
                                    return SensorMdodel.update(token, sensor, changeId.toInt(), id) }
                            }
                        }
                    }
                    "DB_CURTAIN" -> {
                        val curtain = DBUtils.getCurtainByID(changeId)
                        when (type) {
                            Constant.DB_ADD -> {
                                return curtain?.let {
                                    CurtainMdodel.add(token, it, id, changeId) }
                            }
                            Constant.DB_DELETE -> {
                                return CurtainMdodel.delete(token, id, changeId.toInt())
                            }
                            Constant.DB_UPDATE -> {
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
                                //("scene_add--id==" + changeId)
                                if (bodyScene != null) {
                                    return SceneModel.add(token, bodyScene, id, changeId)
                                }
                            }
                            Constant.DB_DELETE -> {
                                return SceneModel.delete(token, changeId.toInt(), id)
                            }
                            Constant.DB_UPDATE -> {
                                if (bodyScene != null) {
                                    return SceneModel.update(token, changeId.toInt(), bodyScene, id)
                                }
                            }

                        }
                    }

                    // 动态变换 dcs
                    "DB_DIY_GRADIENT" -> {
                        val gradient = DBUtils.getGradientByID(changeId)
                        lateinit var postInfoStr: String
                        var bodyGradient: RequestBody? = null
                        if (gradient != null && type != Constant.DB_DELETE) {
                            val body = DbGradientBody()
                            val gson = Gson()
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
                            Constant.DB_UPDATE ->{
                                return AccountModel.update(token, user.avatar, user.name, user.email, "oh my god!") // Oh God!
                            }

                        }
                    }
                }
            }
            return null
        }

*/
}
