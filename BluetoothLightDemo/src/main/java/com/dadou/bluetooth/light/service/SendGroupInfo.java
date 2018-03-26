package com.dadou.bluetooth.light.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.dadou.bluetooth.event.NotificationEvent;
import com.dadou.bluetooth.light.TelinkLightApplication;
import com.dadou.bluetooth.light.model.Group;
import com.dadou.bluetooth.light.model.Groups;
import com.dadou.util.Event;
import com.dadou.util.EventListener;

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
