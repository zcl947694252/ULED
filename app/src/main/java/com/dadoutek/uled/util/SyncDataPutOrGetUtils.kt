package com.dadoutek.uled.util

import android.content.Context
import android.util.Log
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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers import kotlinx.coroutines.GlobalScope import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody


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
                    GlobalScope.launch(Dispatchers.Main) {
                        syncCallback.complete()
                    }
                    return@Thread
                }

                val observableList = ArrayList<Observable<String>>()
//                val observableList : MutableList<Observable<String>> = ArrayList<Observable<String>>()

                for (i in dbDataChangeList.indices) {
                    if (i == 0) {
                        GlobalScope.launch(Dispatchers.Main) {
                            syncCallback.start()
                        }
                    }
                        var observable: Observable<String>?= this.sendDataToServer(dbDataChangeList[i].tableName,
                                dbDataChangeList[i].changeId,
                                dbDataChangeList[i].changeType,
                                dbUser!!.token, dbDataChangeList[i].id!!)
                        observable?.let { observableList.add(it) }



                    if (i == dbDataChangeList.size - 1) {
//                                observableNew = observableList.get(j).mergeWith(observableList.get(j-1))
//                                observableNew=Observable.merge(observableList)
                        val observables = arrayOfNulls<Observable<String>>(observableList.size)
                        observableList.toArray(observables)

                        if(observables.isNotEmpty()){
                            Observable.mergeArrayDelayError<String>(*observables)
                                    .doFinally {
                                    }
                                    .subscribe(object : NetworkObserver<String?>() {
                                        override fun onComplete() {
                                            GlobalScope.launch(Dispatchers.Main) {
                                                syncCallback.complete()
//                                            ToastUtils.showLong(context.getString(R.string.upload_data_success))
                                            }
                                        }
                                        override fun onSubscribe(d: Disposable) {
                                        }

                                        override fun onNext(t: String) {
                                        }

                                        override fun onError(e: Throwable) {
                                            GlobalScope.launch(Dispatchers.Main) {
                                                syncCallback.error(e.cause.toString())
//                                            ToastUtils.showLong(context.getString(R.string.upload_data_success))
                                            }
                                        }
                                    })
                        }else{
                            GlobalScope.launch(Dispatchers.Main) {
                                syncCallback.complete()
//                                            ToastUtils.showLong(context.getString(R.string.upload_data_success))
                            }
                        }
                    }
                }

            }.start()
        }

        private fun sendDataToServer(tableName: String, changeId: Long, type: String,
                                     token: String, id: Long): Observable<String>? {
            var result: Observable<String>?
//            val token = "1"
            when (tableName) {
                "DB_GROUP" -> {
                    when (type) {
                        Constant.DB_ADD -> {
                            val group = DBUtils.getGroupByID(changeId)
                            return GroupMdodel.add(token, group!!, group.belongRegionId, id, changeId)!!
                        }
                        Constant.DB_DELETE -> return GroupMdodel.delete(token, changeId.toInt(), id)
                        Constant.DB_UPDATE -> {
                            val group = DBUtils.getGroupByID(changeId)
                            if(group!=null){
//                                return GroupMdodel.update(token, changeId.toInt(),
//                                        group!!.name, group.brightness, group.colorTemperature, group.color,id)
                                return GroupMdodel.add(token, group!!, group.belongRegionId, id, changeId)!!
                            }
                        }
                    }
                }
                "DB_LIGHT" -> {

                    when (type) {
                        Constant.DB_ADD -> {
                            val light = DBUtils.getLightByID(changeId)
                            return LightModel.add(token, light!!, id, changeId)
                        }
                        Constant.DB_DELETE -> {
                            return LightModel.delete(token,
                                    id, changeId.toInt())
                        }
                        Constant.DB_UPDATE -> {
                            val light = DBUtils.getLightByID(changeId)
                            if(light!=null){
                                return LightModel.update(token,light,
                                        id, changeId.toInt())
                            }
                        }
                    }
                }

                "DB_CONNECTOR"->{
                    when (type) {
                        Constant.DB_ADD -> {
                            val light = DBUtils.getConnectorByID(changeId)
                            return ConnectorModel.add(token, light!!, id, changeId)
                        }
                        Constant.DB_DELETE -> {
                            return ConnectorModel.delete(token,
                                    id, changeId.toInt())
                        }
                        Constant.DB_UPDATE -> {
                            val light = DBUtils.getConnectorByID(changeId)
                            if(light!=null){
                                return ConnectorModel.update(token,light,
                                        id, changeId.toInt())
                            }
                        }
                    }
                }

                "DB_SWITCH" -> {
                    when (type) {
                        Constant.DB_ADD -> {
                            val switch = DBUtils.getSwitchByID(changeId)
                            return SwitchMdodel.add(token, switch!!, id, changeId)
                        }
                        Constant.DB_DELETE -> {
                            return SwitchMdodel.delete(token,
                                    id, changeId.toInt())
                        }
                        Constant.DB_UPDATE -> {
                            val switch = DBUtils.getSwitchByID(changeId)
                            if(switch!=null){
                                return SwitchMdodel.update(token,
                                        switch,changeId.toInt(),id)
                            }
                        }
                    }
                }
                "DB_SENSOR" -> {
                    when (type) {
                        Constant.DB_ADD -> {
                            val sensor = DBUtils.getSensorByID(changeId)
                            return SensorMdodel.add(token, sensor!!, id, changeId)
                        }
                        Constant.DB_DELETE -> {
                            return SensorMdodel.delete(token,
                                    id, changeId.toInt())
                        }
                        Constant.DB_UPDATE -> {
                            val sensor = DBUtils.getSensorByID(changeId)
                            if(sensor!=null){
                                return SensorMdodel.update(token,
                                        sensor,changeId.toInt(),id)
                            }
                        }
                    }
                }
                "DB_CURTAIN" -> {
                    when (type) {
                        Constant.DB_ADD -> {
                            val curtain = DBUtils.getCurtainByID(changeId)
                            return CurtainMdodel.add(token, curtain!!, id, changeId)
                        }
                        Constant.DB_DELETE -> {
                            return CurtainMdodel.delete(token,
                                    id, changeId.toInt())
                        }
                        Constant.DB_UPDATE -> {
                            val curtain = DBUtils.getCurtainByID(changeId)
                            if(curtain!=null){
                                return CurtainMdodel.update(token,
                                        curtain,changeId.toInt(),id)
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
                            LogUtils.d("scene_add--id=="+changeId)
                            if(bodyGradient!=null){
                                return GradientModel.add(token, bodyGradient
                                        , id, changeId)
                            }
                        }
                        Constant.DB_DELETE -> {
                            LogUtils.d("scene_delete--id=="+changeId)
                           val body=DbDeleteGradientBody()
                            body.idList=ArrayList()
                            body.idList.add(changeId.toInt())
                            return GradientModel.delete(token,
                                    body, id)
                        }
                        Constant.DB_UPDATE -> {
                            LogUtils.d("scene_update--id=="+changeId)
                            if(bodyGradient!=null){
                                return GradientModel.update(token, changeId.toInt(), bodyGradient, id)
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

        private var acc: String?=null

        private fun startGet(token: String, accountNow: String, syncCallBack: SyncCallback) {

            NetworkFactory.getApi()
                    .getRegionList(token)
                    .compose(NetworkTransformer())
                    .flatMap {
                        Log.d("itSize", it.size.toString())
                        acc=accountNow
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
                                .getSwitchList(token)
                                .compose(NetworkTransformer())
                    }
                    .flatMap {
                        for (item in it) {
                            DBUtils.saveSwitch(item, true)
                        }
                        NetworkFactory.getApi()
                                .getSensorList(token)
                                .compose(NetworkTransformer())
                    }
                    .flatMap {
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
                                val k = i + 1
                                DBUtils.saveColorNodes(item.colorNodes[i], k.toLong(), item.id)
                            }
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
                            GlobalScope.launch(Dispatchers.Main) {
                                syncCallBack.complete()
                            }
                        }

                        override fun onError(e: Throwable) {
                            super.onError(e)
                            GlobalScope.launch(Dispatchers.Main) {
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
            }else{
                setupMeshCreat(this!!.acc!!)
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
