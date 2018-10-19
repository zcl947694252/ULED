package com.dadoutek.uled.model.DbModel;

import com.dadoutek.uled.R;
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
    private int brightness;
    private int colorTemperature;
    private String macAddr;
    private int meshUUID;
    private int productUUID;
    private Long belongGroupId;

    public String color;//颜色

    @Transient
    public boolean selected;//选择状态
    @Transient
    public String version;//选择状态
    @Transient
    public boolean hasGroup = false;//当前灯是否有被分组
    @Transient
    public int textColor;//文字颜色

    public int connectionStatus = 1;//链接状态
    @Transient
    public int icon = R.drawable.icon_light_on;//灯状态显示图


    @Generated(hash = 2075223479)
    public DbLight() {
    }

    @Generated(hash = 2117051117)
    public DbLight(Long id, int meshAddr, String name, int brightness,
            int colorTemperature, String macAddr, int meshUUID, int productUUID,
            Long belongGroupId, String color, int connectionStatus) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.brightness = brightness;
        this.colorTemperature = colorTemperature;
        this.macAddr = macAddr;
        this.meshUUID = meshUUID;
        this.productUUID = productUUID;
        this.belongGroupId = belongGroupId;
        this.color = color;
        this.connectionStatus = connectionStatus;
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

    public String getMacAddr() {
        return this.macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }

    public int getMeshUUID() {
        return this.meshUUID;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setMeshUUID(int meshUUID) {
        this.meshUUID = meshUUID;
    }

    public int getProductUUID() {
        return this.productUUID;
    }

    public void setProductUUID(int productUUID) {
        this.productUUID = productUUID;
    }

    public Long getBelongGroupId() {
        return this.belongGroupId;
    }

    public void setBelongGroupId(Long belongGroupId) {
        this.belongGroupId = belongGroupId;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getLabel() {
        return Integer.toString(this.getMeshAddr(), 16) + ":" + this.brightness;
    }

    public void updateIcon() {

        if (this.connectionStatus == ConnectionStatus.OFFLINE.getValue()) {
            this.icon = R.drawable.icon_light_offline;
        } else if (this.connectionStatus == ConnectionStatus.OFF.getValue()) {
            this.icon = R.drawable.icon_light_off;
        } else if (this.connectionStatus == ConnectionStatus.ON.getValue()) {
            this.icon = R.drawable.icon_light_on;
        }
    }

    public int getConnectionStatus() {
        return this.connectionStatus;
    }

    public void setConnectionStatus(int connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

}
