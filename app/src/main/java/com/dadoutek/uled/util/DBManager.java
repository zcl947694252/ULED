package com.dadoutek.uled.util;

import android.content.Context;

import com.dadoutek.uled.DbModel.DbSceneActionsUtils;
import com.dadoutek.uled.DbModel.DbSceneUtils;

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
        DbSceneUtils.deleteAll();
        DbSceneActionsUtils.deleteAll();
    }

    public void copyDatabaseToSDCard(Context context) {
        CopyDataBaseFile copyDataBaseFile = new CopyDataBaseFile(context);
        copyDataBaseFile.backupDataBase();
    }
}
