package com.dadoutek.uled.model.DbModel;

import com.dadoutek.uled.R;
import com.google.gson.annotations.Expose;
import com.telink.bluetooth.light.ConnectionStatus;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;

/**
 * Created by hejiajun on 2018/5/14.
 */
@Entity
public class DbLight implements Serializable {

    static final long serialVersionUID = -15515456L;

    @Id(autoincrement = true)
    private Long id;
    private int meshAddr;
    private String name;
    private String deviceName;
    private int brightness;
    private int colorTemperature;
    private String macAddr;
    private int meshUUID;
    private int productUUID;
    private Long belongGroupId;
    private int index;

    public int color=0xffffff;//颜色

    @Expose(serialize = false, deserialize = false)
    @Transient
    public boolean selected;//选择状态
    @Expose(serialize = false, deserialize = false)
    @Transient
    public String version;//选择状态
    @Expose(serialize = false, deserialize = false)
    @Transient
    public boolean hasGroup = false;//当前灯是否有被分组
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int textColor;//文字颜色

    @Expose(serialize = false, deserialize = false)
    @Transient
    public boolean isSeek = true;//选择色温还是亮度

    public int status = 1;//链接状态
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int icon = R.drawable.icon_light_on;//灯状态显示图


    @Generated(hash = 2075223479)
    public DbLight() {
    }

    @Generated(hash = 2051794788)
    public DbLight(Long id, int meshAddr, String name, String deviceName,
            int brightness, int colorTemperature, String macAddr, int meshUUID,
            int productUUID, Long belongGroupId, int index, int color, int status) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.deviceName = deviceName;
        this.brightness = brightness;
        this.colorTemperature = colorTemperature;
        this.macAddr = macAddr;
        this.meshUUID = meshUUID;
        this.productUUID = productUUID;
        this.belongGroupId = belongGroupId;
        this.index = index;
        this.color = color;
        this.status = status;
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

    public String getMacAddr() {
        return macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }

    public int getMeshUUID() {
        return meshUUID;
    }

    public void setMeshUUID(int meshUUID) {
        this.meshUUID = meshUUID;
    }

    public int getProductUUID() {
        return productUUID;
    }

    public void setProductUUID(int productUUID) {
        this.productUUID = productUUID;
    }

    public Long getBelongGroupId() {
        return belongGroupId;
    }

    public void setBelongGroupId(Long belongGroupId) {
        this.belongGroupId = belongGroupId;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isHasGroup() {
        return hasGroup;
    }

    public void setHasGroup(boolean hasGroup) {
        this.hasGroup = hasGroup;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public int getConnectionStatus() {
        return status;
    }

    public void setConnectionStatus(int connectionStatus) {
        this.status = connectionStatus;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public void updateIcon() {

        if (this.status == ConnectionStatus.OFFLINE.getValue()) {
            this.icon = R.drawable.icon_device_down;
        } else if (this.status == ConnectionStatus.OFF.getValue()) {
            this.icon = R.drawable.icon_device_down;
        } else if (this.status == ConnectionStatus.ON.getValue()) {
            this.icon = R.drawable.icon_device_open;
        }
    }

    public void updateRgbIcon(){
        if (this.status == ConnectionStatus.OFFLINE.getValue()) {
            this.icon = R.drawable.icon_rgblight_down;
        } else if (this.status == ConnectionStatus.OFF.getValue()) {
            this.icon = R.drawable.icon_rgblight_down;
        } else if (this.status == ConnectionStatus.ON.getValue()) {
            this.icon = R.drawable.icon_rgblight;
        }
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setSeek(boolean seek) {
        this.isSeek = seek;
    }

    public boolean isSeek() {
        return isSeek;
    }

    @Override
    public String toString() {
        return "DbLight{" +
                "id=" + id +
                ", meshAddr=" + meshAddr +
                ", name='" + name + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", brightness=" + brightness +
                ", colorTemperature=" + colorTemperature +
                ", macAddr='" + macAddr + '\'' +
                ", meshUUID=" + meshUUID +
                ", productUUID=" + productUUID +
                ", belongGroupId=" + belongGroupId +
                ", index=" + index +
                ", color=" + color +
                ", selected=" + selected +
                ", version='" + version + '\'' +
                ", hasGroup=" + hasGroup +
                ", textColor=" + textColor +
                ", isSeek=" + isSeek +
                ", status=" + status +
                ", icon=" + icon +
                '}';
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
