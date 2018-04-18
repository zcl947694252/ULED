package com.dadoutek.uled.model;

import android.content.res.ColorStateList;

import com.telink.bluetooth.light.ConnectionStatus;
import com.telink.bluetooth.light.DeviceInfo;
import com.dadoutek.uled.R;

import java.io.Serializable;
import java.util.ArrayList;

public final class Light implements Serializable,Cloneable{

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
    public boolean hasGroup=false;//当前灯是否有被分组
    public ArrayList<String> belongGroups=new ArrayList<>();//所属分组,每个灯最多属于8个分组

    public ArrayList<String> getBelongGroups() {
        return belongGroups;
    }

    public void setBelongGroups(ArrayList<String> belongGroups) {
        this.belongGroups = belongGroups;
    }

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

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
