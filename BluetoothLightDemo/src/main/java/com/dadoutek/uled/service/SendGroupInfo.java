package com.dadoutek.uled.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.model.Group;
import com.telink.bluetooth.event.NotificationEvent;
import com.dadoutek.uled.model.Groups;
import com.telink.util.Event;
import com.telink.util.EventListener;

/**
 * Created by hejiajun on 2018/3/22.
 */

public class SendGroupInfo extends Service implements EventListener<String>{

    private Group group;
    private Groups groups;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        TelinkLightApplication.getApp().addEventListener(NotificationEvent.ONLINE_STATUS, this);
    }

    @Override
    public void performed(Event<String> event) {

    }
}
