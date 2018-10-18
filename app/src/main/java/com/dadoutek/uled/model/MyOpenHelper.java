package com.dadoutek.uled.model;

import android.content.Context;
import android.util.Log;

import com.dadoutek.uled.dao.DaoMaster;
import com.dadoutek.uled.dao.DbDeleteGroupDao;
import com.dadoutek.uled.dao.DbGroupDao;
import com.dadoutek.uled.dao.DbLightDao;
import com.dadoutek.uled.dao.DbSceneActionsDao;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.util.MigrationHelper;

import org.greenrobot.greendao.database.Database;

public class MyOpenHelper extends DaoMaster.OpenHelper {
    MyOpenHelper(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1:
                DbDeleteGroupDao.createTable(db, true);
                break;
            case 7:
                Log.d("", "onUpgrade: "+"");
                break;
        }
    }
}
