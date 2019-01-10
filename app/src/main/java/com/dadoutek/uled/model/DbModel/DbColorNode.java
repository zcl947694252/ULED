package com.dadoutek.uled.model.DbModel;

import com.google.gson.annotations.Expose;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Transient;

@Entity
public class DbColorNode  implements Serializable{

    static final long serialVersionUID = -15515456L;

    @Id(autoincrement = true)
    private Long id;
    private long index=0;
    @NotNull
    private long belongDynamicModeId=0;
    private int brightness=100;
    private int colorTemperature=0;
    private int rgbw=-1;

    @Expose(serialize = false, deserialize = false)
    @Transient
    public int dstAddress=0;

    @Generated(hash = 1039490734)
    public DbColorNode(Long id, long index, long belongDynamicModeId,
            int brightness, int colorTemperature, int rgbw) {
        this.id = id;
        this.index = index;
        this.belongDynamicModeId = belongDynamicModeId;
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

    public long getBelongDynamicModeId() {
        return belongDynamicModeId;
    }

    public void setBelongDynamicModeId(long belongDynamicModeId) {
        this.belongDynamicModeId = belongDynamicModeId;
    }
}
