package com.dadoutek.uled.DbModel;

import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.dao.DbSceneDao;

import java.util.List;

/**
 * Created by hejiajun on 2018/5/7.
 */

public class DbSceneUtils {
    public static DbSceneDao dbSceneDao= TelinkLightApplication.
            getDaoInstant().getDbSceneDao();

    public static void save(DbScene dbScene){
        dbSceneDao.save(dbScene);
    }

    public static void deleteAll(){
        dbSceneDao.deleteAll();
    }

    public static List<DbScene> getAllScene(){
        return dbSceneDao.loadAll();
    }

    public static void deleteScene(DbScene dbScene){
        dbSceneDao.delete(dbScene);
    }
}
