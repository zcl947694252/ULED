package com.dadoutek.uled.DbModel;

import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.dao.DbSceneActionsDao;

import org.greenrobot.greendao.query.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hejiajun on 2018/5/18.
 */

public class DBUtils {

    public static List<DbSceneActions> searchActionsBySceneId(long id) {
        List<DbSceneActions> list = new ArrayList<>();
        Query<DbSceneActions> query = TelinkLightApplication.getDaoInstant().getDbSceneActionsDao().queryBuilder().
                where(DbSceneActionsDao.Properties.ActionId.eq(id)).build();
        for (DbSceneActions dbSceneActions : query.list()) {
            list.add(dbSceneActions);
        }
        return list;
    }
}
