package com.dadoutek.uled.DbModel;

import android.content.Context;
import android.widget.Toast;

import com.dadoutek.uled.R;
import com.dadoutek.uled.dao.DbGroupDao;
import com.dadoutek.uled.dao.DbLightDao;
import com.dadoutek.uled.dao.DbRegionDao;
import com.dadoutek.uled.dao.DbSceneActionsDao;
import com.dadoutek.uled.model.DaoSessionInstance;
import com.dadoutek.uled.util.SharedPreferencesUtils;

import org.greenrobot.greendao.query.Query;
import org.greenrobot.greendao.query.QueryBuilder;

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

    public static DbRegion getCurrentRegion(long id) {
        DbRegion region = DaoSessionInstance.getInstance().getDbRegionDao().load((Long) id);
        return region;
    }

    public static DbLight getLight(String mesh) {
        DbLight dbLight = DaoSessionInstance.getInstance().getDbLightDao().queryBuilder().
                where(DbLightDao.Properties.MeshAddr.eq(mesh)).unique();
        return dbLight;
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

    /********************************************保存*******************************/

    public static void saveRegion(DbRegion dbRegion) {
        //判断原来是否保存过这个区域
        DbRegion dbRegionOld = DaoSessionInstance.getInstance().getDbRegionDao().queryBuilder().
                where(DbRegionDao.Properties.ControlMesh.eq(dbRegion.getControlMesh())).unique();
        if (dbRegionOld == null) {//直接插入
            DaoSessionInstance.getInstance().getDbRegionDao().insert(dbRegion);
            //暂时用本地保存区域
            SharedPreferencesUtils.saveCurrentUseRegion(dbRegion.getId());
        } else {//更新数据库
            dbRegion.setId(dbRegionOld.getId());
            DaoSessionInstance.getInstance().getDbRegionDao().update(dbRegion);
            //暂时用本地保存区域
            SharedPreferencesUtils.saveCurrentUseRegion(dbRegion.getId());
        }
    }

    public static void saveGroup(DbGroup group) {
        DaoSessionInstance.getInstance().getDbGroupDao().insertOrReplace(group);
    }

    public static void saveLight(DbLight light) {
        DaoSessionInstance.getInstance().getDbLightDao().insertOrReplace(light);
    }

    /********************************************更改*******************************/

    public static void updateGroup(DbGroup group) {
        DaoSessionInstance.getInstance().getDbGroupDao().update(group);
    }

    public static void updateLight(DbLight light) {
        DaoSessionInstance.getInstance().getDbLightDao().update(light);
    }

    /********************************************删除*******************************/

    public static void deleteGroup(DbGroup dbGroup) {
        DaoSessionInstance.getInstance().getDbGroupDao().delete(dbGroup);
    }

    public static void deleteLight(DbLight dbLight) {
        DaoSessionInstance.getInstance().getDbLightDao().delete(dbLight);
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
    public static void createAllLightControllerGroup(Context context) {
        DbGroup groupAllLights = new DbGroup();
        groupAllLights.setName(context.getString(R.string.allLight));
        groupAllLights.setMeshAddr(0xFFFF);
        groupAllLights.setBrightness(100);
        groupAllLights.setColorTemperature(100);
        groupAllLights.setBelongRegionId(-1);
        List<DbGroup> list = getGroupList();

        if (list.size() == 0) {
            DaoSessionInstance.getInstance().getDbGroupDao().insert(groupAllLights);
        }
//        for(int i=0;i<list.size();i++){
//            if(list.get(i).getMeshAddr()!=0xffff){
//                groupAllLights.setId(Long.valueOf(0));
//                DaoSessionInstance.getInstance().getDbGroupDao().save(groupAllLights);
//            }
//        }
    }
}
