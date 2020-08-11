package com.dadoutek.uled.mqtt;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;


public class MyServiceConnection implements ServiceConnection {

    private MqttService mqttService;
    private com.dadoutek.uled.mqtt.IGetMessageCallBack IGetMessageCallBack;

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mqttService = ((MqttService.CustomBinder)iBinder).getService();
        mqttService.setIGetMessageCallBack(IGetMessageCallBack);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mqttService = null;
    }

    public MqttService getMqttService(){
        return mqttService;
    }

    public void setIGetMessageCallBack(IGetMessageCallBack IGetMessageCallBack){
        this.IGetMessageCallBack = IGetMessageCallBack;
    }
}