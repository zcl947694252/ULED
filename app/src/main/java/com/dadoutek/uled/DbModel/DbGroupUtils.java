package com.dadoutek.uled.DbModel;

import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.dao.DbGroupDao;

import java.util.List;

/**
 * Created by hejiajun on 2018/5/14.
 */

public class DbGroupUtils {
    public static DbGroupDao dbGroupDao = TelinkLightApplication.
            getDaoInstant().getDbGroupDao();

    public static void save(DbGroup dbGroup) {
        dbGroupDao.save(dbGroup);
    }

    public static void deleteAll() {
        dbGroupDao.deleteAll();
    }

    public static List<DbGroup> getAllGroup() {
        return dbGroupDao.loadAll();
    }

    public static void deleteGroup(DbGroup dbGroup) {
        dbGroupDao.delete(dbGroup);
    }

    public static void update(DbGroup dbGroup) {
        dbGroupDao.update(dbGroup);
    }
}
