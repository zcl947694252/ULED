package com.dadoutek.uled.DbModel;

import android.content.Context;
import android.graphics.Region;
import android.widget.Toast;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.dao.DaoSession;
import com.dadoutek.uled.dao.DbGroupDao;
import com.dadoutek.uled.dao.DbRegionDao;
import com.dadoutek.uled.dao.DbSceneActionsDao;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DaoSessionInstance;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Groups;
import com.dadoutek.uled.model.Lights;
import com.dadoutek.uled.model.SharedPreferencesHelper;

import org.greenrobot.greendao.query.Query;

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

    public static String getGroupNameByID(Long id){
        DbGroup group=DaoSessionInstance.getInstance().getDbGroupDao().load(id);
        return group.getName();
    }

    public static DbGroup getGroupByID(long id){
        DbGroup group=DaoSessionInstance.getInstance().getDbGroupDao().load((Long) id);
        return group;
    }

    public static List<DbGroup> getGroupList(){
        List<DbGroup> list=new ArrayList<>();
        Query<DbGroup> query = DaoSessionInstance.getInstance().getDbGroupDao().queryBuilder().
                where(DbGroupDao.Properties.BelongRegionId.eq(Constant.CURRENT_USE_REGION_ID)).build();
        for (DbGroup dbGroup : query.list()) {
            list.add(dbGroup);
        }
        return list;
    }

    /********************************************保存*******************************/

    public static void saveRegion(DbRegion dbRegion){
        //判断原来是否保存过这个区域
        DbRegion dbRegionOld=DaoSessionInstance.getInstance().getDbRegionDao().queryBuilder().
                where(DbRegionDao.Properties.ControlMesh.eq(dbRegion.getControlMesh())).unique();
        if(dbRegionOld==null){//直接插入
            DaoSessionInstance.getInstance().getDbRegionDao().insert(dbRegion);
        }else{//更新数据库
            dbRegion.setId(dbRegionOld.getId());
            DaoSessionInstance.getInstance().getDbRegionDao().update(dbRegion);
        }
    }

    public static void saveGroup(DbGroup group){
        DaoSessionInstance.getInstance().getDbGroupDao().insertOrReplace(group);
    }

    public static void saveLight(DbLight light){
        DaoSessionInstance.getInstance().getDbLightDao().insertOrReplace(light);
    }

    /********************************************更改*******************************/

    public static void updateGroup(DbGroup group){
        DaoSessionInstance.getInstance().getDbGroupDao().update(group);
    }

    public static void updateLight(DbLight light){
        DaoSessionInstance.getInstance().getDbLightDao().update(light);
    }

    /********************************************删除*******************************/

    public static void deleteGroup(DbGroup dbGroup){
        DaoSessionInstance.getInstance().getDbGroupDao().delete(dbGroup);
    }

    /********************************************其他*******************************/

    public static void addNewGroup(String name,List<DbGroup> groups,Context context){
        if (!checkRepeat(groups, context, name)) {
            int count = groups.size();
            int newMeshAdress = ++count;
            DbGroup group = new DbGroup();
            group.setName(name);
            group.setMeshAddr(0x8001 + newMeshAdress);
            group.setBrightness(100);
            group.setColorTemperature(100);
            group.setBelongRegionId((int)Constant.CURRENT_USE_REGION_ID);//目前暂无分区 区域ID暂为0
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
     * @return group对象
     */
    public static DbGroup createAllLightControllerGroup(Context context) {
        DbGroup groupAllLights = new DbGroup();
        groupAllLights.setName(context.getString(R.string.allLight));
        groupAllLights.setMeshAddr(0xFFFF);
        groupAllLights.setBrightness(100);
        groupAllLights.setColorTemperature(100);
        return groupAllLights;
    }
}
