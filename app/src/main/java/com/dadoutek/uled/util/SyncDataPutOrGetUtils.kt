package com.dadoutek.uled.util

import android.content.Context
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.TelinkLightApplication
import com.dadoutek.uled.intf.NetworkFactory
import com.dadoutek.uled.intf.NetworkObserver
import com.dadoutek.uled.intf.NetworkTransformer
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.HttpModel.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mob.tools.utils.DeviceHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class SyncDataPutOrGetUtils {
    companion object {

        /********************************同步数据之上传数据 */

        fun syncPutDataStart(context: Context) {

            Thread {
                val dbDataChangeList = DBUtils.getDataChangeAll()

                val dbUser = DBUtils.getLastUser()

                for (i in dbDataChangeList.indices) {
                    getLocalData(dbDataChangeList[i].tableName,
                            dbDataChangeList[i].changeId,
                            dbDataChangeList[i].changeType,
                            dbUser.token, dbDataChangeList[i].id!!)
                }
            }.start()
        }

        @Synchronized
        private fun getLocalData(tableName: String, changeId: Long?, type: String, token: String, id: Long) {
            when (tableName) {
                "DB_GROUP" -> {
                    val group = DBUtils.getGroupByID(changeId!!)
                    when (type) {
                        Constant.DB_ADD -> GroupMdodel.add(token, group.meshAddr, group.name,
                                group.brightness, group.colorTemperature, group.belongRegionId, id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                            }
                        })
                        Constant.DB_DELETE -> GroupMdodel.delete(token, changeId.toInt(), id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                            }
                        })
                        Constant.DB_UPDATE -> GroupMdodel.update(token, changeId.toInt(), group.name, group.brightness, group.colorTemperature, id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                            }
                        })
                    }
                }
                "DB_LIGHT" -> {
                    val light = DBUtils.getLightByID(changeId!!)
                    when (type) {
                        Constant.DB_ADD -> LightModel.add(token, light.meshAddr, light.name,
                                light.brightness, light.colorTemperature, light.macAddr,
                                light.meshUUID, light.productUUID, light.belongGroupId!!.toInt(),
                                id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {

                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                            }
                        })
                        Constant.DB_DELETE -> LightModel.delete(token,
                                changeId.toInt(), id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                            }
                        })
                        Constant.DB_UPDATE -> LightModel.update(token, changeId.toInt(),
                                light.name, light.brightness,
                                light.colorTemperature, light.belongGroupId!!.toInt(),
                                id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                            }
                        })
                    }
                }
                "DB_REGION" -> {
                    val region = DBUtils.getRegionByID(changeId!!)
                    when (type) {
                        Constant.DB_ADD -> RegionModel.add(token, region.controlMesh,
                                region.controlMeshPwd, region.installMesh,
                                region.installMeshPwd, id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                            }
                        })
                        Constant.DB_DELETE -> RegionModel.delete(token, changeId.toInt(),
                                id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                            }
                        })
                        Constant.DB_UPDATE -> RegionModel.update(token,
                                changeId.toInt(), region.controlMesh, region.controlMeshPwd,
                                region.installMesh, region.installMeshPwd, id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                            }
                        })
                    }
                }
                "DB_SCENE" -> {
                    val scene = DBUtils.getSceneByID(changeId!!)
                    val jsonArray = JsonArray()
                    val listActions = scene.actions
                    for (i in listActions.indices) {
                        val `object` = JsonObject()
                        `object`.addProperty("groupAddr",
                                scene.actions[i].groupAddr)
                        `object`.addProperty("brightness",
                                scene.actions[i].brightness)
                        `object`.addProperty("colorTemperature",
                                scene.actions[i].colorTemperature)
                        jsonArray.add(`object`)
                    }
                    when (type) {
                        Constant.DB_ADD -> SceneModel.add(token, scene.name, jsonArray,
                                scene.belongRegionId!!.toInt(), id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                            }
                        })
                        Constant.DB_DELETE -> SceneModel.delete(token,
                                changeId.toInt(), id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                            }
                        })
                        Constant.DB_UPDATE -> SceneModel.update(token, changeId.toInt(),
                                scene.name, jsonArray, id)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
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
                    when (type) {
                        Constant.DB_ADD -> {
                        }
                        Constant.DB_DELETE -> {
                        }
                        Constant.DB_UPDATE ->
                            AccountModel.update(token, user.avatar, user.name,
                                    user.email, "oh my god!")!!.subscribe(object : NetworkObserver<String>() {
                                override fun onNext(t: String) {
                                }

                                override fun onError(e: Throwable) {
                                    super.onError(e)
                                }
                            })
                    }//注册时已经添加
                    //无用户删除操作
                }
            }
        }
    }
}
