package com.dadoutek.uled.util

import android.content.Context
import android.util.Log
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.TelinkLightApplication
import com.dadoutek.uled.intf.NetworkFactory
import com.dadoutek.uled.intf.NetworkObserver
import com.dadoutek.uled.intf.NetworkTransformer
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.model.HttpModel.*
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.google.gson.Gson
import com.mob.tools.utils.DeviceHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import okhttp3.MediaType
import okhttp3.RequestBody


class SyncDataPutOrGetUtils {
    companion object {

        /********************************同步数据之上传数据 */

        fun syncPutDataStart(context: Context, syncCallback: SyncCallback) {

            Thread {
                var dbDataChangeList = DBUtils.getDataChangeAll()

                val dbUser = DBUtils.getLastUser()

                if (dbDataChangeList.size == 0) {
                    ToastUtils.showLong(context.getString(R.string.tip_completed_sync))
                    launch(UI) {
                        syncCallback.complete()
                    }
                }

                for (i in dbDataChangeList.indices) {
                    if (i == 0) {
                        launch(UI) {
                            syncCallback.start()
                        }
                    }
                    getLocalData(dbDataChangeList[i].tableName,
                            dbDataChangeList[i].changeId,
                            dbDataChangeList[i].changeType,
                            dbUser.token, dbDataChangeList[i].id!!, syncCallback)
                    if (i == dbDataChangeList.size - 1) {
                        ToastUtils.showLong(context.getString(R.string.tip_completed_sync))
                        launch(UI) {
                            syncCallback.complete()
                        }
                    }
                }
            }.start()
        }

        @Synchronized
        private fun getLocalData(tableName: String, changeId: Long?, type: String,
                                 token: String, id: Long, syncCallback: SyncCallback) {
            when (tableName) {
                "DB_GROUP" -> {
                    val group = DBUtils.getGroupByID(changeId!!)
                    if (group == null && type != Constant.DELETEING) {
                        Log.d("组数据出错", "")
                        return
                    }
                    when (type) {
                        Constant.DB_ADD ->
                            GroupMdodel.add(token, group.meshAddr, group.name,
                                    group.brightness, group.colorTemperature,
                                    group.belongRegionId, id, changeId)!!.subscribe(object : NetworkObserver<String>() {
                                override fun onNext(t: String) {
                                }

                                override fun onError(e: Throwable) {
                                    super.onError(e)
                                    launch(UI) {
                                        syncCallback.error(e.message)
                                    }
                                }
                            })
                        Constant.DB_DELETE -> GroupMdodel.delete(token, changeId.toInt(), id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                                launch(UI) {
                                    syncCallback.error(e.message)
                                }
                            }
                        })
                        Constant.DB_UPDATE -> GroupMdodel.update(token, changeId.toInt(),
                                group.name, group.brightness, group.colorTemperature, id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                                launch(UI) {
                                    syncCallback.error(e.message)
                                }
                            }
                        })
                    }
                }
                "DB_LIGHT" -> {
                    val light = DBUtils.getLightByID(changeId!!)
                    if (light == null && type != Constant.DELETEING) {
                        Log.d("灯数据出错", "")
                        return
                    }
                    when (type) {
                        Constant.DB_ADD ->
                            LightModel.add(token, light.meshAddr, light.name,
                                    light.brightness, light.colorTemperature, light.macAddr,
                                    light.meshUUID, light.productUUID, light.belongGroupId!!.toInt(),
                                    id, changeId)!!.subscribe(object : NetworkObserver<String>() {
                                override fun onNext(t: String) {

                                }

                                override fun onError(e: Throwable) {
                                    super.onError(e)
                                    launch(UI) {
                                        syncCallback.error(e.message)
                                    }
                                }
                            })
                        Constant.DB_DELETE -> LightModel.delete(token,
                                id, changeId.toInt())!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                                launch(UI) {
                                    syncCallback.error(e.message)
                                }
                            }
                        })
                        Constant.DB_UPDATE -> {

                            LightModel.update(token,
                                    light.name, light.brightness,
                                    light.colorTemperature, light.belongGroupId.toInt(),
                                    id, changeId.toInt())
                                    .subscribe(object : NetworkObserver<String>() {
                                        override fun onNext(t: String) {
                                            ToastUtils.showShort(t)
                                        }

                                        override fun onError(e: Throwable) {
                                            super.onError(e)
                                            launch(UI) {
                                                syncCallback.error(e.message)
                                            }
                                        }
                                    })
                        }
                    }
                }
                "DB_REGION" -> {
                    val region = DBUtils.getRegionByID(changeId!!)
                    if (region == null && type != Constant.DELETEING) {
                        Log.d("区域数据出错", "")
                        return
                    }
                    when (type) {
                        Constant.DB_ADD -> RegionModel.add(token, region.controlMesh,
                                region.controlMeshPwd, region.installMesh,
                                region.installMeshPwd, id, changeId)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                                launch(UI) {
                                    syncCallback.error(e.message)
                                }
                            }
                        })
                        Constant.DB_DELETE -> RegionModel.delete(token, changeId.toInt(),
                                id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                                launch(UI) {
                                    syncCallback.error(e.message)
                                }
                            }
                        })
                        Constant.DB_UPDATE -> RegionModel.update(token,
                                changeId.toInt(), region.controlMesh, region.controlMeshPwd,
                                region.installMesh, region.installMeshPwd, id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                                launch(UI) {
                                    syncCallback.error(e.message)
                                }
                            }
                        })
                    }
                }
                "DB_SCENE" -> {
                    val scene = DBUtils.getSceneByID(changeId!!)

                    if (scene == null && type != Constant.DELETEING) {
                        Log.d("场景数据出错", "")
                        return
                    }

                    lateinit var postInfoStr: String
                    lateinit var bodyScene: RequestBody
                    if (scene != null) {
                        var body: DbSceneBody = DbSceneBody()
                        var gson: Gson = Gson()
                        body.name = scene.name
                        body.belongRegionId = scene.belongRegionId
                        body.actions = DBUtils.getActionsBySceneId(changeId)

                        postInfoStr = gson.toJson(body)

                        bodyScene = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), postInfoStr)
                    }

                    when (type) {
                        Constant.DB_ADD -> SceneModel.add(token, bodyScene
                                , id, changeId)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                                launch(UI) {
                                    syncCallback.error(e.message)
                                }
                            }
                        })
                        Constant.DB_DELETE -> SceneModel.delete(token,
                                changeId.toInt(), id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                                launch(UI) {
                                    syncCallback.error(e.message)
                                }
                            }
                        })
                        Constant.DB_UPDATE -> SceneModel.update(token, changeId.toInt(), bodyScene, id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                                launch(UI) {
                                    syncCallback.error(e.message)
                                }
                            }
                        })
                    }
                }
            //            case "DB_SCENE_ACTIONS":
            //                DbSceneActions actions = DBUtils.getSceneActionsByID(changeId);
            //                DbScene dbScene1=DBUtils.getSceneByID(actions.getActionId());
            //
            //                switch (type) {
            //                    case Constant.DB_ADD:
            //                        //添加场景内容不做任何操作，此操作在添加场景时执行
            //                        break;
            //                    case Constant.DB_DELETE:
            //                        //action删除时在场景表更新数据
            //                        break;
            //                    case Constant.DB_UPDATE:
            //                        SceneModel.INSTANCE.update(token,changeId.intValue(),scene1.getName(),jsonArray);
            //                        break;
            //                }
            //                break;
                "DB_USER" -> {
                    val user = DBUtils.getUserByID(changeId!!)

                    if (user == null && type != Constant.DELETEING) {
                        Log.d("用户数据出错", "")
                        return
                    }

                    when (type) {
                        Constant.DB_ADD -> {
                            //注册时已经添加
                        }
                        Constant.DB_DELETE -> {
                            //无用户删除操作
                        }
                        Constant.DB_UPDATE ->
                            AccountModel.update(token, user.avatar, user.name,
                                    user.email, "oh my god!")!!.subscribe(object : NetworkObserver<String>() {
                                override fun onNext(t: String) {
                                }

                                override fun onError(e: Throwable) {
                                    super.onError(e)
                                    launch(UI) {
                                        syncCallback.error(e.message)
                                    }
                                }
                            })
                    }
                }
            }
        }

        /********************************同步数据之下拉数据 */

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
                                var k = i + 1
                                DBUtils.saveSceneActions(item.actions.get(i), k.toLong(), item.id)
                            }
                        }
                    }
                    .observeOn(AndroidSchedulers.mainThread())!!.subscribe(
                    object : NetworkObserver<List<DbScene>>() {
                        override fun onNext(item: List<DbScene>) {
                            SharedPreferencesUtils.saveCurrentUserList(accountNow)
                            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.IS_LOGIN, true)
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

        private fun setupMesh() {
            val regionList = DBUtils.getRegionAll()

            //数据库有区域数据直接加载
            if (regionList.size != 0) {
//            val usedRegionID=SharedPreferencesUtils.getCurrentUseRegion()
                val dbRegion = DBUtils.getLastRegion()
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
            val account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(), Constant.DB_NAME_KEY, "dadou")
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
