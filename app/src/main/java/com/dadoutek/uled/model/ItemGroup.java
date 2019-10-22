package com.dadoutek.uled.model;

import java.io.Serializable;

/**
 * Created by hejiajun on 2018/5/5.
 */

public class ItemGroup implements Serializable {
    public int groupAddress = 0;
    public int brightness = 50;
    public int temperature = 50;
    public int color = 0x4FFFE0;
    public String gpName = "";
    public boolean enableCheck=false;
    public boolean checked=false;
    public int deviceType;
    public boolean isNo=true;

    @Override
    public String toString() {
        return "ItemGroup{" +
                "groupAddress=" + groupAddress +
                ", brightness=" + brightness +
                ", temperature=" + temperature +
                ", color=" + color +
                ", gpName='" + gpName + '\'' +
                ", enableCheck=" + enableCheck +
                ", checked=" + checked +
                ", deviceType=" + deviceType +
                ", isNo=" + isNo +
                '}';
    }
}
