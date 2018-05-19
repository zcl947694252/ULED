package com.dadoutek.uled.util;

import android.content.Context;

import com.dadoutek.uled.TelinkLightApplication;

/**
 * Created by hejiajun on 2018/5/10.
 */

public class DBManager {

    private static DBManager mInstance;

    public static DBManager getInstance() {
        if (mInstance == null) {
            mInstance = new DBManager();
        }
        return mInstance;
    }

    public void deleteAllData() {
        TelinkLightApplication.getDaoInstant().getDbUserDao().deleteAll();
        TelinkLightApplication.getDaoInstant().getDbSceneDao().deleteAll();
        TelinkLightApplication.getDaoInstant().getDbSceneActionsDao().deleteAll();
        TelinkLightApplication.getDaoInstant().getDbRegionDao().deleteAll();
        TelinkLightApplication.getDaoInstant().getDbGroupDao().deleteAll();
        TelinkLightApplication.getDaoInstant().getDbLightDao().deleteAll();
    }

    public void copyDatabaseToSDCard(Context context) {
        CopyDataBaseFile copyDataBaseFile = new CopyDataBaseFile(context);
        copyDataBaseFile.backupDataBase();
    }
}
