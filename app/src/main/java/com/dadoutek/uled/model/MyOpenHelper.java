package com.dadoutek.uled.model;

import android.content.Context;
import android.util.Log;

import com.dadoutek.uled.dao.DaoMaster;
import com.dadoutek.uled.dao.DbColorNodeDao;
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
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbColorNode;
import com.dadoutek.uled.model.DbModel.DbDiyGradient;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.DbModel.DbScene;
import com.dadoutek.uled.util.MigrationHelper;

import org.greenrobot.greendao.database.Database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyOpenHelper extends DaoMaster.OpenHelper {
    MyOpenHelper(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {

        if(oldVersion < newVersion){
            MigrationHelper.migrate(db,DbRegionDao.class,DbGroupDao.class,
                    DbLightDao.class,DbDataChangeDao.class,DbDeleteGroupDao.class,
                    DbSceneDao.class,DbSceneActionsDao.class,DbUserDao.class,DbDiyGradientDao.class,
                    DbColorNodeDao.class, DbSwitchDao.class, DbSensorDao.class, DbCurtainDao.class);
        }

        switch (oldVersion) {
            case 1:
                DbDeleteGroupDao.createTable(db, true);
                break;
            case 9:
                //原有分组判断方式改变，数据库强制更新然后再创建新数据库
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);

                        Map<Long,Integer> map = new HashMap<>();
                        List<DbLight> lights= DBUtils.INSTANCE.getAllLight();
                        for(int k=0;k<lights.size();k++){
                            DbGroup group=DBUtils.INSTANCE.getGroupByID(lights.get(k).getBelongGroupId());
                            group.setDeviceType(Constant.DEVICE_TYPE_DEFAULT);
                            DBUtils.INSTANCE.updateGroup(group);

                            map.put(lights.get(k).getBelongGroupId(),lights.get(k).getProductUUID());
                        }

                        for (Map.Entry<Long,Integer> entry : map.entrySet()) {
                            Long id=entry.getKey();
                            int uuid=entry.getValue();

                            DbGroup group=DBUtils.INSTANCE.getGroupByID(id);

                            if(group.getMeshAddr()!=0xffff){
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
