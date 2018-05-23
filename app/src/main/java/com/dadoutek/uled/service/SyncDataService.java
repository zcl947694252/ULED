package com.dadoutek.uled.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.dadoutek.uled.DbModel.DBUtils;
import com.dadoutek.uled.DbModel.DbDataChange;

import java.util.List;

/**
 * Created by hejiajun on 2018/5/23.
 */

public class SyncDataService extends Service {

    private DbDataChange dbDataChange;
    private List<DbDataChange> dbDataChangeList;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startSendData();
    }

    private void startSendData() {
        getChange();
    }

    private void getChange() {
        dbDataChangeList= DBUtils.getDataChangeAll();

        for(int i=0;i<dbDataChangeList.size();i++){
            getLocalData(dbDataChangeList.get(i).getTableName(),
                    dbDataChangeList.get(i).getChangeId(),
                    dbDataChangeList.get(i).getChangeType());
        }
    }

    private void getLocalData(String tableName, Long changeId, String type) {
        switch (tableName){
            case "DB_GROUPS":
                break;
            case "DB_LIGHT":
                break;
            case "DB_REGION":
                break;
            case "DB_SCENE":
                break;
            case "DB_SCENE_ACTIONS":
                break;
            case "DB_USER":
                break;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
