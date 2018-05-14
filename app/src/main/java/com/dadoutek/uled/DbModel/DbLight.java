package com.dadoutek.uled.DbModel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by hejiajun on 2018/5/14.
 */
@Entity
public class DbLight {
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
    @Generated(hash = 125913781)
    public DbLight(Long id, int meshAddr, String name, int brightness,
            int colorTemperature, String macAddr, int meshUUID, int productUUID,
            Long belongGroupId) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.brightness = brightness;
        this.colorTemperature = colorTemperature;
        this.macAddr = macAddr;
        this.meshUUID = meshUUID;
        this.productUUID = productUUID;
        this.belongGroupId = belongGroupId;
    }
    @Generated(hash = 2075223479)
    public DbLight() {
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
}
