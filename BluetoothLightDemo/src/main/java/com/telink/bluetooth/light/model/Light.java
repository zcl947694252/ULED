package com.telink.bluetooth.light.model;

import android.content.res.ColorStateList;

import com.telink.bluetooth.light.ConnectionStatus;
import com.telink.bluetooth.light.DeviceInfo;
import com.telink.bluetooth.light.R;

public final class Light {

    public String name;
    public String macAddress;
    public int meshAddress;
    public int brightness;
    public int color;
    public int temperature;
    public ConnectionStatus status;
    public DeviceInfo raw;
    public boolean selected;
    public ColorStateList textColor;
    public int icon = R.drawable.icon_light_on;
    public String version;

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
