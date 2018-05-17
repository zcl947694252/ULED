package com.dadoutek.uled.DbModel;

import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.dao.DbLightDao;

import java.util.List;

/**
 * Created by hejiajun on 2018/5/14.
 */

public class DbLightUtils {
    public static DbLightDao dbLightDao = TelinkLightApplication.
            getDaoInstant().getDbLightDao();

    public static void save(DbLight dbLight) {
        dbLightDao.save(dbLight);
    }

    public static void deleteAll() {
        dbLightDao.deleteAll();
    }

    public static List<DbLight> getAllLight() {
        return dbLightDao.loadAll();
    }

    public static void deleteLight(DbLight dbLight) {
        dbLightDao.delete(dbLight);
    }

    public static void update(DbLight dbLight) {
        dbLightDao.update(dbLight);
    }
}
