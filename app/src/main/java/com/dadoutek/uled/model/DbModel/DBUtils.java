package com.dadoutek.uled.model.DbModel;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.dao.DbGroupDao;
import com.dadoutek.uled.dao.DbLightDao;
import com.dadoutek.uled.dao.DbRegionDao;
import com.dadoutek.uled.dao.DbSceneActionsDao;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DaoSessionInstance;
import com.dadoutek.uled.util.SharedPreferencesUtils;

import org.greenrobot.greendao.query.Query;
import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

import java.util.ArrayList;
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

    public static DbRegion getCurrentRegion(long id) {
        DbRegion region = DaoSessionInstance.getInstance().getDbRegionDao().load((Long) id);
        return region;
    }

    public static DbRegion getLastRegion() {
        List<DbRegion> list  = DaoSessionInstance.getInstance().getDbRegionDao().
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
        List<DbGroup> list = qb.whereOr(
                DbGroupDao.Properties.BelongRegionId.eq(SharedPreferencesUtils.getCurrentUseRegion()),
               DbGroupDao.Properties.BelongRegionId.eq(allGIndex))
        .list();

        return list;
    }

    public static List<DbScene> getSceneAll(){
        return DaoSessionInstance.getInstance().getDbSceneDao().loadAll();
    }

    public static List<DbRegion> getRegionAll(){
        return DaoSessionInstance.getInstance().getDbRegionDao().loadAll();
    }

    public static List<DbDataChange> getDataChangeAll(){
        return DaoSessionInstance.getInstance().getDbDataChangeDao().loadAll();
    }

    /********************************************保存*******************************/

    public static void saveRegion(DbRegion dbRegion) {
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

    public static void saveGroup(DbGroup group) {
        DaoSessionInstance.getInstance().getDbGroupDao().insertOrReplace(group);
        recordingChange(group.getId(),
                DaoSessionInstance.getInstance().getDbGroupDao().getTablename(),
                Constant.DB_ADD);
    }

    public static void saveLight(DbLight light) {
        DaoSessionInstance.getInstance().getDbLightDao().save(light);
        recordingChange(light.getId(),
                DaoSessionInstance.getInstance().getDbLightDao().getTablename(),
                Constant.DB_ADD);
    }

    public static void saveUser(DbUser dbUser) {
        DaoSessionInstance.getInstance().getDbUserDao().save(dbUser);
        recordingChange(dbUser.getId(),
                DaoSessionInstance.getInstance().getDbUserDao().getTablename(),
                Constant.DB_ADD);
    }

    public static void saveScene(DbScene dbScene) {
        DaoSessionInstance.getInstance().getDbSceneDao().save(dbScene);
        recordingChange(dbScene.getId(),
                DaoSessionInstance.getInstance().getDbSceneDao().getTablename(),
                Constant.DB_ADD);
    }

    public static void saveSceneActions(DbSceneActions sceneActions) {
        DaoSessionInstance.getInstance().getDbSceneActionsDao().save(sceneActions);
        recordingChange(sceneActions.getId(),
                DaoSessionInstance.getInstance().getDbSceneActionsDao().getTablename(),
                Constant.DB_ADD);
    }

    public static List<DbGroup> getAllGroups() {
        return DaoSessionInstance.getInstance().getDbGroupDao().queryBuilder().list();
    }

    /********************************************更改*******************************/

    public static void updateGroup(DbGroup group) {
        DaoSessionInstance.getInstance().getDbGroupDao().update(group);
        recordingChange(group.getId(),
                DaoSessionInstance.getInstance().getDbGroupDao().getTablename(),
                Constant.DB_UPDATE);
    }

    public static void updateLight(DbLight light) {
        DaoSessionInstance.getInstance().getDbLightDao().update(light);
        recordingChange(light.getId(),
                DaoSessionInstance.getInstance().getDbLightDao().getTablename(),
                Constant.DB_UPDATE);
    }

    /********************************************删除*******************************/

    public static void deleteGroup(DbGroup dbGroup) {
        DaoSessionInstance.getInstance().getDbGroupDao().delete(dbGroup);
        recordingChange(dbGroup.getId(),
                DaoSessionInstance.getInstance().getDbGroupDao().getTablename(),
                Constant.DB_DELETE);
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

    public static void deleteAllData() {
        DaoSessionInstance.getInstance().getDbUserDao().deleteAll();
        DaoSessionInstance.getInstance().getDbSceneDao().deleteAll();
        DaoSessionInstance.getInstance().getDbSceneActionsDao().deleteAll();
        DaoSessionInstance.getInstance().getDbRegionDao().deleteAll();
        DaoSessionInstance.getInstance().getDbGroupDao().deleteAll();
        DaoSessionInstance.getInstance().getDbLightDao().deleteAll();
    }

    /********************************************其他*******************************/

    public static void addNewGroup(String name, List<DbGroup> groups, Context context) {
        if (!checkRepeat(groups, context, name)) {
            int count = groups.size();
            int newMeshAdress = ++count;
            DbGroup group = new DbGroup();
            group.setName(name);
            group.setMeshAddr(0x8001 + newMeshAdress);
            group.setBrightness(100);
            group.setColorTemperature(100);
            group.setBelongRegionId((int) SharedPreferencesUtils.getCurrentUseRegion());//目前暂无分区 区域ID暂为0
            groups.add(group);
            //新增数据库保存
            DBUtils.saveGroup(group);

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
        groupAllLights.setBelongRegionId(-1);
        List<DbGroup> list = getGroupList();

        if (list.size() == 0) {
            DaoSessionInstance.getInstance().getDbGroupDao().insert(groupAllLights);
            recordingChange(groupAllLights.getId(),
                    DaoSessionInstance.getInstance().getDbGroupDao().getTablename(),
                    Constant.DB_ADD);
        }
//        for(int i=0;i<list.size();i++){
//            if(list.get(i).getMeshAddr()!=0xffff){
//                groupAllLights.setId(Long.valueOf(0));
//                DaoSessionInstance.getInstance().getDbGroupDao().save(groupAllLights);
//            }
//        }
    }

    /**
     * 记录本地数据库变化
     * @param changeIndex 变化的位置
     * @param changeTable 变化数据所属表
     * @param operating 所执行的操作
     */
    public static void recordingChange(Long changeIndex,String changeTable,String operating){
        List<DbDataChange> dataChangeList=DaoSessionInstance.getInstance().getDbDataChangeDao().loadAll();
        for(int i=0;i<dataChangeList.size();i++){
            //首先确定是同一张表的同一条数据进行操作
            if(dataChangeList.get(i).getChangeId()==
                    changeIndex&&dataChangeList.get(i).getTableName().equals(changeTable)){
                //先添加数据后进行其他操作的情况
                if(dataChangeList.get(i).getChangeType().equals(Constant.DB_ADD)&&
                        !operating.equals(Constant.DB_ADD)){
                    if(operating.equals(Constant.DB_UPDATE)){
                        return;
                    }
                }

                if(dataChangeList.get(i).getChangeType().equals(operating)){
                    Log.d("sameData", "recordingChange: "+"--------");
                    return;
                }
            }
        }


        DbDataChange dataChange=new DbDataChange();
        dataChange.setChangeId(changeIndex);
        dataChange.setChangeType(operating);
        dataChange.setTableName(changeTable);
        DaoSessionInstance.getInstance().getDbDataChangeDao().insert(dataChange);
    }
}
