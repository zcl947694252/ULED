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
    private int colorTemperature=0;
    private int rgbw=-1;
    @Generated(hash = 1603503987)
    public DbColorNode(Long id, long index, int brightness, int colorTemperature,
            int rgbw) {
        this.id = id;
        this.index = index;
        this.brightness = brightness;
        this.colorTemperature = colorTemperature;
        this.rgbw = rgbw;
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
    public int getColorTemperature() {
        return this.colorTemperature;
    }
    public void setColorTemperature(int colorTemperature) {
        this.colorTemperature = colorTemperature;
    }
    public int getRgbw() {
        return this.rgbw;
    }
    public void setRgbw(int rgbw) {
        this.rgbw = rgbw;
    }

}
