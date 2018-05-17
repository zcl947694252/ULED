package com.dadoutek.uled.DbModel;

import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.dao.DbRegionDao;

import java.util.List;

/**
 * Created by hejiajun on 2018/5/14.
 */

public class DbRegionUtils {
    public static DbRegionDao dbRegionDao = TelinkLightApplication.
            getDaoInstant().getDbRegionDao();

    public static void save(DbRegion dbRegion) {
        dbRegionDao.save(dbRegion);
    }

    public static void deleteAll() {
        dbRegionDao.deleteAll();
    }

    public static List<DbRegion> getAllRegion() {
        return dbRegionDao.loadAll();
    }

    public static void deleteRegion(DbRegion dbRegion) {
        dbRegionDao.delete(dbRegion);
    }
}
