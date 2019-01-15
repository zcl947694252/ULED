package com.dadoutek.uled.model.DbModel;

import com.google.gson.annotations.Expose;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Transient;

@Entity
public class DbColorNode  implements Serializable{

    static final long serialVersionUID = -15515456L;

    @Id(autoincrement = true)
    private Long id;
    private long index=0;
    private long belongDynamicChangeId =0;
    private int brightness=100;
    private int colorTemperature=0;
    private int rgbw=-1;

    @Expose(serialize = false, deserialize = false)
    @Transient
    public int dstAddress=0;

    public DbColorNode() {
    }

    public DbColorNode(Long id, long index, long belongDynamicChangeId, int brightness, int colorTemperature, int rgbw, int dstAddress) {
        this.id = id;
        this.index = index;
        this.belongDynamicChangeId = belongDynamicChangeId;
        this.brightness = brightness;
        this.colorTemperature = colorTemperature;
        this.rgbw = rgbw;
        this.dstAddress = dstAddress;
    }

    @Generated(hash = 631372582)
    public DbColorNode(Long id, long index, long belongDynamicChangeId, int brightness, int colorTemperature, int rgbw) {
        this.id = id;
        this.index = index;
        this.belongDynamicChangeId = belongDynamicChangeId;
        this.brightness = brightness;
        this.colorTemperature = colorTemperature;
        this.rgbw = rgbw;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public long getBelongDynamicChangeId() {
        return belongDynamicChangeId;
    }

    public void setBelongDynamicChangeId(long belongDynamicChangeId) {
        this.belongDynamicChangeId = belongDynamicChangeId;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    public int getColorTemperature() {
        return colorTemperature;
    }

    public void setColorTemperature(int colorTemperature) {
        this.colorTemperature = colorTemperature;
    }

    public int getRgbw() {
        return rgbw;
    }

    public void setRgbw(int rgbw) {
        this.rgbw = rgbw;
    }

    public int getDstAddress() {
        return dstAddress;
    }

    public void setDstAddress(int dstAddress) {
        this.dstAddress = dstAddress;
    }

    @Override
    public String toString() {
        return "DbColorNode{" +
                "id=" + id +
                ", index=" + index +
                ", belongDynamicChangeId=" + belongDynamicChangeId +
                ", brightness=" + brightness +
                ", colorTemperature=" + colorTemperature +
                ", rgbw=" + rgbw +
                ", dstAddress=" + dstAddress +
                '}';
    }
}
