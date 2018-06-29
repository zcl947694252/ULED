package com.dadoutek.uled.model;

import android.content.Context;

import com.dadoutek.uled.dao.DaoMaster;
import com.dadoutek.uled.dao.DbDeleteGroupDao;

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
        }
    }
}
