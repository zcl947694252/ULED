package com.dadoutek.uled.model.DbModel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * 创建者     ZCL
 * 创建时间   2020/1/16 14:47
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述 @entity @id  build make project完成
 */
@Entity
public class DbEightSwitch {
    @Id(autoincrement = true)
    private  Long id;
    private String firmwareVersion;
    private int meshAddr;
    private String name;
    private String macAddr;
    private int productUUID;
    private int index;
    private String keys;
    @Generated(hash = 767865062)
    public DbEightSwitch(Long id, String firmwareVersion, int meshAddr, String name,
            String macAddr, int productUUID, int index, String keys) {
        this.id = id;
        this.firmwareVersion = firmwareVersion;
        this.meshAddr = meshAddr;
        this.name = name;
        this.macAddr = macAddr;
        this.productUUID = productUUID;
        this.index = index;
        this.keys = keys;
    }
    @Generated(hash = 638875022)
    public DbEightSwitch() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getFirmwareVersion() {
        return this.firmwareVersion;
    }
    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
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
    public String getKeys() {
        return this.keys;
    }
    public void setKeys(String keys) {
        this.keys = keys;
    }
}