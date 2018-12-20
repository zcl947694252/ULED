package com.dadoutek.uled.model.DbModel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class DbColorNode  implements Serializable{

    static final long serialVersionUID = -15515456L;

    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private long index=0;
    private int brightness=100;
    private int rgbw=-1;
    private int colorTemperature=0;
    @Generated(hash = 1439200622)
    public DbColorNode(Long id, long index, int brightness, int rgbw,
            int colorTemperature) {
        this.id = id;
        this.index = index;
        this.brightness = brightness;
        this.rgbw = rgbw;
        this.colorTemperature = colorTemperature;
    }
    @Generated(hash = 1783393701)
    public DbColorNode() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public long getIndex() {
        return this.index;
    }
    public void setIndex(long index) {
        this.index = index;
    }
    public int getBrightness() {
        return this.brightness;
    }
    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }
    public int getRgbw() {
        return this.rgbw;
    }
    public void setRgbw(int rgbw) {
        this.rgbw = rgbw;
    }
    public int getColorTemperature() {
        return this.colorTemperature;
    }
    public void setColorTemperature(int colorTemperature) {
        this.colorTemperature = colorTemperature;
    }

}
