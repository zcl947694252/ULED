package com.dadoutek.uled.model.DbModel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Transient;

/**
 * Created by hejiajun on 2018/5/7.
 */

@Entity
public class DbSceneActions {
    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private long belongSceneId;
    @NotNull
    private int groupAddr;
    @NotNull
    private int colorTemperature;
    @NotNull
    private int brightness;

    public int color=0xffffff;//颜色;

    private boolean isOn;

    private int deviceType;

    @Generated(hash = 885919265)
    public DbSceneActions() {
    }

    @Generated(hash = 1303864798)
    public DbSceneActions(Long id, long belongSceneId, int groupAddr,
            int colorTemperature, int brightness, int color, boolean isOn,
            int deviceType) {
        this.id = id;
        this.belongSceneId = belongSceneId;
        this.groupAddr = groupAddr;
        this.colorTemperature = colorTemperature;
        this.brightness = brightness;
        this.color = color;
        this.isOn = isOn;
        this.deviceType = deviceType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getBelongSceneId() {
        return belongSceneId;
    }

    public void setBelongSceneId(long belongSceneId) {
        this.belongSceneId = belongSceneId;
    }

    public int getGroupAddr() {
        return groupAddr;
    }

    public void setGroupAddr(int groupAddr) {
        this.groupAddr = groupAddr;
    }

    public int getColorTemperature() {
        return colorTemperature;
    }

    public void setColorTemperature(int colorTemperature) {
        this.colorTemperature = colorTemperature;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean getIsOn() {
        return this.isOn;
    }

    public void setIsOn(boolean isOn) {
        this.isOn = isOn;
    }

    public int getDeviceType() {
        return this.deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }
}
