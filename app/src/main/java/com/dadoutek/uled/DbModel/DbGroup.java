package com.dadoutek.uled.DbModel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Transient;

/**
 * Created by hejiajun on 2018/5/14.
 */
@Entity
public class DbGroup {
    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private int meshAddr;
    private String name;
    private int brightness;
    private int colorTemperature;
    private int belongRegionId;

    @Transient
    public boolean checked;

    @Generated(hash = 55678821)
    public DbGroup(Long id, int meshAddr, String name, int brightness,
                   int colorTemperature, int belongRegionId) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.brightness = brightness;
        this.colorTemperature = colorTemperature;
        this.belongRegionId = belongRegionId;
    }

    @Generated(hash = 1966413977)
    public DbGroup() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getMeshAddr() {
        return this.meshAddr;
    }

    public void setMeshAddr(int meshAddr) {
        this.meshAddr = meshAddr;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBrightness() {
        return this.brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    public int getColorTemperature() {
        return this.colorTemperature;
    }

    public void setColorTemperature(int colorTemperature) {
        this.colorTemperature = colorTemperature;
    }

    public int getBelongRegionId() {
        return this.belongRegionId;
    }

    public void setBelongRegionId(int belongRegionId) {
        this.belongRegionId = belongRegionId;
    }
}
