package com.dadoutek.uled.model.DbModel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;

import java.io.Serializable;

@Entity
public class DbSensor implements Serializable {

    static final long serialVersionUID = -15515456L;

    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private int meshAddr;
    private String name;
    private String controlGroupAddr;
    private String macAddr;
    private int productUUID;
    private int index;
    private Long belongGroupId;
    private String version;
    public int rssi =1000;

    @Generated(hash = 2029813700)
    public DbSensor(Long id, int meshAddr, String name, String controlGroupAddr,
            String macAddr, int productUUID, int index, Long belongGroupId,
            String version, int rssi) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.controlGroupAddr = controlGroupAddr;
        this.macAddr = macAddr;
        this.productUUID = productUUID;
        this.index = index;
        this.belongGroupId = belongGroupId;
        this.version = version;
        this.rssi = rssi;
    }
    @Generated(hash = 295132781)
    public DbSensor() {
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
    public String getControlGroupAddr() {
        return this.controlGroupAddr;
    }
    public void setControlGroupAddr(String controlGroupAddr) {
        this.controlGroupAddr = controlGroupAddr;
    }
    public String getMacAddr() {
        return this.macAddr;
    }
    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }
    public int getProductUUID() {
        return this.productUUID;
    }
    public void setProductUUID(int productUUID) {
        this.productUUID = productUUID;
    }
    public int getIndex() {
        return this.index;
    }
    public void setIndex(int index) {
        this.index = index;
    }
    public Long getBelongGroupId() {
        return this.belongGroupId;
    }
    public void setBelongGroupId(Long belongGroupId) {
        this.belongGroupId = belongGroupId;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "DbSensor{" +
                "id=" + id +
                ", meshAddr=" + meshAddr +
                ", name='" + name + '\'' +
                ", controlGroupAddr='" + controlGroupAddr + '\'' +
                ", macAddr='" + macAddr + '\'' +
                ", productUUID=" + productUUID +
                ", index=" + index +
                ", belongGroupId=" + belongGroupId +
                ", version='" + version + '\'' +
                '}';
    }
    public int getRssi() {
        return this.rssi;
    }
    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
