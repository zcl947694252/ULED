package com.dadoutek.uled.DbModel;

import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.dao.DbRegionDao;
import com.dadoutek.uled.dao.DbSceneActionsDao;
import com.dadoutek.uled.model.DaoSessionInstance;

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
//        if(queryRegionCheckRepeat(dbRegion)){
//            DaoSessionInstance.getInstance().getDbRegionDao().update(dbRegion);
//        }else{
//
//        }
    }

    //如果控制mesh重复则判定为重复
    public static boolean queryRegionCheckRepeatAndSave(DbRegion dbRegion){
        DbRegion dbRegionNew=DaoSessionInstance.getInstance().getDbRegionDao().queryBuilder().
                where(DbRegionDao.Properties.ControlMesh.eq(dbRegion.getControlMesh())).unique();
        if(dbRegionNew==null){//直接插入
            DaoSessionInstance.getInstance().getDbRegionDao().insert(dbRegion);
        }else{//更新数据库
            DaoSessionInstance.getInstance().getDbRegionDao()
        }
    }
}
