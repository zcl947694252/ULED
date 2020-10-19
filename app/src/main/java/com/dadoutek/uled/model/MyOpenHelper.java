package com.dadoutek.uled.model;

import android.content.Context;

import com.dadoutek.uled.dao.DaoMaster;
import com.dadoutek.uled.dao.DaoSession;
import com.dadoutek.uled.dao.DbColorNodeDao;
import com.dadoutek.uled.dao.DbConnectorDao;
import com.dadoutek.uled.dao.DbCurtainDao;
import com.dadoutek.uled.dao.DbDataChangeDao;
import com.dadoutek.uled.dao.DbDeleteGroupDao;
import com.dadoutek.uled.dao.DbDiyGradientDao;
import com.dadoutek.uled.dao.DbGroupDao;
import com.dadoutek.uled.dao.DbLightDao;
import com.dadoutek.uled.dao.DbRegionDao;
import com.dadoutek.uled.dao.DbSceneActionsDao;
import com.dadoutek.uled.dao.DbSceneDao;
import com.dadoutek.uled.dao.DbSensorDao;
import com.dadoutek.uled.dao.DbSwitchDao;
import com.dadoutek.uled.dao.DbUserDao;
import com.dadoutek.uled.model.dbModel.DBUtils;
import com.dadoutek.uled.model.dbModel.DbGroup;
import com.dadoutek.uled.model.dbModel.DbLight;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.github.yuweiguocn.library.greendao.MigrationHelper;

import org.greenrobot.greendao.database.Database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyOpenHelper extends DaoMaster.OpenHelper {

    private static DaoMaster daoMaster;
    private static DaoSession daoSession;
    public static final String DBNAME = SharedPreferencesHelper.getString(TelinkLightApplication.Companion.getApp(),
            Constants.DB_NAME_KEY, "uled") + ".db";

    MyOpenHelper(Context context, String name) {
        super(context, name);
    }


    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {

        if (oldVersion < newVersion) {
            MigrationHelper.migrate(db, new MigrationHelper.ReCreateAllTableListener(){
                        @Override
                        public void onCreateAllTables(Database db, boolean ifNotExists) {
                            DaoMaster.createAllTables(db, ifNotExists);
                        }

                        @Override
                        public void onDropAllTables(Database db, boolean ifExists) {
                            DaoMaster.dropAllTables(db, ifExists);
                        }
                    }, DbRegionDao.class, DbGroupDao.class,
                    DbLightDao.class, DbDataChangeDao.class, DbDeleteGroupDao.class,
                    DbSceneDao.class, DbSceneActionsDao.class, DbUserDao.class, DbDiyGradientDao.class,
                    DbColorNodeDao.class, DbSwitchDao.class, DbSensorDao.class, DbCurtainDao.class,DbConnectorDao.class);
        }

        switch (oldVersion) {
            case 1:
                DbDeleteGroupDao.createTable(db, true);
                break;
            case 9:
                //由于新增devicetype对象，进行老用户数据兼容
                //原有分组判断方式改变，数据库强制更新然后再创建新数据库
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);

                        Map<Long, Integer> map = new HashMap<>();
                        List<DbLight> lights = DBUtils.INSTANCE.getAllLight();
                        List<DbGroup> groups = DBUtils.INSTANCE.getAllGroups();

                        for (int i = 0; i < groups.size(); i++) {
                            DbGroup group = groups.get(i);
                            if (group.getMeshAddr() == 0xffff) {
                                group.setDeviceType(Constants.DEVICE_TYPE_DEFAULT_ALL);
                            } else {
                                group.setDeviceType(Constants.DEVICE_TYPE_DEFAULT);
                            }

                            DBUtils.INSTANCE.updateGroup(group);
                        }

                        for (int k = 0; k < lights.size(); k++) {
                            map.put(lights.get(k).getBelongGroupId(), lights.get(k).getProductUUID());
                        }

                        for (Map.Entry<Long, Integer> entry : map.entrySet()) {
                            Long id = entry.getKey();
                            int uuid = entry.getValue();

                            DbGroup group = DBUtils.INSTANCE.getGroupByID(id);
                            if (group!=null&&group.getMeshAddr() != 0xffff) {
                                group.setDeviceType((long) uuid);
                                DBUtils.INSTANCE.updateGroup(group);
                            }

                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                break;

        }
    }
}
