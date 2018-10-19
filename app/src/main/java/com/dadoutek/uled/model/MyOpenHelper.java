package com.dadoutek.uled.model;

import android.content.Context;
import android.util.Log;

import com.dadoutek.uled.dao.DaoMaster;
import com.dadoutek.uled.dao.DbDeleteGroupDao;
import com.dadoutek.uled.dao.DbLightDao;
import com.dadoutek.uled.util.MigrationHelper;

import org.greenrobot.greendao.database.Database;

public class MyOpenHelper extends DaoMaster.OpenHelper {
    MyOpenHelper(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {

        if(oldVersion<newVersion){
            MigrationHelper.getInstance().migrate(db,DbLightDao.class);
        }

        switch (oldVersion) {
            case 1:
                DbDeleteGroupDao.createTable(db, true);
                break;
        }
    }
}
