package com.dadoutek.uled.model;

import android.database.sqlite.SQLiteDatabase;

import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.dao.DaoMaster;
import com.dadoutek.uled.dao.DaoSession;

/**
 * Created by hejiajun on 2018/5/19.
 */

public class DaoSessionInstance {
    private DaoSessionInstance() {}

    private static DaoSession session=null;

    public static String name=null;

    public static DaoSession getInstance() {
        if(name==null){
            name=SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                    Constant.DB_NAME_KEY,"uled")+".db";
        }

        if (session == null) {
            //创建数据库shop.db"
            DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(TelinkLightApplication.getInstance()
                    , name, null);
            //获取可写数据库
            SQLiteDatabase db = helper.getWritableDatabase();
            //获取数据库对象
            DaoMaster daoMaster = new DaoMaster(db);
            //获取Dao对象管理者
            session = daoMaster.newSession();
        }
        return session;
    }

    public static void destroySession(){
        session=null;
    }
}
