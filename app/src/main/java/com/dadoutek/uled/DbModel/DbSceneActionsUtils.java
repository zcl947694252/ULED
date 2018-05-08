package com.dadoutek.uled.DbModel;

import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.dao.DbSceneActionsDao;
import com.dadoutek.uled.dao.DbSceneDao;

import org.greenrobot.greendao.query.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hejiajun on 2018/5/7.
 */

public class DbSceneActionsUtils {
    public static DbSceneActionsDao dbSceneActionsDao= TelinkLightApplication.
            getDaoInstant().getDbSceneActionsDao();

    public static void save(DbSceneActions dbSceneActions){
        dbSceneActionsDao.save(dbSceneActions);
    }

    public static List<DbSceneActions> searchActionsBySceneId(long id){
        List<DbSceneActions> list=new ArrayList<>();
        Query<DbSceneActions> query=dbSceneActionsDao.queryBuilder().
                where(DbSceneActionsDao.Properties.ActionId.eq(id)).build();
        for(DbSceneActions dbSceneActions:query.list()){
            list.add(dbSceneActions);
        }
        return  list;
    }
}
