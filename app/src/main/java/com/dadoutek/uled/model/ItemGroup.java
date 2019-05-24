package com.dadoutek.uled.model;

import java.io.Serializable;

/**
 * Created by hejiajun on 2018/5/5.
 */

public class ItemGroup implements Serializable {
    public int groupAress = 0;
    public int brightness = 50;
    public int temperature = 50;
    public int color = 0x4FFFE0;
    public String gpName = "";
    public boolean enableCheck=false;
    public boolean checked=false;
    public int deviceType;
    public boolean isNo=true;
}
