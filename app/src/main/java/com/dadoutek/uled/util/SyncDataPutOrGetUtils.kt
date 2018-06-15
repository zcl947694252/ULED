package com.dadoutek.uled.util

import android.app.Dialog
import android.content.Context
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.NetworkObserver
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbSceneBody
import com.dadoutek.uled.model.HttpModel.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MediaType
import okhttp3.RequestBody


class SyncDataPutOrGetUtils {
    companion object {

        /********************************同步数据之上传数据 */

        fun syncPutDataStart(context: Context) {

            Thread {
                val dbDataChangeList = DBUtils.getDataChangeAll()

                val dbUser = DBUtils.getLastUser()

//                val loadDialog = Dialog(context,
//                        R.style.FullHeightDialog)

                for (i in dbDataChangeList.indices) {
//                    DialogUtils.showLoadingDialog(context.getString(R.string.tip_start_sync), context, loadDialog)
                    getLocalData(dbDataChangeList[i].tableName,
                            dbDataChangeList[i].changeId,
                            dbDataChangeList[i].changeType,
                            dbUser.token, dbDataChangeList[i].id!!)
                    if (i == dbDataChangeList.size - 1) {
                        ToastUtils.showLong(context.getString(R.string.tip_completed_sync))
//                        DialogUtils.hideLoadingDialog(loadDialog)
                    }
                }
            }.start()
        }

        @Synchronized
        private fun getLocalData(tableName: String, changeId: Long?, type: String,
                                 token: String, id: Long) {
            when (tableName) {
                "DB_GROUP" -> {
                    val group = DBUtils.getGroupByID(changeId!!)
                    when (type) {
                        Constant.DB_ADD -> GroupMdodel.add(token, group.meshAddr, group.name,
                                group.brightness, group.colorTemperature,
                                group.belongRegionId, id,changeId)!!.subscribe(object : NetworkObserver<String>() {
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
                        Constant.DB_UPDATE -> GroupMdodel.update(token, changeId.toInt(),
                                group.name, group.brightness, group.colorTemperature, id,group.id)!!.
                                subscribe(object : NetworkObserver<String>() {
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
                                id, changeId)!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {

                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                            }
                        })
                        Constant.DB_DELETE -> LightModel.delete(token,
                                 id, light.id.toInt())!!.subscribe(object : NetworkObserver<String>() {
                            override fun onNext(t: String) {
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                            }
                        })
                        Constant.DB_UPDATE -> LightModel.update(token,
                                light.name, light.brightness,
                                light.colorTemperature, light.belongGroupId!!.toInt(),
                                id,light.id.toInt())!!.subscribe(object : NetworkObserver<String>() {
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
                                region.installMeshPwd, id,changeId)!!.subscribe(object : NetworkObserver<String>() {
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
                    var body : DbSceneBody = DbSceneBody()
                    var gson : Gson = Gson()
                    body.name=scene.name
                    body.belongRegionId=scene.belongRegionId
                    body.actions=scene.actions

                    val postInfoStr = gson.toJson(body)

                    val bodyScene = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), postInfoStr)

                    when (type) {
                        Constant.DB_ADD -> SceneModel.add(token, bodyScene
                                , id,changeId)!!.subscribe(object : NetworkObserver<String>() {
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
                        Constant.DB_UPDATE -> SceneModel.update(token, changeId.toInt(), bodyScene, id)!!.subscribe(object : NetworkObserver<String>() {
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
                                }
                            })
                    }
                }
            }
        }
    }
}
