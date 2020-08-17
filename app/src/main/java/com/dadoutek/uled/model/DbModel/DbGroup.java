package com.dadoutek.uled.model.DbModel;

import com.google.gson.annotations.Expose;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;

/**
 * Created by hejiajun on 2018/5/14.
 */
@Entity
public class DbGroup implements Serializable{

    static final long serialVersionUID = -15515456L;

    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private int meshAddr;
    private String name;
    private int brightness;
    private int colorTemperature;
    private int belongRegionId;
    private Long deviceType;
    private int index;

    private int color=0xffffff;//颜色;

    @Expose(serialize = false, deserialize = false)
    @Transient
    @Deprecated
    public boolean checked=false;
    @Expose(serialize = false, deserialize = false)
    @Transient
    public boolean isCheckedInGroup =false;
    @Expose(serialize = false, deserialize = false)
    @Transient
    public boolean selected;
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int textColor;//文字颜色

    public int status = 1;//开关状态

    @Expose(serialize = false, deserialize = false)
    @Transient
    public boolean isSeek = true;
    
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int deviceCount = 0;
    public int slowUpSlowDownStatus = 0;//1开0关;
    public int slowUpSlowDownSpeed = 1; //慢中快 1-3-5
    public  String routerName;
    public  String routerId;


    @Generated(hash = 1075427773)
    public DbGroup(Long id, int meshAddr, String name, int brightness,
            int colorTemperature, int belongRegionId, Long deviceType, int index,
            int color, int status, int slowUpSlowDownStatus,
            int slowUpSlowDownSpeed, String routerName, String routerId) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.brightness = brightness;
        this.colorTemperature = colorTemperature;
        this.belongRegionId = belongRegionId;
        this.deviceType = deviceType;
        this.index = index;
        this.color = color;
        this.status = status;
        this.slowUpSlowDownStatus = slowUpSlowDownStatus;
        this.slowUpSlowDownSpeed = slowUpSlowDownSpeed;
        this.routerName = routerName;
        this.routerId = routerId;
    }

    @Generated(hash = 1966413977)
    public DbGroup() {
    }



    public boolean isCheckedInGroup() {
        return isCheckedInGroup;
    }

    public int getslowUpSlowDownSpeed() {
        return slowUpSlowDownSpeed;
    }

    public void setslowUpSlowDownSpeed(int slowUpSlowDownSpeed) {
        this.slowUpSlowDownSpeed = slowUpSlowDownSpeed;
    }

    public void setCheckedInGroup(boolean checkedInGroup) {
        isCheckedInGroup = checkedInGroup;
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

    public int getMeshAddr() {
        return meshAddr;
    }

    public void setMeshAddr(int meshAddr) {
        this.meshAddr = meshAddr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getBelongRegionId() {
        return belongRegionId;
    }

    public void setBelongRegionId(int belongRegionId) {
        this.belongRegionId = belongRegionId;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isSeek() {
        return isSeek;
    }

    public void setSeek(boolean seek) {
        isSeek = seek;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public Long getDeviceType() {
        return this.deviceType;
    }

    public void setDeviceType(Long deviceType) {
        this.deviceType = deviceType;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getConnectionStatus() {
        return this.status;
    }

    public void setConnectionStatus(int connectionStatus) {
        this.status = connectionStatus;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getDeviceCount() {
        return this.deviceCount;
    }

    public void setDeviceCount(int deviceCount) {
        this.deviceCount = deviceCount;
    }

    public void setSlowUpSlowDownStatus(int slowUpSlowDownStatus) {
        this.slowUpSlowDownStatus = slowUpSlowDownStatus;
    }

    public int getSlowUpSlowDownSpeed() {
        return slowUpSlowDownSpeed;
    }

    public void setSlowUpSlowDownSpeed(int slowUpSlowDownSpeed) {
        this.slowUpSlowDownSpeed = slowUpSlowDownSpeed;
    }

    public int getSlowUpSlowDownStatus() {
        return slowUpSlowDownStatus;
    }

    @Override
    public String toString() {
        return "DbGroup{" +
                "id=" + id +
                ", meshAddr=" + meshAddr +
                ", name='" + name + '\'' +
                ", brightness=" + brightness +
                ", colorTemperature=" + colorTemperature +
                ", belongRegionId=" + belongRegionId +
                ", deviceType=" + deviceType +
                ", index=" + index +
                ", color=" + color +
                ", checked=" + checked +
                ", isCheckedInGroup=" + isCheckedInGroup +
                ", selected=" + selected +
                ", textColor=" + textColor +
                ", status=" + status +
                ", isSeek=" + isSeek +
                ", deviceCount=" + deviceCount +
                ", slowUpSlowDownStatus=" + slowUpSlowDownStatus +
                ", slowUpSlowDownSpeed=" + slowUpSlowDownSpeed +
                '}';
    }

    public String getRouterName() {
        return this.routerName;
    }

    public void setRouterName(String routerName) {
        this.routerName = routerName;
    }

    public String getRouterId() {
        return this.routerId;
    }

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

}
