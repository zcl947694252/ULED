package com.dadoutek.uled.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.dadoutek.uled.model.Light;
import com.telink.util.Event;
import com.telink.util.EventListener;

/**
 * Created by hejiajun on 2018/3/22.
 */

public class SendLightsInfo extends Service implements EventListener<String> {

    Light light = null;
    private Bundle bundle;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
//        bundle=intent.getExtras().getBundle("");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void performed(Event<String> event) {

    }
}
