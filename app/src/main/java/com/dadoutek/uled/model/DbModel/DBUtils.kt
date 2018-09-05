package com.dadoutek.uled.model.DbModel

import android.content.Context
import android.util.Log
import android.widget.Toast

import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.dao.DbGroupDao
import com.dadoutek.uled.dao.DbLightDao
import com.dadoutek.uled.dao.DbRegionDao
import com.dadoutek.uled.dao.DbSceneActionsDao
import com.dadoutek.uled.dao.DbSceneDao
import com.dadoutek.uled.dao.DbUserDao
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.util.SharedPreferencesUtils

import org.greenrobot.greendao.query.Query
import org.greenrobot.greendao.query.QueryBuilder

import java.util.ArrayList

import com.dadoutek.uled.model.Constant.MAX_GROUP_COUNT

/**
 * Created by hejiajun on 2018/5/18.
 */

object DBUtils {

    val lastUser: DbUser?
         get() {
            val list = DaoSessionInstance.getInstance().dbUserDao.queryBuilder().orderDesc(DbUserDao.Properties.Id).list()
            return if (list.size == 0) {
                null
            } else list[0]
        }

    val lastRegion: DbRegion
         get() {
            val list = DaoSessionInstance.getInstance().dbRegionDao.queryBuilder().orderDesc(DbRegionDao.Properties.Id).list()
            return list[0]
        }

    val allLight: List<DbLight>
         get() = DaoSessionInstance.getInstance().dbLightDao.loadAll()

    val groupList: MutableList<DbGroup>
         get() {
            val allGIndex = -1
            val qb = DaoSessionInstance.getInstance().dbGroupDao.queryBuilder()

            return qb.where(
                    DbGroupDao.Properties.BelongRegionId.eq(SharedPreferencesUtils.getCurrentUseRegion()))
                    .list()
        }

//    
//    fun getGroupListNew() : MutableList<DbGroup>{
//        val allGIndex = -1
//        val qb = DaoSessionInstance.getInstance().dbGroupDao.queryBuilder()
//
//        return qb.where(
//                DbGroupDao.Properties.BelongRegionId.eq(SharedPreferencesUtils.getCurrentUseRegion()))
//                .list()
//    }

    val sceneList: List<DbScene>
         get() {
            val allGIndex = -1
            val qb = DaoSessionInstance.getInstance().dbSceneDao.queryBuilder()

            return qb.where(
                    DbSceneDao.Properties.BelongRegionId.eq(SharedPreferencesUtils.getCurrentUseRegion()))
                    .list()
        }

    //未分组
    val groupNull: DbGroup?
         get() {
            val allGIndex = -1
            val qb = DaoSessionInstance.getInstance().dbGroupDao.queryBuilder()
            val list = qb.where(
                    DbGroupDao.Properties.BelongRegionId.eq(SharedPreferencesUtils.getCurrentUseRegion()))
                    .list()

            for (i in list.indices) {
                if (list[i].meshAddr == 0xffff) {
                    return list[i]
                }
            }
            return null
        }

    val sceneAll: List<DbScene>
         get() = DaoSessionInstance.getInstance().dbSceneDao.loadAll()

    val regionAll: List<DbRegion>
         get() = DaoSessionInstance.getInstance().dbRegionDao.loadAll()

    val dataChangeAll: List<DbDataChange>
         get() = DaoSessionInstance.getInstance().dbDataChangeDao.loadAll()

    val dataChangeAllHaveAboutLight: Boolean
         get() {
            val list = DaoSessionInstance.getInstance().dbDataChangeDao.loadAll()
            for (i in list.indices) {
                if (list[i].tableName == "DB_LIGHT") {
                    return true
                }
            }
            return false
        }

    val allGroups: List<DbGroup>
         get() = DaoSessionInstance.getInstance().dbGroupDao.queryBuilder().list()

    val deleteGroups: List<DbDeleteGroup>
         get() = DaoSessionInstance.getInstance().dbDeleteGroupDao.queryBuilder().list()

    private//去掉所有组，避免影响判断
    val groupAdress: Int
        get() {
            val list = DBUtils.groupList
            val idList = ArrayList<Int>()
            for (i in list.indices.reversed()) {
                if (list[i].meshAddr == 0xffff) {
                    list.removeAt(i)
                }
            }

            for (i in list.indices) {
                idList.add(list[i].meshAddr)
            }

            var id = 0
            for (i in 0x8001..33023) {
                if (idList.contains(i)) {
                    Log.d("sceneID", "getSceneId: " + "aaaaa")
                    continue
                } else {
                    id = i
                    Log.d("sceneID", "getSceneId: bbbbb$id")
                    break
                }
            }

            if (list.size == 0) {
                id = 0x8001
            }

            return id
        }

    /********************************************查询 */

    
    fun getActionsBySceneId(id: Long): List<DbSceneActions> {
        val query = DaoSessionInstance.getInstance().dbSceneActionsDao.queryBuilder().where(DbSceneActionsDao.Properties.BelongSceneId.eq(id)).build()
        return ArrayList(query.list())
    }

    
    fun getGroupNameByID(id: Long?): String {
        val group = DaoSessionInstance.getInstance().dbGroupDao.load(id)
        return group.name
    }

    
    fun getGroupByID(id: Long): DbGroup {
        return DaoSessionInstance.getInstance().dbGroupDao.load(id)
    }

    
    fun getLightByID(id: Long): DbLight {
        return DaoSessionInstance.getInstance().dbLightDao.load(id)
    }

    
    fun getRegionByID(id: Long): DbRegion {
        return DaoSessionInstance.getInstance().dbRegionDao.load(id)
    }

    
    fun getSceneByID(id: Long): DbScene? {
        return DaoSessionInstance.getInstance().dbSceneDao.load(id)
    }

    
    fun getSceneActionsByID(id: Long): DbSceneActions {
        return DaoSessionInstance.getInstance().dbSceneActionsDao.load(id)
    }

    
    fun getUserByID(id: Long): DbUser {
        return DaoSessionInstance.getInstance().dbUserDao.load(id)
    }

    
    fun getCurrentRegion(id: Long): DbRegion? {
        return DaoSessionInstance.getInstance().dbRegionDao.load(id)
    }

    
    fun getLightByMeshAddr(meshAddr: Int): DbLight? {
        val dbLightList = DaoSessionInstance.getInstance().dbLightDao.queryBuilder().where(DbLightDao.Properties.MeshAddr.eq(meshAddr)).list()
        return if (dbLightList.size > 0) {
            //            for(int i=0;i<dbLightList.size();i++){
            ////                Log.d("DataError", "getLightByMeshAddr: "+dbLightList.get(i).getMeshAddr()+);
            //            }
            dbLightList[0]
        } else null
    }

    
    fun getGroupByMesh(mesh: String): DbGroup {
        val dbGroup = DaoSessionInstance.getInstance().dbGroupDao.queryBuilder().where(DbGroupDao.Properties.MeshAddr.eq(mesh)).unique()
        Log.d("datasave", "getGroupByMesh: $mesh")
        return dbGroup
    }

    
    fun getLightByGroupID(id: Long): ArrayList<DbLight> {
        val query = DaoSessionInstance.getInstance().dbLightDao.queryBuilder().where(DbLightDao.Properties.BelongGroupId.eq(id)).build()
        return ArrayList(query.list())
    }

    /********************************************保存 */

    
    fun saveRegion(dbRegion: DbRegion, isFromServer: Boolean) {
        if (isFromServer) {
            val dbRegionOld = DaoSessionInstance.getInstance().dbRegionDao.queryBuilder().where(DbRegionDao.Properties.ControlMesh.eq(dbRegion.controlMesh)).unique()
            if (dbRegionOld == null) {
                DaoSessionInstance.getInstance().dbRegionDao.insert(dbRegion)
            }
        } else {
            //判断原来是否保存过这个区域
            val dbRegionOld = DaoSessionInstance.getInstance().dbRegionDao.queryBuilder().where(DbRegionDao.Properties.ControlMesh.eq(dbRegion.controlMesh)).unique()
            if (dbRegionOld == null) {//直接插入
                DaoSessionInstance.getInstance().dbRegionDao.insert(dbRegion)
                //暂时用本地保存区域
                SharedPreferencesUtils.saveCurrentUseRegion(dbRegion.id)

                recordingChange(dbRegion.id,
                        DaoSessionInstance.getInstance().dbRegionDao.tablename,
                        Constant.DB_ADD)
                //创建新区域首先创建一个所有灯的分组
                createAllLightControllerGroup()
            } else {//更新数据库
                dbRegion.id = dbRegionOld.id
                DaoSessionInstance.getInstance().dbRegionDao.update(dbRegion)
                //暂时用本地保存区域
                SharedPreferencesUtils.saveCurrentUseRegion(dbRegion.id)
                recordingChange(dbRegion.id,
                        DaoSessionInstance.getInstance().dbRegionDao.tablename,
                        Constant.DB_UPDATE)
            }
        }
    }

    
    fun insertRegion(dbRegion: DbRegion) {
        //判断原来是否保存过这个区域
        val dbRegionOld = DaoSessionInstance.getInstance().dbRegionDao.queryBuilder().where(DbRegionDao.Properties.ControlMesh.eq(dbRegion.controlMesh)).unique()
        if (dbRegionOld == null) {//直接插入
            DaoSessionInstance.getInstance().dbRegionDao.insert(dbRegion)
            //暂时用本地保存区域
            SharedPreferencesUtils.saveCurrentUseRegion(dbRegion.id)

            recordingChange(dbRegion.id,
                    DaoSessionInstance.getInstance().dbRegionDao.tablename,
                    Constant.DB_ADD)
        } else {//更新数据库
            dbRegion.id = dbRegionOld.id
            DaoSessionInstance.getInstance().dbRegionDao.update(dbRegion)
            //暂时用本地保存区域
            SharedPreferencesUtils.saveCurrentUseRegion(dbRegion.id)
            recordingChange(dbRegion.id,
                    DaoSessionInstance.getInstance().dbRegionDao.tablename,
                    Constant.DB_UPDATE)
        }
    }

    
    fun saveGroup(group: DbGroup, isFromServer: Boolean) {
        if (isFromServer) {
            DaoSessionInstance.getInstance().dbGroupDao.insertOrReplace(group)
        } else {
            DaoSessionInstance.getInstance().dbGroupDao.insertOrReplace(group)
            recordingChange(group.id,
                    DaoSessionInstance.getInstance().dbGroupDao.tablename,
                    Constant.DB_ADD)

            //本地匹配index
            val dbOldGroupList = SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(), Constant.OLD_INDEX_DATA) as? ArrayList<DbGroup>
            if (dbOldGroupList != null) {
                dbOldGroupList.add(group)
                SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(), Constant.OLD_INDEX_DATA, dbOldGroupList)
            }
        }
    }

    
    fun saveDeleteGroup(group: DbGroup) {
        val dbDeleteGroup = DbDeleteGroup()
        dbDeleteGroup.groupAress = group.meshAddr
        DaoSessionInstance.getInstance().dbDeleteGroupDao.save(dbDeleteGroup)
    }

    
    fun saveLight(light: DbLight, isFromServer: Boolean) {
        if (isFromServer) {
            DaoSessionInstance.getInstance().dbLightDao.insert(light)
        } else {
            //保存灯之前先把所有的灯都分配到当前的所有组去
            val dbGroup = groupNull
            light.belongGroupId = dbGroup?.id

            DaoSessionInstance.getInstance().dbLightDao.save(light)
            recordingChange(light.id,
                    DaoSessionInstance.getInstance().dbLightDao.tablename,
                    Constant.DB_ADD)
        }
    }

    
    fun oldToNewSaveLight(light: DbLight) {
        DaoSessionInstance.getInstance().dbLightDao.save(light)
        recordingChange(light.id,
                DaoSessionInstance.getInstance().dbLightDao.tablename,
                Constant.DB_ADD)
    }

    
    fun saveUser(dbUser: DbUser) {
        DaoSessionInstance.getInstance().dbUserDao.insertOrReplace(dbUser)
        recordingChange(dbUser.id,
                DaoSessionInstance.getInstance().dbUserDao.tablename,
                Constant.DB_ADD)
    }

    
    fun saveScene(dbScene: DbScene, isFromServer: Boolean) {
        if (isFromServer) {
            DaoSessionInstance.getInstance().dbSceneDao.insert(dbScene)
        } else {
            DaoSessionInstance.getInstance().dbSceneDao.insertOrReplace(dbScene)
            recordingChange(dbScene.id,
                    DaoSessionInstance.getInstance().dbSceneDao.tablename,
                    Constant.DB_ADD)
        }
    }

    
    fun saveSceneActions(sceneActions: DbSceneActions) {
        DaoSessionInstance.getInstance().dbSceneActionsDao.insertOrReplace(sceneActions)
        //        recordingChange(sceneActions.getId(),
        //                DaoSessionInstance.getInstance().getDbSceneActionsDao().getTablename(),
        //                Constant.DB_ADD);
    }

    
    fun saveSceneActions(sceneActions: DbSceneActions, id: Long?,
                         sceneId: Long) {
        val actions = DbSceneActions()
        val account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(), Constant.DB_NAME_KEY, "dadou")
        actions.belongSceneId = sceneId
        actions.brightness = sceneActions.brightness
        actions.colorTemperature = sceneActions.colorTemperature
        actions.groupAddr = sceneActions.groupAddr

        DaoSessionInstance.getInstance().dbSceneActionsDao.save(actions)
    }

    /********************************************更改 */

    
    fun updateGroup(group: DbGroup) {
        DaoSessionInstance.getInstance().dbGroupDao.update(group)
        recordingChange(group.id,
                DaoSessionInstance.getInstance().dbGroupDao.tablename,
                Constant.DB_UPDATE)

        //本地匹配index
        val dbOldGroupList = SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(), Constant.OLD_INDEX_DATA) as? ArrayList<DbGroup>
        if (dbOldGroupList != null) {
            for (k in dbOldGroupList.indices) {
                if (group.meshAddr == dbOldGroupList[k].meshAddr) {
                    dbOldGroupList.set(k, group)
                    SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(), Constant.OLD_INDEX_DATA, dbOldGroupList)
                    break
                }
            }
        }
    }

    
    fun updateLight(light: DbLight) {
        DaoSessionInstance.getInstance().dbLightDao.update(light)
        recordingChange(light.id,
                DaoSessionInstance.getInstance().dbLightDao.tablename,
                Constant.DB_UPDATE)
    }

    
    fun updateLightsLocal(lights: MutableList<DbLight>) {
        DaoSessionInstance.getInstance().dbLightDao.updateInTx(lights)
    }

    fun updateLightLocal(light: DbLight) {
        DaoSessionInstance.getInstance().dbLightDao.update(light)
    }


    
    fun updateScene(scene: DbScene) {
        DaoSessionInstance.getInstance().dbSceneDao.update(scene)
        recordingChange(scene.id,
                DaoSessionInstance.getInstance().dbSceneDao.tablename,
                Constant.DB_UPDATE)
    }

    
    fun updateDbActions(actions: DbSceneActions) {
        DaoSessionInstance.getInstance().dbSceneActionsDao.update(actions)
        recordingChange(actions.id,
                DaoSessionInstance.getInstance().dbSceneActionsDao.tablename,
                Constant.DB_UPDATE)
    }

    
    fun updateDbchange(change: DbDataChange) {
        DaoSessionInstance.getInstance().dbDataChangeDao.update(change)
    }

    /********************************************删除 */

    /**
     * 只删除分组，不动组里的灯
     * @param dbGroup
     */
    
    fun deleteGroupOnly(dbGroup: DbGroup) {
        //        saveDeleteGroup(dbGroup);
        DaoSessionInstance.getInstance().dbGroupDao.delete(dbGroup)
        recordingChange(dbGroup.id,
                DaoSessionInstance.getInstance().dbGroupDao.tablename,
                Constant.DB_DELETE)

        //本地匹配index
        val dbOldGroupList = SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(), Constant.OLD_INDEX_DATA) as? ArrayList<DbGroup>
        if (dbOldGroupList != null) {
            for (k in dbOldGroupList.indices) {
                if (dbGroup.meshAddr == dbOldGroupList[k].meshAddr) {
                    dbOldGroupList.removeAt(k)
                    SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(), Constant.OLD_INDEX_DATA, dbOldGroupList)
                    break
                }
            }
        }
    }

    
    fun deleteLight(dbLight: DbLight) {
        DaoSessionInstance.getInstance().dbLightDao.delete(dbLight)
        recordingChange(dbLight.id,
                DaoSessionInstance.getInstance().dbLightDao.tablename,
                Constant.DB_DELETE)
    }

    
    fun deleteScene(dbScene: DbScene) {
        DaoSessionInstance.getInstance().dbSceneDao.delete(dbScene)
        recordingChange(dbScene.id,
                DaoSessionInstance.getInstance().dbSceneDao.tablename,
                Constant.DB_DELETE)
    }

    
    fun deleteSceneActionsList(sceneActionslist: List<DbSceneActions>) {
        DaoSessionInstance.getInstance().dbSceneActionsDao.deleteInTx(sceneActionslist)
    }

    
    fun deleteAllData() {
        DaoSessionInstance.getInstance().dbUserDao.deleteAll()
        DaoSessionInstance.getInstance().dbSceneDao.deleteAll()
        DaoSessionInstance.getInstance().dbSceneActionsDao.deleteAll()
        DaoSessionInstance.getInstance().dbRegionDao.deleteAll()
        DaoSessionInstance.getInstance().dbGroupDao.deleteAll()
        DaoSessionInstance.getInstance().dbLightDao.deleteAll()
        DaoSessionInstance.getInstance().dbDataChangeDao.deleteAll()
    }

    
    fun deleteLocalData() {
        //        DaoSessionInstance.getInstance().getDbUserDao().deleteAll();
        DaoSessionInstance.getInstance().dbSceneDao.deleteAll()
        DaoSessionInstance.getInstance().dbSceneActionsDao.deleteAll()
        DaoSessionInstance.getInstance().dbRegionDao.deleteAll()
        DaoSessionInstance.getInstance().dbGroupDao.deleteAll()
        DaoSessionInstance.getInstance().dbLightDao.deleteAll()
        DaoSessionInstance.getInstance().dbDataChangeDao.deleteAll()
        SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(), Constant.OLD_INDEX_DATA, null)
    }

    
    fun deleteDbDataChange(id: Long) {
        DaoSessionInstance.getInstance().dbDataChangeDao.deleteByKey(id)
    }

    /********************************************其他 */

    
    fun addNewGroup(name: String, groups: MutableList<DbGroup>, context: Context) {
        //        if (!checkRepeat(groups, context, name) && !checkReachedTheLimit(groups)) {
        if (!checkReachedTheLimit(groups)) {
            val newMeshAdress: Int
            val group = DbGroup()
            newMeshAdress = groupAdress
            group.meshAddr = newMeshAdress
            group.name = name
            group.brightness = 100
            group.colorTemperature = 100
            group.belongRegionId = SharedPreferencesUtils.getCurrentUseRegion().toInt()//目前暂无分区 区域ID暂为0
            groups.add(group)
            //新增数据库保存
            DBUtils.saveGroup(group, false)

            recordingChange(group.id,
                    DaoSessionInstance.getInstance().dbGroupDao.tablename,
                    Constant.DB_ADD)
        }

    }

    private fun checkReachedTheLimit(groups: List<DbGroup>): Boolean {
        if (groups.size >= MAX_GROUP_COUNT) {
            ToastUtils.showLong(R.string.group_limit)
            return true
        }
        return false
    }

    //检查是否重复
    
    fun checkRepeat(groups: List<DbGroup>, context: Context, newName: String): Boolean {
        for (k in groups.indices) {
            if (groups[k].name == newName) {
                Toast.makeText(context, R.string.creat_group_fail_tip, Toast.LENGTH_LONG).show()
                return true
            }
        }
        return false
    }


    /**
     * 创建一个控制所有灯的分组
     *
     * @return group对象
     */
    
    fun createAllLightControllerGroup() {
        val groupAllLights = DbGroup()
        groupAllLights.name = TelinkLightApplication.getInstance().getString(R.string.allLight)
        groupAllLights.meshAddr = 0xFFFF
        groupAllLights.brightness = 100
        groupAllLights.colorTemperature = 100
        groupAllLights.belongRegionId = SharedPreferencesUtils.getCurrentUseRegion().toInt()
        val list = groupList
        DaoSessionInstance.getInstance().dbGroupDao.insert(groupAllLights)
        recordingChange(groupAllLights.id,
                DaoSessionInstance.getInstance().dbGroupDao.tablename,
                Constant.DB_ADD)
        //        for(int i=0;i<list.size();i++){
        //            if(list.get(i).getMeshAddr()!=0xffff){
        //                groupAllLights.setId(Long.valueOf(0));
        //                DaoSessionInstance.getInstance().getDbGroupDao().save(groupAllLights);
        //            }
        //        }
    }

    /**
     * 记录本地数据库变化
     *
     * @param changeIndex 变化的位置
     * @param changeTable 变化数据所属表
     * @param operating   所执行的操作
     */
    
    fun recordingChange(changeIndex: Long?, changeTable: String, operating: String) {
        val dataChangeList = DaoSessionInstance.getInstance().dbDataChangeDao.loadAll()

        if (dataChangeList.size == 0) {
            saveChange(changeIndex, operating, changeTable)
            return
        } else

            for (i in dataChangeList.indices) {
                //首先确定是同一张表的同一条数据进行操作
                if ((dataChangeList[i].tableName == changeTable) and (dataChangeList[i].changeId == changeIndex)) {

                    if (dataChangeList[i].changeType == operating) {
                        break
                    }

                    //如果改变相同数据是删除就再记录一次，如果不是删除则不再记录
                    if (dataChangeList[i].changeType == Constant.DB_ADD && operating == Constant.DB_DELETE) {
                        deleteDbDataChange(dataChangeList[i].id)
                        break
                    } else if (dataChangeList[i].changeType == Constant.DB_UPDATE && operating == Constant.DB_DELETE) {
                        dataChangeList[i].changeType = operating
                        updateDbchange(dataChangeList[i])
                        break
                        //                        deleteDbDataChange(dataChangeList.get(i).getId());
                    } else if (dataChangeList[i].changeType == Constant.DB_ADD && operating == Constant.DB_UPDATE) {
                        break
                    } else if (dataChangeList[i].changeType == Constant.DB_DELETE && operating == Constant.DB_ADD) {
                        dataChangeList[i].changeType = operating
                        updateDbchange(dataChangeList[i])
                        break
                    }
                } else if (i == dataChangeList.size - 1) {
                    saveChange(changeIndex, operating, changeTable)
                    break
                }//如果数据表没有该数据直接添加
            }
    }

    private fun saveChange(changeIndex: Long?, operating: String, changeTable: String) {
        val dataChange = DbDataChange()
        dataChange.changeId = changeIndex
        dataChange.changeType = operating
        dataChange.tableName = changeTable
        DaoSessionInstance.getInstance().dbDataChangeDao.insert(dataChange)
    }
}
