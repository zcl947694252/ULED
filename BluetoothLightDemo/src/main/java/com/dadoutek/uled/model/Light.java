package com.dadoutek.uled.model;

import android.content.res.ColorStateList;

import com.telink.bluetooth.light.ConnectionStatus;
import com.telink.bluetooth.light.DeviceInfo;
import com.dadoutek.uled.R;

public final class Light {

    public String name;//设备名
    public String macAddress;//蓝牙地址
    public int meshAddress;//mesh地址
    public int brightness;//亮度
    public int color;//颜色
    public int temperature;//温度
    public ConnectionStatus status;//链接状态
    public DeviceInfo raw;//设备信息
    public boolean selected;//选择状态？
    public ColorStateList textColor;//文字颜色
    public int icon = R.drawable.icon_light_on;//灯状态显示图
    public String version;
    public boolean isLamp=true;

    public String getLabel() {
        return Integer.toString(this.meshAddress, 16) + ":" + this.brightness;
    }

    public String getLabel1() {
        return  "bulb-" + Integer.toString(this.meshAddress, 16);
    }

    public String getLabel2() {
        return Integer.toString(this.meshAddress, 16);
    }

    public void updateIcon() {
          if (this.status == ConnectionStatus.OFFLINE) {
              this.icon = R.drawable.icon_light_offline;
          } else if (this.status == ConnectionStatus.OFF) {
              this.icon = R.drawable.icon_light_off;
          } else if (this.status == ConnectionStatus.ON) {
              this.icon = R.drawable.icon_light_on;
          }
    }
}
