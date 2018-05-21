package com.dadoutek.uled.DbModel;

import android.graphics.Region;

import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.dao.DbRegionDao;
import com.dadoutek.uled.dao.DbSceneActionsDao;
import com.dadoutek.uled.model.DaoSessionInstance;
import com.dadoutek.uled.model.Groups;
import com.dadoutek.uled.model.Lights;

import org.greenrobot.greendao.query.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hejiajun on 2018/5/18.
 */

public class DBUtils {

    public static List<DbSceneActions> searchActionsBySceneId(long id) {
        List<DbSceneActions> list = new ArrayList<>();
        Query<DbSceneActions> query = DaoSessionInstance.getInstance().getDbSceneActionsDao().queryBuilder().
                where(DbSceneActionsDao.Properties.ActionId.eq(id)).build();
        for (DbSceneActions dbSceneActions : query.list()) {
            list.add(dbSceneActions);
        }
        return list;
    }

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

    public static void saveGroups(Groups groups){
        DbRegion region=new DbRegion();
        for(int i=0;i<groups.size();i++){
            DbGroup dbGroup=new DbGroup();
            dbGroup.setBelongRegionId(region.getId().intValue());
            dbGroup.setBrightness(groups.get(i).brightness);
            dbGroup.setColorTemperature(groups.get(i).temperature);
            dbGroup.setMeshAddr(groups.get(i).meshAddress);
            dbGroup.setName(groups.get(i).name);
            DaoSessionInstance.getInstance().getDbGroupDao().insert(dbGroup);
        }
    }

    public static void saveLight(Lights lights){

    }

//    public static Long getGroupIdByMeshAddress(){
//
//    }
}
