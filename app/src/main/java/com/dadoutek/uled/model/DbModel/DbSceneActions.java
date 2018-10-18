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
    @Transient
    public String color;
    
    @Generated(hash = 885919265)
    public DbSceneActions() {
    }
    @Generated(hash = 511749042)
    public DbSceneActions(Long id, long belongSceneId, int groupAddr,
            int colorTemperature, int brightness, String color) {
        this.id = id;
        this.belongSceneId = belongSceneId;
        this.groupAddr = groupAddr;
        this.colorTemperature = colorTemperature;
        this.brightness = brightness;
        this.color = color;
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public long getBelongSceneId() {
        return this.belongSceneId;
    }
    public void setBelongSceneId(long belongSceneId) {
        this.belongSceneId = belongSceneId;
    }
    public int getGroupAddr() {
        return this.groupAddr;
    }
    public void setGroupAddr(int groupAddr) {
        this.groupAddr = groupAddr;
    }
    public int getColorTemperature() {
        return this.colorTemperature;
    }
    public void setColorTemperature(int colorTemperature) {
        this.colorTemperature = colorTemperature;
    }
    public int getBrightness() {
        return this.brightness;
    }
    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }
    public String getColor() {
        return this.color;
    }
    public void setColor(String color) {
        this.color = color;
    }

}
