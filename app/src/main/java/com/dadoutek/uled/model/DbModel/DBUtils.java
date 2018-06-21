package com.dadoutek.uled.model.DbModel;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.dao.DbDataChangeDao;
import com.dadoutek.uled.dao.DbGroupDao;
import com.dadoutek.uled.dao.DbLightDao;
import com.dadoutek.uled.dao.DbRegionDao;
import com.dadoutek.uled.dao.DbSceneActionsDao;
import com.dadoutek.uled.dao.DbSceneDao;
import com.dadoutek.uled.dao.DbUserDao;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DaoSessionInstance;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.util.SharedPreferencesUtils;

import org.greenrobot.greendao.query.Query;
import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by hejiajun on 2018/5/18.
 */

public class DBUtils {

    /********************************************查询*******************************/

    public static List<DbSceneActions> searchActionsBySceneId(long id) {
        Query<DbSceneActions> query = DaoSessionInstance.getInstance().getDbSceneActionsDao().queryBuilder().
                where(DbSceneActionsDao.Properties.ActionId.eq(id)).build();
        return new ArrayList<>(query.list());
    }

    public static String getGroupNameByID(Long id) {
        DbGroup group = DaoSessionInstance.getInstance().getDbGroupDao().load(id);
        return group.getName();
    }

    public static DbGroup getGroupByID(long id) {
        DbGroup group = DaoSessionInstance.getInstance().getDbGroupDao().load((Long) id);
        return group;
    }

    public static DbLight getLightByID(long id) {
        DbLight light = DaoSessionInstance.getInstance().getDbLightDao().load((Long) id);
        return light;
    }

    public static DbRegion getRegionByID(long id) {
        DbRegion region = DaoSessionInstance.getInstance().getDbRegionDao().load((Long) id);
        return region;
    }

    public static DbScene getSceneByID(long id) {
        DbScene scene = DaoSessionInstance.getInstance().getDbSceneDao().load((Long) id);
        return scene;
    }

    public static DbSceneActions getSceneActionsByID(long id) {
        DbSceneActions actions = DaoSessionInstance.getInstance().getDbSceneActionsDao().load((Long) id);
        return actions;
    }

    public static DbUser getUserByID(long id) {
        DbUser user = DaoSessionInstance.getInstance().getDbUserDao().load((Long) id);
        return user;
    }

    public static DbUser getLastUser() {
        List<DbUser> list = DaoSessionInstance.getInstance().getDbUserDao().
                queryBuilder().orderDesc(DbUserDao.Properties.Id).list();
        return list.get(0);
    }

    public static DbRegion getCurrentRegion(long id) {
        DbRegion region = DaoSessionInstance.getInstance().getDbRegionDao().load((Long) id);
        return region;
    }

    public static DbRegion getLastRegion() {
        List<DbRegion> list = DaoSessionInstance.getInstance().getDbRegionDao().
                queryBuilder().orderDesc(DbRegionDao.Properties.Id).list();
        return list.get(0);
    }

    public static DbLight getLightByMesh(String mesh) {
        DbLight dbLight = DaoSessionInstance.getInstance().getDbLightDao().queryBuilder().
                where(DbLightDao.Properties.MeshAddr.eq(mesh)).unique();
        return dbLight;
    }

    public static DbGroup getGroupByMesh(String mesh) {
        DbGroup dbGroup = DaoSessionInstance.getInstance().getDbGroupDao().queryBuilder().
                where(DbGroupDao.Properties.MeshAddr.eq(mesh)).unique();
        Log.d("datasave", "getGroupByMesh: " + mesh);
        return dbGroup;
    }

    public static List<DbLight> getAllLight() {
        List<DbLight> lights = DaoSessionInstance.getInstance().getDbLightDao().loadAll();
        return lights;
    }

    public static List<DbLight> getLightByGroupID(long id) {
        Query<DbLight> query = DaoSessionInstance.getInstance().getDbLightDao().queryBuilder().
                where(DbLightDao.Properties.BelongGroupId.eq(id)).build();
        return new ArrayList<>(query.list());
    }

    public static List<DbGroup> getGroupList() {
        int allGIndex = -1;
        QueryBuilder<DbGroup> qb = DaoSessionInstance.getInstance().getDbGroupDao().queryBuilder();
        List<DbGroup> list = qb.where(
                DbGroupDao.Properties.BelongRegionId.eq(SharedPreferencesUtils.getCurrentUseRegion()))
                .list();

        return list;
    }

    public static List<DbScene> getSceneList() {
        int allGIndex = -1;
        QueryBuilder<DbScene> qb = DaoSessionInstance.getInstance().getDbSceneDao().queryBuilder();
        List<DbScene> list = qb.where(
                DbSceneDao.Properties.BelongRegionId.eq(SharedPreferencesUtils.getCurrentUseRegion()))
                .list();

        return list;
    }

    //未分组
    public static DbGroup getGroupNull() {
        int allGIndex = -1;
        QueryBuilder<DbGroup> qb = DaoSessionInstance.getInstance().getDbGroupDao().queryBuilder();
        List<DbGroup> list = qb.where(
                DbGroupDao.Properties.BelongRegionId.eq(SharedPreferencesUtils.getCurrentUseRegion()))
                .list();

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getMeshAddr() == 0xffff) {
                return list.get(i);
            }
        }
        return null;
    }

    public static List<DbScene> getSceneAll() {
        return DaoSessionInstance.getInstance().getDbSceneDao().loadAll();
    }

    public static List<DbRegion> getRegionAll() {
        return DaoSessionInstance.getInstance().getDbRegionDao().loadAll();
    }

    public static List<DbDataChange> getDataChangeAll() {
        return DaoSessionInstance.getInstance().getDbDataChangeDao().loadAll();
    }

    public static List<DbGroup> getAllGroups() {
        return DaoSessionInstance.getInstance().getDbGroupDao().queryBuilder().list();
    }

    public static List<DbDeleteGroup> getDeleteGroups() {
        return DaoSessionInstance.getInstance().getDbDeleteGroupDao().queryBuilder().list();
    }

    /********************************************保存*******************************/

    public static void saveRegion(DbRegion dbRegion,boolean isFromServer) {
        if(isFromServer){
            DbRegion dbRegionOld = DaoSessionInstance.getInstance().getDbRegionDao().queryBuilder().
                    where(DbRegionDao.Properties.ControlMesh.eq(dbRegion.getControlMesh())).unique();
            if (dbRegionOld == null) {
                DaoSessionInstance.getInstance().getDbRegionDao().insert(dbRegion);
            }
        }else{
            //判断原来是否保存过这个区域
            DbRegion dbRegionOld = DaoSessionInstance.getInstance().getDbRegionDao().queryBuilder().
                    where(DbRegionDao.Properties.ControlMesh.eq(dbRegion.getControlMesh())).unique();
            if (dbRegionOld == null) {//直接插入
                DaoSessionInstance.getInstance().getDbRegionDao().insert(dbRegion);
                //暂时用本地保存区域
                SharedPreferencesUtils.saveCurrentUseRegion(dbRegion.getId());

                recordingChange(dbRegion.getId(),
                        DaoSessionInstance.getInstance().getDbRegionDao().getTablename(),
                        Constant.DB_ADD);
                //创建新区域首先创建一个所有灯的分组
                createAllLightControllerGroup();
            } else {//更新数据库
                dbRegion.setId(dbRegionOld.getId());
                DaoSessionInstance.getInstance().getDbRegionDao().update(dbRegion);
                //暂时用本地保存区域
                SharedPreferencesUtils.saveCurrentUseRegion(dbRegion.getId());
                recordingChange(dbRegion.getId(),
                        DaoSessionInstance.getInstance().getDbRegionDao().getTablename(),
                        Constant.DB_UPDATE);
            }
        }
    }

    public static void insertRegion(DbRegion dbRegion) {
        //判断原来是否保存过这个区域
        DbRegion dbRegionOld = DaoSessionInstance.getInstance().getDbRegionDao().queryBuilder().
                where(DbRegionDao.Properties.ControlMesh.eq(dbRegion.getControlMesh())).unique();
        if (dbRegionOld == null) {//直接插入
            DaoSessionInstance.getInstance().getDbRegionDao().insert(dbRegion);
            //暂时用本地保存区域
            SharedPreferencesUtils.saveCurrentUseRegion(dbRegion.getId());

            recordingChange(dbRegion.getId(),
                    DaoSessionInstance.getInstance().getDbRegionDao().getTablename(),
                    Constant.DB_ADD);
        } else {//更新数据库
            dbRegion.setId(dbRegionOld.getId());
            DaoSessionInstance.getInstance().getDbRegionDao().update(dbRegion);
            //暂时用本地保存区域
            SharedPreferencesUtils.saveCurrentUseRegion(dbRegion.getId());
            recordingChange(dbRegion.getId(),
                    DaoSessionInstance.getInstance().getDbRegionDao().getTablename(),
                    Constant.DB_UPDATE);
        }
    }

    public static void saveGroup(DbGroup group,boolean isFromServer) {
        if(isFromServer){
            DaoSessionInstance.getInstance().getDbGroupDao().insert(group);
        }else{
            DaoSessionInstance.getInstance().getDbGroupDao().insertOrReplace(group);
            recordingChange(group.getId(),
                    DaoSessionInstance.getInstance().getDbGroupDao().getTablename(),
                    Constant.DB_ADD);

            //本地匹配index
            List<DbGroup> dbOldGroupList = (List<DbGroup>) SharedPreferencesHelper.
                    getObject(TelinkLightApplication.getInstance(),"oldIndexData");
            if(dbOldGroupList!=null){
                dbOldGroupList.add(group);
                SharedPreferencesHelper.
                        putObject(TelinkLightApplication.getInstance(),"oldIndexData",dbOldGroupList);
            }
        }
    }

    public static void saveDeleteGroup(DbGroup group) {
        DbDeleteGroup dbDeleteGroup=new DbDeleteGroup();
        dbDeleteGroup.setGroupAress(group.getMeshAddr());
        DaoSessionInstance.getInstance().getDbDeleteGroupDao().save(dbDeleteGroup);
    }

    public static void saveLight(DbLight light,boolean isFromServer) {
        if(isFromServer){
            DaoSessionInstance.getInstance().getDbLightDao().insert(light);
        }else{
            //保存灯之前先把所有的灯都分配到当前的所有组去
            DbGroup dbGroup = getGroupNull();
            light.setBelongGroupId(dbGroup.getId());

            DaoSessionInstance.getInstance().getDbLightDao().save(light);
            recordingChange(light.getId(),
                    DaoSessionInstance.getInstance().getDbLightDao().getTablename(),
                    Constant.DB_ADD);
        }
    }

    public static void oldToNewSaveLight(DbLight light) {
        DaoSessionInstance.getInstance().getDbLightDao().save(light);
        recordingChange(light.getId(),
                DaoSessionInstance.getInstance().getDbLightDao().getTablename(),
                Constant.DB_ADD);
    }

    public static void saveUser(DbUser dbUser) {
        DaoSessionInstance.getInstance().getDbUserDao().insertOrReplace(dbUser);
        recordingChange(dbUser.getId(),
                DaoSessionInstance.getInstance().getDbUserDao().getTablename(),
                Constant.DB_ADD);
    }

    public static void saveScene(DbScene dbScene,boolean isFromServer) {
        if(isFromServer){
            DaoSessionInstance.getInstance().getDbSceneDao().insert(dbScene);
        }else{
            DaoSessionInstance.getInstance().getDbSceneDao().save(dbScene);
            recordingChange(dbScene.getId(),
                    DaoSessionInstance.getInstance().getDbSceneDao().getTablename(),
                    Constant.DB_ADD);
        }
    }

    public static void saveSceneActions(DbSceneActions sceneActions) {
        DaoSessionInstance.getInstance().getDbSceneActionsDao().save(sceneActions);
//        recordingChange(sceneActions.getId(),
//                DaoSessionInstance.getInstance().getDbSceneActionsDao().getTablename(),
//                Constant.DB_ADD);
    }

    public static void saveSceneActions(DbSceneActions sceneActions,Long id,
                                        Long sceneId) {
        DbSceneActions actions=new DbSceneActions();
        String account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(), Constant.DB_NAME_KEY, "dadou");
        actions.setBelongAccount(account);
        actions.setActionId(sceneId);
        actions.setBrightness(sceneActions.getBrightness());
        actions.setColorTemperature(sceneActions.getColorTemperature());
        actions.setGroupAddr(sceneActions.getGroupAddr());

        DaoSessionInstance.getInstance().getDbSceneActionsDao().save(actions);
    }

    /********************************************更改*******************************/

    public static void updateGroup(DbGroup group) {
        DaoSessionInstance.getInstance().getDbGroupDao().update(group);
        recordingChange(group.getId(),
                DaoSessionInstance.getInstance().getDbGroupDao().getTablename(),
                Constant.DB_UPDATE);

        //本地匹配index
        List<DbGroup> dbOldGroupList = (List<DbGroup>) SharedPreferencesHelper.
                getObject(TelinkLightApplication.getInstance(),"oldIndexData");
        if(dbOldGroupList!=null){
            for(int k=0;k<dbOldGroupList.size();k++){
                if(group.getMeshAddr()==dbOldGroupList.get(k).getMeshAddr()){
                    dbOldGroupList.set(k,group);
                    SharedPreferencesHelper.
                            putObject(TelinkLightApplication.getInstance(),"oldIndexData",dbOldGroupList);
                    break;
                }
            }
        }
    }

    public static void updateLight(DbLight light) {
        DaoSessionInstance.getInstance().getDbLightDao().update(light);
        recordingChange(light.getId(),
                DaoSessionInstance.getInstance().getDbLightDao().getTablename(),
                Constant.DB_UPDATE);
    }

    public static void updateScene(DbScene scene) {
        DaoSessionInstance.getInstance().getDbSceneDao().update(scene);
        recordingChange(scene.getId(),
                DaoSessionInstance.getInstance().getDbSceneDao().getTablename(),
                Constant.DB_UPDATE);
    }

    public static void updateDbActions(DbSceneActions actions) {
        DaoSessionInstance.getInstance().getDbSceneActionsDao().update(actions);
        recordingChange(actions.getId(),
                DaoSessionInstance.getInstance().getDbSceneActionsDao().getTablename(),
                Constant.DB_UPDATE);
    }

    /********************************************删除*******************************/

    public static void deleteGroup(DbGroup dbGroup) {
        List<DbLight> lights = DBUtils.getLightByGroupID(dbGroup.getId());
        DbGroup allGroup = getGroupNull();

        for (int i = 0; i < lights.size(); i++) {
            lights.get(i).setBelongGroupId(allGroup.getId());
            updateLight(lights.get(i));
        }

        saveDeleteGroup(dbGroup);
        DaoSessionInstance.getInstance().getDbGroupDao().delete(dbGroup);
        recordingChange(dbGroup.getId(),
                DaoSessionInstance.getInstance().getDbGroupDao().getTablename(),
                Constant.DB_DELETE);

        //本地匹配index
        List<DbGroup> dbOldGroupList = (List<DbGroup>) SharedPreferencesHelper.
                getObject(TelinkLightApplication.getInstance(),"oldIndexData");
        if(dbOldGroupList!=null){
            for(int k=0;k<dbOldGroupList.size();k++){
                if(dbGroup.getMeshAddr()==dbOldGroupList.get(k).getMeshAddr()){
                    dbOldGroupList.remove(k);
                    SharedPreferencesHelper.
                            putObject(TelinkLightApplication.getInstance(),"oldIndexData",dbOldGroupList);
                    break;
                }
            }
        }
    }

    public static void deleteDeleteGroup(DbDeleteGroup dbDeleteGroup){
        DaoSessionInstance.getInstance().getDbDeleteGroupDao().delete(dbDeleteGroup);
    }

    public static void deleteLight(DbLight dbLight) {
        DaoSessionInstance.getInstance().getDbLightDao().delete(dbLight);
        recordingChange(dbLight.getId(),
                DaoSessionInstance.getInstance().getDbLightDao().getTablename(),
                Constant.DB_DELETE);
    }

    public static void deleteScene(DbScene dbScene) {
        DaoSessionInstance.getInstance().getDbSceneDao().delete(dbScene);
        recordingChange(dbScene.getId(),
                DaoSessionInstance.getInstance().getDbSceneDao().getTablename(),
                Constant.DB_DELETE);
    }

    public static void deleteSceneActionsList(List<DbSceneActions> sceneActionslist) {
        DaoSessionInstance.getInstance().getDbSceneActionsDao().deleteInTx(sceneActionslist);
//        for (int i = 0; i < sceneActionslist.size(); i++) {
//            recordingChange(sceneActionslist.get(i).getId(),
//                    DaoSessionInstance.getInstance().getDbSceneActionsDao().getTablename(),
//                    Constant.DB_DELETE);
//        }
    }

    public static void deleteAllData() {
        DaoSessionInstance.getInstance().getDbUserDao().deleteAll();
        DaoSessionInstance.getInstance().getDbSceneDao().deleteAll();
        DaoSessionInstance.getInstance().getDbSceneActionsDao().deleteAll();
        DaoSessionInstance.getInstance().getDbRegionDao().deleteAll();
        DaoSessionInstance.getInstance().getDbGroupDao().deleteAll();
        DaoSessionInstance.getInstance().getDbLightDao().deleteAll();
        DaoSessionInstance.getInstance().getDbDataChangeDao().deleteAll();
    }

    public static void deleteDbDataChange(long id){
        DaoSessionInstance.getInstance().getDbDataChangeDao().deleteByKey(id);
    }

    /********************************************其他*******************************/

    public static void addNewGroup(String name, List<DbGroup> groups, Context context) {
        if (!checkRepeat(groups, context, name)) {
            int count = groups.size();
            int newMeshAdress;
            DbGroup group = new DbGroup();

            List<DbDeleteGroup> list=getDeleteGroups();
            if(list.size()>0){
                newMeshAdress=list.get(0).getGroupAress();
                group.setMeshAddr(newMeshAdress);
                deleteDeleteGroup(list.get(0));
            }else {
                newMeshAdress = ++count;
                group.setMeshAddr(0x8001 + newMeshAdress);
            }
            group.setName(name);
            group.setBrightness(100);
            group.setColorTemperature(100);
            group.setBelongRegionId((int) SharedPreferencesUtils.getCurrentUseRegion());//目前暂无分区 区域ID暂为0
            groups.add(group);
            //新增数据库保存
            DBUtils.saveGroup(group,false);

            recordingChange(group.getId(),
                    DaoSessionInstance.getInstance().getDbGroupDao().getTablename(),
                    Constant.DB_ADD);
        }

    }

    public static boolean checkRepeat(List<DbGroup> groups, Context context, String newName) {
        for (
                int k = 0; k < groups.size(); k++)

        {
            if (groups.get(k).getName().equals(newName)) {
                Toast.makeText(context, R.string.creat_group_fail_tip, Toast.LENGTH_LONG).show();
                return true;
            }
        }
        return false;
    }


    /**
     * 创建一个控制所有灯的分组
     *
     * @return group对象
     */
    public static void createAllLightControllerGroup() {
        DbGroup groupAllLights = new DbGroup();
        groupAllLights.setName(TelinkLightApplication.getInstance().getString(R.string.allLight));
        groupAllLights.setMeshAddr(0xFFFF);
        groupAllLights.setBrightness(100);
        groupAllLights.setColorTemperature(100);
        groupAllLights.setBelongRegionId((int) SharedPreferencesUtils.getCurrentUseRegion());
        List<DbGroup> list = getGroupList();
        DaoSessionInstance.getInstance().getDbGroupDao().insert(groupAllLights);
        recordingChange(groupAllLights.getId(),
                DaoSessionInstance.getInstance().getDbGroupDao().getTablename(),
                Constant.DB_ADD);
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
    public static void recordingChange(Long changeIndex, String changeTable, String operating) {
        List<DbDataChange> dataChangeList = DaoSessionInstance.getInstance().getDbDataChangeDao().loadAll();

        if (dataChangeList.size() == 0) {
            saveChange(changeIndex, operating, changeTable);
            return;
        } else

            for (int i = 0; i < dataChangeList.size(); i++) {
                //首先确定是同一张表的同一条数据进行操作
                if (dataChangeList.get(i).getTableName().equals(changeTable) &
                        dataChangeList.get(i).getChangeId().equals(changeIndex)) {
                    //如果改变相同数据是删除就再记录一次，如果不是删除则不再记录
                    if (!operating.equals(Constant.DB_DELETE)) {
                        break;
                    } else {
                        saveChange(changeIndex, operating, changeTable);
                        break;
                    }
                }
                //如果数据表没有该数据直接添加
                else if (i == dataChangeList.size() - 1) {
                    saveChange(changeIndex, operating, changeTable);
                    break;
                }
            }
    }

    private static void saveChange(Long changeIndex, String operating, String changeTable) {
        DbDataChange dataChange = new DbDataChange();
        dataChange.setChangeId(changeIndex);
        dataChange.setChangeType(operating);
        dataChange.setTableName(changeTable);
        DaoSessionInstance.getInstance().getDbDataChangeDao().insert(dataChange);
    }
}
